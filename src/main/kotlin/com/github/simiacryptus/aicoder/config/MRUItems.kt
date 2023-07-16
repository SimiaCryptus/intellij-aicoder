package com.github.simiacryptus.aicoder.config

import java.util.ArrayList
import java.util.HashMap
import java.util.HashSet
import java.util.Map
import java.util.stream.Collectors

class MRUItems {
    val mostUsedHistory: MutableMap<String, Int> = HashMap()
    val mostRecentHistory: MutableList<String> = ArrayList()
    var historyLimit = 10
    fun addInstructionToHistory(instruction: CharSequence) {
        synchronized(mostRecentHistory) {
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
                .sorted(Map.Entry.comparingByValue<String, Int>().reversed())
                .limit(historyLimit.toLong())
                .map { (key, _) -> key }.collect(
                    Collectors.toList()
                )
            val toRemove = HashSet<CharSequence>(mostUsedHistory.keys)
            toRemove.removeAll(retain.toSet())
            toRemove.removeAll(mostRecentHistory.toSet())
            toRemove.forEach { key: CharSequence? ->
                mostUsedHistory.remove(
                    key
                )
            }
        }
    }

}