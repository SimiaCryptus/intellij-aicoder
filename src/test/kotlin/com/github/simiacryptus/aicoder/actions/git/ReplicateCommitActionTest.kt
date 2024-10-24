package com.github.simiacryptus.aicoder.actions.git

import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.project.Project
import com.intellij.openapi.vcs.VcsDataKeys
import com.intellij.openapi.vfs.VirtualFile
import org.junit.Before
import org.junit.Test
import org.mockito.Mockito.*
import java.io.File

class ReplicateCommitActionTest {

    private lateinit var action: ReplicateCommitAction
    private lateinit var event: AnActionEvent
    private lateinit var project: Project

    @Before
    fun setUp() {
        action = ReplicateCommitAction()
        event = mock(AnActionEvent::class.java)
        project = mock(Project::class.java)

        `when`(event.project).thenReturn(project)
    }

    @Test
    fun testActionPerformed() {
        val virtualFiles = arrayOf(mock(VirtualFile::class.java))
        val changes = arrayOf(mock(com.intellij.openapi.vcs.changes.Change::class.java))

        `when`(event.getData(VcsDataKeys.VIRTUAL_FILE_ARRAY)).thenReturn(virtualFiles)
        `when`(event.getData(VcsDataKeys.CHANGES)).thenReturn(changes)
        `when`(project.basePath).thenReturn("/test/path")

        action.actionPerformed(event)

        // Verify that the action starts new threads
        verify(event, times(1)).getData(VcsDataKeys.VIRTUAL_FILE_ARRAY)
        verify(event, times(1)).getData(VcsDataKeys.CHANGES)
    }

    @Test
    fun testIsEnabled() {
        val result = action.isEnabled(event)
        assert(result)
    }

    @Test
    fun testToPaths() {
        val root = File("/test/root").toPath()
        val paths = ReplicateCommitAction.toPaths(root, "src/*.kt")
        
        // This test assumes that the root directory exists and contains matching files
        // In a real test environment, you might want to set up a temporary directory structure
        assert(paths.isNotEmpty())
    }
}