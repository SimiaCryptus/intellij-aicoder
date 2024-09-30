package com.github.simiacryptus.aicoder.actions.legacy

import com.github.simiacryptus.aicoder.actions.SelectionAction
import com.github.simiacryptus.aicoder.config.AppSettingsState
import com.simiacryptus.jopenai.models.chatModel
import com.github.simiacryptus.aicoder.util.ComputerLanguage
import com.github.simiacryptus.aicoder.util.TextBlock
import com.github.simiacryptus.aicoder.util.UITools
import com.github.simiacryptus.aicoder.util.psi.PsiClassContext
import com.github.simiacryptus.aicoder.util.psi.PsiUtil
import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.application.runReadAction
import com.intellij.openapi.project.Project
import com.simiacryptus.jopenai.proxy.ChatProxy

class InsertImplementationAction : SelectionAction<String>() {
    override fun getActionUpdateThread() = ActionUpdateThread.BGT

    override fun isEnabled(event: AnActionEvent) = AppSettingsState.instance.enableLegacyActions

    interface VirtualAPI {
        fun implementCode(
            specification: String,
            prefix: String,
            computerLanguage: String,
            humanLanguage: String
        ): ConvertedText

        class ConvertedText {
            var code: String? = null
            var language: String? = null
        }
    }

    private fun getProxy(): VirtualAPI {
        return ChatProxy(
            clazz = VirtualAPI::class.java,
            api = api,
            model = AppSettingsState.instance.smartModel.chatModel(),
            temperature = AppSettingsState.instance.temperature,
            deserializerRetries = 5
        ).create()
    }

    override fun getConfig(project: Project?): String {
        return ""
    }

    override fun defaultSelection(editorState: EditorState, offset: Int): Pair<Int, Int> {
        val foundItem = editorState.contextRanges.filter {
            PsiUtil.matchesType(
                it.name,
                PsiUtil.ELEMENTS_COMMENTS
            )
        }.minByOrNull { it.length() }
        return foundItem?.range() ?: editorState.line
    }

    override fun editSelection(state: EditorState, start: Int, end: Int): Pair<Int, Int> {
        val foundItem = state.contextRanges.filter {
            PsiUtil.matchesType(
                it.name,
                PsiUtil.ELEMENTS_COMMENTS
            )
        }.minByOrNull { it.length() }
        return foundItem?.range() ?: Pair(start, end)
    }

    override fun processSelection(state: SelectionState, config: String?): String {
        val humanLanguage = AppSettingsState.instance.humanLanguage
        val computerLanguage = state.language
        val psiClassContextActionParams = getPsiClassContextActionParams(state)
        val selectedText = state.selectedText ?: ""

        val comment = psiClassContextActionParams.largestIntersectingComment
        var instruct = comment?.subString(state.entireDocument ?: "")?.trim() ?: selectedText
        if (selectedText.split(" ").dropWhile { it.isEmpty() }.size > 4) {
            instruct = selectedText.trim()
        }
        val fromString: TextBlock? = computerLanguage?.getCommentModel(instruct)?.fromString(instruct)
        val specification = fromString?.rawString()?.map { it.toString().trim() }
            ?.filter { it.isNotEmpty() }?.reduce { a, b -> "$a $b" } ?: return selectedText
        val code = if (state.psiFile != null) {
            UITools.run(state.project, "Insert Implementation", true, true) {
                val psiClassContext = runReadAction {
                    PsiClassContext.getContext(
                        state.psiFile,
                        psiClassContextActionParams.selectionStart,
                        psiClassContextActionParams.selectionEnd,
                        computerLanguage
                    ).toString()
                }
                getProxy().implementCode(
                    specification,
                    psiClassContext,
                    computerLanguage.name,
                    humanLanguage
                ).code
            }
        } else {
            getProxy().implementCode(
                specification,
                "",
                computerLanguage.name,
                humanLanguage
            ).code
        }
        return if (code != null) "$selectedText\n${state.indent}$code" else selectedText
    }

    private fun getPsiClassContextActionParams(state: SelectionState): PsiClassContextActionParams {
        val selectionStart = state.selectionOffset
        return PsiClassContextActionParams(
            selectionStart,
            selectionStart + (state.selectionLength ?: 0),
            state.contextRanges.find { PsiUtil.matchesType(it.name, PsiUtil.ELEMENTS_COMMENTS) }
        )
    }

    override fun isLanguageSupported(computerLanguage: ComputerLanguage?): Boolean {
        if (computerLanguage == null || computerLanguage == ComputerLanguage.Text || computerLanguage == ComputerLanguage.Markdown) {
            return false
        }
        return super.isLanguageSupported(computerLanguage)
    }

    private class PsiClassContextActionParams(
        val selectionStart: Int,
        val selectionEnd: Int,
        val largestIntersectingComment: ContextRange?
    )
}