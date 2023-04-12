package com.github.simiacryptus.aicoder.util.psi

import com.intellij.psi.PsiElement
import com.intellij.psi.PsiElementVisitor
import com.intellij.psi.PsiFile
import java.util.concurrent.atomic.AtomicReference

class PsiMarkdownContext(private val parent: PsiMarkdownContext?, val text: String, private val start: Int) {
    val children = ArrayList<PsiMarkdownContext>()

    val end: Int
        get() = (start + text.length).coerceAtLeast(children.stream().mapToInt { obj: PsiMarkdownContext -> obj.end }
            .max().orElse(0))

    fun headerLevel(): Int {
        return text.chars().takeWhile { i: Int -> i == '#'.code }.count().toInt()
    }

    fun init(psiFile: PsiFile, selectionStart: Int, selectionEnd: Int): PsiMarkdownContext {
        val visitor = AtomicReference<PsiElementVisitor>()
        visitor.set(object : PsiElementVisitor() {
            val indent: CharSequence = ""
            var section = this@PsiMarkdownContext

            @Suppress("unused")
            override fun visitElement(element: PsiElement) {
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
//                val within =
//                    textRangeStartOffset <= selectionStart && textRangeEndOffset > selectionStart && textRangeStartOffset <= selectionEnd && textRangeEndOffset > selectionEnd
                if (!isPrior && !isOverlap) return
                val simpleName: CharSequence = element.javaClass.simpleName
                if (simpleName == "MarkdownHeaderImpl") {
                    val content = PsiMarkdownContext(section, text.trim { it <= ' ' }, element.textOffset)
                    while (content.headerLevel() <= section.headerLevel() && section.parent != null) {
                        section = section.parent!!
                    }
                    section.children.add(content)
                    section = content
                } else if (simpleName == "MarkdownParagraphImpl" || simpleName == "MarkdownTableImpl") {
                    section.children.add(
                        PsiMarkdownContext(
                            section,
                            indent.toString() + text.trim { it <= ' ' },
                            element.textOffset
                        )
                    )
                } else if (simpleName == "MarkdownListImpl" || simpleName == "MarkdownListItemImpl") {
                    if (isPrior) {
                        section.children.add(
                            PsiMarkdownContext(
                                section,
                                indent.toString() + text.trim { it <= ' ' },
                                element.textOffset
                            )
                        )
                    } else {
                        element.acceptChildren(visitor.get())
                    }
                } else {
                    element.acceptChildren(visitor.get())
                }
                super.visitElement(element)
            }
        })
        psiFile.accept(visitor.get())
        return this
    }

    fun toString(toPoint: Int): String {
        val sb = ArrayList<String>()
        sb.add(text)
        if (end >= toPoint) {
            children.stream().filter { c: PsiMarkdownContext -> c.headerLevel() != 0 || c.end < toPoint }
                .map { psiMarkdownContext: PsiMarkdownContext ->
                    psiMarkdownContext.toString(
                        toPoint
                    )
                }.forEach { e: String -> sb.add(e) }
        }
        return sb.stream().reduce { l: String, r: String ->
            """
                $l
                $r
                """.trimIndent()
        }.get()
    }

    companion object {
        fun getContext(psiFile: PsiFile, selectionStart: Int, selectionEnd: Int): PsiMarkdownContext {
            return PsiMarkdownContext(null, "", 0).init(psiFile, selectionStart, selectionEnd)
        }
    }
}