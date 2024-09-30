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
        speak("Welcome to the AI Coder demo. Today, we'll be exploring the powerful Command Autofix feature, which can automatically identify and fix issues across your entire codebase.")
        log.info("Starting testCommandAutofixAction")
        Thread.sleep(2000)

        step("Open project view") {
            speak("Let's begin by opening the project view. This will give us access to our project structure.")
            find(CommonContainerFixture::class.java, byXpath("//div[@class='ProjectViewTree']")).click()
            log.info("Project view opened")
            speak("Notice how easily we can navigate through our project files.")
            Thread.sleep(2000)
        }

        step("Select a directory") {
            speak("Next, we'll select a directory where we want to apply the Command Autofix. One of the key benefits of this feature is its ability to work on entire directories, making it incredibly powerful for large-scale code improvements.")
            val projectTree = find(JTreeFixture::class.java, byXpath("//div[@class='ProjectViewTree']"))
            projectTree.rightClickPath(*arrayOf("DataGnome"), fullMatch = false)
            log.info("Directory selected")
            speak("We've selected the 'DataGnome' directory. As you can see, this significantly streamlines the process of choosing the scope for our operation.")
            Thread.sleep(3000)
        }

        step("Select 'AI Coder' menu") {
            speak("Now, let's explore how we can access the AI Coder menu. This is where we find all of our AI-powered coding tools.")
            waitFor(Duration.ofSeconds(10)) {
                try {
                    val aiCoderMenu = find(CommonContainerFixture::class.java, byXpath("//div[contains(@class, 'ActionMenu') and contains(@text, 'AI Coder')]"))
                    aiCoderMenu.click()
                    log.info("'AI Coder' menu clicked")
                    speak("The AI Coder menu is now open. Notice the range of powerful AI-assisted coding features at our disposal.")
                    true
                } catch (e: Exception) {
                    log.warn("Failed to find or click 'AI Coder' menu: ${e.message}")
                    speak("We've encountered a small hiccup in finding the AI Coder menu. Don't worry, this can happen occasionally. Let's try again.")
                    false
                }
            }
            Thread.sleep(2000)
        }

        step("Click 'Auto-Fix' action") {
            speak("From the AI Coder menu, we'll select the 'Auto-Fix' action. This powerful tool allows us to automatically identify and fix issues in our code, saving significant time and improving code quality.")
            waitFor(Duration.ofSeconds(10)) {
                try {
                    findAll(CommonContainerFixture::class.java, byXpath("//div[contains(@class, 'ActionMenuItem') and contains(@text, 'Auto-Fix')]"))
                        .firstOrNull()?.click()
                    log.info("'Auto-Fix' action clicked")
                    speak("Excellent! We've successfully initiated the Auto-Fix action. Let's watch as it begins the process of analyzing and improving our code.")
                    true
                } catch (e: Exception) {
                    log.warn("Failed to find 'Auto-Fix' action: ${e.message}")
                    speak("We're having a bit of trouble locating the Auto-Fix action. In a real-world scenario, we might check our plugin installation or IDE version. For now, let's give it another try.")
                    false
                }
            }
            Thread.sleep(2000)
        }

        step("Configure Command Autofix") {
            speak("Before we proceed, let's explore how we can configure the Command Autofix settings. This allows us to customize how the tool will operate on our code, providing flexibility for different project needs.")
            waitFor(Duration.ofSeconds(10)) {
                val dialog = find(CommonContainerFixture::class.java, byXpath("//div[@class='MyDialog' and @title='Command Autofix Settings']"))
                if (dialog.isShowing) {

                    // Select auto-apply fixes
                    val autoFixCheckbox = dialog.find(JCheckboxFixture::class.java, byXpath("//div[@class='JCheckBox' and @text='Auto-apply fixes']"))
                    autoFixCheckbox.select()
                    speak("We've enabled the 'Auto-apply fixes' option. This powerful feature allows the tool to automatically implement the improvements it identifies, significantly streamlining our workflow.")

                    val okButton = dialog.find(CommonContainerFixture::class.java, byXpath("//div[@class='JButton' and @text='OK']"))
                    okButton.click()
                    log.info("Command Autofix configured and started")
                    speak("Great! We've configured the Command Autofix settings and initiated the operation. Now, let's watch as it analyzes and improves our code. This process typically takes just a few moments, but can save hours of manual code review and refactoring.")
                    Thread.sleep(3000)
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
                speak("The Command Autofix interface has opened in a new window. Let's take a closer look at what it's doing.")
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
                        speak("The Command Autofix agent has completed its operation.")
                        val codeElements = wait.until(ExpectedConditions.presenceOfAllElementsLocatedBy(By.tagName("code")))
                        val buildSuccessful = codeElements.any { it.text.contains("BUILD SUCCESSFUL") }
                        require(buildSuccessful) { "BUILD SUCCESSFUL not found in any code element" }
                        speak("Excellent! The Command Autofix operation has completed successfully. Notice how it not only fixed issues but also ensured our build remains successful.")
                        break
                    } catch (e: Exception) {
                        attempt++
                        log.warn("Error interacting with Command Autofix interface", e)
                        blockUntilDone(wait)
                        speak("It seems we've encountered a small hiccup. Don't worry, this can happen sometimes. Let's give it another try.")
                        (driver as JavascriptExecutor).executeScript("window.scrollTo(0, 0)")
                        // Find <a class="href-link">♻</a> and click it
                        val refreshButton = driver.findElement(By.xpath("//a[@class='href-link' and text()='♻']"))
                        refreshButton.click()
                        log.info("Refresh button clicked")
                        speak("We're refreshing the Command Autofix interface to start a new attempt.")
                        Thread.sleep(1000)
                        Thread.sleep(2000)
                        driver.findElements(By.cssSelector(".tabs-container > .tabs > .tab-button")).get(attempt - 1).click()
                    }
                }
                this@CommandAutofixActionTest.driver.quit()
            } else {
                log.error("No URL found in UDP messages")
                speak("We encountered an issue retrieving the URL for the Command Autofix interface. This is unusual and might require further investigation.")
            }
            clearMessageBuffer()
        }

        speak("This concludes our demonstration of the AI Coder Command Autofix feature. We've successfully initiated, configured, and interacted with a Command Autofix operation. As you've seen, this powerful tool can significantly improve code quality and consistency across your entire project with minimal manual intervention, saving time and reducing errors. Thank you for joining us for this demo of how AI Coder can enhance your development workflow!")
        Thread.sleep(10000)
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