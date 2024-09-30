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
    speak("Welcome to the AI Coder demo. Today, we'll be exploring the 'Create File from Description' feature, which allows developers to generate entire Kotlin files using natural language instructions. This powerful tool significantly streamlines the process of creating new code structures.")
        log.info("Starting testCreateFileFromDescription")
        Thread.sleep(2000)

        step("Open project view") {
        speak("Let's begin by opening the project view. This is where we'll navigate our project structure to set up our demonstration.")
            find(CommonContainerFixture::class.java, byXpath("//div[@class='ProjectViewTree']")).click()
            log.info("Project view opened")
            Thread.sleep(2000)
        }

        step("Open context menu") {
        speak("Next, we'll navigate to the Kotlin source directory. Notice how easily we can expand the project tree to locate our target directory.")
            val projectTree = find(JTreeFixture::class.java, byXpath("//div[@class='ProjectViewTree']"))
            projectTree.doubleClickPath(*arrayOf("TestProject"), fullMatch = false)
            projectTree.doubleClickPath(*arrayOf("TestProject", "src"), fullMatch = false)
            projectTree.doubleClickPath(*arrayOf("TestProject", "src", "main"), fullMatch = false)
        speak("Now that we've found our Kotlin source directory, we'll open the context menu by right-clicking. This is where we'll access the AI Coder features.")
            projectTree.rightClickPath(*arrayOf("TestProject", "src", "main", "kotlin"), fullMatch = false)
            log.info("Context menu opened")
            Thread.sleep(2000)
        }
        Thread.sleep(3000) // Increase delay to ensure the context menu is fully loaded

        step("Select 'AI Coder' menu") {
        speak("In the context menu, you'll notice an 'AI Coder' option. This powerful tool allows us to access various AI-powered coding features. Let's explore how this feature can enhance our development process.")
            waitFor(Duration.ofSeconds(60)) { // Increase timeout
                try {
                    val aiCoderMenu = find(CommonContainerFixture::class.java, byXpath("//div[contains(@class, 'ActionMenu') and contains(@text, 'AI Coder')]"))
                    log.info("Found 'AI Coder' menu")
                    aiCoderMenu.click()
                    log.info("'AI Coder' menu clicked successfully")
                speak("Excellent! We've successfully opened the AI Coder menu. This menu is your gateway to a variety of AI-assisted coding tools.")
                    true
                } catch (e: Exception) {
                    log.warn("Failed to find or click 'AI Coder' menu: ${e.message}")
                    false
                }
            }
            Thread.sleep(2000)
        }

        step("Click 'Create File from Description' action") {
        speak("Among the AI Coder options, we're looking for 'Create File from Description'. This feature demonstrates the AI's ability to understand natural language and translate it into code. Let's select it and see how it can streamline our workflow.")
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
                speak("Great! We've initiated the 'Create File from Description' action. Now, let's see how we can leverage natural language to create code.")
                    true
                } catch (e: Exception) {
                    log.warn("Failed to find 'Create File from Description' action: ${e.message}")
                    false
                }
            }
            Thread.sleep(2000)
        }

        step("Enter file description") {
        speak("Now, we'll enter a natural language description for our new Kotlin data class. Notice how easily we can specify the class name and its properties using simple English. This significantly streamlines the process of creating new code structures.")
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
                speak("We've entered the following description: Create a Kotlin data class named Person with properties: name (String), age (Int), and email (String). As you can see, this natural language input significantly simplifies the process of defining new classes.")
                    log.info("File description entered")
                    Thread.sleep(2000)
                    val okButton = find(CommonContainerFixture::class.java, byXpath("//div[@class='JButton' and @text='Generate']"))
                    okButton.click()
                    log.info("OK button clicked")
                speak("We've clicked the Generate button. The AI is now analyzing our description and creating the corresponding Kotlin file. This process typically takes just a few seconds, showcasing the efficiency of AI-assisted coding.")
                    true
                } catch (e: Exception) {
                    log.warn("Failed to enter file description or click OK: ${e.message}")
                    false
                }
            }
            Thread.sleep(3000)
        }

        step("Verify file creation") {
        speak("Now, let's verify that the file has been created. We'll look for a new file named 'Person.kt' in our project structure. This step demonstrates the AI's ability to not only generate code but also integrate it seamlessly into our project.")
            waitFor(Duration.ofSeconds(20)) {
                val projectTree = find(JTreeFixture::class.java, byXpath("//div[@class='ProjectViewTree']"))
                projectTree.clickPath(*arrayOf("TestProject", "src", "main", "kotlin"), fullMatch = false)
                remoteRobot.keyboard { key(KeyEvent.VK_RIGHT) }
                val fileCreated = projectTree.hasText("Person")
                if (fileCreated) {
                speak("Excellent! The Person.kt file has been successfully created. This demonstrates how quickly and accurately we can generate new files using AI Coder, significantly enhancing our productivity.")
                }
                fileCreated
            }
            Thread.sleep(2000)
        }

        step("Open created file") {
        speak("To examine the contents of our new file, let's open it. We'll double-click on 'Person.kt' in the project tree. This will allow us to verify the quality of the AI-generated code.")
            find(JTreeFixture::class.java, byXpath("//div[@class='ProjectViewTree']"))
                .doubleClickPath(*arrayOf("TestProject", "src", "main", "kotlin", "Person"), fullMatch = false)
            Thread.sleep(2000)
        }

        step("Verify file content") {
        speak("Finally, let's verify the content of the generated file. We'll check if it contains the data class structure we described. This step showcases the AI's ability to accurately translate natural language into proper Kotlin code.")
            val editor = find(EditorFixture::class.java, byXpath("//div[@class='EditorComponentImpl']"))
            waitFor(Duration.ofSeconds(5)) {
                val txt = editor.findAllText().joinToString("") { it.text }.replace("\n", "")
                val contentCorrect = txt.contains("data class Person(") &&
                                     txt.contains("val name: String,") &&
                                     txt.contains("val age: Int,") &&
                                     txt.contains("val email: String")
                if (contentCorrect) {
                speak("Excellent! The file content is correct. It contains a data class named Person with the properties we specified: name as a String, age as an Int, and email as a String. This demonstrates the accuracy and efficiency of our AI code generation, showcasing how it can significantly streamline the development process.")
                }
                contentCorrect
            }
            Thread.sleep(2000)
        }
    speak("This concludes our AI Coder demo of the 'Create File from Description' feature. We've successfully demonstrated how this powerful tool allows developers to generate entire Kotlin files using simple English instructions. By leveraging AI in this way, we can significantly speed up development, reduce boilerplate code, and enhance overall productivity. Thank you for joining us for this demonstration of AI-assisted coding!")
    Thread.sleep(10000) // Final sleep of 10 seconds
    }

}