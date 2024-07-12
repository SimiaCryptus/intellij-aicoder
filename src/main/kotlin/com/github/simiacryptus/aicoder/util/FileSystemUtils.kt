package com.github.simiacryptus.aicoder.util

import com.intellij.openapi.vfs.VirtualFile
import java.io.File
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.util.*

object FileSystemUtils {

    fun getFiles(virtualFiles: Array<out VirtualFile>?): MutableSet<Path> {
        val paths = mutableSetOf<Path>()
        virtualFiles?.forEach { virtualFile ->
            collectFiles(virtualFile, paths)
        }
        return paths
    }

    fun getFiles(paths: Array<out Path>?): MutableSet<Path> {
        val result = mutableSetOf<Path>()
        paths?.forEach { path ->
            collectFiles(path, result)
        }
        return result
    }

    private fun collectFiles(virtualFile: VirtualFile, paths: MutableSet<Path>) {
        if (isGitignore(virtualFile) || virtualFile.name.startsWith(".")) return
        if (virtualFile.isDirectory) {
            virtualFile.children.forEach { child ->
                collectFiles(child, paths)
            }
        } else {
            paths.add(Paths.get(virtualFile.path))
        }
    }

    private fun collectFiles(path: Path, paths: MutableSet<Path>) {
        if (isGitignore(path) || path.fileName.toString().startsWith(".")) return
        if (Files.isDirectory(path)) {
            Files.newDirectoryStream(path).use { stream ->
                stream.forEach { child ->
                    collectFiles(child, paths)
                }
            }
        } else {
            paths.add(path)
        }
    }

    fun expand(data: Array<VirtualFile>?): Array<VirtualFile>? {
        val result = mutableListOf<VirtualFile>()
        data?.forEach { virtualFile ->
            expandVirtualFile(virtualFile, result)
        }
        return result.toTypedArray()
    }

    private fun expandVirtualFile(virtualFile: VirtualFile, result: MutableList<VirtualFile>) {
        if (virtualFile.isDirectory) {
            virtualFile.children.forEach { child ->
                expandVirtualFile(child, result)
            }
        } else {
            result.add(virtualFile)
        }
    }

    fun findGitRoot(path: Path?): Path? {
        var currentPath = path
        while (currentPath != null && !Files.exists(currentPath.resolve(".git"))) {
            currentPath = currentPath.parent
        }
        return currentPath
    }

    fun findGitRoot(virtualFile: VirtualFile?): VirtualFile? {
        var currentFile = virtualFile
        while (currentFile != null && currentFile.findChild(".git") == null) {
            currentFile = currentFile.parent
        }
        return currentFile
    }

    fun getProjectStructure(projectPath: VirtualFile?): String {
        return buildString {
            projectPath?.let { appendProjectStructure(it, "", this) }
        }
    }

    fun getProjectStructure(root: Path): String {
        return buildString {
            appendProjectStructure(root, "", this)
        }
    }

    private fun appendProjectStructure(virtualFile: VirtualFile, indent: String, builder: StringBuilder) {
        builder.append(indent).append(virtualFile.name).append("\n")
        if (virtualFile.isDirectory) {
            virtualFile.children.forEach { child ->
                appendProjectStructure(child, "$indent  ", builder)
            }
        }
    }

    private fun appendProjectStructure(path: Path, indent: String, builder: StringBuilder) {
        builder.append(indent).append(path.fileName).append("\n")
        if (Files.isDirectory(path)) {
            Files.newDirectoryStream(path).use { stream ->
                stream.forEach { child ->
                    appendProjectStructure(child, "$indent  ", builder)
                }
            }
        }
    }

    fun filteredWalk(file: File, fn: (File) -> Boolean) : List<File> {
        val result = mutableListOf<File>()
        if (fn(file)) {
            if (file.isDirectory) {
                file.listFiles()?.forEach { child ->
                    result.addAll(filteredWalk(child, fn))
                }
            } else {
                result.add(file)
            }
        }
        return result
    }

    fun isLLMIncludable(file: File) : Boolean {
        return when {
            !file.exists() -> false
            file.isDirectory -> false
            file.name.startsWith(".") -> false
            file.length() > (256 * 1024) -> false
            isGitignore(file.toPath()) -> false
            file.extension?.lowercase(Locale.getDefault()) in setOf(
                "jar",
                "zip",
                "class",
                "png",
                "jpg",
                "jpeg",
                "gif",
                "ico",
                "stl"
            ) -> false
            else -> true
        }
    }

    fun expandFileList(data: Array<VirtualFile>): Array<VirtualFile> {
        return data.flatMap {
            (when {
                it.name.startsWith(".") -> arrayOf()
                isGitignore(it) -> arrayOf()
                it.length > 1e6 -> arrayOf()
                it.extension?.lowercase(Locale.getDefault()) in
                        setOf("jar", "zip", "class", "png", "jpg", "jpeg", "gif", "ico") -> arrayOf()

                it.isDirectory -> expandFileList(it.children)
                else -> arrayOf(it)
            }).toList()
        }.toTypedArray()
    }

    fun isGitignore(file: VirtualFile) = isGitignore(file.toNioPath())

    fun isGitignore(path: Path): Boolean {
        var currentDir = path.toFile().parentFile
        currentDir ?: return false
        while (!currentDir.resolve(".git").exists()) {
            currentDir.resolve(".gitignore").let {
                if (it.exists()) {
                    val gitignore = it.readText()
                    if (gitignore.split("\n").any { line ->
                            val pattern = line.trim().trimEnd('/').replace(".", "\\.").replace("*", ".*")
                            line.trim().isNotEmpty()
                                    && !line.startsWith("#")
                                    && path.fileName.toString().trimEnd('/').matches(Regex(pattern))
                        }) {
                        return true
                    }
                }
            }
            currentDir = currentDir.parentFile ?: return false
        }
        currentDir.resolve(".gitignore").let {
            if (it.exists()) {
                val gitignore = it.readText()
                if (gitignore.split("\n").any { line ->
                        val pattern = line.trim().trimEnd('/').replace(".", "\\.").replace("*", ".*")
                        line.trim().isNotEmpty()
                                && !line.startsWith("#")
                                && path.fileName.toString().trimEnd('/').matches(Regex(pattern))
                    }) {
                    return true
                }
            }
        }
        return false
    }

    fun toPaths(root: Path, it: String): Iterable<Path> {
        // Implement logic to handle wildcard expansion and resolve relative paths against the root
        return listOf()
    }
}
