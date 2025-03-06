import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.getByType
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension

abstract class MultiplatformConvention : Plugin<Project> {
    override fun apply(target: Project) {
        target.extensions.getByType<KotlinMultiplatformExtension>().apply {
            applyDefaultHierarchyTemplate()

            compilerOptions {
                optIn.addAll(
                    "kotlin.RequiresOptIn",
                    "kotlin.js.ExperimentalJsExport",
                    "kotlin.experimental.ExperimentalExpectActual",
                )
            }

            sourceSets.all {
                languageSettings {
                    optIn("kotlin.experimental.ExperimentalExpectActual")
                }
            }
        }
    }
}