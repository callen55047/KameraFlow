import dev.icerock.gradle.MRVisibility
import environment.ModuleArtifacts

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.kotlinSerialization)
    alias(libs.plugins.androidLibrary)
    alias(libs.plugins.mokkery)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.composeCompiler)
    alias(libs.plugins.moko.resources)

    id("com.egan.plugin.config")
}

version = "1.1.1"
group = "com.egan.core"

val moduleArtifact = ModuleArtifacts.Core

kotlin {

    AndroidBuild()
    IosBuild(moduleArtifact)
    WebBuild(moduleArtifact)

    sourceSets {
        val commonMain by getting {
            dependencies {
                implementation(libs.kotlinx.coroutines)
                implementation(libs.kotlinx.serialization)
                implementation(libs.kotlinx.serialization.protobuf)
                implementation(libs.kotlinx.datetime)
                implementation(libs.ktor.core)
                implementation(libs.ktor.encoding)
                implementation(libs.ktor.logging)
                implementation(libs.ktor.serialization.json)
                implementation(libs.ktor.content.negotiation)
                implementation(compose.runtime)
                implementation(compose.foundation)
                implementation(compose.material)
                implementation(compose.ui)
                implementation(compose.components.resources)
                implementation(compose.components.uiToolingPreview)
                implementation(libs.compose.lifecycle.viewmodel)
            }
        }

        val commonTest by getting {
            dependencies {
                implementation(libs.kotlin.test)
                implementation(libs.test.kotlinx.coroutines)
                implementation(libs.ktor.mocking)
                implementation(libs.ktor.server)
                implementation(libs.ktor.server.cio)
                implementation(libs.ktor.server.tests)
            }
        }

        val androidMain by getting {
            dependencies {
                implementation(libs.ktor.client.android)
                implementation(libs.android.initializer)
                implementation(compose.preview)
                implementation(libs.androidx.activity.compose)
            }
        }

        val androidUnitTest by getting {
            dependencies {
                implementation(libs.android.test.core)
                implementation(libs.android.test.junit)
                implementation(libs.android.test.espresso)
                implementation(libs.android.test.robolectric)
                implementation(libs.android.test.mockito.android)
            }
        }

        val jsMain by getting {
            dependencies {
                implementation(libs.kotlinx.html)
                implementation(npm("hash.js", "1.1.7"))
            }
        }

        val jsTest by getting {
            dependencies {
                implementation(kotlin("test-js"))
            }
        }

        val iosMain by getting {
            dependencies {
                implementation(libs.ktor.client.ios)
            }
        }
    }
}

android {
    namespace = group.toString()
    compileSdk = libs.versions.android.compileSdk.get().toInt()
    defaultConfig {
        minSdk = libs.versions.android.minSdk.get().toInt()
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }
}

dependencies {
    debugImplementation(compose.uiTooling)
    commonMainApi(libs.moko.resources)
    commonMainApi(libs.moko.resources.compose)
    commonTestImplementation(libs.moko.resources.test)
}

multiplatformResources {
    resourcesPackage.set("com.egan.core")
    resourcesVisibility.set(MRVisibility.Internal)
}