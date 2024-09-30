package com.github.simiacryptus.aicoder.demotest

import com.github.simiacryptus.aicoder.demotest.TestUtil.speak
import com.github.simiacryptus.aicoder.demotest.TestUtil.startUdpServer
import com.github.simiacryptus.aicoder.demotest.TestUtil.stopUdpServer
import com.github.simiacryptus.aicoder.demotest.TestUtil.getReceivedMessages
import com.github.simiacryptus.aicoder.demotest.TestUtil.clearMessageBuffer
import com.intellij.remoterobot.RemoteRobot
import com.intellij.remoterobot.fixtures.CommonContainerFixture
import com.intellij.remoterobot.fixtures.JCheckboxFixture
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
import java.time.Duration
import org.openqa.selenium.WebDriver
import org.openqa.selenium.By
import org.openqa.selenium.JavascriptExecutor
import org.openqa.selenium.WebElement
import org.openqa.selenium.chrome.ChromeDriver
import org.openqa.selenium.chrome.ChromeOptions
import org.openqa.selenium.support.ui.ExpectedConditions
import org.openqa.selenium.support.ui.WebDriverWait
import java.awt.event.KeyEvent
import java.awt.event.KeyEvent.*

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class CommandAutofixActionTest {
    private lateinit var remoteRobot: RemoteRobot
    private val log = LoggerFactory.getLogger(CommandAutofixActionTest::class.java)
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
    fun testCommandAutofixAction() = with(remoteRobot) {
        speak("Welcome to the AI Coder demo. We'll demonstrate the Command Autofix feature.")
        log.info("Starting testCommandAutofixAction")
        Thread.sleep(1000)

        step("Open project view") {
            find(CommonContainerFixture::class.java, byXpath("//div[@class='ProjectViewTree']")).click()
            log.info("Project view opened")
        }

        step("Select a directory") {
            speak("Now, we'll select a directory to start a Command Autofix operation.")
            val projectTree = find(JTreeFixture::class.java, byXpath("//div[@class='ProjectViewTree']"))
            projectTree.rightClickPath(*arrayOf("DataGnome"), fullMatch = false)
            log.info("Directory selected")
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

        step("Click 'Auto-Fix' action") {
            speak("Now, we'll choose the 'Auto-Fix' action.")
            waitFor(Duration.ofSeconds(10)) {
                try {
                    findAll(CommonContainerFixture::class.java, byXpath("//div[contains(@class, 'ActionMenuItem') and contains(@text, 'Auto-Fix')]"))
                        .firstOrNull()?.click()
                    log.info("'Auto-Fix' action clicked")
                    true
                } catch (e: Exception) {
                    log.warn("Failed to find 'Auto-Fix' action: ${e.message}")
                    false
                }
            }
            Thread.sleep(1000)
        }

        step("Configure Command Autofix") {
            speak("Let's configure the Command Autofix settings.")
            waitFor(Duration.ofSeconds(10)) {
                val dialog = find(CommonContainerFixture::class.java, byXpath("//div[@class='MyDialog' and @title='Command Autofix Settings']"))
                if (dialog.isShowing) {

                    // Select auto-apply fixes
                    val autoFixCheckbox = dialog.find(JCheckboxFixture::class.java, byXpath("//div[@class='JCheckBox' and @text='Auto-apply fixes']"))
                    autoFixCheckbox.select()

                    val okButton = dialog.find(CommonContainerFixture::class.java, byXpath("//div[@class='JButton' and @text='OK']"))
                    okButton.click()
                    log.info("Command Autofix configured and started")
                    speak("Command Autofix operation configured and started.")
                    true
                } else {
                    false
                }
            }
        }

        step("Interact with Command Autofix interface") {
            val messages = getReceivedMessages()
            val url = messages.firstOrNull { it.startsWith("http") }
            if (url != null) {
                log.info("Retrieved URL: $url")
                val options = ChromeOptions()
                options.addArguments("--start-fullscreen")
                this@CommandAutofixActionTest.driver = ChromeDriver(options)
                (this@CommandAutofixActionTest.driver as JavascriptExecutor).executeScript("document.body.style.zoom='150%'")
                this@CommandAutofixActionTest.driver.get(url)

                var attempt = 1
                while (attempt <= 5) {
                    val wait = WebDriverWait(this@CommandAutofixActionTest.driver, Duration.ofSeconds(90))
                    try {
                        blockUntilDone(wait)
                        speak("The Command Autofix agent has completed its operation.")
                        val codeElements = wait.until(ExpectedConditions.presenceOfAllElementsLocatedBy(By.tagName("code")))
                        val buildSuccessful = codeElements.any { it.text.contains("BUILD SUCCESSFUL") }
                        require(buildSuccessful) { "BUILD SUCCESSFUL not found in any code element" }
                        speak("The operation has completed successfully.")
                        break
                    } catch (e: Exception) {
                        attempt++
                        log.warn("Error interacting with Command Autofix interface", e)
                        blockUntilDone(wait)
                        speak("There was an error. Let's try again.")
                        (driver as JavascriptExecutor).executeScript("window.scrollTo(0, 0)")
                        // Find <a class="href-link">♻</a> and click it
                        val refreshButton = driver.findElement(By.xpath("//a[@class='href-link' and text()='♻']"))
                        refreshButton.click()
                        log.info("Refresh button clicked")
                        Thread.sleep(1000)
                        driver.findElements(By.cssSelector(".tabs-container > .tabs > .tab-button")).get(attempt-1).click()
                    }
                }
                this@CommandAutofixActionTest.driver.quit()
            } else {
                log.error("No URL found in UDP messages")
                speak("Failed to retrieve the URL for the Command Autofix interface.")
            }
            clearMessageBuffer()
        }

        speak("This concludes our AI Coder Command Autofix demo. We've successfully initiated and interacted with a Command Autofix operation.")
    }

    private fun blockUntilDone(wait: WebDriverWait) {
        wait.until<Boolean> {
            try {
                (driver as JavascriptExecutor).executeScript("window.scrollTo(0, document.body.scrollHeight)")
                val loadingElements = driver.findElements(By.xpath("//span[@class='sr-only' and text()='Loading...']"))
                loadingElements.all { !it.isDisplayed }
            } catch (e: Exception) {
                log.warn("Error waiting for loading elements", e)
                false
            }
        }
    }
}