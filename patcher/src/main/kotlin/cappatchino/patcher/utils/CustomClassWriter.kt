package cappatchino.patcher.utils

import org.objectweb.asm.ClassReader
import org.objectweb.asm.ClassWriter

class CustomClassWriter(private val loader: ClassLoader, reader: ClassReader?, flags: Int) : ClassWriter(reader, flags) {
    constructor(loader: ClassLoader, flags: Int) : this(loader, null, flags)

    override fun getClassLoader(): ClassLoader {
        return loader
    }
}