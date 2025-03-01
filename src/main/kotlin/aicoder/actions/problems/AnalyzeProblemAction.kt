package aicoder.actions.problems

import aicoder.actions.SessionProxyServer
import aicoder.actions.agent.toFile
import aicoder.actions.test.TestResultAutofixAction.Companion.findGitRoot
import aicoder.actions.test.TestResultAutofixAction.Companion.getProjectStructure
import aicoder.actions.test.TestResultAutofixAction.Companion.tripleTilde
import aicoder.actions.test.TestResultAutofixAction.ParsedError
import aicoder.actions.test.TestResultAutofixAction.ParsedErrors
import com.intellij.analysis.problemsView.toolWindow.ProblemNode
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.actionSystem.PlatformDataKeys
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.fileEditor.FileDocumentManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.TextRange
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiManager
import com.simiacryptus.aicoder.AppServer
import com.simiacryptus.aicoder.config.AppSettingsState
import com.simiacryptus.aicoder.util.BrowseUtil.browse
import com.simiacryptus.aicoder.util.IdeaChatClient
import com.simiacryptus.diff.AddApplyFileDiffLinks

import com.simiacryptus.jopenai.API
import com.simiacryptus.jopenai.models.chatModel
import com.simiacryptus.skyenet.AgentPatterns
import com.simiacryptus.skyenet.Retryable
import com.simiacryptus.skyenet.core.actors.ParsedActor
import com.simiacryptus.skyenet.core.actors.SimpleActor
import com.simiacryptus.skyenet.core.platform.Session
import com.simiacryptus.skyenet.core.platform.model.User
import com.simiacryptus.skyenet.util.MarkdownUtil.renderMarkdown
import com.simiacryptus.skyenet.webui.application.AppInfoData
import com.simiacryptus.skyenet.webui.application.ApplicationInterface
import com.simiacryptus.skyenet.webui.application.ApplicationServer
import com.simiacryptus.skyenet.webui.application.ApplicationSocketManager
import com.simiacryptus.skyenet.webui.session.SessionTask
import com.simiacryptus.skyenet.webui.session.SocketManager
import com.simiacryptus.util.JsonUtil
import java.text.SimpleDateFormat
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
            appendLine("Position: Line ${lineNumber + 1}, Column ${column + 1}")

            // Capture 5 lines of context
            val startLine = maxOf(0, lineNumber - 2)
            val endLine = minOf(document.lineCount - 1, lineNumber + 2)
            val contextLines = (startLine..endLine).map { line ->
              val start = document.getLineStartOffset(line)
              val end = document.getLineEndOffset(line)
              document.getText(TextRange(start, end))
            }
            appendLine("Context:")
            contextLines.forEachIndexed { index, content ->
              val linePrefix = if (index + startLine == lineNumber) ">" else " "
              appendLine("$linePrefix ${index + startLine + 1}: $content")
            }
            appendLine("${" ".repeat(column + 5)}^")
          }

          val projectStructure = getProjectStructure(gitRoot)
          appendLine("Project structure:\n  ${projectStructure.prependIndent("  ")}\n")
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
    val session = Session.newGlobalID()
    SessionProxyServer.chats[session] = ProblemAnalysisApp(session, problemInfo, gitRoot)
    ApplicationServer.appInfoMap[session] = AppInfoData(
      applicationName = "Code Chat",
      singleInput = false,
      stickyInput = true,
      loadImages = false,
      showMenubar = false
    )
    SessionProxyServer.metadataStorage.setSessionName(
      null,
      session,
      "${javaClass.simpleName} @ ${SimpleDateFormat("HH:mm:ss").format(System.currentTimeMillis())}"
    )

    val server = AppServer.getServer(project)

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
        analyzeProblem(ui, task, api = IdeaChatClient.instance)
      }.start()
      return socketManager
    }

    private fun analyzeProblem(ui: ApplicationInterface, task: SessionTask, api: API) {
      try {
        Retryable(ui, task) {
          val task = ui.newTask(false)
          val plan = ParsedActor(
            resultClass = ParsedErrors::class.java,
            prompt = """
                        You are a helpful AI that helps people with coding.
                        Given the response of a test failure, identify one or more distinct errors.
                        For each error:
                           1) predict the files that need to be fixed
                           2) predict related files that may be needed to debug the issue
                        """.trimIndent(),
            model = AppSettingsState.instance.smartModel.chatModel(),
            parsingModel = AppSettingsState.instance.fastModel.chatModel(),
          ).answer(listOf(problemInfo), api = IdeaChatClient.instance)

          task.add(
            AgentPatterns.displayMapInTabs(
              mapOf(
                "Text" to renderMarkdown(plan.text, ui = ui),
                "JSON" to renderMarkdown(
                  "${tripleTilde}json\n${JsonUtil.toJson(plan.obj)}\n$tripleTilde",
                  ui = ui
                ),
              )
            )
          )

          plan.obj.errors?.forEach { error ->
            Retryable(ui, task) {
              val task = ui.newTask(false)
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
              task.add(generateAndAddResponse(ui, task, error, summary, api))
              task.placeholder
            }
          }
          task.placeholder
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
      api: API
    ): String {
      val response = SimpleActor(
        prompt = """
            You are a helpful AI that helps people with coding.
            Suggest fixes for the following problem:
            """.trimIndent() + problemInfo + """

            Here are the relevant files:
            """.trimIndent() + summary + """

            Response should use one or more code patches in diff format within """.trimIndent() + tripleTilde + """diff code blocks.
            Each diff should be preceded by a header that identifies the file being modified.
            The diff format should use + for line additions, - for line deletions.
            The diff should include 2 lines of context before and after every change.
            """.trimIndent(),
        model = AppSettingsState.instance.smartModel.chatModel()
      ).answer(listOf(error.message ?: ""), api = IdeaChatClient.instance)

      return "<div>${
        renderMarkdown(
          AddApplyFileDiffLinks.instrumentFileDiffs(
            self = ui.socketManager!!,
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
        )
      }</div>"
    }

  }
}