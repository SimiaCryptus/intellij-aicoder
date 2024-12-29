package com.simiacryptus.aicoder.util

import aicoder.actions.agent.toFile
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.fileEditor.FileDocumentManager
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiManager
import java.util.*

object LanguageUtils {

  fun getComputerLanguage(e: AnActionEvent): ComputerLanguage? {
    return ApplicationManager.getApplication().runReadAction<ComputerLanguage?> {
      val editor = e.getData(CommonDataKeys.EDITOR) ?: return@runReadAction null
      val virtualFile: VirtualFile = FileDocumentManager.getInstance().getFile(editor.document) ?: return@runReadAction null
      val file = PsiManager.getInstance(e.project!!).findFile(virtualFile)?.virtualFile?.toFile ?: return@runReadAction null
      val extension = if (file.extension != null) file.extension.lowercase(Locale.getDefault()) else ""
      return@runReadAction ComputerLanguage.findByExtension(extension)
    }
  }
}
