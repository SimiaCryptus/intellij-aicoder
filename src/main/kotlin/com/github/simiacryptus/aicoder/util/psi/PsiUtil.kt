package com.github.simiacryptus.aicoder.util.psi

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

  // Expression types for parsing code
  private val EXPRESSION_TYPES = arrayOf(
    "Expression", "BinaryExpression", "CallExpression", "LiteralExpression",
    "ReferenceExpression", "MethodCallExpression", "LambdaExpression"
  )

  // Import and package related types
  private val IMPORT_TYPES = arrayOf(
    "ImportStatement", "ImportList", "PackageStatement"
  )

  // Common block types used in multiple places
  private val BLOCK_TYPES = arrayOf(
    "CodeBlock", "BlockExpr", "Block", "BlockExpression", "StatementList", "BlockFields",
    "ClassBody", "MethodBody", "FunctionBody", "TryBlock", "CatchBlock", "FinallyBlock"
  )

  // Additional element types for specific use cases
  private val STATEMENT_TYPES = arrayOf(
    "Statement", "ExpressionStatement", "DeclarationStatement", "ReturnStatement",
    "IfStatement", "WhileStatement", "ForStatement", "DoWhileStatement",
    "SwitchStatement", "BreakStatement", "ContinueStatement", "ThrowStatement",
    "TryStatement", "CatchStatement", "FinallyStatement", "AssertStatement",
    "YieldStatement", "SynchronizedStatement"
  )

  // Annotation related types
  private val ANNOTATION_TYPES = arrayOf(
    "Annotation", "AnnotationMethod", "AnnotationParameter", "ModifierList"
  )

  // Generic/Template related types
  private val GENERIC_TYPES = arrayOf(
    "TypeParameter", "TypeArgument", "WildcardType", "GenericType"
  )

  /**
   * Gets all expressions within the given element
   */
  fun getAllExpressions(element: PsiElement): List<PsiElement> {
    return getAll(element, *EXPRESSION_TYPES)
  }

  /**
   * Gets all annotations on an element
   */
  fun getAnnotations(element: PsiElement): List<PsiElement> {
    return getAll(element, *ANNOTATION_TYPES)
  }

  /**
   * Gets all type parameters/generic arguments on an element
   */
  fun getGenericParameters(element: PsiElement): List<PsiElement> {
    return getAll(element, *GENERIC_TYPES)
  }

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

  /**
   * Gets all import statements within the given element
   */
  fun getAllImports(element: PsiElement): List<PsiElement> {
    return getAll(element, *IMPORT_TYPES)
  }

  /**
   * Gets the package statement for the file containing this element
   */
  fun getPackageStatement(element: PsiElement): PsiElement? {
    var current = element
    while (current.parent != null) {
      current = current.parent
    }
    return getAll(current, "PackageStatement").firstOrNull()
  }

  /**
   * Gets the full package name for the file containing this element
   */
  fun getPackageName(element: PsiElement): String? {
    val pkg = getPackageStatement(element) ?: return null
    return pkg.text.substringAfter("package").trim()
  }

  /**
   * Gets all method/function parameters within the given element
   */
  fun getParameters(element: PsiElement): List<PsiElement> {
    return getAll(element, "Parameter")
  }

  /**
   * Gets the return type of a method/function element if available
   */
  fun getReturnType(element: PsiElement): String? {
    if (!matchesType(element, "Method", "Function")) return null
    val declaration = getDeclaration(element)
    return when {
      declaration.contains("->") -> // Lambda syntax
        declaration.substringAfter("->").trim()

      declaration.contains(":") -> // Kotlin syntax
        declaration.substringAfter(":").substringBefore("{").trim()

      declaration.contains(" ") -> // Java syntax
        declaration.substringBefore(" ").trim()

      else -> null
    }
  }

  /**
   * Gets the throws/exception declarations for a method
   */
  fun getThrowsDeclarations(element: PsiElement): List<String> {
    if (!matchesType(element, "Method", "Function")) return emptyList()
    val declaration = getDeclaration(element)
    return if (declaration.contains("throws")) {
      declaration.substringAfter("throws")
        .substringBefore("{")
        .split(",")
        .map { it.trim() }
    } else emptyList()
  }

  /**
   * Gets the generic type parameters for a class/method
   */
  fun getGenericTypeParameters(element: PsiElement): List<String> {
    if (!matchesType(element, "Class", "Interface", "Method", "Function")) return emptyList()
    val declaration = getDeclaration(element)
    return if (declaration.contains("<")) {
      declaration.substringAfter("<")
        .substringBefore(">")
        .split(",")
        .map { it.trim() }
    } else emptyList()
  }

  /**
   * Gets all variable declarations within the given scope
   */
  fun getVariableDeclarations(element: PsiElement): List<PsiElement> {
    return getAll(element, "Variable", "Field")
  }

  /**
   * Gets the type of a variable/field declaration if available
   */
  fun getVariableType(element: PsiElement): String? {
    if (!matchesType(element, "Variable", "Field")) return null
    val declaration = getDeclaration(element)
    return declaration.substringBefore(" ").trim()
  }

  /**
   * Checks if the element represents a static member
   */
  fun isStatic(element: PsiElement): Boolean {
    val text = element.text.trim()
    return text.startsWith("static ") || text.contains(" static ")
  }

  /**
   * Gets the visibility modifier of an element (public, private, protected)
   */
  fun getVisibility(element: PsiElement): String {
    val text = element.text.trim()
    return when {
      text.startsWith("public ") || text.contains(" public ") -> "public"
      text.startsWith("private ") || text.contains(" private ") -> "private"
      text.startsWith("protected ") || text.contains(" protected ") -> "protected"
      text.startsWith("internal ") || text.contains(" internal ") -> "internal" // Kotlin visibility
      else -> "default"
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

  /**
   * Gets all statements within the given element
   */
  fun getAllStatements(element: PsiElement): List<PsiElement> {
    return getAll(element, *STATEMENT_TYPES)
  }

  /**
   * Gets the parent block containing this element
   */
  fun getParentBlock(element: PsiElement): PsiElement? {
    var current = element.parent
    while (current != null) {
      if (matchesType(current, *BLOCK_TYPES)) return current
      current = current.parent
    }
    return null
  }

  /**
   * Gets the nearest parent element of any of the specified types
   */
  fun getParentOfType(element: PsiElement, vararg types: CharSequence): PsiElement? {
    var current = element.parent
    while (current != null) {
      if (matchesType(current, *types)) return current
      current = current.parent
    }
    return null
  }

  /**
   * Checks if an element has a parent of any of the specified types
   */
  fun hasParentOfType(element: PsiElement, vararg types: CharSequence): Boolean {
    return getParentOfType(element, *types) != null
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

  /**
   * Gets the full qualified name of a class/method/field element
   */
  fun getQualifiedName(element: PsiElement): String? {
    if (!matchesType(element, *ELEMENTS_CODE)) return null
    val parts = mutableListOf<String>()
    var current: PsiElement? = element
    while (current != null) {
      if (matchesType(current, "Class", "Interface", "Enum")) {
        parts.add(0, current.text.substringBefore("{").trim())
      }
      current = current.parent
    }
    return if (parts.isEmpty()) null else parts.joinToString(".")
  }
}