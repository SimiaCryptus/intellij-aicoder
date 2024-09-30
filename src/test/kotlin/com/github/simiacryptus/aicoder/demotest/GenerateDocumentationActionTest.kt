package com.github.simiacryptus.aicoder.demotest

import com.github.simiacryptus.aicoder.demotest.TestUtil.speak
import com.intellij.remoterobot.RemoteRobot
import com.intellij.remoterobot.fixtures.CommonContainerFixture
import com.intellij.remoterobot.fixtures.JCheckboxFixture
import com.intellij.remoterobot.fixtures.JTextAreaFixture
import com.intellij.remoterobot.fixtures.JTextFieldFixture
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
import java.lang.Thread.sleep
import java.time.Duration

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class GenerateDocumentationActionTest {
    private lateinit var remoteRobot: RemoteRobot
    private val log = LoggerFactory.getLogger(GenerateDocumentationActionTest::class.java)

    @BeforeAll
    fun setup() {
        remoteRobot = RemoteRobot("http://127.0.0.1:8082")
    }

    @Test
    fun testGenerateDocumentation() = with(remoteRobot) {
        speak("Welcome to the AI Coder demo. We'll generate documentation for the files utility package.")
        log.info("Starting testGenerateDocumentation")
        sleep(1000)

        step("Open project view") {
            speak("First, let's open the project view.")
            find(CommonContainerFixture::class.java, byXpath("//div[@class='ProjectViewTree']")).click()
            log.info("Project view opened")
            sleep(1000)
        }

        step("Navigate to files utility package") {
            speak("Now, we'll navigate to the files utility package.")
            val projectTree = find(JTreeFixture::class.java, byXpath("//div[@class='ProjectViewTree']"))
            projectTree.rightClickPath(*arrayOf("DataGnome", "src", "main", "kotlin", "com.simiacryptus.util", "files"), fullMatch = false)
            log.info("Files utility package selected")
            sleep(1000)
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
            sleep(1000)
        }

        step("Click 'Generate Documentation' action") {
            speak("Now, we'll choose the 'Generate Documentation' action.")
            waitFor(Duration.ofSeconds(10)) {
                try {
                    findAll(CommonContainerFixture::class.java, byXpath("//div[contains(@class, 'ActionMenuItem') and contains(@text, 'Generate Documentation')]"))
                        .firstOrNull()?.click()
                    log.info("'Generate Documentation' action clicked")
                    true
                } catch (e: Exception) {
                    log.warn("Failed to find 'Generate Documentation' action: ${e.message}")
                    false
                }
            }
            sleep(1000)
        }

        step("Configure documentation generation") {
            speak("Let's configure the documentation generation settings.")
            waitFor(Duration.ofSeconds(10)) {
                val dialog = find(CommonContainerFixture::class.java, byXpath("//div[@class='MyDialog' and @title='Compile Documentation']"))
                dialog.isShowing
            }
            val dialog = find(CommonContainerFixture::class.java, byXpath("//div[@class='MyDialog' and @title='Compile Documentation']"))
            val aiInstructionField = dialog.find(JTextAreaFixture::class.java, byXpath("//div[@class='JBTextArea']"))
            aiInstructionField.click()
            // select all text and delete
            keyboard {
                pressing(KeyEvent.VK_CONTROL) {
                    key(KeyEvent.VK_A) // Select all
                    key(KeyEvent.VK_BACK_SPACE) // Delete
                }
            }
            remoteRobot.keyboard { enterText("Create comprehensive API documentation for the files utility package") }


            try {
                val generateButton = find(CommonContainerFixture::class.java, byXpath("//div[@class='JButton' and @text='OK']"))
                generateButton.click()
                log.info("Documentation generation configured and started")
                speak("Documentation generation configured and started.")
                true
            } catch (e: Exception) {
                log.warn("Failed to configure documentation generation: ${e.message}")
                false
            }
        }
        sleep(5000) // Increase wait time to allow for processing


        step("Verify documentation creation")
        {
            speak("Let's verify that the documentation has been generated and opened in the editor.")
            waitFor(Duration.ofSeconds(60)) {
                try {
                    val editor = find(CommonContainerFixture::class.java, byXpath("//div[@class='EditorComponentImpl']"))
                    if (editor.isShowing) {
                        speak("The documentation has been generated and opened in the editor.")
                        true
                    } else {
                        false
                    }
                } catch (e: Exception) {
                    log.warn("Failed to find opened editor: ${e.message}")
                    false
                }
            }
            sleep(1000)
            speak("This concludes our AI Coder demo. We've successfully generated and opened the documentation for the files utility package.")
        }
    }
}