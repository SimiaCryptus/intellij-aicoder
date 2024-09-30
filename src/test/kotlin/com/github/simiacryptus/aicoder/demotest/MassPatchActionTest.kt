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
import org.openqa.selenium.chrome.ChromeOptions
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
    speak("Welcome to the AI Coder demo. Today, we'll be exploring the powerful Mass Patch feature, which allows us to apply consistent changes across multiple files. This tool can significantly streamline your development process, especially when working with large codebases.")
        log.info("Starting testMassPatchAction")
        Thread.sleep(3000)

        step("Open project view") {
        speak("Let's begin by opening the project view to access our file structure. This is typically the first step in navigating our project.")
            find(CommonContainerFixture::class.java, byXpath("//div[@class='ProjectViewTree']")).click()
            log.info("Project view opened")
            Thread.sleep(2000)
        }

        step("Select a directory") {
        speak("Now, we'll select a specific directory where we want to apply our mass patch. By choosing a directory, we can target our changes efficiently, saving time and ensuring consistency across related files.")
            val projectTree = find(JTreeFixture::class.java, byXpath("//div[@class='ProjectViewTree']"))
            projectTree.rightClickPath(*arrayOf("DataGnome", "src", "main", "kotlin", "com.simiacryptus.util", "files"), fullMatch = false)
            log.info("Directory selected")
            Thread.sleep(3000)
        }

        step("Select 'AI Coder' menu") {
        speak("From the context menu that appears, we'll select the AI Coder option. This menu is your gateway to all the AI-powered features that can enhance your coding experience.")
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
        speak("Within the AI Coder menu, we'll now choose the 'Mass Patch' action. This powerful feature allows us to apply consistent changes across multiple files simultaneously, which can be a huge time-saver when refactoring or updating code patterns.")
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
        speak("Now, let's configure the Mass Patch settings. This is where we can specify the changes we want to apply across our selected files. The flexibility here allows us to tackle a wide range of coding tasks efficiently.")
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
                speak("For this demonstration, we're instructing the AI to add logging to all methods in our selected files. This is a common task that can be tedious to do manually, but with Mass Patch, we can accomplish it in just a few clicks.")
                    val okButton = dialog.find(CommonContainerFixture::class.java, byXpath("//div[@class='JButton' and @text='OK']"))
                    okButton.click()
                    log.info("Mass Patch configured and started")
                speak("Great! We've configured our Mass Patch operation and started the process. The AI will now analyze our files and propose changes. This analysis is typically quite fast, even for larger codebases.")
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
            speak("The AI has generated a URL for us to review the proposed changes. Let's open it in a browser. This review step is crucial as it allows us to maintain control over the changes being made to our codebase.")
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
            speak("Now that the page has loaded, let's review the patches proposed by the AI. This interface allows us to examine each change in detail, ensuring that the AI's suggestions align with our coding standards and project requirements.")
                Thread.sleep(4000)

                // Wait for the patches to be generated
                val longWait = WebDriverWait(this@MassPatchActionTest.driver, Duration.ofSeconds(60))
                try {
                    longWait.until<Boolean> {
                        val containers = it.findElements(By.xpath("//div[contains(@class, 'message-container')]"))
                        containers.any { container -> container.isDisplayed }
                    }
                speak("Excellent! The AI has successfully generated patches for our selected files. Let's take a closer look at what changes are being proposed. This review process is a key advantage of AI Coder, as it combines the efficiency of AI with the expertise of the developer.")
                    Thread.sleep(3000)

                    // Find all tab buttons
                    val tabButtons = driver.findElements(By.cssSelector(".tabs-container > .tabs > .tab-button"))
                        .filter { it.isDisplayed }
                Thread({ speak("We have ${tabButtons.size} patches to review. For each patch, we can decide whether to apply it or not, giving us full control over the changes. This granular control ensures that we can leverage the AI's suggestions while maintaining the integrity of our codebase.") }).start()
                    tabButtons.take(3).forEachIndexed { index, button ->
                    speak("Let's examine patch number ${index + 1}. Notice how the AI has intelligently added logging statements to our methods, potentially improving our ability to debug and monitor our application.")
                        (this@MassPatchActionTest.driver as JavascriptExecutor).executeScript("arguments[0].click();", button)
                        Thread.sleep(3000)
                    }
                } catch (e: Exception) {
                    log.warn("Patches not found within the expected time.", e)
                speak("It seems the AI is taking a bit longer than expected to generate patches. This can happen with larger codebases or more complex changes. In a real-world scenario, we might use this time to review our requirements or plan our next steps.")
                }

                Thread.sleep(4000)
            speak("We've now successfully demonstrated the Mass Patch feature. As you can see, it provides a powerful way to make consistent changes across multiple files, saving time and reducing the chance of errors. This tool can significantly enhance your productivity, especially when working on large-scale refactoring or code improvement tasks.")
                this@MassPatchActionTest.driver.quit()
            } else {
                log.error("No URL found in UDP messages")
            speak("I apologize, but we encountered an issue retrieving the URL for the Mass Patch interface. This is an unexpected error that our development team will investigate. In a real-world scenario, we would troubleshoot this issue or try rerunning the Mass Patch action.")
            }
            clearMessageBuffer()
        }

    speak("This concludes our AI Coder Mass Patch demo. We've walked through the process of initiating a Mass Patch operation, from selecting our target directory to reviewing the AI-generated patches. As we've seen, this feature can significantly streamline your development process, especially when working with large codebases. It combines the power of AI with the control and expertise of the developer, resulting in efficient and accurate code modifications. Thank you for joining us for this demonstration of how AI Coder can enhance your coding workflow.")
    Thread.sleep(10000)
    }
}