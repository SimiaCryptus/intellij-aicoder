package com.github.simiacryptus.aicoder.util

import com.intellij.openapi.project.Project
import org.junit.Test

class IdeaOpenAIClientTest {
    @Test
    fun testUiEdit() {
        IdeaOpenAIClient.uiEdit(
            //language=JSON
            jsonTxt = """
                |{
                |  "key": "value"
                |}
                |""".trimMargin(),
            project = null as Project?
        )
    }
}