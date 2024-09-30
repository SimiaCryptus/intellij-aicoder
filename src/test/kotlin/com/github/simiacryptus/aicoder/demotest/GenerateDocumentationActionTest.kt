package com.github.simiacryptus.aicoder.demotest

import com.github.simiacryptus.aicoder.demotest.TestUtil.speak
import com.intellij.remoterobot.RemoteRobot
import com.intellij.remoterobot.fixtures.CommonContainerFixture
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
    speak("Welcome to the AI Coder demo. Today, we'll be exploring the Generate Documentation feature, which automatically creates comprehensive API documentation for your code packages.")
        log.info("Starting testGenerateDocumentation")
        sleep(3000)

        step("Open project view") {
        speak("Let's begin by opening the project view to access our files. This step is crucial as it allows us to navigate our project structure efficiently.")
            find(CommonContainerFixture::class.java, byXpath("//div[@class='ProjectViewTree']")).click()
            log.info("Project view opened")
            sleep(2000)
        }

        step("Navigate to files utility package") {
        speak("Now, we'll navigate to the files utility package. We're selecting this package because it contains important utility functions that would benefit from clear, AI-generated documentation.")
            val projectTree = find(JTreeFixture::class.java, byXpath("//div[@class='ProjectViewTree']"))
            projectTree.rightClickPath(*arrayOf("DataGnome", "src", "main", "kotlin", "com.simiacryptus.util", "files"), fullMatch = false)
            log.info("Files utility package selected")
            sleep(2000)
        }

        step("Select 'AI Coder' menu") {
        speak("From the context menu that appears, we'll select the AI Coder option. Notice how easily we can access various AI-powered coding tools from this menu, streamlining our development process.")
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
            sleep(2000)
        }

        step("Click 'Generate Documentation' action") {
        speak("Within the AI Coder menu, we'll choose the 'Generate Documentation' action. This powerful tool allows us to automatically create detailed documentation for our selected package, saving significant time and ensuring consistency.")
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
            sleep(2000)
        }

        step("Configure documentation generation") {
        speak("A dialog box has appeared, allowing us to configure the documentation generation settings. One of the key benefits of this feature is its flexibility. Let's customize the AI instructions for our specific needs to generate the most relevant documentation.")
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
            speak("We've entered our custom instructions and clicked 'OK'. As you can see, this significantly streamlines the documentation process. The AI is now generating the documentation based on our specifications.")
                true
            } catch (e: Exception) {
                log.warn("Failed to configure documentation generation: ${e.message}")
                false
            }
        }
        speak("The AI is now processing our request. This typically takes just a few seconds, but it may vary depending on the size and complexity of the package. While we wait, it's worth noting how this feature can dramatically reduce the time spent on documentation, allowing developers to focus more on coding and problem-solving.")
        sleep(5000)


        step("Verify documentation creation")
        {
        speak("Now, let's verify that the documentation has been generated and opened in the editor. This step ensures that our action was successful and allows us to review the AI's output.")
            waitFor(Duration.ofSeconds(60)) {
                try {
                    val editor = find(CommonContainerFixture::class.java, byXpath("//div[@class='EditorComponentImpl']"))
                    if (editor.isShowing) {
                    speak("Excellent! The documentation has been successfully generated and opened in the editor. This demonstrates the AI's ability to quickly produce comprehensive documentation. You can now review and make any necessary adjustments, significantly reducing the time and effort required for documentation tasks.")
                        true
                    } else {
                        false
                    }
                } catch (e: Exception) {
                    log.warn("Failed to find opened editor: ${e.message}")
                    false
                }
            }
            sleep(3000)
            speak("This concludes our demonstration of the AI Coder Generate Documentation feature. We've successfully shown how it can automatically create comprehensive API documentation for the files utility package, highlighting its ability to save time, improve consistency, and enhance overall code documentation. Thank you for joining us for this demonstration of the Generate Documentation feature!")
            sleep(10000)
        }
    }
}