package com.simiacryptus.aicoder.util.psi

import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.util.TextRange
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiElementVisitor
import com.simiacryptus.util.StringUtil
import java.util.*
import java.util.concurrent.atomic.AtomicReference
import java.util.stream.Collectors
import java.util.stream.Stream

object PsiUtil {
  val ELEMENTS_CODE = arrayOf<CharSequence>(
    "Method", "Field", "Class", "Function", "CssBlock", "FunctionDefinition",
    "Property", "Interface", "Enum", "Constructor", "Parameter", "Variable"
  )
  val ELEMENTS_COMMENTS = arrayOf<CharSequence>(
    "Comment", "DocComment", "LineComment", "BlockComment", "JavadocComment"
  )

  // Common block types used in multiple places
  private val BLOCK_TYPES = arrayOf(
    "CodeBlock", "BlockExpr", "Block", "BlockExpression", "StatementList", "BlockFields",
    "ClassBody", "MethodBody", "FunctionBody", "TryBlock", "CatchBlock", "FinallyBlock"
  )

  /**
   * Gets the name of an element (class, method, field etc)
   */
  fun getName(element: PsiElement): String? {
    if (!matchesType(element, *ELEMENTS_CODE)) return null
    val declaration = getDeclaration(element)
    // Extract name from declaration based on element type
    return when {
      matchesType(element, "Class", "Interface", "Enum") ->
        declaration.substringAfter("class ")
          .substringAfter("interface ")
          .substringAfter("enum ")
          .substringBefore("<")
          .substringBefore(" ")
          .trim()

      matchesType(element, "Method", "Function") ->
        declaration.substringAfter(" ")
          .substringBefore("(")
          .trim()

      matchesType(element, "Field", "Variable") ->
        declaration.substringAfterLast(" ")
          .substringBefore("=")
          .trim()

      else -> null
    }
  }

  fun getAll(element: PsiElement, vararg types: CharSequence): List<PsiElement> {
    val elements: MutableList<PsiElement> = ArrayList()
    val visitor = AtomicReference<PsiElementVisitor>()
    visitor.set(object : PsiElementVisitor() {
      override fun visitElement(element: PsiElement) {
        if (matchesType(element, *types)) {
          elements.add(element)
        }
        element.acceptChildren(visitor.get())
        super.visitElement(element)
      }
    })
    element.accept(visitor.get())
    return elements
  }

  fun getSmallestIntersecting(
    element: PsiElement, selectionStart: Int, selectionEnd: Int, vararg types: CharSequence
  ): PsiElement? {
    val smallest = AtomicReference<PsiElement?>(null)
    val visitor = AtomicReference<PsiElementVisitor>()
    visitor.set(object : PsiElementVisitor() {
      override fun visitElement(element: PsiElement) {
        val textRange = element.textRange
        if (intersects(TextRange(selectionStart, selectionEnd), textRange)) {
          if (matchesType(element, *types)) {
            smallest.updateAndGet { s: PsiElement? ->
              if ((s?.text?.length ?: Int.MAX_VALUE) < element.text.length) s else element
            }
          }
        }
        super.visitElement(element)
        element.acceptChildren(visitor.get())
      }
    })
    element.accept(visitor.get())
    return smallest.get()
  }

  private fun within(textRange: TextRange, vararg offset: Int): Boolean = offset.any { it in textRange.startOffset..textRange.endOffset }
  private fun intersects(a: TextRange, b: TextRange): Boolean {
    return within(a, b.startOffset, b.endOffset) || within(b, a.startOffset, a.endOffset)
  }

  fun matchesType(element: PsiElement, vararg types: CharSequence): Boolean {
    return matchesType(element.javaClass.simpleName, types)
  }

  fun matchesType(simpleName: CharSequence, types: Array<out CharSequence>): Boolean {
    var simpleName1 = simpleName
    simpleName1 = StringUtil.stripSuffix(simpleName1, "Impl")
    simpleName1 = StringUtil.stripPrefix(simpleName1, "Psi")
    val str = simpleName1.toString()
    return Stream.of(*types).map { s: CharSequence? ->
      StringUtil.stripSuffix(
        s!!, "Impl"
      )
    }.map { s: String? ->
      StringUtil.stripPrefix(
        s!!, "Psi"
      )
    }.anyMatch { t: CharSequence -> str.endsWith(t.toString()) }
  }

  fun getFirstBlock(element: PsiElement, vararg blockType: CharSequence): PsiElement? {
    val children = element.children
    if (children.isEmpty()) return null
    val first = children[0]
    return if (matchesType(first, *blockType)) first else null
  }

  fun getLargestBlock(element: PsiElement, vararg blockType: CharSequence): PsiElement? {
    val largest = AtomicReference<PsiElement?>(null)
    val visitor = AtomicReference<PsiElementVisitor>()
    visitor.set(object : PsiElementVisitor() {
      override fun visitElement(element: PsiElement) {
        if (matchesType(element, *blockType)) {
          largest.updateAndGet { s: PsiElement? -> if (s != null && s.text.length > element.text.length) s else element }
          super.visitElement(element)
        } else {
          super.visitElement(element)
        }
        element.acceptChildren(visitor.get())
      }
    })
    element.accept(visitor.get())
    return largest.get()
  }

  fun printTree(element: PsiElement): String {
    val builder = StringBuilder()
    printTree(element, builder, 0)
    return builder.toString()
  }

  private fun printTree(element: PsiElement, builder: StringBuilder, level: Int) {
    builder.append("  ".repeat(0.coerceAtLeast(level)))
    val elementClass: Class<out PsiElement> = element.javaClass
    val simpleName = getName(elementClass)
    builder.append(simpleName).append("    ").append(element.text.replace("\n".toRegex(), "\\\\n"))
    builder.append("\n")
    for (child in element.children) {
      printTree(child, builder, level + 1)
    }
  }

  private fun getName(elementClass: Class<*>): String {
    var elementClassVar = elementClass
    val stringBuilder = StringBuilder()
    val interfaces = getInterfaces(elementClassVar)
    while (elementClassVar != Any::class.java) {
      if (stringBuilder.isNotEmpty()) stringBuilder.append("/")
      stringBuilder.append(elementClassVar.simpleName)
      elementClassVar = elementClassVar.superclass
    }
    stringBuilder.append("[ ")
    stringBuilder.append(interfaces.stream().sorted().collect(Collectors.joining(",")))
    stringBuilder.append("]")
    return stringBuilder.toString()
  }

  fun getInterfaces(elementClass: Class<*>): Set<String> {
    val strings = Arrays.stream(elementClass.interfaces).map { obj: Class<*> -> obj.simpleName }.collect(
      Collectors.toCollection { HashSet() })
    if (elementClass.superclass != Any::class.java) strings.addAll(getInterfaces(elementClass.superclass))
    return strings
  }

  fun getLargestContainedEntity(element: PsiElement?, selectionStart: Int, selectionEnd: Int): PsiElement? {
    if (null == element) return null
    val textRange = element.textRange
    if (textRange.startOffset >= selectionStart && textRange.endOffset <= selectionEnd) return element
    var largestContainedChild: PsiElement? = null
    for (child in element.children) {
      val entity = getLargestContainedEntity(child, selectionStart, selectionEnd)
      if (null != entity) {
        if (largestContainedChild == null || largestContainedChild.textRange.length < entity.textRange.length) {
          largestContainedChild = entity
        }
      }
    }
    return largestContainedChild
  }

  fun getLargestContainedEntity(e: AnActionEvent): PsiElement? {
    val caret = e.getData(CommonDataKeys.CARET) ?: return null
    var psiFile: PsiElement? = e.getData(CommonDataKeys.PSI_FILE) ?: return null
    val selectionStart = caret.selectionStart
    val selectionEnd = caret.selectionEnd
    val largestContainedEntity = getLargestContainedEntity(psiFile, selectionStart, selectionEnd)
    if (largestContainedEntity != null) psiFile = largestContainedEntity
    return psiFile
  }

  fun getSmallestContainingEntity(element: PsiElement?, selectionStart: Int, selectionEnd: Int, minSize: Int = 0): PsiElement? {
    if (null == element) {
      return null
    }
    for (child in element.children) {
      val entity = getSmallestContainingEntity(child, selectionStart, selectionEnd, minSize)
      if (null != entity) {
        return entity
      }
    }
    val textRange = element.textRange
    if (textRange.startOffset <= selectionStart) {
      if (textRange.endOffset >= selectionEnd) {
        if (element.text.length >= minSize) {
          return element
        }
      }
    }
    return null
  }

  fun getCodeElement(
    psiFile: PsiElement?, selectionStart: Int, selectionEnd: Int
  ) = getSmallestIntersecting(psiFile!!, selectionStart.toInt(), selectionEnd.toInt(), *ELEMENTS_CODE)

  fun getDeclaration(element: PsiElement): String {
    var declaration: CharSequence = element.text
    declaration = StringUtil.stripPrefix(declaration.toString().trim { it <= ' ' }, getDocComment(element).trim { it <= ' ' })
    declaration = StringUtil.stripSuffix(declaration.toString().trim { it <= ' ' }, getCode(element).trim { it <= ' ' })
    return declaration.toString().trim { it <= ' ' }
  }

  fun getCode(element: PsiElement): String {
    val codeBlock = getLargestBlock(element, *BLOCK_TYPES)
    var code = ""
    if (null != codeBlock) {
      code = codeBlock.text
    }
    return code
  }

  fun getDocComment(element: PsiElement): String {
    var docComment = getLargestBlock(element, *ELEMENTS_COMMENTS)
    if (null == docComment) docComment = getFirstBlock(element, *ELEMENTS_COMMENTS)
    return docComment?.text?.trim() ?: ""
  }

}