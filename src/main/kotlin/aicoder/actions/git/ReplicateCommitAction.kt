package aicoder.actions.git

import aicoder.actions.BaseAction
import aicoder.actions.SessionProxyServer
import aicoder.actions.agent.toFile
import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.actionSystem.PlatformDataKeys
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.ui.Messages
import com.intellij.openapi.vcs.VcsDataKeys
import com.intellij.openapi.vcs.changes.Change
import com.intellij.openapi.vfs.VirtualFile
import com.simiacryptus.aicoder.AppServer
import com.simiacryptus.aicoder.config.AppSettingsState
import com.simiacryptus.aicoder.util.BrowseUtil.browse
import com.simiacryptus.aicoder.util.UITools
import com.simiacryptus.diff.AddApplyFileDiffLinks
import com.simiacryptus.jopenai.API
import com.simiacryptus.jopenai.describe.Description
import com.simiacryptus.jopenai.models.chatModel
import com.simiacryptus.skyenet.AgentPatterns
import com.simiacryptus.skyenet.Retryable
import com.simiacryptus.skyenet.core.actors.ParsedActor
import com.simiacryptus.skyenet.core.actors.SimpleActor
import com.simiacryptus.skyenet.core.platform.Session
import com.simiacryptus.skyenet.core.platform.model.User
import com.simiacryptus.skyenet.core.util.FileSelectionUtils
import com.simiacryptus.skyenet.core.util.IterativePatchUtil
import com.simiacryptus.skyenet.core.util.IterativePatchUtil.patchFormatPrompt
import com.simiacryptus.skyenet.util.MarkdownUtil.renderMarkdown
import com.simiacryptus.skyenet.webui.application.AppInfoData
import com.simiacryptus.skyenet.webui.application.ApplicationInterface
import com.simiacryptus.skyenet.webui.application.ApplicationServer
import com.simiacryptus.skyenet.webui.session.SessionTask
import com.simiacryptus.util.JsonUtil
import org.slf4j.LoggerFactory
import java.io.File
import java.nio.file.Files
import java.nio.file.Path
import java.text.SimpleDateFormat
import kotlin.io.path.ExperimentalPathApi
import kotlin.io.path.walk

class ReplicateCommitAction : BaseAction() {
  private val log = Logger.getInstance(ReplicateCommitAction::class.java)

  override fun getActionUpdateThread() = ActionUpdateThread.BGT

  override fun handle(event: AnActionEvent) {
    val project = event.project ?: return
    try {
      val settings = getUserSettings(event) ?: run {
        Messages.showErrorDialog(project, "Could not determine working directory", "Configuration Error")
        return
      }

      val dataContext = event.dataContext
      val virtualFiles = PlatformDataKeys.VIRTUAL_FILE_ARRAY.getData(dataContext)
      val folder = UITools.getSelectedFolder(event)
      var root = if (null != folder) {
        folder.toFile.toPath()
      } else {
        project.basePath?.let { File(it).toPath() }
      }!!

      val virtualFiles1 = event.getData(CommonDataKeys.VIRTUAL_FILE_ARRAY)
      val files = expand(virtualFiles1)
      val changes = event.getData(VcsDataKeys.CHANGES)
      val session = Session.newGlobalID()

      UITools.run(project, "Replicating Commit", true) { progress ->
        progress.text = "Generating diff info..."
        val diffInfo = generateDiffInfo(files, changes)
        progress.text = "Creating patch application..."
        val patchApp = object : PatchApp(root.toFile(), session, settings, diffInfo) {
          override fun codeFiles() = getFiles(virtualFiles)
            .filter { it.toFile().length() < 1024 * 1024 / 2 } // Limit to 0.5MB
            .map { root.relativize(it) ?: it }.toSet()

          override fun codeSummary(paths: List<Path>): String = paths
            .filter { it.toFile().exists() }
            .joinToString("\n\n") { path ->
              "# ${settings.workingDirectory.toPath().relativize(path)}\n$tripleTilde${path.toString().split('.').lastOrNull()}\n${
                path.toFile().readText(Charsets.UTF_8)
              }\n$tripleTilde"
            }

          override fun projectSummary(): String {
            val codeFiles = codeFiles()
            val str = codeFiles
              .asSequence()
              .filter { settings.workingDirectory.toPath()?.resolve(it)?.toFile()?.exists() == true }
              .distinct().sorted()
              .joinToString("\n") { path ->
                "* ${path} - ${
                  settings.workingDirectory.toPath()?.resolve(path)?.toFile()?.length() ?: "?"
                } bytes".trim()
              }
            return str
          }
        }
        progress.text = "Setting up session..."
        SessionProxyServer.metadataStorage.setSessionName(
          null,
          session,
          "${javaClass.simpleName} @ ${SimpleDateFormat("HH:mm:ss").format(System.currentTimeMillis())}"
        )
        SessionProxyServer.chats[session] = patchApp
        ApplicationServer.appInfoMap[session] = AppInfoData(
          applicationName = "Code Chat",
          singleInput = true,
          stickyInput = false,
          loadImages = false,
          showMenubar = false
        )
      }
      ApplicationManager.getApplication().executeOnPooledThread {
        Thread.sleep(500)
        try {
          val server = AppServer.getServer(project)
          val uri = server.server.uri.resolve("/#$session")
          log.info("Opening browser to $uri")
          browse(uri)
        } catch (e: Throwable) {
          log.error("Error opening browser", e)
          UITools.showErrorDialog("Failed to open browser: ${e.message}", "Error")
        }
      }
    } catch (e: Exception) {
      log.error("Error in ReplicateCommitAction", e)
      Messages.showErrorDialog(project, "Operation failed: ${e.message}", "Error")
    }
  }

  // Add proper enablement logic
  override fun isEnabled(event: AnActionEvent): Boolean {
    if (!super.isEnabled(event)) return false
    val project = event.project ?: return false
    val changes = event.getData(VcsDataKeys.CHANGES)
    return changes != null && changes.isNotEmpty()
  }

  private fun generateDiffInfo(files: Array<VirtualFile>?, changes: Array<out Change>?): String {
    val map = changes?.toList()
      ?.associateBy { (it.beforeRevision?.file ?: it.afterRevision?.file)!!.toString() }
    val entries = map?.entries
      ?.filter { (file, change) ->
        try {
          val find = files?.find { it.toNioPath().toFile().absolutePath == File(file).absolutePath }
          find != null
        } catch (e: Exception) {
          log.error("Error comparing changes", e)
          false
        }
      }
    return entries
      ?.joinToString("\n\n") { (file, change) ->
        val before = change.beforeRevision?.content
        val after = change.afterRevision?.content
        if ((before ?: after)!!.isBinary)
          return@joinToString "# Binary: ${change.afterRevision?.file}".prependIndent("  ")
        if (before == null) return@joinToString "# Deleted: ${change.afterRevision?.file}\n${after}".replace(
          "\n",
          "\n  "
        )
        if (after == null) return@joinToString "# Added: ${change.beforeRevision?.file}\n${before}".replace(
          "\n",
          "\n  "
        )
        val diff = IterativePatchUtil.generatePatch(before, after)
        "# Change: ${change.beforeRevision?.file}\n$diff".prependIndent("  ")
      } ?: "No changes found"
  }

  abstract inner class PatchApp(
    override val root: File,
    val session: Session,
    val settings: Settings,
    val diffInfo: String,
  ) : ApplicationServer(
    applicationName = "Replicate Commit",
    path = "/replicateCommit",
    showMenubar = false,
  ) {
    abstract fun codeFiles(): Set<Path>
    abstract fun codeSummary(paths: List<Path>): String
    override val singleInput = true
    override val stickyInput = false

    override fun userMessage(
      session: Session,
      user: User?,
      userMessage: String,
      ui: ApplicationInterface,
      api: API
    ) {
      val task = ui.newTask()
      task.echo(userMessage)
      Thread {
        run(ui, task, session, settings, userMessage, diffInfo)
      }.start()
    }

    abstract fun projectSummary(): String
  }

  private fun PatchApp.run(
    ui: ApplicationInterface,
    task: SessionTask,
    session: Session,
    settings: Settings,
    userMessage: String = "",
    diffInfo: String
  ) {
    try {
      val planTxt = projectSummary()
      task.add(renderMarkdown(planTxt))
      Retryable(ui, task) {
        val task = ui.newTask(false)
        val plan = ParsedActor(
          resultClass = ParsedTasks::class.java,
          prompt = """
                      You are a helpful AI that helps people with coding.
                      
                      You will be answering questions about the following project:
                      
                      Project Root: """.trimIndent() + (settings.workingDirectory.absolutePath ?: "") + """
                      
                      Files:
                      """.trimIndent() + planTxt + """
                      
                      Given the request, identify one or more tasks.
                      For each task:
                         1) predict the files that need to be fixed
                         2) predict related files that may be needed to debug the issue
                      """.trimIndent(),
          model = AppSettingsState.instance.smartModel.chatModel(),
          parsingModel = AppSettingsState.instance.fastModel.chatModel(),
        ).answer(
          listOf(
            "We want to create a change based on the following prior commit:\n\n$tripleTilde\n$diffInfo\n$tripleTilde\n\nThe change should implement the user's request:\n\n$tripleTilde\n$userMessage\n$tripleTilde"
          ), api = api
        )
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
        plan.obj.errors?.map { planTask ->
          Retryable(ui, task) {
            val task = ui.newTask(false)
            val paths =
              ((planTask.fixFiles ?: emptyList()) + (planTask.relatedFiles ?: emptyList())).flatMap {
                toPaths(settings.workingDirectory.toPath(), it)
              }
            val codeSummary = codeSummary(paths)
            val response = SimpleActor(
                prompt = """
                  You are a helpful AI that helps people with coding.
                  
                  You will be answering questions about the following code:
                  
                  """.trimIndent() + codeSummary + "\n" + patchFormatPrompt +
                        "\nIf needed, new files can be created by using code blocks labeled with the filename in the same manner.",
              model = AppSettingsState.instance.smartModel.chatModel()
            ).answer(
              listOf(
                """
                              We are working on executing the following directive:
                              
                              """.trimIndent() + tripleTilde + """
                              """.trimIndent() + userMessage + """
                              """.trimIndent() + tripleTilde + """
                              
                              Focus on the task at hand:
                              """.trimIndent() + (planTask.message?.prependIndent("  ") ?: "")
              ), api = api
            )
            var markdown = AddApplyFileDiffLinks.instrumentFileDiffs(
              ui.socketManager!!,
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
            task.add(renderMarkdown(markdown))
            task.placeholder
          }
          ""
        }?.joinToString { it }?.apply { task.add(this) }
        task.placeholder
      }
    } catch (e: Exception) {
      task.error(ui, e)
    }
  }

  data class ParsedTasks(
    val errors: List<ParsedTask>? = null
  )

  data class ParsedTask(
    @Description("The task to be performed")
    val message: String? = null,
    @Description("Files identified as needing modification and issue-related files")
    val relatedFiles: List<String>? = null,
    @Description("Files identified as needing modification and issue-related files")
    val fixFiles: List<String>? = null
  )

  data class Settings(
    var workingDirectory: File,
  )

  private fun getFiles(
    virtualFiles: Array<out VirtualFile>?
  ): MutableSet<Path> {
    val codeFiles = mutableSetOf<Path>()    // Set to avoid duplicates
    virtualFiles?.forEach { file ->
      if (file.isDirectory) {
        if (file.name.startsWith(".")) return@forEach
        if (FileSelectionUtils.Companion.isGitignore(file.toNioPath())) return@forEach
        codeFiles.addAll(getFiles(file.children))
      } else {
        codeFiles.add((file.toNioPath()))
      }
    }
    return codeFiles
  }

  private fun getUserSettings(event: AnActionEvent?): Settings? {
    val root = UITools.getSelectedFolder(event ?: return null)?.toNioPath() ?: event.project?.basePath?.let { File(it).toPath() }
    val files = UITools.getSelectedFiles(event).map { it.path.let { File(it).toPath() } }.toMutableSet()
    if (files.isEmpty()) Files.walk(root)
      .filter { Files.isRegularFile(it) && !Files.isDirectory(it) }
      .toList().filterNotNull().forEach { files.add(it) }
    return Settings(root?.toFile() ?: return null)
  }

  private fun expand(data: Array<VirtualFile>?): Array<VirtualFile>? {
    return data?.flatMap {
      if (it.isDirectory) {
        expand(it.children.toList().toTypedArray())?.toList() ?: listOf()
      } else {
        listOf(it)
      }
    }?.toTypedArray()
  }

  companion object {
    private val log = LoggerFactory.getLogger(ReplicateCommitAction::class.java)
    val tripleTilde = "`" + "``" // This is a workaround for the markdown parser when editing this file

    @OptIn(ExperimentalPathApi::class)
    fun toPaths(root: Path, it: String): Iterable<Path> {
      // Expand any wildcards
      if (it.contains("*")) {
        val prefix = it.substringBefore("*")
        val suffix = it.substringAfter("*")
        val files = root.walk().toList()
        val pathList = files.filter {
          it.toString().startsWith(prefix) && it.toString().endsWith(suffix)
        }.toList()
        return pathList
      } else {
        return listOf(Path.of(it))
      }
    }
  }
}