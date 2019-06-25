import org.apache.maven.cli.MavenCli
import org.apache.maven.model.Model
import org.apache.maven.model.io.xpp3.MavenXpp3Reader
import org.jboss.shrinkwrap.resolver.api.NoResolvedResultException
import org.jboss.shrinkwrap.resolver.api.maven.*
import java.io.BufferedReader
import java.io.File
import java.io.FileNotFoundException
import java.io.FileReader
import java.lang.IllegalArgumentException
import java.lang.IllegalStateException


private fun MavenArtifactInfo.toDependency(): Dependency =
    Dependency(
        coordinate.groupId,
        coordinate.artifactId,
        resolvedVersion,
        coordinate.type.extension,
        coordinate.classifier ?: ""
    )

private fun org.apache.maven.model.Dependency.toDependency(): Dependency =
    Dependency(
        groupId,
        artifactId,
        version,
        type,
        classifier ?: ""
    )

class MavenAdapter(private val workdir: String) {


    private fun initResolver(): ConfigurableMavenResolverSystem {
        val resolver = Maven.configureResolver()

        resolver.fromFile("$workdir/maven-settings.xml")

        return resolver
    }

    fun downloadArtifact(dependency: Dependency): File {
        return readArtifact(dependency).asFile()
    }

    private fun readArtifact(dependency: Dependency): MavenResolvedArtifact {
        try {
            return initResolver().resolve("${dependency.groupId}:${dependency.artifactId}:${dependency.type}:${dependency.version}")
                .withoutTransitivity()
                //.using(AcceptScopesStrategy(ScopeType.COMPILE, ScopeType.PROVIDED, ScopeType.RUNTIME, ScopeType.SYSTEM))
                .asSingleResolvedArtifact()
        } catch (e: NoResolvedResultException) {
            println("could not readArtifact '$dependency': ${e.message}")
            throw e
        }
    }

    fun readArtifactDependencies(dependency: Dependency): List<Dependency> {

        if (dependency.type == "ear") {
            val effectiveMavenModel = createEffectiveMavenModel(dependency)

            val resolvedDependencyLookup = effectiveMavenModel.dependencies?.map {
                val dependencyOfDependency = it.toDependency()
                dependencyOfDependency.mainKey() to dependencyOfDependency
            }?.toMap() ?: mapOf()

            return effectiveMavenModel.dependencies?.map {
                resolvedDependencyLookup[it.toDependency().mainKey()]
                    ?: throw IllegalStateException("effective pom does not contain direct dependency ${it.toDependency()}.")
            }?.toList() ?: listOf()
        }

        val artifact = readArtifact(dependency)

        return artifact.dependencies?.map {
            it.toDependency()
        }?.toList() ?: listOf()
    }

    private fun pomToModel(pomFile: File): Model {
        val inputStream = BufferedReader(FileReader(pomFile))
        return inputStream.use {
            val reader = MavenXpp3Reader()
            reader.read(it)
        }
    }


    fun readParentArtifact(dependency: Dependency): Dependency? {
        val model = readMavenModel(dependency)

        if (model.parent == null) return null

        return Dependency(model.parent.groupId, model.parent.artifactId, model.parent.version, "pom", "")
    }

    private fun readMavenModel(dependency: Dependency): Model {
        // TODO m2-folder parameter
        val pomFile = File(
            File("$workdir/m2"),
            "${dependency.groupId.replace(
                '.',
                '/'
            )}/${dependency.artifactId}/${dependency.version}/${dependency.artifactId}-${dependency.version}.pom"
        )

        if (!pomFile.exists()) {
            throw IllegalStateException("Missing pom file: $pomFile.")
        }

        try {
            return pomToModel(pomFile)
        } catch (e: FileNotFoundException) {
            throw IllegalArgumentException("pom file: '$pomFile' for dependency: '$dependency' not found.", e)
        }
    }


    private fun createEffectiveMavenModel(coordinate: Dependency): Model {
        val fileName = "effectivepom.xml"
        val file = "$workdir/$fileName"

        System.setProperty("maven.multiModuleProjectDirectory", workdir)

        MavenCli().doMain(
            arrayOf(
                "help:effective-pom",
                // "-X",
                "-s$workdir/maven-settings.xml",
                "-Dartifact=${coordinate.groupId}:${coordinate.artifactId}:${coordinate.version}",
                "-Doutput=$file"
            ), workdir, System.out, System.err
        )

        try {
            val model = FileReader(file).use {
                MavenXpp3Reader().read(it)
            }

            File(file).delete()

            return model
        } catch (e: Exception) {
            val message = "Error in '$file': ${e.message}"
            throw IllegalArgumentException(message, e)
        }
    }

}

