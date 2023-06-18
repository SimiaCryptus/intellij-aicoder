@file:Suppress("PLATFORM_CLASS_MAPPED_TO_KOTLIN")

package com.simiacryptus.skyenet.heart

import com.simiacryptus.skyenet.Heart
import java.lang.reflect.Method

open class WeakGroovyInterpreter(defs: java.util.Map<String, Object>) : Heart {

    private val shell: Any
    private val parseMethod: Method
    private val runMethod: Method
    private val setVariableMethod: Method

    init {
        val groovyClassLoader = WeakGroovyInterpreter::class.java.classLoader
        val compilerConfigurationClass = groovyClassLoader.loadClass("org.codehaus.groovy.control.CompilerConfiguration")
        val groovyShellClass = groovyClassLoader.loadClass("groovy.lang.GroovyShell")
        val scriptClass = groovyClassLoader.loadClass("groovy.lang.Script")

        parseMethod = groovyShellClass.getMethod("parse", String::class.java)
        runMethod = scriptClass.getMethod("run")
        setVariableMethod = groovyShellClass.getMethod("setVariable", String::class.java, Any::class.java)

        val compilerConfiguration = compilerConfigurationClass.getDeclaredConstructor().newInstance()
        shell = groovyShellClass.getDeclaredConstructor(compilerConfigurationClass).newInstance(compilerConfiguration)

        defs.entrySet().forEach { (key, value) ->
            setVariableMethod.invoke(shell, key, value)
        }
    }

    override fun getLanguage(): String {
        return "groovy"
    }

    override fun run(code: String): Any? {
        val wrapExecution = wrapExecution {
            try {
                val script = parseMethod.invoke(shell, wrapCode(code))
                runMethod.invoke(script)
            } catch (e: Exception) {
                if (e.cause?.javaClass?.name == "org.codehaus.groovy.control.CompilationFailedException") {
                    throw e.cause as Exception
                } else {
                    throw e
                }
            }
        }
        return wrapExecution
    }

    override fun validate(code: String): Exception? {
        return try {
            parseMethod.invoke(shell, wrapCode(code))
            null
        } catch (e: Exception) {
            if (e.cause?.javaClass?.name == "org.codehaus.groovy.control.CompilationFailedException") {
                e.cause as Exception
            } else {
                null
            }
        }
    }
}
