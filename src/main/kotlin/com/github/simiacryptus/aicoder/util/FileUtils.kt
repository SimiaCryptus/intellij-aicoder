package com.github.simiacryptus.aicoder.util

import com.intellij.openapi.vfs.VirtualFile

fun VirtualFile.findRecursively(predicate: (VirtualFile) -> Boolean): List<VirtualFile> {
    val results = mutableListOf<VirtualFile>()
    if (this.isDirectory) {
        this.children?.forEach { child ->
            if (child.isDirectory) {
                results.addAll(child.findRecursively(predicate))
            } else if (predicate(child)) {
                results.add(child)
            }
        }
    } else if (predicate(this)) {
        results.add(this)
    }
    return results
}