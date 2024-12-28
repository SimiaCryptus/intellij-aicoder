package com.simiacryptus.aicoder.util.psi

import com.intellij.psi.PsiElement
import com.intellij.psi.PsiElementVisitor
import com.intellij.psi.PsiFile
import java.util.concurrent.atomic.AtomicReference

abstract class PsiVisitorBase {
  fun build(psiFile: PsiFile) {
    val visitor = AtomicReference<PsiElementVisitor>()
    visitor.set(object : PsiElementVisitor() {
      override fun visitElement(element: PsiElement) {
        visit(element, visitor.get())
        super.visitElement(element)
      }
    })
    psiFile.accept(visitor.get())
  }

  protected abstract fun visit(element: PsiElement, self: PsiElementVisitor)
}
