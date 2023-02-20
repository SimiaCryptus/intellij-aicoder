package com.github.simiacryptus.aicoder

import com.intellij.remoterobot.RemoteRobot
import com.intellij.remoterobot.fixtures.ComponentFixture
import com.intellij.remoterobot.search.locators.byXpath
import com.intellij.remoterobot.utils.Keyboard
import org.junit.Test

import java.awt.event.KeyEvent
import java.awt.image.BufferedImage
import java.io.File
import java.io.FileOutputStream
import java.io.PrintWriter
import java.lang.Thread.sleep
import javax.imageio.ImageIO

class ImplementationTest {

    @Test
    fun implementClass() {
        val buildDir = File("$basePath\\build")
        if (!buildDir.exists()) return
        val testOutputFile = File(buildDir, "Implementations.md")
        val out = PrintWriter(FileOutputStream(testOutputFile))
        documentJavaImplementation("PrimeNumbers", "Print all prime numbers from 1 to 100", out, buildDir)
        documentJavaImplementation("Fibonacci", "Print the first 100 Fibonacci numbers", out, buildDir)
        out.close() // Close file
    }

    private fun newJavaClass(name: String) {
        click("//div[@class='ProjectViewTree']")
        keyboard.key(KeyEvent.VK_RIGHT)
        keyboard.enterText("src")
        keyboard.key(KeyEvent.VK_CONTEXT_MENU)
        click("//div[contains(@text.key, 'group.NewGroup.text')]")
        sleep(500)
        click("//div[contains(@text.key, 'group.NewGroup.text')]//div[@text.key='action.NewClass.text']")
        keyboard.enterText(name)
        keyboard.enter()
    }


    //robot.find(ComponentFixture::class.java, byXpath("//div[contains(@myvisibleactions, 'all')]//div[contains(@myaction.key, 'action.stop')]")).isShowing
    private fun isStarted(): Boolean =
        robot.findAll(ComponentFixture::class.java, byXpath("//div[contains(@accessiblename.key, 'editor.accessible.name')]")).isNotEmpty()
    private fun isDialogOpen(): Boolean =
        robot.findAll(ComponentFixture::class.java, byXpath("//div[@class='EngravedLabel']")).isNotEmpty()

    private fun isStillRunning(): Boolean {
        val resultText = resultText() ?: return false
        return !(resultText.contains("Process finished"))
    }

    private fun resultText(): String? {
        val data = robot.find(
            ComponentFixture::class.java,
            byXpath("//div[contains(@accessiblename.key, 'editor.accessible.name')]")
        ).data
        return data.getAll().map { x -> x.text }.reduceOrNull { a, b -> a + b }
    }

    private fun editorText() =
        robot.find(ComponentFixture::class.java, byXpath("//div[@accessiblename.key='editor.for.file.accessible.name']")).data.getAll()
            .map { x -> x.text }.reduce { a, b -> a + b }

    private fun implementJava(name: String, task: String): BufferedImage {
        click("//div[@accessiblename.key='editor.for.file.accessible.name']")
        keyboard.selectAll()
        keyboard.key(KeyEvent.VK_DELETE)
        multiline("public class $name {\\n  // $task")
        keyboard.key(KeyEvent.VK_CONTEXT_MENU)
        sleep(100)
        keyboard.key(KeyEvent.VK_A)
        sleep(100)
        keyboard.key(KeyEvent.VK_I)
        val image = robot.find(ComponentFixture::class.java, byXpath("//div[@class='IdeRootPane']")).getScreenshot()
        sleep(100)
        keyboard.enter()
        awaitProcessing()
        return image
    }

    private fun awaitRunCompletion() {
        while (!isStarted()) sleep(1)
        val start = System.currentTimeMillis()
        println("Process started")
        while (isStillRunning()) {
            sleep(100)
        }
        val end = System.currentTimeMillis()
        println("Process ended after ${(end - start) / 1000.0}")
    }

    private fun awaitProcessing() {
        while (!isDialogOpen()) sleep(1)
        val start = System.currentTimeMillis()
        println("Dialog opened")
        while (isDialogOpen()) sleep(100)
        val end = System.currentTimeMillis()
        println("Dialog closed after ${(end - start) / 1000.0}")
    }

    private fun documentJavaImplementation(
        name: String,
        directive: String,
        out: PrintWriter,
        file: File
    ) {
        out.println("# $name")
        out.println(
            """```
           |$directive
           |```""".trimMargin()
        )
        newJavaClass(name)

        val image = implementJava(name, directive)
        keyboard.hotKey(KeyEvent.VK_CONTROL, KeyEvent.VK_ALT, KeyEvent.VK_L) // Reformat
        ImageIO.write(image, "png", File(file, "$name-menu.png"))
        out.println("|![menu](${name}-menu.png)")
        keyboard.hotKey(KeyEvent.VK_CONTROL, KeyEvent.VK_SHIFT, KeyEvent.VK_F10) // Run
        awaitRunCompletion()

        out.println("```")
        out.println(editorText())
        out.println("```")
        out.println("")
        out.println("```")
        out.println(resultText())
        out.println("```")

        ImageIO.write(robot.find(ComponentFixture::class.java, byXpath("//div[@class='IdeRootPane']")).getScreenshot(), "png", File(file, "$name-result.png"))
        out.println("|![result](${name}-result.png)")

        // Close run tab
        sleep(100)
        clickr("//div[@class='ContentTabLabel']")
        sleep(100)
        click("//div[contains(@text.key, 'action.CloseContent.text')]")
        sleep(100)
        // Close editor
        click("//div[@class='InplaceButton']")
    }

    companion object {
        private val basePath = "C:\\Users\\andre\\code\\aicoder\\intellij-aicoder"
        private val url = "http://127.0.0.1:8082"
        private val robot = RemoteRobot(url)
        private val keyboard = Keyboard(robot)

        private fun click(path: String) {
            robot.find(ComponentFixture::class.java, byXpath(path)).click()
        }

        private fun clickr(path: String) {
            robot.find(ComponentFixture::class.java, byXpath(path)).rightClick()
        }

        private fun multiline(input: String) {
            input.split("\n").map { line -> { -> keyboard.enterText(line) } }.reduce { a, b ->
                { ->
                    a()
                    keyboard.enter()
                    b()
                }
            }()
        }

//        private fun isIdeReady(): Boolean {
//            return try {
//                RemoteRobot(url).findAll<ComponentFixture>()
//                true
//            } catch (e: Exception) {
//                e.printStackTrace()
//                false
//            }
//        }
//        private val version = "1.0.9"
//        var ideaProcess: Process? = null
//
//        @JvmStatic
//        @BeforeClass
//        fun setupClass(): Unit {
//            val pluginPath = File("${basePath}\\build\\libs\\intellij-aicoder-${version}.jar").toPath()
//            println("Plugin Path: $pluginPath")
//            require(pluginPath.toFile().exists()) { "Plugin not found" }
//
//            val tmpDir = File("${basePath}\\build\\idea-ui-tests").toPath()
//            tmpDir.toFile().mkdirs()
//            println("Temp Dir: $tmpDir")
//
//            val client = OkHttpClient()
//            val ideDownloader = IdeDownloader(client)
//            val pathToIde: Path = arrayOf(tmpDir).filter { it.toFile().exists() }.firstOrNull()
//                ?: ideDownloader.downloadAndExtract(Ide.IDEA_COMMUNITY, tmpDir)
//            println("IDE Path: $pathToIde")
//            val robotPlugin = arrayOf(File(tmpDir.toFile(),"robot-server-plugin-0.11.16").toPath()).filter { it.toFile().exists() }.firstOrNull()
//                ?: ideDownloader.downloadRobotPlugin(tmpDir)
//            println("Robot Plugin Path: $robotPlugin")
//
//            //FileUtils.writeStringToFile(File(pathToIde.toFile(), "bin\\idea.properties"),"","UTF-8")
//            //FileUtils.writeStringToFile(File(pathToIde.toFile(), "bin\\.vmoptions"),"","UTF-8")
//            //FileUtils.writeStringToFile(File(pathToIde.toFile(), "build.txt"),"IC-232.SNAPSHOT","UTF-8")
//            ideaProcess = IdeLauncher.launchIde(
//                pathToIde = pathToIde,
//                additionalProperties = mapOf("robot-server.port" to 8082),
//                additionalVmOptions = listOf(),
//                requiredPluginsArchives = listOf(robotPlugin, pluginPath),
//                ideSandboxDir = tmpDir
//            )
//            println("IDEA Process: $ideaProcess")
//            while (!ideaProcess!!.isAlive) {
//                println("IDEA Process not started")
//                sleep(5000)
//            }
//            println("IDEA Process started")
//            while (!isIdeReady()) {
//                println("IDEA Is Not Ready")
//                sleep(5000)
//            }
//            println("IDEA Is Alive")
//
//            robot.find(ComponentFixture::class.java, byXpath("//div[@class='WelcomeScreen']")).click()
//            click("//div[@accessiblename='Create New Project' and @class='JButton']")
//        }
//
//        @JvmStatic
//        @AfterClass
//        fun teardownClass(): Unit {
//            try { ideaProcess!!.destroyForcibly() } catch (e: Exception) { }
//        }
    }

}