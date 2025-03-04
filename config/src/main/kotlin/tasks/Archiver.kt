package tasks

import org.gradle.api.DefaultTask
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputDirectory
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.TaskAction
import java.io.File
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

abstract class Archiver : DefaultTask() {
    @InputDirectory
    lateinit var inputDir: File

    @OutputDirectory
    lateinit var outputDir: File

    @Input
    lateinit var zipName: String

    @TaskAction
    fun archive() {
        if (!outputDir.exists()) {
            outputDir.mkdirs()
        }

        val zipFile = File(outputDir, "$zipName.zip")

        ZipOutputStream(zipFile.outputStream()).use { zipOut ->
            inputDir.walkTopDown().forEach { file ->
                val zipEntry = ZipEntry(file.relativeTo(inputDir).path)
                if (file.isDirectory) {
                    zipEntry.isDirectory
                } else {
                    zipOut.putNextEntry(zipEntry)
                    file.inputStream().use { it.copyTo(zipOut) }
                    zipOut.closeEntry()
                }
            }
        }

        println("zip archive created: ${zipFile.absolutePath}")
    }
}
