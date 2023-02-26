package com.github.simiacryptus.aicoder.util.psi

import com.github.simiacryptus.aicoder.openai.OpenAI_API.complete
import com.github.simiacryptus.aicoder.openai.OpenAI_API.pool
import com.github.simiacryptus.aicoder.config.AppSettingsState
import com.github.simiacryptus.aicoder.util.ComputerLanguage
import com.google.common.collect.Streams
import com.google.common.util.concurrent.Futures
import com.google.common.util.concurrent.ListenableFuture
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.NlsSafe
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiElementVisitor
import com.intellij.psi.PsiFile
import java.util.*
import java.util.concurrent.ExecutionException
import java.util.concurrent.TimeUnit
import java.util.concurrent.TimeoutException
import java.util.regex.Pattern
import java.util.stream.Collectors
import java.util.stream.Stream

class PsiTranslationSkeleton(private val stubId: String?, text: String?, private val elementText: @NlsSafe String?) {
    val prefix: StringBuffer
    val suffix = StringBuffer()
    val children = ArrayList<PsiTranslationSkeleton>()

    @Volatile
    var translateFuture: ListenableFuture<CharSequence?>? = null

    init {
        prefix = StringBuffer(text)
    }

    fun injectStubTranslation(
        translatedOuter: CharSequence?,
        translatedInner: CharSequence?,
        targetLanguage: ComputerLanguage?
    ): CharSequence? {
        if (stubId == null) return translatedOuter
        var regex: String
        when (targetLanguage) {
            ComputerLanguage.Python -> regex =  /*
                 This is a Java regular expression that looks for a pattern of a line of text followed by a colon and then another line
                 of text. The (?s) indicates that the expression should span multiple lines, and the (?<=\n|^) indicates that the pattern
                 should start at the beginning of a line or the beginning of the string. The [^\n]*? indicates that any number of
                 characters that are not a new line should be matched, followed by a colon and then another line of characters that are
                 not a new line.
                */"(?s)(?<=\n|^)[^\n]*?:\n[^\n]*?$stubId\\s*?pass\\s*?"

            ComputerLanguage.Rust, ComputerLanguage.Kotlin, ComputerLanguage.Scala, ComputerLanguage.Java, ComputerLanguage.JavaScript -> regex =
                "(?s)(?<=\n|^)[^\n]*?\\{[^{}]*?$stubId.*?\\}"

            else -> {
                regex = "(?s)(?<=\n|^)[^\n]*?\\{[^{}]*?$stubId.*?\\}"
                regex = if (Pattern.compile(regex).matcher(translatedOuter).find()) regex else stubId
            }
        }
        return translatedOuter.toString().replace(regex.toRegex(), translatedInner.toString())
    }

    fun translate(
        project: Project?,
        indent: CharSequence?,
        sourceLanguage: ComputerLanguage,
        targetLanguage: ComputerLanguage
    ): ListenableFuture<CharSequence?>? {
        if (null == translateFuture) {
            synchronized(this) {
                if (null == translateFuture) {
                    translateFuture = complete(
                        project, AppSettingsState.getInstance()
                            .createTranslationRequest()
                            .setInstruction(
                                String.format(
                                    "Translate %s into %s",
                                    sourceLanguage.name,
                                    targetLanguage.name
                                )
                            )
                            .setInputType("source")
                            .setInputText(translationText())
                            .setInputAttribute("language", sourceLanguage.name)
                            .setOutputType("translated")
                            .setOutputAttrute("language", targetLanguage.name)
                            .buildCompletionRequest(), indent!!
                    )
                }
            }
        }
        return translateFuture
    }

    private fun translationText(): String {
        return if (null != stubId) {
            elementText!!
        } else {
            toString()
        }
    }

    fun sequentialTranslate(
        project: Project?,
        indent: CharSequence?,
        sourceLanguage: ComputerLanguage,
        targetLanguage: ComputerLanguage
    ): ListenableFuture<*>? {
        var future: ListenableFuture<*>? = translate(project, indent, sourceLanguage, targetLanguage)
        for (stub in stubs) {
            future = Futures.transformAsync(
                future,
                { _: Any? -> stub.sequentialTranslate(project, indent, sourceLanguage, targetLanguage) },
                pool
            )
        }
        return future
    }

    fun parallelTranslate(
        project: Project?,
        indent: CharSequence,
        sourceLanguage: ComputerLanguage,
        targetLanguage: ComputerLanguage
    ): ListenableFuture<*> {
        return Futures.allAsList(
            translationFutures(project, indent, sourceLanguage, targetLanguage).collect(
                Collectors.toList()
            )
        )
    }

    fun translationFutures(
        project: Project?,
        indent: CharSequence,
        sourceLanguage: ComputerLanguage,
        targetLanguage: ComputerLanguage
    ): Stream<ListenableFuture<*>?> {
        return if (!stubs.isEmpty()) {
            Streams.concat(
                Stream.of(translate(project, indent, sourceLanguage, targetLanguage)),
                stubs.stream().flatMap { stub: PsiTranslationSkeleton ->
                    stub.translationFutures(
                        project,
                        "$indent  ",
                        sourceLanguage,
                        targetLanguage
                    )
                }
            )
        } else {
            Stream.of(translate(project, indent, sourceLanguage, targetLanguage))
        }
    }

    fun getTranslatedDocument(targetLanguage: ComputerLanguage?): CharSequence? {
        return try {
            var translated = translateFuture!![1, TimeUnit.MILLISECONDS]
            for (child in stubs) {
                translated =
                    child.injectStubTranslation(translated, child.getTranslatedDocument(targetLanguage), targetLanguage)
            }
            translated
        } catch (e: InterruptedException) {
            throw RuntimeException(e)
        } catch (e: ExecutionException) {
            throw RuntimeException(e)
        } catch (e: TimeoutException) {
            throw RuntimeException(e)
        }
    }

    private class Parser constructor(
        private val sourceLanguage: ComputerLanguage,
        private val targetLanguage: ComputerLanguage,
        private var currentContext: PsiTranslationSkeleton
    ) :
        PsiVisitorBase() {
        private var indent = ""
        override fun visit(element: PsiElement, self: PsiElementVisitor) {
            val text = element.text
            if (PsiUtil.matchesType(element, "Class", "ImplItem")) {
                val newNode = PsiTranslationSkeleton(null, getClassDefPrefix(text), element.text)
                currentContext.children.add(newNode)
                processChildren(element, self, newNode)
                newNode.suffix.append("}")
            } else if (PsiUtil.matchesType(
                    element,
                    "Method",
                    "FunctionDefinition",
                    "Function",
                    "StructItem",
                    "Struct"
                )
            ) {
                val declaration = PsiUtil.getDeclaration(element).trim { it <= ' ' }
                val stubID = "STUB: " + UUID.randomUUID().toString().substring(0, 8)
                // TODO: This needs to support arbitrary languages
                val newNode = PsiTranslationSkeleton(stubID, stubMethodText(declaration, stubID), element.text)
                currentContext.children.add(newNode)
                processChildren(element, self, newNode)
            } else if (PsiUtil.matchesType(element, "ImportList", "Field")) {
                val newNode = PsiTranslationSkeleton(null, element.text.trim { it <= ' ' }, element.text)
                currentContext.children.add(newNode)
                processChildren(element, self, newNode)
            } else {
                element.acceptChildren(self)
            }
        }

        private fun stubMethodText(declaration: String, stubID: String): String {
            return when (sourceLanguage) {
                ComputerLanguage.Python -> String.format(
                    "%s\n    %s\n    pass",
                    declaration,
                    targetLanguage.lineComment!!.fromString(stubID)
                )

                ComputerLanguage.Go, ComputerLanguage.Kotlin, ComputerLanguage.Scala, ComputerLanguage.Java, ComputerLanguage.JavaScript, ComputerLanguage.Rust -> String.format(
                    "%s {\n%s\n}\n",
                    declaration,
                    targetLanguage.lineComment!!.fromString(stubID)
                )

                else -> String.format("%s {\n%s\n}\n", declaration, targetLanguage.lineComment!!.fromString(stubID))
            }
        }

        private fun getClassDefPrefix(text: String): String {
            return when (sourceLanguage) {
                ComputerLanguage.Python -> indent + text.substring(0, text.indexOf(':')).trim { it <= ' ' } + ":"
                ComputerLanguage.Go, ComputerLanguage.Kotlin, ComputerLanguage.Scala, ComputerLanguage.Java, ComputerLanguage.JavaScript, ComputerLanguage.Rust -> indent + text.substring(
                    0,
                    text.indexOf('{')
                ).trim { it <= ' ' } + " {"

                else -> indent + text.substring(0, text.indexOf('{')).trim { it <= ' ' } + " {"
            }
        }

        private fun processChildren(
            element: PsiElement,
            self: PsiElementVisitor,
            newNode: PsiTranslationSkeleton
        ): PsiTranslationSkeleton {
            val prevclassBuffer = currentContext
            currentContext = newNode
            val prevIndent = indent
            indent += "  "
            element.acceptChildren(self)
            currentContext = prevclassBuffer
            indent = prevIndent
            return newNode
        }
    }

    val stubs: List<PsiTranslationSkeleton>
        get() = Stream.concat(
            children.stream().filter { x: PsiTranslationSkeleton -> x.stubId != null },
            children.stream().filter { x: PsiTranslationSkeleton -> x.stubId == null }
                .flatMap { x: PsiTranslationSkeleton -> x.stubs.stream() }
        ).collect(Collectors.toList())

    override fun toString(): String {
        val sb = ArrayList<String>()
        sb.add(prefix.toString())
        if (stubId == null) children.stream().map { obj: PsiTranslationSkeleton -> obj.toString() }
            .forEach { e: String ->
                sb.add(
                    e
                )
            }
        sb.add(suffix.toString())
        return sb.stream().reduce { l: String, r: String ->
            """
                $l
                $r
                """.trimIndent()
        }.get()
    }

    companion object {
        fun parseFile(
            psiFile: PsiFile,
            sourceLanguage: ComputerLanguage,
            targetLanguage: ComputerLanguage
        ): PsiTranslationSkeleton {
            val psiTranslationSkeleton = PsiTranslationSkeleton(null, "", psiFile.text)
            Parser(sourceLanguage, targetLanguage, psiTranslationSkeleton).build(psiFile)
            return psiTranslationSkeleton
        }
    }
}