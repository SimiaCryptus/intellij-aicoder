import org.jetbrains.changelog.Changelog
import org.jetbrains.changelog.markdownToHTML
import org.jetbrains.intellij.platform.gradle.TestFrameworkType
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

fun properties(key: String) = providers.gradleProperty(key).getOrElse("")

plugins {
  id("java")
  kotlin("jvm") version "2.0.20"
  id("org.jetbrains.intellij.platform") version "2.4.0"
  id("org.jetbrains.changelog") version "2.2.1"
  id("org.jetbrains.qodana") version "2024.2.3"
  id("org.jetbrains.dokka") version "2.0.0-Beta"
}


group = "com.simiacryptus"
version = properties("pluginVersion")

repositories {
  mavenCentral()
  intellijPlatform {
    defaultRepositories()
  }
}

val jetty_version = "11.0.24"
val slf4j_version = "2.0.16"
val skyenet_version = "1.2.22"
val remoterobot_version = "0.11.23"
val jackson_version = "2.17.2"
val aws_sdk_version = "2.25.60"
val httpclient5_version = "5.3.1"
val logback_version = "1.5.16"
val commons_text_version = "1.11.0"
val commons_lang3_version = "3.15.0"

dependencies {
  
  
  // Pin all AWS SDK dependencies to the same version to avoid forced upgrades and reduce duplicates
  implementation("software.amazon.awssdk:bedrockruntime:$aws_sdk_version")
  implementation("software.amazon.awssdk:s3:$aws_sdk_version")
  implementation("software.amazon.awssdk:kms:$aws_sdk_version")
  
  implementation("org.apache.commons:commons-text:$commons_text_version")
  implementation("org.apache.commons:commons-lang3:$commons_lang3_version")
  implementation("com.vladsch.flexmark:flexmark:0.64.8")
  implementation("com.googlecode.java-diff-utils:diffutils:1.3.0")
  implementation("org.apache.httpcomponents.client5:httpclient5:$httpclient5_version")
  
  implementation("com.simiacryptus:jo-penai:1.1.13") {
    exclude(group = "org.jetbrains.kotlin")
  }
  implementation("com.simiacryptus.skyenet:core:$skyenet_version") {
    exclude(group = "org.jetbrains.kotlin")
  }
  implementation("com.simiacryptus.skyenet:webui:$skyenet_version") {
    exclude(group = "org.jetbrains.kotlin")
  }
  
  implementation("com.fasterxml.jackson.core:jackson-databind:$jackson_version")
  implementation("com.fasterxml.jackson.core:jackson-annotations:$jackson_version")
  implementation("com.fasterxml.jackson.module:jackson-module-kotlin:$jackson_version")
  
  implementation("org.eclipse.jetty:jetty-server:$jetty_version")
  implementation("org.eclipse.jetty:jetty-servlet:$jetty_version")
  implementation("org.eclipse.jetty:jetty-annotations:$jetty_version")
  implementation("org.eclipse.jetty.websocket:websocket-jetty-server:$jetty_version")
  implementation("org.eclipse.jetty.websocket:websocket-jetty-client:$jetty_version")
  implementation("org.eclipse.jetty.websocket:websocket-servlet:$jetty_version")
  
  implementation("org.slf4j:slf4j-api:$slf4j_version")
  implementation("ch.qos.logback:logback-classic:$logback_version")

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

  intellijPlatform {
    create(providers.gradleProperty("platformType"), providers.gradleProperty("platformVersion"))

    bundledPlugins(providers.gradleProperty("platformBundledPlugins").map { it.split(',') })
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

changelog {
  groups.empty()
  repositoryUrl = providers.gradleProperty("pluginRepositoryUrl")
}

tasks {
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