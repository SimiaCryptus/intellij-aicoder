package aicoder.actions.chat

import aicoder.actions.BaseAction
import aicoder.actions.SessionProxyServer
import aicoder.actions.agent.MultiStepPatchAction.AutoDevApp.Settings
import aicoder.actions.agent.toFile
import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.PlatformDataKeys
import com.simiacryptus.aicoder.AppServer
import com.simiacryptus.aicoder.config.AppSettingsState
import com.simiacryptus.aicoder.util.BrowseUtil.browse
import com.simiacryptus.aicoder.util.UITools
import com.simiacryptus.diff.AddApplyFileDiffLinks
import com.simiacryptus.jopenai.API
import com.simiacryptus.jopenai.ChatClient
import com.simiacryptus.jopenai.describe.Description
import com.simiacryptus.jopenai.models.ApiModel
import com.simiacryptus.jopenai.models.ApiModel.Role
import com.simiacryptus.jopenai.models.chatModel
import com.simiacryptus.jopenai.proxy.ValidatedObject
import com.simiacryptus.jopenai.util.ClientUtil.toContentList
import com.simiacryptus.skyenet.Discussable
import com.simiacryptus.skyenet.core.actors.ParsedActor
import com.simiacryptus.skyenet.core.actors.SimpleActor
import com.simiacryptus.skyenet.core.platform.Session
import com.simiacryptus.skyenet.core.platform.model.User
import com.simiacryptus.skyenet.core.util.FileSelectionUtils
import com.simiacryptus.skyenet.core.util.IterativePatchUtil.patchFormatPrompt
import com.simiacryptus.skyenet.core.util.getModuleRootForFile
import com.simiacryptus.skyenet.util.MarkdownUtil.renderMarkdown
import com.simiacryptus.skyenet.webui.application.AppInfoData
import com.simiacryptus.skyenet.webui.application.ApplicationInterface
import com.simiacryptus.skyenet.webui.application.ApplicationServer
import com.simiacryptus.skyenet.webui.session.getChildClient
import org.slf4j.LoggerFactory
import java.io.File
import java.nio.file.Path
import java.text.SimpleDateFormat
import java.util.concurrent.Semaphore
import java.util.concurrent.atomic.AtomicReference
import kotlin.io.path.relativeTo

class CodeChangeAction : BaseAction() {
    override fun getActionUpdateThread() = ActionUpdateThread.BGT
    
    override fun isEnabled(event: AnActionEvent): Boolean {
        if (FileSelectionUtils.expandFileList(
            *PlatformDataKeys.VIRTUAL_FILE_ARRAY.getData(event.dataContext)?.map { it.toFile }?.toTypedArray<File>() ?: arrayOf()
        ).isEmpty()) return false
        return super.isEnabled(event)
    }

    data class FileAnalysis(
        @Description("List of files that are likely to need modifications")
        val filesToModify: List<String>? = null,
        @Description("List of files that should be referenced for context")
        val contextFiles: List<String>? = null,
        @Description("Brief explanation of why these files were selected")
        val explanation: String? = null
    ) : ValidatedObject {
        override fun validate(): String? = when {
            filesToModify.isNullOrEmpty() -> "Files to modify are required"
            filesToModify.any { it.isBlank() } -> "Invalid file path"
            else -> null
        }
    }

    override fun handle(event: AnActionEvent) {
        try {
            val root = getRoot(event) ?: throw RuntimeException("No file or folder selected")
            val virtualFiles = PlatformDataKeys.VIRTUAL_FILE_ARRAY.getData(event.dataContext)
            val initialFiles = FileSelectionUtils.expandFileList(*virtualFiles?.map { it.toFile }?.toTypedArray() ?: arrayOf()).map {
                it.toPath().relativeTo(root)
            }.toSet()
            
            val session = Session.newGlobalID()
            SessionProxyServer.metadataStorage.setSessionName(
                null,
                session,
                "${javaClass.simpleName} @ ${SimpleDateFormat("HH:mm:ss").format(System.currentTimeMillis())}"
            )
            SessionProxyServer.chats[session] = PatchApp(root.toFile(), initialFiles)
            ApplicationServer.appInfoMap[session] = AppInfoData(
                applicationName = "Code Change",
                singleInput = true,
                stickyInput = false,
                loadImages = false,
                showMenubar = false
            )
            val server = AppServer.getServer(event.project)
            launchBrowser(server, session.toString())
        } catch (e: Exception) {
            log.error("Error in CodeChangeAction", e)
            UITools.showErrorDialog(e.message ?: "", "Error")
        }
    }

    private fun getRoot(event: AnActionEvent): Path? {
        val folder = UITools.getSelectedFolder(event)
        return if (null != folder) {
            folder.toFile.toPath()
        } else {
            getModuleRootForFile(UITools.getSelectedFile(event)?.parent?.toFile ?: return null).toPath()
        }
    }

    private fun launchBrowser(server: AppServer, session: String) {
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

    inner class PatchApp(
        override val root: File,
        private val initialFiles: Set<Path>,
    ) : ApplicationServer(
        applicationName = "Code Change",
        path = "/codeChange",
        showMenubar = false,
    ) {
        private val log = LoggerFactory.getLogger(PatchApp::class.java)

        override val singleInput = false
        override val stickyInput = true

        private fun getCodeFiles(): Set<Path> {
            if (!root.exists()) {
                log.warn("Root directory does not exist: $root")
                return emptySet()
            }
            return initialFiles.filter { path ->
                val file = root.toPath().resolve(path).toFile()
                val exists = file.exists()
                if (!exists) log.warn("File does not exist: $file")
                exists
            }.toSet()
        }

        private fun codeSummary(paths: Set<Path>): String {
            return paths.associateWith { root.toPath().resolve(it).toFile().readText(Charsets.UTF_8) }
                .entries.joinToString("\n\n") { (path, code) ->
                    val extension = path.toString().split('.').lastOrNull()
                    "# $path\n```$extension\n$code\n```"
                }
        }

        override fun userMessage(
            session: Session,
            user: User?,
            userMessage: String,
            ui: ApplicationInterface,
            api: API
        ) {
            try {
                val settings = getSettings(session, user) ?: Settings()
                if (api is ChatClient) api.budget = settings.budget ?: 2.00

                val task = ui.newTask()
                task.add("Analyzing files...")

                val api = (api as ChatClient).getChildClient(task)
                
                // First stage: Analyze files with fast model
                val fileAnalyzer = ParsedActor(
                    resultClass = FileAnalysis::class.java,
                    prompt = """
                        You are a helpful AI that analyzes code files to determine which ones need to be modified.
                        Given the code and a modification request, identify:
                        1) Files that will likely need to be modified
                        2) Related files that provide important context
                        Be selective and only include files that are directly relevant.
                    """.trimIndent(),
                    model = AppSettingsState.instance.fastModel.chatModel(),
                    parsingModel = AppSettingsState.instance.fastModel.chatModel(),
                )

                val allFiles = getCodeFiles()
                val initialAnalysis = fileAnalyzer.answer(
                    listOf(codeSummary(allFiles), userMessage),
                    api = api
                )

                task.add("Identified relevant files: ${initialAnalysis.obj.filesToModify?.joinToString(", ")}")

                // Second stage: Process selected files with smart model
                val relevantPaths = ((initialAnalysis.obj.filesToModify ?: emptyList()) +
                    (initialAnalysis.obj.contextFiles ?: emptyList())).mapNotNull { filePath ->
                    allFiles.find { it.toString().endsWith(filePath) }
                }.toSet()

                fun mainActor(): SimpleActor {
                    return SimpleActor(
                        prompt = """
                            You are a helpful AI that helps people with coding.
                            
                            You will be answering questions about the following code:
                            
                        """.trimIndent() + codeSummary(relevantPaths) + patchFormatPrompt,
                        model = AppSettingsState.instance.smartModel.chatModel()
                    )
                }

                val toInput = { it: String -> listOf(codeSummary(relevantPaths), it) }
                Discussable(
                    task = task,
                    userMessage = { userMessage },
                    heading = renderMarkdown(userMessage),
                    initialResponse = { it: String -> mainActor().answer(toInput(it), api = api) },
                    outputFn = { design: String ->
                        """<div>${
                            renderMarkdown(design) {
                                AddApplyFileDiffLinks.instrumentFileDiffs(
                                    ui.socketManager!!,
                                    root = root.toPath(),
                                    response = it,
                                    handle = { newCodeMap ->
                                        newCodeMap.forEach { (path, newCode) ->
                                            task.complete("<a href='${"fileIndex/$session/$path"}'>$path</a> Updated")
                                        }
                                    },
                                    ui = ui,
                                    api = api,
                                )
                            }
                        }</div>"""
                    },
                    ui = ui,
                    reviseResponse = { userMessages: List<Pair<String, Role>> ->
                        mainActor().respond(
                            messages = userMessages.map { ApiModel.ChatMessage(it.second, it.first.toContentList()) }
                                .toTypedArray(),
                            input = toInput(userMessage),
                            api = api
                        )
                    },
                    atomicRef = AtomicReference(),
                    semaphore = Semaphore(0),
                ).call()
            } catch (e: Exception) {
                log.error("Error processing user message", e)
                ui.newTask().error(ui, e)
            }
        }
    }

    companion object {
        private val log = LoggerFactory.getLogger(CodeChangeAction::class.java)
    }
}