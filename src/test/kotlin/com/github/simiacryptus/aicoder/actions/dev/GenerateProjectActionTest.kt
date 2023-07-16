package com.github.simiacryptus.aicoder.actions.dev

import com.github.simiacryptus.aicoder.actions.ActionTestBase
import com.github.simiacryptus.aicoder.actions.generic.GenerateProjectAction
import org.junit.Test

class GenerateProjectActionTest : ActionTestBase() {

    @Test
    fun testProcessing() {
        testScript_FileContextAction(GenerateProjectAction(), "/GenerateProjectActionTest.md", false)
    }


}