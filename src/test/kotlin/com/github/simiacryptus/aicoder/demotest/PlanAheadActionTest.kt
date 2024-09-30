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

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class PlanAheadActionTest {
    private lateinit var remoteRobot: RemoteRobot
    private val log = LoggerFactory.getLogger(PlanAheadActionTest::class.java)
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
    fun testPlanAheadAction() = with(remoteRobot) {
        speak("Welcome to the AI Coder demo. We'll demonstrate the Task Runner feature.")
        log.info("Starting testPlanAheadAction")
        Thread.sleep(1000)

        step("Open project view") {
            find(CommonContainerFixture::class.java, byXpath("//div[@class='ProjectViewTree']")).click()
            log.info("Project view opened")
        }

        step("Select a directory") {
            speak("Now, we'll select a directory to start a Task Runner operation.")
            val projectTree = find(JTreeFixture::class.java, byXpath("//div[@class='ProjectViewTree']"))
            projectTree.rightClickPath(*arrayOf("DataGnome", "src", "main", "kotlin"), fullMatch = false)
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

        step("Click 'Task Runner' action") {
            speak("Now, we'll choose the 'Task Runner' action.")
            waitFor(Duration.ofSeconds(10)) {
                try {
                    findAll(CommonContainerFixture::class.java, byXpath("//div[contains(@class, 'ActionMenuItem') and contains(@text, 'Task Runner')]"))
                        .firstOrNull()?.click()
                    log.info("'Task Runner' action clicked")
                    true
                } catch (e: Exception) {
                    log.warn("Failed to find 'Task Runner' action: ${e.message}")
                    false
                }
            }
            Thread.sleep(1000)
        }

        step("Configure Task Runner") {
            speak("Let's configure the Task Runner settings.")
            waitFor(Duration.ofSeconds(10)) {
                val dialog = find(CommonContainerFixture::class.java, byXpath("//div[@class='MyDialog' and @title='Configure Plan Ahead Action']"))
                if (dialog.isShowing) {

                    // Select autofix checkbox
                    val autoFixCheckbox = dialog.find(JCheckboxFixture::class.java, byXpath("//div[@class='JCheckBox' and @text='Auto-apply fixes']"))
                    if (!autoFixCheckbox.isSelected()) {
                        autoFixCheckbox.click()
                        log.info("Auto-apply fixes checkbox selected")
                        speak("We've enabled the auto-apply fixes option.")
                    }
                    Thread.sleep(1000)

                    val okButton = dialog.find(CommonContainerFixture::class.java, byXpath("//div[@class='JButton' and @text='OK']"))
                    okButton.click()
                    log.info("Task Runner configured and started")
                    speak("Task Runner operation configured and started.")
                    true
                } else {
                    false
                }
            }
        }

        step("Interact with Task Runner interface") {
            val messages = getReceivedMessages()
            val url = messages.firstOrNull { it.startsWith("http") }
            if (url != null) {
                log.info("Retrieved URL: $url")
                val options = ChromeOptions()
                options.addArguments("--start-fullscreen")
                driver = ChromeDriver(options)
                (driver as JavascriptExecutor).executeScript("document.body.style.zoom='150%'")
                driver.get(url)
                val wait = WebDriverWait(this@PlanAheadActionTest.driver, Duration.ofSeconds(90))
                val chatInput = wait.until<WebElement>(ExpectedConditions.elementToBeClickable(By.id("chat-input")))
                speak("Now, let's interact with the Task Runner interface.")
                Thread.sleep(2000)

                // Wait for the interface to load
                try {
                    // Interact with the interface
                    chatInput.sendKeys("Add a feature")
                    remoteRobot.keyboard {
                        enter()
                    }
                    speak("We've sent a task to add a feature.")
                    Thread.sleep(5000)

                    // Wait for the accept button to appear
                    val acceptButton = wait.until<WebElement>(ExpectedConditions.elementToBeClickable(By.xpath("//a[contains(@class, 'cmd-button') and contains(text(), 'Accept')]")))
                    speak("The AI has responded with a plan to create the HelloWorld class.")
                    // Scroll the accept button into view
                    (driver as JavascriptExecutor).executeScript("arguments[0].scrollIntoView(true);", acceptButton)
                    // Click the accept button
                    acceptButton.click()
                    speak("We've accepted the AI's plan.")
                    Thread.sleep(60000)

                } catch (e: Exception) {
                    log.warn("Error interacting with Task Runner interface", e)
                    speak("We encountered an issue while interacting with the Task Runner interface.")
                }

                Thread.sleep(2000)
                speak("We've successfully demonstrated the Task Runner feature.")
                this@PlanAheadActionTest.driver.quit()
            } else {
                log.error("No URL found in UDP messages")
                speak("Failed to retrieve the URL for the Task Runner interface.")
            }
            clearMessageBuffer()
        }

        speak("This concludes our AI Coder Task Runner demo. We've successfully initiated and interacted with a Task Runner operation.")
    }
}