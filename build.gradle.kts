import org.jetbrains.changelog.Changelog
import org.jetbrains.changelog.markdownToHTML
import org.jetbrains.intellij.platform.gradle.TestFrameworkType
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

fun properties(key: String) = providers.gradleProperty(key).get()

plugins {
    id("java")
    kotlin("jvm") version "2.0.20"
    id("org.jetbrains.intellij.platform") version "2.1.0"
    id("org.jetbrains.changelog") version "2.2.1"
    id("org.jetbrains.qodana") version "2024.2.3"
    id("org.jetbrains.kotlinx.kover") version "0.9.0-RC"
    id("org.jetbrains.dokka") version "2.0.0-Beta"
}


group = "com.github.simiacryptus"
version = properties("pluginVersion")

repositories {
    mavenCentral()
    intellijPlatform {
        defaultRepositories()
    }
}

val jetty_version = "11.0.24"
val slf4j_version = "2.0.16"
val skyenet_version = "1.2.11"
val remoterobot_version = "0.11.23"
val jackson_version = "2.17.2"

dependencies {
    implementation("software.amazon.awssdk:bedrock:2.25.9")
    implementation("software.amazon.awssdk:bedrockruntime:2.25.9")
    implementation("software.amazon.awssdk:s3:2.25.9")
    implementation("software.amazon.awssdk:kms:2.25.9")

    implementation("org.apache.commons:commons-text:1.11.0")
    implementation(group = "com.vladsch.flexmark", name = "flexmark", version = "0.64.8")
    implementation("com.googlecode.java-diff-utils:diffutils:1.3.0")
    implementation(group = "org.apache.httpcomponents.client5", name = "httpclient5", version = "5.2.3")

    implementation(group = "com.simiacryptus", name = "jo-penai", version = "1.1.9")
    implementation(group = "com.simiacryptus.skyenet", name = "kotlin", version = skyenet_version)
    implementation(group = "com.simiacryptus.skyenet", name = "core", version = skyenet_version)
    implementation(group = "com.simiacryptus.skyenet", name = "webui", version = skyenet_version)

    implementation(group = "com.fasterxml.jackson.core", name = "jackson-databind", version = jackson_version)
    implementation(group = "com.fasterxml.jackson.core", name = "jackson-annotations", version = jackson_version)
    implementation(group = "com.fasterxml.jackson.module", name = "jackson-module-kotlin", version = jackson_version)
    implementation ("com.fasterxml.jackson.datatype:jackson-datatype-jsr310:$jackson_version")

    implementation(group = "org.eclipse.jetty", name = "jetty-server", version = jetty_version)
    implementation(group = "org.eclipse.jetty", name = "jetty-servlet", version = jetty_version)
    implementation(group = "org.eclipse.jetty", name = "jetty-annotations", version = jetty_version)
    implementation(group = "org.eclipse.jetty.websocket", name = "websocket-jetty-server", version = jetty_version)
    implementation(group = "org.eclipse.jetty.websocket", name = "websocket-jetty-client", version = jetty_version)
    implementation(group = "org.eclipse.jetty.websocket", name = "websocket-servlet", version = jetty_version)

    implementation(group = "org.slf4j", name = "slf4j-api", version = slf4j_version)

    testImplementation(platform("org.junit:junit-bom:5.11.2"))
    testImplementation("org.junit.jupiter:junit-jupiter-api")
    testImplementation("org.junit.jupiter:junit-jupiter-engine")
    testImplementation("org.junit.vintage:junit-vintage-engine")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")

    testImplementation(group = "com.intellij.remoterobot", name = "remote-robot", version = remoterobot_version)
    testImplementation(group = "com.intellij.remoterobot", name = "remote-fixtures", version = remoterobot_version)
    testImplementation(
        group = "com.intellij.remoterobot",
        name = "robot-server-plugin",
        version = remoterobot_version,
        ext = "zip"
    )

    // IntelliJ Platform Gradle Plugin Dependencies Extension - read more: https://plugins.jetbrains.com/docs/intellij/tools-intellij-platform-gradle-plugin-dependencies-extension.html
    intellijPlatform {
        create(providers.gradleProperty("platformType"), providers.gradleProperty("platformVersion"))

        // Plugin Dependencies. Uses `platformBundledPlugins` property from the gradle.properties file for bundled IntelliJ Platform plugins.
        bundledPlugins(providers.gradleProperty("platformBundledPlugins").map { it.split(',') })

        // Plugin Dependencies. Uses `platformPlugins` property from the gradle.properties file for plugin from JetBrains Marketplace.
        plugins(providers.gradleProperty("platformPlugins").map { it.split(',') })

        instrumentationTools()
        pluginVerifier()
        zipSigner()
        testFramework(TestFrameworkType.Platform)
    }
}

kotlin {
    jvmToolchain(17)
}

tasks {
    withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile> {
        compilerOptions {
            jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17)
        }
    }

    jar {
        duplicatesStrategy = DuplicatesStrategy.EXCLUDE
        exclude("org/jetbrains/**")
    }


    test {
        useJUnitPlatform()
        testLogging {
            events("passed", "skipped", "failed")
        }
        systemProperty("junit.jupiter.execution.parallel.enabled", "true")
        systemProperty("junit.jupiter.execution.parallel.mode.default", "concurrent")
        systemProperty("idea.force.use.core.classloader", "true")
        systemProperty("junit.jupiter.extensions.autodetection.enabled", "true")
        // Include JUnit 3/4 tests
        include("**/*Test.class")
    }
    withType<KotlinCompile> {
        compilerOptions {
            jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17)
            javaParameters.set(true)
        }
    }


    runIde {
        maxHeapSize = "8g"
    }

}

// Configure IntelliJ Platform Gradle Plugin - read more: https://plugins.jetbrains.com/docs/intellij/tools-intellij-platform-gradle-plugin-extension.html
intellijPlatform {
    pluginConfiguration {
        version = providers.gradleProperty("pluginVersion")

        // Extract the <!-- Plugin description --> section from README.md and provide for the plugin's manifest
        description = providers.fileContents(layout.projectDirectory.file("README.md")).asText.map {
            val start = "<!-- Plugin description -->"
            val end = "<!-- Plugin description end -->"

            with(it.lines()) {
                if (!containsAll(listOf(start, end))) {
                    throw GradleException("Plugin description section not found in README.md:\n$start ... $end")
                }
                subList(indexOf(start) + 1, indexOf(end)).joinToString("\n").let(::markdownToHTML)
            }
        }

        val changelog = project.changelog // local variable for configuration cache compatibility
        // Get the latest available change notes from the changelog file
        changeNotes = providers.gradleProperty("pluginVersion").map { pluginVersion ->
            with(changelog) {
                renderItem(
                    (getOrNull(pluginVersion) ?: getUnreleased())
                        .withHeader(false)
                        .withEmptySections(false),
                    Changelog.OutputType.HTML,
                )
            }
        }

        ideaVersion {
            sinceBuild = providers.gradleProperty("pluginSinceBuild")
            untilBuild = providers.gradleProperty("pluginUntilBuild")
        }
    }

    signing {
        certificateChain = providers.environmentVariable("CERTIFICATE_CHAIN")
        privateKey = providers.environmentVariable("PRIVATE_KEY")
        password = providers.environmentVariable("PRIVATE_KEY_PASSWORD")
    }

    publishing {
    // Include VCS plugin
        token = providers.environmentVariable("PUBLISH_TOKEN")
        // The pluginVersion is based on the SemVer (https://semver.org) and supports pre-release labels, like 2.1.7-alpha.3
        // Specify pre-release label to publish the plugin in a custom Release Channel automatically. Read more:
        // https://plugins.jetbrains.com/docs/intellij/deployment.html#specifying-a-release-channel
        channels = providers.gradleProperty("pluginVersion").map { listOf(it.substringAfter('-', "").substringBefore('.').ifEmpty { "default" }) }
    }
    pluginVerification {
        ides {
            recommended()
        }
    }
}
repositories {
    mavenCentral()
    intellijPlatform {
        defaultRepositories()
    }
}

// Configure Gradle Changelog Plugin - read more: https://github.com/JetBrains/gradle-changelog-plugin
changelog {
    groups.empty()
    repositoryUrl = providers.gradleProperty("pluginRepositoryUrl")
}

// Configure Gradle Kover Plugin - read more: https://github.com/Kotlin/kotlinx-kover#configuration
kover {
    reports {
        total {
            xml {
                onCheck = true
            }
        }
    }
}

tasks {
    wrapper {
        gradleVersion = providers.gradleProperty("gradleVersion").get()
    }

    publishPlugin {
        dependsOn(patchChangelog)
    }
}

intellijPlatformTesting {
    runIde {
        register("runIdeForUiTests") {
            task {
                jvmArgumentProviders += CommandLineArgumentProvider {
                    listOf(
                        "-Drobot-server.port=8082",
                        "-Dide.mac.message.dialogs.as.sheets=false",
                        "-Djb.privacy.policy.text=<!--999.999-->",
                        "-Djb.consents.confirmation.enabled=false",
                        "-Dorg.gradle.configuration-cache=false"
                    )
                }
            }

            plugins {
                robotServerPlugin()
            }
        }
    }
}