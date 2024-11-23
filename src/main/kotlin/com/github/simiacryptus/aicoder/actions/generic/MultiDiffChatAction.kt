package com.github.simiacryptus.aicoder.actions.generic

import com.github.simiacryptus.aicoder.AppServer
import com.github.simiacryptus.aicoder.actions.BaseAction
import com.github.simiacryptus.aicoder.actions.generic.MultiStepPatchAction.AutoDevApp.Settings
import com.github.simiacryptus.aicoder.config.AppSettingsState
import com.github.simiacryptus.aicoder.util.BrowseUtil.browse
import com.github.simiacryptus.aicoder.util.UITools
import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.PlatformDataKeys
import com.intellij.openapi.vfs.VirtualFile
import com.simiacryptus.diff.addApplyFileDiffLinks
import com.simiacryptus.jopenai.API
import com.simiacryptus.jopenai.ChatClient
import com.simiacryptus.jopenai.models.ApiModel
import com.simiacryptus.jopenai.models.ApiModel.Role
import com.simiacryptus.jopenai.models.chatModel
import com.simiacryptus.jopenai.util.ClientUtil.toContentList
import com.simiacryptus.jopenai.util.GPT4Tokenizer
import com.simiacryptus.skyenet.Discussable
import com.simiacryptus.skyenet.core.actors.SimpleActor
import com.simiacryptus.skyenet.core.platform.Session
import com.simiacryptus.skyenet.core.platform.model.User
import com.simiacryptus.skyenet.core.util.getModuleRootForFile
import com.simiacryptus.skyenet.util.MarkdownUtil.renderMarkdown
import com.simiacryptus.skyenet.webui.application.AppInfoData
import com.simiacryptus.skyenet.webui.application.ApplicationInterface
import com.simiacryptus.skyenet.webui.application.ApplicationServer
import org.slf4j.LoggerFactory
import java.io.File
import java.nio.file.Path
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.Semaphore
import java.util.concurrent.atomic.AtomicReference

class MultiDiffChatAction : BaseAction() {
    override fun getActionUpdateThread() = ActionUpdateThread.BGT
    override fun isEnabled(event: AnActionEvent): Boolean {
        if (!super.isEnabled(event)) return false
        val virtualFiles = PlatformDataKeys.VIRTUAL_FILE_ARRAY.getData(event.dataContext)
        return virtualFiles != null && virtualFiles.isNotEmpty()
    }

    override fun handle(event: AnActionEvent) {
        try {
            val root: Path

            val dataContext = event.dataContext
            val virtualFiles = PlatformDataKeys.VIRTUAL_FILE_ARRAY.getData(dataContext)
            val folder = UITools.getSelectedFolder(event)
            root = if (null != folder) {
                folder.toFile.toPath()
            } else {
                getModuleRootForFile(
                    UITools.getSelectedFile(event)?.parent?.toFile
                        ?: throw RuntimeException("No file or folder selected")
                ).toPath()
            }
            val initialFiles = getFiles(virtualFiles, root)
            val session = Session.newGlobalID()
            SessionProxyServer.metadataStorage.setSessionName(null, session, "${javaClass.simpleName} @ ${SimpleDateFormat("HH:mm:ss").format(System.currentTimeMillis())}")
            SessionProxyServer.chats[session] = PatchApp(root.toFile(), initialFiles)
            ApplicationServer.appInfoMap[session] = AppInfoData(
                applicationName = "Code Chat",
                singleInput = true,
                stickyInput = false,
                loadImages = false,
                showMenubar = false
            )
            val server = AppServer.getServer(event.project)
            UITools.runAsync(event.project, "Opening Browser", true) { progress ->
                Thread.sleep(500)
                try {
                    val uri = server.server.uri.resolve("/#$session")
                    BaseAction.log.info("Opening browser to $uri")
                    browse(uri)
                } catch (e: Throwable) {
                    val message = "Failed to open browser: ${e.message}"
                    log.error(message, e)
                    UITools.showErrorDialog(event.project, message, "Error")
                }
            }
        } catch (e: Exception) {
            // Comprehensive error logging
            log.error("Error in MultiDiffChatAction", e)
            UITools.showErrorDialog(event.project, e.message ?: "", "Error")
        }
    }

    inner class PatchApp(
        override val root: File,
        private val initialFiles: Set<Path>,
    ) : ApplicationServer(
        applicationName = "Multi-file Patch Chat",
        path = "/patchChat",
        showMenubar = false,
    ) {
        // Add proper logging
        private val log = LoggerFactory.getLogger(PatchApp::class.java)

        override val singleInput = false
        override val stickyInput = true

        // Add validation
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

        private fun codeSummary(): String {
            return getCodeFiles().associateWith { root.toPath().resolve(it).toFile().readText(Charsets.UTF_8) }
                .entries.joinToString("\n\n") { (path, code) ->
                    val extension =
                        path.toString().split('.').lastOrNull()?.let { /*escapeHtml4*/(it)/*.indent("  ")*/ }
                    """
# $path
```$extension
${code.let { /*escapeHtml4*/(it)/*.indent("  ")*/ }}
```
""".trimMargin()
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
                fun mainActor() = SimpleActor(
                    prompt = """
|You are a helpful AI that helps people with coding.
|
|You will be answering questions about the following code:
|
|${codeSummary()}
|
|Response should use one or more code patches in diff format within ```diff code blocks.
|Each diff should be preceded by a header that identifies the file being modified.
|The diff format should use + for line additions, - for line deletions.
|The diff should include 2 lines of context before and after every change.
|
|Example:
|
|Here are the patches:
|
|### src/utils/exampleUtils.js
|```diff
| // Utility functions for example feature
| const b = 2;
| function exampleFunction() {
|-   return b + 1;
|+   return b + 2;
| }
|```
|
|### tests/exampleUtils.test.js
|```diff
| // Unit tests for exampleUtils
| const assert = require('assert');
| const { exampleFunction } = require('../src/utils/exampleUtils');
| 
| describe('exampleFunction', () => {
|-   it('should return 3', () => {
|+   it('should return 4', () => {
|     assert.equal(exampleFunction(), 3);
|   });
| });
|```
|
|If needed, new files can be created by using code blocks labeled with the filename in the same manner.
""".trimMargin(),
                    model = AppSettingsState.instance.smartModel.chatModel()
                )

                val settings = getSettings(session, user) ?: Settings()
                if (api is ChatClient) api.budget = settings.budget ?: 2.00

                val task = ui.newTask()
// Add progress indication
                task.add("Processing request...")

                val api = (api as ChatClient).getChildClient().apply {
                    val createFile = task.createFile(".logs/api-${UUID.randomUUID()}.log")
                    createFile.second?.apply {
                        logStreams += this.outputStream().buffered()
                        task.verbose("API log: <a href=\"file:///$this\">$this</a>")
                    }
                }
                val codex = GPT4Tokenizer()
                task.verbose(renderMarkdown(getCodeFiles().joinToString("\n") { path ->
                    "* $path - ${codex.estimateTokenCount(root.resolve(path.toFile()).readText())} tokens"
                }))
                val toInput = { it: String -> listOf(codeSummary(), it) }
                Discussable(
                    task = task,
                    userMessage = { userMessage },
                    heading = renderMarkdown(userMessage),
                    initialResponse = { it: String -> mainActor().answer(toInput(it), api = api) },
                    outputFn = { design: String ->
                        """<div>${
                            renderMarkdown(design) {
                                return@renderMarkdown ui.socketManager?.addApplyFileDiffLinks(
                                    root = root.toPath(),
                                    response = it,
                                    handle = { newCodeMap ->
                                        newCodeMap.forEach { (path, newCode) ->
                                            task.complete("<a href='${"fileIndex/$session/$path"}'>$path</a> Updated")
                                        }
                                    },
                                    ui = ui,
                                    api = api,
                                ) ?: it
                            }
                        }</div>"""
                    },
                    ui = ui,
                    reviseResponse = { userMessages: List<Pair<String, Role>> ->
                        mainActor().respond(messages = (userMessages.map { ApiModel.ChatMessage(it.second, it.first.toContentList()) }
                            .toTypedArray<ApiModel.ChatMessage>()),
                            input = toInput(userMessage),
                            api = api)
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


    private fun getFiles(
        virtualFiles: Array<out VirtualFile>?,
        root: Path
    ): MutableSet<Path> {
        val codeFiles = mutableSetOf<Path>()
        if (virtualFiles == null) {
            log.warn("No virtual files provided")
            return codeFiles
        }
        // Filter out unsupported file types
        val supportedExtensions = setOf("kt", "java", "py", "js", "ts", "html", "css", "xml")
        fun isSupportedFile(file: VirtualFile): Boolean {
            return file.extension in supportedExtensions
        }

        virtualFiles?.forEach { file ->
            if (file.isDirectory) {
                getFiles(file.children, root)
            } else if (isSupportedFile(file)) {
                codeFiles.add(root.relativize(file.toNioPath()))
            }
        }
        return codeFiles
    }


    companion object {
        private val log = LoggerFactory.getLogger(MultiDiffChatAction::class.java)

    }
}