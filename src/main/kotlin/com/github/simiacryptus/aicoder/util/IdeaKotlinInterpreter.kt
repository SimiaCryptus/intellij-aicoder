package com.github.simiacryptus.aicoder.util

import com.intellij.lang.Language
import com.intellij.openapi.application.runReadAction
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiFileFactory
import com.simiacryptus.skyenet.kotlin.KotlinInterpreter
import org.jetbrains.kotlin.jsr223.KotlinJsr223StandardScriptEngineFactory4Idea
import org.jetbrains.kotlin.resolve.AnalyzingUtils
import org.slf4j.LoggerFactory
import java.util.Map

class IdeaKotlinInterpreter(defs: Map<String, Object>) : KotlinInterpreter(defs) {
  companion object {
    private val log = LoggerFactory.getLogger(IdeaKotlinInterpreter::class.java)

    var project: Project? = null
  }
  override val scriptEngine: javax.script.ScriptEngine
    get() = KotlinJsr223StandardScriptEngineFactory4Idea().scriptEngine
  override fun validate(code: String) = try {
    val messageCollector = MessageCollectorImpl(code)
    val psiFileFactory = PsiFileFactory.getInstance(project!!)
    runReadAction {
      AnalyzingUtils.checkForSyntacticErrors(
        psiFileFactory.createFileFromText(
          "Dummy.kt",
          Language.findLanguageByID("kotlin")!!,
          code
        )
      )
    }
    if (messageCollector.errors.isEmpty()) {
      null
    } else RuntimeException(
      """
      |${messageCollector.errors.joinToString("\n") { "Error: $it" }}
      |${messageCollector.warnings.joinToString("\n") { "Warning: $it" }}
      """.trimMargin()
    )
  } catch (e: Throwable) {
    e
  }
}