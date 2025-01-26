package com.simiacryptus.aicoder.config

import com.fasterxml.jackson.annotation.JsonIgnore
import java.io.Serializable
import java.time.Instant
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.locks.ReentrantReadWriteLock
import kotlin.concurrent.read
import kotlin.concurrent.write
import kotlin.math.min

class MRUItems : Serializable {

  companion object {
    const val DEFAULT_LIMIT = 10
  }

  data class HistoryItem(val instruction: String, var usageCount: Int, var lastUsed: Instant) : Serializable

  val history: MutableList<HistoryItem> = CopyOnWriteArrayList()

  private val lock = ReentrantReadWriteLock()

  var historyLimit = DEFAULT_LIMIT
    set(value) {
      require(value > 0) { "History limit must be positive" }
      lock.write {
        field = value
        trimHistories()
      }
    }

  override fun equals(other: Any?): Boolean {
    return other is MRUItems && history == other.history
  }

  override fun hashCode(): Int {
    return history.hashCode()
  }

  fun addInstructionToHistory(instruction: CharSequence) {
    lock.write {
      val instructionStr = instruction.toString()
      val existingItem = history.find { it.instruction == instructionStr }
      if (existingItem != null) {
        existingItem.usageCount++
        existingItem.lastUsed = Instant.now()
        history.remove(existingItem)
        history.add(0, existingItem)
      } else {
        history.add(0, HistoryItem(instructionStr, 1, Instant.now()))
      }
      trimHistories()
    }
  }

  @JsonIgnore
  fun getMostUsed(limit: Int = DEFAULT_LIMIT): List<String> {
    return lock.read {
      history
        .sortedByDescending { it.usageCount }
        .take(min(limit, historyLimit))
        .map { it.instruction }
    }
  }

  @JsonIgnore
  fun getMostRecent(limit: Int = DEFAULT_LIMIT): List<String> {
    return lock.read {
      history.take(min(limit, historyLimit)).map { it.instruction }
    }
  }

  fun clear() {
    lock.write {
      history.clear()
    }
  }

  fun size(): Int = lock.read { history.size }

  fun isEmpty(): Boolean = lock.read { history.isEmpty() }

  fun remove(item: String) {
    lock.write {
      history.removeIf { it.instruction == item }
    }
  }

  private fun trimHistories() {
    lock.write {
      if (history.size > historyLimit) {
        history.subList(historyLimit, history.size).clear()
      }
    }
  }

  fun contains(item: String): Boolean {
    return lock.read {
      history.any { it.instruction == item }
    }
  }

  override fun toString(): String {
    return lock.read {
      "MRUItems(mostUsed=${getMostUsed(5)}, mostRecent=${getMostRecent(5)}, size=${history.size})"
    }
  }

}