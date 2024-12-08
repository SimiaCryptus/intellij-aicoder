package com.github.simiacryptus.aicoder.actions.knowledge

import com.github.simiacryptus.aicoder.actions.FileContextAction
import com.github.simiacryptus.aicoder.config.AppSettingsState
import com.github.simiacryptus.aicoder.config.Name
import com.github.simiacryptus.aicoder.util.UITools
import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.project.Project
import com.simiacryptus.util.JsonUtil
import org.apache.commons.io.FileUtils
import java.io.File
import java.net.URI
import java.net.URLEncoder
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import javax.swing.JTextField

class GoogleSearchAndDownloadAction : FileContextAction<GoogleSearchAndDownloadAction.Settings>() {
    private val log = Logger.getInstance(GoogleSearchAndDownloadAction::class.java)

    override fun getActionUpdateThread() = ActionUpdateThread.BGT

    class SettingsUI {
        @Name("Search Query")
        var searchQuery: JTextField = JTextField("", 40)
    }

    class UserSettings(
        var searchQuery: String = ""
    )

    class Settings(
        val settings: UserSettings? = null,
        val project: Project? = null
    )

    override fun isEnabled(event: AnActionEvent): Boolean {
        return super.isEnabled(event)
            && !AppSettingsState.instance.googleApiKey.isNullOrBlank()
            && !AppSettingsState.instance.googleSearchEngineId.isNullOrBlank()
            && AppSettingsState.instance.devActions
    }

    override fun getConfig(project: Project?, e: AnActionEvent): Settings {
        return Settings(
            UITools.showDialog(
                project,
                SettingsUI::class.java,
                UserSettings::class.java,
                "Google Search and Download"
            ),
            project
        )
    }

    override fun processSelection(state: SelectionState, config: Settings?, progress: ProgressIndicator): Array<File> {
        val searchQuery = config?.settings?.searchQuery ?: return emptyArray()
        progress.text = "Performing Google search..."
        progress.isIndeterminate = true
        try {
            val searchResults = performGoogleSearch(searchQuery)
            progress.text = "Downloading search results..."
            return downloadResults(searchResults, state.selectedFile, progress)
        } catch (e: Exception) {
            throw RuntimeException("Failed to process search request: ${e.message}", e)
        }
    }

    private fun performGoogleSearch(query: String): List<Map<String, Any>> {
        val client = HttpClient.newBuilder().build()
        val encodedQuery = URLEncoder.encode(query, "UTF-8")
        val uriBuilder = "https://www.googleapis.com/customsearch/v1?key=${AppSettingsState.instance.googleApiKey}&cx=${AppSettingsState.instance.googleSearchEngineId}&q=$encodedQuery&num=10"
        val request = HttpRequest.newBuilder().uri(URI.create(uriBuilder)).GET().build()
        val response = client.send(request, HttpResponse.BodyHandlers.ofString())
        if (response.statusCode() != 200) {
            log.error("Google API request failed: ${response.body()}")
            throw RuntimeException("Google API request failed with status ${response.statusCode()}")
        }
        val searchResults: Map<String, Any> = JsonUtil.fromJson(response.body(), Map::class.java)
        return (searchResults["items"] as List<Map<String, Any>>?) ?: emptyList()
    }

    private fun downloadResults(results: List<Map<String, Any>>, targetDir: File, progress: ProgressIndicator): Array<File> {
        val client = HttpClient.newBuilder().build()
        progress.isIndeterminate = false
        return results.mapIndexedNotNull { index, item ->
            if (progress.isCanceled) {
                throw InterruptedException("Operation cancelled by user")
            }
            progress.fraction = index.toDouble() / results.size
            val url = item["link"] as String
            val title = item["title"] as String
            progress.text = "Downloading ${index + 1}/${results.size}: $title"
            val fileName = "${index + 1}_${sanitizeFileName(title)}.html"
            val targetFile = File(targetDir, fileName)
            
            try {
                val request = HttpRequest.newBuilder().uri(URI.create(url)).GET().build()
                val response = client.send(request, HttpResponse.BodyHandlers.ofString())
                if (response.statusCode() == 200) {
                    FileUtils.writeStringToFile(targetFile, response.body(), "UTF-8")
                    targetFile
                } else {
                    log.warn("Failed to download $url: ${response.statusCode()}")
                    null
                }
            } catch (e: Exception) {
                log.warn("Failed to download $url", e)
                null
            }
        }.toTypedArray()
    }

    private fun sanitizeFileName(fileName: String): String {
        return fileName.replace(Regex("[^a-zA-Z0-9.-]"), "_").take(50)
    }
}