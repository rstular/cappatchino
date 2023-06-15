package cappatchino.patcher.annotations

import cappatchino.patcher.patch.Context
import cappatchino.patcher.patch.PatchInterface
import kotlin.reflect.KClass

@Target(AnnotationTarget.CLASS)
annotation class DependsOn(
    val dependencies: Array<KClass<out PatchInterface<Context>>> = []
)
