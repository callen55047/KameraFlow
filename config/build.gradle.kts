
plugins {
    `kotlin-dsl`
    `java-gradle-plugin`
}

group = "com.egan.plugin"

repositories {
    mavenCentral()
    google()
}

dependencies {
    implementation("org.jetbrains.kotlin.multiplatform:org.jetbrains.kotlin.multiplatform.gradle.plugin:2.1.10")
    implementation("com.vanniktech:gradle-maven-publish-plugin:0.30.0")
}

kotlinDslPluginOptions {}

gradlePlugin {
    plugins {
        register("egan-convention") {
            id = "com.egan.plugin.config"
            implementationClass = "MultiplatformConvention"
        }
    }
}