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
import java.lang.Thread
import java.time.Duration

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class CreateFileFromDescriptionActionTest : BaseActionTest() {


    companion object {
        val log = LoggerFactory.getLogger(CreateFileFromDescriptionActionTest::class.java)
    }
    @Test
    fun testCreateFileFromDescription() = with(remoteRobot) {
        speak("Welcome to the AI Coder demo. We'll explore the 'Create File from Description' feature, which generates Kotlin files from natural language instructions.")
        log.info("Starting testCreateFileFromDescription")
        Thread.sleep(2000)

        step("Open project view") {
            speak("Opening the project view to navigate the project structure.")
            find(CommonContainerFixture::class.java, byXpath("//div[@class='ProjectViewTree']")).click()
            log.info("Project view opened")
            Thread.sleep(2000)
        }

        step("Open context menu") {
            speak("Navigating to the Kotlin source directory.")
            val projectTree = find(JTreeFixture::class.java, byXpath("//div[@class='ProjectViewTree']"))
            projectTree.doubleClickPath(*arrayOf("TestProject"), fullMatch = false)
            projectTree.doubleClickPath(*arrayOf("TestProject", "src"), fullMatch = false)
            projectTree.doubleClickPath(*arrayOf("TestProject", "src", "main"), fullMatch = false)
            speak("Opening the context menu to access AI Coder features.")
            projectTree.rightClickPath(*arrayOf("TestProject", "src", "main", "kotlin"), fullMatch = false)
            log.info("Context menu opened")
            Thread.sleep(2000)
        }
        Thread.sleep(3000)

        step("Select 'AI Coder' menu") {
            speak("Selecting the 'AI Coder' option from the context menu.")
            waitFor(Duration.ofSeconds(60)) {
                try {
                    val aiCoderMenu = find(CommonContainerFixture::class.java, byXpath("//div[contains(@class, 'ActionMenu') and contains(@text, 'AI Coder')]"))
                    log.info("Found 'AI Coder' menu")
                    aiCoderMenu.click()
                    log.info("'AI Coder' menu clicked successfully")
                    speak("AI Coder menu opened.")
                    true
                } catch (e: Exception) {
                    log.warn("Failed to find or click 'AI Coder' menu: ${e.message}")
                    false
                }
            }
            Thread.sleep(2000)
        }

        step("Click 'Create File from Description' action") {
            speak("Selecting 'Create File from Description' action.")
            waitFor(Duration.ofSeconds(30)) {
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
                    speak("'Create File from Description' action initiated.")
                    true
                } catch (e: Exception) {
                    log.warn("Failed to find 'Create File from Description' action: ${e.message}")
                    false
                }
            }
            Thread.sleep(2000)
        }

        step("Enter file description") {
            speak("Entering a natural language description for a new Kotlin data class.")
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
                    speak("Description entered: Create a Kotlin data class named Person with properties: name (String), age (Int), and email (String).")
                    log.info("File description entered")
                    Thread.sleep(2000)
                    val okButton = find(CommonContainerFixture::class.java, byXpath("//div[@class='JButton' and @text='Generate']"))
                    okButton.click()
                    log.info("Generate button clicked")
                    true
                } catch (e: Exception) {
                    log.warn("Failed to enter file description or click OK: ${e.message}")
                    false
                }
            }
            Thread.sleep(3000)
        }

        step("Verify file creation") {
            speak("Verifying file creation in the project structure.")
            waitFor(Duration.ofSeconds(20)) {
                val projectTree = find(JTreeFixture::class.java, byXpath("//div[@class='ProjectViewTree']"))
                projectTree.clickPath(*arrayOf("TestProject", "src", "main", "kotlin"), fullMatch = false)
                remoteRobot.keyboard { key(KeyEvent.VK_RIGHT) }
                val fileCreated = projectTree.hasText("Person")
                if (fileCreated) {
                    speak("Person.kt file successfully created.")
                }
                fileCreated
            }
            Thread.sleep(2000)
        }

        step("Open created file") {
            speak("Opening the created file to examine its contents.")
            find(JTreeFixture::class.java, byXpath("//div[@class='ProjectViewTree']"))
                .doubleClickPath(*arrayOf("TestProject", "src", "main", "kotlin", "Person"), fullMatch = false)
            Thread.sleep(2000)
        }

        step("Verify file content") {
            speak("Verifying the content of the generated file.")
            val editor = find(EditorFixture::class.java, byXpath("//div[@class='EditorComponentImpl']"))
            waitFor(Duration.ofSeconds(5)) {
                val txt = editor.findAllText().joinToString("") { it.text }.replace("\n", "")
                val contentCorrect = txt.contains("data class Person(") &&
                        txt.contains("val name: String,") &&
                        txt.contains("val age: Int,") &&
                        txt.contains("val email: String")
                if (contentCorrect) {
                    speak("File content verified: Person data class with specified properties created successfully.")
                }
                contentCorrect
            }
            Thread.sleep(2000)
        }
        speak("Demo concluded. The 'Create File from Description' feature successfully generated a Kotlin file from natural language input, demonstrating its potential to streamline development processes.")
        Thread.sleep(5000)
    }

}