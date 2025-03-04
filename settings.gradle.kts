
pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
        maven("https://jitpack.io")

        maven {
            url = uri("https://plugins.gradle.org/m2/")
        }
    }
}

dependencyResolutionManagement {
    repositories {
        google()
        mavenCentral()
    }

    versionCatalogs {
        create("libs") {
            from(files("./gradle/common.versions.toml"))
        }
    }
}

include(":core")

include(":app:android")

include(":sdk:capture")

include(":tools:mockery")
include("app")
