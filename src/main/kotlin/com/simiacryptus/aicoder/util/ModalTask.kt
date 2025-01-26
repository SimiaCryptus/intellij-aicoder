package com.simiacryptus.aicoder.util

import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.progress.Task
import com.intellij.openapi.project.Project
import org.slf4j.LoggerFactory
import java.util.*
import java.util.concurrent.Semaphore
import java.util.concurrent.TimeUnit
import java.util.concurrent.TimeoutException
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicReference
import java.util.function.Supplier

class ModalTask<T>(
  project: Project, title: String, canBeCancelled: Boolean, val task: (ProgressIndicator) -> T
) : Task.WithResult<T, Exception>(project, title, canBeCancelled), Supplier<T> {
  private val taskLog = LoggerFactory.getLogger(ModalTask::class.java)
  private val result = AtomicReference<T>()
  private val isError = AtomicBoolean(false)
  private val error = AtomicReference<Throwable>()
  private val semaphore = Semaphore(0)
  private val completed = AtomicBoolean(false)
  private val threadList = Collections.synchronizedList(ArrayList<Thread>())
  private val cancelled = AtomicBoolean(false)
  private val started = AtomicBoolean(false)
  private val lock = Object()

  override fun compute(indicator: ProgressIndicator): T? {
    taskLog.debug("Starting compute() for ModalTask: $title")
    synchronized(lock) {
      taskLog.debug("Checking task state - started: ${started.get()}, completed: ${completed.get()}, cancelled: ${cancelled.get()}")
      if (!started.compareAndSet(false, true)) return null
      if (completed.get() || cancelled.get()) {
        taskLog.debug("Task already completed or cancelled, releasing semaphore")
        semaphore.release()
        return null
      }
    }
    val currentThread = Thread.currentThread()
    taskLog.debug("Adding thread ${currentThread.name} to threadList")
    threadList.add(currentThread)
    val scheduledFuture = UITools.scheduledPool.scheduleAtFixedRate({
      if (indicator.isCanceled) {
        taskLog.debug("Indicator cancelled, interrupting threads")
        cancelled.set(true)
        threadList.forEach { it.interrupt() }
      }
    }, 0, 1, TimeUnit.SECONDS)
    return try {
      synchronized(lock) {
        if (completed.get() || cancelled.get()) {
          taskLog.debug("Task completed or cancelled during execution")
          semaphore.release()
          return null
        }
      }
      taskLog.debug("Executing task")
      result.set(task(indicator))
      taskLog.debug("Task completed successfully")
      result.get()
    } catch (e: Throwable) {
      taskLog.error("Error executing task", e)
      log.info("Error running task", e)
      isError.set(true)
      error.set(e)
      null
    } finally {
      synchronized(lock) {
        taskLog.debug("Finalizing task execution")
        completed.set(true)
        semaphore.release()
      }
      taskLog.debug("Removing thread ${currentThread.name} from threadList")
      threadList.remove(currentThread)
      scheduledFuture.cancel(true)
    }
  }

  override fun get(): T {
    taskLog.debug("Attempting to get task result")
    try {
      val acquired = semaphore.tryAcquire(30, TimeUnit.SECONDS)
      taskLog.debug("Semaphore acquired: $acquired")
      synchronized(lock) {
        if (!started.get() || !acquired) {
          taskLog.error("Task timed out or never started")
          cancelled.set(true)
          throw TimeoutException("Task timed out after 30 seconds")
        }
      }
    } finally {
      semaphore.release()
    }
    synchronized(lock) {
      taskLog.debug("Checking final task state - completed: ${completed.get()}, error: ${isError.get()}, cancelled: ${cancelled.get()}")
      if (!completed.get()) {
        throw IllegalStateException(
          "Task not completed" +
              (if (cancelled.get()) " (cancelled)" else "")
        )
      }
      if (isError.get()) {
        val e = error.get() ?: RuntimeException("Unknown error occurred")
        taskLog.error("Task failed with error", e)
        throw e
      }
      if (cancelled.get()) {
        taskLog.debug("Task was cancelled")
        throw InterruptedException("Task was cancelled")
      }
      taskLog.debug("Returning successful task result")
      return result.get() ?: throw IllegalStateException("No result available")
    }
  }

  override fun onCancel() {
    taskLog.debug("Task cancelled")
    super.onCancel()
    synchronized(lock) {
      cancelled.set(true)
      threadList.forEach { it.interrupt() }
      semaphore.release()
    }
  }

  companion object {
    private val log = LoggerFactory.getLogger(ModalTask::class.java)
  }

}