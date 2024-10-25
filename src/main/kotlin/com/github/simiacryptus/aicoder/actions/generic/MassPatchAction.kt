package com.github.simiacryptus.aicoder.actions.generic

import com.github.simiacryptus.aicoder.AppServer
import com.github.simiacryptus.aicoder.actions.BaseAction
import com.github.simiacryptus.aicoder.actions.generic.MassPatchAction.Settings
import com.github.simiacryptus.aicoder.config.AppSettingsState
import com.github.simiacryptus.aicoder.config.Name
import com.github.simiacryptus.aicoder.util.BrowseUtil.browse
import com.github.simiacryptus.aicoder.util.UITools
import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.DialogWrapper
import com.intellij.ui.CheckBoxList
import com.intellij.ui.components.JBScrollPane
import com.intellij.ui.components.JBTextArea
import com.simiacryptus.diff.FileValidationUtils.Companion.isLLMIncludableFile
import com.simiacryptus.diff.addApplyFileDiffLinks
import com.simiacryptus.jopenai.API
import com.simiacryptus.jopenai.ChatClient
import com.simiacryptus.jopenai.models.ApiModel
import com.simiacryptus.jopenai.models.ApiModel.Role
import com.simiacryptus.jopenai.models.chatModel
import com.simiacryptus.jopenai.util.ClientUtil.toContentList
import com.simiacryptus.skyenet.Discussable
import com.simiacryptus.skyenet.TabbedDisplay
import com.simiacryptus.skyenet.core.actors.SimpleActor
import com.simiacryptus.skyenet.core.platform.Session
import com.simiacryptus.skyenet.core.platform.model.User
import com.simiacryptus.skyenet.util.MarkdownUtil.renderMarkdown
import com.simiacryptus.skyenet.webui.application.AppInfoData
import com.simiacryptus.skyenet.webui.application.ApplicationServer
import com.simiacryptus.skyenet.webui.application.ApplicationSocketManager
import com.simiacryptus.skyenet.webui.session.SocketManager
import com.simiacryptus.skyenet.webui.session.SocketManagerBase
import java.awt.BorderLayout
import java.awt.Dimension
import java.nio.file.Files
import java.nio.file.Path
import java.util.UUID
import java.util.concurrent.Semaphore
import java.util.concurrent.atomic.AtomicReference
import javax.swing.Box
import javax.swing.BoxLayout
import javax.swing.DefaultComboBoxModel
import javax.swing.JComboBox
import javax.swing.JComponent
import javax.swing.JLabel
import javax.swing.JPanel

class MassPatchAction : BaseAction() {
    override fun getActionUpdateThread() = ActionUpdateThread.BGT
    override fun isEnabled(event: AnActionEvent): Boolean {
        if (UITools.getSelectedFile(event)?.isDirectory == false) return false
        return super.isEnabled(event)
    }

    class SettingsUI {
        @Name("Files to Process")
        val filesToProcess = CheckBoxList<Path>()

        @Name("AI Instruction")
        val transformationMessage = JBTextArea(4, 40)

        @Name("Recent Instructions")
        val recentInstructions = JComboBox<String>()

    }

    class UserSettings(
        var transformationMessage: String = "Review, fix, and improve",
        var filesToProcess: List<Path> = listOf(),
    )

    class Settings(
        val settings: UserSettings? = null,
        val project: Project? = null
    )

    fun getConfig(project: Project?, e: AnActionEvent): Settings? {
        val root = UITools.getSelectedFolder(e)?.toNioPath()
        val files = Files.walk(root)
            .filter { isLLMIncludableFile(it.toFile()) }
            .toList().filterNotNull().toTypedArray()
        val settingsUI = SettingsUI().apply {
            filesToProcess.setItems(files.toMutableList()) { path ->
                root?.relativize(path)?.toString() ?: path.toString()
            }
            files.forEach { path ->
                filesToProcess.setItemSelected(path, true)
            }
        }
        val mruPatchInstructions = AppSettingsState.instance.getRecentCommands("PatchInstructions")
        settingsUI.recentInstructions.model = DefaultComboBoxModel(
            mruPatchInstructions.getMostRecent(10).toTypedArray()
        )
        settingsUI.recentInstructions.selectedIndex = -1
        settingsUI.recentInstructions.addActionListener {
            updateUIFromSelection(settingsUI)
        }

        val dialog = ConfigDialog(project, settingsUI, "Mass Patch")
        dialog.show()
        if (!dialog.isOK) return null
        val settings: UserSettings = dialog.userSettings
        settings.filesToProcess = files.filter { path -> settingsUI.filesToProcess.isItemSelected(path) }.toList()
        mruPatchInstructions.addInstructionToHistory(settings.transformationMessage)
        return Settings(settings, project)
    }

    private fun updateUIFromSelection(settingsUI: SettingsUI) {
        val selected = settingsUI.recentInstructions.selectedItem as? String
        if (selected != null) {
            settingsUI.transformationMessage.text = selected
        }
    }


    override fun handle(e: AnActionEvent) {
        val project = e.project
        val config = getConfig(project, e)

        val session = Session.newGlobalID()
        ApplicationServer.appInfoMap[session] = AppInfoData(
            applicationName = "Code Chat",
            singleInput = true,
            stickyInput = false,
            loadImages = false,
            showMenubar = false
        )

        val server = AppServer.getServer(e.project)
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

    class ConfigDialog(project: Project?, private val settingsUI: SettingsUI, title: String) : DialogWrapper(project) {
        val userSettings = UserSettings()

        init {
            this.title = title
            // Set the default values for the UI elements from userSettings
            settingsUI.transformationMessage.text = userSettings.transformationMessage
            init()
        }

        override fun createCenterPanel(): JComponent {
            val panel = JPanel(BorderLayout()).apply {
                val filesScrollPane = JBScrollPane(settingsUI.filesToProcess).apply {
                    preferredSize = Dimension(400, 300) // Adjust the preferred size as needed
                }
                add(JLabel("Files to Process"), BorderLayout.NORTH)
                add(filesScrollPane, BorderLayout.CENTER) // Make the files list the dominant element

                val optionsPanel = JPanel().apply {
                    layout = BoxLayout(this, BoxLayout.Y_AXIS)
                    add(JLabel("Recent Instructions"))
                    add(settingsUI.recentInstructions)
                    add(Box.createVerticalStrut(10))
                    add(JLabel("AI Instruction"))
                    add(settingsUI.transformationMessage)
                }
                add(optionsPanel, BorderLayout.SOUTH)
            }
            return panel
        }

        override fun doOKAction() {
            super.doOKAction()
            userSettings.transformationMessage = settingsUI.transformationMessage.text
            userSettings.filesToProcess =
                settingsUI.filesToProcess.items.filter { path -> settingsUI.filesToProcess.isItemSelected(path) }
        }
    }
}

class MassPatchServer(
    val config: Settings,
    val api: ChatClient
) : ApplicationServer(
    applicationName = "Multi-file Patch Chat",
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
                            |You are a helpful AI that helps people with coding.
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
                model = AppSettingsState.instance.smartModel.chatModel(),
                temperature = AppSettingsState.instance.temperature,
            )
        }

    override fun newSession(user: User?, session: Session): SocketManager {
        val socketManager = super.newSession(user, session)
        val ui = (socketManager as ApplicationSocketManager).applicationInterface
        _root = config.project?.basePath?.let { Path.of(it) } ?: Path.of(".")
        val task = ui.newTask(true)
        val api = (api as ChatClient).getChildClient().apply {
            val createFile = task.createFile(".logs/api-${UUID.randomUUID()}.log")
            createFile.second?.apply {
                logStreams += this.outputStream().buffered()
                task.verbose("API log: <a href=\"file:///$this\">$this</a>")
            }
        }
        val tabs = TabbedDisplay(task)
        val userMessage = config.settings?.transformationMessage ?: "Create user documentation"
        val codeFiles = config.settings?.filesToProcess
        codeFiles?.forEach { path ->
            socketManager.scheduledThreadPoolExecutor.schedule({
                socketManager.pool.submit {
                    try {
                        val codeSummary = listOf(path)
                            .filter { isLLMIncludableFile(it.toFile()) }
                            ?.associateWith { it.toFile().readText(Charsets.UTF_8) }
                            ?.entries?.joinToString("\n\n") { (path, code) ->
                                val extension = path.toString().split('.').lastOrNull()
                                """
                            |# $path
                            |```$extension
                            |${code.let { /*escapeHtml4*/(it)/*.indent("  ")*/ }}
                            |```
                            """.trimMargin()
                            }
                        val fileTask = ui.newTask(false).apply {
                            tabs[path.toString()] = placeholder
                        }
                        val toInput = { it: String -> listOf(codeSummary ?: "", it) }
                        Discussable(
                            task = fileTask,
                            userMessage = { userMessage },
                            heading = renderMarkdown(userMessage),
                            initialResponse = {
                                mainActor.answer(toInput(it), api = api)
                            },
                            outputFn = { design: String ->
                                var markdown = (ui as SocketManagerBase).addApplyFileDiffLinks(
                                    root = _root as Path,
                                    response = design as String,
                                    handle = { newCodeMap: Map<Path, String> ->
                                        newCodeMap.forEach { (path, newCode) ->
                                            fileTask.complete("<a href='${"fileIndex/$session/$path"}'>$path</a> Updated")
                                        }
                                    } as (Map<Path, String>) -> Unit,
                                    ui = ui,
                                    api = api as API,
                                    shouldAutoApply = { true } as (Path) -> Boolean,
                                )
                                """<div>${renderMarkdown(markdown!!)}</div>"""
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
                    } catch (e: Exception) {
                        log.warn("Error processing $path", e)
                        task.error(ui, e)
                    }
                }
            }, 10, java.util.concurrent.TimeUnit.MILLISECONDS)

        }
        return socketManager
    }

    companion object {
        val log = org.slf4j.LoggerFactory.getLogger(MassPatchServer::class.java)
    }
}
