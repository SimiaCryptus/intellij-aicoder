package com.github.simiacryptus.aicoder.actions

import com.github.simiacryptus.aicoder.util.MarkdownProcessor
import com.github.simiacryptus.aicoder.actions.code.DocAction
import com.github.simiacryptus.aicoder.config.AppSettingsState
import com.github.simiacryptus.aicoder.util.ComputerLanguage
import com.simiacryptus.openai.OpenAIClient
import com.simiacryptus.util.JsonUtil
import org.junit.jupiter.api.Assertions

open class ActionTestBase {
    companion object {

        data class TestData(
            val indent : String? = null,
        )

        fun testActionScript(docAction: SelectionAction, scriptPath: String) {
            AppSettingsState.instance.apiKey = OpenAIClient.keyTxt
            AppSettingsState.instance.temperature = 0.0
            AppSettingsState.instance.useGPT4 = false
            MarkdownProcessor.parse(
                docAction.javaClass.getResourceAsStream(scriptPath)?.readAllBytes()?.toString(Charsets.UTF_8) ?: ""
            ).forEach { markdownData ->
                val jsonSection = markdownData.sections.find { it.title.lowercase() == "settings" }
                val fromSection = markdownData.sections.find { it.title.lowercase() == "from" }
                val toSection = markdownData.sections.find { it.title.lowercase() == "to" }
                if (jsonSection != null && fromSection != null && toSection != null) {
                    val testData = JsonUtil.fromJson<TestData>(jsonSection.code, TestData::class.java)
                    val selectionState = SelectionAction.SelectionState(
                        fromSection.code,
                        ComputerLanguage.values().find { fromSection.codeType.equals(it.name, true) },
                        testData.indent ?: ""
                    )
                    val result = docAction.processSelection(selectionState)
                    Assertions.assertEquals(toSection.code.trim().replace("\r\n", "\n"), result.trim().replace("\r\n", "\n"))
                } else {
                    throw RuntimeException("Invalid test data")
                }
            }
        }
    }

}

