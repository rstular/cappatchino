package cappatchino.patcher.patch

import cappatchino.patcher.fingerprint.MethodFingerprint
import java.io.Closeable

sealed interface PatchInterface<out T : Context> : Closeable {
    fun execute(ctx: @UnsafeVariance T): PatchResult

    override fun close() {}
}

abstract class BytecodePatch(
    internal val fingerprints: Iterable<MethodFingerprint> = listOf()
) : PatchInterface<BytecodeContext>

sealed interface PatchResult {
    companion object {
        val success: PatchResult get() = PatchSuccess

        fun failure(reason: String): PatchResult = PatchFailure(reason)
    }
}

object PatchSuccess : PatchResult
class PatchFailure(val reason: String) : PatchResult {
    constructor() : this("Patch application failed")
}
