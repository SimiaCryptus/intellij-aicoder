package com.github.simiacryptus.aicoder.actions.test

import com.github.simiacryptus.aicoder.AppServer
import com.github.simiacryptus.aicoder.actions.BaseAction
import com.github.simiacryptus.aicoder.actions.generic.SessionProxyServer
import com.github.simiacryptus.aicoder.config.AppSettingsState
import com.github.simiacryptus.aicoder.util.FileSystemUtils.isGitignore
import com.github.simiacryptus.aicoder.util.IdeaChatClient
import com.intellij.execution.testframework.AbstractTestProxy
import com.intellij.execution.testframework.sm.runner.SMTestProxy
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.PlatformDataKeys
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.vfs.VirtualFile
import com.simiacryptus.diff.FileValidationUtils.Companion.isGitignore
import com.simiacryptus.diff.addApplyFileDiffLinks
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
import org.jetbrains.annotations.NotNull
import java.awt.Desktop
import java.io.File
import java.nio.file.Path
import javax.swing.JOptionPane

class TestResultAutofixAction : BaseAction() {
    companion object {
        private val log = Logger.getInstance(TestResultAutofixAction::class.java)
        val tripleTilde = "`" + "``" // This is a workaround for the markdown parser when editing this file

        fun getFiles(
            virtualFiles: Array<out VirtualFile>?
        ): MutableSet<Path> {
            val codeFiles = mutableSetOf<Path>()    // Set to avoid duplicates
            virtualFiles?.forEach { file ->
                if(file.name.startsWith(".")) return@forEach
                if(isGitignore(file)) return@forEach
                if (file.isDirectory) {
                    codeFiles.addAll(getFiles(file.children))
                } else {
                    codeFiles.add((file.toNioPath()))
                }
            }
            return codeFiles
        }
        fun getFiles(
            virtualFiles: Array<out Path>?
        ): MutableSet<Path> {
            val codeFiles = mutableSetOf<Path>()    // Set to avoid duplicates
            virtualFiles?.forEach { file ->
                if(file.fileName.startsWith(".")) return@forEach
                if(isGitignore(file)) return@forEach
                if (file.toFile().isDirectory) {
                    codeFiles.addAll(getFiles(file.toFile().listFiles().map { it.toPath() }.toTypedArray()))
                } else {
                    codeFiles.add(file)
                }
            }
            return codeFiles
        }

        fun getProjectStructure(projectPath: VirtualFile?): String {
            return getProjectStructure(Path.of((projectPath?: return "Project path is null").path))
        }

        fun getProjectStructure(root: Path): String {
            val codeFiles = getFiles(arrayOf(root))
                .filter { it.toFile().length() < 1024 * 1024 / 2 } // Limit to 0.5MB
                .map { root.relativize(it) ?: it }.toSet()
            val str = codeFiles
                .asSequence()
                .filter { root?.resolve(it)?.toFile()?.exists() == true }
                .distinct().sorted()
                .joinToString("\n") { path ->
                    "* ${path} - ${root?.resolve(path)?.toFile()?.length() ?: "?"} bytes".trim()
                }
            return str
        }

        fun findGitRoot(path: Path?): Path? {
            var current: Path? = path
            while (current != null) {
                if (current.resolve(".git").toFile().exists()) {
                    return current
                }
                current = current.parent
            }
            return null
        }

        fun findGitRoot(virtualFile: VirtualFile?): VirtualFile? {
            var current: VirtualFile? = virtualFile
            while (current != null) {
                if (current.findChild(".git") != null) {
                    return current
                }
                current = current.parent
            }
            return null
        }
    }

    override fun handle(e: AnActionEvent) {
        val testProxy = e.getData(AbstractTestProxy.DATA_KEY) as? SMTestProxy ?: return
        val dataContext = e.dataContext
        val virtualFile = PlatformDataKeys.VIRTUAL_FILE_ARRAY.getData(dataContext)?.firstOrNull()
        val root = Companion.findGitRoot(virtualFile)
        Thread {
            try {
                val testInfo = getTestInfo(testProxy)
                val projectStructure = getProjectStructure(root)
                openAutofixWithTestResult(e, testInfo, projectStructure)
            } catch (ex: Throwable) {
                log.error("Error analyzing test result", ex)
                JOptionPane.showMessageDialog(null, ex.message, "Error", JOptionPane.ERROR_MESSAGE)
            }
        }.start()
    }

    override fun isEnabled(@NotNull e: AnActionEvent): Boolean {
        val testProxy = e.getData(AbstractTestProxy.DATA_KEY)
        return testProxy != null
    }

    private fun getTestInfo(testProxy: SMTestProxy): String {
        val sb = StringBuilder()
        sb.appendLine("Test Name: ${testProxy.name}")
        sb.appendLine("Duration: ${testProxy.duration} ms")
        
        if (testProxy.errorMessage != null) {
            sb.appendLine("Error Message:")
            sb.appendLine(testProxy.errorMessage)
        }
        
        if (testProxy.stacktrace != null) {
            sb.appendLine("Stacktrace:")
            sb.appendLine(testProxy.stacktrace)
        }
        
        return sb.toString()
    }

    private fun openAutofixWithTestResult(e: AnActionEvent, testInfo: String, projectStructure: String) {
        val session = StorageInterface.newGlobalID()
        SessionProxyServer.chats[session] = TestResultAutofixApp(session, testInfo, e.project?.basePath, projectStructure)
        ApplicationServer.sessionAppInfoMap[session.toString()] = mapOf(
            "applicationName" to "Test Result Autofix",
            "singleInput" to false,
            "stickyInput" to true,
            "loadImages" to false,
            "showMenubar" to false,
        )

        val server = AppServer.getServer(e.project)

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

    inner class TestResultAutofixApp(
        val session: Session,
        val testInfo: String,
        val projectPath: String?,
        val projectStructure: String
    ) : ApplicationServer(
        applicationName = "Test Result Autofix",
        path = "/fixTest",
        showMenubar = false,
    ) {
        override val singleInput = true
        override val stickyInput = false
        override fun newSession(user: User?, session: Session): SocketManager {
            val socketManager = super.newSession(user, session)
            val ui = (socketManager as ApplicationSocketManager).applicationInterface
            val task = ui.newTask()
            task.add("Analyzing test result and suggesting fixes...")
            Thread {
                runAutofix(ui, task)
            }.start()
            return socketManager
        }

        private fun runAutofix(ui: ApplicationInterface, task: SessionTask) {
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
                        
                        Project structure:
                        $projectStructure
                           1) predict the files that need to be fixed
                           2) predict related files that may be needed to debug the issue
                        """.trimIndent(),
                        model = AppSettingsState.instance.defaultSmartModel()
                    ).answer(listOf(testInfo), api = IdeaChatClient.instance)

                    task.add(AgentPatterns.displayMapInTabs(
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
                                val file = File(projectPath, filePath)
                                if (file.exists()) {
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

                            generateAndAddResponse(ui, task, error, summary, filesToFix)
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
            filesToFix: List<String>
        ) : String {
            val response = SimpleActor(
                prompt = """
                You are a helpful AI that helps people with coding.
                Suggest fixes for the following test failure:
                $testInfo

                Here are the relevant files:
                $summary

Project structure:
$projectStructure

                Response should use one or more code patches in diff format within ${tripleTilde}diff code blocks.
                Each diff should be preceded by a header that identifies the file being modified.
                The diff format should use + for line additions, - for line deletions.
                The diff should include 2 lines of context before and after every change.
                """.trimIndent(),
                model = AppSettingsState.instance.defaultSmartModel()
            ).answer(listOf(error.message ?: ""), api = IdeaChatClient.instance)

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
            val msg = "<div>${renderMarkdown(markdown!!)}</div>"
            return msg
        }
    }

    data class ParsedErrors(
        val errors: List<ParsedError>? = null
                    )

    data class ParsedError(
        val message: String? = null,
        val relatedFiles: List<String>? = null,
        val fixFiles: List<String>? = null
    )
}