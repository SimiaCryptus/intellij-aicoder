package com.github.simiacryptus.aicoder.demotest

import com.github.simiacryptus.aicoder.demotest.TestUtil.speak
import com.intellij.remoterobot.RemoteRobot
import com.intellij.remoterobot.fixtures.CommonContainerFixture
import com.intellij.remoterobot.fixtures.EditorFixture
import com.intellij.remoterobot.fixtures.JTextAreaFixture
import com.intellij.remoterobot.fixtures.JTreeFixture
import com.intellij.remoterobot.search.locators.byXpath
import com.intellij.remoterobot.stepsProcessing.step
import com.intellij.remoterobot.utils.keyboard
import com.intellij.remoterobot.utils.waitFor
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.slf4j.LoggerFactory
import java.awt.event.KeyEvent
import java.time.Duration

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class GenerateRelatedFileActionTest {
    private lateinit var remoteRobot: RemoteRobot
    private val log = LoggerFactory.getLogger(GenerateRelatedFileActionTest::class.java)

    @BeforeAll
    fun setup() {
        remoteRobot = RemoteRobot("http://127.0.0.1:8082")
    }

    @Test
    fun testGenerateRelatedFile() = with(remoteRobot) {
        speak("Welcome to the AI Coder demo. We'll convert the readme.md file into a reveal.js HTML presentation.")
        log.info("Starting testGenerateRelatedFile")
        Thread.sleep(1000)

        step("Open project view") {
            speak("First, let's open the project view.")
            find(CommonContainerFixture::class.java, byXpath("//div[@class='ProjectViewTree']")).click()
            log.info("Project view opened")
            Thread.sleep(1000)
        }

        step("Select readme.md file") {
            speak("Now, we'll select the readme.md file.")
            val projectTree = find(JTreeFixture::class.java, byXpath("//div[@class='ProjectViewTree']"))
            projectTree.clickPath(*arrayOf("TestProject", "readme.md"), fullMatch = false)
            log.info("readme.md file selected")
            Thread.sleep(1000)
        }

        step("Open context menu") {
            speak("Let's open the context menu to access the AI Coder option.")
            val projectTree = find(JTreeFixture::class.java, byXpath("//div[@class='ProjectViewTree']"))
            projectTree.rightClick()
            log.info("Context menu opened via right-click")
            Thread.sleep(1000)
        }

        step("Select 'AI Coder' menu") {
            speak("From the context menu, we'll select the AI Coder option.")
            waitFor(Duration.ofSeconds(10)) {
                try {
                    val aiCoderMenu = find(CommonContainerFixture::class.java, byXpath("//div[contains(@class, 'ActionMenu') and contains(@text, 'AI Coder')]"))
                    aiCoderMenu.click()
                    log.info("'AI Coder' menu clicked")
                    true
                } catch (e: Exception) {
                    log.warn("Failed to find or click 'AI Coder' menu: ${e.message}")
                    false
                }
            }
            Thread.sleep(1000)
        }

        step("Click 'Generate Related File' action") {
            speak("Now, we'll choose the 'Generate Related File' action.")
            waitFor(Duration.ofSeconds(10)) {
                try {
                    findAll(CommonContainerFixture::class.java, byXpath("//div[contains(@class, 'ActionMenuItem') and contains(@text, 'Generate Related File')]"))
                        .firstOrNull()?.click()
                    log.info("'Generate Related File' action clicked")
                    true
                } catch (e: Exception) {
                    log.warn("Failed to find 'Generate Related File' action: ${e.message}")
                    false
                }
            }
            Thread.sleep(1000)
        }

        step("Enter file generation directive") {
            speak("Let's enter a directive to convert the readme into a reveal.js presentation.")
            waitFor(Duration.ofSeconds(10)) {
                try {
                    val textField = find(JTextAreaFixture::class.java, byXpath("//div[@class='JTextArea']"))
                    textField.click()
                    remoteRobot.keyboard {
                        this.pressing(KeyEvent.VK_CONTROL) {
                            key(KeyEvent.VK_A)
                        }
                        enterText("Convert this readme.md into a reveal.js HTML presentation")
                    }
                    speak("Directive entered: Convert this readme.md into a reveal.js HTML presentation")
                    log.info("File generation directive entered")
                    Thread.sleep(1000)
                    val okButton = find(CommonContainerFixture::class.java, byXpath("//div[@class='JButton' and @text='Generate']"))
                    okButton.click()
                    log.info("Generate button clicked")
                    speak("Generating the presentation file now.")
                    true
                } catch (e: Exception) {
                    log.warn("Failed to enter directive or click Generate: ${e.message}")
                    false
                }
            }
            Thread.sleep(2000)
        }

        step("Verify file creation") {
            speak("Let's verify that the presentation file has been created.")
            waitFor(Duration.ofSeconds(20)) {
                val projectTree = find(JTreeFixture::class.java, byXpath("//div[@class='ProjectViewTree']"))
                projectTree.clickPath(*arrayOf("TestProject"), fullMatch = false)
                remoteRobot.keyboard { key(KeyEvent.VK_RIGHT) }
                val fileCreated = projectTree.hasText("presentation.html")
                if (fileCreated) {
                    speak("The presentation.html file has been successfully created.")
                }
                fileCreated
            }
            Thread.sleep(1000)
        }

        speak("This concludes our AI Coder demo. We've successfully converted the readme.md file into a reveal.js HTML presentation.")
    }
}