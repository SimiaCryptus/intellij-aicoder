package com.github.simiacryptus.aicoder.util.psi

import com.github.simiacryptus.aicoder.util.StringTools
import com.intellij.lang.Language
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.command.WriteCommandAction
import com.intellij.openapi.editor.Caret
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.TextRange
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiElementVisitor
import com.intellij.psi.PsiFile
import com.intellij.psi.PsiFileFactory
import java.util.*
import java.util.concurrent.atomic.AtomicReference
import java.util.function.Supplier
import java.util.stream.Collectors
import java.util.stream.Stream

object PsiUtil {
    val ELEMENTS_CODE = arrayOf<CharSequence>(
        "Method",
        "Field",
        "Class",
        "Function",
        "CssBlock",
        "FunctionDefinition"
    )
    val ELEMENTS_COMMENTS = arrayOf<CharSequence>(
        "Comment"
    )

    fun getLargestIntersectingComment(element: PsiElement, selectionStart: Int, selectionEnd: Int): PsiElement? {
        return getLargestIntersecting(element, selectionStart, selectionEnd, *ELEMENTS_COMMENTS)
    }

    /**
     * This method is used to get the largest element that intersects with the given selection range.
     *
     * @param element        The element to search within.
     * @param selectionStart The start of the selection range.
     * @param selectionEnd   The end of the selection range.
     * @param types          The types of elements to search for.
     * @return The largest element that intersects with the given selection range.
     */
    fun getLargestIntersecting(
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
                        largest.updateAndGet { s: PsiElement? -> if (s?.text?.length ?: 0 > element.text.length) s else element }
                    }
                }
                super.visitElement(element)
                element.acceptChildren(visitor.get())
            }
        })
        element.accept(visitor.get())
        return largest.get()
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

    /**
     * This method is used to get the smallest intersecting entity from a given PsiElement.
     *
     * @param element        The PsiElement from which the smallest intersecting entity is to be retrieved.
     * @param selectionStart The start of the selection.
     * @param selectionEnd   The end of the selection.
     * @param types          The types of the elements to be retrieved.
     * @return The smallest intersecting entity from the given PsiElement.
     */
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
                if (within(textRange, selectionStart, selectionEnd)) {
                    if (matchesType(element, *types)) {
                        largest.updateAndGet { s: PsiElement? -> if ((s?.text?.length ?: Int.MAX_VALUE) < element.text.length) s else element }
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

    fun getAllIntersecting(
        element: PsiElement,
        selectionStart: Int,
        selectionEnd: Int,
        vararg types: CharSequence
    ): List<PsiElement> {
        val elements: MutableList<PsiElement> = ArrayList()
        val visitor = AtomicReference<PsiElementVisitor>()
        visitor.set(object : PsiElementVisitor() {
            override fun visitElement(element: PsiElement) {
                val textRange = element.textRange
                if (matchesType(element, *types)) {
                    if (within(textRange, selectionStart, selectionEnd)) {
                        elements.add(element)
                    }
                }
                super.visitElement(element)
                element.acceptChildren(visitor.get())
            }
        })
        element.accept(visitor.get())
        return elements
    }

    private fun within(textRange: TextRange, vararg offset: Int) : Boolean =
        (textRange.startOffset <= offset.maxOrNull()?:0) && (textRange.endOffset > offset.minOrNull()?:0)


    fun matchesType(element: PsiElement, vararg types: CharSequence): Boolean {
        var simpleName: CharSequence = element.javaClass.simpleName
        simpleName = StringTools.stripSuffix(simpleName, "Impl")
        simpleName = StringTools.stripPrefix(simpleName, "Psi")
        val str = simpleName.toString()
        return Stream.of(*types)
            .map { s: CharSequence? ->
                StringTools.stripSuffix(
                    s!!, "Impl"
                )
            }
            .map { s: String? ->
                StringTools.stripPrefix(
                    s!!, "Psi"
                )
            }
            .anyMatch { t: CharSequence -> str.endsWith(t.toString()) }
    }

    fun getFirstBlock(element: PsiElement, vararg blockType: CharSequence): PsiElement? {
        val children = element.children
        if (0 == children.size) return null
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
        builder.append("  ".repeat(Math.max(0, level)))
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
            if (stringBuilder.length > 0) stringBuilder.append("/")
            stringBuilder.append(elementClass.simpleName)
            elementClass = elementClass.superclass
        }
        stringBuilder.append("[ ")
        stringBuilder.append(interfaces.stream().sorted().collect(Collectors.joining(",")))
        stringBuilder.append("]")
        return stringBuilder.toString()
    }

    private fun getInterfaces(elementClass: Class<*>): Set<String> {
        val strings = Arrays.stream(elementClass.interfaces).map { obj: Class<*> -> obj.simpleName }
            .collect(
                Collectors.toCollection(
                    Supplier { HashSet() })
            )
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

    fun getSmallestIntersectingMajorCodeElement(psiFile: PsiFile, caret: Caret): PsiElement? {
        return getSmallestIntersecting(psiFile, caret.selectionStart, caret.selectionEnd, *ELEMENTS_CODE)
    }

    fun getDeclaration(element: PsiElement): String {
        var declaration: CharSequence = element.text
        declaration = StringTools.stripPrefix(declaration.toString().trim { it <= ' ' },
            getDocComment(element).trim { it <= ' ' })
        declaration =
            StringTools.stripSuffix(declaration.toString().trim { it <= ' ' }, getCode(element).trim { it <= ' ' })
        return declaration.toString().trim { it <= ' ' }
    }

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

    fun getDocComment(element: PsiElement): String {
        var docComment = getLargestBlock(element, "DocComment")
        if (null == docComment) docComment = getFirstBlock(element, "Comment")
        var prefix = ""
        if (null != docComment) {
            prefix = docComment.text.trim { it <= ' ' }
        }
        return prefix
    }

    /**
     * Parses a file from a given text and language.
     *
     * @param project  The project to which the file belongs.
     * @param language The language of the file.
     * @param text     The text of the file.
     * @return The parsed file.
     */
    fun parseFile(project: Project?, language: Language?, text: CharSequence?): PsiFile {
        val fileFromText = AtomicReference<PsiFile>()
        WriteCommandAction.runWriteCommandAction(
            project
        ) {
            fileFromText.set(
                PsiFileFactory.getInstance(project).createFileFromText(
                    language!!, text!!
                )
            )
        }
        return fileFromText.get()
    }

    fun getPsiFile(e: AnActionEvent) : PsiFile? {
        return e.getData(CommonDataKeys.PSI_FILE)
    }
}


