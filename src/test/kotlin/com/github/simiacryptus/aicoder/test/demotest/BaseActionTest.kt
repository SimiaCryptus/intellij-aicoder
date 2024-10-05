package com.github.simiacryptus.aicoder.test.demotest

import com.github.simiacryptus.aicoder.test.demotest.TestUtil.startUdpServer
import com.github.simiacryptus.aicoder.test.demotest.TestUtil.stopUdpServer
import com.intellij.remoterobot.RemoteRobot
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.TestInstance
import org.openqa.selenium.WebDriver
import org.slf4j.Logger
import org.slf4j.LoggerFactory

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
abstract class BaseActionTest : ScreenRec() {
    protected lateinit var remoteRobot: RemoteRobot

    companion object {
        private val log: Logger = LoggerFactory.getLogger(this.javaClass)
    }

    protected lateinit var driver: WebDriver

    @BeforeAll
    fun setup() {
        remoteRobot = RemoteRobot("http://127.0.0.1:8082")
        startUdpServer()
        System.setProperty("webdriver.chrome.driver", "/usr/bin/chromedriver")
        try {
            startScreenRecording()
        } catch (e: Exception) {
            log.error("Failed to start screen recording", e)
        }
    }

    @AfterAll
    fun tearDown() {
        stopUdpServer()
        if (::driver.isInitialized) {
            driver.quit()
        }
        stopScreenRecording()
    }

}

class TimeoutException(message: String) : Exception(message)