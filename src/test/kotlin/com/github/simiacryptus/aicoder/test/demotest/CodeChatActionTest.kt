package com.github.simiacryptus.aicoder.test.demotest

import com.github.simiacryptus.aicoder.test.demotest.TestUtil.speak
import com.github.simiacryptus.aicoder.test.demotest.TestUtil.getReceivedMessages
import com.github.simiacryptus.aicoder.test.demotest.TestUtil.clearMessageBuffer
import com.intellij.remoterobot.fixtures.CommonContainerFixture
import com.intellij.remoterobot.fixtures.EditorFixture
import com.intellij.remoterobot.fixtures.JTreeFixture
import com.intellij.remoterobot.search.locators.byXpath
import com.intellij.remoterobot.stepsProcessing.step
import com.intellij.remoterobot.utils.keyboard
import com.intellij.remoterobot.utils.waitFor
import org.junit.jupiter.api.Test
import java.awt.event.KeyEvent
import java.time.Duration
import org.openqa.selenium.By
import org.openqa.selenium.WebElement
import org.openqa.selenium.chrome.ChromeDriver
import org.openqa.selenium.support.ui.ExpectedConditions
import org.openqa.selenium.support.ui.WebDriverWait
import org.openqa.selenium.JavascriptExecutor
import org.openqa.selenium.chrome.ChromeOptions
import org.slf4j.LoggerFactory


class CodeChatActionTest : BaseActionTest() {

    @Test
    fun testCodeChatAction() = with(remoteRobot) {
        speak("Welcome to the AI Coder demo. We'll explore the Code Chat feature, which enables AI interaction for code-related queries and assistance.")
        log.info("Starting testCodeChatAction")
        Thread.sleep(2000)

        step("Open project view") {
            find(CommonContainerFixture::class.java, byXpath("//div[@class='ProjectViewTree']")).click()
            log.info("Project view opened")
            speak("Opening the project view to access files.")
            Thread.sleep(2000)
        }

        step("Open a Kotlin file") {
            speak("Opening a Kotlin file for the Code Chat demonstration.")
            val projectTree = find(JTreeFixture::class.java, byXpath("//div[@class='ProjectViewTree']"))
            projectTree.doubleClickPath(*arrayOf("TestProject", "src", "main", "kotlin", "Person"), fullMatch = false)
            log.info("Kotlin file opened")
            Thread.sleep(2000)
        }

        step("Select code") {
            speak("Selecting code to provide context for the AI.")
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
            speak("Opening the context menu to access AI Coder features.")
            val editor = find(EditorFixture::class.java, byXpath("//div[@class='EditorComponentImpl']"))
            editor.rightClick()
            log.info("Context menu opened via right-click")
            Thread.sleep(2000)
        }

        step("Select 'AI Coder' menu") {
            speak("Selecting the 'AI Coder' option from the context menu.")
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
            speak("Initiating the 'Code Chat' feature for an interactive dialogue with AI about our code.")
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
                speak("Entering a request for the AI to create a user manual for our class.")
                val request = "Create a user manual for this class"
                request.forEach { char ->
                    chatInput.sendKeys(char.toString())
                    Thread.sleep(100) // Add a small delay between each character
                }
                Thread.sleep(3000) // Pause after typing the full request
                val submitButton = wait.until<WebElement>(ExpectedConditions.elementToBeClickable(By.xpath("//button[@type='submit']")))
                speak("Submitting the request to the AI.")
                log.info("Submitting request to AI")
                submitButton.click()
                Thread.sleep(3000) // Short pause after clicking submit
                speak("Waiting for the AI to process the request and generate a response.")
                try {
                    val markdownTab =
                        longWait.until<WebElement>(ExpectedConditions.elementToBeClickable(By.xpath("(//button[contains(@class, 'tab-button') and contains(text(), 'Markdown')])[3]")))
                    Thread.sleep(2000)
                    speak("Switching to the Markdown tab for better readability of the AI response.")
                    markdownTab.click()
                    Thread.sleep(2000)
                    // Simulate mouseover on the upper-right corner of the message container
                    val responseContent =
                        longWait.until<WebElement>(ExpectedConditions.presenceOfElementLocated(By.xpath("//div[contains(@class, 'tab-content') and @data-tab='Markdown']")))
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
                    speak("AI response is delayed. Skipping the copy action.")
                }

                Thread.sleep(3000) // Wait for the hide action to complete
                speak("We've successfully interacted with the Code Chat interface. As you can see, this feature provides quick, context-aware assistance for your coding tasks.")
                this@CodeChatActionTest.driver.quit()
            } else {
                log.error("No URL found in UDP messages")
                speak("Error: Unable to retrieve the necessary URL.")
            }
            clearMessageBuffer()
        }

        speak("Demo concluded. We've demonstrated initiating a Code Chat session, submitting a query, and receiving an AI-generated response.")
        Thread.sleep(5000) // Final sleep of 5 seconds
    }

    companion object {
        val log = LoggerFactory.getLogger(CodeChatActionTest::class.java)
    }
}