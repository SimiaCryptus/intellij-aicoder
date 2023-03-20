package com.github.simiacryptus.aicoder

import com.intellij.remoterobot.RemoteRobot
import com.intellij.remoterobot.fixtures.ComponentFixture
import com.intellij.remoterobot.fixtures.dataExtractor.RemoteText
import com.intellij.remoterobot.search.locators.byXpath
import com.intellij.remoterobot.utils.Keyboard
import com.jetbrains.rd.util.first
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

    public companion object {

        val outputDir = File("C:\\Users\\andre\\code\\aicoder\\intellij-aicoder-docs")
        val testProjectPath = File("C:\\Users\\andre\\IdeaProjects\\automated-ui-test-workspace")
        private val testIdeUrl = "http://127.0.0.1:8082"
        private val robot = RemoteRobot(testIdeUrl)
        val keyboard = Keyboard(robot)

        /**
         *
         *  Creates a new file with the given name.
         *
         *  @param name The name of the file to create.
         */
        fun newFile(name: String) {
            click("//div[@class='ProjectViewTree']")
            keyboard.key(KeyEvent.VK_RIGHT)
            keyboard.enterText("src")
            keyboard.key(KeyEvent.VK_CONTEXT_MENU)
            click(
                "//div[contains(@text.key, 'group.NewGroup.text')]",
                "//div[contains(@text.key, 'group.WeighingNewGroup.text')]"
            )
            sleep(500)
            click("//div[@class='HeavyWeightWindow'][.//div[@class='MyMenu']]//div[@class='HeavyWeightWindow']//div[contains(@text.key, 'group.FileMenu.text')]")
            keyboard.enterText(name)
            keyboard.enter()
            sleep(500)
        }

        private fun isStarted(): Boolean =
            robot.findAll(
                ComponentFixture::class.java,
                byXpath("//div[contains(@accessiblename.key, 'editor.accessible.name')]")
            ).isNotEmpty()

        private fun isDialogOpen(): Boolean =
            robot.findAll(ComponentFixture::class.java, byXpath("//div[@class='EngravedLabel']")).isNotEmpty()

        private fun isStillRunning(): Boolean {
            val resultText =
                componentText("//div[contains(@accessiblename.key, 'editor.accessible.name')]")
            return !(resultText.contains("Process finished"))
        }


        fun componentText(s: String): String {
            return getLines(getComponent(s))
        }

        private fun getLines(element: ComponentFixture): String {
            val lines = element.data.getAll().groupBy { it.point.y }
            return lines.toList().sortedBy { it.first }.map { it.second }
                .map { it.toList().sortedBy { it.point.x }.map { it.text }.reduce { a, b -> a + b } }
                .reduceOrNull { a, b -> a + "\n" + b }.orEmpty()
        }

        fun findText(element: ComponentFixture, text: String): Pair<Point, Point>? {
            val lines: Map<Int, Iterable<RemoteText>> = element.data.getAll().groupBy { it.point.y }
            val line = lines.filter { it.value.map { it.text }.reduce { a, b -> a + b }.contains(text) }.firstOrNull()?.value
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

        fun clickText(element: ComponentFixture, text: String) {
            val (leftPoint, _) = findText(element, text) ?: throw Exception("Could not find text $text")
            element.runJs("robot.click(component, new Point(${leftPoint.x}, ${leftPoint.y}))")
        }

        fun rightClickText(element: ComponentFixture, text: String) {
            val (leftPoint, _) = findText(element, text) ?: throw Exception("Could not find text $text")
            element.runJs("robot.rightClick(component, new Point(${leftPoint.x}, ${leftPoint.y}))")
        }

        /**
         *
         *  Implements a Java class with the given name and task.
         *
         *  @param name The name of the class to be implemented.
         *  @param task The task to be implemented in the class.
         *  @return A [BufferedImage] of the command used.
         */
        fun implementCode(prompt: String): BufferedImage {
            click("//div[@class='EditorComponentImpl']")
            keyboard.selectAll()
            keyboard.key(KeyEvent.VK_DELETE)
            enterLines(prompt)
            val image = menuAction("Insert Implementation")
            awaitProcessing()
            return image
        }

        fun menuAction(menuText: String): BufferedImage {
            keyboard.key(KeyEvent.VK_CONTEXT_MENU)
            sleep(100)
            val aiCoderMenuItem = getComponent("//div[@text='AI Coder']")
            aiCoderMenuItem.click()
            val point1 = aiCoderMenuItem.locationOnScreen
            sleep(100)
            val submenu =
                getComponent("//div[@class='HeavyWeightWindow'][.//div[@class='MyMenu']]//div[@class='HeavyWeightWindow']//div[contains(@text, '$menuText')]")
            val point2 = submenu.locationOnScreen
            submenu.runJs("robot.moveMouse(new Point(${point2.x}, ${point1.y}))")
            submenu.runJs("robot.moveMouse(component)")
            val image = screenshot("//div[@class='IdeRootPane']")
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
        fun awaitRunCompletion() {
            while (!isStarted()) sleep(1)
            val start = System.currentTimeMillis()
            println("Process started")
            while (isStillRunning()) {
                sleep(100)
            }
            val end = System.currentTimeMillis()
            println("Process ended after ${(end - start) / 1000.0}")
        }

        fun awaitProcessing() {
            while (!isDialogOpen()) sleep(1)
            val start = System.currentTimeMillis()
            println("Dialog opened")
            while (isDialogOpen()) sleep(100)
            val end = System.currentTimeMillis()
            println("Dialog closed after ${(end - start) / 1000.0}")
        }

        /**
         *
         *  Documents the Java implementation of the given name and directive.
         *
         *  @param name The name of the Java implementation.
         *  @param directive The directive for the Java implementation.
         *  @param out The [PrintWriter] to write the documentation to.
         *  @param reportDir The [File] directory to save the screenshots to.
         */
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
                prompt = "public class $name {\\n  // $directive",
                reportDir = reportDir,
                language = "java",
                selector = "static void main"
            )
        }

        fun documentImplementation(
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
            val out2 = PrintWriter(FileOutputStream(testOutputFile))

            try {
                out1.println("[$directive ($language)]($reportPrefix$name.md)\n\n")
                val out = out2

                out.println(
                    """
                
                # $name
                
                In this test we will used AI Coding Assistant to implement the $name class to solve the following problem:
                
                """.trimIndent()
                )
                out.println("```\n$directive\n```")
                newFile("$name.$extension")


                out.println(
                    """
                
                ## Implementation
                
                The first step is to translate the problem into code. We can do this by using the "Insert Implementation" command.
                
                """.trimIndent()
                )
                val image = implementCode(prompt)
                writeImage(image, reportDir, name, "${reportPrefix}menu", out)
                keyboard.hotKey(KeyEvent.VK_SHIFT, KeyEvent.VK_UP)
                keyboard.hotKey(KeyEvent.VK_DELETE)
                keyboard.hotKey(KeyEvent.VK_CONTROL, KeyEvent.VK_ALT, KeyEvent.VK_L) // Reformat
                keyboard.hotKey(KeyEvent.VK_CONTROL, KeyEvent.VK_S) // Save
                out.println(
                    """
                
                This results in the following code:
                
                ```$language""".trimIndent()
                )
                out.println(FileUtils.readFileToString(File(testProjectPath, "src/$name.$extension"), "UTF-8"))
                out.println("```")


                out.println(
                    """
                
                ## Execution
                
                This code can be executed by pressing the "Run" button in the top right corner of the IDE. 
                What could possibly go wrong?
                
                """.trimIndent()
                )
                keyboard.hotKey(KeyEvent.VK_CONTROL, KeyEvent.VK_SHIFT, KeyEvent.VK_F10) // Run
                awaitRunCompletion()
                out.println(
                    """
                
                ```""".trimIndent()
                )
                out.println(componentText("//div[contains(@accessiblename.key, 'editor.accessible.name')]"))
                out.println(
                    """
                ```
                """.trimIndent()
                )
                writeImage(screenshot("//div[@class='IdeRootPane']"), reportDir, name, "${reportPrefix}result", out)
                // Close run tab
                sleep(100)
                clickr("//div[@class='ContentTabLabel']")
                sleep(100)
                click("//div[contains(@text.key, 'action.CloseContent.text')]")
                sleep(100)


                out.println(
                    """
                
                ## Rename Variables
                
                The code is not very readable. We can use the "Rename Variables" command to make it more readable...
                
                """.trimIndent()
                )
                keyboard.hotKey(KeyEvent.VK_CONTROL, KeyEvent.VK_HOME) // Move to top
                selectText(getEditor(), selector)
                keyboard.hotKey(KeyEvent.VK_CONTROL, KeyEvent.VK_W) // Select function
                writeImage(menuAction("Rename Variables"), reportDir, name, "${reportPrefix}Rename_Variables", out)
                awaitProcessing()
                sleep(1000)
                writeImage(
                    screenshot("//div[@class='JDialog']"),
                    reportDir,
                    name,
                    "${reportPrefix}Rename_Variables_Dialog",
                    out
                )
                click("//div[@text.key='button.ok']")


                out.println(
                    """
                
                ## Documentation Comments
                
                We also want good documentation for our code. We can use the "Add Documentation Comments" command to do this.
                
                """.trimIndent()
                )
                selectText(getEditor(), selector)
                writeImage(menuAction("Doc Comments"), reportDir, name, "${reportPrefix}Add_Doc_Comments", out)
                awaitProcessing()
                keyboard.hotKey(KeyEvent.VK_CONTROL, KeyEvent.VK_HOME) // Move to top
                writeImage(
                    screenshot("//div[@class='IdeRootPane']"),
                    reportDir,
                    name,
                    "${reportPrefix}Add_Doc_Comments2",
                    out
                )


                out.println(
                    """
                
                ## Ad-Hoc Questions
                
                We can also ask questions about the code. For example, we can ask what the big-O runtime is for this code.
                
                """.trimIndent()
                )
                selectText(getEditor(), selector)
                keyboard.hotKey(KeyEvent.VK_CONTROL, KeyEvent.VK_W) // Select function
                writeImage(menuAction("Ask a question"), reportDir, name, "${reportPrefix}Ask_Q", out)
                click("//div[@class='MultiplexingTextField']")
                keyboard.enterText("What is the big-O runtime and why?")
                writeImage(screenshot("//div[@class='JDialog']"), reportDir, name, "${reportPrefix}Ask_Q2", out)
                click("//div[@text.key='button.ok']")
                awaitProcessing()
                keyboard.hotKey(KeyEvent.VK_CONTROL, KeyEvent.VK_HOME) // Move to top
                writeImage(screenshot("//div[@class='IdeRootPane']"), reportDir, name, "${reportPrefix}Ask_Q3", out)

                out.println(
                    """
                
                ## Code Comments
                
                We can also add code comments to the code. This is useful for explaining the code to other developers.
                
                """.trimIndent()
                )
                selectText(getEditor(), selector)
                keyboard.hotKey(KeyEvent.VK_CONTROL, KeyEvent.VK_W) // Select function
                writeImage(menuAction("Code Comments"), reportDir, name, "${reportPrefix}Add_Code_Comments", out)
                awaitProcessing()
                writeImage(
                    screenshot("//div[@class='IdeRootPane']"),
                    reportDir,
                    name,
                    "${reportPrefix}Add_Code_Comments2",
                    out
                )
                keyboard.hotKey(KeyEvent.VK_CONTROL, KeyEvent.VK_ALT, KeyEvent.VK_L) // Reformat
                keyboard.hotKey(KeyEvent.VK_CONTROL, KeyEvent.VK_S) // Save
                out.println(
                    """
                
                ```$language""".trimIndent()
                )
                out.println(FileUtils.readFileToString(File(testProjectPath, "src/$name.$extension"), "UTF-8"))
                out.println(
                    """
                ```
                
                """.trimIndent()
                )


                out.println(
                    """
                
                ## Conversion to other languages
                
                ### JavaScript
                
                We can also convert the code to other languages. For example, we can convert the code to JavaScript.
                
                """.trimIndent()
                )
                clickText(getComponent("//div[@class='ProjectViewTree']"), name)
                menuAction("Convert To")
                keyboard.hotKey(KeyEvent.VK_DOWN)
                writeImage(
                    screenshot("//div[@class='IdeRootPane']"),
                    reportDir,
                    name,
                    "${reportPrefix}Convert_to_js",
                    out
                )
                keyboard.hotKey(KeyEvent.VK_ENTER)
                while (!File(testProjectPath, "src/$name.js").exists()) {
                    sleep(1000)
                }
                out.println(
                    """
                
                ```js""".trimIndent()
                )
                out.println(FileUtils.readFileToString(File(testProjectPath, "src/$name.js"), "UTF-8"))
                out.println(
                    """
                ```
                """.trimIndent()
                )


                out.println(
                    """
                ### Conversion to Scala
                
                We can also convert the code to Scala.
                
                """.trimIndent()
                )
                clickText(getComponent("//div[@class='ProjectViewTree']"), name)
                menuAction("Convert To")
                keyboard.hotKey(KeyEvent.VK_DOWN)
                keyboard.hotKey(KeyEvent.VK_DOWN)
                writeImage(
                    screenshot("//div[@class='IdeRootPane']"),
                    reportDir,
                    name,
                    "${reportPrefix}Convert_to_scala",
                    out
                )
                keyboard.hotKey(KeyEvent.VK_ENTER)
                while (!File(testProjectPath, "src/$name.scala").exists()) {
                    sleep(1000)
                }
                out.println(
                    """
                ```scala""".trimIndent()
                )
                out.println(FileUtils.readFileToString(File(testProjectPath, "src/$name.scala"), "UTF-8"))
                out.println(
                    """
                ```
                """.trimIndent()
                )

                // Close editor
                click("//div[@class='InplaceButton']")
            } finally {
                out2.close()
            }
        }

        fun documentTextAppend(
            name: String,
            directive: String,
            out: PrintWriter,
            file: File
        ) {
            val reportPrefix = "${name}_"
            out.println("")
            out.println(
                """
            # $name

            In this example, we'll use the "Append Text" command to add some text after our prompt.

            ```
            $directive
            ```

            """.trimIndent()
            )
            newFile("$name.txt")

            click("//div[@class='EditorComponentImpl']")
            keyboard.selectAll()
            keyboard.key(KeyEvent.VK_DELETE)
            enterLines(directive)
            keyboard.selectAll()
            val image1 = menuAction("Append Text")
            awaitProcessing()
            val image = image1
            writeImage(image, file, name, "${reportPrefix}menu", out)
            keyboard.hotKey(KeyEvent.VK_CONTROL, KeyEvent.VK_S) // Save

            out.println(
                """

            This generates the following text:

            ```""".trimIndent()
            )
            out.println(FileUtils.readFileToString(File(testProjectPath, "src/$name.txt"), "UTF-8"))
            out.println(
                """
            ```

            """.trimIndent()
            )


            out.println(
                """

            ## Edit Text

            We can also edit the text using the "Edit Text" command.

            """.trimIndent()
            )
            click("//div[@class='EditorComponentImpl']")
            keyboard.hotKey(KeyEvent.VK_CONTROL, KeyEvent.VK_A) // Select all
            writeImage(menuAction("Edit Text"), file, name, "${reportPrefix}edit_text", out)
            click("//div[@class='MultiplexingTextField']")
            keyboard.enterText("Translate into a series of haikus")
            writeImage(screenshot("//div[@class='JDialog']"), file, name, "${reportPrefix}edit_text_2", out)
            keyboard.enter()
            awaitProcessing()
            writeImage(screenshot("//div[@class='IdeRootPane']"), file, name, "${reportPrefix}result1", out)


            out.println(
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
            writeImage(menuAction("Replace Options"), file, name, "${reportPrefix}replace_options", out)
            awaitProcessing()
            sleep(500)
            writeImage(screenshot("//div[@class='JDialog']"), file, name, "${reportPrefix}replace_options_2", out)
            sleep(500)

            keyboard.enter()
            writeImage(screenshot("//div[@class='IdeRootPane']"), file, name, "${reportPrefix}result2", out)

            keyboard.hotKey(KeyEvent.VK_CONTROL, KeyEvent.VK_S) // Save
            documentText = FileUtils.readFileToString(File(testProjectPath, "src/$name.txt"), "UTF-8")
            out.println(
                """
            
            Our text now looks like this:
            
            ```""".trimIndent()
            )
            out.println(documentText)
            out.println(
                """
            ```
            
            """.trimIndent()
            )


            // Close editor
            sleep(1000)
            click("//div[@class='InplaceButton']")
        }

        fun getEditor() = getComponent("//div[@class='EditorComponentImpl']")

        fun documentMarkdownTableOps(
            name: String,
            directive: String,
            out: PrintWriter,
            file: File
        ) {
            val reportPrefix = "${name}_"
            out.println("")
            out.println(
                """
            # $name
            
            In this demo, we add a table to a Markdown document, and then add columns and rows to the table.
            
            We start with this seed directive:
            
            ```
            $directive
            ```
            
            """.trimIndent()
            )
            newFile("$name.md")

            click("//div[@class='EditorComponentImpl']")
            keyboard.selectAll()
            keyboard.key(KeyEvent.VK_DELETE)
            enterLines(
                """
            $directive
            
            |""".trimIndent()
            )
            keyboard.selectAll()
            writeImage(menuAction("Append Text"), file, name, "${reportPrefix}menu", out)
            awaitProcessing()

            keyboard.hotKey(KeyEvent.VK_CONTROL, KeyEvent.VK_S) // Save
            out.println(
                """
            This gives us the following Markdown document:
            
            ```
            """.trimIndent()
            )
            out.println(FileUtils.readFileToString(File(testProjectPath, "src/$name.md"), "UTF-8"))
            out.println(
                """
            ```
            
            """.trimIndent()
            )

            keyboard.hotKey(KeyEvent.VK_CONTROL, KeyEvent.VK_END)
            writeImage(menuAction("Add Table Columns"), file, name, "${reportPrefix}add_columns", out)
            awaitProcessing()

            keyboard.hotKey(KeyEvent.VK_CONTROL, KeyEvent.VK_S) // Save
            out.println(
                """
            
            ```""".trimIndent()
            )
            out.println(FileUtils.readFileToString(File(testProjectPath, "src/$name.md"), "UTF-8"))
            out.println(
                """
            ```
            
            """.trimIndent()
            )

            keyboard.hotKey(KeyEvent.VK_CONTROL, KeyEvent.VK_Z) // Undo
            writeImage(menuAction("Add Table Rows"), file, name, "${reportPrefix}add_rows", out)
            awaitProcessing()

            keyboard.hotKey(KeyEvent.VK_CONTROL, KeyEvent.VK_S) // Save
            out.println(
                """
            
            ```""".trimIndent()
            )
            out.println(FileUtils.readFileToString(File(testProjectPath, "src/$name.md"), "UTF-8"))
            out.println(
                """
            ```
            
            """.trimIndent()
            )

            writeImage(screenshot("//div[@class='IdeRootPane']"), file, name, "${reportPrefix}result", out)

            // Close editor
            sleep(1000)
            click("//div[@class='InplaceButton']")
        }

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

            click("//div[@class='EditorComponentImpl']")
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
            awaitProcessing()

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

            writeImage(screenshot("//div[@class='IdeRootPane']"), file, name, "${reportPrefix}result", out)
            writeImage(menuAction("Add List Items"), file, name, "${reportPrefix}add_list_items", out)
            awaitProcessing()
            writeImage(screenshot("//div[@class='IdeRootPane']"), file, name, "${reportPrefix}result2", out)

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
            click("//div[@class='InplaceButton']")
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

        fun clickr(path: String) {
            getComponent(path).rightClick()
        }

        fun enterLines(input: String) {
            input.split("\n").map { line -> { -> keyboard.enterText(line, 5) } }.reduce { a, b ->
                { ->
                    a()
                    keyboard.enter()
                    b()
                }
            }()
        }

        fun getComponent(vararg paths: String) =
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