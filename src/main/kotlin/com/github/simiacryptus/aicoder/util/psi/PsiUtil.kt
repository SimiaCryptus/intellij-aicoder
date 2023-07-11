package com.github.simiacryptus.aicoder.util.psi

import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.editor.Caret
import com.intellij.openapi.util.TextRange
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiElementVisitor
import com.intellij.psi.PsiFile
import com.simiacryptus.util.StringUtil
import java.util.*
import java.util.concurrent.atomic.AtomicReference
import java.util.stream.Collectors
import java.util.stream.Stream

object PsiUtil {
    private val ELEMENTS_CODE = arrayOf<CharSequence>(
        "Method",
        "Field",
        "Class",
        "Function",
        "CssBlock",
        "FunctionDefinition"
    )
    private val ELEMENTS_COMMENTS = arrayOf<CharSequence>(
        "Comment"
    )

    @JvmStatic
    fun getLargestIntersectingComment(element: PsiElement, selectionStart: Int, selectionEnd: Int): PsiElement? {
        return getLargestIntersecting(element, selectionStart, selectionEnd, *ELEMENTS_COMMENTS)
    }

    fun getAll(element: PsiElement, vararg types: CharSequence): List<PsiElement> {
        val elements: MutableList<PsiElement> = ArrayList()
        val visitor = AtomicReference<PsiElementVisitor>()
        visitor.set(object : PsiElementVisitor() {
            override fun visitElement(element: PsiElement) {
                if (matchesType(element, *types)) {
                    elements.add(element)
                } else {
                    element.acceptChildren(visitor.get())
                }
                super.visitElement(element)
            }
        })
        element.accept(visitor.get())
        return elements
    }

    @JvmStatic
    private fun getLargestIntersecting(
        element: PsiElement,
        selectionStart: Int,
        selectionEnd: Int,
        vararg types: CharSequence
    ): PsiElement? {
        val largest = AtomicReference<PsiElement?>(null)
        val visitor = AtomicReference<PsiElementVisitor>()
        visitor.set(object : PsiElementVisitor() {
            override fun visitElement(element: PsiElement) {
                val textRange = element.textRange
                val within =
                    within(
                        textRange,
                        selectionStart
                    ) && textRange.startOffset <= selectionEnd && textRange.endOffset + 1 >= selectionEnd
                if (matchesType(element, *types)) {
                    if (within) {
                        largest.updateAndGet { s: PsiElement? ->
                            if ((s?.text?.length ?: 0) > element.text.length) s else element
                        }
                    }
                }
                super.visitElement(element)
                element.acceptChildren(visitor.get())
            }
        })
        element.accept(visitor.get())
        return largest.get()
    }

    @JvmStatic
    fun getSmallestIntersecting(
        element: PsiElement,
        selectionStart: Int,
        selectionEnd: Int,
        vararg types: CharSequence
    ): PsiElement? {
        val largest = AtomicReference<PsiElement?>(null)
        val visitor = AtomicReference<PsiElementVisitor>()
        visitor.set(object : PsiElementVisitor() {
            override fun visitElement(element: PsiElement) {
                val textRange = element.textRange
                if (intersects(TextRange(selectionStart, selectionEnd), textRange)) {
                    if (matchesType(element, *types)) {
                        largest.updateAndGet { s: PsiElement? ->
                            if ((s?.text?.length ?: Int.MAX_VALUE) < element.text.length) s else element
                        }
                    }
                }
                //System.out.printf("%s : %s%n", simpleName, element.getText());
                super.visitElement(element)
                element.acceptChildren(visitor.get())
            }
        })
        element.accept(visitor.get())
        return largest.get()
    }

    @JvmStatic
    private fun within(textRange: TextRange, vararg offset: Int): Boolean =
        offset.any { it in textRange.startOffset..textRange.endOffset }

    private fun intersects(a: TextRange, b: TextRange): Boolean {
        return within(a, b.startOffset, b.endOffset) || within(b, a.startOffset, a.endOffset)
    }


    fun matchesType(element: PsiElement, vararg types: CharSequence): Boolean {
        var simpleName: CharSequence = element.javaClass.simpleName
        simpleName = StringUtil.stripSuffix(simpleName, "Impl")
        simpleName = StringUtil.stripPrefix(simpleName, "Psi")
        val str = simpleName.toString()
        return Stream.of(*types)
            .map { s: CharSequence? ->
                StringUtil.stripSuffix(
                    s!!, "Impl"
                )
            }
            .map { s: String? ->
                StringUtil.stripPrefix(
                    s!!, "Psi"
                )
            }
            .anyMatch { t: CharSequence -> str.endsWith(t.toString()) }
    }

    @JvmStatic
    private fun getFirstBlock(element: PsiElement, vararg blockType: CharSequence): PsiElement? {
        val children = element.children
        if (children.isEmpty()) return null
        val first = children[0]
        return if (matchesType(first, *blockType)) first else null
    }

    @JvmStatic
    private fun getLargestBlock(element: PsiElement, vararg blockType: CharSequence): PsiElement? {
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
        var elementClass = elementClass
        val stringBuilder = StringBuilder()
        val interfaces = getInterfaces(elementClass)
        while (elementClass != Any::class.java) {
            if (stringBuilder.isNotEmpty()) stringBuilder.append("/")
            stringBuilder.append(elementClass.simpleName)
            elementClass = elementClass.superclass
        }
        stringBuilder.append("[ ")
        stringBuilder.append(interfaces.stream().sorted().collect(Collectors.joining(",")))
        stringBuilder.append("]")
        return stringBuilder.toString()
    }

    @JvmStatic
    private fun getInterfaces(elementClass: Class<*>): Set<String> {
        val strings = Arrays.stream(elementClass.interfaces).map { obj: Class<*> -> obj.simpleName }
            .collect(
                Collectors.toCollection { HashSet() }
            )
        if (elementClass.superclass != Any::class.java) strings.addAll(getInterfaces(elementClass.superclass))
        return strings
    }

    @JvmStatic
    private fun getLargestContainedEntity(element: PsiElement?, selectionStart: Int, selectionEnd: Int): PsiElement? {
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

    @JvmStatic
    fun getLargestContainedEntity(e: AnActionEvent): PsiElement? {
        val caret = e.getData(CommonDataKeys.CARET)
            ?: return null
        var psiFile: PsiElement? = e.getData(CommonDataKeys.PSI_FILE)
            ?: return null
        val selectionStart = caret.selectionStart
        val selectionEnd = caret.selectionEnd
        val largestContainedEntity = getLargestContainedEntity(psiFile, selectionStart, selectionEnd)
        if (largestContainedEntity != null) psiFile = largestContainedEntity
        return psiFile
    }

    @JvmStatic
    fun getSmallestIntersectingMajorCodeElement(psiFile: PsiFile, caret: Caret) =
        getCodeElement(psiFile, caret.selectionStart, caret.selectionEnd)

    @JvmStatic
    fun getCodeElement(
        psiFile: PsiElement?,
        selectionStart: Int,
        selectionEnd: Int
    ) = getSmallestIntersecting(psiFile!!, selectionStart?.toInt() ?: 0, selectionEnd?.toInt() ?: 0, *ELEMENTS_CODE)

    @JvmStatic
    fun getDeclaration(element: PsiElement): String {
        var declaration: CharSequence = element.text
        declaration = StringUtil.stripPrefix(declaration.toString().trim { it <= ' ' },
            getDocComment(element).trim { it <= ' ' })
        declaration =
            StringUtil.stripSuffix(declaration.toString().trim { it <= ' ' }, getCode(element).trim { it <= ' ' })
        return declaration.toString().trim { it <= ' ' }
    }

    @JvmStatic
    fun getCode(element: PsiElement): String {
        val codeBlock = getLargestBlock(
            element,
            "CodeBlock",
            "BlockExpr",
            "Block",
            "BlockExpression",
            "StatementList",
            "BlockFields"
        )
        var code = ""
        if (null != codeBlock) {
            code = codeBlock.text
        }
        return code
    }

    @JvmStatic
    fun getDocComment(element: PsiElement): String {
        var docComment = getLargestBlock(element, "DocComment")
        if (null == docComment) docComment = getFirstBlock(element, "Comment")
        var prefix = ""
        if (null != docComment) {
            prefix = docComment.text.trim { it <= ' ' }
        }
        return prefix
    }

}



