﻿import org.jetbrains.changelog.Changelog
import org.jetbrains.changelog.markdownToHTML

fun properties(key: String) = providers.gradleProperty(key).get()
fun environment(key: String) = providers.environmentVariable(key).get()

plugins {
    id("java") // Java support
    id("groovy")
    id("org.jetbrains.kotlin.jvm") version "1.9.20"
    id("org.jetbrains.intellij") version "1.16.0"
    id("org.jetbrains.changelog") version "2.2.0"
    id("org.jetbrains.qodana") version "2023.2.1"
    id("org.jetbrains.kotlinx.kover") version "0.7.4"
}

group = "com.github.simiacryptus"
version = properties("pluginVersion")

repositories {
    mavenCentral()
    maven(url = "https://packages.jetbrains.team/maven/p/ij/intellij-dependencies")
}

val kotlin_version = "1.9.20"
val jetty_version = "11.0.18"
val slf4j_version = "2.0.9"
val skyenet_version = "1.0.40"
dependencies {

    implementation(group = "com.simiacryptus", name = "jo-penai", version = "1.0.36")

    implementation(group = "com.simiacryptus.skyenet", name = "core", version = skyenet_version)
    implementation(group = "com.simiacryptus.skyenet", name = "kotlin", version = skyenet_version)
    implementation(group = "com.simiacryptus.skyenet", name = "webui", version = skyenet_version)

    implementation(group = "org.apache.httpcomponents.client5", name = "httpclient5", version = "5.2.1")
    implementation(group = "org.eclipse.jetty", name = "jetty-server", version = jetty_version)
    implementation(group = "org.eclipse.jetty", name = "jetty-servlet", version = jetty_version)
    implementation(group = "org.eclipse.jetty", name = "jetty-annotations", version = jetty_version)
    implementation(group = "org.eclipse.jetty.websocket", name = "websocket-jetty-server", version = jetty_version)
    implementation(group = "org.eclipse.jetty.websocket", name = "websocket-jetty-client", version = jetty_version)
    implementation(group = "org.eclipse.jetty.websocket", name = "websocket-servlet", version = jetty_version)

    implementation("org.codehaus.groovy:groovy-all:3.0.13")


//    implementation(kotlin("compiler-embeddable"))
//    implementation(kotlin("scripting-compiler-embeddable"))
//    implementation(kotlin("script-util"))

    implementation(kotlin("stdlib"))
    implementation(kotlin("reflect"))

    implementation(group = "org.slf4j", name = "slf4j-api", version = slf4j_version)
    testImplementation(group = "org.slf4j", name = "slf4j-simple", version = slf4j_version)

    testImplementation(kotlin("script-runtime"))

    testImplementation(group = "com.intellij.remoterobot", name = "remote-robot", version = "0.11.16")
    testImplementation(group = "com.intellij.remoterobot", name = "remote-fixtures", version = "0.11.16")
    testImplementation(group = "com.squareup.okhttp3", name = "okhttp", version = "3.14.9")


}


tasks.register<Copy>("copyGroovySourcesToResources") {
    from("src/main/groovy")
    into("src/main/resources/sources/groovy")
}
tasks.named("processResources") {
    dependsOn("copyGroovySourcesToResources")
}

kotlin {
    jvmToolchain(17)
}

tasks {
    compileKotlin {
        kotlinOptions {
            javaParameters = true
            jvmTarget = "17"
        }
    }

    compileGroovy {
        dependsOn += compileKotlin
        classpath += files(compileKotlin.get().destinationDirectory)
        groovyOptions.isParameters = true
    }

    compileTestKotlin {
        kotlinOptions {
            javaParameters = true
        }
    }

    wrapper {
        gradleVersion = properties("gradleVersion")
    }

    patchPluginXml {
        version.set(properties("pluginVersion"))
        sinceBuild.set(properties("pluginSinceBuild"))
        untilBuild.set(properties("pluginUntilBuild"))

        pluginDescription.set(
            file("README.md").readText().lines().run {
                val start = "<!-- Plugin description -->"
                val end = "<!-- Plugin description end -->"

                if (!containsAll(listOf(start, end))) {
                    throw GradleException("Plugin description section not found in README.md:\n$start ... $end")
                }
                subList(indexOf(start) + 1, indexOf(end))
            }.joinToString("\n").let { markdownToHTML(it) }
        )

        changeNotes.set(provider {
            with(changelog) {
                renderItem(
                    getOrNull(properties("pluginVersion")) ?: getLatest(),
                    Changelog.OutputType.HTML,
                )
            }
        })
    }

    runIde {
        maxHeapSize = "8g"
    }

    runIdeForUiTests {
        systemProperty("robot-server.port", "8082")
        systemProperty("ide.mac.message.dialogs.as.sheets", "false")
        systemProperty("jb.privacy.policy.text", "<!--999.999-->")
        systemProperty("jb.consents.confirmation.enabled", "false")
        jvmArgs("-Xmx8G")
    }

    signPlugin {
        certificateChain.set(System.getenv("CERTIFICATE_CHAIN"))
        privateKey.set(System.getenv("PRIVATE_KEY"))
        password.set(System.getenv("PRIVATE_KEY_PASSWORD"))
    }

    publishPlugin {
        dependsOn("patchChangelog")
        token.set(System.getenv("PUBLISH_TOKEN"))
        channels.set(listOf(properties("pluginVersion").split('-').getOrElse(1) { "default" }.split('.').first()))
    }
}

intellij {
    pluginName.set(properties("pluginName"))
    version.set(properties("platformVersion"))
    type.set(properties("platformType"))
    plugins.set(properties("platformPlugins").split(',').map(String::trim).filter(String::isNotEmpty))
}

changelog {
    groups.set(emptyList())
    repositoryUrl.set(properties("pluginRepositoryUrl"))
}

qodana {
    cachePath.set(file(".qodana").canonicalPath)
//    reportPath.set(file("build/reports/inspections").canonicalPath)
//    saveReport.set(true)
//    showReport.set(System.getenv("QODANA_SHOW_REPORT")?.toBoolean() ?: false)
}

//kover.xmlReport {
//    onCheck.set(true)
//}

