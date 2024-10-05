package com.github.simiacryptus.aicoder.test

import com.intellij.remoterobot.RemoteRobot
import com.intellij.remoterobot.fixtures.ComponentFixture
import com.intellij.remoterobot.fixtures.dataExtractor.RemoteText
import com.intellij.remoterobot.search.locators.byXpath
import com.intellij.remoterobot.utils.Keyboard
import com.jetbrains.rd.util.firstOrNull
import org.apache.commons.io.FileUtils
import java.awt.Point

import java.awt.event.KeyEvent
import java.awt.image.BufferedImage
import java.io.File
import java.io.FileOutputStream
import java.io.PrintWriter
import java.lang.Thread.sleep
import javax.imageio.ImageIO

/**
 * See Also:
 *  https://github.com/JetBrains/intellij-ui-test-robot
 *  https://joel-costigliola.github.io/assertj/swing/api/org/assertj/swing/core/Robot.html
 */
class UITestUtil {

    companion object {

        val outputDir = File("C:\\Users\\andre\\code\\intellij-aicoder\\build")
        val testProjectPath = File("C:\\Users\\andre\\ideTest\\TestProject")
        private const val testIdeUrl = "http://127.0.0.1:8082"
        private val robot = RemoteRobot(testIdeUrl)
        val keyboard = Keyboard(robot)

        /**
         *
         *  Creates a new file with the given name.
         *
         *  @param name The name of the file to create.
         */
        fun newFile(name: String) {
            click("""//div[@class="ProjectViewTree"]""")
            keyboard.key(KeyEvent.VK_RIGHT)
            keyboard.enterText("src")
            keyboard.key(KeyEvent.VK_CONTEXT_MENU)
            click(
                "//div[contains(@text.key, 'group.NewGroup.text')]",
                "//div[contains(@text.key, 'group.WeighingNewGroup.text')]"
            )
            sleep(500)
            click("""//div[@class="HeavyWeightWindow"][.//div[@class="MyMenu"]]//div[@class="HeavyWeightWindow"]//div[contains(@text.key, 'group.FileMenu.text')]""")
            keyboard.enterText(name)
            keyboard.enter()
            sleep(500)
        }

        private fun isStarted(): Boolean =
            robot.findAll(
                ComponentFixture::class.java,
                byXpath("""//div[contains(@accessiblename.key, 'editor.accessible.name')]""")
            ).isNotEmpty()

        private fun isStillRunning(): Boolean {
            val resultText =
                componentText("//div[contains(@accessiblename.key, 'editor.accessible.name')]")
            return !(resultText.contains("Process finished"))
        }


        private fun componentText(s: String): String {
            return getLines(getComponent(s))
        }

        private fun getLines(element: ComponentFixture): String {
            val lines = element.data.getAll().groupBy { it.point.y }
            return lines.toList().sortedBy { it.first }.map { it.second }
                .map { it.toList().sortedBy { it.point.x }.map { it.text }.reduce { a, b -> a + b } }
                .reduceOrNull { a, b -> a + "\n" + b }.orEmpty()
        }

        private fun findText(element: ComponentFixture, text: String): Pair<Point, Point>? {
            val lines: Map<Int, Iterable<RemoteText>> = element.data.getAll().groupBy { it.point.y }
            val line =
                lines.filter { it.value.map { it.text }.reduce { a, b -> a + b }.contains(text) }.firstOrNull()?.value
                    ?: return null
            val index = line.map { it.text }.reduce { a, b -> a + b }.indexOf(text)
            val lineBuffer = line.toMutableList()
            var left = index
            while (left > 0) {
                val first = lineBuffer.first()
                if (first.text.length <= left) {
                    left -= first.text.length
                    lineBuffer.removeAt(0)
                }
            }
            val leftPoint = lineBuffer.first().point
            left += text.length
            var rightPoint: Point = leftPoint
            while (left > 0) {
                val first = lineBuffer.first()
                if (first.text.length <= left) {
                    left -= first.text.length
                    rightPoint = lineBuffer.removeAt(0).point
                }
            }
            if (lineBuffer.isNotEmpty()) {
                rightPoint = lineBuffer.first().point
            }
            return Pair(leftPoint, rightPoint)
        }

        fun selectText(element: ComponentFixture, text: String) {
            keyboard.hotKey(KeyEvent.VK_CONTROL, KeyEvent.VK_HOME)
            var findText = findText(element, text)
            var pages = 0
            while (null == findText && pages++ < 10) {
                keyboard.hotKey(KeyEvent.VK_PAGE_DOWN)
                findText = findText(element, text)
            }
            val (leftPoint: Point, rightPoint: Point) = findText ?: throw Exception("Could not find text $text")
            element.runJs("robot.pressMouse(component, new Point(${leftPoint.x}, ${leftPoint.y}))")
            element.runJs("robot.moveMouse(component, new Point(${rightPoint.x}, ${rightPoint.y}))")
            element.runJs("robot.releaseMouseButtons()")
        }

        private fun clickText(element: ComponentFixture, text: String) {
            val (leftPoint, _) = findText(element, text) ?: throw Exception("Could not find text $text")
            element.runJs("robot.click(component, new Point(${leftPoint.x}, ${leftPoint.y}))")
        }

        private fun implementCode(prompt: String): BufferedImage {
            click("""//div[@class="EditorComponentImpl"]""")
            keyboard.selectAll()
            keyboard.key(KeyEvent.VK_DELETE)
            enterLines(prompt)
            val image = menuAction("Insert Implementation")
            awaitBackgroundProgress()
            return image
        }

        fun menuAction(menuText: String): BufferedImage {
            keyboard.key(KeyEvent.VK_CONTEXT_MENU)
            sleep(500)
            val aiCoderMenuItem = getComponent("//div[@text='AI Coder']")
            aiCoderMenuItem.click()
            val point1 = aiCoderMenuItem.locationOnScreen
            sleep(500)
            val submenu =
                getComponent("""//div[@class="HeavyWeightWindow"][.//div[@class="MyMenu"]]//div[@class="HeavyWeightWindow"]//div[contains(@text, '$menuText')]""")
            val point2 = submenu.locationOnScreen
            submenu.runJs("robot.moveMouse(new Point(${point2.x}, ${point1.y}))")
            submenu.runJs("robot.moveMouse(component)")
            val image = screenshot("""//div[@class="IdeRootPane"]""")
            submenu.click()
            return image
        }

        /**
         *
         *  Awaits the completion of a running process.
         *
         *  This function will wait until the process is started, then wait until it is finished.
         *  It will also print out the total time the process took to complete.
         */
        private fun awaitRunCompletion() {
            val timeout = 1 * 60 * 1000
            val startOverall = System.currentTimeMillis()

            while (!isStarted()) {
                sleep(1)
                if (System.currentTimeMillis() - startOverall > timeout) {
                    throw RuntimeException("Timeout waiting for process to start")
                }
            }

            val start = System.currentTimeMillis()
            println("Process started")

            while (isStillRunning()) {
                sleep(100)
                if (System.currentTimeMillis() - startOverall > timeout) {
                    throw RuntimeException("Timeout waiting for process to end")
                }
            }

            val end = System.currentTimeMillis()
            println("Process ended after ${(end - start) / 1000.0}")
        }

        fun awaitBackgroundProgress() {
            // Await a background task - it must be open then closed
            val timeout = 1 * 60 * 1000
            val startOverall = System.currentTimeMillis()
            while (!isBackgroundProgressOpen()) {
                sleep(100)
                if (System.currentTimeMillis() - startOverall > timeout) {
                    throw RuntimeException("Timeout waiting for background progress to open")
                }
            }
            val start = System.currentTimeMillis()
            println("Background progress opened")
            while (isBackgroundProgressOpen()) {
                sleep(100)
                if (System.currentTimeMillis() - start > timeout) {
                    throw RuntimeException("Timeout waiting for background progress to close")
                }
            }
            val end = System.currentTimeMillis()
            println("Background progress closed after ${(end - start) / 1000.0}")
        }

        private fun isBackgroundProgressOpen(): Boolean {
            val progressPanel = robot.findAll(
                ComponentFixture::class.java,
                byXpath("""//div[@class="InlineProgressPanel"]""")
            )
            if (progressPanel.isEmpty()) return false
            val componentFixture = progressPanel.get(0)
            val labels = componentFixture.data.getAll()
            return labels.size != 0
        }

        fun documentJavaImplementation(
            name: String,
            directive: String,
            out: PrintWriter,
            reportDir: File
        ) {
            documentImplementation(
                name = name,
                out1 = out,
                directive = directive,
                extension = "java",
                prompt = "public class $name {\\n  // $directive and write a main method to test the code",
                reportDir = reportDir,
                language = "java",
                selector = "static void main"
            )
        }

        private fun documentImplementation(
            name: String,
            out1: PrintWriter,
            directive: String,
            extension: String,
            prompt: String,
            reportDir: File,
            language: String,
            selector: String
        ) {
            val reportPrefix = "${name}_${language}_"
            val testOutputFile = File(outputDir, "$reportPrefix$name.md")

            PrintWriter(FileOutputStream(testOutputFile)).use { report ->
                out1.println("[$directive ($language)]($reportPrefix$name.md)\n\n")

                report.println(
                    """
                        
                        # $name
                        
                        In this test we will used AI Coding Assistant to implement the $name class to solve the following problem:
                        
                        """.trimIndent()
                )
                report.println("```\n$directive\n```")
                newFile("$name.$extension")


                report.println(
                    """
                        
                        ## Implementation
                        
                        The first step is to translate the problem into code. We can do this by using the "Insert Implementation" command.
                        
                        """.trimIndent()
                )
                val image = implementCode(prompt)
                writeImage(image, reportDir, name, "${reportPrefix}menu", report)
                keyboard.hotKey(KeyEvent.VK_SHIFT, KeyEvent.VK_UP)
                keyboard.hotKey(KeyEvent.VK_DELETE)
                keyboard.hotKey(KeyEvent.VK_CONTROL, KeyEvent.VK_ALT, KeyEvent.VK_L) // Reformat
                keyboard.hotKey(KeyEvent.VK_CONTROL, KeyEvent.VK_S) // Save
                report.println(
                    """
                        
                        This results in the following code:
                        
                        ```$language""".trimIndent()
                )
                report.println(FileUtils.readFileToString(File(testProjectPath, "src/$name.$extension"), "UTF-8"))
                report.println("```")


                report.println(
                    """
                        
                        ## Execution
                        
                        This code can be executed by pressing the "Run" button in the top right corner of the IDE. 
                        What could possibly go wrong?
                        
                        """.trimIndent()
                )
                keyboard.hotKey(KeyEvent.VK_CONTROL, KeyEvent.VK_SHIFT, KeyEvent.VK_F10) // Run
                awaitRunCompletion()
                report.println(
                    """
                        
                        ```""".trimIndent()
                )
                report.println(componentText("//div[contains(@accessiblename.key, 'editor.accessible.name')]"))
                report.println(
                    """
                        ```
                        """.trimIndent()
                )
                writeImage(
                    screenshot("""//div[@class="IdeRootPane"]"""),
                    reportDir,
                    name,
                    "${reportPrefix}result",
                    report
                )
                // Close run tab
                sleep(100)
                clickr("""//div[@class="ContentTabLabel"]""")
                sleep(100)
                click("//div[contains(@text.key, 'action.CloseContent.text')]")
                sleep(100)


                report.println(
                    """
                        
                        ## Rename Variables
                        
                        The code is not very readable. We can use the "Rename Variables" command to make it more readable...
                        
                        """.trimIndent()
                )
                keyboard.hotKey(KeyEvent.VK_CONTROL, KeyEvent.VK_HOME) // Move to top
                selectText(getEditor(), selector)
                keyboard.hotKey(KeyEvent.VK_CONTROL, KeyEvent.VK_W) // Select function
                writeImage(menuAction("Rename Variables"), reportDir, name, "${reportPrefix}Rename_Variables", report)
                awaitBackgroundProgress()
                sleep(1000)
                writeImage(
                    screenshot("""//div[@class="JDialog"]"""),
                    reportDir,
                    name,
                    "${reportPrefix}Rename_Variables_Dialog",
                    report
                )
                click("//div[@text.key='button.ok']")


                report.println(
                    """
                        
                        ## Documentation Comments
                        
                        We also want good documentation for our code. We can use the "Add Documentation Comments" command to do this.
                        
                        """.trimIndent()
                )
                selectText(getEditor(), selector)
                writeImage(menuAction("Doc Comments"), reportDir, name, "${reportPrefix}Add_Doc_Comments", report)
                awaitBackgroundProgress()
                keyboard.hotKey(KeyEvent.VK_CONTROL, KeyEvent.VK_HOME) // Move to top
                writeImage(
                    screenshot("""//div[@class="IdeRootPane"]"""),
                    reportDir,
                    name,
                    "${reportPrefix}Add_Doc_Comments2",
                    report
                )


                report.println(
                    """
                        
                        ## Ad-Hoc Questions
                        
                        We can also ask questions about the code. For example, we can ask what the big-O runtime is for this code.
                        
                        """.trimIndent()
                )
                selectText(getEditor(), selector)
                keyboard.hotKey(KeyEvent.VK_CONTROL, KeyEvent.VK_W) // Select function
                writeImage(menuAction("Ask a question"), reportDir, name, "${reportPrefix}Ask_Q", report)
                click("""//div[@class="MultiplexingTextField"]""")
                keyboard.enterText("What is the big-O runtime and why?")
                writeImage(screenshot("""//div[@class="JDialog"]"""), reportDir, name, "${reportPrefix}Ask_Q2", report)
                click("//div[@text.key='button.ok']")
                awaitBackgroundProgress()
                keyboard.hotKey(KeyEvent.VK_CONTROL, KeyEvent.VK_HOME) // Move to top
                writeImage(
                    screenshot("""//div[@class="IdeRootPane"]"""),
                    reportDir,
                    name,
                    "${reportPrefix}Ask_Q3",
                    report
                )

                report.println(
                    """
                        
                        ## Code Comments
                        
                        We can also add code comments to the code. This is useful for explaining the code to other developers.
                        
                        """.trimIndent()
                )
                selectText(getEditor(), selector)
                keyboard.hotKey(KeyEvent.VK_CONTROL, KeyEvent.VK_W) // Select function
                writeImage(menuAction("Code Comments"), reportDir, name, "${reportPrefix}Add_Code_Comments", report)
                awaitBackgroundProgress()
                writeImage(
                    screenshot("""//div[@class="IdeRootPane"]"""),
                    reportDir,
                    name,
                    "${reportPrefix}Add_Code_Comments2",
                    report
                )
                keyboard.hotKey(KeyEvent.VK_CONTROL, KeyEvent.VK_ALT, KeyEvent.VK_L) // Reformat
                keyboard.hotKey(KeyEvent.VK_CONTROL, KeyEvent.VK_S) // Save
                report.println(
                    """
                        
                        ```$language""".trimIndent()
                )
                report.println(FileUtils.readFileToString(File(testProjectPath, "src/$name.$extension"), "UTF-8"))
                report.println(
                    """
                        ```
                        
                        """.trimIndent()
                )


                report.println(
                    """
                        
                        ## Conversion to other languages
                        
                        ### JavaScript
                        
                        We can also convert the code to other languages. For example, we can convert the code to JavaScript.
                        
                        """.trimIndent()
                )
                clickText(getComponent("""//div[@class="ProjectViewTree"]"""), name)
                menuAction("Convert To")
                keyboard.hotKey(KeyEvent.VK_DOWN)
                writeImage(
                    screenshot("""//div[@class="IdeRootPane"]"""),
                    reportDir,
                    name,
                    "${reportPrefix}Convert_to_js",
                    report
                )
                keyboard.hotKey(KeyEvent.VK_ENTER)
                while (!File(testProjectPath, "src/$name.js").exists()) {
                    sleep(1000)
                }
                report.println(
                    """
                        
                        ```js""".trimIndent()
                )
                report.println(FileUtils.readFileToString(File(testProjectPath, "src/$name.js"), "UTF-8"))
                report.println(
                    """
                        ```
                        """.trimIndent()
                )


                report.println(
                    """
                        ### Conversion to Scala
                        
                        We can also convert the code to Scala.
                        
                        """.trimIndent()
                )
                clickText(getComponent("""//div[@class="ProjectViewTree"]"""), name)
                menuAction("Convert To")
                keyboard.hotKey(KeyEvent.VK_DOWN)
                keyboard.hotKey(KeyEvent.VK_DOWN)
                writeImage(
                    screenshot("""//div[@class="IdeRootPane"]"""),
                    reportDir,
                    name,
                    "${reportPrefix}Convert_to_scala",
                    report
                )
                keyboard.hotKey(KeyEvent.VK_ENTER)
                while (!File(testProjectPath, "src/$name.scala").exists()) {
                    sleep(1000)
                }
                report.println(
                    """
                        ```scala""".trimIndent()
                )
                report.println(FileUtils.readFileToString(File(testProjectPath, "src/$name.scala"), "UTF-8"))
                report.println(
                    """
                        ```
                        """.trimIndent()
                )

                // Close editor
                click("""//div[@class="InplaceButton"]""")
            }
        }

        fun documentTextAppend(
            name: String,
            directive: String,
            report: PrintWriter,
            file: File
        ) {
            val reportPrefix = "${name}_"
            report.println("")
            report.println(
                """
            # $name

            In this example, we'll use the "Append Text" command to add some text after our prompt.

            ```
            $directive
            ```

            """.trimIndent()
            )
            newFile("$name.txt")

            click("""//div[@class="EditorComponentImpl"]""")
            keyboard.selectAll()
            keyboard.key(KeyEvent.VK_DELETE)
            enterLines(directive)
            keyboard.selectAll()
            val image = menuAction("Append Text")
            awaitBackgroundProgress()
            writeImage(image, file, name, "${reportPrefix}menu", report)
            keyboard.hotKey(KeyEvent.VK_CONTROL, KeyEvent.VK_S) // Save

            report.println(
                """

            This generates the following text:

            ```""".trimIndent()
            )
            report.println(FileUtils.readFileToString(File(testProjectPath, "src/$name.txt"), "UTF-8"))
            report.println(
                """
            ```

            """.trimIndent()
            )


            report.println(
                """

            ## Edit Text

            We can also edit the text using the "Edit Text" command.

            """.trimIndent()
            )
            click("""//div[@class="EditorComponentImpl"]""")
            keyboard.hotKey(KeyEvent.VK_CONTROL, KeyEvent.VK_A) // Select all
            writeImage(menuAction("Edit Code"), file, name, "${reportPrefix}edit_text", report)
            click("""//div[@class="MultiplexingTextField"]""")
            keyboard.enterText("Translate into a series of haikus")
            writeImage(screenshot("""//div[@class="JDialog"]"""), file, name, "${reportPrefix}edit_text_2", report)
            keyboard.enter()
            awaitBackgroundProgress()
            writeImage(screenshot("""//div[@class="IdeRootPane"]"""), file, name, "${reportPrefix}result1", report)


            report.println(
                """
            
            We can also replace text using the "Replace Options" command.
            
            """.trimIndent()
            )

            keyboard.hotKey(KeyEvent.VK_CONTROL, KeyEvent.VK_S) // Save
            keyboard.hotKey(KeyEvent.VK_CONTROL, KeyEvent.VK_HOME)
            var documentText = FileUtils.readFileToString(File(testProjectPath, "src/$name.txt"), "UTF-8")
            val randomLine =
                documentText.split("\n").map { it.trim() }.filter { it.length < 100 }.take(40).toTypedArray().random()
            selectText(getEditor(), randomLine)
            keyboard.hotKey(KeyEvent.VK_SHIFT, KeyEvent.VK_END)
            writeImage(menuAction("Replace Options"), file, name, "${reportPrefix}replace_options", report)
            awaitBackgroundProgress()
            sleep(500)
            writeImage(
                screenshot("""//div[@class="JDialog"]"""),
                file,
                name,
                "${reportPrefix}replace_options_2",
                report
            )
            sleep(500)

            keyboard.enter()
            writeImage(screenshot("""//div[@class="IdeRootPane"]"""), file, name, "${reportPrefix}result2", report)

            keyboard.hotKey(KeyEvent.VK_CONTROL, KeyEvent.VK_S) // Save
            documentText = FileUtils.readFileToString(File(testProjectPath, "src/$name.txt"), "UTF-8")
            report.println(
                """
            
            Our text now looks like this:
            
            ```""".trimIndent()
            )
            report.println(documentText)
            report.println(
                """
            ```
            
            """.trimIndent()
            )


            // Close editor
            sleep(1000)
            click("""//div[@class="InplaceButton"]""")
        }

        fun getEditor() = getComponent("""//div[@class="EditorComponentImpl"]""")

        fun documentMarkdownListAppend(
            name: String,
            directive: String,
            examples: Array<String>,
            out: PrintWriter,
            file: File
        ) {
            val reportPrefix = "${name}_"
            out.println(
                """
            # $name            
            
            In this demo, we add a list to a Markdown document, and then add items to the list.
            
            ```
            $directive
            ```
            
            """.trimIndent()
            )
            newFile("$name.md")

            click("""//div[@class="EditorComponentImpl"]""")
            keyboard.selectAll()
            keyboard.key(KeyEvent.VK_DELETE)
            enterLines(directive)
            keyboard.enter()
            keyboard.enter()
            keyboard.enterText("1. ")
            for (example in examples) {
                keyboard.enterText(example)
                keyboard.enter()
            }
            keyboard.selectAll()
            writeImage(menuAction("Append Text"), file, name, "${reportPrefix}append_text", out)
            awaitBackgroundProgress()

            keyboard.hotKey(KeyEvent.VK_CONTROL, KeyEvent.VK_S) // Save
            out.println(
                """
            
            ```
            """.trimIndent()
            )
            out.println(FileUtils.readFileToString(File(testProjectPath, "src/$name.md"), "UTF-8"))
            out.println(
                """
            ```
            
            """.trimIndent()
            )
            keyboard.hotKey(KeyEvent.VK_CONTROL, KeyEvent.VK_END) // End of document

            writeImage(screenshot("""//div[@class="IdeRootPane"]"""), file, name, "${reportPrefix}result", out)
            writeImage(menuAction("Add List Items"), file, name, "${reportPrefix}add_list_items", out)
            awaitBackgroundProgress()
            writeImage(screenshot("""//div[@class="IdeRootPane"]"""), file, name, "${reportPrefix}result2", out)

            keyboard.hotKey(KeyEvent.VK_CONTROL, KeyEvent.VK_S) // Save
            out.println(
                """
            
            ```
            """.trimIndent()
            )
            out.println(FileUtils.readFileToString(File(testProjectPath, "src/$name.md"), "UTF-8"))
            out.println(
                """
            ```
            
            """.trimIndent()
            )

            // Close editor
            sleep(1000)
            click("""//div[@class="InplaceButton"]""")
        }


        fun writeImage(
            screenshot: BufferedImage,
            file: File,
            name: String,
            subname: String,
            out: PrintWriter
        ) {
            ImageIO.write(screenshot, "png", File(file, "$name-$subname.png"))
            out.println(
                """
            ![$subname](${name}-$subname.png)
            
            """.trimIndent()
            )
        }

        fun screenshot(path: String) =
            getComponent(path).getScreenshot()

        fun click(vararg path: String) {
            getComponent(*path).click()
        }

        private fun clickr(path: String) {
            getComponent(path).rightClick()
        }

        fun enterLines(input: String) {
            input.split("\n").map { line -> { keyboard.enterText(line, 5) } }.reduce { a, b ->
                {
                    a()
                    keyboard.enter()
                    b()
                }
            }()
        }

        private fun getComponent(vararg paths: String) =
            paths.flatMap { path ->
                try {
                    listOf(robot.find(ComponentFixture::class.java, byXpath(path)))
                } catch (ex: Throwable) {
                    listOf()
                }
            }.first()

        fun canRunTests() = outputDir.exists() && testProjectPath.exists()

    }

}
