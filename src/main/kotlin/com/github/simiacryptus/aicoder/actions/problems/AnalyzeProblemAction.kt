package com.github.simiacryptus.aicoder.actions.problems

import com.github.simiacryptus.aicoder.AppServer
import com.github.simiacryptus.aicoder.actions.generic.SessionProxyServer
import com.github.simiacryptus.aicoder.actions.generic.toFile
import com.github.simiacryptus.aicoder.actions.test.TestResultAutofixAction.*
import com.github.simiacryptus.aicoder.actions.test.TestResultAutofixAction.Companion.findGitRoot
import com.github.simiacryptus.aicoder.actions.test.TestResultAutofixAction.Companion.getProjectStructure
import com.github.simiacryptus.aicoder.actions.test.TestResultAutofixAction.Companion.tripleTilde
import com.github.simiacryptus.aicoder.config.AppSettingsState
import com.github.simiacryptus.aicoder.util.IdeaOpenAIClient
import com.intellij.analysis.problemsView.toolWindow.ProblemNode
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.actionSystem.PlatformDataKeys
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiManager
import com.intellij.openapi.fileEditor.FileDocumentManager
import com.simiacryptus.diff.addApplyFileDiffLinks
import com.simiacryptus.diff.addSaveLinks
import com.simiacryptus.jopenai.API
import com.simiacryptus.jopenai.util.JsonUtil
import com.simiacryptus.skyenet.AgentPatterns
import com.simiacryptus.skyenet.Retryable
import com.simiacryptus.skyenet.core.actors.ParsedActor
import com.simiacryptus.skyenet.core.actors.SimpleActor
import com.simiacryptus.skyenet.core.platform.Session
import com.simiacryptus.skyenet.core.platform.StorageInterface
import com.simiacryptus.skyenet.core.platform.User
import com.simiacryptus.skyenet.webui.application.ApplicationInterface
import com.simiacryptus.skyenet.webui.application.ApplicationServer
import com.simiacryptus.skyenet.webui.application.ApplicationSocketManager
import com.simiacryptus.skyenet.webui.session.SessionTask
import com.simiacryptus.skyenet.webui.session.SocketManager
import com.simiacryptus.skyenet.webui.util.MarkdownUtil.renderMarkdown
import java.awt.Desktop
import javax.swing.JOptionPane

class AnalyzeProblemAction : AnAction() {
    companion object {
        private val log = Logger.getInstance(AnalyzeProblemAction::class.java)
    }

    override fun actionPerformed(e: AnActionEvent) {
        val project: Project = e.project ?: return
        val item = e.getData(PlatformDataKeys.SELECTED_ITEM) as ProblemNode? ?: return
        val file: VirtualFile = e.getData(CommonDataKeys.VIRTUAL_FILE) ?: return
        val gitRoot = findGitRoot(file)

        Thread {
            try {
                val problemInfo = buildString {
                    appendLine("File: ${file.path}")
                    appendLine("Problem: ${item.text}")

                    val psiFile = PsiManager.getInstance(project).findFile(file)
                    val fileType = if (psiFile != null) {
                        val fileType = psiFile.fileType.name
                        appendLine("File type: $fileType")
                        fileType
                    } else {
                        ""
                    }

                    val document = FileDocumentManager.getInstance().getDocument(file)
                    if (document != null) {
                        val lineNumber = item.line
                        val column = item.column
                        appendLine("Position: Line $lineNumber, Column $column")

                        val lineContent = document.getText(com.intellij.openapi.util.TextRange(
                            document.getLineStartOffset(lineNumber), document.getLineEndOffset(lineNumber)
                        ))
                        appendLine("Source:\n  $lineContent\n${" ".repeat(column + 2)}^\n")
                    }
                    val projectStructure = getProjectStructure(gitRoot)
                    appendLine("Project structure:\n  ${projectStructure.replace("\n","\n  ")}\n")
                    appendLine("## ${file.path}\n```${fileType.lowercase()}\n${document?.text}\n```\n")
                }
                log.info("Problem info: $problemInfo")
                openAnalysisSession(project, problemInfo, gitRoot)
            } catch (ex: Throwable) {
                log.error("Error analyzing problem", ex)
                JOptionPane.showMessageDialog(null, ex.message, "Error", JOptionPane.ERROR_MESSAGE)
            }
        }.start()

    }

    private fun openAnalysisSession(project: Project, problemInfo: String, gitRoot: VirtualFile?) {
        val session = StorageInterface.newGlobalID()
        SessionProxyServer.chats[session] = ProblemAnalysisApp(session, problemInfo, gitRoot)
        ApplicationServer.sessionAppInfoMap[session.toString()] = mapOf(
            "applicationName" to "Problem Analysis",
            "singleInput" to false,
            "stickyInput" to true,
            "loadImages" to false,
            "showMenubar" to false,
        )

        val server = AppServer.getServer(project)

        Thread {
            Thread.sleep(500)
            try {
                val uri = server.server.uri.resolve("/#$session")
                log.info("Opening browser to $uri")
                Desktop.getDesktop().browse(uri)
            } catch (e: Throwable) {
                log.warn("Error opening browser", e)
            }
        }.start()
    }

    override fun update(e: AnActionEvent) {
        val project = e.project
        val file = e.getData(CommonDataKeys.VIRTUAL_FILE)
        e.presentation.isEnabledAndVisible = project != null && file != null
    }

    inner class ProblemAnalysisApp(
        val session: Session,
        val problemInfo: String,
        val gitRoot: VirtualFile?
    ) : ApplicationServer(
        applicationName = "Problem Analysis",
        path = "/analyzeProblem",
        showMenubar = false,
    ) {
        override val singleInput = true
        override val stickyInput = false

        override fun newSession(user: User?, session: Session): SocketManager {
            val socketManager = super.newSession(user, session)
            val ui = (socketManager as ApplicationSocketManager).applicationInterface
            val task = ui.newTask()
            task.add("Analyzing problem and suggesting fixes...")
            Thread {
                analyzeProblem(ui, task, api = IdeaOpenAIClient.instance)
            }.start()
            return socketManager
        }

        private fun analyzeProblem(ui: ApplicationInterface, task: SessionTask, api: API) {
            try {
                Retryable(ui, task) {
                    val plan = ParsedActor(
                        resultClass = ParsedErrors::class.java,
                        prompt = """
                        You are a helpful AI that helps people with coding.
                        Given the response of a test failure, identify one or more distinct errors.
                        For each error:
                           1) predict the files that need to be fixed
                           2) predict related files that may be needed to debug the issue
                        """.trimIndent(),
                        model = AppSettingsState.instance.defaultSmartModel()
                    ).answer(listOf(problemInfo), api = IdeaOpenAIClient.instance)

                    task.add(
                        AgentPatterns.displayMapInTabs(
                        mapOf(
                            "Text" to renderMarkdown(plan.text, ui = ui),
                            "JSON" to renderMarkdown(
                                "${tripleTilde}json\n${JsonUtil.toJson(plan.obj)}\n$tripleTilde",
                                ui = ui
                            ),
                        )
                    ))

                    plan.obj.errors?.forEach { error ->
                        Retryable(ui, task) {
                            val filesToFix = (error.fixFiles ?: emptyList()) + (error.relatedFiles ?: emptyList())
                            val summary = filesToFix.joinToString("\n\n") { filePath ->
                                val file = gitRoot?.toFile?.resolve(filePath)
                                if (file?.exists() == true) {
                                    """
                                    # $filePath
                                    $tripleTilde${filePath.split('.').lastOrNull()}
                                    ${file.readText()}
                                    $tripleTilde
                                    """.trimIndent()
                                } else {
                                    "# $filePath\nFile not found"
                                }
                            }

                            generateAndAddResponse(ui, task, error, summary, filesToFix, api)
                        }
                    }
                    ""
                }
            } catch (e: Exception) {
                task.error(ui, e)
            }
        }
        private fun generateAndAddResponse(
            ui: ApplicationInterface,
            task: SessionTask,
            error: ParsedError,
            summary: String,
            filesToFix: List<String>,
            api: API
        ) : String {
            val response = SimpleActor(
                prompt = """
                |You are a helpful AI that helps people with coding.
                |Suggest fixes for the following problem:
                |$problemInfo

                |Here are the relevant files:
                |$summary

                |Response should use one or more code patches in diff format within ${tripleTilde}diff code blocks.
                |Each diff should be preceded by a header that identifies the file being modified.
                |The diff format should use + for line additions, - for line deletions.
                |The diff should include 2 lines of context before and after every change.
                """.trimMargin(),
                model = AppSettingsState.instance.defaultSmartModel()
            ).answer(listOf(error.message ?: ""), api = IdeaOpenAIClient.instance)

            var markdown = ui.socketManager?.addApplyFileDiffLinks(
                root = root.toPath(),
                response = response,
                handle = { newCodeMap ->
                    newCodeMap.forEach { (path, newCode) ->
                        task.complete("<a href='${"fileIndex/$session/$path"}'>$path</a> Updated")
                    }
                },
                ui = ui,
                api = api,
            )
            markdown = ui.socketManager?.addSaveLinks(
                root = root.toPath(),
                response = markdown!!,
                task = task,
                ui = ui,
            )
            val msg = "<div>${renderMarkdown(markdown!!)}</div>"
            return msg
        }

    }
}