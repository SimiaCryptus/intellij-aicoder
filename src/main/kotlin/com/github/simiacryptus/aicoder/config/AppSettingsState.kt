package com.github.simiacryptus.aicoder.config

/**
 * Stores and manages plugin configuration settings.
 *
 * This class is responsible for persisting and retrieving the plugin's
 * configuration settings. It uses the IntelliJ Platform's persistence
 * framework to save settings across IDE restarts.
 */
import com.fasterxml.jackson.annotation.JsonIgnore
import com.github.simiacryptus.aicoder.util.PluginStartupActivity.Companion.addUserSuppliedModels
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.components.PersistentStateComponent
import com.intellij.openapi.components.State
import com.intellij.openapi.components.Storage
import com.intellij.openapi.util.io.FileUtil
import com.intellij.util.xmlb.XmlSerializerUtil
import com.simiacryptus.jopenai.models.APIProvider
import com.simiacryptus.jopenai.models.ImageModels
import com.simiacryptus.jopenai.models.OpenAIModels
import com.simiacryptus.skyenet.core.platform.AwsPlatform
import com.simiacryptus.util.JsonUtil
import org.slf4j.LoggerFactory
import java.io.File

@State(name = "org.intellij.sdk.settings.AppSettingsState", storages = [Storage("SdkSettingsPlugin.xml")])
data class AppSettingsState(
    var temperature: Double = 0.1,
    var smartModel: String = OpenAIModels.GPT4o.modelName,
    var fastModel: String = OpenAIModels.GPT4oMini.modelName,
    var mainImageModel: String = ImageModels.DallE3.modelName,
    var listeningPort: Int = 8081,
    var listeningEndpoint: String = "localhost",
    var humanLanguage: String = "English",
    var apiThreads: Int = 4,
    var apiBase: Map<String, String>? = mapOf("OpenAI" to "https://api.openai.com/v1"),
    var apiKey: Map<String, String>? = mapOf("OpenAI" to ""),
    var modalTasks: Boolean = false,
    var suppressErrors: Boolean = false,
    var apiLog: Boolean = false,
    var devActions: Boolean = false,
    var editRequests: Boolean = false,
    var disableAutoOpenUrls: Boolean = false,
    var storeMetadata: String? = null,
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
    var executables: MutableSet<String> = mutableSetOf(),
    var recentArguments: MutableList<String> = mutableListOf(),
    val recentCommands: MutableMap<String, MRUItems> = mutableMapOf<String, MRUItems>(),
    var userSuppliedModels: MutableList<UserSuppliedModel> = mutableListOf(),
    var githubToken: String? = null,
    var googleApiKey: String? = null,
    var googleSearchEngineId: String? = null,
    var awsProfile: String? = null,
    var awsRegion: String? = null,
    var awsBucket: String? = null
) : PersistentStateComponent<SimpleEnvelope> {
    private var onSettingsLoadedListeners = mutableListOf<() -> Unit>()

    @JsonIgnore
    override fun getState(): SimpleEnvelope {
        val value = JsonUtil.toJson(this)
        return SimpleEnvelope(value)
    }

    fun getRecentCommands(id: String) = recentCommands.computeIfAbsent(id) { MRUItems() }

    override fun loadState(state: SimpleEnvelope) {
        state.value ?: return
        val fromJson = try {
            JsonUtil.fromJson(state.value!!, AppSettingsState::class.java)
        } catch (e: Exception) {
            log.warn("Error loading settings: ${state.value}", e)
            AppSettingsState()
        }
        XmlSerializerUtil.copyBean(fromJson, this)
        addUserSuppliedModels(fromJson.userSuppliedModels)
        recentCommands.clear()
        recentCommands.putAll(fromJson.recentCommands)
        notifySettingsLoaded()
    }

    fun addOnSettingsLoadedListener(listener: () -> Unit) {
        onSettingsLoadedListeners.add(listener)
    }

    private fun notifySettingsLoaded() {
        onSettingsLoadedListeners.forEach { it() }
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false
        other as AppSettingsState
        if (temperature != other.temperature) return false
        if (smartModel != other.smartModel) return false
        if (fastModel != other.fastModel) return false
        if (mainImageModel != other.mainImageModel) return false
        if (listeningPort != other.listeningPort) return false
        if (listeningEndpoint != other.listeningEndpoint) return false
        if (humanLanguage != other.humanLanguage) return false
        if (apiThreads != other.apiThreads) return false
        if (apiBase != other.apiBase) return false
        if (apiKey != other.apiKey) return false
        if (modalTasks != other.modalTasks) return false
        if (suppressErrors != other.suppressErrors) return false
        if (apiLog != other.apiLog) return false
        if (devActions != other.devActions) return false
        if (editRequests != other.editRequests) return false
        if (storeMetadata != other.storeMetadata) return false
        if (FileUtil.filesEqual(pluginHome, other.pluginHome)) return false
        if (recentCommands != other.recentCommands) return false
        if (showWelcomeScreen != other.showWelcomeScreen) return false
        if (greetedVersion != other.greetedVersion) return false
        if (mainImageModel != other.mainImageModel) return false
        if (enableLegacyActions != other.enableLegacyActions) return false
        if (executables != other.executables) return false
        //userSuppliedModels
        if (userSuppliedModels.toTypedArray().contentDeepEquals(other.userSuppliedModels.toTypedArray()).not()) return false
        if (googleApiKey != other.googleApiKey) return false
        if (googleSearchEngineId != other.googleSearchEngineId) return false
        if (githubToken != other.githubToken) return false
        if (awsProfile != other.awsProfile) return false
        if (awsRegion != other.awsRegion) return false
        if (awsBucket != other.awsBucket) return false
        return true
    }

    override fun hashCode(): Int {
        var result = temperature.hashCode()
        result = 31 * result + smartModel.hashCode()
        result = 31 * result + fastModel.hashCode()
        result = 31 * result + enableLegacyActions.hashCode()
        result = 31 * result + mainImageModel.hashCode()
        result = 31 * result + listeningPort
        result = 31 * result + listeningEndpoint.hashCode()
        result = 31 * result + humanLanguage.hashCode()
        result = 31 * result + apiThreads
        result = 31 * result + (apiBase?.hashCode() ?: 0)
        result = 31 * result + (apiKey?.hashCode() ?: 0)
        result = 31 * result + modalTasks.hashCode()
        result = 31 * result + suppressErrors.hashCode()
        result = 31 * result + apiLog.hashCode()
        result = 31 * result + devActions.hashCode()
        result = 31 * result + editRequests.hashCode()
        result = 31 * result + (storeMetadata?.hashCode() ?: 0)
        result = 31 * result + FileUtil.fileHashCode(pluginHome)
        result = 31 * result + recentCommands.hashCode()
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
    var analyticsEnabled: Boolean = false
}