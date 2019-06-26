import java.util.*

class DependencyVisitor(
    private val dependencyChecker: DependencyChecker,
    private val dependencyResolvers: List<DependencyResolver>
) {
    private val callbackMap = mutableMapOf<Dependency, CallbackStack>()
    // TODO check if chache is at the right place -> wrapper with cache?
    private val checkExistInGraphDbCache = mutableSetOf<Dependency>()

    fun visit(dependency: Dependency, callback: (dependency: Dependency) -> Unit) {

        if (callbackMap.contains(dependency)) {
            println("add callback to stack for $dependency.")
            callbackMap[dependency]!!.push(callback)
            return
        }

        if (!checkExistInGraphDbCache.contains(dependency) && !dependencyChecker.checkExistInGraphDb(dependency)) {

            println("analyse $dependency")
            callbackMap[dependency] = CallbackStack(dependency).apply {
                push(callback)
            }

            dependencyResolvers.filter { it.applicable(dependency) }.forEach { dependencyResolver ->
                val dependentDependencies = dependencyResolver.resolve(dependency)

                dependentDependencies.forEach {
                    visit(it, callback)
                }
            }

            callbackMap[dependency]!!.processCallbacks()
        } else {
            println("found $dependency in database.")
            checkExistInGraphDbCache.add(dependency)
        }

    }

}

class CallbackStack(private val dependency: Dependency) {
    private val stack = Stack<(dependency: Dependency) -> Unit>()
    private var isDependencyProcessed = false

    fun push(callback: (dependency: Dependency) -> Unit) {
        if (isDependencyProcessed) callback.invoke(dependency)
        else stack.push(callback)
    }

    fun processCallbacks() {
        isDependencyProcessed = true
        stack.stream().forEach { it.invoke(dependency) }
    }
}