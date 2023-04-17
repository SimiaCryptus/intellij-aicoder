package com.github.simiacryptus.aicoder.openai.async

import com.github.simiacryptus.aicoder.config.AppSettingsState
import com.github.simiacryptus.aicoder.util.UITools
import com.github.simiacryptus.aicoder.util.UITools.run
import com.simiacryptus.util.StringTools
import com.google.common.util.concurrent.*
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.progress.Task
import com.intellij.openapi.progress.util.AbstractProgressIndicatorBase
import com.intellij.openapi.project.Project
import com.jetbrains.rd.util.AtomicReference
import com.jetbrains.rd.util.LogLevel
import com.simiacryptus.openai.*
import java.io.IOException
import java.nio.charset.Charset
import java.util.*
import java.util.concurrent.*
import java.util.function.Consumer
import java.util.stream.Collectors

open class AsyncAPI(
    val openAIClient: OpenAIClient,
    private val suppressProgress: Boolean = false
) {

    open fun checkCanceled(indicator: ProgressIndicator, threadRef: AtomicReference<Thread?>) {}

    fun edit(
        project: Project?,
        editRequest: EditRequest
    ): ListenableFuture<CompletionResponse> {
        return map(moderateAsync(project, editRequest.toString())) { _: Any? ->
            try {
                val task: Task.WithResult<CompletionResponse, Exception?> =
                    object : Task.WithResult<CompletionResponse, Exception?>(
                        project,
                        "Text Completion",
                        false
                    ) {
                        override fun compute(indicator: ProgressIndicator): CompletionResponse {
                            try {
                                return openAIClient.edit(editRequest)
                            } catch (e: IOException) {
                                throw RuntimeException(e)
                            } catch (e: InterruptedException) {
                                throw RuntimeException(e)
                            }
                        }
                    }
                if (!suppressProgress) {
                    return@map ProgressManager.getInstance().run(task)
                } else {
                    task.run(AbstractProgressIndicatorBase())
                    return@map task.result
                }
            } catch (e: RuntimeException) {
                throw e
            } catch (e: Exception) {
                throw RuntimeException(e)
            }
        }
    }

    fun complete(
        project: Project?,
        completionRequest: CompletionRequest,
        model: String
    ): ListenableFuture<CompletionResponse> {
        val canBeCancelled =
            true // Cancel doesn't seem to work; the cancel event is only dispatched after the request completes.
        return map(
            moderateAsync(
                project,
                StringTools.restrictCharacterSet(completionRequest.prompt, allowedCharset)
            )
        ) { _: Any? ->
            run(
                task = object : Task.WithResult<CompletionResponse, Exception?>(
                    project,
                    "Text Completion",
                    canBeCancelled
                ) {
                    var threadRef =
                        AtomicReference<Thread?>(null)

                    override fun compute(indicator: ProgressIndicator): CompletionResponse {
                        val cancelMonitor =
                            scheduledPool.scheduleAtFixedRate(
                                { checkCanceled(indicator, threadRef) },
                                0,
                                100,
                                TimeUnit.MILLISECONDS
                            )
                        threadRef.getAndSet(Thread.currentThread())
                        try {
                            return openAIClient.complete(completionRequest, model)
                        } catch (e: IOException) {
                            log.error(e)
                            throw RuntimeException(e)
                        } catch (e: InterruptedException) {
                            throw RuntimeException(e)
                        } finally {
                            threadRef.getAndSet(null)
                            cancelMonitor.cancel(true)
                        }
                    }

                    override fun onCancel() {
                        val thread = threadRef.get()
                        if (null != thread) {
                            log.warn(
                                Arrays.stream(
                                    thread.stackTrace
                                ).map(StackTraceElement::toString)
                                    .collect(Collectors.joining("\n"))
                            )
                            thread.interrupt()
                        }
                        super.onCancel()
                    }
                },
                3
            )
        }
    }


    private fun moderateAsync(project: Project?, text: String): ListenableFuture<*> {
        return run(
            task = object : Task.WithResult<ListenableFuture<*>, Exception?>(project, "Moderation", false) {
                override fun compute(indicator: ProgressIndicator): ListenableFuture<*> {
                    return pool.submit {
                        openAIClient.moderate(text)
                    }
                }
            },
            0
        )
    }

    fun chat(project: Project?, newRequest: ChatRequest, settings: AppSettingsState?): ListenableFuture<ChatResponse> {
        return map(
            moderateAsync(
                project,
                StringTools.restrictCharacterSet(newRequest.messages.map { "${it.role?.name ?: "?"}: ${it.content}" }.joinToString { "\n" }, allowedCharset)
            )
        ) { _: Any? ->
            run(
                task = object : Task.WithResult<ChatResponse, Exception?>(
                    project,
                    "Chat",
                    true
                ) {
                    override fun compute(indicator: ProgressIndicator): ChatResponse {
                        try {
                            newRequest.max_tokens = settings!!.maxTokens
                            newRequest.temperature = settings.temperature
                            newRequest.model = settings.model_chat
                            return openAIClient.chat(newRequest)
                        } catch (e: IOException) {
                            throw RuntimeException(e)
                        } catch (e: InterruptedException) {
                            throw RuntimeException(e)
                        }
                    }
                },
                3
            )
        }
    }

    companion object {
        private val apiThreads = AppSettingsState.instance.apiThreads
        val log = Logger.getInstance(AsyncAPI::class.java)

        fun log(level: LogLevel, msg: String) {
            val message = msg.trim { it <= ' ' }.replace("\n", "\n\t")
            when (level) {
                LogLevel.Error -> OpenAIClient.log.error(message)
                LogLevel.Warn -> OpenAIClient.log.warn(message)
                LogLevel.Info -> OpenAIClient.log.info(message)
                else -> OpenAIClient.log.debug(message)
            }
        }

        fun <I : Any?, O : Any?> map(
            moderateAsync: ListenableFuture<I>,
            o: com.google.common.base.Function<in I, out O>
        ): ListenableFuture<O> = Futures.transform(moderateAsync, o, pool)




        val threadFactory: ThreadFactory = ThreadFactoryBuilder().setNameFormat("API Thread %d").build()
        val pool: ListeningExecutorService = MoreExecutors.listeningDecorator(
            ThreadPoolExecutor(
                apiThreads,
                apiThreads,
                0L, TimeUnit.MILLISECONDS,
                LinkedBlockingQueue(),
                threadFactory,
                ThreadPoolExecutor.AbortPolicy()
            )
        )
        val scheduledPool: ListeningScheduledExecutorService =
            MoreExecutors.listeningDecorator(ScheduledThreadPoolExecutor(1, threadFactory))

        fun <I : Any?> onSuccess(moderateAsync: ListenableFuture<I>, o: Consumer<in I>) {
            Futures.addCallback(moderateAsync, object : FutureCallback<I> {
                override fun onSuccess(result: I) {
                    o.accept(result)
                }

                override fun onFailure(t: Throwable) {
                    UITools.handle(t)
                }
            }, pool)
        }

    }

    val allowedCharset: Charset = Charset.forName("ASCII")

}