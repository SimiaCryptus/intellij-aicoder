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
import java.awt.Dimension
import java.util.concurrent.atomic.AtomicReference
import javax.swing.JPanel
import javax.swing.JTextArea

class IdeaOpenAIClient : OpenAIClient(
    key = AppSettingsState.instance.apiKey,
    apiBase = AppSettingsState.instance.apiBase,
) {

    override fun chat(completionRequest: ChatRequest): ChatResponse {
        lastEvent ?: return super.chat(completionRequest)
        return withJsonDialog(completionRequest) {
            val chatRequest = it
            UITools.run(
                lastEvent!!.project, "OpenAI Request", true, suppressProgress = false
            ) {
                super.chat(chatRequest)
            }
        }
    }

    private fun <T : Any, V : Any> withJsonDialog(
        request: T,
        function: (T) -> V
    ): V {
        val project = lastEvent!!.project
        return function(JsonUtil.fromJson(uiEdit(JsonUtil.toJson(request), project), request::class.java))
    }

    override fun incrementTokens(totalTokens: Int) {
        AppSettingsState.instance.tokenCounter += totalTokens
    }

    override fun authorize(request: HttpRequestBase) {
        key = UITools.checkApiKey(key)
        request.addHeader("Authorization", "Bearer $key")
    }

    companion object {

        val api: OpenAIClient by lazy { IdeaOpenAIClient() }
        var lastEvent: AnActionEvent? = null
        fun uiEdit(jsonTxt: String, project: Project? = null): String {
            val json = JTextArea(
                /* text = */ "",
                /* rows = */ 3,
                /* columns = */ 120
            )
            json.isEditable = true
            json.lineWrap = false
//            json.autoscrolls = true
//            json.maximumSize = Dimension(1024, 800)
//            json.preferredSize = Dimension(600, 200)

            val application = ApplicationManager.getApplication()
            val ref: AtomicReference<String> = AtomicReference()
            if(null != application) {
                application.invokeAndWait { ref.set(uiEdit(jsonTxt, json, project)) }
            } else {
                ref.set(uiEdit(jsonTxt, json, project))
            }
            return ref.get()
        }

        private fun uiEdit(jsonTxt: String, json: JTextArea, project: Project?): String {
            val jbScrollPane = JBScrollPane(json)
            jbScrollPane.horizontalScrollBarPolicy = JBScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED
            jbScrollPane.verticalScrollBarPolicy = JBScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED
            val dialog = object : DialogWrapper(project) {
                init {
                    this.init()
                    this.title = "Edit Request"
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
            log.warn("dialog.initialSize = " + dialog.initialSize)
            json.text = jsonTxt
            dialog.show()
            log.warn("dialog.size = " + dialog.size)
            if (!dialog.isOK) {
                throw RuntimeException("Cancelled")
            }
            return json.text
        }
    }
}