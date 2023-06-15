package cappatchino.patcher.utils.proxy

class ProxyClassInfoList(internal val classes: MutableMap<String, ClassProxy>) : Set<ClassProxy> {
    override val size get() = classes.size
    fun contains(className: String): Boolean = classes.containsKey(className)
    override fun contains(element: ClassProxy): Boolean = classes.containsValue(element)
    override fun containsAll(elements: Collection<ClassProxy>): Boolean = classes.values.containsAll(elements)
    override fun isEmpty(): Boolean = classes.isEmpty()
    override fun iterator(): Iterator<ClassProxy> = classes.values.iterator()

    fun addAll(elements: Collection<ClassProxy>) = classes.putAll(elements.map { it.info.name to it })
}