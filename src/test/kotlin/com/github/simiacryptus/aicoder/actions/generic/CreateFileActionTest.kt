package com.github.simiacryptus.aicoder.actions.generic

import com.github.simiacryptus.aicoder.actions.ActionTestBase
import org.junit.Test

class CreateFileActionTest : ActionTestBase() {
    @Test
    fun testProcessing() {
        testScript_FileContextAction(CreateFileAction(), "/CreateFileActionTest.md", false)
    }
}