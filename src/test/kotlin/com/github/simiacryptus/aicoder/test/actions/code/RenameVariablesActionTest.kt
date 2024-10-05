package com.github.simiacryptus.aicoder.test.actions.code

import com.github.simiacryptus.aicoder.test.actions.ActionTestBase
import com.github.simiacryptus.aicoder.actions.SelectionAction
import com.github.simiacryptus.aicoder.actions.legacy.RenameVariablesAction
import com.github.simiacryptus.aicoder.util.ComputerLanguage
import org.junit.Test
import org.junit.jupiter.api.Assertions

class RenameVariablesActionTest : ActionTestBase() {

    @Test
    fun testProcessing() {
        testScript_SelectionAction(object : RenameVariablesAction() {
            override fun choose(renameSuggestions: Map<String, String>): Set<String> {
                return renameSuggestions.keys.toSet()
            }
        }, "/RenameVariablesActionTest.md")
    }

    @Test
    fun testIsLanguageSupported() {
        val docAction = RenameVariablesAction()
        Assertions.assertTrue(docAction.isLanguageSupported(ComputerLanguage.Kotlin))
        Assertions.assertFalse(docAction.isLanguageSupported(ComputerLanguage.Text))
    }

    @Test
    fun testEditSelection() {
        val docAction = RenameVariablesAction()
        val editorState =
            SelectionAction.EditorState("fun hello() {\nprintln(\"Hello, world!\")\n}", 0, Pair(0, 10), null, arrayOf())
        val result = docAction.editSelection(editorState, 0, 10)
        Assertions.assertEquals(Pair(0, 10), result)
    }

}
