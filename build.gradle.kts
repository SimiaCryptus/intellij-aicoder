import org.jetbrains.changelog.Changelog
import org.jetbrains.changelog.markdownToHTML

fun properties(key: String) = project.findProperty(key).toString()

plugins {
    id("java")
    id("scala")
    id("org.jetbrains.kotlin.jvm") version "1.7.21"
    id("org.jetbrains.intellij") version "1.13.3"
    id("org.jetbrains.changelog") version "2.0.0"
    id("org.jetbrains.qodana") version "0.1.13"
    id("org.jetbrains.kotlinx.kover") version "0.6.1"
}

group = "com.github.simiacryptus"
version = "1.0.20"

repositories {
    mavenCentral()
    maven(url = "https://packages.jetbrains.team/maven/p/ij/intellij-dependencies")
}

val kotlin_version = "1.7.21"
val kotlinCompiler by configurations.creating
dependencies {

//    implementation("com.simiacryptus:JoePenai:1.0.7")
    implementation("com.simiacryptus:joe-penai:1.0.6")

    implementation("com.simiacryptus:skyenet:1.0.2")
//    implementation("com.simiacryptus:SkyeNet:1.0.3")
//    implementation(files("../SkyeNet/lib/ui.jar"))

    implementation("org.codehaus.groovy:groovy-all:2.5.14")

//    // Used in KotlinInterpreterAlternate
//    implementation("org.jetbrains.kotlin:kotlin-script-runtime:$kotlin_version")
//    implementation("org.jetbrains.kotlin:kotlin-scripting-compiler-embeddable:$kotlin_version")
//    implementation("org.jetbrains.kotlin:kotlin-scripting-jvm-host:$kotlin_version")

    implementation("org.jetbrains.kotlin:kotlin-stdlib:$kotlin_version")
    implementation("org.jetbrains.kotlin:kotlin-reflect:$kotlin_version")

    implementation("org.slf4j:slf4j-api:2.0.5")

    testImplementation("com.intellij.remoterobot:remote-robot:0.11.16")
    testImplementation("com.intellij.remoterobot:remote-fixtures:0.11.16")
    testImplementation("com.squareup.okhttp3:okhttp:3.14.9")
    testImplementation(kotlin("script-runtime"))
}


kotlin {
    jvmToolchain(11)
}

tasks {
    compileKotlin {
        kotlinOptions {
            javaParameters = true
        }
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

    runIdeForUiTests {
        systemProperty("kotlin.compiler.classpath", kotlinCompiler.joinToString(separator = File.pathSeparator) { it.absolutePath })
        systemProperty("robot-server.port", "8082")
        systemProperty("ide.mac.message.dialogs.as.sheets", "false")
        systemProperty("jb.privacy.policy.text", "<!--999.999-->")
        systemProperty("jb.consents.confirmation.enabled", "false")
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
    reportPath.set(file("build/reports/inspections").canonicalPath)
    saveReport.set(true)
    showReport.set(System.getenv("QODANA_SHOW_REPORT")?.toBoolean() ?: false)
}

kover.xmlReport {
    onCheck.set(true)
}
