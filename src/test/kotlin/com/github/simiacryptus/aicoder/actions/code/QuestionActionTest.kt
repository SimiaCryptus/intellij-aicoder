package com.github.simiacryptus.aicoder.actions.code

import com.github.simiacryptus.aicoder.actions.ActionTestBase
import com.github.simiacryptus.aicoder.actions.SelectionAction
import com.github.simiacryptus.aicoder.util.ComputerLanguage
import com.intellij.openapi.project.Project
import org.junit.Test
import org.junit.jupiter.api.Assertions

class QuestionActionTest : ActionTestBase() {

    @Test
    fun testProcessing() {
        testScript_SelectionAction(object : QuestionAction() {
            override fun getConfig(project: Project?): String {
                return "How are you feeling?"
            }
        }, "/QuestionActionTest.md")
    }

    @Test
    fun testIsLanguageSupported() {
        val docAction = QuestionAction()
        Assertions.assertTrue(docAction.isLanguageSupported(ComputerLanguage.Kotlin))
        Assertions.assertFalse(docAction.isLanguageSupported(ComputerLanguage.Text))
    }

    @Test
    fun testEditSelection() {
        val docAction = QuestionAction()
        val editorState =
            SelectionAction.EditorState("fun hello() {\nprintln(\"Hello, world!\")\n}", 0, Pair(0, 10), null, arrayOf())
        val result = docAction.editSelection(editorState, 0, 10)
        Assertions.assertEquals(Pair(0, 10), result)
    }

}
