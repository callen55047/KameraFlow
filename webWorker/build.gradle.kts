plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.kotlinSerialization)

    id("com.egan.plugin.config")
}

version = "6.6.6.6"
group = "com.egan.webworker"

repositories {
    mavenCentral()
}

kotlin {
    js("modelWorker", IR) {
        moduleName = "model-worker"
        binaries.executable()

        browser {
            webpackTask {
                outputFileName = "modelWorker.js"
            }

            distribution {
                name = "modelWorker"
            }
        }
    }

    sourceSets {
        val commonMain by getting {
            dependencies {
                implementation(project(":core"))
            }
        }

        val modelWorkerMain by getting {}
    }
}