import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension

fun KotlinMultiplatformExtension.AndroidBuild() {
    androidTarget {
        publishLibraryVariantsGroupedByFlavor = true
        publishAllLibraryVariants()

        compilerOptions {
            jvmTarget.set(JvmTarget.JVM_1_8)
        }
    }

    with(this.project.tasks) {
        // TODO: complete build tasks
    }
}