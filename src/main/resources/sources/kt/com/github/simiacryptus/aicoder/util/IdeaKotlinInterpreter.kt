package com.github.simiacryptus.aicoder.util

import com.intellij.lang.Language
import com.intellij.openapi.application.runReadAction
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiFileFactory
import com.simiacryptus.skyenet.kotlin.KotlinInterpreter
import org.jetbrains.kotlin.cli.common.repl.KotlinJsr223JvmScriptEngineFactoryBase
import org.jetbrains.kotlin.cli.common.repl.ScriptArgsWithTypes
import org.jetbrains.kotlin.jsr223.KotlinJsr223JvmScriptEngine4Idea
import org.jetbrains.kotlin.resolve.AnalyzingUtils
import org.slf4j.LoggerFactory
import javax.script.ScriptEngine
import kotlin.script.experimental.jvm.util.KotlinJars.kotlinScriptStandardJars
import kotlin.script.experimental.jvm.util.scriptCompilationClasspathFromContextOrStdlib

class IdeaKotlinInterpreter(defs: Map<String, Any>) : KotlinInterpreter(defs) {
  companion object {
    private val log = LoggerFactory.getLogger(IdeaKotlinInterpreter::class.java)

    var project: Project? = null
  }

  override val scriptEngine: ScriptEngine
    get() {
      val factory = object : KotlinJsr223JvmScriptEngineFactoryBase() {
        override fun getScriptEngine(): ScriptEngine = KotlinJsr223JvmScriptEngine4Idea(
          factory = this,
          templateClasspath = scriptCompilationClasspathFromContextOrStdlib(
            keyNames = arrayOf(),
            classLoader = KotlinInterpreter::class.java.classLoader!!,
            wholeClasspath = true,
          ) + kotlinScriptStandardJars,
          templateClassName = "kotlin.script.templates.standard.SimpleScriptTemplate",
          getScriptArgs = { context, kClasses ->
            ScriptArgsWithTypes(
            scriptArgs = arrayOf(
//              context.getBindings(ScriptContext.ENGINE_SCOPE)
            ),
            scriptArgsTypes = arrayOf(
//              Bindings::class
            )) },
          scriptArgsTypes = arrayOf(
            //Reflection.getOrCreateKotlinClass(MutableMap::class.java)
          )
        )
      }
      return factory.scriptEngine
    }

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