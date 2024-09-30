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
    speak("Welcome to the AI Coder demo. Today, we'll demonstrate the Generate Related File feature by converting a readme.md file into a reveal.js HTML presentation.")
        log.info("Starting testGenerateRelatedFile")
        Thread.sleep(3000)

        step("Open project view") {
        speak("To begin, we'll open the project view to access our files.")
            find(CommonContainerFixture::class.java, byXpath("//div[@class='ProjectViewTree']")).click()
            log.info("Project view opened")
            Thread.sleep(2000)
        }

        step("Select readme.md file") {
        speak("Next, we'll locate and select the readme.md file in our project structure.")
            val projectTree = find(JTreeFixture::class.java, byXpath("//div[@class='ProjectViewTree']"))
            projectTree.clickPath(*arrayOf("TestProject", "readme.md"), fullMatch = false)
            log.info("readme.md file selected")
            Thread.sleep(2000)
        }

        step("Open context menu") {
        speak("Now, we'll right-click to open the context menu, which contains our AI Coder options.")
            val projectTree = find(JTreeFixture::class.java, byXpath("//div[@class='ProjectViewTree']"))
            projectTree.rightClick()
            log.info("Context menu opened via right-click")
            Thread.sleep(2000)
        }

        step("Select 'AI Coder' menu") {
        speak("In the context menu, we'll navigate to and select the AI Coder option to access its features.")
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
        speak("From the AI Coder menu, we'll select the 'Generate Related File' action. This powerful feature allows us to create associated files based on existing content.")
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
        speak("Now, we'll provide a directive to the AI, instructing it to convert our readme into a reveal.js presentation. This showcases the AI's ability to understand complex instructions and generate appropriate content.")
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
                speak("We've entered the following directive: Convert this readme.md into a reveal.js HTML presentation. This instructs the AI to transform our markdown content into an interactive slideshow.")
                    log.info("File generation directive entered")
                    Thread.sleep(3000)
                    val okButton = find(CommonContainerFixture::class.java, byXpath("//div[@class='JButton' and @text='Generate']"))
                    okButton.click()
                    log.info("Generate button clicked")
                speak("We've initiated the generation process. The AI will now create our presentation file based on the readme content.")
                    true
                } catch (e: Exception) {
                    log.warn("Failed to enter directive or click Generate: ${e.message}")
                    false
                }
            }
        speak("While we wait for the AI to generate our presentation, it's worth noting how this feature can save significant time in creating associated documents or code files.")
        Thread.sleep(5000)
        }

        step("Verify file creation") {
        speak("Finally, let's verify that our new presentation file has been successfully created.")
            waitFor(Duration.ofSeconds(20)) {
                val projectTree = find(JTreeFixture::class.java, byXpath("//div[@class='ProjectViewTree']"))
                projectTree.clickPath(*arrayOf("TestProject"), fullMatch = false)
                remoteRobot.keyboard { key(KeyEvent.VK_RIGHT) }
                val fileCreated = projectTree.hasText("presentation.html")
                if (fileCreated) {
                speak("Great! We can see that the presentation.html file has been successfully created in our project structure. This demonstrates the AI's ability to generate complex, related files from existing content.")
                }
                fileCreated
            }
            Thread.sleep(3000)
        }

    speak("This concludes our AI Coder Generate Related File demo. We've successfully converted a readme.md file into a reveal.js HTML presentation, showcasing the power and flexibility of this feature. Thank you for watching, and we hope you're excited about the possibilities this opens up for your development workflow.")
    Thread.sleep(10000)
    }
}