import cocoapods.BuildPodSpec
import cocoapods.CocoapodsPublish
import environment.IModuleArtifact
import org.gradle.api.tasks.Copy
import org.gradle.kotlin.dsl.register
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension
import org.jetbrains.kotlin.gradle.plugin.mpp.apple.XCFramework
import tasks.Archiver

fun KotlinMultiplatformExtension.IosBuild(module: IModuleArtifact) {
    val xcFramework = this.project.XCFramework(module.name)
    listOf(
        iosArm64(),
        iosSimulatorArm64(),
    ).forEach {
        it.compilerOptions {
            optIn.addAll(
                "kotlin.RequiresOptIn",
                "kotlin.experimental.ExperimentalExpectActual",
            )
        }
        it.binaries.framework {
            baseName = module.name
            xcFramework.add(this)
        }
    }

    with(this.project.tasks) {
        register("build${module.name}IosArtifact") {
            group = "kamera"
            description = "builds final ios artifact ready for deployment."

            dependsOn("assemble${module.name}ReleaseXCFramework")
        }

        register<Copy>("copy${module.name}Resources") {
            group = "kamera"
            description = "copies ios artifact for ${module.name} to podspec build location."

            dependsOn("build${module.name}IosArtifact")

            into(project.layout.buildDirectory.dir("cocoapods"))

            from(project.layout.buildDirectory.dir("XCFrameworks/release")) {
                into("frameworks")
            }

            from(project.rootDir.resolve("../config/src/main/kotlin/staticFiles/LICENSE.md"))
        }

        register<BuildPodSpec>("build${module.name}Podspec") {
            group = "kamera"
            description = "builds podspec file for ${module.name}."

            dependsOn("copy${module.name}Resources")

            this.url = "https://s3-bucket/ios"
            this.module = module
            this.version = "6.6.6"
            this.outputDir = project.layout.buildDirectory.dir("cocoapods").get().asFile
        }

        register<Archiver>("archive${module.name}") {
            group = "kamera"
            description = "archives ios artifact for ${module.name} for cloud upload."

            dependsOn("copy${module.name}Resources")

            val cocoapodsFile = project.layout.buildDirectory.dir("cocoapods").get().asFile
            inputDir = cocoapodsFile.resolve("frameworks")
            outputDir = cocoapodsFile
            zipName = module.name
        }

        register("prepare${module.name}ForDeployment") {
            group = "kamera"
            description = "builds, copies and deploys ${module.name} ios artifact to cocoapods directory."

            dependsOn(
                "copy${module.name}Resources",
                "build${module.name}Podspec",
            )

            finalizedBy(
                "archive${module.name}",
            )
        }

        // TODO: continue on publishing
        register<CocoapodsPublish>("publish${module.name}IosCocoapods") {
            group = "kamera"
            description = "builds, copies and deploys ${module.name} ios artifact to cocoapods trunk."

            dependsOn("prepare${module.name}ForDeployment")

            this.module = module

//            doFirst {
//                syncWithS3()
//                validatePodspecFile()
//                deployToCocoapods()
//            }
        }
    }
}
