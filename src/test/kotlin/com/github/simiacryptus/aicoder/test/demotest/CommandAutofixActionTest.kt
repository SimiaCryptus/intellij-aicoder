package com.github.simiacryptus.aicoder.test.demotest

import com.github.simiacryptus.aicoder.test.demotest.TestUtil.speak
import com.github.simiacryptus.aicoder.test.demotest.TestUtil.startUdpServer
import com.github.simiacryptus.aicoder.test.demotest.TestUtil.stopUdpServer
import com.github.simiacryptus.aicoder.test.demotest.TestUtil.getReceivedMessages
import com.github.simiacryptus.aicoder.test.demotest.TestUtil.clearMessageBuffer
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
import java.lang.Thread.sleep

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class CommandAutofixActionTest : BaseActionTest() {

    companion object {
        val log = LoggerFactory.getLogger(CommandAutofixActionTest::class.java)
    }

    @Test
    fun testCommandAutofixAction() = with(remoteRobot) {
        speak("This demo showcases the Command Autofix feature, which automatically identifies and fixes issues across the codebase.")
        log.info("Starting testCommandAutofixAction")
        Thread.sleep(2000)

        step("Open project view") {
            speak("Opening the project view to access the project structure.")
            find(CommonContainerFixture::class.java, byXpath("//div[@class='ProjectViewTree']")).click()
            log.info("Project view opened")
        }

        step("Select a directory") {
            speak("Selecting a directory to apply Command Autofix.")
            val projectTree = find(JTreeFixture::class.java, byXpath("//div[@class='ProjectViewTree']"))
            projectTree.rightClickPath(*arrayOf("DataGnome"), fullMatch = false)
            log.info("Directory selected")
        }

        step("Select 'AI Coder' menu") {
            speak("Accessing the AI Coder menu.")
            waitFor(Duration.ofSeconds(10)) {
                try {
                    val aiCoderMenu = find(CommonContainerFixture::class.java, byXpath("//div[contains(@class, 'ActionMenu') and contains(@text, 'AI Coder')]"))
                    aiCoderMenu.click()
                    log.info("'AI Coder' menu clicked")
                    true
                } catch (e: Exception) {
                    log.warn("Failed to find or click 'AI Coder' menu: ${e.message}")
                    speak("Failed to find AI Coder menu. Retrying.")
                    false
                }
            }
        }

        step("Click 'Auto-Fix' action") {
            speak("Selecting the 'Auto-Fix' action.")
            waitFor(Duration.ofSeconds(10)) {
                try {
                    findAll(CommonContainerFixture::class.java, byXpath("//div[contains(@class, 'ActionMenuItem') and contains(@text, 'Auto-Fix')]"))
                        .firstOrNull()?.click()
                    log.info("'Auto-Fix' action clicked")
                    true
                } catch (e: Exception) {
                    log.warn("Failed to find 'Auto-Fix' action: ${e.message}")
                    speak("Failed to find Auto-Fix action. Retrying.")
                    false
                }
            }
        }

        step("Configure Command Autofix") {
            speak("Configuring Command Autofix settings.")
            waitFor(Duration.ofSeconds(10)) {
                val dialog = find(CommonContainerFixture::class.java, byXpath("//div[@class='MyDialog' and @title='Command Autofix Settings']"))
                if (dialog.isShowing) {

                    val autoFixCheckbox = dialog.find(JCheckboxFixture::class.java, byXpath("//div[@class='JCheckBox' and @text='Auto-apply fixes']"))
                    autoFixCheckbox.select()
                    speak("Enabled 'Auto-apply fixes' option.")

                    val okButton = dialog.find(CommonContainerFixture::class.java, byXpath("//div[@class='JButton' and @text='OK']"))
                    okButton.click()
                    log.info("Command Autofix configured and started")
                    true
                } else {
                    false
                }
            }
        }
        sleep(1000)

        step("Interact with Command Autofix interface") {
            val messages = getReceivedMessages()
            val url = messages.firstOrNull { it.startsWith("http") }
            if (url != null) {
                log.info("Retrieved URL: $url")
                speak("Command Autofix interface opened in a new window.")
                val options = ChromeOptions()
                options.addArguments("--start-fullscreen")
                this@CommandAutofixActionTest.driver = ChromeDriver(options)
                (this@CommandAutofixActionTest.driver as JavascriptExecutor).executeScript("document.body.style.zoom='150%'")
                this@CommandAutofixActionTest.driver.get(url)

                var attempt = 1
                while (attempt <= 5) {
                    val wait = WebDriverWait(this@CommandAutofixActionTest.driver, Duration.ofSeconds(600))
                    try {
                        blockUntilDone(wait)
                        speak("Command Autofix operation completed.")
                        val codeElements = wait.until(ExpectedConditions.presenceOfAllElementsLocatedBy(By.tagName("code")))
                        val buildSuccessful = codeElements.any { it.text.contains("BUILD SUCCESSFUL") }
                        require(buildSuccessful) { "BUILD SUCCESSFUL not found in any code element" }
                        speak("Command Autofix operation completed successfully. Build remains successful.")
                        break
                    } catch (e: Exception) {
                        attempt++
                        log.warn("Error interacting with Command Autofix interface", e)
                        blockUntilDone(wait)
                        speak("Error encountered. Retrying.")
                        (driver as JavascriptExecutor).executeScript("window.scrollTo(0, 0)")
                        val refreshButton = driver.findElement(By.xpath("//a[@class='href-link' and text()='♻']"))
                        refreshButton.click()
                        log.info("Refresh button clicked")
                        speak("Refreshing Command Autofix interface.")
                        driver.findElements(By.cssSelector(".tabs-container > .tabs > .tab-button")).get(attempt - 1).click()
                    }
                }
                this@CommandAutofixActionTest.driver.quit()
            } else {
                log.error("No URL found in UDP messages")
                speak("Error retrieving Command Autofix interface URL.")
            }
            clearMessageBuffer()
        }

        speak("Demo concluded. Command Autofix feature demonstrated.")
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