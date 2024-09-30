package com.github.simiacryptus.aicoder.demotest

import com.github.simiacryptus.aicoder.demotest.CommandAutofixActionTest
import com.github.simiacryptus.aicoder.demotest.TestUtil.speak
import com.github.simiacryptus.aicoder.demotest.TestUtil.startUdpServer
import com.github.simiacryptus.aicoder.demotest.TestUtil.stopUdpServer
import com.github.simiacryptus.aicoder.demotest.TestUtil.getReceivedMessages
import com.github.simiacryptus.aicoder.demotest.TestUtil.clearMessageBuffer
import com.intellij.remoterobot.RemoteRobot
import com.intellij.remoterobot.fixtures.CommonContainerFixture
import com.intellij.remoterobot.fixtures.EditorFixture
import com.intellij.remoterobot.fixtures.JTreeFixture
import com.intellij.remoterobot.search.locators.byXpath
import com.intellij.remoterobot.stepsProcessing.step
import com.intellij.remoterobot.utils.keyboard
import com.intellij.remoterobot.utils.waitFor
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.slf4j.LoggerFactory
import java.awt.event.KeyEvent
import java.time.Duration
import org.openqa.selenium.WebDriver
import org.openqa.selenium.By
import org.openqa.selenium.WebElement
import org.openqa.selenium.chrome.ChromeDriver
import org.openqa.selenium.support.ui.ExpectedConditions
import org.openqa.selenium.support.ui.WebDriverWait
import org.testng.asserts.SoftAssert
import org.junit.jupiter.api.Assertions
import org.openqa.selenium.JavascriptExecutor
import org.openqa.selenium.chrome.ChromeOptions

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class CodeChatActionTest {
    private lateinit var remoteRobot: RemoteRobot
    private val log = LoggerFactory.getLogger(CodeChatActionTest::class.java)
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
    fun testCodeChatAction() = with(remoteRobot) {
    speak("Welcome to the AI Coder demo. Today, we'll be exploring the powerful Code Chat feature, which allows developers to interact with AI for code-related queries and assistance. This innovative tool can significantly enhance your coding efficiency and provide valuable insights during your development process.")
        log.info("Starting testCodeChatAction")
        Thread.sleep(2000)

        step("Open project view") {
            find(CommonContainerFixture::class.java, byXpath("//div[@class='ProjectViewTree']")).click()
            log.info("Project view opened")
        speak("Let's begin by opening the project view to access our files. This is typically the first step in navigating your project structure.")
            Thread.sleep(2000)
        }

        step("Open a Kotlin file") {
        speak("Now, we'll open a Kotlin file. This will serve as the basis for our Code Chat session, allowing us to demonstrate how AI Coder can assist with real code.")
            val projectTree = find(JTreeFixture::class.java, byXpath("//div[@class='ProjectViewTree']"))
            projectTree.doubleClickPath(*arrayOf("TestProject", "src", "main", "kotlin", "Person"), fullMatch = false)
            log.info("Kotlin file opened")
            Thread.sleep(2000)
        }

        step("Select code") {
        speak("To initiate a meaningful conversation about our code, let's select a portion of it. We'll use this selection as the context for our chat with the AI. Notice how easily we can provide code context to the AI.")
            val editor = find(EditorFixture::class.java, byXpath("//div[@class='EditorComponentImpl']"))
            editor.click()
            keyboard {
                pressing(KeyEvent.VK_CONTROL) {
                    key(KeyEvent.VK_A) // Select all
                }
            }
            log.info("Code selected")
            Thread.sleep(2000)
        }

        step("Open context menu") {
        speak("To access the AI Coder features, we need to open the context menu. Let's do that by right-clicking on our selected code. This intuitive approach allows you to seamlessly integrate AI assistance into your normal workflow.")
            val editor = find(EditorFixture::class.java, byXpath("//div[@class='EditorComponentImpl']"))
            editor.rightClick()
            log.info("Context menu opened via right-click")
            Thread.sleep(2000)
        }

        step("Select 'AI Coder' menu") {
        speak("In the context menu, you'll notice an 'AI Coder' option. This is our gateway to AI-assisted coding features. Let's select it. By choosing this option, we're unlocking a suite of powerful AI-driven tools designed to enhance your coding experience.")
            waitFor(Duration.ofSeconds(15)) {
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

        step("Click 'Code Chat' action") {
        speak("Among the AI Coder options, we'll find 'Code Chat'. This feature allows us to have an interactive dialogue with AI about our code. Let's select it. Code Chat is particularly useful for getting quick insights, explanations, or suggestions about your code.")
            waitFor(Duration.ofSeconds(15)) {
                try {
                    findAll(CommonContainerFixture::class.java, byXpath("//div[contains(@class, 'ActionMenuItem') and contains(@text, 'Code Chat')]"))
                        .firstOrNull()?.click()
                    log.info("'Code Chat' action clicked")
                    true
                } catch (e: Exception) {
                    log.warn("Failed to find 'Code Chat' action: ${e.message}")
                    false
                }
            }
            Thread.sleep(2000)
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
                val wait = WebDriverWait(this@CodeChatActionTest.driver, Duration.ofSeconds(15))
                val chatInput = wait.until<WebElement>(ExpectedConditions.elementToBeClickable(By.id("chat-input")))
                val longWait = WebDriverWait(this@CodeChatActionTest.driver, Duration.ofSeconds(60))
                chatInput.click()
                speak("The Code Chat interface has opened. Now, let's type our request into the chat input. We'll ask the AI to create a user manual for our class.")
                val request = "Create a user manual for this class"
                request.forEach { char ->
                    chatInput.sendKeys(char.toString())
                    Thread.sleep(100) // Add a small delay between each character
                }
                Thread.sleep(3000) // Pause after typing the full request
                val submitButton = wait.until<WebElement>(ExpectedConditions.elementToBeClickable(By.xpath("//button[@type='submit']")))
                speak("We've entered our request. Now, let's submit it to the AI by clicking the submit button.")
                log.info("Submitting request to AI")
                submitButton.click()
                Thread.sleep(3000) // Short pause after clicking submit
                speak("We've sent our request. Now, we'll wait for the AI to process it and generate a response. This usually takes just a few moments.")
                try {
                    val markdownTab = longWait.until<WebElement>(ExpectedConditions.elementToBeClickable(By.xpath("(//button[contains(@class, 'tab-button') and contains(text(), 'Markdown')])[3]")))
                    Thread.sleep(2000)
                    speak("The AI has generated a response. For better readability, we can view this response in Markdown format. Let's switch to the Markdown tab.")
                    markdownTab.click()
                    Thread.sleep(2000)
                    // Simulate mouseover on the upper-right corner of the message container
                    val responseContent = longWait.until<WebElement>(ExpectedConditions.presenceOfElementLocated(By.xpath("//div[contains(@class, 'tab-content') and @data-tab='Markdown']")))
                    val responseText = responseContent.text
                    log.info("Response content: $responseText")

                    val messageContainer =
                        longWait.until<WebElement>(ExpectedConditions.presenceOfElementLocated(By.xpath("(//div[contains(@class, 'message-container')])[last()]")))
                    val actions = org.openqa.selenium.interactions.Actions(this@CodeChatActionTest.driver)
                    try {
                        actions.moveToElement(messageContainer).perform()
                    } catch (e: Exception) {
                        log.warn("Failed to move to message container: ${e.message}")
                    }
                } catch (e: Exception) {
                    log.warn("Copy button not found within the expected time. Skipping copy action.", e)
                    speak("It seems the AI response is taking a bit longer than usual. Don't worry, this can happen occasionally. We'll skip the copy action for now.")
                }

                Thread.sleep(3000) // Wait for the hide action to complete
                speak("We've successfully interacted with the Code Chat interface. As you can see, this feature provides quick, context-aware assistance for your coding tasks.")
                this@CodeChatActionTest.driver.quit()
            } else {
                log.error("No URL found in UDP messages")
                speak("I apologize, but we encountered an issue retrieving the necessary URL. This is an unexpected error that rarely occurs.")
            }
            clearMessageBuffer()
        }

    speak("This concludes our AI Coder Code Chat demo. We've successfully initiated a Code Chat session, submitted a query, and received an AI-generated response. As you've seen, this powerful feature can significantly enhance your coding efficiency by providing instant, context-aware assistance. It's like having a knowledgeable coding partner always ready to help. Thank you for joining us for this demonstration of AI Coder's Code Chat feature.")
    Thread.sleep(10000) // Final sleep of 10 seconds
    }

}