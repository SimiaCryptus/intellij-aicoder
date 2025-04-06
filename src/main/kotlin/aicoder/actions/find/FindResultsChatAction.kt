package aicoder.actions.find

import aicoder.actions.BaseAction
import aicoder.actions.SessionProxyServer
import aicoder.actions.agent.MultiStepPatchAction
import aicoder.actions.agent.toFile
import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.vfs.findPsiFile
import com.intellij.psi.PsiDocumentManager
import com.intellij.usages.ReadWriteAccessUsageInfo2UsageAdapter
import com.intellij.usages.Usage
import com.intellij.usages.UsageView
import com.simiacryptus.aicoder.AppServer
import com.simiacryptus.aicoder.config.AppSettingsState
import com.simiacryptus.aicoder.util.BrowseUtil.browse
import com.simiacryptus.aicoder.util.UITools
import com.simiacryptus.jopenai.API
import com.simiacryptus.jopenai.ChatClient
import com.simiacryptus.jopenai.models.chatModel
import com.simiacryptus.skyenet.Retryable
import com.simiacryptus.skyenet.apps.general.renderMarkdown
import com.simiacryptus.skyenet.core.actors.SimpleActor
import com.simiacryptus.skyenet.core.platform.Session
import com.simiacryptus.skyenet.core.platform.model.User
import com.simiacryptus.skyenet.core.util.getModuleRootForFile
import com.simiacryptus.skyenet.util.MarkdownUtil.renderMarkdown
import com.simiacryptus.skyenet.webui.application.AppInfoData
import com.simiacryptus.skyenet.webui.application.ApplicationInterface
import com.simiacryptus.skyenet.webui.application.ApplicationServer
import com.simiacryptus.skyenet.webui.session.getChildClient
import java.io.File
import java.text.SimpleDateFormat
import javax.swing.Icon

class FindResultsChatAction(
    name: String? = "Chat About Find Results",
    description: String? = "Start a code chat about find results",
    icon: Icon? = null
) : BaseAction(name, description, icon) {

    override fun getActionUpdateThread() = ActionUpdateThread.BGT

    override fun handle(event: AnActionEvent) {
        val project = event.project ?: return
        val usageView = event.getData(UsageView.USAGE_VIEW_KEY) ?: return
        val usages = usageView.usages.toTypedArray()

        if (usages.isEmpty()) {
            UITools.showWarning(project, "No find results selected for chat")
            return
        }

        try {
            val root = getModuleRootForFile(
                UITools.getSelectedFile(event)?.parent?.toFile
                    ?: throw RuntimeException("No file selected")
            )

            val session = Session.newGlobalID()
            SessionProxyServer.metadataStorage.setSessionName(
                null,
                session,
                "${javaClass.simpleName} @ ${SimpleDateFormat("HH:mm:ss").format(System.currentTimeMillis())}"
            )

            val fileListMap = usages.groupBy { getFile(it) }
            SessionProxyServer.chats[session] = ChatApp(
                root = root,
                project = project,
                usages = fileListMap
            )

            ApplicationServer.appInfoMap[session] = AppInfoData(
                applicationName = "Find Results Chat",
                singleInput = false,
                stickyInput = true,
                loadImages = false,
                showMenubar = false
            )

            val server = AppServer.getServer(event.project)
            UITools.runAsync(event.project, "Opening Browser", true) { progress ->
                Thread.sleep(500)
                try {
                    val uri = server.server.uri.resolve("/#$session")
                    log.info("Opening browser to $uri")
                    browse(uri)
                } catch (e: Throwable) {
                    val message = "Failed to open browser: ${e.message}"
                    log.error(message, e)
                    UITools.showErrorDialog(message, "Error")
                }
            }
        } catch (ex: Exception) {
            UITools.error(log, "Error starting chat", ex)
        }
    }

    private fun getFile(it: Usage): VirtualFile? {
        var file = it.location?.editor?.file
        if (file != null && file.isValid) return file
        if (it is ReadWriteAccessUsageInfo2UsageAdapter) file = it.file
        if (file != null && file.isValid) return file
        log.warn("Usage location does not have an editor, cannot determine file")
        return null
    }

    override fun isEnabled(event: AnActionEvent): Boolean {
        val usageView = event.getData(UsageView.USAGE_VIEW_KEY)
        return usageView != null && usageView.usages.isNotEmpty()
    }

    inner class ChatApp(
        override val root: File,
        val project: Project,
        val usages: Map<VirtualFile?, List<Usage>>
    ) : ApplicationServer(
        applicationName = "Find Results Chat",
        path = "/findChat",
        showMenubar = false,
    ) {
        override val singleInput = false
        override val stickyInput = true
        private fun formatLine(index: Int, line: String, isFocused: Boolean) = when {
            isFocused -> "/* L$index */ $line /* <<< */"
            else -> "/* L$index */ $line"
        }

        private fun getFilteredLines(project: Project, file: VirtualFile, usages: List<Usage>): String? {
            val document = PsiDocumentManager.getInstance(project)
                .getDocument(file.findPsiFile(project) ?: return null) ?: return null
            return document.text.lines().mapIndexed { index: Int, line: String ->
                val lineStart = document.getLineStartOffset(index)
                val lineEnd = document.getLineEndOffset(index)
                val intersectingUsages = usages.filter { usage ->
                    val startOffset = usage.navigationOffset
                    val endOffset = startOffset + 1
                    when {
                        startOffset >= lineEnd -> false
                        endOffset <= lineStart -> false
                        else -> true
                    }
                }
                when {
                    intersectingUsages.isNotEmpty() -> formatLine(index, line, true)
                    else -> "..."
                }
            }.joinToString("\n").replace("(?:\\.\\.\\.\n){2,}".toRegex(), "...\n")
        }


        private fun getCodeContext(): String {
            return usages.entries.joinToString("\n\n") { (file, usages) ->
                file ?: return@joinToString ""

                val document = PsiDocumentManager.getInstance(project).getDocument(
                    file.findPsiFile(project) ?: return@joinToString ""
                ) ?: return@joinToString ""

                val usageLocations = usages.joinToString("\n") { usage ->
                    val lineNumber = document.getLineNumber(usage.navigationOffset)
                    "* Line ${lineNumber + 1}: ${usage.presentation.plainText}"
                }
                "\n## ${file.name}\nUsage locations:\n$usageLocations\n```${file.extension}\n${
                    getFilteredLines(
                        project,
                        file,
                        usages
                    )
                }\n```"
            }
        }

        override fun userMessage(
            session: Session,
            user: User?,
            userMessage: String,
            ui: ApplicationInterface,
            api: API
        ) {
            val settings = getSettings(session, user) ?: MultiStepPatchAction.AutoDevApp.Settings()
            if (api is ChatClient) api.budget = settings.budget ?: 2.00

            val task = ui.newTask()
            val api = (api as ChatClient).getChildClient(task)

            task.echo(renderMarkdown(userMessage))

            task.verbose((getCodeContext()).renderMarkdown())

            Retryable(ui = ui, task = task) { content ->
                val task = ui.newTask(false)
                task.add("<div>" + renderMarkdown(
                        SimpleActor(
                            prompt = """
                             You are a helpful AI that helps people understand code.
                             You will be answering questions about code with the following find results:
                             """.trimIndent() + getCodeContext(),
                            model = AppSettingsState.instance.smartModel.chatModel()
                        ).answer(listOf(userMessage), api = api)
                    ) + "</div>"
                )
                task.placeholder
            }
        }
    }
}