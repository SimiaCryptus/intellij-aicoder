package com.github.simiacryptus.aicoder.actions.dev

import com.github.simiacryptus.aicoder.config.AppSettingsState
import com.github.simiacryptus.aicoder.util.UITools
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.simiacryptus.skyenet.Heart
import com.simiacryptus.skyenet.OutputInterceptor
import com.simiacryptus.skyenet.body.ClasspathResource
import com.simiacryptus.skyenet.body.SessionServerUtil.asJava
import com.simiacryptus.skyenet.body.SkyenetCodingSessionServer
import com.simiacryptus.skyenet.heart.WeakGroovyInterpreter
import org.eclipse.jetty.util.resource.Resource
import java.util.Map
import java.util.function.Supplier

class SkyenetProjectCodingSessionServer(
    val project: Project,
    val selectedFolder: VirtualFile
) : SkyenetCodingSessionServer(
    applicationName = "IdeaAgent",
    model = AppSettingsState.instance.defaultChatModel(),
    apiKey = AppSettingsState.instance.apiKey
) {
    override val baseResource: Resource
        get() = ClasspathResource(javaClass.classLoader.getResource(resourceBase))

    override fun hands() = Map.of(
        "ide",
        object : LaunchSkyenetAction.TestTools {
            override fun getProject(): Project {
                return project
            }

            override fun getSelectedFolder(): VirtualFile {
                return selectedFolder
            }

            val toolStream =
                OutputInterceptor.createInterceptorStream(
                    System.out
                    //PrintStream(NullOutputStream.NULL_OUTPUT_STREAM)
                )

            override fun print(msg: String) {
                toolStream.println(msg)
            }
        } as Object,
    ).asJava

    override fun toString(e: Throwable): String {
        return e.message ?: e.toString()
    }

    override fun heart(hands: Map<String, Object>): Heart = object : WeakGroovyInterpreter(hands) {
        override fun <T : Any> wrapExecution(fn: Supplier<T?>): T? {
            return UITools.run(
                project, "Running Script", false
            ) {
                fn.get()
            }
        }
    }
}