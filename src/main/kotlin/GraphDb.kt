import org.neo4j.graphdb.Result
import org.neo4j.graphdb.factory.GraphDatabaseFactory
import java.io.File

fun startNeo4j(cypherExecutor: CypherExecutor) {
    registerShutdownHook(cypherExecutor)
}

private fun registerShutdownHook(cypherExecutor: CypherExecutor) {
    // Registers a shutdown hook for the Neo4j instance so that it
    // shuts down nicely when the VM exits (even if you "Ctrl-C" the
    // running application).
    Runtime.getRuntime().addShutdownHook(object : Thread() {
        override fun run() {
            cypherExecutor.close()
        }
    })
}

interface CypherExecutor {
    fun run(query: String,  callback: ((result: Result) -> Unit)? = null)

    fun close()
}

class EmbeddedCypherExecutor(workdir: String) : CypherExecutor {
    private val graphDb = GraphDatabaseFactory().newEmbeddedDatabase(File("$workdir/databases/graph.db"))!!

    override fun run(query: String, callback: ((result: Result) -> Unit)?) {

        println("------------------------------------------------------")
        println("Query: $query")
        println("------------------------------------------------------")
        val result = graphDb.beginTx().use {
            val result = graphDb.execute(query) ?: throw IllegalStateException("result is null")
            // Caution: maybe necessary to ensure data will be written:
            println(result.resultAsString())
            it.success()
            result
        }
        callback?.invoke(result)
        println("------------------------------------------------------")

    }

    override fun close() {
        graphDb.shutdown()
    }
}
