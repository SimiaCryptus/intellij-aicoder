package com.github.simiacryptus.aicoder.actions.generic

import ai.grazie.utils.mpp.UUID
import com.github.simiacryptus.aicoder.AppServer
import com.github.simiacryptus.aicoder.actions.BaseAction
import com.github.simiacryptus.aicoder.actions.BaseAction.Companion
import com.github.simiacryptus.aicoder.actions.generic.MultiStepPatchAction.AutoDevApp.Settings
import com.github.simiacryptus.aicoder.config.AppSettingsState
import com.github.simiacryptus.aicoder.config.AppSettingsState.Companion.imageModel
import com.github.simiacryptus.aicoder.util.UITools
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
import com.simiacryptus.skyenet.core.actors.*
import com.simiacryptus.skyenet.core.platform.*
import com.simiacryptus.skyenet.core.platform.file.DataStorage
import com.simiacryptus.skyenet.webui.application.ApplicationInterface
import com.simiacryptus.skyenet.webui.application.ApplicationServer
import com.simiacryptus.skyenet.webui.util.MarkdownUtil.renderMarkdown
import org.slf4j.LoggerFactory
import java.awt.Desktop
import java.io.ByteArrayOutputStream
import java.io.File
import java.nio.file.Path
import java.util.concurrent.Semaphore
import java.util.concurrent.atomic.AtomicReference
import javax.imageio.ImageIO

class CreateImageAction : BaseAction() {
    override fun getActionUpdateThread() = ActionUpdateThread.BGT

    val path = "/imageCreator"

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
        } else if (1 == virtualFiles?.size) {
            UITools.getSelectedFile(event)?.parent?.toNioPath()
        } else {
            getModuleRootForFile(UITools.getSelectedFile(event)?.parent?.toFile ?: throw RuntimeException("")).toPath()
        }

        val files = getFiles(virtualFiles, root!!)
        codeFiles.addAll(files)

        val session = StorageInterface.newGlobalID()
//        val storage = ApplicationServices.dataStorageFactory(root?.toFile()!!) as DataStorage?
//        val selectedFile = UITools.getSelectedFolder(event)
        if (/*null != storage &&*/ null != root) {
            DataStorage.sessionPaths[session] = root?.toFile()!!
        }

        SessionProxyServer.chats[session] = PatchApp(event, root!!.toFile(), ::codeSummary)

        val server = AppServer.getServer(event.project)

        Thread {
            Thread.sleep(500)
            try {

                val uri = server.server.uri.resolve("/#$session")
                BaseAction.log.info("Opening browser to $uri")
                Desktop.getDesktop().browse(uri)
            } catch (e: Throwable) {
                log.warn("Error opening browser", e)
            }
        }.start()
    }

    inner class PatchApp(
        private val event: AnActionEvent,
        override val root: File,
        val codeSummary: () -> String = { "" },
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
                codeSummary = { codeSummary() },
                event = event,
                root = root,
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
        val codeSummary: () -> String = { "" },
        actorMap: Map<ActorTypes, BaseActor<*, *>> = mapOf(
            ActorTypes.MainActor to ImageActor(
                prompt = """
                        |You are a technical drawing assistant.
                        |
                        |You will be composing an image about the following code:
                        |
                        |${codeSummary()}
                        |
                        """.trimMargin(),
                textModel = model,
                imageModel = AppSettingsState.instance.mainImageModel.imageModel()
            ),
        ),
        val event: AnActionEvent,
        val root: File,
    ) : ActorSystem<ActorTypes>(
        actorMap.map { it.key.name to it.value }.toMap(), dataStorage, user, session
    ) {

        private val mainActor by lazy { getActor(ActorTypes.MainActor) as ImageActor }

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
                outputFn = { img: ImageResponse ->
                    val id = UUID.random().text
                    renderMarkdown(
                        "<img src='${
                            task.saveFile(
                                "$id.png",
                                write(img, root.resolve("$id.png").toPath())
                            )
                        }' style='max-width: 100%;'/><img src='${
                            task.saveFile(
                                "$id.jpg",
                                write(img, root.resolve("$id.jpg").toPath())
                            )
                        }' style='max-width: 100%;'/>", ui = ui
                    )
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
    private fun write(
        code: ImageResponse,
        path: Path
    ): ByteArray {
        val byteArrayOutputStream = ByteArrayOutputStream()
        val data = ImageIO.write(
            code.image,
            path.toString().split(".").last(),
            byteArrayOutputStream
        )
        val bytes = byteArrayOutputStream.toByteArray()
        return bytes
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
        private val log = LoggerFactory.getLogger(CreateImageAction::class.java)
    }
}
