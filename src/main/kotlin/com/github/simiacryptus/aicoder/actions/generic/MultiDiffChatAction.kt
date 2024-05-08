﻿package com.github.simiacryptus.aicoder.actions.generic

import com.github.simiacryptus.aicoder.AppServer
import com.github.simiacryptus.aicoder.actions.BaseAction
import com.github.simiacryptus.aicoder.actions.generic.MultiStepPatchAction.AutoDevApp.Settings
import com.github.simiacryptus.aicoder.util.UITools
import com.simiacryptus.diff.addApplyFileDiffLinks
import com.simiacryptus.diff.addSaveLinks
import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.PlatformDataKeys
import com.intellij.openapi.vfs.VirtualFile
import com.simiacryptus.jopenai.API
import com.simiacryptus.jopenai.ApiModel
import com.simiacryptus.jopenai.ApiModel.Role
import com.simiacryptus.jopenai.models.ChatModels
import com.simiacryptus.jopenai.util.ClientUtil.toContentList
import com.simiacryptus.skyenet.Discussable
import com.simiacryptus.skyenet.core.actors.ActorSystem
import com.simiacryptus.skyenet.core.actors.BaseActor
import com.simiacryptus.skyenet.core.actors.SimpleActor
import com.simiacryptus.skyenet.core.platform.*
import com.simiacryptus.skyenet.webui.application.ApplicationInterface
import com.simiacryptus.skyenet.webui.application.ApplicationServer
import com.simiacryptus.skyenet.webui.chat.ChatServer
import com.simiacryptus.skyenet.webui.util.MarkdownUtil.renderMarkdown
import org.slf4j.LoggerFactory
import java.awt.Desktop
import java.io.File
import java.nio.file.Path
import java.util.concurrent.Semaphore
import java.util.concurrent.atomic.AtomicReference

class MultiDiffChatAction : BaseAction() {
    override fun getActionUpdateThread() = ActionUpdateThread.BGT

    val path = "/multiDiffChat"

    override fun handle(event: AnActionEvent) {

        var root: Path? = null
        val codeFiles: MutableSet<Path> = mutableSetOf()
        fun codeSummary() = codeFiles.filter {
            root!!.resolve(it).toFile().exists()
        }.associateWith { root!!.resolve(it).toFile().readText(Charsets.UTF_8) }
            .entries.joinToString("\n\n") { (path, code) ->
                val extension = path.toString().split('.').lastOrNull()?.let { /*escapeHtml4*/(it)/*.indent("  ")*/ }
                """
            |# $path
            |```$extension
            |${code.let { /*escapeHtml4*/(it)/*.indent("  ")*/ }}
            |```
            """.trimMargin()
            }

        val dataContext = event.dataContext
        val virtualFiles = PlatformDataKeys.VIRTUAL_FILE_ARRAY.getData(dataContext)
        val folder = UITools.getSelectedFolder(event)
        root = if (null != folder) {
            folder.toFile.toPath()
        } else {
            getModuleRootForFile(UITools.getSelectedFile(event)?.parent?.toFile ?: throw RuntimeException("")).toPath()
        }

        val files = getFiles(virtualFiles, root!!)
        codeFiles.addAll(files)

        val session = StorageInterface.newGlobalID()

        agents[session] = PatchApp(event, root!!.toFile(), { codeSummary() }, codeFiles)

        val server = AppServer.getServer(event.project)
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

    inner class PatchApp(
        private val event: AnActionEvent,
        override val root: File,
        val codeSummary: () -> String,
        val codeFiles: Set<Path> = setOf(),
    ) : ApplicationServer(
        applicationName = "Multi-file Patch Chat",
        path = path,
        showMenubar = false,
    ) {
        override val singleInput = false
        override val stickyInput = true

        override fun userMessage(
            session: Session,
            user: User?,
            userMessage: String,
            ui: ApplicationInterface,
            api: API
        ) {
            val settings = getSettings(session, user) ?: Settings()
            if (api is ClientManager.MonitoredClient) api.budget = settings.budget ?: 2.00
            PatchAgent(
                api = api,
                dataStorage = dataStorage,
                session = session,
                user = user,
                ui = ui,
                model = settings.model!!,
                event = event,
                root = root,
                codeSummary = { codeSummary() },
                codeFiles = { codeFiles },
            ).start(
                userMessage = userMessage,
            )
        }
    }

    enum class ActorTypes {
        MainActor,
    }

    inner class PatchAgent(
        val api: API,
        dataStorage: StorageInterface,
        session: Session,
        user: User?,
        val ui: ApplicationInterface,
        val model: ChatModels,
        val codeSummary: () -> String,
        val codeFiles: () -> Set<Path>,
        actorMap: Map<ActorTypes, BaseActor<*, *>> = mapOf(
            ActorTypes.MainActor to SimpleActor(
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
                model = model
            ),
        ),
        val event: AnActionEvent,
        val root: File,
    ) : ActorSystem<ActorTypes>(
        actorMap.map { it.key.name to it.value }.toMap(), dataStorage, user, session
    ) {

        private val mainActor by lazy { getActor(ActorTypes.MainActor) as SimpleActor }

        fun start(
            userMessage: String,
        ) {
            val task = ui.newTask()
            val toInput = { it: String -> listOf(codeSummary(), it) }
            Discussable(
                task = task,
                userMessage = { userMessage },
                heading = userMessage,
                initialResponse = { it: String -> mainActor.answer(toInput(it), api = api) },
                outputFn = { design: String ->
                    var markdown = ui.socketManager.addApplyFileDiffLinks(
                        root = root.toPath(),
                        code = { codeFiles().associateWith { root.resolve(it.toFile()).readText(Charsets.UTF_8) } },
                        response = design,
                        handle = { newCodeMap ->
                            newCodeMap.forEach { (path, newCode) ->
                                task.complete("<a href='${"fileIndex/$session/$path"}'>$path</a> Updated")
                            }
                        },
                        ui = ui,
                    )
                    markdown = ui.socketManager.addSaveLinks(
                        response = markdown,
                        task = task,
                        ui = ui,
                        handle = { path, newCode ->
                            root.resolve(path.toFile()).writeText(newCode, Charsets.UTF_8)
                        },
                    )
                    """<div>${renderMarkdown(markdown)}</div>"""
                },
                ui = ui,
                reviseResponse = { userMessages: List<Pair<String, Role>> ->
                    mainActor.respond(
                        messages = (userMessages.map { ApiModel.ChatMessage(it.second, it.first.toContentList()) }
                            .toTypedArray<ApiModel.ChatMessage>()),
                        input = toInput(userMessage),
                        api = api
                    )
                },
                atomicRef = AtomicReference(),
                semaphore = Semaphore(0),
            ).call()
        }
    }

    private fun getFiles(
        virtualFiles: Array<out VirtualFile>?,
        root: Path
    ): MutableSet<Path> {
        val codeFiles = mutableSetOf<Path>()
        virtualFiles?.forEach { file ->
            if (file.isDirectory) {
                getFiles(file.children, root)
            } else {
                val relative = root.relativize(file.toNioPath())
                codeFiles.add(relative) //[] = file.contentsToByteArray().toString(Charsets.UTF_8)
            }
        }
        return codeFiles
    }

    override fun isEnabled(event: AnActionEvent) = true

    companion object {
        private val log = LoggerFactory.getLogger(MultiDiffChatAction::class.java)
        private val agents = mutableMapOf<Session, ApplicationServer>()
        private fun initApp(server: AppServer, path: String): ChatServer {
            server.appRegistry[path]?.let { return it }
            val socketServer = object : ApplicationServer(
                applicationName = "Multi-file Patch Chat",
                path = path,
                showMenubar = false,
            ) {
                override val singleInput = true
                override val stickyInput = false
                override fun newSession(user: User?, session: Session) = agents[session]!!.newSession(user, session)
            }
            server.addApp(path, socketServer)
            return socketServer
        }

    }
}
