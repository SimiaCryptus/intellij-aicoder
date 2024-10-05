package com.github.simiacryptus.aicoder.test.actions.code

import com.github.simiacryptus.aicoder.test.actions.ActionTestBase
import com.github.simiacryptus.aicoder.actions.SelectionAction
import com.github.simiacryptus.aicoder.actions.legacy.DocAction
import com.github.simiacryptus.aicoder.util.ComputerLanguage
import org.junit.Test
import org.junit.jupiter.api.Assertions.*

class DocActionTest : ActionTestBase() {

    @Test
    fun testProcessing() {
        testScript_SelectionAction(DocAction(), "/DocActionTest.md")
    }

    @Test
    fun testIsLanguageSupported() {
        val docAction = DocAction()
        assertTrue(docAction.isLanguageSupported(ComputerLanguage.Kotlin))
        assertFalse(docAction.isLanguageSupported(ComputerLanguage.Text))
    }

    @Test
    fun testEditSelection() {
        val docAction = DocAction()
        val editorState =
            SelectionAction.EditorState("fun hello() {\nprintln(\"Hello, world!\")\n}", 0, Pair(0, 10), null, arrayOf())
        val result = docAction.editSelection(editorState, 0, 10)
        assertEquals(Pair(0, 10), result)
    }

}


