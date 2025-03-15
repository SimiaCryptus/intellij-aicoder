package com.simiacryptus.aicoder.util

import com.intellij.openapi.command.WriteCommandAction
import com.intellij.openapi.fileTypes.FileTypeRegistry
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiFileFactory
import com.intellij.testFramework.LightVirtualFile
import com.simiacryptus.skyenet.core.util.GrammarValidator
import com.intellij.psi.PsiErrorElement

class IntelliJPsiValidator(private val project: Project, val extension: String, val filename: String) : GrammarValidator {
    override fun validateGrammar(code: String): List<GrammarValidator.ValidationError> {
        var errors: List<GrammarValidator.ValidationError>? = null
        WriteCommandAction.runWriteCommandAction(project) {
             try {
                val fileType = FileTypeRegistry.getInstance().getFileTypeByExtension(extension)
                val virtualFile = LightVirtualFile("dummy.$extension", fileType, code)
                val psiFile = PsiFileFactory.getInstance(project).createFileFromText(virtualFile.name, fileType, code)
                errors = collectErrors(psiFile)
            } catch (e: Exception) {
                listOf(GrammarValidator.ValidationError(
                    message = "Error validating ${SUPPORTED_LANGUAGES[extension.lowercase()]} grammar: ${e.message}",
                    severity = GrammarValidator.Severity.ERROR
                ))
            }
        }
        return errors ?: emptyList()
    }
    companion object {
        // Map of supported file extensions to their language names
        private val SUPPORTED_LANGUAGES = mapOf(
            "kt" to "Kotlin",
            "java" to "Java",
            "py" to "Python",
            "js" to "JavaScript",
            "ts" to "TypeScript",
            "go" to "Go",
            "rs" to "Rust",
            "cpp" to "C++",
            "c" to "C",
            "cs" to "C#",
            "scala" to "Scala",
            "rb" to "Ruby",
            "php" to "PHP",
            "swift" to "Swift",
            "ts" to "TypeScript",
            "tsx" to "TypeScript",
            "jsx" to "JavaScript",
            "vue" to "Vue",
            "html" to "HTML",
            "css" to "CSS",
            "scss" to "SCSS",
            "sass" to "SASS",
            "less" to "LESS",
            "json" to "JSON",
            "xml" to "XML",
            "yaml" to "YAML",
            "yml" to "YAML",
            "md" to "Markdown"
        )
        /**
         * Check if a language is supported
         */
        fun isLanguageSupported(extension: String?): Boolean {
            return extension?.lowercase()?.let { SUPPORTED_LANGUAGES.containsKey(it) } ?: false
        }
    }


    private fun collectErrors(psiFile: com.intellij.psi.PsiFile): List<GrammarValidator.ValidationError> {
        val errors = mutableListOf<GrammarValidator.ValidationError>()
        psiFile.accept(object : com.intellij.psi.PsiRecursiveElementVisitor() {
            override fun visitErrorElement(element: com.intellij.psi.PsiErrorElement) {
                errors.add(GrammarValidator.ValidationError(
                    message = element.errorDescription,
                    line = element.lineNumber,
                    column = element.startOffsetInParent,
                    severity = GrammarValidator.Severity.ERROR
                ))
            }
        })
        return errors
    }
    private val PsiErrorElement.lineNumber: Int?
        get() = containingFile.viewProvider.document?.getLineNumber(textRange.startOffset)?.plus(1)
}