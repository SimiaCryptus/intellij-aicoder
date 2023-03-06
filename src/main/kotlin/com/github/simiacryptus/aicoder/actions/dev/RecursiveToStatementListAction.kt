package com.github.simiacryptus.aicoder.actions.dev

import com.github.simiacryptus.aicoder.config.AppSettingsState
import com.github.simiacryptus.aicoder.openai.OpenAI_API
import com.github.simiacryptus.aicoder.util.ComputerLanguage
import com.github.simiacryptus.aicoder.util.UITools.getInstruction
import com.github.simiacryptus.aicoder.util.UITools.handle
import com.github.simiacryptus.aicoder.util.UITools.replaceString
import com.github.simiacryptus.aicoder.util.UITools.startProgress
import com.github.simiacryptus.aicoder.util.psi.PsiUtil
import com.google.common.util.concurrent.FutureCallback
import com.google.common.util.concurrent.Futures.*
import com.google.common.util.concurrent.ListenableFuture
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.command.WriteCommandAction
import com.intellij.openapi.util.NlsSafe
import java.util.concurrent.ConcurrentHashMap

class RecursiveToStatementListAction : AnAction() {
    override fun update(e: AnActionEvent) {
        e.presentation.isEnabledAndVisible = isEnabled(e)
        super.update(e)
    }

    override fun actionPerformed(event: AnActionEvent) {
        val settings = AppSettingsState.getInstance()
        val caret = event.getRequiredData(CommonDataKeys.EDITOR).caretModel.primaryCaret
        val languageName = ComputerLanguage.getComputerLanguage(event)!!.name
        val endOffset: Int
        val startOffset: Int
        val text = if (caret.hasSelection()) {
            startOffset = caret.selectionStart
            endOffset = caret.selectionEnd
            caret.selectedText
        } else {
            val psiFile = PsiUtil.getLargestContainedEntity(event) ?: return
            val element = PsiUtil.getSmallestIntersecting(psiFile, caret.offset, caret.offset, "ListItem") ?: return
            startOffset = element.textOffset
            endOffset = element.textOffset + element.textLength
            element.children.map { it.text }.joinToString("\n")
        }
        val progressIndicator = startProgress()
        transform(
            OpenAI_API.complete(event.project, topicsRequest(settings, languageName, text), ""),
            { topicsTxt ->
                val topics: List<String> = topicsTxt.replace("\"".toRegex(), "").split("\n\\d+\\.\\s+".toRegex())
                var future = expand(settings, event, languageName, text!!, topics)
                val document = event.getRequiredData(CommonDataKeys.EDITOR).document
                addCallback(future, object : FutureCallback<List<String>> {
                    override fun onSuccess(result: List<String>) {
                        progressIndicator?.cancel()
                        WriteCommandAction.runWriteCommandAction(event.project) {
                            replaceString(
                                document,
                                startOffset,
                                endOffset,
                                result.distinct().mapIndexed { index, s -> "${index + 1}. $s" }.joinToString("\n")
                            )
                        }
                    }

                    override fun onFailure(t: Throwable) {
                        progressIndicator?.cancel()
                        handle(t)
                    }
                }, OpenAI_API.pool)
            },
            OpenAI_API.pool
        )
    }

    private fun expand(
        settings: AppSettingsState,
        event: AnActionEvent,
        languageName: String,
        text: String,
        topics: List<String>,
        cache: ConcurrentHashMap<String, ListenableFuture<List<String>>> = ConcurrentHashMap<String, ListenableFuture<List<String>>>()
    ): ListenableFuture<List<String>> = cache.computeIfAbsent(text) {
        var rawFuture = OpenAI_API.complete(event.project, completionRequest(settings, languageName, text, topics), "")
        rawFuture = transform(rawFuture, { result -> "1. $result" }, OpenAI_API.pool)
        var listFuture = transform(
            rawFuture,
            { it!!.split("(?<![^\n])\\d+\\.\\s*".toRegex()).map { it.trim() }.filter { it.isNotEmpty() } },
            OpenAI_API.pool
        )
        listFuture = transformAsync(
            listFuture,
            fun(result: List<String>): ListenableFuture<List<String>> =
                if (result.size <= 1) {
                    log.warn("No expansion: $text")
                    immediateFuture(listOf(text))
                } else {
                    log.warn(
                        String.format(
                            "Expanding: \\%s --> \\%s",
                            text.replace("\n", "\n\t"),
                            result.joinToString("\n").replace("\n", "\n\t")
                        )
                    )
                    log.info("Expanding: $text -> $result")
                    transform(allAsList(result.map { expand(settings, event, languageName, it, topics, cache) }), {
                        log.warn(
                            String.format(
                                "Expanded: \\%s --> \\%s --> \\%s",
                                text.replace("\n", "\n\t"),
                                result.joinToString("\n").replace("\n", "\n\t"),
                                it.joinToString("\n").replace("\n", "\n\t")
                            )
                        )
                        it.flatten()
                    }, OpenAI_API.pool)
                },
            OpenAI_API.pool
        )
        catching(listFuture, Throwable::class.java, {
            log.warn("Expansion failed: $text", it)
            listOf(text)
        }, OpenAI_API.pool)
    }

    private fun completionRequest(
        settings: AppSettingsState,
        languageName: String,
        text: @NlsSafe String?,
        topics: List<String>
    ) = settings.createTranslationRequest()
        .setInstruction(getInstruction("""
            Transform into a list of independent statements of fact. 
            Resolve all pronouns and fully qualify each item.
            """.trimIndent().trim()))
        .setInputType(languageName)
        .setInputAttribute("type", "before")
        .setInputText(text)
        .setOutputType(languageName)
        .setOutputAttrute("style", settings.style)
        .setOutputAttrute("type", "after")
        .setOutputAttrute("pronouns", "none")
        .setOutputAttrute("topics", topics.joinToString(","))
        .buildCompletionRequest()
        .appendPrompt("1.")

    private fun topicsRequest(
        settings: AppSettingsState,
        languageName: String,
        text: @NlsSafe String?
    ) = settings.createTranslationRequest()
        .setInstruction(getInstruction("""
            Describe the context of this text by listing terms and topics.
            """.trimIndent()))
        .setInputType(languageName)
        .setInputAttribute("type", "before")
        .setInputText(text)
        .setOutputType(languageName)
        .setOutputAttrute("style", settings.style)
        .setOutputAttrute("type", "after")
        .setOutputAttrute("pronouns", "none")
        .buildCompletionRequest()
        .appendPrompt("1.")

    companion object {
        val log = org.slf4j.LoggerFactory.getLogger(RecursiveToStatementListAction::class.java)!!
        private fun isEnabled(e: AnActionEvent): Boolean {
            if (!AppSettingsState.getInstance().devActions) return false
            val computerLanguage = ComputerLanguage.getComputerLanguage(e) ?: return false
            if (ComputerLanguage.Markdown != computerLanguage) return false
            val caret = e.getData(CommonDataKeys.CARET) ?: return false
            return (PsiUtil.getSmallestIntersecting(
                PsiUtil.getLargestContainedEntity(e) ?: return false,
                caret.selectionStart,
                caret.selectionEnd,
                "ListItem"
            ) != null) || caret.hasSelection()
        }
    }
}


