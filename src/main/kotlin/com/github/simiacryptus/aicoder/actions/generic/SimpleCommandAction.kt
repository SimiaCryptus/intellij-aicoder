package com.github.simiacryptus.aicoder.actions.generic

import com.github.simiacryptus.aicoder.AppServer
import com.github.simiacryptus.aicoder.actions.BaseAction
import com.github.simiacryptus.aicoder.config.AppSettingsState
import com.github.simiacryptus.aicoder.util.FileSystemUtils.isGitignore
import com.github.simiacryptus.aicoder.util.UITools
import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.PlatformDataKeys
import com.intellij.openapi.vfs.VirtualFile
import com.simiacryptus.diff.addApplyFileDiffLinks
import com.simiacryptus.diff.addSaveLinks
import com.simiacryptus.jopenai.API
import com.simiacryptus.jopenai.describe.Description
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
import com.simiacryptus.skyenet.webui.session.SessionTask
import com.simiacryptus.skyenet.webui.util.MarkdownUtil.renderMarkdown
import org.slf4j.LoggerFactory
import java.awt.Desktop
import java.io.File
import java.nio.file.Files
import java.nio.file.Path
import kotlin.io.path.ExperimentalPathApi
import kotlin.io.path.walk

class SimpleCommandAction : BaseAction() {
    override fun getActionUpdateThread() = ActionUpdateThread.BGT

    override fun handle(event: AnActionEvent) {
        val settings = getUserSettings(event) ?: return
        val dataContext = event.dataContext
        val virtualFiles = PlatformDataKeys.VIRTUAL_FILE_ARRAY.getData(dataContext)
        val folder = UITools.getSelectedFolder(event)
        var root = if (null != folder) {
            folder.toFile.toPath()
        } else {
            event.project?.basePath?.let { File(it).toPath() }
        }!!

        val session = StorageInterface.newGlobalID()
        val patchApp = object : PatchApp(root.toFile(), session, settings) {
            override fun codeFiles() = getFiles(virtualFiles)
                .filter { it.toFile().length() < 1024 * 1024 / 2 } // Limit to 0.5MB
                .map { root.relativize(it) ?: it }.toSet()

            override fun codeSummary(paths: List<Path>): String = paths
                .filter { it.toFile().exists() }
                .joinToString("\n\n") { path ->
                    """
                        |# ${settings.workingDirectory?.toPath()?.relativize(path)}
                        |$tripleTilde${path.toString().split('.').lastOrNull()}
                        |${path.toFile().readText(Charsets.UTF_8)}
                        |$tripleTilde
                        """.trimMargin()
                }

            override fun projectSummary(): String {
                val codeFiles = codeFiles()
                val str = codeFiles
                    .asSequence()
                    .filter { settings.workingDirectory?.toPath()?.resolve(it)?.toFile()?.exists() == true }
                    .distinct().sorted()
                    .joinToString("\n") { path ->
                        "* ${path} - ${
                            settings.workingDirectory?.toPath()?.resolve(path)?.toFile()?.length() ?: "?"
                        } bytes".trim()
                    }
                return str
            }
        }
        SessionProxyServer.chats[session] = patchApp
        val server = AppServer.getServer(event.project)

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

    abstract inner class PatchApp(
        override val root: File,
        val session: Session,
        val settings: Settings,
    ) : ApplicationServer(
        applicationName = "Magic Code Genie",
        path = "/doCmd",
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
            task.echo(renderMarkdown(userMessage))
            Thread {
                run(ui, task, session, settings, userMessage)
            }.start()
            task.placeholder
        }

        abstract fun projectSummary(): String
    }

    private fun PatchApp.run(
        ui: ApplicationInterface,
        task: SessionTask,
        session: Session,
        settings: Settings,
        userMessage: String = ""
    ) {
        try {
            val planTxt = projectSummary()
            task.add(renderMarkdown(planTxt))
            Retryable(ui,task) {
                val plan = ParsedActor(
                    resultClass = ParsedTasks::class.java,
                    prompt = """
                        |You are a helpful AI that helps people with coding.
                        |
                        |You will be answering questions about the following project:
                        |
                        |Project Root: ${settings.workingDirectory?.absolutePath ?: ""}
                        |
                        |Files:
                        |$planTxt
                        |
                        |Given the request, identify one or more tasks.
                        |For each task:
                        |   1) predict the files that need to be fixed
                        |   2) predict related files that may be needed to debug the issue
                        """.trimMargin(),
                    model = AppSettingsState.instance.defaultSmartModel()
                ).answer(
                    listOf(
                        """
                            |Execute the following directive:
                            |
                            |$tripleTilde
                            |$userMessage
                            |$tripleTilde
                            |""".trimMargin()
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
                val progress = ui.newTask()
                val progressHeader = progress.header("Processing tasks")
                val errors = plan.obj.errors?.map { planTask ->
                    Retryable(ui, task) {
                        val paths =
                            ((planTask.fixFiles ?: emptyList()) + (planTask.relatedFiles ?: emptyList())).flatMap {
                                toPaths(settings.workingDirectory.toPath(), it)
                            }
                        val codeSummary = codeSummary(paths.map { settings.workingDirectory.toPath().resolve(it) })
                        val response = SimpleActor(
                            prompt = """
                            |You are a helpful AI that helps people with coding.
                            |
                            |You will be answering questions about the following code:
                            |
                            |$codeSummary
                            |
                            |
                            |Response should use one or more code patches in diff format within ${tripleTilde}diff code blocks.
                            |Each diff should be preceded by a header that identifies the file being modified.
                            |The diff format should use + for line additions, - for line deletions.
                            |The diff should include 2 lines of context before and after every change.
                            |
                            |Example:
                            |
                            |Here are the patches:
                            |
                            |### src/utils/exampleUtils.js
                            |${tripleTilde}diff
                            | // Utility functions for example feature
                            | const b = 2;
                            | function exampleFunction() {
                            |-   return b + 1;
                            |+   return b + 2;
                            | }
                            |$tripleTilde
                            |
                            |### tests/exampleUtils.test.js
                            |${tripleTilde}diff
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
                            |$tripleTilde
                            |
                            |If needed, new files can be created by using code blocks labeled with the filename in the same manner.
                            """.trimMargin(),
                            model = AppSettingsState.instance.defaultSmartModel()
                        ).answer(
                            listOf(
                                """
                                |We are working on executing the following directive:
                                |
                                |${tripleTilde}
                                |$userMessage
                                |${tripleTilde}
                                |
                                |Focus on the task at hand:
                                |  ${planTask.message?.replace("\n", "\n  ") ?: ""}
                                |""".trimMargin()
                            ), api = api
                        )
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
                        "<div>${renderMarkdown(markdown!!)}</div>"
                    }
                    ""
                }?.joinToString { it } ?: ""
                progressHeader?.clear()
                progress.append("", false)
                ""
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

    private fun getUserSettings(event: AnActionEvent?): Settings? {
        val root = UITools.getSelectedFolder(event ?: return null)?.toNioPath() ?: event.project?.basePath?.let {
            File(
                it
            ).toPath()
        }
        val files = UITools.getSelectedFiles(event).map { it.path.let { File(it).toPath() } }.toMutableSet()
        if (files.isEmpty()) Files.walk(root)
            .filter { Files.isRegularFile(it) && !Files.isDirectory(it) }
            .toList().filterNotNull().forEach { files.add(it) }
        return Settings(root?.toFile() ?: return null)
    }



    override fun isEnabled(event: AnActionEvent) = true

    companion object {
        private val log = LoggerFactory.getLogger(SimpleCommandAction::class.java)
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

        fun getFiles(
            virtualFiles: Array<out VirtualFile>?
        ): MutableSet<Path> {
            val codeFiles = mutableSetOf<Path>()    // Set to avoid duplicates
            virtualFiles?.forEach { file ->
                if (file.isDirectory) {
                    if (file.name.startsWith(".")) return@forEach
                    if (isGitignore(file)) return@forEach
                    if (file.name.endsWith(".png")) return@forEach
                    if (file.length > 1024 * 256) return@forEach
                    codeFiles.addAll(getFiles(file.children))
                } else {
                    codeFiles.add((file.toNioPath()))
                }
            }
            return codeFiles
        }
    }
}