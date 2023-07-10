package com.github.simiacryptus.aicoder.util

import com.github.simiacryptus.aicoder.config.AppSettingsState
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.DialogWrapper
import com.intellij.ui.components.JBScrollPane
import com.intellij.util.ui.FormBuilder
import com.simiacryptus.openai.OpenAIClient
import com.simiacryptus.util.JsonUtil
import org.apache.http.client.methods.HttpRequestBase
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicReference
import javax.swing.JPanel
import javax.swing.JTextArea

class IdeaOpenAIClient : OpenAIClient(
    key = AppSettingsState.instance.apiKey,
    apiBase = AppSettingsState.instance.apiBase,
) {
    val isInRequest = AtomicBoolean(false)

    override fun incrementTokens(totalTokens: Int) {
        AppSettingsState.instance.tokenCounter += totalTokens
    }

    override fun authorize(request: HttpRequestBase) {
        key = UITools.checkApiKey(key)
        super.authorize(request)
    }

    override fun chat(
        completionRequest: ChatRequest,
        model: Model
    ): ChatResponse {
        lastEvent ?: return super.chat(completionRequest, model)
        if (isInRequest.getAndSet(true)) {
            return super.chat(completionRequest, model)
        } else {
            try {
                if (!AppSettingsState.instance.editRequests) return super.chat(completionRequest, model)
                return withJsonDialog(completionRequest, {
                    val chatRequest = it
                    UITools.run(
                        lastEvent!!.project, "OpenAI Request", true, suppressProgress = false
                    ) {
                        super.chat(chatRequest, model)
                    }
                }, "Edit Chat Request")
            } finally {
                isInRequest.set(false)
            }
        }
    }


    override fun complete(request: CompletionRequest, model: Model): CompletionResponse {
        lastEvent ?: return super.complete(request, model)
        if (isInRequest.getAndSet(true)) {
            return super.complete(request, model)
        } else {
            try {
                if (!AppSettingsState.instance.editRequests) return super.complete(request, model)
                return withJsonDialog(request, {
                    val completionRequest = it
                    UITools.run(
                        lastEvent!!.project, "OpenAI Request", true, suppressProgress = false
                    ) {
                        super.complete(completionRequest, model)
                    }
                }, "Edit Completion Request")
            } finally {
                isInRequest.set(false)
            }
        }
    }

    override fun edit(editRequest: EditRequest): CompletionResponse {
        lastEvent ?: return super.edit(editRequest)
        if (isInRequest.getAndSet(true)) {
            return super.edit(editRequest)
        } else {
            try {
                if (!AppSettingsState.instance.editRequests) return super.edit(editRequest)
                return withJsonDialog(editRequest, {
                    val editRequest = it
                    UITools.run(
                        lastEvent!!.project, "OpenAI Request", true, suppressProgress = false
                    ) {
                        super.edit(editRequest)
                    }
                }, "Edit Edit Request")
            } finally {
                isInRequest.set(false)
            }
        }
    }

    companion object {

        val api: OpenAIClient by lazy { IdeaOpenAIClient() }
        var lastEvent: AnActionEvent? = null
        fun uiEdit(
            project: Project? = null,
            title: String = "Edit Request",
            jsonTxt: String
        ): String {
            return execute {
                val json = JTextArea(
                    /* text = */ "",
                    /* rows = */ 3,
                    /* columns = */ 120
                )
                json.isEditable = true
                json.lineWrap = false
                val jbScrollPane = JBScrollPane(json)
                jbScrollPane.horizontalScrollBarPolicy = JBScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED
                jbScrollPane.verticalScrollBarPolicy = JBScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED
                val dialog = object : DialogWrapper(project) {
                    init {
                        this.init()
                        this.title = title
                        this.setOKButtonText("OK")
                        this.setCancelButtonText("Cancel")
                        this.isResizable = true
                    }

                    override fun createCenterPanel(): JPanel? {
                        val formBuilder = FormBuilder.createFormBuilder()
                        formBuilder.addLabeledComponentFillVertically("JSON", jbScrollPane)
                        return formBuilder.panel
                    }
                }
                json.text = jsonTxt
                dialog.show()
                log.warn("dialog.size = " + dialog.size)
                if (!dialog.isOK) {
                    throw RuntimeException("Cancelled")
                }
                json.text
            } ?: jsonTxt
        }

        fun <T : Any> execute(
            fn: () -> T
        ): T? {
            val application = ApplicationManager.getApplication()
            val ref: AtomicReference<T> = AtomicReference()
            if (null != application) {
                application.invokeAndWait { ref.set(fn()) }
            } else {
                ref.set(fn())
            }
            return ref.get()
        }


        fun <T : Any, V : Any> withJsonDialog(
            request: T,
            function: (T) -> V,
            title: String
        ): V {
            val project = lastEvent?.project ?: return function(request)
            return function(JsonUtil.fromJson(uiEdit(project, title, JsonUtil.toJson(request)), request::class.java))
        }
    }
}
