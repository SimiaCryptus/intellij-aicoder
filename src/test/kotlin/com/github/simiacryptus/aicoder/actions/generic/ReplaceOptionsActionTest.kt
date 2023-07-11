package com.github.simiacryptus.aicoder.actions.generic

import com.github.simiacryptus.aicoder.actions.ActionTestBase
import com.github.simiacryptus.aicoder.actions.SelectionAction
import com.github.simiacryptus.aicoder.util.ComputerLanguage
import org.junit.Test
import org.junit.jupiter.api.Assertions

class ReplaceOptionsActionTest : ActionTestBase() {

    @Test
    fun testProcessing() {
        testScript_SelectionAction(object : ReplaceOptionsAction() {
            override fun choose(choices: List<String>): String {
                return choices.sorted().last()
            }
        }, "/ReplaceOptionsActionTest.md")
    }

    @Test
    fun testIsLanguageSupported() {
        val docAction = ReplaceOptionsAction()
        Assertions.assertTrue(docAction.isLanguageSupported(ComputerLanguage.Kotlin))
        Assertions.assertTrue(docAction.isLanguageSupported(ComputerLanguage.Text))
    }

    @Test
    fun testEditSelection() {
        val docAction = ReplaceOptionsAction()
        val editorState =
            SelectionAction.EditorState("fun hello() {\nprintln(\"Hello, world!\")\n}", 0, Pair(0, 10), null, arrayOf())
        val result = docAction.editSelection(editorState, 0, 10)
        Assertions.assertEquals(Pair(0, 10), result)
    }

}
