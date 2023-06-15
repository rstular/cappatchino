package cappatchino.patcher

import cappatchino.patcher.utils.CustomClassWriter
import cappatchino.patcher.utils.proxy.ClassProxy
import cappatchino.patcher.utils.proxy.ProxyClassInfoList
import io.github.classgraph.ClassGraph
import io.github.classgraph.ScanResult
import mu.two.KotlinLogging
import org.objectweb.asm.ClassWriter
import java.io.File
import java.net.URL
import java.net.URLClassLoader
import java.util.jar.JarInputStream
import java.util.jar.JarOutputStream
import java.util.zip.ZipEntry

private val logger = KotlinLogging.logger {}

internal class JarLoader(val jarFile: File) {
    constructor(jarFileURL: URL) : this(File(jarFileURL.toURI()))

    private val loader = URLClassLoader(arrayOf(this.jarFile.toURI().toURL()))

    fun loadAllClasses(): ProxyClassInfoList {
        val classes = ClassGraph()
            .ignoreParentClassLoaders()
            .overrideClassLoaders(loader)
            .ignoreClassVisibility()
            .enableClassInfo()
            .ignoreMethodVisibility()
            .enableMethodInfo()
            .scan()
            .allClassesAsMap

        logger.debug { "Loaded ${classes.size} classes from ${jarFile.absolutePath}" }
        return ProxyClassInfoList(classes.mapValues { ClassProxy(it.value) } as MutableMap<String, ClassProxy>)
    }

    fun saveAll(newJarFile: File, classes: ProxyClassInfoList) {
        logger.info { "Saving patched JAR to ${newJarFile.absolutePath}" }

        val jarIn = JarInputStream(this.jarFile.inputStream())
        val jarOut = JarOutputStream(newJarFile.outputStream())

        // Write all class files
        logger.debug("Writing patched and un-patched classes")
        classes.forEach { proxy ->
            if (proxy.info.resource == null) {
                return@forEach
            }
            try {
                jarOut.putNextEntry(ZipEntry("${proxy.info.name.replace('.', '/')}.class"))
                if (proxy.resolved) {
                    val writer = CustomClassWriter(loader, ClassWriter.COMPUTE_FRAMES)
                    proxy.node.accept(writer)
                    jarOut.write(writer.toByteArray())
                } else {
                    proxy.info.resource.open().use {
                        it.copyTo(jarOut)
                    }
                }
            } catch (e: Exception) {
                logger.error(e) { "Failed to write class ${proxy.info.name}" }
            } finally {
                jarOut.closeEntry()
            }
        }

        // Copy all non-class files
        logger.debug("Copying non-class files")
        do {
            val entry = jarIn.nextEntry?.also {
                val entryName = it.name
                if (!entryName.endsWith(".class") && !it.isDirectory && !entryName.startsWith("META-INF/")) {
                    try {
                        jarOut.putNextEntry(ZipEntry(entryName))
                        jarIn.copyTo(jarOut)
                        logger.trace { "Copied entry $entryName" }
                    } catch (e: Exception) {
                        logger.error(e) { "Failed to copy entry $entryName" }
                    } finally {
                        jarOut.closeEntry()
                        jarIn.closeEntry()
                    }
                }
            }
        } while (entry != null)

        jarIn.close()
        jarOut.close()
        logger.info { "Done!" }
    }

    fun loadAnnotations(): ScanResult {
        return ClassGraph()
            .enableAnnotationInfo()
            .enableClassInfo()
            .ignoreParentClassLoaders()
            .overrideClassLoaders(loader)
            .scan()
    }
}