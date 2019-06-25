class DependencyChecker(private val cypherExecutor: CypherExecutor) {

    fun checkExistInGraphDb(dependency: Dependency): Boolean {
        val query =
            "MATCH ${QueryUtil.createArtifact(dependency)}<-[:${RelationType.IS_VERSION_OF}]-${QueryUtil.createArtifactVersion(
                dependency,
                "artifactVersion"
            )} RETURN artifactVersion"

        var exists = false
        cypherExecutor.run(query) { result ->
            exists = result.hasNext()
        }

        return exists
    }

}