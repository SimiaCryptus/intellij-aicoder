package aicoder.actions.agent

import java.util.concurrent.Future
import java.util.concurrent.Semaphore
import java.util.concurrent.ThreadPoolExecutor

class FixedConcurrencyProcessor(
  val pool: ThreadPoolExecutor,
  val concurrencyLimit: Int,
  val queue: java.util.concurrent.BlockingQueue<Runnable> = pool.queue
) {
  private val semaphore = Semaphore(concurrencyLimit)
  fun <T> submit(task: () -> T): Future<*> {
    return pool.submit {
      task()
    }
  }
}
