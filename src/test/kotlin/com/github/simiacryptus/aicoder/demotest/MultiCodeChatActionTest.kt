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
    speak("Welcome to the AI Coder demo. Today, we'll be exploring the powerful Multi-Code Chat feature, which allows us to analyze multiple code files simultaneously. This innovative tool significantly enhances our ability to understand and improve complex codebases.")
        log.info("Starting testMultiCodeChatAction")
        Thread.sleep(2000)

        step("Open project view") {
        speak("Let's begin by opening the project view to access our code files. This is typically the first step in navigating our project structure.")
            find(CommonContainerFixture::class.java, byXpath("//div[@class='ProjectViewTree']")).click()
            log.info("Project view opened")
            Thread.sleep(2000)
        }

        step("Select multiple Kotlin files") {
        speak("Now, we'll select a Kotlin file in our project structure. This will be the starting point for our Multi-Code Chat. Notice how easily we can navigate through our project hierarchy.")
            val projectTree = find(JTreeFixture::class.java, byXpath("//div[@class='ProjectViewTree']"))
            projectTree.clickPath(*arrayOf("TestProject", "src", "main", "kotlin"), fullMatch = false)
            // Select a single file
            projectTree.clickPath(*arrayOf("TestProject", "src", "main", "kotlin", "Person"), fullMatch = false)
            log.info("Kotlin file selected")
            Thread.sleep(3000)
        }

        step("Open context menu") {
        speak("To access the AI Coder features, we'll open the context menu by right-clicking on our selected file. This contextual approach allows for seamless integration of AI capabilities into your workflow.")
            val projectTree = find(JTreeFixture::class.java, byXpath("//div[@class='ProjectViewTree']"))
            projectTree.rightClick()
            log.info("Context menu opened via right-click")
            Thread.sleep(2000)
        }

        step("Select 'AI Coder' menu") {
        speak("In the context menu, you'll notice the AI Coder option. Let's select it to reveal more AI-powered features. This menu centralizes all the AI capabilities, making them easily accessible.")
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

        step("Click 'Multi-Code Chat' action") {
        speak("Among the AI Coder options, we'll find and select the 'Multi-Code Chat' action. This powerful feature allows us to analyze multiple code files in a single conversation, significantly streamlining our code review and analysis process.")
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
            Thread.sleep(3000)
        }

        step("Get URL from UDP messages") {
            val messages = getReceivedMessages()
            val url = messages.firstOrNull { it.startsWith("http") }
            if (url != null) {
                log.info("Retrieved URL: $url")
            speak("The Multi-Code Chat interface has been launched in a new browser window. Let's interact with it. This seamless integration between your IDE and a web interface provides a user-friendly experience for AI-assisted coding.")
                val options = ChromeOptions()
                options.addArguments("--start-fullscreen")
                driver = ChromeDriver(options)
                (driver as JavascriptExecutor).executeScript("document.body.style.zoom='150%'")
                driver.get(url)
                val wait = WebDriverWait(this@MultiCodeChatActionTest.driver, Duration.ofSeconds(10))
                val chatInput = wait.until<WebElement>(ExpectedConditions.elementToBeClickable(By.id("chat-input")))
                chatInput.click()
                Thread.sleep(2000)
            speak("In the chat interface, we'll type our request to analyze the selected code. Notice how we can use natural language to communicate with the AI, making the interaction intuitive and efficient.")
                val request = "Analyze this class"
                request.forEach { char ->
                    chatInput.sendKeys(char.toString())
                    Thread.sleep(100) // Add a small delay between each character
                }
                Thread.sleep(3000) // Pause after typing the full request
                val submitButton = wait.until<WebElement>(ExpectedConditions.elementToBeClickable(By.xpath("//button[@type='submit']")))
            speak("Now, we'll submit our request to the AI for analysis. This simple action initiates a sophisticated process of code understanding and analysis.")
                log.info("Submitting request to AI")
                submitButton.click()
                Thread.sleep(3000) // Short pause after clicking submit
            speak("The AI is now analyzing our code. This process typically takes just a few seconds, showcasing the efficiency of the Multi-Code Chat feature. While we wait, it's worth noting how this feature can significantly reduce the time spent on code review and understanding complex codebases.")
                // Wait for the response to be generated with a longer timeout
                val longWait = WebDriverWait(this@MultiCodeChatActionTest.driver, Duration.ofSeconds(60))
                try {
                    val markdownTab = longWait.until<WebElement>(ExpectedConditions.elementToBeClickable(By.xpath("(//button[contains(@class, 'tab-button') and contains(text(), 'Markdown')])[3]")))
                    Thread.sleep(2000)
                speak("Excellent! The AI has generated its response. For better readability, we can view the response in Markdown format by clicking this tab. This feature demonstrates the AI's ability to provide structured and easily digestible information.")
                    markdownTab.click()
                    Thread.sleep(3000)
                    // Simulate mouseover on the upper-right corner of the message container
                    val messageContainer =
                        longWait.until<WebElement>(ExpectedConditions.presenceOfElementLocated(By.xpath("(//div[contains(@class, 'message-container')])[last()]")))
                    val actions = org.openqa.selenium.interactions.Actions(this@MultiCodeChatActionTest.driver)
                    try {
                        actions.moveToElement(messageContainer).perform()
                    speak("Notice the options available for each message, allowing you to copy, edit, or perform other actions on the AI's response. This flexibility enables seamless integration of AI insights into your development workflow.")
                        Thread.sleep(3000)
                    } catch (e: Exception) {
                        log.warn("Failed to move to message container: ${e.message}")
                    }
                } catch (e: Exception) {
                    log.warn("Copy button not found within the expected time. Skipping copy action.", e)
                speak("It seems the AI response is taking a bit longer than usual. This can happen with more complex code analyses. In a real-world scenario, we might refresh the page or check our network connection. Let's move on to the next step.")
                    Thread.sleep(3000)
                }

                Thread.sleep(3000) // Wait for the hide action to complete
            speak("We've successfully interacted with the Multi-Code Chat interface, demonstrating how easily you can get AI-powered insights into your code. This feature significantly enhances code comprehension and can boost developer productivity.")
                this@MultiCodeChatActionTest.driver.quit()
            } else {
                log.error("No URL found in UDP messages")
            speak("We've encountered a small hiccup in retrieving the Multi-Code Chat URL. Don't worry, this is an uncommon occurrence. In a real-world scenario, we would retry the action or contact our support team for assistance.")
                Thread.sleep(3000)
            }
            clearMessageBuffer()
        }

    speak("This concludes our AI Coder Multi-Code Chat demo. We've successfully demonstrated how to initiate a Multi-Code Chat session, submit a code analysis request, and interact with the AI's response. This powerful feature can significantly enhance your coding efficiency, improve code quality, and deepen your understanding of complex codebases. Thank you for joining us for this demonstration of the Multi-Code Chat feature.")
    Thread.sleep(10000) // Final sleep of 10 seconds
    }
}