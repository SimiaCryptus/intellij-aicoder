package com.github.simiacryptus.aicoder.util

import com.intellij.openapi.project.Project
import com.simiacryptus.skyenet.kotlin.KotlinInterpreter

class IdeaKotlinInterpreter(
  symbols: Map<String, Any>
) : KotlinInterpreter(symbols) {
  companion object {
    var project: Project? = null
  }

//  override val scriptEngine
//    get() = object : KotlinJsr223JvmScriptEngineFactoryBase() {
//      override fun getScriptEngine() = KotlinJsr223JvmScriptEngine4Idea(
//        factory = this,
//        templateClasspath = scriptCompilationClasspathFromContextOrStdlib(
//          keyNames = arrayOf(),
//          classLoader = KotlinInterpreter::class.java.classLoader!!,
//          wholeClasspath = true,
//        ) + kotlinScriptStandardJars,
//        templateClassName = "kotlin.script.templates.standard.ScriptTemplateWithBindings",
//        getScriptArgs = { context, kClasses ->
//          ScriptArgsWithTypes(
//            scriptArgs = arrayOf(),
//            scriptArgsTypes = arrayOf()
//          )
//          },
//        scriptArgsTypes = arrayOf()
//      )
//    }.scriptEngine.apply {
//      getBindings(ScriptContext.ENGINE_SCOPE).putAll(symbols)
//    }
}