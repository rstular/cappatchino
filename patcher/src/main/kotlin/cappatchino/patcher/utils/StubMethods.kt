package cappatchino.patcher.utils

import org.objectweb.asm.Opcodes
import org.objectweb.asm.tree.AbstractInsnNode
import org.objectweb.asm.tree.InsnNode

object StubMethods {
    fun returnVoid(): List<AbstractInsnNode> = listOf(
        InsnNode(Opcodes.RETURN)
    )

    fun returnBool(value: Boolean) = listOf(
        InsnNode(if (value) Opcodes.ICONST_1 else Opcodes.ICONST_0),
        InsnNode(Opcodes.IRETURN)
    )

    fun returnNull() = listOf(
        InsnNode(Opcodes.ACONST_NULL),
        InsnNode(Opcodes.ARETURN)
    )
}