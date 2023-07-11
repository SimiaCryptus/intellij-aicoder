﻿package com.github.simiacryptus.aicoder.actions

import com.github.simiacryptus.aicoder.config.AppSettingsState
import com.github.simiacryptus.aicoder.util.ComputerLanguage
import com.github.simiacryptus.aicoder.util.MarkdownProcessor
import com.simiacryptus.openai.OpenAIClient
import com.simiacryptus.util.JsonUtil
import org.junit.jupiter.api.Assertions
import java.io.File
import java.util.*

open class ActionTestBase {
    companion object {

        data class SelectionActionTestData(
            val indent: String? = null,
        )

        fun testScript_SelectionAction(selectionAction: SelectionAction, scriptPath: String) {
            AppSettingsState.instance.apiKey = OpenAIClient.keyTxt
            AppSettingsState.instance.temperature = 0.0
            AppSettingsState.instance.useGPT4 = false
            val input =
                selectionAction.javaClass.getResourceAsStream(scriptPath)?.readAllBytes()?.toString(Charsets.UTF_8)
                    ?: ""
            MarkdownProcessor.parse(input).forEach { markdownData ->
                val jsonSection = markdownData.sections.find { it.title.lowercase() == "settings" }
                val fromSection = markdownData.sections.find { it.title.lowercase() == "from" }
                val toSection = markdownData.sections.find { it.title.lowercase() == "to" }
                if (jsonSection != null && fromSection != null && toSection != null) {
                    val testData = JsonUtil.fromJson<SelectionActionTestData>(
                        jsonSection.code,
                        SelectionActionTestData::class.java
                    )
                    val selectionState = SelectionAction.SelectionState(
                        fromSection.code,
                        ComputerLanguage.values().find { fromSection.codeType.equals(it.name, true) },
                        testData.indent ?: ""
                    )
                    val result = selectionAction.processSelection(selectionState)
                    Assertions.assertEquals(
                        toSection.code.trim().replace("\r\n", "\n"),
                        result.trim().replace("\r\n", "\n")
                    )
                } else {
                    throw RuntimeException("Invalid test data")
                }
            }
        }

        data class FileContextActionTestData(
            val filename: String? = null,
        )

        inline fun <reified T : Any> testScript_FileContextAction(
            selectionAction: FileContextAction<T>,
            scriptPath: String
        ) {
            AppSettingsState.instance.apiKey = OpenAIClient.keyTxt
            AppSettingsState.instance.temperature = 0.0
            AppSettingsState.instance.useGPT4 = false
            val input =
                selectionAction.javaClass.getResourceAsStream(scriptPath)?.readAllBytes()?.toString(Charsets.UTF_8)
                    ?: ""
            MarkdownProcessor.parse(input).forEach { markdownData ->
                val configSection = markdownData.sections.find { it.title.lowercase() == "config" }
                if (configSection != null) {
                    val configData = JsonUtil.fromJson<T>(configSection.code, T::class.java)
                    val projectRoot = File("./build/temp/" + UUID.randomUUID().toString()).absoluteFile

                    val inputFiles =
                        markdownData.sections.filter { it.title.lowercase().startsWith("from ") }.take(1).map {
                            val file = File(projectRoot, it.title.substring("from ".length))
                            file.parentFile?.mkdirs()
                            file.writeText(it.code)
                            file
                        }
                    val selectionState = FileContextAction.SelectionState(
                        (inputFiles.firstOrNull() ?: File(projectRoot,"default")).apply { parentFile?.mkdirs() }.absoluteFile,
                        projectRoot.absoluteFile
                    )
                    val result = selectionAction.processSelection(selectionState, configData)
                    Assertions.assertEquals(1, result.size)
                    val file = result.first()
                    val filePath = projectRoot.toPath().relativize(file.toPath()).toString()
                    val find = markdownData.sections.find { it.title.lowercase().replace('\\','/') == ("to " + filePath).lowercase().replace('\\','/') }
                    Assertions.assertTrue((find != null), "Missing section: to " + filePath)
                    Assertions.assertEquals(
                        find!!.code.trim().replace("\r\n", "\n"),
                        file.readText().trim().replace("\r\n", "\n")
                    )
                } else {
                    throw RuntimeException("Invalid test data")
                }
            }
        }
    }

}

