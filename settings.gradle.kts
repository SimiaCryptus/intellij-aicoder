rootProject.name = "intellij-aicoder"

plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version ("0.4.0")
}

includeBuild("../jo-penai/")
includeBuild("../SkyeNet/")
