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
    speak("Welcome to the AI Coder demo. Today, we'll be exploring the powerful Task Runner feature, which can significantly enhance your coding workflow by automating complex tasks and boosting productivity.")
        log.info("Starting testPlanAheadAction")
        Thread.sleep(3000)

        step("Open project view") {
        speak("Let's begin by opening the project view to access our file structure. This is typically the first step in navigating our project efficiently.")
            find(CommonContainerFixture::class.java, byXpath("//div[@class='ProjectViewTree']")).click()
            log.info("Project view opened")
            Thread.sleep(2000)
        }

        step("Select a directory") {
        speak("Now, we'll select a specific directory where we want to initiate our Task Runner operation. This allows us to focus the AI's attention on a particular part of our project, ensuring more relevant and targeted results.")
            val projectTree = find(JTreeFixture::class.java, byXpath("//div[@class='ProjectViewTree']"))
            projectTree.rightClickPath(*arrayOf("DataGnome", "src", "main", "kotlin"), fullMatch = false)
            log.info("Directory selected")
            Thread.sleep(3000)
        }

        step("Select 'AI Coder' menu") {
        speak("From the context menu that appears, we'll select the AI Coder option. This menu is a gateway to all the AI-powered features available to us, designed to streamline various aspects of our coding process.")
            waitFor(Duration.ofSeconds(10)) {
                try {
                    val aiCoderMenu = find(CommonContainerFixture::class.java, byXpath("//div[contains(@class, 'ActionMenu') and contains(@text, 'AI Coder')]"))
                    aiCoderMenu.click()
                    log.info("'AI Coder' menu clicked")
                speak("Excellent! The AI Coder menu has been successfully opened, giving us access to a suite of powerful AI-assisted coding tools.")
                    true
                } catch (e: Exception) {
                    log.warn("Failed to find or click 'AI Coder' menu: ${e.message}")
                speak("We've encountered a small hiccup finding the AI Coder menu. Don't worry, this can happen occasionally. Let's give it another moment to appear.")
                    false
                }
            }
            Thread.sleep(3000)
        }

        step("Click 'Task Runner' action") {
        speak("Now, let's choose the 'Task Runner' action. This powerful tool allows us to delegate complex coding tasks to the AI, significantly reducing the time and effort required for various programming tasks.")
            waitFor(Duration.ofSeconds(10)) {
                try {
                    findAll(CommonContainerFixture::class.java, byXpath("//div[contains(@class, 'ActionMenuItem') and contains(@text, 'Task Runner')]"))
                        .firstOrNull()?.click()
                    log.info("'Task Runner' action clicked")
                speak("Great! We've successfully initiated the Task Runner. Notice how easily we can access this feature directly from our project structure.")
                    true
                } catch (e: Exception) {
                    log.warn("Failed to find 'Task Runner' action: ${e.message}")
                speak("We're encountering a small issue finding the Task Runner action. In a real-world scenario, we might try refreshing the menu or restarting the IDE if this persists.")
                    false
                }
            }
            Thread.sleep(3000)
        }

        step("Configure Task Runner") {
        speak("Before we start, let's configure the Task Runner settings to optimize its performance for our needs. This customization is key to getting the most out of the AI's capabilities.")
            waitFor(Duration.ofSeconds(10)) {
                val dialog = find(CommonContainerFixture::class.java, byXpath("//div[@class='MyDialog' and @title='Configure Plan Ahead Action']"))
                if (dialog.isShowing) {

                    // Select autofix checkbox
                    val autoFixCheckbox = dialog.find(JCheckboxFixture::class.java, byXpath("//div[@class='JCheckBox' and @text='Auto-apply fixes']"))
                    if (!autoFixCheckbox.isSelected()) {
                        autoFixCheckbox.click()
                        log.info("Auto-apply fixes checkbox selected")
                    speak("We've enabled the auto-apply fixes option. This powerful feature allows the AI to automatically implement its suggested changes, significantly streamlining our workflow and saving valuable time.")
                    }
                    Thread.sleep(3000)

                    val okButton = dialog.find(CommonContainerFixture::class.java, byXpath("//div[@class='JButton' and @text='OK']"))
                    okButton.click()
                    log.info("Task Runner configured and started")
                speak("Excellent! We've configured the Task Runner and started the operation. Now let's see this powerful tool in action and observe how it can transform our coding process.")
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
            speak("The Task Runner has opened a web interface for us to interact with. This intuitive interface allows us to communicate our coding tasks directly to the AI. Let's take a closer look at how this works.")
                val options = ChromeOptions()
                options.addArguments("--start-fullscreen")
                driver = ChromeDriver(options)
                (driver as JavascriptExecutor).executeScript("document.body.style.zoom='150%'")
                driver.get(url)
                val wait = WebDriverWait(this@PlanAheadActionTest.driver, Duration.ofSeconds(90))
                val chatInput = wait.until<WebElement>(ExpectedConditions.elementToBeClickable(By.id("chat-input")))
            speak("Now that the interface has loaded, let's interact with the Task Runner. We can type our coding tasks here, and the AI will assist us in completing them. This is where the real magic happens - watch how the AI understands and executes complex coding tasks.")
                Thread.sleep(5000)

                // Wait for the interface to load
                try {
                    // Interact with the interface
                    chatInput.sendKeys("Add a feature")
                    remoteRobot.keyboard {
                        enter()
                    }
                speak("We've sent a task to add a feature. Let's see how the AI responds to this request. Notice how quickly it processes our input and generates a response.")
                    Thread.sleep(8000)

                    // Wait for the accept button to appear
                    val acceptButton = wait.until<WebElement>(ExpectedConditions.elementToBeClickable(By.xpath("//a[contains(@class, 'cmd-button') and contains(text(), 'Accept')]")))
                speak("The AI has responded with a plan to create the HelloWorld class. This demonstrates the AI's ability to generate structured, relevant code suggestions in a matter of seconds. Imagine how this could accelerate your development process!")
                    // Scroll the accept button into view
                    (driver as JavascriptExecutor).executeScript("arguments[0].scrollIntoView(true);", acceptButton)
                    // Click the accept button
                    acceptButton.click()
                speak("We've accepted the AI's plan. Now, it will implement the suggested changes in our project. This is where we see the real power of the Task Runner - it's not just suggesting code, but actually implementing it for us.")
                    Thread.sleep(65000)

                } catch (e: Exception) {
                    log.warn("Error interacting with Task Runner interface", e)
                speak("We've encountered a small hiccup while interacting with the Task Runner interface. Don't worry, this can happen occasionally. In a real-world scenario, we might try refreshing the page or restarting the task to resolve this.")
                }

                Thread.sleep(5000)
            speak("We've successfully demonstrated the Task Runner feature. As you can see, it greatly simplifies complex coding tasks and can significantly boost productivity. This tool has the potential to revolutionize your coding workflow, saving you time and effort on a daily basis.")
                this@PlanAheadActionTest.driver.quit()
            } else {
                log.error("No URL found in UDP messages")
            speak("We're having trouble retrieving the URL for the Task Runner interface. This is an unusual occurrence that might require a restart of the IDE. In a real development environment, our IT support would be able to quickly resolve such issues.")
            }
            clearMessageBuffer()
        }

    speak("This concludes our AI Coder Task Runner demo. We've successfully initiated and interacted with a Task Runner operation, showcasing its potential to streamline your coding process. The ability to delegate complex tasks to AI, automate code generation, and quickly implement changes can significantly enhance your productivity and code quality. Thank you for joining us in exploring this powerful feature. We hope you're as excited as we are about the possibilities it opens up for efficient, AI-assisted coding!")
    Thread.sleep(10000)
    }
}