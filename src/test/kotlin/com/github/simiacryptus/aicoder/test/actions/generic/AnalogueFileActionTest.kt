﻿package com.github.simiacryptus.aicoder.test.actions.generic

import com.github.simiacryptus.aicoder.actions.generic.GenerateRelatedFileAction
import com.github.simiacryptus.aicoder.test.actions.ActionTestBase

class AnalogueFileActionTest : ActionTestBase() {
    //    @Test
    fun testProcessing() {
        testScript_FileContextAction(GenerateRelatedFileAction(), "/AnalogueFileActionTest.md")
    }
}

