import environment.ModuleArtifacts

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.kotlinSerialization)
    alias(libs.plugins.androidLibrary)
    alias(libs.plugins.mokkery)

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
            }
        }

        val androidUnitTest by getting {
            dependencies {
                implementation(libs.android.test.core)
                implementation(libs.android.test.espresso)
                implementation(libs.android.test.robolectric)
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
