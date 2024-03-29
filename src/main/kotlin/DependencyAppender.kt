import java.io.File


data class Dependency(
    val groupId: String,
    val artifactId: String,
    val version: String,
    val type: String = "jar",
    val classifier: String = "") {

    fun mainKey(): String {
        return "$groupId:$artifactId:$type"
    }
}

interface DependencyAppender {
    fun applicable(dependency: Dependency): Boolean
    fun append(dependency: Dependency)
}

class ClassDependencyAppender(private val mavenAdapter: MavenAdapter, private val cypherExecutor: CypherExecutor, private val workdir: String) :
    DependencyAppender {

    override fun applicable(dependency: Dependency): Boolean =  arrayOf("jar", "ejb" ,"war").contains(dependency.type)

    override fun append(dependency: Dependency) {
        val jdepsResult = resolve(dependency)

        insert(dependency, jdepsResult)
    }


    private fun resolve(dependency: Dependency): JdepsResult {
        val jarFile = mavenAdapter.downloadArtifact(dependency)

        if (jarFile.extension == "war") {
            return Expander(workdir, jarFile).use { AnalyseJavaDependencies.read(File(it.expand(), "WEB-INF/classes")) }
        }

        return AnalyseJavaDependencies.read(jarFile)
    }

    private fun insert(dependency: Dependency, jdepsResult: JdepsResult) {

        val artifactRef = "artifact"
        val arrifactVersionRef = "artifactVersion"

        val artifactNode = QueryUtil.createArtifact(dependency, artifactRef)
        val artifactVersionNode = QueryUtil.createArtifactVersion(dependency, arrifactVersionRef)

        val artifactVersionQueryPart =
            """|MERGE $artifactNode
               |MERGE ($artifactRef)<-[:${RelationType.IS_VERSION_OF}]-$artifactVersionNode
               |""".trimMargin()

        jdepsResult.resultMap.forEach { (className, referencedClassNames) ->
            val classNameRef = className.replace('.', '_').replace('-', '_')
            val classQueryPart = QueryUtil.createClass(arrifactVersionRef, className, classNameRef)

            val builder = StringBuilder()
            builder.append(artifactVersionQueryPart).append("\n")
            builder.append(classQueryPart).append("\n")

            referencedClassNames.forEach { referencedClassName ->
                val referencedClassNameRef = referencedClassName.replace('.', '_').replace('-', '_')
                val depClassQuery =
                    QueryUtil.createClassDependency(classNameRef, referencedClassNameRef, referencedClassName)
                builder.append(depClassQuery).append("\n")
            }

            cypherExecutor.run(builder.toString())
        }
    }
}


class MavenDependencyAppender(private val mavenDependencyResolver: MavenDependencyResolver, private val cypherExecutor: CypherExecutor) : DependencyAppender {
    override fun applicable(dependency: Dependency): Boolean = dependency.type == "jar" || dependency.type == "ear"

    override fun append(dependency: Dependency) {
        val dependentDependencies = mavenDependencyResolver.resolve(dependency)

        val artifactRefA = "artifactA"
        val artifactGroupRefA = "artifactGroupA"
        val artifactVersionRefA = "artifactVersionA"

        val artifactNode = QueryUtil.createArtifact(dependency, artifactRefA)
        val artifactGroupNode = QueryUtil.createArtifactGroup(dependency, artifactGroupRefA)
        val artifactVersionNode = QueryUtil.createArtifactVersion(dependency, artifactVersionRefA)

        val artifactVersionAQueryPart =
            """|MERGE $artifactNode
               |MERGE ($artifactRefA)<-[:${RelationType.IS_VERSION_OF}]-$artifactVersionNode
               |MERGE $artifactGroupNode
               |MERGE ($artifactGroupRefA)<-[:${RelationType.IS_GROUP_OF}]-($artifactRefA)
               |MERGE ($artifactGroupRefA)<-[:${RelationType.IS_GROUP_OF}]-($artifactVersionRefA)
               |""".trimMargin()

        dependentDependencies.forEach {
            val dependentDependency = it

            val artifactRefB = "artifactB"
            val artifactGroupRefB = "artifactGroupB"
            val artifactVersionRefB = "artifactVersionB"

            val dependentArtifactNode = QueryUtil.createArtifact(dependentDependency, artifactRefB)
            val dependentArtifactGroupNode = QueryUtil.createArtifactGroup(dependentDependency, artifactGroupRefB)
            val dependentArtifactVersionNode = QueryUtil.createArtifactVersion(dependentDependency, artifactVersionRefB)

            val query = StringBuilder(artifactVersionAQueryPart)
            query.append(
                """|MERGE $dependentArtifactNode
                   |MERGE ($artifactRefB)<-[:${RelationType.IS_VERSION_OF}]-$dependentArtifactVersionNode
                   |MERGE ($artifactVersionRefA)-[:${RelationType.DEPEND_ON_ARTIFACT_VERSION}]->($artifactVersionRefB)
                   |MERGE $dependentArtifactGroupNode
                   |MERGE ($artifactGroupRefB)<-[:${RelationType.IS_GROUP_OF}]-($artifactRefB)
                   |MERGE ($artifactGroupRefB)<-[:${RelationType.IS_GROUP_OF}]-($artifactVersionRefB)
                   |""".trimMargin()
            )

            cypherExecutor.run(query.toString())
        }

    }

}


class ParentDependencyAppender(private val mavenParentResolver: MavenParentResolver, private val cypherExecutor: CypherExecutor) :
    DependencyAppender {

    override fun applicable(dependency: Dependency): Boolean = true

    override fun append(dependency: Dependency) {
        val list = mavenParentResolver.resolve(dependency)

        if (list.isEmpty()) return

        if(list.size != 1) throw IllegalStateException("list must contain one element.")

        val parentDependency = list[0]

        val artifactRef = "artifact"
        val artifactGroupRef = "artifactGroup"
        val artifactVersionRef = "artifactVersion"

        val artifactNode = QueryUtil.createArtifact(dependency, artifactRef)
        val artifactGroupNode = QueryUtil.createArtifactGroup(dependency, artifactGroupRef)
        val artifactVersionNode = QueryUtil.createArtifactVersion(dependency, artifactVersionRef)

        val parentArtifactRef = "parentArtifact"
        val parentArtifactVersionRef = "parentArtifactVersion"

        val parentArtifactNode = QueryUtil.createArtifact(parentDependency, parentArtifactRef)
        val parentArtifactVersionNode = QueryUtil.createArtifactVersion(parentDependency, parentArtifactVersionRef)

        val query =
            """|MERGE $artifactNode
               |MERGE ($artifactRef)<-[:${RelationType.IS_VERSION_OF}]-$artifactVersionNode
               |MERGE $parentArtifactNode
               |MERGE ($parentArtifactRef)<-[:${RelationType.IS_VERSION_OF}]-$parentArtifactVersionNode
               |MERGE ($artifactVersionRef)-[:${RelationType.IS_CHILD_OF_ARTIFACT_VERSION}]->($parentArtifactVersionRef)
               |MERGE $artifactGroupNode
               |MERGE ($artifactGroupRef)<-[:${RelationType.IS_GROUP_OF}]-($parentArtifactRef)
               |MERGE ($artifactGroupRef)<-[:${RelationType.IS_GROUP_OF}]-($parentArtifactVersionRef)
               |""".trimMargin()

        cypherExecutor.run(query)
    }
}

enum class ArtifactProperty(val key: String) {
    GroupId("groupId"),
    ArtifactId("artifactId"),
    Type("type"),
}

enum class ArtifactGroupProperty(val key: String) {
    GroupId("groupId"),
}

enum class ArtifactVersionProperty(val key: String) {
    Version("version"),

    // TODO should be properties of ArtifactVersion yes/no?
    GroupId("groupId"),
    ArtifactId("artifactId"),
    Type("type"),
}

enum class JavaClassProperty(val key: String) {
    Name("name"),
}

object QueryUtil {

    fun createArtifact(
        dependency: Dependency,
        ref: String = ""
    ): String {
        return "($ref:${NodeLabel.ARTIFACT.key} { ${ArtifactProperty.GroupId.key}: '${dependency.groupId}', ${ArtifactProperty.ArtifactId.key}: '${dependency.artifactId}', ${ArtifactProperty.Type.key}: '${dependency.type}' } )"
    }

    fun createArtifactGroup(
        dependency: Dependency,
        ref: String = ""
    ): String {
        return "($ref:${NodeLabel.ARTIFACT_GROUP.key} { ${ArtifactProperty.GroupId.key}: '${dependency.groupId}'} )"
    }

    private fun createArtifactIndex(): String {
        return "CREATE INDEX ON :${NodeLabel.ARTIFACT.key}( ${ArtifactProperty.GroupId.key}, ${ArtifactProperty.ArtifactId.key}, ${ArtifactProperty.Type.key})"
    }

    fun createArtifactVersion(
        dependency: Dependency,
        ref: String = ""
    ): String {
        return "($ref:${NodeLabel.ARTIFACT_VERSION.key} { ${ArtifactVersionProperty.Version.key}: '${dependency.version}', ${ArtifactVersionProperty.GroupId.key}: '${dependency.groupId}', ${ArtifactVersionProperty.ArtifactId.key}: '${dependency.artifactId}', ${ArtifactVersionProperty.Type.key}: '${dependency.type}' } )"
    }

    private fun createArtifactGroupIndex(): String {
        return "CREATE INDEX ON :${NodeLabel.ARTIFACT_GROUP.key}(${ArtifactGroupProperty.GroupId.key})"
    }

    private fun createArtifactVersionIndex(): String {
        return "CREATE INDEX ON :${NodeLabel.ARTIFACT_VERSION.key}(${ArtifactVersionProperty.Version.key})"
    }

    fun createClass(artifactVersionRefNameA: String, className: String, classRefName: String): String {
        return """|MERGE ($classRefName:${NodeLabel.JAVA_CLASS.key} {${JavaClassProperty.Name.key}: '$className'})
            |MERGE ($artifactVersionRefNameA)-[:${RelationType.PROVIDE_JAVA_CLASS}]->($classRefName)
            |""".trimMargin()
    }

    private fun createJavaClassIndex(): String {
        return "CREATE INDEX ON :${NodeLabel.JAVA_CLASS.key}(${JavaClassProperty.Name.key})"
    }

    fun createClassDependency(classRefName: String, dependencyClassRefName: String, className: String): String {
        return """|MERGE ($dependencyClassRefName:${NodeLabel.JAVA_CLASS.key} {${JavaClassProperty.Name.key}: '$className'})
            |MERGE ($classRefName)-[:${RelationType.DEPEND_ON_EXTERNAL_JAVA_CLASS}]->($dependencyClassRefName)
            |""".trimMargin()
    }

    fun createIndices(): List<String> {

        return listOf(
            createArtifactIndex(),
            createArtifactVersionIndex(),
            createArtifactGroupIndex(),
            createJavaClassIndex()
        )
    }
}

enum class RelationType {

    /**
     * Describe relation between a version ([NodeLabel.ARTIFACT_VERSION]) of an artifact ([NodeLabel.ARTIFACT]).
     * <pre>
     * (:ArtifactVersion) -[:IS_VERSION_OF]-> (:Artifact)
    </pre> *
     */
    IS_VERSION_OF,
    /**
     * Describe relation between a version ([NodeLabel.ARTIFACT_VERSION]) of an artifact ([NodeLabel.ARTIFACT]).
     * <pre>
     * (:Artifact) -[:IS_GROUP_OF]-> (:ArtifactGroup)
     * (:ArtifactVersion) -[:IS_GROUP_OF]-> (:ArtifactGroup)
    </pre> *
     */
    IS_GROUP_OF,
    /**
     * Describe relation between a dependency of two versions of artifacts ([NodeLabel.ARTIFACT_VERSION]).
     * <pre>
     * (:ArtifactVersion) -[:DEPEND_ON_ARTIFACT_VERSION]-> (:ArtifactVersion)
    </pre> *
     */
    DEPEND_ON_ARTIFACT_VERSION,
    /**
     * Describe relation between provided class ([NodeLabel.JAVA_CLASS]) of a artifact version ([NodeLabel.ARTIFACT_VERSION]).
     * <pre>
     * (:ArtifactVersion) -[:PROVIDE_JAVA_CLASS]-> (:JavaClass)
    </pre> *
     */
    PROVIDE_JAVA_CLASS,
    /**
     * Describe relation between  class ([NodeLabel.JAVA_CLASS]) of a artifact version ([NodeLabel.ARTIFACT_VERSION]).
     * <pre>
     * (:ArtifactVersion) -[:DEPEND_ON_EXTERNAL_JAVA_CLASS]-> (:JavaClass)
    </pre> *
     */
    DEPEND_ON_EXTERNAL_JAVA_CLASS,

    /**
     * Describe parent child relation between two version of artifacts ([NodeLabel.ARTIFACT_VERSION]).
     * This is ordinary a child artifact to parent POM relation.
     * <pre>
     * (:ArtifactVersion) -[:IS_CHILD_OF_ARTIFACT_VERSION]-> (:ArtifactVersion)
    </pre> *
     */
    IS_CHILD_OF_ARTIFACT_VERSION
}

/**
 * Labels of nodes used in graph-db.
 * @see RelationType
 */
enum class NodeLabel(val key: String) {
    /**
     * [.Artifact] describes an maven artifact.
     * With properties:
     * - `groupId`
     * - `artifactId`
     * - `type`
     */
    ARTIFACT("Artifact"),
    /**
     * [.ArtifactVersion] describes a version of an maven artifact.
     * With properties:
     * - `version`
     */
    ARTIFACT_VERSION("ArtifactVersion"),
    /**
     * [.ArtifactGroup] describes a group of maven artifacts.
     * With properties:
     * - `groupId`
     */
    ARTIFACT_GROUP("ArtifactGroup"),
    /**
     * [.JavaClass] describes a class provided by an maven artifact version.
     * With properties:
     * - `name`
     */
    JAVA_CLASS("JavaClass")
}
