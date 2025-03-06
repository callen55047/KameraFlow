import environment.IModuleArtifact
import groovyjarjarantlr.Tool.version
import org.gradle.api.GradleException
import org.gradle.api.tasks.Copy
import org.gradle.api.tasks.Exec
import org.gradle.kotlin.dsl.get
import org.gradle.kotlin.dsl.register
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension
import org.jetbrains.kotlin.gradle.targets.js.dsl.KotlinJsTargetDsl

fun KotlinMultiplatformExtension.WebBuild(
    module: IModuleArtifact,
) {
    val config = WebBuildConfiguration(
        releaseVersion = this.project.version as String,
        sdkName = module.webNPMName
    )

    generateWebConventionJsTarget(
        sourceSetName = config.sourceSetName,
        sdkName = config.sdkName,
        releaseVersion = config.releaseVersion,
        applyConfig = config.applySourceSetConfig,
    )

    applyWebConventionTasks(
        module = module,
        sourceSetName = config.sourceSetName,
        releaseOutputDir = config.releaseOutputDir,
        releaseVersion = config.releaseVersion,
        npmAccessToken = config.npmAccessToken,
    )
}

fun KotlinMultiplatformExtension.generateWebConventionJsTarget(
    sourceSetName: String,
    sdkName: String,
    releaseVersion: String,
    applyConfig: KotlinJsTargetDsl.() -> Unit = {},
) = js(sourceSetName, IR) {
    val mModuleName = setOf("egan", "kamera", sdkName).joinToString("-")

    moduleName = mModuleName
    version = releaseVersion

    generateTypeScriptDefinitions()
    binaries.executable()
    binaries.library()

    useCommonJs()
    useEsModules()

    browser {
        commonWebpackConfig {
            outputFileName = "$mModuleName.js"

            cssSupport {
                enabled.set(true)
            }

            scssSupport {
                enabled.set(true)
            }
        }

        webpackTask {
            args.plusAssign(listOf("--env", "sdkVersion='$releaseVersion'"))
            webpackConfigApplier {
                export = false
            }
        }
    }

    compilations["main"].packageJson {
        val packageName = mModuleName.replaceFirst("-", "/")

        customField("name", "@$packageName")
        customField("main", "$mModuleName.js")
        customField("types", "$mModuleName.d.ts")
        customField("license", "SEE LICENSE IN LICENSE.md")
    }

    applyConfig()
}

fun KotlinMultiplatformExtension.applyWebConventionTasks(
    module: IModuleArtifact,
    sourceSetName: String,
    releaseOutputDir: String,
    releaseVersion: String,
    npmAccessToken: String,
) {
    val buildDirectory = project.layout.buildDirectory

    val npmOutputDir = buildDirectory.dir("$releaseOutputDir/npmPackage/files/$releaseVersion")
    val npmExecutable = buildDirectory.dir("kotlin-webpack/$sourceSetName/productionExecutable")
    val typeDefinitions = buildDirectory.dir("compileSync/$sourceSetName/main/productionExecutable/kotlin")
    val packageJson = buildDirectory.dir("tmp/${sourceSetName}PublicPackageJson/")
    val markdownFile = buildDirectory.dir("npmFiles")
    val licenseFile = project.rootDir.resolve("../config/src/main/kotlin/staticFiles/LICENSE.md")

    with(project.tasks) {
        val buildWebPlatformFilesTask =
            register("build${module.name}WebFiles") {
                group = "kamera"
                description = "Task to build the compiled js codes from a kotlin multiplatform project. " +
                        "Note that this task utilizing the built in multiplatform plugin BrowserProductionWebpack task."

                val buildTaskName = "${sourceSetName}BrowserProductionWebpack"
                dependsOn(
                    this.project.provider {
                        this.project.tasks.findByName(buildTaskName)
                            ?: throw GradleException("Task '$buildTaskName' for buildWebPlatformFiles not found.")
                    },
                )
            }

        val prepareNpmPackageTask =
            register<Copy>("prepare${module.name}WebNPMPackage") {
                group = "kamera"
                description = "Prepares the files created from the buildWebPlatformFiles task for npm packaging."

                val outputDir = npmOutputDir.get().asFile

                from(npmExecutable) {
                    include("**/*.js")
                }

                from(typeDefinitions) {
                    include("**/*.d.ts")
                }

                from(packageJson) {
                    include("**/package.json")
                }

                from(markdownFile) {
                    include("**/*.md")
                }

                from(licenseFile)

                into(outputDir)
            }

        val npmPackTask =
            register<Exec>("pack${module.name}WebNPM") {
                group = "kamera"
                description = "Packages the web files into a .tgz archive using npm pack."

                val packageDir =
                    buildDirectory.dir(
                        "$releaseOutputDir/npmPackage/files/$releaseVersion",
                    ).get().asFile
                val outputTgzDir = buildDirectory.dir("$releaseOutputDir/npmPackage/tgz").get().asFile

                workingDir = packageDir
                commandLine = listOf("npm", "pack")

                doLast {
                    val generatedTgz =
                        packageDir.listFiles { _, name ->
                            name.contains(releaseVersion) && name.endsWith(".tgz")
                        }?.firstOrNull()

                    if (generatedTgz != null) {
                        outputTgzDir.mkdirs()
                        val moved = generatedTgz.renameTo(outputTgzDir.resolve(generatedTgz.name))
                        if (moved) {
                            logger.lifecycle(".tgz package moved to ${outputTgzDir.absolutePath}/${generatedTgz.name}")
                        } else {
                            logger.error("Failed to move the .tgz file.")
                        }
                    } else {
                        logger.warn("No .tgz file matching version '$releaseVersion' found after npm pack.")
                    }
                }
            }

        register("build${module.name}WebArtifact") {
            group = "kamera"
            description = "Builds Web Artifact for NPM release."

            dependsOn(buildWebPlatformFilesTask)
            finalizedBy(prepareNpmPackageTask, npmPackTask)
        }

        register("publish${module.name}WebNPM") {
            group = "kamera"
            description = "Publish the bundled Web .tgz file to npm production."

            doLast {
                val tgzDir =
                    this.project.layout.buildDirectory
                        .dir("$releaseOutputDir/npmPackage/tgz")
                        .get().asFile

                val tarGzFile =
                    tgzDir.listFiles { _, name ->
                        name.contains(releaseVersion) && name.endsWith(".tgz")
                    }?.firstOrNull() ?: throw IllegalStateException(
                        "No .tgz file matching version '$releaseVersion' found in ${tgzDir.absolutePath}. " +
                                "Ensure the npmPack task was successful.",
                    )

                logger.lifecycle("Found package to publish: ${tarGzFile.name}")

                if (npmAccessToken.isEmpty()) {
                    throw IllegalArgumentException("NPM access token is not provided in the extension configuration.")
                }

                this.project.exec {
                    commandLine(
                        "npm",
                        "publish",
                        tarGzFile.absolutePath,
                        "--//registry.npmjs.org/:_authToken=$npmAccessToken",
                    )
                }

                logger.lifecycle("Successfully published ${tarGzFile.name} to npm.")
            }
        }
    }
}

data class WebBuildConfiguration(
    val releaseOutputDir: String = "release/web",
    val sourceSetName: String = "js",
    val releaseVersion: String = "6.6.6",
    val sdkName: String = "capture",
    val npmAccessToken: String = "",
    val applySourceSetConfig: KotlinJsTargetDsl.() -> Unit = {
        browser {
            testTask {
                useKarma {
                    useChromeHeadless()
                }
            }
        }
    },
)