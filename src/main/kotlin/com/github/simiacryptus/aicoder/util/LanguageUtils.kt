package com.github.simiacryptus.aicoder.util

import com.github.simiacryptus.aicoder.actions.generic.toFile
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.fileEditor.FileDocumentManager
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiManager
import java.util.*

object LanguageUtils {

    fun isLanguageSupported(computerLanguage: ComputerLanguage?): Boolean {
        return computerLanguage != null
    }

    fun getComputerLanguage(e: AnActionEvent): ComputerLanguage? {
        val editor = e.getData(CommonDataKeys.EDITOR) ?: return null
        val virtualFile: VirtualFile = FileDocumentManager.getInstance().getFile(editor.document) ?: return null
        val file = PsiManager.getInstance(e.project!!).findFile(virtualFile)?.virtualFile?.toFile ?: return null
        val extension = if (file.extension != null) file.extension!!.lowercase(Locale.getDefault()) else ""
        return ComputerLanguage.findByExtension(extension)
    }
}
