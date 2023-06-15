package cappatchino.patcher.utils.proxy

import io.github.classgraph.ClassInfo
import org.objectweb.asm.ClassReader
import org.objectweb.asm.tree.ClassNode

class ClassProxy internal constructor(val info: ClassInfo) {
    internal var resolved = false

    val node: ClassNode by lazy {
        resolved = true

        val reader = ClassReader(info.resource.load())
        val classNode = ClassNode()
        reader.accept(classNode, ClassReader.EXPAND_FRAMES)
        classNode
    }

    override fun toString(): String {
        return "ClassProxy(name=${info.name}, resolved=$resolved)"
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as ClassProxy

        if (info != other.info) return false

        return true
    }

    override fun hashCode(): Int {
        return info.hashCode()
    }
}