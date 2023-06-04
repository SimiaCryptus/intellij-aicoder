package com.github.simiacryptus.aicoder.actions.code

import com.github.simiacryptus.aicoder.actions.BaseAction
import com.github.simiacryptus.aicoder.util.ComputerLanguage
import com.github.simiacryptus.aicoder.util.UITools
import com.github.simiacryptus.aicoder.util.UITools.getIndent
import com.github.simiacryptus.aicoder.util.psi.PsiTranslationTree
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.command.WriteCommandAction
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import java.io.IOException
import java.util.concurrent.atomic.AtomicReference

class ConvertFileToLanguage(private val targetLanguage: ComputerLanguage) : BaseAction(
    targetLanguage.name
) {
    override fun actionPerformed(event: AnActionEvent) {
        val sourceLanguage = ComputerLanguage.getComputerLanguage(event)
        val indent = getIndent(event.getData(CommonDataKeys.CARET))
        val virtualFile = event.getData(CommonDataKeys.VIRTUAL_FILE) ?: return
        val psiFile = event.getData(CommonDataKeys.PSI_FILE) ?: return
        val project = event.project!!
        Thread {
            UITools.run(project, "Converting to " + targetLanguage.name) {
                try {
                    val skeleton = PsiTranslationTree.parseFile(
                        psiFile, sourceLanguage!!, targetLanguage
                    )
                    skeleton.translateTree(project, indent)
                    val content = skeleton.getTranslatedDocument()
                    val newFile = getNewFile(project, virtualFile, targetLanguage)
                    write(project, newFile, content.toString())
                } catch (e: Throwable) {
                    log.error("Error translating", e)
                }
            }
        }.start()
    }

    companion object {
        private val log = Logger.getInstance(
            ConvertFileToLanguage::class.java
        )

        fun getNewFile(project: Project?, file: VirtualFile, language: ComputerLanguage): VirtualFile {
            val newFileRef = AtomicReference<VirtualFile>()
            WriteCommandAction.runWriteCommandAction(project) {
                try {
                    val newFileName = file.nameWithoutExtension + "." + language.extensions[0]
                    newFileRef.set(file.parent.createChildData(file, newFileName))
                } catch (ex: IOException) {
                    throw RuntimeException(ex)
                }
            }
            return newFileRef.get()
        }

        fun write(project: Project?, newFile: VirtualFile, content: String) {
            WriteCommandAction.runWriteCommandAction(project) {
                try {
                    newFile.setBinaryContent(content.toByteArray())
                } catch (e: IOException) {
                    throw RuntimeException(e)
                }
            }
        }
    }
}