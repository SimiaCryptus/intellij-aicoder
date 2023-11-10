package com.github.simiacryptus.aicoder

import com.github.simiacryptus.aicoder.UITestUtil.Companion.awaitBackgroundProgress
import com.github.simiacryptus.aicoder.UITestUtil.Companion.canRunTests
import com.github.simiacryptus.aicoder.UITestUtil.Companion.click
import com.github.simiacryptus.aicoder.UITestUtil.Companion.enterLines
import com.github.simiacryptus.aicoder.UITestUtil.Companion.getEditor
import com.github.simiacryptus.aicoder.UITestUtil.Companion.keyboard
import com.github.simiacryptus.aicoder.UITestUtil.Companion.menuAction
import com.github.simiacryptus.aicoder.UITestUtil.Companion.newFile
import com.github.simiacryptus.aicoder.UITestUtil.Companion.outputDir
import com.github.simiacryptus.aicoder.UITestUtil.Companion.screenshot
import com.github.simiacryptus.aicoder.UITestUtil.Companion.selectText
import com.github.simiacryptus.aicoder.UITestUtil.Companion.testProjectPath
import com.github.simiacryptus.aicoder.UITestUtil.Companion.writeImage
import org.apache.commons.io.FileUtils
import org.junit.Test
import java.awt.event.KeyEvent
import java.io.File
import java.io.FileOutputStream
import java.io.PrintWriter
import java.lang.Thread.sleep

/**
 * See Also:
 *  https://github.com/JetBrains/intellij-ui-test-robot
 *  https://joel-costigliola.github.io/assertj/swing/api/org/assertj/swing/core/Robot.html
 */
class ApplicationDevelopmentUITest {

    @Test
    fun java_test() {
        if (!canRunTests()) return
        val (name, description) = test_problem_calculator()
        val (language, functionalHandle, seedPrompt) = java(name)
        test(name, language, description, seedPrompt, functionalHandle)
    }

    private fun java(name: String) =
        Triple("java", "static void main", """public class $name {\n""")

    @Test
    fun kotlin_test() {
        if (!canRunTests()) return
        val (name, description) = test_problem_calculator()
        val (language, functionalHandle, seedPrompt) = kotlin(name)
        test(name, language, description, seedPrompt, functionalHandle)
    }

    private fun kotlin(name: String) =
        Triple("kt", "fun main", """object $name {\n""")

    @Test
    fun scala_test() {
        if (!canRunTests()) return
        val (name, description) = test_problem_calculator()
        val (language, functionalHandle, seedPrompt) = scala(name)
        test(name, language, description, seedPrompt, functionalHandle)
    }

    private fun scala(name: String) =
        Triple("scala", "def main", """object $name {\n""")

    private fun javascript(name: String) =
        Triple("js", "function main", """function $name() {\n""")

    @Test
    fun javascript_test() {
        if (!canRunTests()) return
        val (name, description) = test_problem_calculator()
        val (language, functionalHandle, seedPrompt) = javascript(name)
        test(name, language, description, seedPrompt, functionalHandle)
    }

    private fun python(name: String) =
        Triple("py", "def main", """def $name():\n""")

    @Test
    fun python_test() {
        if (!canRunTests()) return
        val (name, description) = test_problem_calculator()
        val (language, functionalHandle, seedPrompt) = python(name)
        test(name, language, description, seedPrompt, functionalHandle)
    }

    private fun test_problem_calculator(): Pair<String, List<String>> {
        return Pair(
            "String_Calculator",
            listOf(
                // First, two simple directives
                "Create a utility function to find all simple addition expressions in a string and replace them with the calculated result.",
                "Create a utility function to find all simple multiplication expressions in a string and replace them with the calculated result",
                // This directive is more complex, but can be solved by chaining the previous two directives
                "Implement a utility function to interpret simple math expressions.",
                // Finally, we can test the entire class
                "Implement a static main method that tests each class member and validates the output."
            )
        )
    }


    private fun test(
        name: String,
        language: String,
        description: List<String>,
        seedPrompt: String,
        functionalHandle: String,
        question: String = "What is the big-O runtime and why?"
    ) {
        val reportPrefix = "${name}_${language}_"
        val testOutputFile = File(outputDir, "${name}_${language}.md")
        val out = PrintWriter(FileOutputStream(testOutputFile))
        out.use { out ->
            out.println(
                """
                                
                                # $name
                                
                                In this test we will used AI Coding Assistant to implement the $name class to solve the following problem:
                                
                                """.trimIndent()
            )
            out.println("```\n$description\n```")
            newFile("$name.$language")


            out.println(
                """
                                
                                ## Implementation
                                
                                The first step is to translate the problem into code. We can do this by using the "Insert Implementation" command.
                                
                                """.trimIndent()
            )
            click("//div[@class='EditorComponentImpl']")
            keyboard.selectAll()
            keyboard.key(KeyEvent.VK_DELETE)
            enterLines(seedPrompt)

            for (line in description) {
                click("//div[@class='EditorComponentImpl']")
                newEndOfLine()
                enterLines("// $line")
                writeImage(
                    menuAction("Insert Implementation"), outputDir,
                    name, "${reportPrefix}menu", out
                )
                awaitBackgroundProgress()
                //keyboard.hotKey(KeyEvent.VK_SHIFT, KeyEvent.VK_UP)
                //keyboard.hotKey(KeyEvent.VK_DELETE)
                keyboard.hotKey(KeyEvent.VK_SHIFT, KeyEvent.VK_ALT, KeyEvent.VK_DOWN) // Move current line down
                keyboard.hotKey(KeyEvent.VK_CONTROL, KeyEvent.VK_ALT, KeyEvent.VK_L) // Reformat
                keyboard.hotKey(KeyEvent.VK_CONTROL, KeyEvent.VK_S) // Save
            }


            out.println(
                """
                                
                                This results in the following code:
                                
                                ```$language""".trimIndent()
            )
            out.println(FileUtils.readFileToString(File(testProjectPath, "src/$name.$language"), "UTF-8"))
            out.println("```")


            //                out.println(
            //                    """
            //
            //                    ## Execution
            //
            //                    This code can be executed by pressing the "Run" button in the top right corner of the IDE.
            //                    What could possibly go wrong?
            //
            //                    """.trimIndent()
            //                )
            //                keyboard.hotKey(KeyEvent.VK_CONTROL, KeyEvent.VK_SHIFT, KeyEvent.VK_F10) // Run
            //                awaitRunCompletion()
            //                out.println(
            //                    """
            //
            //                    ```""".trimIndent()
            //                )
            //                out.println(componentText("//div[contains(@accessiblename.key, 'editor.accessible.name')]"))
            //                out.println(
            //                    """
            //                    ```
            //                    """.trimIndent()
            //                )
            //                writeImage(
            //                    screenshot("//div[@class='IdeRootPane']"),
            //                    outputDir,
            //                    name, "${reportPrefix}result", out
            //                )
            //                // Close run tab
            //                sleep(100)
            //                clickr("//div[@class='ContentTabLabel']")
            //                sleep(100)
            //                click("//div[contains(@text.key, 'action.CloseContent.text')]")
            //                sleep(100)


            out.println(
                """
                                
                                ## Rename Variables
                                
                                We can use the "Rename Variables" command to make the code more readable...
                                
                                """.trimIndent()
            )
            keyboard.hotKey(KeyEvent.VK_CONTROL, KeyEvent.VK_HOME) // Move to top
            selectText(
                getEditor(), functionalHandle
            )
            keyboard.hotKey(KeyEvent.VK_CONTROL, KeyEvent.VK_W) // Select function
            writeImage(
                menuAction("Rename Variables"),
                outputDir,
                name, "${reportPrefix}Rename_Variables", out
            )
            awaitBackgroundProgress()
            sleep(1000)
            writeImage(
                screenshot("//div[@class='JDialog']"),
                outputDir,
                name,
                "${reportPrefix}Rename_Variables_Dialog",
                out
            )
            click("//div[@text.key='button.ok']")


            out.println(
                """
                                
                                ## Documentation Comments
                                
                                We also want good documentation for our code. We can use the "Add Documentation Comments" command to do 
                                
                                """.trimIndent()
            )
            selectText(getEditor(), functionalHandle)
            writeImage(
                menuAction("Doc Comments"),
                outputDir,
                name, "${reportPrefix}Add_Doc_Comments", out
            )
            awaitBackgroundProgress()
            keyboard.hotKey(KeyEvent.VK_CONTROL, KeyEvent.VK_HOME) // Move to top
            writeImage(
                screenshot("//div[@class='IdeRootPane']"),
                outputDir,
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
            selectText(getEditor(), functionalHandle)
            keyboard.hotKey(KeyEvent.VK_CONTROL, KeyEvent.VK_W) // Select function
            writeImage(
                menuAction("Ask a question"),
                outputDir,
                name, "${reportPrefix}Ask_Q", out
            )
            click("//div[@class='MultiplexingTextField']")
            keyboard.enterText(question)
            writeImage(
                screenshot("//div[@class='JDialog']"),
                outputDir,
                name, "${reportPrefix}Ask_Q2", out
            )
            click("//div[@text.key='button.ok']")
            awaitBackgroundProgress()
            keyboard.hotKey(KeyEvent.VK_CONTROL, KeyEvent.VK_HOME) // Move to top
            writeImage(
                screenshot("//div[@class='IdeRootPane']"),
                outputDir,
                name, "${reportPrefix}Ask_Q3", out
            )

            out.println(
                """
                                
                                ## Code Comments
                                
                                We can also add code comments to the code. This is useful for explaining the code to other developers.
                                
                                """.trimIndent()
            )
            selectText(getEditor(), functionalHandle)
            keyboard.hotKey(KeyEvent.VK_CONTROL, KeyEvent.VK_W) // Select function
            writeImage(
                menuAction("Code Comments"),
                outputDir,
                name, "${reportPrefix}Add_Code_Comments", out
            )
            awaitBackgroundProgress()
            writeImage(
                screenshot("//div[@class='IdeRootPane']"),
                outputDir,
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
            out.println(FileUtils.readFileToString(File(testProjectPath, "src/$name.$language"), "UTF-8"))
            out.println(
                """
                                ```
                                
                                """.trimIndent()
            )


            // Close editor
            click("//div[@class='InplaceButton']")
        }
    }

    private fun newEndOfLine() {
        keyboard.hotKey(KeyEvent.VK_CONTROL, KeyEvent.VK_END)
        keyboard.hotKey(KeyEvent.VK_LEFT)
        keyboard.hotKey(KeyEvent.VK_ENTER)
        keyboard.hotKey(KeyEvent.VK_LEFT)
        keyboard.hotKey(KeyEvent.VK_TAB)
    }


}
