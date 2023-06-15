package cappatchino.patcher.utils

import org.objectweb.asm.tree.AbstractInsnNode
import org.objectweb.asm.tree.InsnList
import org.objectweb.asm.tree.MethodNode

fun InsnList.replace(index: Int, instructions: Collection<AbstractInsnNode>) {
    if (this.size() - index < instructions.size || index < 0) {
        throw IndexOutOfBoundsException("Cannot overwrite index $index with ${instructions.size} instructions")
    }

    for ((idx, insn) in instructions.withIndex()) {
        this.set(this.get(idx + index), insn)
    }
}

fun InsnList.replace(index: Int, instructions: Array<AbstractInsnNode>) {
    replace(index, instructions.toList())
}

fun InsnList.replace(index: Int, insn: AbstractInsnNode) {
    if (index < 0 || index >= this.size()) {
        throw IndexOutOfBoundsException("Cannot overwrite index $index with 1 instruction")
    }
    this.set(this.get(index), insn)
}

fun InsnList.insertBefore(index: Int, instructions: List<AbstractInsnNode>) {
    if (index < 0 || index >= this.size()) {
        throw IndexOutOfBoundsException("Cannot insert before index $index with ${instructions.size} instructions")
    }

    for (insn in instructions) {
        this.insertBefore(this.get(index), insn)
    }
}

fun InsnList.add(instructions: List<AbstractInsnNode>) {
    for (insn in instructions) {
        this.add(insn)
    }
}

fun MethodNode.replaceMethod(instructions: List<AbstractInsnNode>, clearTryCatch: Boolean = true): Unit? {
    if (clearTryCatch) {
        this.tryCatchBlocks?.clear() ?: return null
    }
    this.instructions?.clear() ?: return null
    this.instructions?.add(instructions) ?: return null
    return Unit
}
