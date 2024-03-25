package com.github.simiacryptus.aicoder.actions.generic

import com.github.simiacryptus.aicoder.ApplicationEvents
import com.github.simiacryptus.aicoder.actions.BaseAction
import com.github.simiacryptus.aicoder.actions.dev.AppServer
import com.github.simiacryptus.aicoder.config.AppSettingsState
import com.github.simiacryptus.aicoder.config.AppSettingsState.Companion.chatModel
import com.github.simiacryptus.aicoder.util.ComputerLanguage
import com.github.simiacryptus.aicoder.util.addApplyDiffLinks2
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.PlatformDataKeys
import com.intellij.openapi.command.WriteCommandAction
import com.intellij.openapi.fileEditor.FileDocumentManager
import com.simiacryptus.skyenet.core.platform.ApplicationServices
import com.simiacryptus.skyenet.core.platform.Session
import com.simiacryptus.skyenet.core.platform.StorageInterface
import com.simiacryptus.skyenet.core.platform.User
import com.simiacryptus.skyenet.webui.application.ApplicationServer
import com.simiacryptus.skyenet.webui.chat.ChatServer
import com.simiacryptus.skyenet.webui.chat.ChatSocketManager
import com.simiacryptus.skyenet.webui.session.SessionTask
import com.simiacryptus.skyenet.webui.session.SocketManager
import com.simiacryptus.skyenet.webui.util.MarkdownUtil.renderMarkdown
import org.slf4j.LoggerFactory
import java.awt.Desktop
import java.io.File
import java.nio.file.Path

class MultiDiffChatAction : BaseAction() {

  val path = "/multiDiffChat"

  override fun handle(e: AnActionEvent) {

    val dataContext = e.dataContext
    val virtualFiles = PlatformDataKeys.VIRTUAL_FILE_ARRAY.getData(dataContext)
    val languages =
      virtualFiles?.associate { it to ComputerLanguage.findByExtension(it.extension ?: "")?.name } ?: mapOf()
    val virtualFileMap = virtualFiles?.associate { it.toNioPath() to it } ?: mapOf()
    val codeFiles = mutableMapOf<String, String>()
    val root = virtualFiles?.map { file ->
      file.toNioPath()
    }?.toTypedArray()?.commonRoot()!!
    val paths = virtualFiles.associate { file ->
      val relative = root.relativize(file.toNioPath())
      val path = relative.toString()
      val language = languages[file] ?: "plaintext"
      val code = file.contentsToByteArray().toString(Charsets.UTF_8)
      codeFiles[path] = code
      path to language
    }

    fun codeSummary() = codeFiles.entries.joinToString("\n\n") { (path, code) ->
      "# $path\n```${
        path.split('.').last()
      }\n$code\n```"
    }
    val session = StorageInterface.newGlobalID()
    //DataStorage.sessionPaths[session] = root.toFile()

    agents[session] = object : ChatSocketManager(
      session = session,
      model = AppSettingsState.instance.smartModel.chatModel(),
      userInterfacePrompt = """
        |
        |${codeSummary()}
        |
        """.trimMargin().trim(),
      systemPrompt = """
        You are a helpful AI that helps people with coding.
        
        You will be answering questions about the following code:
        
        ${codeSummary()}
        
        Response should use one or more code patches in diff format within ```diff code blocks.
        Each diff should be preceded by a header that identifies the file being modified.
        The diff format should use + for line additions, - for line deletions.
        The diff should include 2 lines of context before and after every change.
        
        Example:
        
        Explanation text
        
        ### scripts/filename.js
        ```diff
        - const b = 2;
        + const a = 1;
        ```
        
        Continued text
        """.trimIndent(),
      api = api,
      applicationClass = ApplicationServer::class.java,
      storage = ApplicationServices.dataStorageFactory(DiffChatAction.root),
    ) {
      override fun renderResponse(response: String, task: SessionTask): String {
        val html = renderMarkdown(addApplyDiffLinks2(codeFiles, response, handle = { newCodeMap ->
          newCodeMap.map { (path, newCode) ->
            val prev = codeFiles[path]
            if (prev != newCode) {
              codeFiles[path] = newCode
              root.resolve(path).let { file ->
                file.toFile().writeText(newCode)
                val virtualFile = virtualFileMap.get(file)
                if(null != virtualFile) FileDocumentManager.getInstance().getDocument(virtualFile)?.let { doc ->
                  WriteCommandAction.runWriteCommandAction(e.project) {
                    doc.setText(newCode)
                  }
                }
              }
              "<a href='$path'>$path</a> Updated"
            } else {
//              "<a href='$path'>$path</a> Unchanged"
              ""
            }
          }
        }, task = task))
        return """<div>$html</div>"""
      }
    }

    val server = AppServer.getServer(e.project)
    val app = initApp(server, path)
    app.sessions[session] = app.newSession(null, session)

    Thread {
      Thread.sleep(500)
      try {
        Desktop.getDesktop().browse(server.server.uri.resolve("$path/#$session"))
      } catch (e: Throwable) {
        log.warn("Error opening browser", e)
      }
    }.start()
  }

  override fun isEnabled(event: AnActionEvent) = true

  companion object {
    private val log = LoggerFactory.getLogger(MultiDiffChatAction::class.java)
    private val agents = mutableMapOf<Session, SocketManager>()
    val root: File get() = File(ApplicationEvents.pluginHome, "mdiff_chat")
    private fun initApp(server: AppServer, path: String): ChatServer {
      server.appRegistry[path]?.let { return it }
      val socketServer = object : ApplicationServer("Multi-file Diff Chat", path) {
        override val singleInput = false
        override val stickyInput = true
        override fun newSession(user: User?, session: Session) = agents[session]!!
      }
      server.addApp(path, socketServer)
      return socketServer
    }

  }
}

fun Array<Path>.commonRoot() : Path = when {
  isEmpty() -> error("No paths")
  size == 1 && first().toFile().isFile -> first().parent
  size == 1 -> first()
  else -> this.reduce { a, b ->
    when {
      a.startsWith(b) -> b
      b.startsWith(a) -> a
      else -> when (val common = a.commonPrefixWith(b)) {
        a -> a
        b -> b
        else -> common.toAbsolutePath()
      }
    }
  }
}
private fun Path.commonPrefixWith(b: Path): Path {
  val a = this
  val aParts = a.toAbsolutePath().toString().split(File.separator)
  val bParts = b.toAbsolutePath().toString().split(File.separator)
  val common = aParts.zip(bParts).takeWhile { (a, b) -> a == b }.map { it.first }
  return File(File.separator + common.joinToString(File.separator)).toPath()
}

