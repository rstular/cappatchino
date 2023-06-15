package cappatchino.patcher.fingerprint

import cappatchino.patcher.patch.BytecodeContext
import cappatchino.patcher.utils.proxy.ClassProxy
import io.github.classgraph.MethodInfo
import org.objectweb.asm.Opcodes
import org.objectweb.asm.Type
import org.objectweb.asm.tree.LabelNode
import org.objectweb.asm.tree.LdcInsnNode
import org.objectweb.asm.tree.LineNumberNode
import org.objectweb.asm.tree.MethodNode

private data class OpcodeSearchState(var startIndex: Int, var ptr: Int)

abstract class MethodFingerprint(
    @Suppress("MemberVisibilityCanBePrivate") val definingClass: String? = null,
    @Suppress("MemberVisibilityCanBePrivate") val name: String? = null,
    @Suppress("MemberVisibilityCanBePrivate") val access: Int? = null,
    @Suppress("MemberVisibilityCanBePrivate") val strings: Set<String>? = null,
    @Suppress("MemberVisibilityCanBePrivate") val parameterTypes: Array<Type>? = null,
    @Suppress("MemberVisibilityCanBePrivate") val returnType: Type? = null,
    @Suppress("MemberVisibilityCanBePrivate") val opcodes: Iterable<Int>? = null,
) : Fingerprint {
    var result: MethodFingerprintScanResult? = null

    companion object {
        private fun MethodFingerprint.resolve(cls: ClassProxy, method: MethodNode): MethodFingerprintScanResult? {
            // Whether the search for strings has been successful
            var stringSearchSuccess = strings.isNullOrEmpty()
            val stringMatchResults = mutableMapOf<String, Int>()

            // List of opcodes that we yet have to find
            val opcodesToFind = opcodes?.toList()
            // States of the opcode search
            val opcodeSearchPointers = mutableSetOf<OpcodeSearchState>()
            // Whether the search for opcodes has been successful
            var opcodeSearchSuccess = opcodesToFind.isNullOrEmpty()
            // Beginning of the opcode search match (only set when a full match is detected)
            var opcodeSearchResult: Pair<Int, Int>? = null

            for ((i, @Suppress("SpellCheckingInspection") insn) in method.instructions.withIndex()) {
                // If the search is over, return the result
                if (stringSearchSuccess && opcodeSearchSuccess) {
                    break
                }

                // If the search is not over, check for strings
                if (!stringSearchSuccess && insn.opcode == Opcodes.LDC) {
                    val ldc = insn as LdcInsnNode
                    // String search success is false only when stringsToFind is not null and not empty
                    if (ldc.cst is String && strings!!.contains(ldc.cst as String)) {
                        stringMatchResults[ldc.cst as String] = i
                        if (stringMatchResults.size == strings.size) {
                            stringSearchSuccess = true
                        }
                    }
                }

                // Check for opcodes
                if (!opcodeSearchSuccess && insn !is LabelNode && insn !is LineNumberNode) {
                    // Update the progress of other search states
                    val searchStatesToRemove = mutableSetOf<OpcodeSearchState>()
                    opcodeSearch@ for (state in opcodeSearchPointers) {
                        // If the instruction matches, we advance the pointer. If the pointer reaches the end, we have a match.
                        if (opcodesToFind!![state.ptr] == insn.opcode) {
                            if (++state.ptr == opcodesToFind.size) {
                                opcodeSearchSuccess = true
                                opcodeSearchResult = Pair(state.startIndex, i)
                                break@opcodeSearch
                            }
                        } else {
                            // If the instruction does not match, we kill the pointer
                            searchStatesToRemove.add(state)
                        }
                    }
                    opcodeSearchPointers.removeAll(searchStatesToRemove)

                    // Check if the first instruction matches. If so, we start tracking the progress.
                    if (opcodesToFind!!.first() == insn.opcode) {
                        opcodeSearchPointers.add(OpcodeSearchState(i, 1))
                    }
                }
            }

            // If the search is over, return the result
            if (stringSearchSuccess && opcodeSearchSuccess) {
                return MethodFingerprintScanResult(
                    cls,
                    method,
                    opcodeSearchResult?.let { MethodFingerprintScanResult.OpcodeInfo(it) },
                    stringMatchResults
                )
            }

            // If the search is over, and we have not found anything, return null
            return null
        }

        private fun MethodFingerprint.resolve(cls: ClassProxy, method: MethodInfo): MethodFingerprintScanResult? {
            val methodNode = cls.node.methods.firstOrNull { it.name == method.name && it.desc == method.typeDescriptorStr }
                ?: return null
            return resolve(cls, methodNode)
        }

        fun MethodFingerprint.resolve(cls: ClassProxy): MethodFingerprintScanResult? {
            // ClassGraph does not expose <clinit> methods, so we have to do it manually using ASM
            this.result = if (this.name == "<clinit>") {
                cls.node.methods.find { it.name == "<clinit>" }?.let {
                    resolve(cls, it)
                }
            } else {
                cls.info.methodAndConstructorInfo.filter {
                    (this.name == null || it.name == this.name) &&
                            (this.access == null || (it.modifiers and this.access) == this.access) &&
                            (this.returnType == null || Type.getReturnType(it.typeDescriptorStr) == this.returnType) &&
                            (this.parameterTypes == null || Type.getArgumentTypes(it.typeDescriptorStr)
                                .contentEquals(this.parameterTypes))
                }.firstNotNullOfOrNull { resolve(cls, it) }
            }
            return this.result
        }

        fun MethodFingerprint.resolve(classes: Iterable<ClassProxy>) {
            this.result = if (definingClass != null) {
                classes.filter { it.info.name == definingClass.replace('/', '.') }.toSet()
            } else {
                classes
            }.firstNotNullOfOrNull { resolve(it) }
        }

        fun Iterable<MethodFingerprint>.resolve(ctx: BytecodeContext) {
            for (fp in this) {
                fp.resolve(ctx.classes)
            }
        }
    }
}

data class MethodFingerprintScanResult(
    val classDef: ClassProxy,
    val methodNode: MethodNode,
    val opcodeInfo: OpcodeInfo?,
    val stringInfo: Map<String, Int>?
) {
    data class OpcodeInfo(val start: Int, val end: Int) {
        constructor(pair: Pair<Int, Int>) : this(pair.first, pair.second)
    }
}
