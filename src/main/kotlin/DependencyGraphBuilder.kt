class DependencyGraphBuilder(
    private val dependencyVisitor: DependencyVisitor,
    private val dependencyAppenders: List<DependencyAppender>
) {

    fun build(dependency: Dependency) {
        val dependencies = mutableSetOf<Dependency>()

        dependencyVisitor.visit(dependency) {
            dependencies.add(it)
        }

        println("Dependencies:")
        dependencies.forEach { println(it) }
        println("---------------------")

        dependencies.forEach { dependentDependency ->
            dependencyAppenders.forEach { dependencyAppender ->
                if (dependencyAppender.applicable(dependentDependency)) {
                    dependencyAppender.append(dependentDependency)
                }
            }

        }
    }

    fun createIndices(): List<String> = QueryUtil.createIndices()
}