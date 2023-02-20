package com.github.simiacryptus.aicoder.actions.code

import com.github.simiacryptus.aicoder.com.github.simiacryptus.aicoder.openai.OpenAI_API.pool
import com.github.simiacryptus.aicoder.config.AppSettingsState
import com.github.simiacryptus.aicoder.util.ComputerLanguage
import com.github.simiacryptus.aicoder.util.UITools.getIndent
import com.github.simiacryptus.aicoder.util.psi.PsiTranslationSkeleton
import com.google.common.util.concurrent.FutureCallback
import com.google.common.util.concurrent.Futures
import com.google.common.util.concurrent.ListenableFuture
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.command.WriteCommandAction
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import java.io.IOException
import java.util.concurrent.ExecutionException
import java.util.concurrent.TimeUnit
import java.util.concurrent.TimeoutException
import java.util.concurrent.atomic.AtomicReference

class ConvertFileToLanguage(private val targetLanguage: ComputerLanguage) : AnAction(
    targetLanguage.name
) {
    override fun actionPerformed(event: AnActionEvent) {
        val sourceLanguage = ComputerLanguage.getComputerLanguage(event)
        val indent = getIndent(event.getData(CommonDataKeys.CARET))
        val skeleton = PsiTranslationSkeleton.parseFile(
            event.getRequiredData(CommonDataKeys.PSI_FILE),
            sourceLanguage!!,
            targetLanguage
        )
        translate(event, sourceLanguage, indent, skeleton)
    }

    private fun translate(
        event: AnActionEvent,
        sourceLanguage: ComputerLanguage?,
        indent: CharSequence,
        root: PsiTranslationSkeleton
    ) {
        val future: ListenableFuture<*> = if (AppSettingsState.getInstance().apiThreads > 1) {
            root.parallelTranslate(event.project, indent, sourceLanguage!!, targetLanguage)
        } else {
            root.sequentialTranslate(event.project, indent, sourceLanguage!!, targetLanguage)!!
        }
        Futures.addCallback(future, object : FutureCallback<Any?> {
            override fun onSuccess(newText: Any?) {
                try {
                    future[1, TimeUnit.MILLISECONDS]
                } catch (e: InterruptedException) {
                    throw RuntimeException(e)
                } catch (e: ExecutionException) {
                    throw RuntimeException(e)
                } catch (e: TimeoutException) {
                    throw RuntimeException(e)
                }
                val content = root.getTranslatedDocument(targetLanguage).toString()
                write(
                    event.project,
                    getNewFile(event.project, event.getRequiredData(CommonDataKeys.VIRTUAL_FILE), targetLanguage),
                    content
                )
            }

            override fun onFailure(e: Throwable) {
                log.error("Error translating file", e)
            }
        }, pool)
    }

    companion object {
        private val log = Logger.getInstance(
            ConvertFileToLanguage::class.java
        )

        fun getNewFile(project: Project?, file: VirtualFile, language: ComputerLanguage): VirtualFile {
            val newFileRef = AtomicReference<VirtualFile>()
            WriteCommandAction.runWriteCommandAction(project) {
                try {
                    newFileRef.set(
                        file.parent
                            .createChildData(file, file.nameWithoutExtension + "." + language.extensions[0])
                    )
                } catch (ex: IOException) {
                    throw RuntimeException(ex)
                }
            }
            return newFileRef.get()
        }

        fun write(project: Project?, newFile: VirtualFile, content: String) {
            WriteCommandAction.runWriteCommandAction(project) {
                try {
                    newFile.setBinaryContent(content.toByteArray())
                } catch (e: IOException) {
                    throw RuntimeException(e)
                }
            }
        }
    }
}