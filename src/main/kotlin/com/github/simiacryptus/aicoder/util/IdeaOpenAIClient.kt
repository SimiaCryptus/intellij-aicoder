﻿package com.github.simiacryptus.aicoder.util

import com.github.simiacryptus.aicoder.ApplicationEvents
import com.github.simiacryptus.aicoder.config.AppSettingsState
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.DialogWrapper
import com.intellij.ui.components.JBScrollPane
import com.intellij.util.ui.FormBuilder
import com.simiacryptus.jopenai.ApiModel.*
import com.simiacryptus.jopenai.OpenAIClient
import com.simiacryptus.jopenai.models.APIProvider
import com.simiacryptus.jopenai.models.OpenAIModel
import com.simiacryptus.jopenai.models.OpenAITextModel
import com.simiacryptus.jopenai.util.JsonUtil
import com.simiacryptus.skyenet.core.platform.ApplicationServices
import com.simiacryptus.skyenet.core.platform.StorageInterface
import com.simiacryptus.skyenet.core.platform.User
import org.apache.hc.core5.http.HttpRequest
import org.slf4j.LoggerFactory
import java.io.File
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicReference
import javax.swing.JPanel
import javax.swing.JTextArea

class IdeaOpenAIClient : OpenAIClient(
    key = mapOf(APIProvider.valueOf(AppSettingsState.instance.apiProvider) to AppSettingsState.instance.apiKey),
    apiBase = mapOf(APIProvider.valueOf(AppSettingsState.instance.apiProvider) to AppSettingsState.instance.apiBase),
) {
    private val isInRequest = AtomicBoolean(false)

    override fun onUsage(model: OpenAIModel?, tokens: Usage) {
//        AppSettingsState.instance.tokenCounter += tokens.total_tokens
        ApplicationServices.usageManager.incrementUsage(currentSession, localUser, model!!, tokens)
    }

    override fun authorize(request: HttpRequest, apiProvider: APIProvider) {
        val a = APIProvider.valueOf(AppSettingsState.instance.apiProvider)
        val checkApiKey = UITools.checkApiKey(key.get(a)!!)
        key = mapOf(
            APIProvider.valueOf(AppSettingsState.instance.apiProvider) to checkApiKey
        )
        super.authorize(request, APIProvider.valueOf(AppSettingsState.instance.apiProvider))
    }

    @Suppress("NAME_SHADOWING")
    override fun chat(
        chatRequest: ChatRequest,
        model: OpenAITextModel
    ): ChatResponse {
        lastEvent ?: return super.chat(chatRequest, model)
        if (isInRequest.getAndSet(true)) {
            val response = super.chat(chatRequest, model)
            UITools.logAction("""
                |Chat Response: ${JsonUtil.toJson(response.usage!!)}
            """.trimMargin().trim())
            return response
        } else {
            try {
                if (!AppSettingsState.instance.editRequests) {
                    val response = super.chat(chatRequest, model)
                    UITools.logAction("""
                        |Chat Response: ${JsonUtil.toJson(response.usage!!)}
                    """.trimMargin().trim())
                    return response
                }
                return withJsonDialog(chatRequest, { chatRequest ->
                    UITools.run(
                        lastEvent!!.project, "OpenAI Request", true, suppressProgress = false
                    ) {
                        val response = super.chat(chatRequest, model)
                        UITools.logAction("""
                            |Chat Response: ${JsonUtil.toJson(response.usage!!)}
                        """.trimMargin().trim())
                        response
                    }
                }, "Edit Chat Request")
            } finally {
                isInRequest.set(false)
            }
        }
    }


    override fun complete(request: CompletionRequest, model: OpenAITextModel): CompletionResponse {
        lastEvent ?: return super.complete(request, model)
        if (isInRequest.getAndSet(true)) {
            val response = super.complete(request, model)
            UITools.logAction("""
                |Completion Response: ${JsonUtil.toJson(response.usage!!)}
            """.trimMargin().trim())
            return response
        } else {
            try {
                if (!AppSettingsState.instance.editRequests) return super.complete(request, model)
                return withJsonDialog(request, {
                    val completionRequest = it
                    UITools.run(
                        lastEvent!!.project, "OpenAI Request", true, suppressProgress = false
                    ) {
                        val response = super.complete(completionRequest, model)
                        UITools.logAction("""
                            |Completion Response: ${JsonUtil.toJson(response.usage!!)}
                        """.trimMargin().trim())
                        response
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
                return withJsonDialog(editRequest, { request ->
                    UITools.run(
                        lastEvent!!.project, "OpenAI Request", true, suppressProgress = false
                    ) {
                        super.edit(request)
                    }
                }, "Edit Edit Request")
            } finally {
                isInRequest.set(false)
            }
        }
    }

    companion object {

        val instance by lazy {
            val client = IdeaOpenAIClient()
            if (AppSettingsState.instance.apiLog) {
                try {
                    val file = File(ApplicationEvents.pluginHome, "openai.log")
                    file.parentFile.mkdirs()
                    AppSettingsState.auxiliaryLog = file
                    client.logStreams.add(java.io.FileOutputStream(file, file.exists()).buffered())
                } catch (e: Exception) {
                    log.warn("Error initializing log file", e)
                }
            }
            client
        }

        var lastEvent: AnActionEvent? = null
        private fun uiEdit(
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

        private fun <T : Any> execute(
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

        private val log = LoggerFactory.getLogger(IdeaOpenAIClient::class.java)
        val currentSession = StorageInterface.newGlobalID()
        val localUser = User(id = "1", email = "user@localhost")
    }

}
