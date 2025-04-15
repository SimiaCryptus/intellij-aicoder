package com.simiacryptus.aicoder.config

/**
  var transcriptionModel: String = AudioModels.Whisper.modelName
 * Stores and manages plugin configuration settings.
 *
 * This class is responsible for persisting and retrieving the plugin's
 * configuration settings. It uses the IntelliJ Platform's persistence
 * framework to save settings across IDE restarts.
 */
import com.fasterxml.jackson.annotation.JsonIgnore
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.components.PersistentStateComponent
import com.intellij.openapi.components.State
import com.intellij.openapi.components.Storage
import com.intellij.openapi.util.io.FileUtil
import com.intellij.util.xmlb.XmlSerializerUtil
import com.simiacryptus.jopenai.models.APIProvider
import com.simiacryptus.jopenai.models.ImageModels
import com.simiacryptus.jopenai.models.OpenAIModels
import com.simiacryptus.skyenet.apps.general.PatchApp
import com.simiacryptus.skyenet.apps.plan.TaskSettingsBase
import com.simiacryptus.util.JsonUtil.fromJson
import com.simiacryptus.util.JsonUtil.toJson
import com.fasterxml.jackson.databind.JsonNode
import org.slf4j.LoggerFactory
import java.io.File

data class CommandConfig(
  val commands: List<PatchApp.CommandSettings>,
  val exitCodeOption: String,
  val autoFix: Boolean,
  val maxRetries: Int,
  val additionalInstructions: String,
  val includeGitDiffs: Boolean = false,
  val includeLineNumbers: Boolean = false,
  val apiBudget: Double,
)

@State(name = "com.simiacryptus.aicoder.config.AppSettingsState", storages = [Storage("SdkSettingsPlugin.xml")])
data class AppSettingsState(
  var selectedMicLine: String? = null,
  var talkTime: Double = 1.0,
  var memorySeconds: Double = 10.0,
  var lookbackSeconds: Double = 5.0,
  var diffLoggingEnabled: Boolean = false,
  var minRMS: Double = 0.5,
  var minIEC61672: Double = 0.5,
  var minSpectralEntropy: Double = 0.5,
  var minimumTalkSeconds: Double = 1.0,
  var rmsLevel: Int = 0,
  var iec61672Level: Int = 0,
  var spectralEntropyLevel: Int = 0,
  var sampleRate: Int = 44100,
  var sampleSize: Int = 16,
  var channels: Int = 1,
  var temperature: Double = 0.1,
  var reasoningEffort: String = "Low",
  var smartModel: String = OpenAIModels.GPT4o.modelName,
  var fastModel: String = OpenAIModels.GPT4oMini.modelName,
  var analyticsEnabled: Boolean = false,
  var mainImageModel: String = ImageModels.DallE3.modelName,
  var listeningPort: Int = 8081,
  var listeningEndpoint: String = "localhost",
  var humanLanguage: String = "English",
  var apiThreads: Int = 4,
  var modalTasks: Boolean = false,
  var suppressErrors: Boolean = false,
  var apiLog: Boolean = false,
  var devActions: Boolean = false,
  var editRequests: Boolean = false,
  var disableAutoOpenUrls: Boolean = false,
  var storeMetadata: String? = null,
  var transcriptionModel: String? = null,
  var pluginHome: File = run {
    var logPath = System.getProperty("idea.plugins.path")
    if (logPath == null) {
      logPath = System.getProperty("java.io.tmpdir")
    }
    if (logPath == null) {
      logPath = System.getProperty("user.home")
    }
    File(logPath, "AICodingAsst")
  },
  var showWelcomeScreen: Boolean = true,
  var greetedVersion: String = "",
  var shellCommand: String = getDefaultShell(),
  var enableLegacyActions: Boolean = false,
  var githubToken: String? = null,
  var googleApiKey: String? = null,
  var googleSearchEngineId: String? = null,
  var awsProfile: String? = null,
  var awsRegion: String? = null,
  var awsBucket: String? = null,
  var interceptOutput: Boolean = false,
  val apiBase: MutableMap<String, String>? = mapOf("OpenAI" to "https://api.openai.com/v1").toMutableMap(),
  val apiKeys: MutableMap<String, String>? = mapOf("OpenAI" to "").toMutableMap(),
  val userSuppliedModels: MutableList<String>? = mutableListOf(),
  val executables: MutableSet<String>? = mutableSetOf(),
  val savedCommandConfigsJson: MutableMap<String, String>? = mutableMapOf(),
  val savedPlanConfigs: MutableMap<String, String>? = mutableMapOf(),
  val recentCommandsJson: MutableMap<String, String>? = mutableMapOf(),
  val recentArguments: MutableList<String>? = mutableListOf(),
  val recentWorkingDirs: MutableList<String>? = mutableListOf(),
) : PersistentStateComponent<SimpleEnvelope> {
  @JsonIgnore
  var onSettingsLoadedListeners = mutableListOf<() -> Unit>()
  
  @JsonIgnore
  override fun getState(): SimpleEnvelope {
    val value = toJson(this)
    //log.info("Serialize AppSettingsState: ${value.indent("  ")}", RuntimeException("Stack trace"))
    return SimpleEnvelope(value)
  }
  @JsonIgnore
  private fun handleLegacyApiKeys(jsonNode: JsonNode): AppSettingsState {
    val mapper = com.fasterxml.jackson.databind.ObjectMapper()
    val appSettings = fromJson<AppSettingsState>(mapper.writeValueAsString(jsonNode), AppSettingsState::class.java)
    // Check if there's an "apiKey" field but no "apiKeys" field
    if (jsonNode.has("apiKey") && !jsonNode.has("apiKeys")) {
      //log.info("Found legacy 'apiKey' field, migrating to 'apiKeys'")
      val apiKeyNode = jsonNode.get("apiKey")
      if (apiKeyNode.isObject) {
        appSettings.apiKeys?.clear()
        apiKeyNode.fields().forEach { (key, value) -> appSettings.apiKeys?.set(key, value.asText()) }
      }
    }
    return appSettings
  }
  
  
  @JsonIgnore
  fun getRecentCommands(id: String) = recentCommandsJson?.get(id)?.let {
    try {
      fromJson(it, MRUItems::class.java)
    } catch (e: Exception) {
      log.warn("Error loading recent commands: ${it}", e)
      MRUItems()
    }
  } ?: MRUItems()
  
  @JsonIgnore
  fun updateRecentCommands(id: String, mruItems: MRUItems) {
    recentCommandsJson?.set(id, toJson(mruItems))
  }
  
  @JsonIgnore
  override fun loadState(state: SimpleEnvelope) {
    state.value ?: return
    val fromJson = try {
      // Parse to JsonNode first to handle legacy fields
      val mapper = com.fasterxml.jackson.databind.ObjectMapper()
      val jsonNode = mapper.readTree(state.value)
      
      // Handle legacy apiKey field if present
      handleLegacyApiKeys(jsonNode)
      
    } catch (e: Exception) {
      log.warn("Error loading settings: ${state.value}", e)
      AppSettingsState()
    }
    //log.info("Loaded settings: ${fromJson.toJson().indent("  ")} from ${state.value?.indent("  ")}", RuntimeException("Stack trace"))
    XmlSerializerUtil.copyBean(fromJson, this)
    /* Copy userSuppliedModels */
    userSuppliedModels?.clear()
    fromJson.userSuppliedModels?.map { fromJson<UserSuppliedModel>(it, UserSuppliedModel::class.java) }?.forEach { model ->
      userSuppliedModels?.add(toJson(model))
    }
    /* Copy executables */
    executables?.clear()
    fromJson.executables?.forEach { executable ->
      executables?.add(executable)
    }
    /* Copy savedCommandConfigsJson */
    savedCommandConfigsJson?.clear()
    fromJson.savedCommandConfigsJson?.forEach { (key, value) ->
      savedCommandConfigsJson?.set(key, value)
    }
    /* Copy savedPlanConfigs */
    savedPlanConfigs?.clear()
    fromJson.savedPlanConfigs?.forEach { (key, value) ->
      savedPlanConfigs?.set(key, value)
    }
    /* Copy recentCommandsJson */
    recentCommandsJson?.clear()
    fromJson.recentCommandsJson?.forEach { (key, value) ->
      recentCommandsJson?.set(key, value)
    }
    /* Copy recentArguments */
    recentArguments?.clear()
    fromJson.recentArguments?.forEach { argument ->
      recentArguments?.add(argument)
    }
    /* Copy recentWorkingDirs */
    recentWorkingDirs?.clear()
    fromJson.recentWorkingDirs?.forEach { workingDir ->
      recentWorkingDirs?.add(workingDir)
    }
     /* Copy apiBase */
    apiBase?.clear()
    fromJson.apiBase?.forEach { (key, value) ->
      apiBase?.set(key, value)
    }
    /* Copy apiKeys */
    apiKeys?.clear()
    fromJson.apiKeys?.forEach { (key, value) ->
      apiKeys?.set(key, value)
    }
    notifySettingsLoaded()
  }
  
  private fun notifySettingsLoaded() {
    onSettingsLoadedListeners.forEach { it() }
  }
  
  override fun equals(other: Any?): Boolean {
    if (this === other) return true
    if (javaClass != other?.javaClass) return false
    other as AppSettingsState
    if (minRMS != other.minRMS) return false
    if (minIEC61672 != other.minIEC61672) return false
    if (minSpectralEntropy != other.minSpectralEntropy) return false
    if (rmsLevel != other.rmsLevel) return false
    if (iec61672Level != other.iec61672Level) return false
    if (spectralEntropyLevel != other.spectralEntropyLevel) return false
    if (sampleRate != other.sampleRate) return false
    if (sampleSize != other.sampleSize) return false
    if (channels != other.channels) return false
    if (temperature != other.temperature) return false
    if (smartModel != other.smartModel) return false
    if (fastModel != other.fastModel) return false
    if (mainImageModel != other.mainImageModel) return false
    if (listeningPort != other.listeningPort) return false
    if (listeningEndpoint != other.listeningEndpoint) return false
    if (humanLanguage != other.humanLanguage) return false
    if (apiThreads != other.apiThreads) return false
    if (apiBase != other.apiBase) return false
    if (apiKeys != other.apiKeys) return false
    if (modalTasks != other.modalTasks) return false
    if (suppressErrors != other.suppressErrors) return false
    if (apiLog != other.apiLog) return false
    if (devActions != other.devActions) return false
    if (editRequests != other.editRequests) return false
    if (storeMetadata != other.storeMetadata) return false
    if (FileUtil.filesEqual(pluginHome, other.pluginHome)) return false
    if (recentCommandsJson != other.recentCommandsJson) return false
    if (showWelcomeScreen != other.showWelcomeScreen) return false
    if (greetedVersion != other.greetedVersion) return false
    if (mainImageModel != other.mainImageModel) return false
    if (enableLegacyActions != other.enableLegacyActions) return false
    if (executables != other.executables) return false
    //userSuppliedModels
    if (userSuppliedModels != other.userSuppliedModels) return false
    if (googleApiKey != other.googleApiKey) return false
    if (googleSearchEngineId != other.googleSearchEngineId) return false
    if (githubToken != other.githubToken) return false
    if (awsProfile != other.awsProfile) return false
    if (awsRegion != other.awsRegion) return false
    if (awsBucket != other.awsBucket) return false
    if (selectedMicLine != other.selectedMicLine) return false
    if (reasoningEffort != other.reasoningEffort) return false
    return true
  }
  
  override fun hashCode(): Int {
    var result = temperature.hashCode()
    result = 31 * result + minRMS.hashCode()
    result = 31 * result + minIEC61672.hashCode()
    result = 31 * result + minSpectralEntropy.hashCode()
    result = 31 * result + rmsLevel
    result = 31 * result + iec61672Level
    result = 31 * result + spectralEntropyLevel
    result = 31 * result + sampleRate
    result = 31 * result + sampleSize
    result = 31 * result + channels
    result = 31 * result + smartModel.hashCode()
    result = 31 * result + fastModel.hashCode()
    result = 31 * result + enableLegacyActions.hashCode()
    result = 31 * result + mainImageModel.hashCode()
    result = 31 * result + listeningPort
    result = 31 * result + listeningEndpoint.hashCode()
    result = 31 * result + humanLanguage.hashCode()
    result = 31 * result + apiThreads
    result = 31 * result + (apiBase?.hashCode() ?: 0)
    result = 31 * result + (apiKeys?.hashCode() ?: 0)
    result = 31 * result + modalTasks.hashCode()
    result = 31 * result + suppressErrors.hashCode()
    result = 31 * result + apiLog.hashCode()
    result = 31 * result + devActions.hashCode()
    result = 31 * result + editRequests.hashCode()
    result = 31 * result + (storeMetadata?.hashCode() ?: 0)
    result = 31 * result + FileUtil.fileHashCode(pluginHome)
    result = 31 * result + recentCommandsJson.hashCode()
    result = 31 * result + showWelcomeScreen.hashCode()
    result = 31 * result + greetedVersion.hashCode()
    result = 31 * result + mainImageModel.hashCode()
    result = 31 * result + enableLegacyActions.hashCode()
    result = 31 * result + executables.hashCode()
    result = 31 * result + userSuppliedModels.hashCode()
    result = 31 * result + (googleApiKey?.hashCode() ?: 0)
    result = 31 * result + (googleSearchEngineId?.hashCode() ?: 0)
    result = 31 * result + (githubToken?.hashCode() ?: 0)
    result = 31 * result + (awsProfile?.hashCode() ?: 0)
    result = 31 * result + (awsRegion?.hashCode() ?: 0)
    result = 31 * result + (awsBucket?.hashCode() ?: 0)
    result = 31 * result + (selectedMicLine?.hashCode() ?: 0)
    result = 31 * result + reasoningEffort.hashCode()
    return result
  }
  
  companion object {
    val log = LoggerFactory.getLogger(AppSettingsState::class.java)
    var auxiliaryLog: File? = null
    const val WELCOME_VERSION: String = "1.5.0"
    
    @JvmStatic
    val instance: AppSettingsState by lazy {
      ApplicationManager.getApplication()?.getService(AppSettingsState::class.java) ?: AppSettingsState()
    }
    
    fun String.imageModel(): ImageModels {
      return ImageModels.values().firstOrNull {
        it.modelName == this || it.name == this
      } ?: ImageModels.DallE3
    }
    
    fun getDefaultShell() = if (System.getProperty("os.name").lowercase().contains("win")) "powershell" else "bash"
  }
  
  data class UserSuppliedModel(
    var displayName: String = "",
    var modelId: String = "",
    var provider: APIProvider = APIProvider.OpenAI
  )
  
  data class SavedPlanConfig(
    val name: String,
    val temperature: Double,
    val autoFix: Boolean,
    val apiBudget: Double? = 10.0,
    val taskSettings: Map<String, TaskSettingsBase>
  )
  
}