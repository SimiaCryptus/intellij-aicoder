package com.github.simiacryptus.aicoder.actions.generic

import com.github.simiacryptus.aicoder.actions.ActionTestBase
import com.github.simiacryptus.aicoder.actions.SelectionAction
import com.github.simiacryptus.aicoder.actions.legacy.AppendTextWithChatAction
import com.github.simiacryptus.aicoder.util.ComputerLanguage
import org.junit.Test
import org.junit.jupiter.api.Assertions

class AppendActionTest : ActionTestBase() {

    @Test
    fun testProcessing() {
        testScript_SelectionAction(AppendTextWithChatAction(), "/AppendActionTest.md")
    }

    @Test
    fun testIsLanguageSupported() {
        val docAction = AppendTextWithChatAction()
        Assertions.assertTrue(docAction.isLanguageSupported(ComputerLanguage.Kotlin))
        Assertions.assertTrue(docAction.isLanguageSupported(ComputerLanguage.Text))
    }

    @Test
    fun testEditSelection() {
        val docAction = AppendTextWithChatAction()
        val editorState =
            SelectionAction.EditorState("fun hello() {\nprintln(\"Hello, world!\")\n}", 0, Pair(0, 10), null, arrayOf())
        val result = docAction.editSelection(editorState, 0, 10)
        Assertions.assertEquals(Pair(0, 10), result)
    }

}
