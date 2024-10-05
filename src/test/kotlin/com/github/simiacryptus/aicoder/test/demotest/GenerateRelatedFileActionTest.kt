package com.github.simiacryptus.aicoder.test.demotest

import com.github.simiacryptus.aicoder.test.demotest.TestUtil.speak
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
class GenerateRelatedFileActionTest : BaseActionTest() {


    companion object {
        val log = LoggerFactory.getLogger(GenerateRelatedFileActionTest::class.java)
    }
    @Test
    fun testGenerateRelatedFile() = with(remoteRobot) {
    speak("This demo showcases the Generate Related File feature, converting a README.md to a reveal.js HTML presentation.")
        log.info("Starting testGenerateRelatedFile")
        Thread.sleep(3000)

        step("Open project view") {
        speak("Opening the project view.")
            find(CommonContainerFixture::class.java, byXpath("//div[@class='ProjectViewTree']")).click()
            log.info("Project view opened")
            Thread.sleep(2000)
        }

        step("Select README.md file") {
        speak("Selecting the README.md file.")
            val projectTree = find(JTreeFixture::class.java, byXpath("//div[@class='ProjectViewTree']"))
            projectTree.clickPath(*arrayOf("TestProject", "README.md"), fullMatch = false)
            log.info("README.md file selected")
            Thread.sleep(2000)
        }

        step("Open context menu") {
        speak("Opening the context menu.")
            val projectTree = find(JTreeFixture::class.java, byXpath("//div[@class='ProjectViewTree']"))
            projectTree.rightClick()
            log.info("Context menu opened via right-click")
            Thread.sleep(2000)
        }

        step("Select 'AI Coder' menu") {
        speak("Selecting the AI Coder menu.")
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
            Thread.sleep(2000)
        }

        step("Click 'Generate Related File' action") {
        speak("Selecting 'Generate Related File' action.")
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
            Thread.sleep(2000)
        }

        step("Enter file generation directive") {
        speak("Entering the file generation directive.")
            waitFor(Duration.ofSeconds(10)) {
                try {
                    val textField = find(JTextAreaFixture::class.java, byXpath("//div[@class='JTextArea']"))
                    textField.click()
                    remoteRobot.keyboard {
                        this.pressing(KeyEvent.VK_CONTROL) {
                            key(KeyEvent.VK_A)
                        }
                        enterText("Convert this README.md into a reveal.js HTML presentation")
                    }
                speak("Directive entered: Convert this README.md into a reveal.js HTML presentation.")
                    log.info("File generation directive entered")
                    Thread.sleep(3000)
                    val okButton = find(CommonContainerFixture::class.java, byXpath("//div[@class='JButton' and @text='Generate']"))
                    okButton.click()
                    log.info("Generate button clicked")
                speak("Generation process initiated.")
                    true
                } catch (e: Exception) {
                    false
                }
            }
        speak("Waiting for file generation.")
        Thread.sleep(5000)
        }

        step("Verify file creation") {
        speak("Verifying file creation.")
            waitFor(Duration.ofSeconds(20)) {
                val projectTree = find(JTreeFixture::class.java, byXpath("//div[@class='ProjectViewTree']"))
                projectTree.clickPath(*arrayOf("TestProject"), fullMatch = false)
                remoteRobot.keyboard { key(KeyEvent.VK_RIGHT) }
                val fileCreated = projectTree.hasText("presentation.html")
                if (fileCreated) {
                speak("presentation.html file successfully created.")
                }
                fileCreated
            }
            Thread.sleep(3000)
        }

    speak("Demo concluded. The Generate Related File feature has converted README.md to a reveal.js HTML presentation.")
    Thread.sleep(10000)
    }
}