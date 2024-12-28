package com.simiacryptus.aicoder.util.psi

import com.intellij.psi.PsiElement
import com.intellij.psi.PsiElementVisitor
import com.intellij.psi.PsiFile
import com.simiacryptus.aicoder.util.ComputerLanguage

class PsiClassContext(
  val text: String,
  private val isPrior: Boolean,
  private val isOverlap: Boolean,
  val language: ComputerLanguage
) {
  val children = ArrayList<PsiClassContext>()

  /**
   * This java code is initializing a PsiClassContext object. It is doing this by creating a PsiElementVisitor and using it to traverse the PsiFile.
   * It is checking the text range of each element and whether it is prior to, overlapping, or within the selectionStart and selectionEnd parameters.
   * Depending on the element, it is adding the text of the element to the PsiClassContext object, or recursively visiting its children.
   *
   * @param psiFile
   * @param selectionStart
   * @param selectionEnd
   * @return
   */
  fun init(psiFile: PsiFile?, selectionStart: Int, selectionEnd: Int): PsiClassContext {
    object : PsiVisitorBase() {
      var currentContext = this@PsiClassContext
      var indent = ""
      override fun visit(element: PsiElement, self: PsiElementVisitor) {
        val text = element.text
        val textRange = element.textRange
        val textRangeEndOffset = textRange.endOffset + 1
        val textRangeStartOffset = textRange.startOffset
        // Check if the element comes before the selection
        val isPrior = textRangeEndOffset < selectionStart
        // Check if the element overlaps with the selection
        val isOverlap =
          textRangeStartOffset in selectionStart..selectionEnd || textRangeEndOffset in selectionStart..selectionEnd || selectionStart in textRangeStartOffset..textRangeEndOffset || selectionEnd in textRangeStartOffset..textRangeEndOffset
        // Check if the element is within the selection
        val within =
          selectionStart in textRangeStartOffset until textRangeEndOffset && textRangeStartOffset <= selectionEnd && textRangeEndOffset > selectionEnd
        if (PsiUtil.matchesType(element, "ImportList")) {
          currentContext.children.add(PsiClassContext(text.trim { it <= ' ' }, isPrior, isOverlap, language))
        } else if (PsiUtil.matchesType(element, "Comment", "DocComment")) {
          if (within) {
            currentContext.children.add(
              PsiClassContext(
                indent + text.trim { it <= ' ' },
                false,
                true,
                language
              )
            )
          }
        } else if (PsiUtil.matchesType(element, "Field")) {
          processChildren(
            element,
            self,
            isPrior,
            isOverlap,
            indent + PsiUtil.getDeclaration(element).trim { it <= ' ' } + if (isOverlap) " {" else ";")
        } else if (PsiUtil.matchesType(element, "Method", "Function", "FunctionDefinition", "Constructor")) {
          val methodTerminator = when (language) {
            ComputerLanguage.Java -> " { /* ... */}"
            ComputerLanguage.Kotlin -> " { /* ... */}"
            ComputerLanguage.Scala -> " { /* ... */}"
            else -> ";"
          }
          processChildren(
            element,
            self,
            isPrior,
            isOverlap,
            indent + PsiUtil.getDeclaration(element)
              .trim { it <= ' ' } + (if (isOverlap) " {" else methodTerminator))
        } else if (PsiUtil.matchesType(element, "LocalVariable")) {
          currentContext.children.add(PsiClassContext(indent + text.trim { it <= ' ' } + ";",
            isPrior,
            isOverlap,
            language))
        } else if (PsiUtil.matchesType(element, "Class")) {
          processChildren(
            element,
            self,
            isPrior,
            isOverlap,
            indent + text.substring(0, text.indexOf('{')).trim { it <= ' ' } + " {")
          if (!isOverlap) {
            currentContext.children.add(PsiClassContext("}", isPrior, false, language))
          }
        } else if (!isOverlap && PsiUtil.matchesType(element, "CodeBlock", "ForStatement")) {
          // Skip
        } else {
          element.acceptChildren(self)
        }
      }

      private fun processChildren(
        element: PsiElement,
        self: PsiElementVisitor,
        isPrior: Boolean,
        isOverlap: Boolean,
        declarationText: String
      ): PsiClassContext {
        val newNode = PsiClassContext(declarationText, isPrior, isOverlap, language)
        currentContext.children.add(newNode)
        val prevclassBuffer = currentContext
        currentContext = newNode
        val prevIndent = indent
        indent += "  "
        element.acceptChildren(self)
        indent = prevIndent
        currentContext = prevclassBuffer
        return newNode
      }
    }.build(psiFile!!)
    return this
  }

  override fun toString(): String {
    val sb = ArrayList<String>()
    sb.add(text)
    children.stream().filter { x: PsiClassContext -> x.isPrior }.map { obj: PsiClassContext -> obj.toString() }
      .forEach { e: String -> sb.add(e) }
    children.stream().filter { x: PsiClassContext -> !x.isOverlap && !x.isPrior }
      .map { obj: PsiClassContext -> obj.toString() }
      .forEach { e: String -> sb.add(e) }
    children.stream().filter { x: PsiClassContext -> x.isOverlap }.map { obj: PsiClassContext -> obj.toString() }
      .forEach { e: String -> sb.add(e) }
    return sb.stream().reduce { l: String, r: String ->
      """
                $l
                $r
                """.trimIndent()
    }.get()
  }

  companion object {
    @JvmStatic
    fun getContext(
      psiFile: PsiFile?,
      selectionStart: Int,
      selectionEnd: Int,
      language: ComputerLanguage
    ): PsiClassContext {
      return PsiClassContext("", false, true, language).init(psiFile, selectionStart, selectionEnd)
    }

  }
}
