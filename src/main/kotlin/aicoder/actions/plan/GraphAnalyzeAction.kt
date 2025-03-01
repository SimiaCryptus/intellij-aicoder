package aicoder.actions.plan

import aicoder.actions.BaseAction
import aicoder.actions.SessionProxyServer
import aicoder.actions.agent.toFile
import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnActionEvent
import com.simiacryptus.aicoder.AppServer
import com.simiacryptus.aicoder.config.AppSettingsState
import com.simiacryptus.aicoder.util.BrowseUtil.browse
import com.simiacryptus.aicoder.util.UITools
import com.simiacryptus.jopenai.describe.AbbrevWhitelistYamlDescriber
import com.simiacryptus.jopenai.describe.TypeDescriber
import com.simiacryptus.jopenai.models.chatModel
import com.simiacryptus.skyenet.TabbedDisplay
import com.simiacryptus.skyenet.apps.graph.SoftwareNodeType
import com.simiacryptus.skyenet.core.actors.ParsedActor
import com.simiacryptus.skyenet.core.platform.Session
import com.simiacryptus.skyenet.core.platform.file.DataStorage
import com.simiacryptus.skyenet.core.platform.model.User
import com.simiacryptus.skyenet.core.util.FileValidationUtils
import com.simiacryptus.skyenet.util.MarkdownUtil.renderMarkdown
import com.simiacryptus.skyenet.webui.application.AppInfoData
import com.simiacryptus.skyenet.webui.application.ApplicationServer
import com.simiacryptus.skyenet.webui.application.ApplicationSocketManager
import com.simiacryptus.skyenet.webui.session.SocketManager
import com.simiacryptus.skyenet.webui.session.getChildClient
import com.simiacryptus.util.JsonUtil
import org.slf4j.LoggerFactory
import java.io.File

/**
 * GraphAnalyzeAction analyzes a set of code and documentation files and produces a detailed software graph.
 */
class GraphAnalyzeAction : BaseAction() {

    override fun getActionUpdateThread() = ActionUpdateThread.BGT

    override fun isEnabled(event: AnActionEvent): Boolean {
        return UITools.getSelectedFiles(event).any { it.isValid }
    }

    override fun handle(e: AnActionEvent) {
        val selectedFiles = UITools.getSelectedFiles(e).toList().flatMap {
            FileValidationUtils.expandFileList(it.toFile).toList()
        }
        if (selectedFiles.isEmpty()) return
        // Use requireNotNull for clearer error handling
        val workingDirectory = requireNotNull(selectedFiles.first().parentFile?.path) {
            "No working directory found for selected files"
        }
        val session = Session.newGlobalID()
        val root = File(workingDirectory)
        DataStorage.sessionPaths[session] = root
        // Extract constants for better maintainability
        val APPLICATION_NAME = "Graph Analysis"
        val OUTPUT_FILENAME = "software_graph.json"

        SessionProxyServer.chats[session] = object : ApplicationServer(
            applicationName = APPLICATION_NAME,
            path = "/graphAnalyze",
            showMenubar = false
        ) {
            override val singleInput = true
            override val stickyInput = false

            override fun newSession(user: User?, session: Session): SocketManager {
                val socketManager = super.newSession(user, session)
                val describer: TypeDescriber = object : AbbrevWhitelistYamlDescriber(
                    "com.simiacryptus", "aicoder.actions"
                ) {
                    override val includeMethods: Boolean get() = false
                }
                val ui = (socketManager as ApplicationSocketManager).applicationInterface
                val task = ui.newTask()
                val api = api.getChildClient(task)
                val tabs = TabbedDisplay(task)
                Thread {
                    try {
                        var accumulatedGraph: SoftwareNodeType.SoftwareGraph? = null
                        selectedFiles.filter {
                            !it.isDirectory
                        }.map { file ->
                            val task = ui.newTask(false).apply { tabs[file.name] = placeholder }
                            try {
                                val softwareGraph = ParsedActor(
                                    resultClass = SoftwareNodeType.SoftwareGraph::class.java,
                                    model = AppSettingsState.instance.smartModel.chatModel(),
                                  parsingModel = AppSettingsState.instance.fastModel.chatModel(),
                                ).answer(
                                    listOf(
                                        /* Directive */
                                        """
                                        You are a software analysis assistant. Analyze the following file content and produce a detailed software graph in JSON format.
                                        The graph should include nodes representing modules, classes, functions, dependencies, and documentation references.
                                    """.trimIndent(),
                                        /* Graph Schema */
                                        "The graph should accurately represent the software architecture including:\n\nAvailable Node Types:\n" +
                                                SoftwareNodeType.values().joinToString("\n") {
                                                    "* ${it.name}: ${
                                                        it.description?.replace(
                                                            "\n",
                                                            "\n  "
                                                        )
                                                    }\n    ${
                                                        describer.describe(rawType = it.nodeClass)
                                                            .prependIndent("  ")
                                                    }"
                                                },
                                        /* Accumulated Graph */
                                        when {
                                            accumulatedGraph != null -> "\n\nHere is the current accumulated graph that you should extend:\n```json\n${
                                                JsonUtil.toJson(
                                                    accumulatedGraph ?: ""
                                                )
                                            }\n```"

                                            else -> "\n\nNo accumulated graph found."
                                        },
                                        /* File Listing */
                                        "### File Listing\n\n${selectedFiles.joinToString("\n") { "* $it\n" }}",
                                        /* File Content */
                                        file.readText(),
                                    ),
                                    api = api
                                ).obj
                                task.complete(
                                    renderMarkdown(
                                        "# Detailed Software Graph\n\n ```json\n${
                                            JsonUtil.toJson(
                                                softwareGraph
                                            )
                                        }\n```"
                                    )
                                )
                                accumulatedGraph =
                                    if (accumulatedGraph == null) softwareGraph else accumulatedGraph!! + softwareGraph
                            } catch (e: Throwable) {
                                task.error(ui, e)
                                log.warn("Error processing file ${file.name}", e)
                            }
                        }
                        accumulatedGraph?.let { JsonUtil.toJson(it) }
                            ?.let {
                                File(root, OUTPUT_FILENAME).writeText(it)
                                task.complete(renderMarkdown("# Final Software Graph\n\n```json\n$it\n```"))
                            }
                    } catch (e: Throwable) {
                        task.error(ui, e)
                        log.warn("Error in graph analysis", e)
                    }
                }.start()
                appInfoMap[session] = AppInfoData(
                    applicationName = APPLICATION_NAME,
                    singleInput = true,
                    stickyInput = false,
                    loadImages = false,
                    showMenubar = false
                )
                return socketManager
            }
        }
        val server = AppServer.getServer(e.project)
        Thread {
            Thread.sleep(500)
            try {
                val uri = server.server.uri.resolve("/#$session")
                log.info("Opening browser to $uri")
                browse(uri)
            } catch (e: Throwable) {
                log.warn("Error opening browser", e)
            }
        }.start()
    }

    companion object {
        private val log = LoggerFactory.getLogger(GraphAnalyzeAction::class.java)
    }
}