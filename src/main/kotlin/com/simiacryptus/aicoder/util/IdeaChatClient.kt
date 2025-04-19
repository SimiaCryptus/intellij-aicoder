package com.simiacryptus.aicoder.util

import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.DialogWrapper
import com.intellij.ui.components.JBScrollPane
import com.intellij.util.ui.FormBuilder
import com.simiacryptus.aicoder.config.AppSettingsState
import com.simiacryptus.jopenai.ChatClient
import com.simiacryptus.jopenai.models.APIProvider
import com.simiacryptus.jopenai.models.ApiModel.*
import com.simiacryptus.jopenai.models.OpenAIModel
import com.simiacryptus.jopenai.models.TextModel
import com.simiacryptus.skyenet.core.platform.ApplicationServices
import com.simiacryptus.skyenet.core.platform.Session
import com.simiacryptus.skyenet.core.platform.model.User
import com.simiacryptus.util.JsonUtil
import com.simiacryptus.util.JsonUtil.toJson
import org.apache.hc.core5.http.HttpRequest
import org.slf4j.LoggerFactory
import org.slf4j.event.Level
import java.io.File
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicReference
import javax.swing.JPanel
import javax.swing.JTextArea


open class IdeaChatClient(
  key: Map<APIProvider, String> = AppSettingsState.instance.apiKeys?.mapKeys { APIProvider.valueOf(it.key) }?.entries?.toTypedArray()
    ?.associate { it.key to it.value } ?: mapOf(),
  apiBase: Map<APIProvider, String> = AppSettingsState.instance.apiBase?.mapKeys { APIProvider.valueOf(it.key) }?.entries?.toTypedArray()
    ?.associate { it.key to it.value } ?: mapOf(),
  reasoningEffort: ReasoningEffort = ReasoningEffort.valueOf(AppSettingsState.instance.reasoningEffort)
) : ChatClient(
  key = key,
  apiBase = apiBase,
  reasoningEffort = reasoningEffort,
) {

  init {
    //log.info("Initializing OpenAI Client", Throwable())
    require(key.size == apiBase.size) {
      "API Key not configured for all providers: ${key.keys} != ${APIProvider.values().toList()}"
    }
  }

  private class IdeaChildClient(
    val inner: IdeaChatClient,
    key: Map<APIProvider, String>,
    apiBase: Map<APIProvider, String>
  ) : IdeaChatClient(
    key = key,
    apiBase = apiBase,
    reasoningEffort = inner.reasoningEffort
  ) {
    override fun log(level: Level, msg: String) {
      super.log(level, msg)
      inner.log(level, msg)
    }
  }

  override fun getChildClient(): ChatClient = IdeaChildClient(inner = this, key = key, apiBase = apiBase).apply {
    session = inner.session
    user = inner.user
    textCompressor = inner.textCompressor
  }

  private val isInRequest = AtomicBoolean(false)

  override fun onUsage(model: OpenAIModel?, tokens: Usage) {
//        AppSettingsState.instance.tokenCounter += tokens.total_tokens
    ApplicationServices.usageManager.incrementUsage(currentSession, localUser, model!!, tokens)
  }

  override fun authorize(request: HttpRequest, apiProvider: APIProvider) {
    val checkApiKey = key.get(apiProvider) ?: throw IllegalArgumentException("No API Key for $apiProvider")
    key = key.toMutableMap().let {
      it[apiProvider] = checkApiKey
      it
    }.entries.toTypedArray().associate { it.key to it.value }
    super.authorize(request, apiProvider)
  }

  @Suppress("NAME_SHADOWING")
  override fun chat(
    chatRequest: ChatRequest,
    model: TextModel
  ): ChatResponse {
    val storeMetadata = AppSettingsState.instance.storeMetadata
    var chatRequest = chatRequest.copy(
      store = storeMetadata?.let { it.isNotBlank() },
      metadata = storeMetadata?.let { JsonUtil.fromJson(it, Map::class.java) }
    )
    val lastEvent = lastEvent
    lastEvent ?: return super.chat(chatRequest, model)
    chatRequest = chatRequest.copy(
      store = chatRequest.store,
      metadata = chatRequest.metadata?.let {
        it + mapOf(
          "project" to lastEvent.project?.name,
          "action" to lastEvent.presentation.text,
          "language" to lastEvent.getData(CommonDataKeys.PSI_FILE)?.language?.displayName,
        )
      }
    )
    if (isInRequest.getAndSet(true)) {
      val response = super.chat(chatRequest, model)
      if (null != response.usage) {
        UITools.logAction(
          "Chat Response: ${toJson(response.usage!!)}"
        )
      }
      return response
    } else {
      try {
        if (!AppSettingsState.instance.editRequests) {
          val response = super.chat(chatRequest, model)
          if (null != response.usage) {
            UITools.logAction(
              "Chat Response: ${toJson(response.usage!!)}"
            )
          }
          return response
        }
        return withJsonDialog(chatRequest, { chatRequest ->
          UITools.run(
            lastEvent.project, "OpenAI Request", true, suppressProgress = false
          ) {
            val response = super.chat(chatRequest, model)
            if (null != response.usage) {
              UITools.logAction(
                "Chat Response: ${toJson(response.usage!!)}".trim()
              )
            }
            response
          }
        }, "Edit Chat Request")
      } finally {
        isInRequest.set(false)
      }
    }
  }


  companion object {

    val instance
      get() = _instance.apply {
        reasoningEffort = AppSettingsState.instance.reasoningEffort.let(ReasoningEffort::valueOf)
      }
    private val _instance by lazy {
      //log.info("Initializing OpenAI Client", Throwable())
      val client = IdeaChatClient()
      if (AppSettingsState.instance.apiLog) {
        try {
          val file = File(AppSettingsState.instance.pluginHome, "openai.log")
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
      return function(JsonUtil.fromJson(uiEdit(project, title, toJson(request)), request::class.java))
    }

    private val log = LoggerFactory.getLogger(IdeaChatClient::class.java)
    val currentSession = Session.newGlobalID()
    val localUser = User(id = "1", email = "user@localhost")
  }

}