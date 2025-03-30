package aicoder.actions.agent

import com.simiacryptus.aicoder.config.AppSettingsState
import com.simiacryptus.diff.AddApplyFileDiffLinks
import com.simiacryptus.jopenai.API
import com.simiacryptus.jopenai.ChatClient
import com.simiacryptus.jopenai.models.ApiModel
import com.simiacryptus.jopenai.models.chatModel
import com.simiacryptus.jopenai.util.ClientUtil.toContentList
import com.simiacryptus.skyenet.Discussable
import com.simiacryptus.skyenet.TabbedDisplay
import com.simiacryptus.skyenet.apps.general.renderMarkdown
import com.simiacryptus.skyenet.core.actors.SimpleActor
import com.simiacryptus.skyenet.core.platform.Session
import com.simiacryptus.skyenet.core.platform.model.User
import com.simiacryptus.skyenet.core.util.FixedConcurrencyProcessor
import com.simiacryptus.skyenet.util.MarkdownUtil.renderMarkdown
import com.simiacryptus.skyenet.webui.application.ApplicationServer
import com.simiacryptus.skyenet.webui.application.ApplicationSocketManager
import com.simiacryptus.skyenet.webui.session.SocketManager
import com.simiacryptus.skyenet.webui.session.getChildClient
import java.nio.file.Path
import java.util.concurrent.Semaphore
import java.util.concurrent.atomic.AtomicReference

class DocumentedMassPatchServer(
  val config: DocumentedMassPatchAction.Settings, val api: ChatClient, val autoApply: Boolean
  /**
   * Server for handling documented mass code patches
   * @param config Settings containing project and file configurations
   * @param api ChatClient for AI interactions
   * @param autoApply Whether to automatically apply suggested patches */
) : ApplicationServer(
  applicationName = "Documented Code Patch",
  path = "/patchChat",
  showMenubar = false,
) {
  private lateinit var _root: Path
  
  override val singleInput = false
  override val stickyInput = true
  
  private val mainActor: SimpleActor
    get() {
      return SimpleActor(
        prompt = """
         You are a helpful AI that helps people with coding.
         
         You will be reviewing code files based on documentation files and suggesting improvements.
         Please analyze both the documentation and code to ensure they are aligned and suggest improvements.
         
         Response should use one or more code patches in diff format within ```diff code blocks.
         Each diff should be preceded by a header that identifies the file being modified.
         The diff format should use + for line additions, - for line deletions.
         The diff should include 2 lines of context before and after every change.
         """.trimIndent(),
        model = AppSettingsState.instance.smartModel.chatModel(),
        temperature = AppSettingsState.instance.temperature,
      )
    }
  
  /**
   * Creates a new session for handling code review and patch generation
   * @param user The user initiating the session
   * @param session The session context
   * @return SocketManager for managing the session
   */
  
  override fun newSession(user: User?, session: Session): SocketManager {
    val socketManager = super.newSession(user, session)
    val ui = (socketManager as ApplicationSocketManager).applicationInterface
    _root = config.project?.basePath?.let { Path.of(it) } ?: Path.of(".")
    val task = ui.newTask(true)
    val api = api.getChildClient(task)
    
    val tabs = TabbedDisplay(task)
    val userMessage = config.settings?.transformationMessage ?: "Review and update code according to documentation"
    
    // Process documentation files first
    val docSummary = config.settings?.documentationFiles?.joinToString("\n\n") { path ->
      """
             # Documentation: $path
             ```md
             ${_root.resolve(path).toFile().readText(Charsets.UTF_8)}
             ```
             """.trimIndent()
    } ?: ""
    
    // Then process code files
    val fixedConcurrencyProcessor = FixedConcurrencyProcessor(socketManager.pool, 4)
    config.settings?.codeFilePaths?.map { path: Path ->
      fixedConcurrencyProcessor.submit {
        try {
          task.add("Processing ${path}...")
          val codeSummary = """
                             $docSummary
                             
                             # Code: $path
                             ```${path.toString().split('.').lastOrNull()}
                             ${_root.resolve(path).toFile().readText(Charsets.UTF_8)}
                             ```
                         """.trimIndent()
          
          val fileTask = ui.newTask(false).apply {
            tabs[path.toString()] = placeholder
          }
          
          val toInput = { it: String -> listOf(codeSummary, it) }
          if (autoApply) {
            val design = mainActor.answer(toInput(userMessage), api = api).toContentList().firstOrNull()?.text ?: ""
            if (design.isNotBlank()) {
              task.add(
                AddApplyFileDiffLinks.instrumentFileDiffs(
                  self = ui.socketManager!!,
                  root = _root,
                  response = design,
                  handle = { newCodeMap ->
                    newCodeMap.forEach { (path, newCode) ->
                      fileTask.complete("<a href='${"fileIndex/$session/$path"}'>$path</a> Updated")
                    }
                  },
                  ui = ui,
                  api = api as API,
                  shouldAutoApply = { autoApply },
                  model = AppSettingsState.instance.fastModel.chatModel(),
                  defaultFile = path.toString()
                ).renderMarkdown
              )
            } else {
              fileTask.complete("No changes suggested.")
            }
          } else {
            Discussable(
              task = fileTask,
              userMessage = { userMessage },
              heading = renderMarkdown(userMessage),
              initialResponse = {
                mainActor.answer(toInput(it), api = api)
              },
              outputFn = { design: String ->
                """<div>${
                  renderMarkdown(design) {
                    AddApplyFileDiffLinks.instrumentFileDiffs(
                      self = ui.socketManager!!,
                      root = _root,
                      response = design,
                      handle = { newCodeMap ->
                        newCodeMap.forEach { (path, newCode) ->
                          fileTask.complete("<a href='${"fileIndex/$session/$path"}'>$path</a> Updated")
                        }
                      },
                      ui = ui,
                      api = api as API,
                      shouldAutoApply = { autoApply },
                      model = AppSettingsState.instance.fastModel.chatModel(),
                      defaultFile = path.toString()
                    )
                  }
                }</div>"""
              },
              ui = ui,
              reviseResponse = { userMessages ->
                mainActor.respond(
                  messages = userMessages.map { ApiModel.ChatMessage(it.second, it.first.toContentList()) }.toTypedArray(),
                  input = toInput(userMessage),
                  api = api
                )
              },
              atomicRef = AtomicReference(),
              semaphore = Semaphore(0),
            ).call()
          }
          task.add("Completed processing ${path}")
        } catch (e: Exception) {
          log.warn("Error processing $path", e)
          task.error(ui, e)
        }
      }
    }//?.toTypedArray()?.forEach { it.get() }
    return socketManager
  }
  
  companion object {
    private val log = org.slf4j.LoggerFactory.getLogger(DocumentedMassPatchServer::class.java)
  }
}

