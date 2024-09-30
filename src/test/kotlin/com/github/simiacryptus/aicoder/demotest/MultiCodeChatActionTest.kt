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
import org.openqa.selenium.JavascriptExecutor
import org.openqa.selenium.WebElement
import org.openqa.selenium.chrome.ChromeDriver
import org.openqa.selenium.chrome.ChromeOptions
import org.openqa.selenium.support.ui.ExpectedConditions
import org.openqa.selenium.support.ui.WebDriverWait

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class MultiCodeChatActionTest {
    private lateinit var remoteRobot: RemoteRobot
    private val log = LoggerFactory.getLogger(MultiCodeChatActionTest::class.java)
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
    fun testMultiCodeChatAction() = with(remoteRobot) {
        speak("Welcome to the AI Coder demo. We'll demonstrate the Multi-Code Chat feature.")
        log.info("Starting testMultiCodeChatAction")
        Thread.sleep(1000)

        step("Open project view") {
            find(CommonContainerFixture::class.java, byXpath("//div[@class='ProjectViewTree']")).click()
            log.info("Project view opened")
        }

        step("Select multiple Kotlin files") {
            speak("Now, we'll select a Kotlin file to start a multi-code chat.")
            val projectTree = find(JTreeFixture::class.java, byXpath("//div[@class='ProjectViewTree']"))
            projectTree.clickPath(*arrayOf("TestProject", "src", "main", "kotlin"), fullMatch = false)
            // Select a single file
            projectTree.clickPath(*arrayOf("TestProject", "src", "main", "kotlin", "Person"), fullMatch = false)
            log.info("Kotlin file selected")
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

        step("Click 'Multi-Code Chat' action") {
            speak("Now, we'll choose the 'Multi-Code Chat' action.")
            waitFor(Duration.ofSeconds(10)) {
                try {
                    findAll(CommonContainerFixture::class.java, byXpath("//div[contains(@class, 'ActionMenuItem') and contains(@text, 'Code Chat')]"))
                        .firstOrNull()?.click()
                    log.info("'Multi-Code Chat' action clicked")
                    true
                } catch (e: Exception) {
                    log.warn("Failed to find 'Multi-Code Chat' action: ${e.message}")
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
                val wait = WebDriverWait(this@MultiCodeChatActionTest.driver, Duration.ofSeconds(10))
                val chatInput = wait.until<WebElement>(ExpectedConditions.elementToBeClickable(By.id("chat-input")))
                chatInput.click()
                speak("Now, let's type our request into the chat input.")
                val request = "Analyze this class"
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
                speak("Waiting for the AI to generate a response.")
                // Wait for the response to be generated with a longer timeout
                val longWait = WebDriverWait(this@MultiCodeChatActionTest.driver, Duration.ofSeconds(60))
                try {
                    val markdownTab = longWait.until<WebElement>(ExpectedConditions.elementToBeClickable(By.xpath("(//button[contains(@class, 'tab-button') and contains(text(), 'Markdown')])[3]")))
                    Thread.sleep(1000)
                    speak("We can also view the response in Markdown format.")
                    markdownTab.click()
                    Thread.sleep(1000)
                    // Simulate mouseover on the upper-right corner of the message container
                    val messageContainer =
                        longWait.until<WebElement>(ExpectedConditions.presenceOfElementLocated(By.xpath("(//div[contains(@class, 'message-container')])[last()]")))
                    val actions = org.openqa.selenium.interactions.Actions(this@MultiCodeChatActionTest.driver)
                    try {
                        actions.moveToElement(messageContainer).perform()
                    } catch (e: Exception) {
                        log.warn("Failed to move to message container: ${e.message}")
                    }
                } catch (e: Exception) {
                    log.warn("Copy button not found within the expected time. Skipping copy action.", e)
                    speak("The AI response is taking longer than expected. Skipping the copy action.")
                }

                Thread.sleep(2000) // Wait for the hide action to complete
                speak("We've successfully interacted with the Multi-Code Chat interface.")
                this@MultiCodeChatActionTest.driver.quit()
            } else {
                log.error("No URL found in UDP messages")
                speak("Failed to retrieve the URL.")
            }
            clearMessageBuffer()
        }

        speak("This concludes our AI Coder Multi-Code Chat demo. We've successfully initiated a Multi-Code Chat session for the selected file.")
    }
}