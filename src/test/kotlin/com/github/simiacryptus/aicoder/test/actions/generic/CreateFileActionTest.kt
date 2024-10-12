package com.github.simiacryptus.aicoder.test.actions.generic

import com.github.simiacryptus.aicoder.actions.generic.CreateFileFromDescriptionAction
import com.github.simiacryptus.aicoder.test.actions.ActionTestBase
import org.junit.jupiter.api.Test

class CreateFileActionTest : ActionTestBase() {
//    @Test
    fun testProcessing() {
        testScript_FileContextAction(CreateFileFromDescriptionAction(), "/CreateFileActionTest.md")
    }
}