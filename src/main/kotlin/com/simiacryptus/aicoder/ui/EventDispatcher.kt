package com.simiacryptus.aicoder.ui

open class EventDispatcher {
  private val listeners = mutableListOf<() -> Unit>()
  fun addListener(listener: () -> Unit) {
    listeners.add(listener)
  }

  fun removeListener(listener: () -> Unit) {
    listeners.remove(listener)
  }

  fun notifyListeners() {
    listeners.forEach { it() }
  }
}