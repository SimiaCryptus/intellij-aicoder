package com.github.simiacryptus.aicoder.actions.markdown

import com.github.simiacryptus.aicoder.actions.ActionTestBase
import com.github.simiacryptus.aicoder.actions.SelectionAction
import com.github.simiacryptus.aicoder.util.ComputerLanguage
import org.junit.Test
import org.junit.jupiter.api.Assertions

class MarkdownImplementActionTest : ActionTestBase() {

    @Test
    fun testProcessing() {
        testScript_SelectionAction(object : MarkdownImplementActionGroup.MarkdownImplementAction("kotlin") {
            override fun processSelection(state: SelectionState, config: String?): String {
                return super.processSelection(state, config).trim().split("\n").drop(1).dropLast(1).joinToString("\n")
            }
        }, "/MarkdownImplementActionTest.md")
    }

    @Test
    fun testIsLanguageSupported() {
        val docAction = MarkdownImplementActionGroup.MarkdownImplementAction("kotlin")
        Assertions.assertFalse(docAction.isLanguageSupported(ComputerLanguage.Kotlin))
        Assertions.assertTrue(docAction.isLanguageSupported(ComputerLanguage.Markdown))
        Assertions.assertFalse(docAction.isLanguageSupported(ComputerLanguage.Text))
    }

    @Test
    fun testEditSelection() {
        val docAction = MarkdownImplementActionGroup.MarkdownImplementAction("kotlin")
        val editorState =
            SelectionAction.EditorState("fun hello() {\nprintln(\"Hello, world!\")\n}", 0, Pair(0, 10), null, arrayOf())
        val result = docAction.editSelection(editorState, 0, 10)
        Assertions.assertEquals(Pair(0, 10), result)
    }

}
