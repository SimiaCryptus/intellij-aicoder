package com.github.simiacryptus.aicoder.util

import com.intellij.openapi.project.Project
import com.simiacryptus.skyenet.kotlin.KotlinInterpreter
import org.jetbrains.kotlin.jsr223.KotlinJsr223JvmScriptEngine4Idea
import org.jetbrains.kotlin.jsr223.KotlinJsr223StandardScriptEngineFactory4Idea
import java.lang.ref.WeakReference
import java.lang.reflect.Proxy
import java.util.*
import javax.script.ScriptContext

class IdeaKotlinInterpreter(
  symbols: Map<String, Any>
) : KotlinInterpreter(symbols) {
  companion object {
    var project: Project? = null
    val storageMap = WeakHashMap<Any, UUID>()
    val retrievalIndex = HashMap<UUID, WeakReference<Any>>()
  }
  override val scriptEngine get() =
      (KotlinJsr223StandardScriptEngineFactory4Idea().scriptEngine as KotlinJsr223JvmScriptEngine4Idea).apply {
        val engineBindings = this.context.getBindings(ScriptContext.ENGINE_SCOPE)
        val symbols = getSymbols()
        val globalBindings = this.context.getBindings(ScriptContext.GLOBAL_SCOPE)
        engineBindings?.putAll(symbols)
        globalBindings?.putAll(symbols)
      }

  override fun wrapCode(code: String): String {
    val out = ArrayList<String>()
    val (imports, otherCode) = code.split("\n").partition { it.trim().startsWith("import ") }
    out.addAll(imports)
    defs.forEach { (key, value) ->
      val uuid = storageMap.getOrPut(value) { UUID.randomUUID() }
      retrievalIndex.put(uuid, WeakReference(value))
      val fqClassName = IdeaKotlinInterpreter::class.java.name.replace("$", ".")
      val typeStr = typeOf(value)
      out.add("val $key : $typeStr = $fqClassName.retrievalIndex.get(java.util.UUID.fromString(\"$uuid\"))?.get()!! as $typeStr\n")
    }
    out.addAll(otherCode)
    val txt = out.joinToString("\n")
    return txt
  }

  fun typeOf(value: Any?): String {
    if (value is Proxy) {
      return value.javaClass.interfaces[0].name.replace("$", ".") + "?"
    }
    val replace = value?.javaClass?.name?.replace("$", ".")
    return if (replace != null) ("$replace") else "null"
  }

}