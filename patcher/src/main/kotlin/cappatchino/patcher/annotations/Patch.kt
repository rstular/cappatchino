package cappatchino.patcher.annotations

@Target(AnnotationTarget.CLASS)
annotation class Patch(val name: String, val visible: Boolean = true)
