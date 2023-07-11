package com.github.simiacryptus.aicoder.actions.code

import com.github.simiacryptus.aicoder.actions.ActionTestBase
import com.github.simiacryptus.aicoder.actions.SelectionAction
import com.github.simiacryptus.aicoder.util.ComputerLanguage
import org.junit.Test
import org.junit.jupiter.api.Assertions

class InsertImplementationActionTest : ActionTestBase() {

    @Test
    fun testProcessing() {
        testScript_SelectionAction(InsertImplementationAction(), "/InsertImplementationActionTest.md")
    }

    @Test
    fun testIsLanguageSupported() {
        val docAction = InsertImplementationAction()
        Assertions.assertTrue(docAction.isLanguageSupported(ComputerLanguage.Kotlin))
        Assertions.assertFalse(docAction.isLanguageSupported(ComputerLanguage.Text))
    }

    @Test
    fun testEditSelection() {
        val docAction = InsertImplementationAction()
        val editorState = SelectionAction.EditorState("fun hello() {\nprintln(\"Hello, world!\")\n}", 0, Pair(0, 10), null, arrayOf())
        val result = docAction.editSelection(editorState, 0, 10)
        Assertions.assertEquals(Pair(0, 10), result)
    }

}
