package com.simiacryptus.aicoder.util

import com.intellij.openapi.vfs.VirtualFile

fun VirtualFile.findRecursively(deadline: Long = System.currentTimeMillis() + 100, predicate: (VirtualFile) -> Boolean): List<VirtualFile> {
  val results = mutableListOf<VirtualFile>()
  when {
    System.currentTimeMillis() > deadline -> return results
    this.isDirectory -> {
      val children = this.children
      children?.forEach { child ->
        when {
          System.currentTimeMillis() > deadline -> return results
          child.isDirectory -> results.addAll(child.findRecursively(deadline, predicate))
          predicate(child) -> results.add(child)
        }
      }
    }

    predicate(this) -> {
      results.add(this)
    }
  }
  return results
}