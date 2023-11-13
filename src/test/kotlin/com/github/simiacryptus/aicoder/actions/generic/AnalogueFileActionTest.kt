package com.github.simiacryptus.aicoder.actions.generic

import com.github.simiacryptus.aicoder.actions.ActionTestBase
import org.junit.Test

class AnalogueFileActionTest : ActionTestBase() {
    @Test
    fun testProcessing() {
        testScript_FileContextAction(AnalogueFileAction(), "/AnalogueFileActionTest.md")
    }
}

