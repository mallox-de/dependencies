import java.nio.file.Paths
import kotlin.system.exitProcess
import com.hubspot.jinjava.Jinjava
import java.io.File

fun main() {

    val workdir = "${Paths.get(".").toAbsolutePath().normalize()}/workingdir"

    Initializer.initWorkingDir(workdir)

    val mavenAdapter = MavenAdapter(workdir)
    val cypherExecutor = EmbeddedCypherExecutor(workdir)
    startNeo4j(cypherExecutor)

    println("Start")

    val dependencyChecker = DependencyChecker(cypherExecutor)
    val mavenParentResolver = MavenParentResolver(mavenAdapter)
    val builder = DependencyGraphBuilder(
        DependencyVisitor(
            dependencyChecker,
            listOf(MavenDependencyResolver(mavenAdapter), mavenParentResolver)
        ),
        listOf(
            ClassDependencyAppender(mavenAdapter, cypherExecutor,  workdir),
            MavenDependencyAppender(MavenDependencyResolver(mavenAdapter), cypherExecutor),
            ParentDependencyAppender(mavenParentResolver, cypherExecutor)
        )
    )

    println("create index")

    builder.createIndices().forEach { query ->
        println("create index for $query")
        cypherExecutor.run(query) {}
    }

    println("create dependencies")

    // val dependency = Dependency("com.charlyghislain.authenticator", "example-ear", "1.0.6", "ear")
    val dependency = Dependency("net.sourceforge.htmlunit", "htmlunit", "2.35.0")
    // val dependency = Dependency("org.apache.commons", "commons-text", "1.6", "jar")
    builder.build(dependency)

    println("end")

    exitProcess(0)
}

class Initializer {
    companion object {
        fun initWorkingDir(workingDir: String) {
            File(workingDir).mkdirs()

            val jinjava = Jinjava()
            val context = mapOf<String, Any>("workingDir" to workingDir, "mavenHome" to "$workingDir/m2")

            // pom.xml:
            val pomResource = Initializer::class.java.getResource("pom.xml.j2")
            val pom = jinjava.render(pomResource.readText(Charsets.UTF_8), context)
            val pomFile = File("$workingDir/pom.xml")
            if (!pomFile.exists()) pomFile.writeText(pom)

            // settings.xml:
            val settingsResource = Initializer::class.java.getResource("maven-settings.xml.j2")
            val settings = jinjava.render(settingsResource.readText(Charsets.UTF_8), context)
            val settingsFile = File("$workingDir/maven-settings.xml")
            if (!settingsFile.exists()) settingsFile.writeText(settings)
        }
    }
}
