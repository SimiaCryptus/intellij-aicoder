import org.jetbrains.changelog.Changelog
import org.jetbrains.changelog.markdownToHTML
import org.jetbrains.dokka.gradle.DokkaTask
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

fun properties(key: String) = providers.gradleProperty(key).get()
fun environment(key: String) = providers.environmentVariable(key).get()

plugins {
    id("java") // Java support
    kotlin("jvm") version "2.0.20"
    id("org.jetbrains.intellij") version "1.17.2"
    id("org.jetbrains.changelog") version "2.2.0"
    id("org.jetbrains.qodana") version "2023.2.1"
    id("org.jetbrains.kotlinx.kover") version "0.7.4"
    id("org.jetbrains.dokka") version "1.9.10"
    kotlin("plugin.jpa") version "2.0.20"
    kotlin("plugin.allopen") version "2.0.20"
}


group = "com.github.simiacryptus"
version = properties("pluginVersion")

repositories {
    mavenCentral()
    maven(url = "https://packages.jetbrains.team/maven/p/ij/intellij-dependencies")
    maven(url = "https://packages.jetbrains.team/maven/p/iuia/qa-automation-maven")
}


val kotlin_version = "2.0.20" // This line can be removed if not used elsewhere
val jetty_version = "11.0.24"
val slf4j_version = "2.0.16"
val skyenet_version = "1.2.6"
val remoterobot_version = "0.11.23"
val jackson_version = "2.17.2"

dependencies {
    testImplementation("org.seleniumhq.selenium:selenium-java:4.15.0")
    testImplementation("org.testng:testng:7.8.0")
    implementation("software.amazon.awssdk:bedrock:2.25.7")
    implementation("software.amazon.awssdk:bedrockruntime:2.25.7")

    implementation("org.apache.commons:commons-text:1.11.0")
    implementation(group = "com.vladsch.flexmark", name = "flexmark-all", version = "0.64.8")
    implementation("com.googlecode.java-diff-utils:diffutils:1.3.0")
    implementation(group = "org.apache.httpcomponents.client5", name = "httpclient5", version = "5.2.3")

    implementation(group = "com.simiacryptus", name = "jo-penai", version = "1.1.5")
    implementation(group = "com.simiacryptus.skyenet", name = "kotlin", version = skyenet_version)
    implementation(group = "com.simiacryptus.skyenet", name = "core", version = skyenet_version)
    implementation(group = "com.simiacryptus.skyenet", name = "webui", version = skyenet_version)

    implementation(group = "com.fasterxml.jackson.core", name = "jackson-databind", version = jackson_version)
    implementation(group = "com.fasterxml.jackson.core", name = "jackson-annotations", version = jackson_version)
    implementation(group = "com.fasterxml.jackson.module", name = "jackson-module-kotlin", version = jackson_version)

    implementation(group = "org.eclipse.jetty", name = "jetty-server", version = jetty_version)
    implementation(group = "org.eclipse.jetty", name = "jetty-servlet", version = jetty_version)
    implementation(group = "org.eclipse.jetty", name = "jetty-annotations", version = jetty_version)
    implementation(group = "org.eclipse.jetty.websocket", name = "websocket-jetty-server", version = jetty_version)
    implementation(group = "org.eclipse.jetty.websocket", name = "websocket-jetty-client", version = jetty_version)
    implementation(group = "org.eclipse.jetty.websocket", name = "websocket-servlet", version = jetty_version)

    implementation(group = "org.slf4j", name = "slf4j-api", version = slf4j_version)

    testImplementation(group = "com.intellij.remoterobot", name = "remote-robot", version = remoterobot_version)
    testImplementation(group = "com.intellij.remoterobot", name = "remote-fixtures", version = remoterobot_version)
    testImplementation(
        group = "com.intellij.remoterobot",
        name = "robot-server-plugin",
        version = remoterobot_version,
        ext = "zip"
    )

    testImplementation(group = "com.squareup.okhttp3", name = "okhttp", version = "4.12.0")

    testImplementation(platform("org.junit:junit-bom:5.10.1"))
    testImplementation("org.junit.jupiter:junit-jupiter")

}


kotlin {
    jvmToolchain {
        languageVersion.set(JavaLanguageVersion.of(17))
    }
}

tasks {
    buildSearchableOptions {
        enabled = false
    }
    test {
        useJUnitPlatform()
        testLogging {
            events("passed", "skipped", "failed")
        }
    }
    val dokkaHtml by getting(DokkaTask::class) {
        outputDirectory.set(file("docs/api"))
    }

    register<Copy>("copyReadmeToSite") {
        from("README.md")
        into("docs")
    }
    register<Copy>("copyChangelogToSite") {
        from("CHANGELOG.md")
        into("docs")
    }

    register<Task>("generateProjectInfo") {
        doLast {
            file("docs/project-info.md").writeText(
                """
# Project Information
- Group: $group
- Version: $version
- Kotlin Version: $kotlin_version
- Jetty Version: $jetty_version
- SLF4J Version: $slf4j_version
For more details, please check the [README](README.md) and [CHANGELOG](CHANGELOG.md).
""".trimIndent()
            )
        }
    }
    withType<KotlinCompile> {
        compilerOptions {
            jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17)
        }
    }

    withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile> {
        compilerOptions {
            jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17)
            javaParameters.set(true)
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
    }
}

intellij {
    pluginName.set(properties("pluginName"))
    version.set(properties("platformVersion"))
    type.set(properties("platformType"))
    plugins.set(
        listOf(
            "com.intellij.java",
            "org.jetbrains.kotlin",
            "Git4Idea",
            "org.jetbrains.plugins.github"
        )
    )
}

changelog {
    groups.set(emptyList())
    repositoryUrl.set(properties("pluginRepositoryUrl"))
}

tasks {
    patchPluginXml {
        sinceBuild.set(properties("pluginSinceBuild"))
        untilBuild.set(properties("pluginUntilBuild"))
    }
}

qodana {
    cachePath.set(file(".qodana").canonicalPath)
}