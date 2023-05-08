package com.github.simiacryptus.aicoder.util.psi

import com.github.simiacryptus.aicoder.config.AppSettingsState
import com.github.simiacryptus.aicoder.util.ComputerLanguage
import com.github.simiacryptus.aicoder.util.UITools.api
import com.github.simiacryptus.aicoder.util.UITools.filterStringResult
import com.intellij.openapi.application.runReadAction
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiElementVisitor
import com.intellij.psi.PsiFile
import com.simiacryptus.openai.proxy.ChatProxy
import java.util.*
import java.util.concurrent.ExecutionException
import java.util.concurrent.TimeoutException
import java.util.regex.Pattern

class PsiTranslationTree(
    private val stubId: String?,
    val prefix: String?,
    private val elementText: String?,
    val sourceLanguage: ComputerLanguage,
    val targetLanguage: ComputerLanguage,
) {
    val suffix = StringBuffer()
    val children = ArrayList<PsiTranslationTree>()

    fun getStubRegex(
        targetLanguage: ComputerLanguage?,
        translatedOuter: CharSequence?,
    ): Regex {
        var regex: String
        when (targetLanguage) {
            ComputerLanguage.Python ->
                regex = "(?s)(?<=\n|^)[^\n]*?:\n[^\n]*?$stubId\\s*?pass\\s*?"

            ComputerLanguage.Rust, ComputerLanguage.Kotlin, ComputerLanguage.Scala, ComputerLanguage.Java, ComputerLanguage.JavaScript ->
                regex = "(?s)(?<=\n|^)[^\n]*?\\{[^{}]*?$stubId.*?\\}"

            else -> {
                regex = "(?s)(?<=\n|^)[^\n]*?\\{[^{}]*?$stubId.*?\\}"
                regex = if (Pattern.compile(regex).matcher(translatedOuter!!).find()) regex else stubId!!
            }
        }
        return regex.toRegex()
    }

    @Volatile
    var translatedResult: CharSequence? = null

    private val stubs: List<PsiTranslationTree>
        get() = listOf(
            children.filter { it.stubId != null },
            children.filter { it.stubId == null }.flatMap { it.stubs }
        ).flatten()

    private fun executeTranslation(
        indent: CharSequence,
    ): CharSequence {
        if (null == translatedResult) {
            synchronized(this) {
                if (null == translatedResult) {
                    val text = translationText()
                    val filterStringResult = filterStringResult(indent)(
                        proxy.convert(
                            text = text,
                            sourceLanguage.name,
                            targetLanguage.name
                        ).code ?: ""
                    )
                    translatedResult = filterStringResult
                }
            }
        }
        return translatedResult!!
    }

    private fun translationText(): String {
        return if (null != stubId) {
            elementText ?: ""
        } else {
            toString()
        }
    }

    fun translateTree(
        project: Project,
        indent: CharSequence,
    ) {
        executeTranslation(indent)
        for (stub in stubs) {
            stub.translateTree(project, indent)
        }
    }

    fun getTranslatedDocument(): CharSequence = try {
        var translated = translatedResult.toString()
        if(!stubs.isEmpty()) {
            logger.warn("Translating ${stubs.size} stubs in ${stubId ?: "---"} - Initial Code: \n```\n    ${translationText().replace("\n", "\n    ")}\n```\n")
        }
        for (child in stubs) {
            translated = if (child.stubId == null) translated
            else {
                val regex = child.getStubRegex(targetLanguage, translated)
                val childDoc = child.getTranslatedDocument().toString()
                val findAll = regex.findAll(translated).toList()
                logger.warn("Replacing ${findAll.size} instances of ${child.stubId} with: \n```\n    ${childDoc.replace("\n", "\n    ")}\n```\n")
                translated.replace(regex, childDoc.replace("\$", "\\\$"))
            }
        }
        logger.warn("Translation for ${stubId ?: "---"}: \n```\n    ${translated.replace("\n", "\n    ")}\n```\n")
        translated
    } catch (e: InterruptedException) {
        throw RuntimeException(e)
    } catch (e: ExecutionException) {
        throw RuntimeException(e)
    } catch (e: TimeoutException) {
        throw RuntimeException(e)
    }

    inner class Parser(val indent: String = "") : PsiElementVisitor() {

        override fun visitElement(element: PsiElement) {
            val text = element.text
            if (PsiUtil.matchesType(element, "Class", "ImplItem")) {
                val newNode =
                    PsiTranslationTree(null, getClassDefPrefix(text), element.text, sourceLanguage, targetLanguage)
                children.add(newNode)
                element.acceptChildren(Parser(indent + "  "))
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
                val declaration = PsiUtil.getDeclaration(element).trim()
                val stubID = "STUB: " + UUID.randomUUID().toString().substring(0, 8)
                // TODO: This needs to support arbitrary languages
                val newNode = PsiTranslationTree(
                    stubID,
                    stubMethodText(declaration, stubID),
                    element.text,
                    sourceLanguage,
                    targetLanguage
                )
                children.add(newNode)
                element.acceptChildren(Parser(indent + "  "))
            } else if (PsiUtil.matchesType(element, "ImportList", "Field")) {
                val newNode =
                    PsiTranslationTree(null, element.text.trim(), element.text, sourceLanguage, targetLanguage)
                children.add(newNode)
                element.acceptChildren(Parser(indent + "  "))
            } else {
                element.acceptChildren(this)
            }
        }

        private fun stubMethodText(declaration: String, stubID: String): String {
            return when (sourceLanguage) {
                ComputerLanguage.Python -> String.format(
                    """
                    |%s
                    |    %s
                    |    pass""".trimMargin().trim(),
                    declaration,
                    targetLanguage.lineComment.fromString(stubID)
                )

                ComputerLanguage.Go, ComputerLanguage.Kotlin, ComputerLanguage.Scala, ComputerLanguage.Java, ComputerLanguage.JavaScript, ComputerLanguage.Rust -> String.format(
                    "%s {\n%s\n}\n",
                    declaration,
                    targetLanguage.lineComment.fromString(stubID)
                )

                else -> String.format(
                    "%s {\n%s\n}\n",
                    declaration,
                    targetLanguage.lineComment.fromString(stubID)
                )
            }
        }

        private fun getClassDefPrefix(text: String): String {
            val prefixToCurly = text.substring(0, text.indexOf('{')).trim()
            return when (sourceLanguage) {
                ComputerLanguage.Python -> indent + text.substring(0, text.indexOf(':')).trim() + ":"
                ComputerLanguage.Go, ComputerLanguage.Kotlin, ComputerLanguage.Scala, ComputerLanguage.Java, ComputerLanguage.JavaScript, ComputerLanguage.Rust -> {
                    "$indent$prefixToCurly {"
                }

                else -> "$indent$prefixToCurly {"
            }
        }

    }

    override fun toString(): String {
        val sb = ArrayList<String>()
        sb.add(prefix ?: "")
        if (stubId == null) children.map { it.toString() }.forEach { sb.add(it) }
        sb.add(suffix.toString())
        return sb.joinToString("\n")
    }

    companion object {

        val logger = Logger.getInstance(PsiTranslationTree::class.java)

        fun parseFile(
            psiFile: PsiFile,
            sourceLanguage: ComputerLanguage,
            targetLanguage: ComputerLanguage,
        ): PsiTranslationTree {
            val psiTranslationTree = PsiTranslationTree(null, "", psiFile.text, sourceLanguage, targetLanguage)
            runReadAction {
                psiFile.accept(psiTranslationTree.Parser())
            }
            return psiTranslationTree
        }

        interface VirtualAPI {
            fun convert(text: String, from_language: String, to_language: String): ConvertedText
            data class ConvertedText(
                val code: String? = null,
                val language: String? = null,
            )
        }

        val proxy: VirtualAPI
            get() = ChatProxy(
                clazz = VirtualAPI::class.java,
                api = api,
                maxTokens = AppSettingsState.instance.maxTokens,
                deserializerRetries = 5,
            ).create()
    }
}