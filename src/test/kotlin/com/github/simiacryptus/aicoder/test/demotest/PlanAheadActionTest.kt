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

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class PlanAheadActionTest : BaseActionTest() {


    companion object {
        val log = LoggerFactory.getLogger(PlanAheadActionTest::class.java)
    }
    @Test
    fun testPlanAheadAction() = with(remoteRobot) {
    speak("Welcome to the AI Coder demo. We'll explore the Task Runner feature, which automates complex tasks to enhance coding workflow.")
        log.info("Starting testPlanAheadAction")
        Thread.sleep(3000)

        step("Open project view") {
        speak("Opening the project view to access the file structure.")
            find(CommonContainerFixture::class.java, byXpath("//div[@class='ProjectViewTree']")).click()
            log.info("Project view opened")
            Thread.sleep(2000)
        }

        step("Select a directory") {
        speak("Selecting a directory to initiate the Task Runner operation.")
            val projectTree = find(JTreeFixture::class.java, byXpath("//div[@class='ProjectViewTree']"))
            projectTree.rightClickPath(*arrayOf("DataGnome", "src", "main", "kotlin"), fullMatch = false)
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
                speak("AI Coder menu opened.")
                    true
                } catch (e: Exception) {
                    log.warn("Failed to find or click 'AI Coder' menu: ${e.message}")
                speak("Failed to find AI Coder menu. Retrying.")
                    false
                }
            }
            Thread.sleep(3000)
        }

        step("Click 'Task Runner' action") {
        speak("Selecting the 'Task Runner' action.")
            waitFor(Duration.ofSeconds(10)) {
                try {
                    findAll(CommonContainerFixture::class.java, byXpath("//div[contains(@class, 'ActionMenuItem') and contains(@text, 'Task Runner')]"))
                        .firstOrNull()?.click()
                    log.info("'Task Runner' action clicked")
                speak("Task Runner initiated.")
                    true
                } catch (e: Exception) {
                    log.warn("Failed to find 'Task Runner' action: ${e.message}")
                speak("Failed to find Task Runner action. Consider refreshing the menu or restarting the IDE if this persists.")
                    false
                }
            }
            Thread.sleep(3000)
        }

        step("Configure Task Runner") {
        speak("Configuring Task Runner settings.")
            waitFor(Duration.ofSeconds(10)) {
                val dialog = find(CommonContainerFixture::class.java, byXpath("//div[@class='MyDialog' and @title='Configure Plan Ahead Action']"))
                if (dialog.isShowing) {

                    // Select autofix checkbox
                    val autoFixCheckbox = dialog.find(JCheckboxFixture::class.java, byXpath("//div[@class='JCheckBox' and @text='Auto-apply fixes']"))
                    if (!autoFixCheckbox.isSelected()) {
                        autoFixCheckbox.click()
                        log.info("Auto-apply fixes checkbox selected")
                    speak("Enabled auto-apply fixes option.")
                    }
                    Thread.sleep(3000)

                    val okButton = dialog.find(CommonContainerFixture::class.java, byXpath("//div[@class='JButton' and @text='OK']"))
                    okButton.click()
                    log.info("Task Runner configured and started")
                speak("Task Runner configured and started.")
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
            speak("Task Runner web interface opened.")
                val options = ChromeOptions()
                options.addArguments("--start-fullscreen")
                driver = ChromeDriver(options)
                (driver as JavascriptExecutor).executeScript("document.body.style.zoom='150%'")
                driver.get(url)
                val wait = WebDriverWait(this@PlanAheadActionTest.driver, Duration.ofSeconds(90))
                val chatInput = wait.until<WebElement>(ExpectedConditions.elementToBeClickable(By.id("chat-input")))
            speak("Interface loaded. Interacting with Task Runner.")
                Thread.sleep(5000)

                // Wait for the interface to load
                try {
                    // Interact with the interface
                    chatInput.sendKeys("Add a feature")
                    remoteRobot.keyboard {
                        enter()
                    }
                speak("Sent task to add a feature.")
                    Thread.sleep(8000)

                    // Wait for the accept button to appear
                    val acceptButton = wait.until<WebElement>(ExpectedConditions.elementToBeClickable(By.xpath("//a[contains(@class, 'cmd-button') and contains(text(), 'Accept')]")))
                speak("AI responded with a plan to create the HelloWorld class.")
                    // Scroll the accept button into view
                    (driver as JavascriptExecutor).executeScript("arguments[0].scrollIntoView(true);", acceptButton)
                    // Click the accept button
                    acceptButton.click()
                speak("Accepted AI's plan. Implementing changes.")
                    Thread.sleep(65000)

                } catch (e: Exception) {
                    log.warn("Error interacting with Task Runner interface", e)
                speak("Error interacting with Task Runner interface. Consider refreshing or restarting the task.")
                }

                Thread.sleep(5000)
            speak("Task Runner demonstration completed. This tool simplifies complex coding tasks and boosts productivity.")
                this@PlanAheadActionTest.driver.quit()
            } else {
                log.error("No URL found in UDP messages")
            speak("Failed to retrieve Task Runner interface URL. Consider restarting the IDE or contacting IT support.")
            }
            clearMessageBuffer()
        }

    speak("AI Coder Task Runner demo concluded. This feature streamlines coding processes by delegating complex tasks to AI, automating code generation, and implementing changes quickly.")
    Thread.sleep(10000)
    }
}