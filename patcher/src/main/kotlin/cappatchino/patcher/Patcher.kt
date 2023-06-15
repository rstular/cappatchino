package cappatchino.patcher

import cappatchino.patcher.annotations.DependsOn
import cappatchino.patcher.annotations.Patch
import cappatchino.patcher.fingerprint.MethodFingerprint.Companion.resolve
import cappatchino.patcher.patch.*
import cappatchino.patcher.utils.Graph
import cappatchino.patcher.utils.proxy.ClassProxy
import cappatchino.patcher.utils.proxy.ProxyClassInfoList
import io.github.classgraph.AnnotationClassRef
import mu.two.KotlinLogging
import java.io.File
import java.net.URL

private val logger = KotlinLogging.logger {}

class Patcher(jarFile: File) {
    private val classes: ProxyClassInfoList
    private val loader: JarLoader = JarLoader(jarFile)

    init {
        classes = loader.loadAllClasses()
    }

    constructor(jarFilePath: String) : this(File(jarFilePath))

    fun patch(patchJarFile: File) = patch(patchJarFile.toURI().toURL())
    fun patch(patchJarFilePath: String) = patch(File(patchJarFilePath))
    fun patch(patchesURL: URL) {
        val patchDataScan = JarLoader(patchesURL).loadAnnotations()

        val patchClasses = patchDataScan.getClassesWithAnnotation(Patch::class.qualifiedName).asMap()
        logger.debug { "Loaded ${patchClasses.size} patches" }

        val patchGraph = Graph(patchClasses.keys)
        patchClasses.forEach { (className, classInfo) ->
            run {
                if (classInfo.hasAnnotation(DependsOn::class.java)) {
                    val deps = classInfo.getAnnotationInfo(DependsOn::class.java)
                        .parameterValues["dependencies"]?.value
                    if (deps !is Array<*>) {
                        throw IllegalStateException("Invalid annotation value for $className")
                    }
                    deps.forEach { dep ->
                        if (dep !is AnnotationClassRef) {
                            throw IllegalStateException("Invalid annotation value for $className")
                        }
                        patchGraph.addEdge(dep.name, className)
                    }
                }
            }
        }

        patchGraph.topologicalOrder()?.forEach { patchName ->
            logger.info { "Applying patch $patchName" }
            val patch = patchClasses[patchName]!!.loadClass().getConstructor().newInstance() as PatchInterface<Context>
            val ctx = BytecodeContext(this.classes)

            if (patch is BytecodePatch) {
                patch.fingerprints.resolve(ctx)
                val patchResult = patch.execute(ctx)
                if (patchResult is PatchFailure) {
                    // TODO: Remove children and apply as much as possible
                    throw RuntimeException("Patch $patchName failed to apply: ${patchResult.reason}.")
                }
            } else {
                throw IllegalStateException("Patch $patchName is of unknown type")
            }
        } ?: throw IllegalStateException("Patches contain a circular dependency")

        loader.saveAll(File(loader.jarFile.parentFile, "patched-${loader.jarFile.name}"), classes)
    }
}

