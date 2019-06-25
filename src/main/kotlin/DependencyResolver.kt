interface DependencyResolver {
    fun applicable(dependency: Dependency): Boolean
    fun resolve(dependency: Dependency): List<Dependency>
}

class MavenDependencyResolver(private val mavenAdapter: MavenAdapter) : DependencyResolver {
    override fun applicable(dependency: Dependency): Boolean = listOf("jar", "war", "ear").contains(dependency.type)

    override fun resolve(dependency: Dependency): List<Dependency> {
        return mavenAdapter.readArtifactDependencies(dependency)
    }
}

class MavenParentResolver(private val mavenAdapter: MavenAdapter) : DependencyResolver {
    override fun applicable(dependency: Dependency): Boolean = true

    override fun resolve(dependency: Dependency): List<Dependency> {
        val parentDependency = mavenAdapter.readParentArtifact(dependency) ?: return emptyList()
        return listOf(parentDependency)
    }
}
