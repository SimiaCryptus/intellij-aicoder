﻿package com.github.simiacryptus.aicoder.config

import com.fasterxml.jackson.annotation.JsonIgnore
import com.github.simiacryptus.aicoder.ui.EditorMenu
import com.github.simiacryptus.aicoder.util.UITools
import com.intellij.openapi.actionSystem.AnAction
import groovy.lang.GroovyClassLoader
import java.io.File
import java.util.stream.Collectors

class ActionSettingsRegistry {

    val actionSettings: MutableMap<String, ActionSettings> = HashMap()
    private val version = 2.0002

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
                        actionConfig.version = version
                    } else if (!actionConfig.isDynamic && (actionConfig.version ?: 0.0) < version) {
                        val canLoad = try {
                            ActionSettingsRegistry::class.java.classLoader.loadClass(actionConfig.id)
                            true
                        } catch (e: Throwable) {
                            false
                        }
                        if (canLoad) {
                            actionConfig.file.writeText(code)
                            actionConfig.version = version
                        } else {
                            children.remove(it)
                        }
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
                    UITools.error(log, "Error loading ${it.javaClass}", e)
                }
            }
        }
        this.getDynamicActions().forEach {
            try {
                if (!it.file.exists()) return@forEach
                if (!it.enabled) return@forEach
                val element = it.buildAction(it.file.readText())
                children.add(element)
            } catch (e: Throwable) {
                UITools.error(log, "Error loading dynamic action", e)
            }
        }
        return children.toTypedArray()
    }

    class DynamicActionException(
        cause: Throwable,
        msg: String,
        val file: File,
        val actionSetting: ActionSettings
    ) : Exception(msg, cause)

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
            try {
                return with(
                    actionCache.getOrPut("$packageName.$newClassName") {
                        (GroovyClassLoader(ActionSettingsRegistry::class.java.classLoader).parseClass(
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
            } catch (e: Throwable) {
                throw DynamicActionException(e, "Error in Action " + displayText, file, this)
            }
        }

        private val packageName: String get() = id.substringBeforeLast('.')
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

    private fun getActionConfig(action: AnAction): ActionSettings {
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

        private val log = org.slf4j.LoggerFactory.getLogger(ActionSettingsRegistry::class.java)

        val actionCache = HashMap<String, AnAction>()
        private fun load(actionPackage: String, actionName: String, language: String) =
            load("/sources/${language}/$actionPackage/$actionName.$language")

        private fun load(path: String) =
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
