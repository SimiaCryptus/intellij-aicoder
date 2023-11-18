package com.github.simiacryptus.aicoder.actions

import com.github.simiacryptus.aicoder.config.AppSettingsState
import com.github.simiacryptus.aicoder.util.UITools
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.LocalFileSystem
import com.intellij.openapi.vfs.VirtualFile
import org.slf4j.LoggerFactory
import java.io.File

abstract class FileContextAction<T : Any>(
    private val supportsFiles: Boolean = true,
    private val supportsFolders: Boolean = true,
) : BaseAction() {

    data class SelectionState(
        val selectedFile: File,
        val projectRoot: File,
    )

    abstract fun processSelection(state: SelectionState, config: T?): Array<File>

    final override fun handle(e: AnActionEvent) {
        val config = getConfig(e.project)
        val virtualFile = UITools.getSelectedFile(e) ?: UITools.getSelectedFolder(e) ?: return
        val project = e.project ?: return
        val projectRoot = File(project.basePath!!).toPath()
        Thread {
            try {
                UITools.redoableTask(e) {
                    UITools.run(e.project, templateText!!, true) {
                        val newFiles = try {
                            processSelection(
                                SelectionState(
                                    selectedFile = virtualFile.toNioPath().toFile(),
                                    projectRoot = projectRoot.toFile(),
                                ), config
                            )
                        } finally {
                            if (it.isCanceled) throw InterruptedException()
                        }
                        UITools.writeableFn(e) {
                            val files = newFiles.map { file ->
                                val localFileSystem = LocalFileSystem.getInstance()
                                localFileSystem.findFileByIoFile(file.parentFile)?.refresh(false, true)
                                val generatedFile = localFileSystem.findFileByIoFile(file)
                                if (generatedFile == null) {
                                    log.warn("Generated file not found: ${file.path}")
                                } else {
                                    generatedFile.refresh(false, false)
                                    FileEditorManager.getInstance(project).openFile(generatedFile, true)
                                }
                                generatedFile
                            }.filter { it != null }.toTypedArray<VirtualFile?>()
                            Runnable {
                                files.forEach { it?.delete(this@FileContextAction) }
                            }
                        }
                    }
                }
            } catch (e: Throwable) {
                UITools.error(log, "Error in ${javaClass.simpleName}", e)
            }
        }.start()
    }

    open fun getConfig(project: Project?): T? = null

    private var isDevAction = false
    override fun isEnabled(event: AnActionEvent): Boolean {
        if (!super.isEnabled(event)) return false
        if (UITools.isSanctioned()) return false
        if(isDevAction && !AppSettingsState.instance.devActions) return false
        val virtualFile = UITools.getSelectedFile(event) ?: UITools.getSelectedFolder(event) ?: return false
        return if (virtualFile.isDirectory) supportsFolders else supportsFiles
    }

    companion object {
        private val log = LoggerFactory.getLogger(FileContextAction::class.java)
    }

}