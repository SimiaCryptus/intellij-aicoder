package com.github.simiacryptus.aicoder.test.actions.code

import com.github.simiacryptus.aicoder.actions.SelectionAction
import com.github.simiacryptus.aicoder.actions.legacy.ImplementStubAction
import com.github.simiacryptus.aicoder.test.actions.ActionTestBase
import com.github.simiacryptus.aicoder.util.ComputerLanguage
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test

class ImplementStubActionTest : ActionTestBase() {

    //    @Test
    fun testProcessing() {
        testScript_SelectionAction(ImplementStubAction(), "/ImplementStubActionTest.md")
    }

    @Test
    fun testIsLanguageSupported() {
        val docAction = ImplementStubAction()
        Assertions.assertTrue(docAction.isLanguageSupported(ComputerLanguage.Kotlin))
        Assertions.assertFalse(docAction.isLanguageSupported(ComputerLanguage.Text))
    }

    @Test
    fun testEditSelection() {
        val docAction = ImplementStubAction()
        val editorState =
            SelectionAction.EditorState("fun hello() {\nprintln(\"Hello, world!\")\n}", 0, Pair(0, 10), null, arrayOf())
        val result = docAction.editSelection(editorState, 0, 10)
        Assertions.assertEquals(Pair(0, 10), result)
    }

}
