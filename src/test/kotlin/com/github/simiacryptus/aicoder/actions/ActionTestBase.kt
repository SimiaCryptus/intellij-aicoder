package com.github.simiacryptus.aicoder.actions

import com.github.simiacryptus.aicoder.config.AppSettingsState
import com.github.simiacryptus.aicoder.util.ComputerLanguage
import com.github.simiacryptus.aicoder.util.MarkdownProcessor
import com.simiacryptus.jopenai.models.ChatModels
import com.simiacryptus.jopenai.util.ClientUtil
import com.simiacryptus.jopenai.util.JsonUtil
import org.junit.jupiter.api.Assertions
import java.io.File
import java.util.*

open class ActionTestBase {
    companion object {

        fun <T:Any>testScript_SelectionAction(selectionAction: SelectionAction<T>, scriptPath: String) {
            AppSettingsState.instance.apiKey = ClientUtil.keyTxt
            AppSettingsState.instance.temperature = 0.0
            AppSettingsState.instance.modelName = ChatModels.GPT35Turbo.name
            val input =
                selectionAction.javaClass.getResourceAsStream(scriptPath)?.readAllBytes()?.toString(Charsets.UTF_8)
                    ?: ""
            MarkdownProcessor.parse(input).forEach { markdownData ->
                val jsonSection = markdownData.sections.find { it.title.lowercase() == "settings" }
                val fromSection = markdownData.sections.find { it.title.lowercase() == "from" }
                val toSection = markdownData.sections.find { it.title.lowercase() == "to" }
                if (jsonSection != null && fromSection != null && toSection != null) {
                    val testData = JsonUtil.fromJson<SelectionAction.SelectionState>(
                        jsonSection.code,
                        SelectionAction.SelectionState::class.java
                    )
                    var selectionState = testData.copy(
                        selectedText = fromSection.code,
                        language = ComputerLanguage.values().find { fromSection.codeType.equals(it.name, true) }
                    )
                    if ((selectionState.selectionLength ?: 0) != 0) {
                        selectionState = selectionState.copy(
                            entireDocument = selectionState.selectedText,
                            selectedText = selectionState.selectedText?.substring(
                                selectionState.selectionOffset,
                                selectionState.selectionOffset + selectionState.selectionLength!!
                            ),
                        )
                    }
                    var result = selectionAction.processSelection(null, selectionState, selectionAction.getConfig(null))
                    if ((selectionState.selectionLength ?: 0) != 0) {
                        result = selectionState.entireDocument?.substring(0, selectionState.selectionOffset) +
                                result +
                                selectionState.entireDocument?.substring(selectionState.selectionOffset + selectionState.selectionLength!!)
                    }
                    Assertions.assertEquals(
                        toSection.code.trim().replace("\r\n", "\n"),
                        result.trim().replace("\r\n", "\n")
                    )
                } else {
                    throw RuntimeException("Invalid test data")
                }
            }
        }

        inline fun <reified T : Any> testScript_FileContextAction(
            selectionAction: FileContextAction<T>,
            scriptPath: String
        ) {
            AppSettingsState.instance.apiKey = ClientUtil.keyTxt
            AppSettingsState.instance.temperature = 0.0
            AppSettingsState.instance.modelName = ChatModels.GPT35Turbo.name
            val input =
                selectionAction.javaClass.getResourceAsStream(scriptPath)?.readAllBytes()?.toString(Charsets.UTF_8)
                    ?: ""
            var lastException: Throwable? = null
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
                        (inputFiles.firstOrNull() ?: File(
                            projectRoot,
                            "default"
                        )).apply { parentFile?.mkdirs() }.absoluteFile,
                        projectRoot.absoluteFile
                    )
                    val result = selectionAction.processSelection(selectionState, configData)
                    for (file in result) {
                        try {
                            val filePath = projectRoot.toPath().relativize(file.toPath()).toString()
                            val find = markdownData.sections.find {
                                it.title.lowercase().replace('\\', '/') == ("to " + filePath).lowercase()
                                    .replace('\\', '/')
                            }
                            Assertions.assertTrue((find != null), "Missing section: to " + filePath)
                            Assertions.assertEquals(
                                find!!.code.trim().replace("\r\n", "\n"),
                                file.readText().trim().replace("\r\n", "\n")
                            )
                        } catch (e: Throwable) {
                            e.printStackTrace()
                            lastException = e
                        }
                    }
                } else {
                    throw RuntimeException("Invalid test data")
                }
            }
            if (lastException != null) throw lastException!!
        }
    }

}

