package cocoapods

import environment.IModuleArtifact
import org.gradle.api.DefaultTask
import org.gradle.api.tasks.Input
import tasks.CommandLineRunner.cmdline
import java.io.File

abstract class CocoapodsPublish : DefaultTask() {
    @Input
    lateinit var module: IModuleArtifact

    private val podspecFile: File
        get() =
            File(
                project.layout.buildDirectory.dir("cocoapods").get().asFile,
                "${module.name}.podspec",
            )

    fun clearCaches() {
        File("~/Library/Caches/Cocoapods").deleteRecursively()
        File("~/Library/Developer/Xcode/DerivedData").deleteRecursively()
    }

    fun validatePodspecFile() {
        "pod spec lint ${podspecFile.path} --allow-warnings".cmdline()
    }

    fun syncWithS3() {
        // TODO: implement
    }

    fun deployToCocoapods() {
        "pod spec lint ${podspecFile.path} --allow-warnings".cmdline()
    }
}
