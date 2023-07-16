package com.github.simiacryptus.aicoder.actions.code

import com.github.simiacryptus.aicoder.actions.ActionTestBase
import com.github.simiacryptus.aicoder.actions.SelectionAction
import com.github.simiacryptus.aicoder.util.ComputerLanguage
import com.intellij.openapi.project.Project
import org.junit.Test
import org.junit.jupiter.api.Assertions

class CustomEditActionTest : ActionTestBase() {

    private val instruction = "Add code comments"

    @Test
    fun testProcessing() {
        testScript_SelectionAction(object : CustomEditAction() {
            override fun getConfig(project: Project?): String {
                return this@CustomEditActionTest.instruction
            }
        }, "/CustomEditActionTest.md")
    }

    @Test
    fun testIsLanguageSupported() {
        val docAction = object : CustomEditAction() {
            override fun getConfig(project: Project?): String {
                return this@CustomEditActionTest.instruction
            }
        }
        Assertions.assertTrue(docAction.isLanguageSupported(ComputerLanguage.Kotlin))
        Assertions.assertTrue(docAction.isLanguageSupported(ComputerLanguage.Text))
    }

    @Test
    fun testEditSelection() {
        val docAction = object : CustomEditAction() {
            override fun getConfig(project: Project?): String {
                return this@CustomEditActionTest.instruction
            }
        }
        val editorState = SelectionAction.EditorState("fun hello() {\nprintln(\"Hello, world!\")\n}", 0, Pair(0, 10), null, arrayOf())
        val result = docAction.editSelection(editorState, 0, 10)
        Assertions.assertEquals(Pair(0, 10), result)
    }

}
