package aicoder.actions.agent

import java.util.concurrent.Future
import java.util.concurrent.Semaphore
import java.util.concurrent.ThreadPoolExecutor

class FixedConcurrencyProcessor(
  val pool: ThreadPoolExecutor,
  val concurrencyLimit: Int
) {
  private val semaphore = Semaphore(concurrencyLimit)
  fun <T> submit(task: () -> T): Future<T> {
  
  }
}
