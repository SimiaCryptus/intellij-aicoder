package com.simiacryptus.skyenet.heart

import com.simiacryptus.skyenet.core.Heart
import java.lang.reflect.Method

@Suppress("unused")
open class WeakKotlinInterpreter(
    defs: Map<String, Any> = mapOf(),
) : Heart {

    private val engine: Any

    init {
        // Use WeakKotlinInterpreter classloader to load Kotlin classes
        val groovyClassLoader = WeakKotlinInterpreter::class.java.classLoader
        val factoryClass =
            groovyClassLoader.loadClass("org.jetbrains.kotlin.script.jsr223.KotlinJsr223JvmLocalScriptEngineFactory")
        val factory = factoryClass.getDeclaredConstructor().newInstance()
        val getScriptEngineMethod: Method = factoryClass.getMethod("getScriptEngine")
        engine = getScriptEngineMethod.invoke(factory)
        defs.forEach { (key, value) ->
            val putMethod: Method = engine.javaClass.getMethod("put", String::class.java, Any::class.java)
            putMethod.invoke(engine, key, value)
        }
    }

    override fun getLanguage(): String {
        return "Kotlin"
    }

    override fun run(code: String): Any? {
        val evalMethod: Method = engine.javaClass.getMethod("eval", String::class.java)
        return wrapExecution { evalMethod.invoke(engine, wrapCode(code)) }
    }

    override fun validate(code: String): Exception? {
        return try {
            val compileMethod: Method =
                engine.javaClass.getMethod("compile", String::class.java, Class.forName("javax.script.ScriptContext"))
            compileMethod.invoke(engine, wrapCode(code), engine.javaClass.getMethod("getContext").invoke(engine))
            null
        } catch (e: Exception) {
            e
        }
    }
}
