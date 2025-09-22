plugins {
    alias(libs.plugins.kotlinMultiplatform) apply false
    alias(libs.plugins.androidLibrary) apply false
    alias(libs.plugins.kotlinSerialization) apply false
    alias(libs.plugins.kotlinAndroid) apply false

    alias(libs.plugins.skie) apply false
    alias(libs.plugins.grease) apply false
}

buildscript {
    repositories {
        gradlePluginPortal()
    }

    dependencies {
        classpath(libs.moko.resources.generator)
    }
}

allprojects {
    repositories {
        google()
        mavenCentral()
    }
}