package com.github.simiacryptus.aicoder.actions.dev

import com.github.simiacryptus.aicoder.actions.ActionTestBase
import org.junit.Test

class GenerateStoryActionTest : ActionTestBase() {

    @Test
    fun testProcessing() {
        testScript_FileContextAction(GenerateStoryAction(), "/GenerateStoryActionTest.md")
    }


}