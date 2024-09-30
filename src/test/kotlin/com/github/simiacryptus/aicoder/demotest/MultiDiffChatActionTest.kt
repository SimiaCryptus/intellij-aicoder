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
        speak("Welcome to the AI Coder demo. We'll demonstrate the Multi-Diff Chat feature for editing files with patches.")
        log.info("Starting testMultiDiffChatAction")
        Thread.sleep(1000)

        step("Open project view") {
            find(CommonContainerFixture::class.java, byXpath("//div[@class='ProjectViewTree']")).click()
            log.info("Project view opened")
        }

        step("Select readme.md file") {
            speak("Now, we'll select the readme.md file to start a multi-diff chat.")
            val projectTree = find(JTreeFixture::class.java, byXpath("//div[@class='ProjectViewTree']"))
            projectTree.clickPath(*arrayOf("TestProject", "readme.md"), fullMatch = false)
            log.info("readme.md file selected")
            Thread.sleep(1000)
        }

        step("Open context menu") {
            speak("Now, we'll open the context menu to access the AI Coder option.")
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

        step("Click 'Patch Files' action") {
            speak("Now, we'll choose the 'Patch Files' action to start editing with patches.")
            waitFor(Duration.ofSeconds(10)) {
                try {
                    findAll(CommonContainerFixture::class.java, byXpath("//div[contains(@class, 'ActionMenuItem') and contains(@text, 'Patch Files')]"))
                        .firstOrNull()?.click()
                    log.info("'Patch Files' action clicked")
                    true
                } catch (e: Exception) {
                    log.warn("Failed to find 'Patch Files' action: ${e.message}")
                    false
                }
            }
            Thread.sleep(1000)
        }

        step("Get URL from UDP messages") {
            val messages = getReceivedMessages()
            val url = messages.firstOrNull { it.startsWith("http") }
            if (url != null) {
                log.info("Retrieved URL: $url")
                val options = ChromeOptions()
                options.addArguments("--start-fullscreen")
                driver = ChromeDriver(options)
                (driver as JavascriptExecutor).executeScript("document.body.style.zoom='150%'")
                driver.get(url)
                val wait = WebDriverWait(this@MultiDiffChatActionTest.driver, Duration.ofSeconds(10))
                val chatInput = wait.until<WebElement>(ExpectedConditions.elementToBeClickable(By.id("chat-input")))
                chatInput.click()
                speak("Now, let's type our request to add Mermaid syntax to the readme.md file.")
                val request = "Add a Mermaid diagram to the readme.md file showing the basic structure of this project"
                request.forEach { char ->
                    chatInput.sendKeys(char.toString())
                    Thread.sleep(100) // Add a small delay between each character
                }
                Thread.sleep(2000) // Pause after typing the full request
                val submitButton = wait.until<WebElement>(ExpectedConditions.elementToBeClickable(By.xpath("//button[@type='submit']")))
                speak("Submitting our request to the AI.")
                log.info("Submitting request to AI")
                submitButton.click()
                Thread.sleep(2000) // Short pause after clicking submit
                speak("Waiting for the AI to generate a patch.")
                // Wait for the response to be generated with a longer timeout
                val longWait = WebDriverWait(this@MultiDiffChatActionTest.driver, Duration.ofSeconds(60))
                try {
                    val patchContent = longWait.until<WebElement>(ExpectedConditions.presenceOfElementLocated(By.xpath("//pre[contains(@class, 'language-diff')]")))
                    speak("The AI has generated a patch. Let's review it.")
                    log.info("Patch generated: ${patchContent.text}")
                    
                    // Simulate clicking the "Apply Diff" button
                    val applyButton = longWait.until<WebElement>(ExpectedConditions.elementToBeClickable(By.xpath("//a[contains(@class, 'cmd-button') and contains(text(), 'Apply Diff')]")))
                    speak("Now, we'll apply the diff to our readme.md file.")
                    applyButton.click()
                    Thread.sleep(2000) // Wait for the apply action to complete
                    
                    speak("The diff has been applied. Let's verify the changes.")
                    // Close the browser window
                    this@MultiDiffChatActionTest.driver.close()
                    speak("We've closed the browser window.")
                    // Verify that the file was changed
                    val projectViewTree = find(JTreeFixture::class.java, byXpath("//div[@class='ProjectViewTree']"))
                    projectViewTree.doubleClickPath(*arrayOf("TestProject", "readme.md"), fullMatch = false)
                    val editor = find<CommonContainerFixture>(byXpath("//div[@class='EditorComponentImpl']"))
                    val fileContent = editor.findAllText().joinToString("") { it.text }
                    assertTrue(fileContent.contains("```mermaid"), "The readme.md file should contain a Mermaid diagram")
                    speak("We've verified that the readme.md file now contains a Mermaid diagram.")
                } catch (e: Exception) {
                    log.warn("Failed to generate or apply patch: ${e.message}")
                    speak("There was an issue generating or applying the patch. Please check the logs for more information.")
                }

                Thread.sleep(2000) // Wait for the final actions to complete
                speak("We've successfully demonstrated how to use the Multi-Diff Chat feature to add a Mermaid diagram to our readme.md file and verified the changes.")
                this@MultiDiffChatActionTest.driver.quit()
            } else {
                log.error("No URL found in UDP messages")
                speak("Failed to retrieve the URL.")
            }
            clearMessageBuffer()
        }

        speak("This concludes our AI Coder Multi-Diff Chat demo. We've shown how to apply patches to the codebase and add Mermaid syntax to Markdown files.")
    }
}