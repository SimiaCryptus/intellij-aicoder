package com.github.simiacryptus.openai

import com.google.common.util.concurrent.ListeningExecutorService
import com.google.common.util.concurrent.ListeningScheduledExecutorService
import com.google.common.util.concurrent.MoreExecutors
import com.google.common.util.concurrent.ThreadFactoryBuilder
import com.intellij.openapi.diagnostic.Logger
import org.apache.http.impl.client.CloseableHttpClient
import org.apache.http.impl.client.HttpClientBuilder
import java.io.IOException
import java.time.Duration
import java.util.*
import java.util.concurrent.*
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.math.pow

@Suppress("MemberVisibilityCanBePrivate")
open class HttpClientManager {

    companion object {
        val log = Logger.getInstance(OpenAIClient::class.java)
        val threadFactory: ThreadFactory = ThreadFactoryBuilder().setNameFormat("API Thread %d").build()
        val scheduledPool: ListeningScheduledExecutorService =
            MoreExecutors.listeningDecorator(ScheduledThreadPoolExecutor(4, threadFactory))
        val workPool: ListeningExecutorService =
            MoreExecutors.listeningDecorator(
                ThreadPoolExecutor(
                    1, 32,
                    0, TimeUnit.MILLISECONDS, LinkedBlockingQueue(), threadFactory
                )
            )

        fun <T> withPool(fn: () -> T): T = workPool.submit(Callable {
            return@Callable fn()
        }).get()

        fun <T> withExpBackoffRetry(retryCount: Int = 3, sleepScale: Long = 1000L, fn: () -> T): T {
            var lastException: Exception? = null
            for (i in 0 until retryCount) {
                try {
                    return fn()
                } catch (e: Exception) {
                    lastException = e
                    log.info("Request failed; retrying ($i/$retryCount): " + e.message)
                    Thread.sleep(sleepScale * 2.0.pow(i.toDouble()).toLong())
                }
            }
            throw lastException!!
        }

    }

    protected val clients: MutableMap<Thread, CloseableHttpClient> = WeakHashMap()
    fun getClient(thread: Thread = Thread.currentThread()): CloseableHttpClient =
        if (thread in clients) clients[thread]!!
        else synchronized(clients) {
            val client = HttpClientBuilder.create().build()
            clients[thread] = client
            client
        }

    fun closeClient(thread: Thread) {
        try {
            synchronized(clients) {
                clients[thread]
            }?.close()
        } catch (e: IOException) {
            log.info("Error closing client: " + e.message)
        }
    }

    fun <T> withCancellationMonitor(fn: () -> T, cancelCheck: () -> Boolean = { Thread.currentThread().isInterrupted }): T {
        val thread = Thread.currentThread()
        val isCompleted = AtomicBoolean(false)
        val start = Date()
        val future = scheduledPool.scheduleAtFixedRate({
            if (cancelCheck()) {
                log.info("Request cancelled at ${Date()} (started $start); closing client for thread $thread")
                closeClient(thread, isCompleted)
            }
        }, 0, 10, TimeUnit.MILLISECONDS)
        try {
            return fn()
        } finally {
            isCompleted.set(true)
            future.cancel(false)
        }
    }

    fun <T> withTimeout(duration: Duration, fn: () -> T): T {
        val thread = Thread.currentThread()
        val isCompleted = AtomicBoolean(false)
        val start = Date()
        val future = scheduledPool.schedule({
            log.info("Request timed out after $duration at ${Date()} (started $start); closing client for thread $thread")
            closeClient(thread, isCompleted)
        }, duration.toMillis(), TimeUnit.MILLISECONDS)
        try {
            return fn()
        } finally {
            isCompleted.set(true)
            future.cancel(false)
        }
    }

    private fun closeClient(thread: Thread, isCompleted: AtomicBoolean) {
        closeClient(thread)
        Thread.sleep(10)
        while (isCompleted.get()) {
            Thread.sleep(5000)
            if (isCompleted.get()) break
            log.info("Request still not completed; thread stack: \n\t${thread.stackTrace.joinToString { "\n\t" }}\nkilling thread $thread")
            @Suppress("DEPRECATION")
            thread.stop()
        }
    }

    fun <T> withReliability(requestTimeoutSeconds: Long = 180, retryCount: Int = 3, fn: () -> T): T =
        withExpBackoffRetry(retryCount) {
//            withPool {
//            }
            withTimeout(Duration.ofSeconds(requestTimeoutSeconds)) {
                withCancellationMonitor(fn)
            }
        }

    fun <T> withPerformanceLogging(fn: () -> T):T {
        val start = Date()
        try {
            return fn()
        } finally {
            log.debug("Request completed in ${Date().time - start.time}ms")
        }
    }



}