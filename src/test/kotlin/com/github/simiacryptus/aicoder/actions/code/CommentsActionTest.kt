package com.github.simiacryptus.aicoder.actions.code

import com.github.simiacryptus.aicoder.actions.ActionTestBase
import com.github.simiacryptus.aicoder.actions.SelectionAction
import com.github.simiacryptus.aicoder.util.ComputerLanguage
import org.junit.Test
import org.junit.jupiter.api.Assertions

class CommentsActionTest : ActionTestBase() {

    @Test
    fun testProcessing() {
        testActionScript(CommentsAction(), "/CommentsActionTest.md")
    }

    @Test
    fun testIsLanguageSupported() {
        val docAction = DocAction()
        Assertions.assertTrue(docAction.isLanguageSupported(ComputerLanguage.Kotlin))
        Assertions.assertFalse(docAction.isLanguageSupported(ComputerLanguage.Text))
    }

    @Test
    fun testEditSelection() {
        val docAction = DocAction()
        val editorState = SelectionAction.EditorState("fun hello() {\nprintln(\"Hello, world!\")\n}", 0, 10, 0, null)
        val result = docAction.editSelection(editorState, 0, 10)
        Assertions.assertEquals(Pair(0, 10), result)
    }

}
