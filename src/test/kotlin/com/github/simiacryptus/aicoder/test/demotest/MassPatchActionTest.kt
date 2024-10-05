package com.github.simiacryptus.aicoder.test.demotest

import com.github.simiacryptus.aicoder.test.demotest.TestUtil.speak
import com.github.simiacryptus.aicoder.test.demotest.TestUtil.startUdpServer
import com.github.simiacryptus.aicoder.test.demotest.TestUtil.stopUdpServer
import com.github.simiacryptus.aicoder.test.demotest.TestUtil.getReceivedMessages
import com.github.simiacryptus.aicoder.test.demotest.TestUtil.clearMessageBuffer
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
import org.openqa.selenium.chrome.ChromeOptions
import org.openqa.selenium.support.ui.ExpectedConditions
import org.openqa.selenium.support.ui.WebDriverWait

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class MassPatchActionTest :BaseActionTest() {


    companion object {
        val log = LoggerFactory.getLogger(MassPatchActionTest::class.java)
    }
    @Test
    fun testMassPatchAction() = with(remoteRobot) {
    speak("Welcome to the AI Coder demo. We'll explore the Mass Patch feature for applying consistent changes across multiple files.")
        log.info("Starting testMassPatchAction")
        Thread.sleep(3000)

        step("Open project view") {
        speak("Opening the project view to access the file structure.")
            find(CommonContainerFixture::class.java, byXpath("//div[@class='ProjectViewTree']")).click()
            log.info("Project view opened")
            Thread.sleep(2000)
        }

        step("Select a directory") {
        speak("Selecting the target directory for the mass patch.")
            val projectTree = find(JTreeFixture::class.java, byXpath("//div[@class='ProjectViewTree']"))
            projectTree.rightClickPath(*arrayOf("DataGnome", "src", "main", "kotlin", "com.simiacryptus.util", "files"), fullMatch = false)
            log.info("Directory selected")
            Thread.sleep(3000)
        }

        step("Select 'AI Coder' menu") {
        speak("Selecting the AI Coder option from the context menu.")
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
            Thread.sleep(3000)
        }

        step("Click 'Mass Patch' action") {
        speak("Initiating the Mass Patch action.")
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
            Thread.sleep(3000)
        }

        step("Configure Mass Patch") {
        speak("Configuring Mass Patch settings.")
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
                speak("Instructing the AI to add logging to all methods in the selected files.")
                    val okButton = dialog.find(CommonContainerFixture::class.java, byXpath("//div[@class='JButton' and @text='OK']"))
                    okButton.click()
                    log.info("Mass Patch configured and started")
                    Thread.sleep(3000)
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
            speak("Opening the AI-generated URL to review proposed changes.")
                val options = ChromeOptions()
                options.addArguments("--start-fullscreen")
                driver = ChromeDriver(options)
                (driver as JavascriptExecutor).executeScript("document.body.style.zoom='150%'")
                driver.get(url)
                val wait = WebDriverWait(this@MassPatchActionTest.driver, Duration.ofSeconds(10))
                wait.until<Boolean> {
                    val loadingElements = it.findElements(By.xpath("//span[contains(@class, 'sr-only') and contains(text(), 'Loading')]"))
                    loadingElements.none { element -> element.isDisplayed }
                }
                val chatInput = driver.findElement(By.className("response-message"))
            speak("Reviewing AI-proposed patches.")
                Thread.sleep(4000)

                // Wait for the patches to be generated
                val longWait = WebDriverWait(this@MassPatchActionTest.driver, Duration.ofSeconds(60))
                try {
                    longWait.until<Boolean> {
                        val containers = it.findElements(By.xpath("//div[contains(@class, 'message-container')]"))
                        containers.any { container -> container.isDisplayed }
                    }
                speak("AI has generated patches. Examining proposed changes.")
                    Thread.sleep(3000)

                    // Find all tab buttons
                    val tabButtons = driver.findElements(By.cssSelector(".tabs-container > .tabs > .tab-button"))
                        .filter { it.isDisplayed }
                Thread({ speak("${tabButtons.size} patches to review. Each can be individually applied or rejected.") }).start()
                    tabButtons.take(3).forEachIndexed { index, button ->
                    speak("Examining patch ${index + 1}. AI has added logging statements to methods.")
                        (this@MassPatchActionTest.driver as JavascriptExecutor).executeScript("arguments[0].click();", button)
                        Thread.sleep(3000)
                    }
                } catch (e: Exception) {
                    log.warn("Patches not found within the expected time.", e)
                speak("AI is taking longer than expected to generate patches. This can occur with larger codebases or complex changes.")
                }

                Thread.sleep(4000)
            speak("Mass Patch feature demonstration complete. This tool enables consistent changes across multiple files, enhancing productivity for large-scale refactoring or code improvements.")
                this@MassPatchActionTest.driver.quit()
            } else {
                log.error("No URL found in UDP messages")
            speak("Error retrieving URL for Mass Patch interface. In a real scenario, we would troubleshoot or rerun the action.")
            }
            clearMessageBuffer()
        }

    speak("AI Coder Mass Patch demo concluded. We've demonstrated initiating a Mass Patch operation, from directory selection to patch review. This feature streamlines development, especially for large codebases, combining AI efficiency with developer control.")
    Thread.sleep(10000)
    }
}