package com.github.simiacryptus.aicoder.actions.dev

import com.github.simiacryptus.aicoder.config.AppSettingsState
import com.github.simiacryptus.aicoder.util.UITools
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.simiacryptus.openai.OpenAIClient
import com.simiacryptus.skyenet.*
import com.simiacryptus.skyenet.heart.GroovyInterpreter
import com.simiacryptus.skyenet.heart.JavaInterpreter
import com.simiacryptus.skyenet.heart.KotlinInterpreterAlternate
import com.simiacryptus.util.YamlDescriber

class LaunchSkyenetAction : AnAction() {

    override fun update(e: AnActionEvent) {
        e.presentation.isEnabledAndVisible = isEnabled()
        super.update(e)
    }

    interface TestTools {
        fun getProject(): Project
        fun getSelectedFolder(): VirtualFile
        fun print(text: String)
    }

    override fun actionPerformed(e: AnActionEvent) {
        val api = OpenAIClient(
            AppSettingsState.instance.apiKey,
            AppSettingsState.instance.apiBase,
            AppSettingsState.instance.apiLogLevel
        )
        val face = Face()
        val ears = Ears(api, 0.5)
        val tools = object : TestTools {
            override fun getProject(): Project {
                return e.project!!
            }

            override fun getSelectedFolder(): VirtualFile {
                return UITools.getSelectedFolder(e)!!
            }

            override fun print(text: String) {
                face.scriptingResult.text = face.scriptingResult.text + "\n" + text
            }
        }
        val apiObjects = mapOf(
            "toolObj" to tools
        )
        val heart = object : GroovyInterpreter(apiObjects) {
            override fun <T:Any> wrapExecution(fn: () -> T?): T? = UITools.run(
                e.project, "Running Script", false
            ) {
                fn()
            }
        }
        val brain = Brain(
            api = api,
            apiObjects = apiObjects,
            model = AppSettingsState.instance.model_chat,
            maxTokens = AppSettingsState.instance.maxTokens,
            temperature = AppSettingsState.instance.temperature,
            yamlDescriber = object : YamlDescriber(false) {
                override fun toYaml(
                    rawType: Class<in Nothing>,
                    stackMax: Int,
                ): String {
                    val abbreviated = listOf(
                        "com.intellij"
                    )
                    if (abbreviated.find { rawType.name.startsWith(it) } != null) return """
                                                    |type: object
                                                    |class: ${rawType.name}
                                                    """.trimMargin()
                    return super.toYaml(rawType, stackMax)
                }
            },
            language = heart.getLanguage(),
        )
        val body = Body(
            api,
            apiObjects,
            brain,
            heart
        )
        val head = Head(
            body,
            ears,
            face
        )
        head.start(api)
    }

    private fun isEnabled(): Boolean {
        if (UITools.isSanctioned()) return false
        if (!AppSettingsState.instance.devActions) return false
        return true
    }

    companion object {
        private val log = org.slf4j.LoggerFactory.getLogger(LaunchSkyenetAction::class.java)
    }
}
