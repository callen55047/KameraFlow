
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

include(":config")

include(":core")

include(":app:android")

include(":tools:mockery")
include("app")
include("config")
