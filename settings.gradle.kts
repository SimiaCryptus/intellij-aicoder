rootProject.name = "intellij-aicoder"

includeBuild("../jo-penai/")
includeBuild("../SkyeNet/")

pluginManagement {
    repositories {
        gradlePluginPortal()
        mavenCentral()
    }
    plugins {
        kotlin("jvm") version "2.0.20" apply false
    }
}
plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version ("0.4.0")
}