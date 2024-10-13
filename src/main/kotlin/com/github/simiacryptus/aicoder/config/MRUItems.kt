package com.github.simiacryptus.aicoder.config

import com.fasterxml.jackson.annotation.JsonIgnore
import kotlin.math.min
import java.io.Serializable
import java.time.Instant
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.locks.ReentrantReadWriteLock
import kotlin.concurrent.read
import kotlin.concurrent.write

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

    @JsonIgnore
    fun getMostRecentWithTimestamp(limit: Int = DEFAULT_LIMIT): List<Pair<String, Instant>> {
        return lock.read {
            history.take(min(limit, historyLimit)).map { Pair(it.instruction, it.lastUsed) }
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

    @JsonIgnore
    fun getUsageCount(item: String): Int {
        return lock.read { history.find { it.instruction == item }?.usageCount ?: 0 }
    }

    @JsonIgnore
    fun getLastUsedTimestamp(item: String): Instant? {
        return lock.read { history.find { it.instruction == item }?.lastUsed }
    }

    fun merge(other: MRUItems) {
        lock.write {
            other.history.forEach { otherItem ->
                val existingItem = history.find { it.instruction == otherItem.instruction }
                if (existingItem != null) {
                    existingItem.usageCount += otherItem.usageCount
                    existingItem.lastUsed = maxOf(existingItem.lastUsed, otherItem.lastUsed)
                } else {
                    history.add(otherItem)
                }
            }
            history.sortByDescending { it.lastUsed }
            trimHistories()
        }
    }

    fun removeOlderThan(timestamp: Instant) {
        lock.write {
            history.removeAll { it.lastUsed.isBefore(timestamp) }
        }
    }

    override fun toString(): String {
        return lock.read {
            "MRUItems(mostUsed=${getMostUsed(5)}, mostRecent=${getMostRecent(5)}, size=${history.size})"
        }
    }

}