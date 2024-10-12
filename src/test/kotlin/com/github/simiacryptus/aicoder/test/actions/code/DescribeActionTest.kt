package com.github.simiacryptus.aicoder.test.actions.code

import com.github.simiacryptus.aicoder.test.actions.ActionTestBase
import com.github.simiacryptus.aicoder.actions.SelectionAction
import com.github.simiacryptus.aicoder.actions.code.DescribeAction
import com.github.simiacryptus.aicoder.util.ComputerLanguage
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions

class DescribeActionTest : ActionTestBase() {

//    @Test
    fun testProcessing() {
        testScript_SelectionAction(DescribeAction(), "/DescribeActionTest.md")
    }

    @Test
    fun testIsLanguageSupported() {
        val docAction = DescribeAction()
        Assertions.assertTrue(docAction.isLanguageSupported(ComputerLanguage.Kotlin))
        Assertions.assertTrue(docAction.isLanguageSupported(ComputerLanguage.Text))
    }

    @Test
    fun testEditSelection() {
        val docAction = DescribeAction()
        val editorState =
            SelectionAction.EditorState("fun hello() {\nprintln(\"Hello, world!\")\n}", 0, Pair(0, 10), null, arrayOf())
        val result = docAction.editSelection(editorState, 0, 10)
        Assertions.assertEquals(Pair(0, 10), result)
    }

}
