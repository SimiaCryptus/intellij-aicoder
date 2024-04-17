package com.github.simiacryptus.aicoder.actions.generic

import com.github.simiacryptus.aicoder.actions.ActionTestBase
import com.github.simiacryptus.aicoder.actions.SelectionAction
import com.github.simiacryptus.aicoder.actions.code.PasteAction
import com.github.simiacryptus.aicoder.util.ComputerLanguage
import org.junit.Test
import org.junit.jupiter.api.Assertions
import java.awt.Toolkit
import java.awt.datatransfer.StringSelection

class PasteActionTest : ActionTestBase() {

    @Test
    fun testProcessing() {
        testScript_SelectionAction(object : PasteAction() {
            override fun processSelection(state: SelectionState, config: String?): String {
                Toolkit.getDefaultToolkit().systemClipboard.setContents(StringSelection(state.selectedText), null)
                return super.processSelection(state, config)
            }
        }, "/PasteActionTest.md")
    }

    @Test
    fun testIsLanguageSupported() {
        val docAction = PasteAction()
        Assertions.assertTrue(docAction.isLanguageSupported(ComputerLanguage.Kotlin))
        Assertions.assertFalse(docAction.isLanguageSupported(ComputerLanguage.Text))
    }

    @Test
    fun testEditSelection() {
        val docAction = PasteAction()
        val editorState =
            SelectionAction.EditorState("fun hello() {\nprintln(\"Hello, world!\")\n}", 0, Pair(0, 10), null, arrayOf())
        val result = docAction.editSelection(editorState, 0, 10)
        Assertions.assertEquals(Pair(0, 10), result)
    }

}
