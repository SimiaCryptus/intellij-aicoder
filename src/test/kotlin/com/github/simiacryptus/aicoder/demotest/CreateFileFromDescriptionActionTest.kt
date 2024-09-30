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
import java.lang.Thread
import java.time.Duration

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class CreateFileFromDescriptionActionTest {
    private lateinit var remoteRobot: RemoteRobot
    private val log = LoggerFactory.getLogger(CreateFileFromDescriptionActionTest::class.java)

    @BeforeAll
    fun setup() {
        remoteRobot = RemoteRobot("http://127.0.0.1:8082")
    }

    @Test
    fun testCreateFileFromDescription() = with(remoteRobot) {
        speak("Welcome to the AI Coder demo. We'll create a Kotlin data class using natural language description.")
        log.info("Starting testCreateFileFromDescription")
        Thread.sleep(1000)

        step("Open project view") {
            speak("First, let's open the project view.")
            find(CommonContainerFixture::class.java, byXpath("//div[@class='ProjectViewTree']")).click()
            log.info("Project view opened")
            Thread.sleep(1000)
        }

        step("Open context menu") {
            speak("Now, we'll navigate to the Kotlin source directory and open the context menu.")
            val projectTree = find(JTreeFixture::class.java, byXpath("//div[@class='ProjectViewTree']"))
            projectTree.doubleClickPath(*arrayOf("TestProject"), fullMatch = false)
            projectTree.doubleClickPath(*arrayOf("TestProject", "src"), fullMatch = false)
            projectTree.doubleClickPath(*arrayOf("TestProject", "src", "main"), fullMatch = false)
            projectTree.rightClickPath(*arrayOf("TestProject", "src", "main", "kotlin"), fullMatch = false)
            log.info("Context menu opened")
            Thread.sleep(1000)
        }
        Thread.sleep(2000) // Increase delay to ensure the context menu is fully loaded

        step("Select 'AI Coder' menu") {
            speak("From the context menu, we'll select the AI Coder option.")
            waitFor(Duration.ofSeconds(60)) { // Increase timeout
                try {
                    val aiCoderMenu = find(CommonContainerFixture::class.java, byXpath("//div[contains(@class, 'ActionMenu') and contains(@text, 'AI Coder')]"))
                    log.info("Found 'AI Coder' menu")
                    aiCoderMenu.click()
                    log.info("'AI Coder' menu clicked successfully")
                    speak("AI Coder menu selected.")
                    true
                } catch (e: Exception) {
                    log.warn("Failed to find or click 'AI Coder' menu: ${e.message}")
                    false
                }
            }
            Thread.sleep(1000)
        }

        step("Click 'Create File from Description' action") {
            speak("Now, we'll choose the 'Create File from Description' action.")
            waitFor(Duration.ofSeconds(30)) { // Increase timeout
                try {
                    val menuItems = findAll(CommonContainerFixture::class.java, byXpath("//div[contains(@class, 'ActionMenuItem')]"))
                    log.info("Found ${menuItems.size} menu items")
                    menuItems.forEach { log.info("Menu item: ${it.findAllText().joinToString(" / ") { "\t${it.text}" }}") }
                } catch (e: Exception) {
                    log.warn("Failed to list menu items: ${e.message}")
                }
                try {
                    findAll(CommonContainerFixture::class.java, byXpath("//div[contains(@class, 'ActionMenuItem') and contains(@text, 'Create File from Description')]"))
                        .firstOrNull()?.click()
                    log.info("'Create File from Description' action clicked")
                    speak("Create File from Description action selected.")
                    true
                } catch (e: Exception) {
                    log.warn("Failed to find 'Create File from Description' action: ${e.message}")
                    false
                }
            }
            Thread.sleep(1000)
        }

        step("Enter file description") {
            speak("Let's enter a description for our new Kotlin data class.")
            waitFor(Duration.ofSeconds(10)) {
                try {
                    val textField = find(JTextAreaFixture::class.java, byXpath("//div[@class='JTextArea']"))
                    textField.click()
                    remoteRobot.keyboard {
                        this.pressing(KeyEvent.VK_CONTROL) {
                            key(KeyEvent.VK_A)
                        }
                        enterText("Create a Kotlin data class named Person with properties: name (String), age (Int), and email (String)")
                    }
                    speak("Description entered: Create a Kotlin data class named Person with properties: name (String), age (Int), and email (String)")
                    log.info("File description entered")
                    Thread.sleep(1000)
                    val okButton = find(CommonContainerFixture::class.java, byXpath("//div[@class='JButton' and @text='Generate']"))
                    okButton.click()
                    log.info("OK button clicked")
                    speak("Generating the file now.")
                    true
                } catch (e: Exception) {
                    log.warn("Failed to enter file description or click OK: ${e.message}")
                    false
                }
            }
            Thread.sleep(2000)
        }

        step("Verify file creation") {
            speak("Let's verify that the file has been created.")
            waitFor(Duration.ofSeconds(20)) {
                val projectTree = find(JTreeFixture::class.java, byXpath("//div[@class='ProjectViewTree']"))
                projectTree.clickPath(*arrayOf("TestProject", "src", "main", "kotlin"), fullMatch = false)
                remoteRobot.keyboard { key(KeyEvent.VK_RIGHT) }
                val fileCreated = projectTree.hasText("Person")
                if (fileCreated) {
                    speak("The Person.kt file has been successfully created.")
                }
                fileCreated
            }
            Thread.sleep(1000)
        }

        step("Open created file") {
            speak("Now, let's open the newly created file.")
            find(JTreeFixture::class.java, byXpath("//div[@class='ProjectViewTree']"))
                .doubleClickPath(*arrayOf("TestProject", "src", "main", "kotlin", "Person"), fullMatch = false)
            Thread.sleep(1000)
        }

        step("Verify file content") {
            speak("Finally, we'll verify the content of the generated file.")
            val editor = find(EditorFixture::class.java, byXpath("//div[@class='EditorComponentImpl']"))
            waitFor(Duration.ofSeconds(5)) {
                val txt = editor.findAllText().joinToString("") { it.text }.replace("\n", "")
                val contentCorrect = txt.contains("data class Person(") &&
                                     txt.contains("val name: String,") &&
                                     txt.contains("val age: Int,") &&
                                     txt.contains("val email: String")
                if (contentCorrect) {
                    speak("The file content is correct. It contains a data class Person with the specified properties.")
                }
                contentCorrect
            }
        }
        speak("This concludes our AI Coder demo. We've successfully created a Kotlin data class using natural language description.")
    }

}

