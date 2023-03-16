package com.github.simiacryptus.aicoder.openai.async

import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.databind.MapperFeature
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.SerializationFeature
import com.github.simiacryptus.aicoder.config.AppSettingsState
import com.github.simiacryptus.aicoder.openai.core.CompletionRequest
import com.github.simiacryptus.aicoder.openai.core.CompletionResponse
import com.github.simiacryptus.aicoder.openai.core.CoreAPI
import com.github.simiacryptus.aicoder.openai.core.EditRequest
import com.github.simiacryptus.aicoder.util.StringTools
import com.github.simiacryptus.aicoder.util.UITools
import com.google.common.util.concurrent.*
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.progress.Task
import com.intellij.openapi.progress.util.AbstractProgressIndicatorBase
import com.intellij.openapi.project.Project
import com.jetbrains.rd.util.AtomicReference
import com.jetbrains.rd.util.LogLevel
import java.io.IOException
import java.nio.charset.Charset
import java.util.*
import java.util.concurrent.*
import java.util.function.Consumer
import java.util.stream.Collectors

open class AsyncAPI(
    val coreAPI: CoreAPI,
    private val suppressProgress: Boolean = false
) {

    open fun checkCanceled(indicator: ProgressIndicator, threadRef: AtomicReference<Thread?>) {}

    fun edit(
        project: Project?,
        editRequest: EditRequest,
        settings: AppSettingsState
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
                                if (editRequest.input == null) {
                                    log(
                                        settings.apiLogLevel, String.format(
                                            "Text Edit Request\nInstruction:\n\t%s\n",
                                            editRequest.instruction.replace("\n", "\n\t")
                                        )
                                    )
                                } else {
                                    log(
                                        settings.apiLogLevel, String.format(
                                            "Text Edit Request\nInstruction:\n\t%s\nInput:\n\t%s\n",
                                            editRequest.instruction.replace("\n", "\n\t"),
                                            editRequest.input!!.replace("\n", "\n\t")
                                        )
                                    )
                                }
                                val request: String =
                                    StringTools.restrictCharacterSet(
                                        mapper.writeValueAsString(editRequest),
                                        allowedCharset
                                    )
                                val result = coreAPI.post(settings.apiBase + "/edits", request)
                                val completionResponse = coreAPI.processCompletionResponse(result)
                                coreAPI.logComplete(
                                    completionResponse.firstChoice.orElse("").toString().trim { it <= ' ' }
                                )
                                return completionResponse
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
                object : Task.WithResult<CompletionResponse, Exception?>(
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
                            return coreAPI.complete(completionRequest, model)
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
            object : Task.WithResult<ListenableFuture<*>, Exception?>(project, "Moderation", false) {
                override fun compute(indicator: ProgressIndicator): ListenableFuture<*> {
                    return pool.submit {
                        coreAPI.moderate(text)
                    }
                }
            },
            0
        )
    }

    fun <T> run(task: Task.WithResult<T, Exception?>, retries: Int): T {
        return try {
            if (!suppressProgress) {
                ProgressManager.getInstance().run(task)
            } else {
                task.run(AbstractProgressIndicatorBase())
                task.result
            }
        } catch (e: RuntimeException) {
            if (isInterruptedException(e)) throw e
            if (retries > 0) {
                log.warn("Retrying request", e)
                run(task, retries - 1)
            } else {
                throw e
            }
        } catch (e: InterruptedException) {
            throw RuntimeException(e)
        } catch (e: Exception) {
            if (isInterruptedException(e)) throw RuntimeException(e)
            if (retries > 0) {
                log.warn("Retrying request", e)
                try {
                    Thread.sleep(15000)
                } catch (ex: InterruptedException) {
                    Thread.currentThread().interrupt()
                }
                run(task, retries - 1)
            } else {
                throw RuntimeException(e)
            }
        }
    }

    private fun isInterruptedException(e: Throwable?): Boolean {
        if (e is InterruptedException) return true
        return if (e!!.cause != null && e.cause !== e) isInterruptedException(
            e.cause
        ) else false
    }

    companion object {
        private val apiThreads = AppSettingsState.instance.apiThreads
        val log = Logger.getInstance(AsyncAPI::class.java)

        fun log(level: LogLevel, msg: String) {
            val message = msg.trim { it <= ' ' }.replace("\n", "\n\t")
            when (level) {
                LogLevel.Error -> CoreAPI.log.error(message)
                LogLevel.Warn -> CoreAPI.log.warn(message)
                LogLevel.Info -> CoreAPI.log.info(message)
                else -> CoreAPI.log.debug(message)
            }
        }

        fun <I : Any?, O : Any?> map(
            moderateAsync: ListenableFuture<I>,
            o: com.google.common.base.Function<in I, out O>
        ): ListenableFuture<O> = Futures.transform(moderateAsync, o, pool)

        val mapper: ObjectMapper
            get() {
                val mapper = ObjectMapper()
                mapper
                    .enable(SerializationFeature.INDENT_OUTPUT)
                    .enable(SerializationFeature.ORDER_MAP_ENTRIES_BY_KEYS)
                    .enable(MapperFeature.USE_STD_BEAN_NAMING)
                    .setSerializationInclusion(JsonInclude.Include.NON_NULL)
                    .activateDefaultTyping(mapper.polymorphicTypeValidator)
                return mapper
            }

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