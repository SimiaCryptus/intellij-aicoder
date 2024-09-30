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
import org.openqa.selenium.support.ui.ExpectedConditions
import org.openqa.selenium.support.ui.WebDriverWait

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class MassPatchActionTest {
    private lateinit var remoteRobot: RemoteRobot
    private val log = LoggerFactory.getLogger(MassPatchActionTest::class.java)
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
    fun testMassPatchAction() = with(remoteRobot) {
        speak("Welcome to the AI Coder demo. We'll demonstrate the Mass Patch feature.")
        log.info("Starting testMassPatchAction")
        Thread.sleep(1000)

        step("Open project view") {
            find(CommonContainerFixture::class.java, byXpath("//div[@class='ProjectViewTree']")).click()
            log.info("Project view opened")
        }

        step("Select a directory") {
            speak("Now, we'll select a directory to start a mass patch operation.")
            val projectTree = find(JTreeFixture::class.java, byXpath("//div[@class='ProjectViewTree']"))
            projectTree.rightClickPath(*arrayOf("DataGnome", "src", "main", "kotlin", "com.simiacryptus.util", "files"), fullMatch = false)
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

        step("Click 'Mass Patch' action") {
            speak("Now, we'll choose the 'Mass Patch' action.")
            waitFor(Duration.ofSeconds(10)) {
                try {
                    findAll(CommonContainerFixture::class.java, byXpath("//div[contains(@class, 'ActionMenuItem') and contains(@text, 'Mass Patch')]"))
                        .firstOrNull()?.click()
                    log.info("'Mass Patch' action clicked")
                    true
                } catch (e: Exception) {
                    log.warn("Failed to find 'Mass Patch' action: ${e.message}")
                    false
                }
            }
            Thread.sleep(1000)
        }

        step("Configure Mass Patch") {
            speak("Let's configure the Mass Patch settings.")
            waitFor(Duration.ofSeconds(10)) {
                val dialog = find(CommonContainerFixture::class.java, byXpath("//div[@class='MyDialog' and @title='Compile Documentation']"))
                if (dialog.isShowing) {
                    val aiInstructionField = dialog.find(CommonContainerFixture::class.java, byXpath("//div[@class='JBTextArea']"))
                    aiInstructionField.click()
                    remoteRobot.keyboard {
                        pressing(java.awt.event.KeyEvent.VK_CONTROL) {
                            key(java.awt.event.KeyEvent.VK_A) // Select all
                        }
                        enterText("Add logging to all methods")
                    }
                    val okButton = dialog.find(CommonContainerFixture::class.java, byXpath("//div[@class='JButton' and @text='OK']"))
                    okButton.click()
                    log.info("Mass Patch configured and started")
                    speak("Mass Patch operation configured and started.")
                    true
                } else {
                    false
                }
            }
        }

        step("Get URL from UDP messages") {
            val messages = getReceivedMessages()
            val url = messages.firstOrNull { it.startsWith("http") }
            if (url != null) {
                log.info("Retrieved URL: $url")
                this@MassPatchActionTest.driver = ChromeDriver()
                this@MassPatchActionTest.driver.get(url)
                val wait = WebDriverWait(this@MassPatchActionTest.driver, Duration.ofSeconds(10))
                wait.until<Boolean> {
                    val loadingElements = it.findElements(By.xpath("//span[contains(@class, 'sr-only') and contains(text(), 'Loading')]"))
                    loadingElements.none { element -> element.isDisplayed }
                }
                val chatInput = driver.findElement(By.className("response-message"))
                speak("Now, let's review the proposed patches.")
                Thread.sleep(2000)

                // Wait for the patches to be generated
                val longWait = WebDriverWait(this@MassPatchActionTest.driver, Duration.ofSeconds(60))
                try {
                    longWait.until<Boolean> {
                        val containers = it.findElements(By.xpath("//div[contains(@class, 'message-container')]"))
                        containers.any { container -> container.isDisplayed }
                    }
                    speak("The AI has generated patches for the selected files.")
                    Thread.sleep(1000)

                    // Find all tab buttons
                    val tabButtons = driver.findElements(By.cssSelector(".tabs-container > .tabs > .tab-button"))
                        .filter { it.isDisplayed }
                    Thread({ speak("Reviewing ${tabButtons.size} patches. We can decide which ones to apply.") }).start()
                    tabButtons.take(3).forEachIndexed { index, button ->
                        speak("Clicking tab button $index")
                        (this@MassPatchActionTest.driver as JavascriptExecutor).executeScript("arguments[0].click();", button)
                        Thread.sleep(1000)
                    }
                } catch (e: Exception) {
                    log.warn("Patches not found within the expected time.", e)
                    speak("The AI is taking longer than expected to generate patches.")
                }

                Thread.sleep(2000)
                speak("We've successfully demonstrated the Mass Patch feature.")
                this@MassPatchActionTest.driver.quit()
            } else {
                log.error("No URL found in UDP messages")
                speak("Failed to retrieve the URL for the Mass Patch interface.")
            }
            clearMessageBuffer()
        }

        speak("This concludes our AI Coder Mass Patch demo. We've successfully initiated a Mass Patch operation for the selected directory.")
    }
}