package com.github.simiacryptus.aicoder.config

import com.fasterxml.jackson.annotation.JsonIgnore
import com.github.simiacryptus.aicoder.ui.EditorMenu
import com.intellij.openapi.actionSystem.AnAction
import groovy.lang.GroovyClassLoader
import java.io.File
import java.util.stream.Collectors

class ActionSettingsRegistry {

    val actionSettings: MutableMap<String, ActionSettings> = HashMap()
    val version = 1.3

    fun edit(superChildren: Array<out AnAction>): Array<AnAction> {
        val children = superChildren.toList().toMutableList()
        children.toTypedArray().forEach {
            val language = "groovy"
            val code = load(it.javaClass, language)
            if (null != code) {
                try {
                    val actionConfig = this.getActionConfig(it)
                    actionConfig.language = language
                    actionConfig.isDynamic = false
                    with(it) {
                        templatePresentation.text = actionConfig.displayText
                        templatePresentation.description = actionConfig.displayText
                    }
                    if (!actionConfig.enabled) {
                        children.remove(it)
                    } else if (!actionConfig.file.exists()
                        || actionConfig.file.readText().isBlank()
                        || (actionConfig.version ?: 0.0) < version
                    ) {
                        actionConfig.file.writeText(code)
                    } else {
                        val localCode = actionConfig.file.readText()
                        if (localCode != code) {
                            val element = actionConfig.buildAction(localCode)
                            children.remove(it)
                            children.add(element)
                        }
                    }
                    actionConfig.version = version
                } catch (e: Throwable) {
                    log.warn("Error loading ${it.javaClass}", e)
                }
            }
        }
        this.getDynamicActions().forEach {
            try {
                if (!it.file.exists()) return@forEach
                if (!it.enabled) return@forEach
                children.add(it.buildAction(it.file.readText()))
            } catch (e: Throwable) {
                log.warn("Error loading dynamic action", e)
            }
        }
        return children.toTypedArray()
    }

    data class ActionSettings(
        val id: String, // Static property
        var enabled: Boolean = true, // User settable
        var displayText: String? = null, // User settable
        var version: Double? = null, // System property
        var isDynamic: Boolean = false, // Static property
        var language: String? = null, // Static property
    ) {

        fun buildAction(
            code: String
        ): AnAction {
            val newClassName = this.className + "_" + Integer.toHexString(code.hashCode())
            return with(
                actionCache.getOrPut("$packageName.$newClassName") {
                    (GroovyClassLoader(EditorMenu::class.java.classLoader).parseClass(
                        code.replace(
                            ("""(?<![\w\d])${this.className}(?![\w\d])""").toRegex(),
                            newClassName
                        )
                    ).getDeclaredConstructor().newInstance() as AnAction)
                }
            ) {
                templatePresentation.text = displayText
                templatePresentation.description = displayText
                this
            }
        }

        val packageName: String get() = id.substringBeforeLast('.')
        val className: String get() = id.substringAfterLast('.')

        val file: File
            get() {
                val file = File(configDir(), "aicoder/actions/${packageName.replace('.', '/')}/$className.$language")
                file.parentFile.mkdirs()
                return file
            }

        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (javaClass != other?.javaClass) return false

            other as ActionSettings

            if (id != other.id) return false
            if (enabled != other.enabled) return false
            if (displayText != other.displayText) return false
            if (isDynamic != other.isDynamic) return false
            return language == other.language
        }

        override fun hashCode(): Int {
            var result = id.hashCode()
            result = 31 * result + enabled.hashCode()
            result = 31 * result + (displayText?.hashCode() ?: 0)
            result = 31 * result + isDynamic.hashCode()
            result = 31 * result + (language?.hashCode() ?: 0)
            return result
        }

    }

    fun getActionConfig(action: AnAction): ActionSettings {
        return actionSettings.getOrPut(action.javaClass.name) {
            val actionConfig = ActionSettings(action.javaClass.name)
            actionConfig.displayText = action.templatePresentation.text
            actionConfig
        }
    }

    @JsonIgnore
    fun getDynamicActions(): List<ActionSettings> {
        return actionSettings.entries.stream().filter { it.value.isDynamic && it.value.enabled }.map { it.value }
            .collect(Collectors.toList())
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false
        other as ActionSettingsRegistry
        return actionSettings == other.actionSettings
    }

    override fun hashCode(): Int {
        return actionSettings.hashCode()
    }

    companion object {

        val log = org.slf4j.LoggerFactory.getLogger(ActionSettingsRegistry::class.java)

        val actionCache = HashMap<String, AnAction>()
        fun load(actionPackage: String, actionName: String, language: String) =
            load("/sources/${language}/$actionPackage/$actionName.$language")

        fun load(path: String) =
            EditorMenu::class.java.getResourceAsStream(path)?.readAllBytes()?.toString(Charsets.UTF_8)

        fun load(clazz: Class<AnAction>, language: String) =
            load(clazz.`package`.name.replace('.', '/'), clazz.simpleName, language)

        fun configDir(): File {
            var baseDir = System.getProperty("idea.config.path")
            if (baseDir == null) baseDir = System.getProperty("user.home")
            return File(baseDir)
        }
    }

}
