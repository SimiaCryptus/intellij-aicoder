﻿package com.github.simiacryptus.aicoder.actions.dev

import com.github.simiacryptus.aicoder.actions.BaseAction
import com.github.simiacryptus.aicoder.config.AppSettingsState
import com.github.simiacryptus.aicoder.util.ComputerLanguage
import com.github.simiacryptus.aicoder.util.UITools
import com.github.simiacryptus.aicoder.util.psi.PsiTranslationTree
import com.intellij.openapi.actionSystem.ActionGroup
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.command.WriteCommandAction
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import org.slf4j.LoggerFactory
import java.io.IOException
import java.util.concurrent.atomic.AtomicReference

class ConvertFileTo : ActionGroup() {
    private var supportedLanguages = listOf(
        ComputerLanguage.Java,
        ComputerLanguage.JavaScript,
        ComputerLanguage.Scala,
        ComputerLanguage.Kotlin,
        ComputerLanguage.Go,
        ComputerLanguage.Rust,
        ComputerLanguage.Python
    )

    override fun update(e: AnActionEvent) {
        e.presentation.isEnabledAndVisible = isEnabled(e)
        super.update(e)
    }

    private fun isEnabled(e: AnActionEvent): Boolean {
        if (UITools.isSanctioned()) return false
        if(!AppSettingsState.instance.devActions) return false
        val computerLanguage = ComputerLanguage.getComputerLanguage(e) ?: return false
        if (computerLanguage == ComputerLanguage.Text) return false
        return supportedLanguages.contains(computerLanguage)
    }

    override fun getChildren(e: AnActionEvent?): Array<AnAction> {
        if (null == e) return arrayOf()
        val computerLanguage = ComputerLanguage.getComputerLanguage(e)
        val actions = ArrayList<AnAction>()
        for (language in supportedLanguages) {
            if (computerLanguage == language) continue
            actions.add(ConvertFileToLanguage(language))
        }
        return actions.toArray(arrayOf())
    }


    class ConvertFileToLanguage(private val targetLanguage: ComputerLanguage) : BaseAction(
        targetLanguage.name
    ) {
        override fun handle(e: AnActionEvent) {
            val sourceLanguage = ComputerLanguage.getComputerLanguage(e)
            val indent = UITools.getIndent(e.getData(CommonDataKeys.CARET))
            val virtualFile = e.getData(CommonDataKeys.VIRTUAL_FILE) ?: return
            val psiFile = e.getData(CommonDataKeys.PSI_FILE) ?: return
            val project = e.project!!
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
                        UITools.error(log, "Error translating", e)
                    }
                }
            }.start()
        }

    }

    companion object {

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

        private val log = LoggerFactory.getLogger(ConvertFileTo::class.java)
    }
}
