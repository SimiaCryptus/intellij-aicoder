package com.github.simiacryptus.aicoder.config

import java.util.Map.Entry.comparingByValue
import java.util.stream.Collectors

class MRUItems {
    val mostUsedHistory: MutableMap<String, Int> = HashMap()
    private val mostRecentHistory: MutableList<String> = ArrayList()
    private var historyLimit = 10
    fun addInstructionToHistory(instruction: CharSequence) {
        synchronized(mostRecentHistory) {
            if(mostRecentHistory.contains(instruction.toString())) {
                mostRecentHistory.remove(instruction.toString())
            }
            mostRecentHistory.add(instruction.toString())
            while (mostRecentHistory.size > historyLimit) {
                mostRecentHistory.removeAt(0)
            }
        }
        synchronized(mostUsedHistory) {
            mostUsedHistory.put(
                instruction.toString(),
                (mostUsedHistory[instruction] ?: 0) + 1
            )
        }

        if (mostUsedHistory.size > historyLimit) {
            val retain = mostUsedHistory.entries.stream()
                .sorted(comparingByValue<String, Int>().reversed())
                .limit(historyLimit.toLong())
                .map { (key, _) -> key }.collect(
                    Collectors.toList()
                )
            val toRemove = HashSet<CharSequence>(mostUsedHistory.keys)
            toRemove.removeAll(retain.toSet())
            toRemove.removeAll(mostRecentHistory.toSet())
            toRemove.forEach { key: CharSequence? ->
                mostUsedHistory.remove(key)
                mostRecentHistory.remove(key.toString())
            }
        }
    }

}