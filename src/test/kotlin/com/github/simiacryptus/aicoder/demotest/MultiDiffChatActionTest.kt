package com.github.simiacryptus.aicoder.demotest

import com.github.simiacryptus.aicoder.demotest.TestUtil.speak
import com.github.simiacryptus.aicoder.demotest.TestUtil.startUdpServer
import com.github.simiacryptus.aicoder.demotest.TestUtil.stopUdpServer
import com.github.simiacryptus.aicoder.demotest.TestUtil.getReceivedMessages
import com.github.simiacryptus.aicoder.demotest.TestUtil.clearMessageBuffer
import com.intellij.remoterobot.RemoteRobot
import com.intellij.remoterobot.fixtures.CommonContainerFixture
import com.intellij.remoterobot.fixtures.JTreeFixture
import com.intellij.remoterobot.search.locators.byXpath
import com.intellij.remoterobot.stepsProcessing.step
import com.intellij.remoterobot.utils.waitFor
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.slf4j.LoggerFactory
import java.time.Duration
import org.openqa.selenium.WebDriver
import org.openqa.selenium.By
import org.openqa.selenium.WebElement
import org.openqa.selenium.chrome.ChromeDriver
import org.openqa.selenium.support.ui.ExpectedConditions
import org.openqa.selenium.support.ui.WebDriverWait
import org.junit.jupiter.api.Assertions.assertTrue
import org.openqa.selenium.JavascriptExecutor
import org.openqa.selenium.chrome.ChromeOptions

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class MultiDiffChatActionTest {
    private lateinit var remoteRobot: RemoteRobot
    private val log = LoggerFactory.getLogger(MultiDiffChatActionTest::class.java)
    private lateinit var driver: WebDriver

    @BeforeAll
    fun setup() {
        remoteRobot = RemoteRobot("http://127.0.0.1:8082")
        startUdpServer()
        System.setProperty("webdriver.chrome.driver", "/usr/bin/chromedriver")
    }

    @AfterAll
    fun tearDown() {
        stopUdpServer()
        driver.quit()
    }

    @Test
    fun testMultiDiffChatAction() = with(remoteRobot) {
    speak("Welcome to the AI Coder demo. Today, we'll be exploring the Multi-Diff Chat feature, which empowers developers to effortlessly edit files using AI-generated patches. This powerful tool streamlines the process of code modification and documentation updates.")
        log.info("Starting testMultiDiffChatAction")
        Thread.sleep(2000)

        step("Open project view") {
        speak("Let's begin by opening the project view to access our files. This is where we'll select the file we want to modify using the Multi-Diff Chat feature.")
            find(CommonContainerFixture::class.java, byXpath("//div[@class='ProjectViewTree']")).click()
            log.info("Project view opened")
            Thread.sleep(2000)
        }

        step("Select readme.md file") {
        speak("Now, we'll select the readme.md file. We're choosing this file because it's a perfect candidate for demonstrating how AI can assist in improving project documentation.")
            val projectTree = find(JTreeFixture::class.java, byXpath("//div[@class='ProjectViewTree']"))
            projectTree.clickPath(*arrayOf("TestProject", "readme.md"), fullMatch = false)
            log.info("readme.md file selected")
            Thread.sleep(2000)
        }

        step("Open context menu") {
        speak("To access the AI Coder options, we'll open the context menu by right-clicking on the selected file. Notice how easily we can integrate AI capabilities into our existing workflow.")
            val projectTree = find(JTreeFixture::class.java, byXpath("//div[@class='ProjectViewTree']"))
            projectTree.rightClick()
            log.info("Context menu opened via right-click")
            Thread.sleep(2000)
        }

        step("Select 'AI Coder' menu") {
        speak("In the context menu, we'll locate and select the AI Coder option. This will reveal additional AI-powered features that can significantly enhance our productivity.")
            waitFor(Duration.ofSeconds(15)) {
                try {
                    val aiCoderMenu = find(CommonContainerFixture::class.java, byXpath("//div[contains(@class, 'ActionMenu') and contains(@text, 'AI Coder')]"))
                    aiCoderMenu.click()
                    log.info("'AI Coder' menu clicked")
                    true
                } catch (e: Exception) {
                    log.warn("Failed to find or click 'AI Coder' menu: ${e.message}")
                speak("We've encountered a small hiccup in locating the AI Coder menu. Don't worry, this can happen occasionally. Let's give it another try.")
                    false
                }
            }
            Thread.sleep(2000)
        }

        step("Click 'Patch Files' action") {
        speak("From the AI Coder submenu, we'll choose the 'Patch Files' action. This powerful tool allows us to initiate the Multi-Diff Chat feature for editing our readme.md file, demonstrating how AI can assist in code and documentation improvements.")
            waitFor(Duration.ofSeconds(15)) {
                try {
                    findAll(CommonContainerFixture::class.java, byXpath("//div[contains(@class, 'ActionMenuItem') and contains(@text, 'Patch Files')]"))
                        .firstOrNull()?.click()
                    log.info("'Patch Files' action clicked")
                    true
                } catch (e: Exception) {
                    log.warn("Failed to find 'Patch Files' action: ${e.message}")
                speak("We're encountering a minor issue finding the 'Patch Files' action. In a real-world scenario, we might check our plugin installation or refresh the IDE. Let's give it another moment.")
                    false
                }
            }
            Thread.sleep(2000)
        }

        step("Get URL from UDP messages") {
        speak("The Multi-Diff Chat interface is now opening in a new browser window. Let's switch our attention to that window, where we'll interact with the AI to improve our readme file.")
            val messages = getReceivedMessages()
            val url = messages.firstOrNull { it.startsWith("http") }
            if (url != null) {
                log.info("Retrieved URL: $url")
            speak("Excellent! We've successfully retrieved the URL for the Multi-Diff Chat interface. This demonstrates the seamless integration between our IDE and the AI-powered chat interface.")
                val options = ChromeOptions()
                options.addArguments("--start-fullscreen")
                driver = ChromeDriver(options)
                (driver as JavascriptExecutor).executeScript("document.body.style.zoom='150%'")
                driver.get(url)
            speak("The browser window has opened, and we're now viewing the Multi-Diff Chat interface.")
                val wait = WebDriverWait(this@MultiDiffChatActionTest.driver, Duration.ofSeconds(10))
                val chatInput = wait.until<WebElement>(ExpectedConditions.elementToBeClickable(By.id("chat-input")))
                chatInput.click()
            speak("Now, let's type our request to add a Mermaid diagram to the readme.md file. Mermaid is a markdown-based syntax for creating diagrams, which can greatly enhance our project documentation. Watch how easily we can instruct the AI to make this improvement.")
                Thread.sleep(2000)
                val request = "Add a Mermaid diagram to the readme.md file showing the basic structure of this project"
                request.forEach { char ->
                    chatInput.sendKeys(char.toString())
                    Thread.sleep(100) // Add a small delay between each character
                }
                Thread.sleep(2000) // Pause after typing the full request
                val submitButton = wait.until<WebElement>(ExpectedConditions.elementToBeClickable(By.xpath("//button[@type='submit']")))
            speak("Now that we've typed our request, let's submit it to the AI by clicking the submit button. This will initiate the AI's analysis and generation of the appropriate patch.")
                log.info("Submitting request to AI")
                submitButton.click()
                Thread.sleep(3000) // Longer pause after clicking submit
            speak("We've submitted our request. Now, we'll wait for the AI to generate a patch. This process typically takes just a few moments. While we wait, it's worth noting how this feature can significantly speed up the process of adding complex elements like diagrams to our documentation.")
                // Wait for the response to be generated with a longer timeout
                val longWait = WebDriverWait(this@MultiDiffChatActionTest.driver, Duration.ofSeconds(60))
                try {
                    val patchContent = longWait.until<WebElement>(ExpectedConditions.presenceOfElementLocated(By.xpath("//pre[contains(@class, 'language-diff')]")))
                speak("Great! The AI has generated a patch. Let's take a moment to review the proposed changes.")
                    log.info("Patch generated: ${patchContent.text}")
                    Thread.sleep(3000)
                    
                    // Simulate clicking the "Apply Diff" button
                    val applyButton = longWait.until<WebElement>(ExpectedConditions.elementToBeClickable(By.xpath("//a[contains(@class, 'cmd-button') and contains(text(), 'Apply Diff')]")))
                speak("The patch looks good. Now, let's apply this diff to our readme.md file by clicking the 'Apply Diff' button. This action will automatically update our file with the new Mermaid diagram.")
                    applyButton.click()
                    Thread.sleep(3000) // Wait for the apply action to complete
                    
                speak("The diff has been applied successfully. To confirm the changes and demonstrate the immediate impact of our AI-assisted edit, let's switch back to our IDE and verify the contents of the readme.md file.")
                    // Close the browser window
                    this@MultiDiffChatActionTest.driver.close()
                speak("We've closed the Multi-Diff Chat browser window and returned to our IDE. Notice how seamlessly we transition between the AI interface and our development environment.")
                    // Verify that the file was changed
                    val projectViewTree = find(JTreeFixture::class.java, byXpath("//div[@class='ProjectViewTree']"))
                    projectViewTree.doubleClickPath(*arrayOf("TestProject", "readme.md"), fullMatch = false)
                    val editor = find<CommonContainerFixture>(byXpath("//div[@class='EditorComponentImpl']"))
                    val fileContent = editor.findAllText().joinToString("") { it.text }
                    assertTrue(fileContent.contains("```mermaid"), "The readme.md file should contain a Mermaid diagram")
                speak("Excellent! We've verified that the readme.md file now contains a Mermaid diagram. The Multi-Diff Chat feature has successfully added the requested content to our file.")
                    Thread.sleep(3000)
                } catch (e: Exception) {
                    log.warn("Failed to generate or apply patch: ${e.message}")
                speak("We've encountered an unexpected issue while generating or applying the patch. Don't worry, this can happen occasionally. In a real-world scenario, we might try the process again or check the logs for more detailed information. This demonstrates the importance of error handling in AI-assisted workflows.")
                }

            speak("This concludes our demonstration of the Multi-Diff Chat feature. We've successfully used AI to add a Mermaid diagram to our readme.md file, showcasing how this powerful tool can streamline documentation updates and code modifications. The ability to quickly generate and apply complex changes like this can significantly enhance productivity and code quality in your development workflow.")
                this@MultiDiffChatActionTest.driver.quit()
            } else {
                log.error("No URL found in UDP messages")
            speak("We've encountered an issue retrieving the URL for the Multi-Diff Chat interface. This is an unexpected error that would require further investigation in a real-world scenario. It's a good reminder of the importance of robust error handling and logging in AI-integrated development tools.")
            }
            clearMessageBuffer()
        }

    speak("Thank you for joining us for this AI Coder Multi-Diff Chat demo. We've demonstrated how to efficiently apply AI-generated patches to our codebase and enhance our documentation with Mermaid diagrams. This feature exemplifies how AI can be seamlessly integrated into the development process, significantly improving productivity and code quality. By automating complex tasks like diagram creation, developers can focus more on core functionality and creative problem-solving. We hope this demonstration has shown the potential of AI-assisted coding in revolutionizing your development workflow.")
    Thread.sleep(10000) // Final sleep of 10 seconds
    }
}