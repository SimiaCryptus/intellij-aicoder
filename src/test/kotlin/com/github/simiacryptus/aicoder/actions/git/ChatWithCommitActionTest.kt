package com.github.simiacryptus.aicoder.actions.git

import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.project.Project
import com.intellij.openapi.vcs.VcsDataKeys
import com.intellij.openapi.vcs.changes.Change
import com.intellij.openapi.vfs.VirtualFile
import org.junit.Before
import org.junit.Test
import org.mockito.Mockito.*

class ChatWithCommitActionTest {

    private lateinit var action: ChatWithCommitAction
    private lateinit var event: AnActionEvent
    private lateinit var project: Project

    @Before
    fun setUp() {
        action = ChatWithCommitAction()
        event = mock(AnActionEvent::class.java)
        project = mock(Project::class.java)
        `when`(event.project).thenReturn(project)
    }

    @Test
    fun testActionPerformed() {
        val virtualFiles = arrayOf(mock(VirtualFile::class.java))
        val changes = arrayOf(mock(Change::class.java))

        `when`(event.getData(VcsDataKeys.VIRTUAL_FILE_ARRAY)).thenReturn(virtualFiles)
        `when`(event.getData(VcsDataKeys.CHANGES)).thenReturn(changes)

        action.actionPerformed(event)

        // Verify that the action starts a new thread
        // Note: This is a basic test and doesn't verify the thread's behavior
        verify(event, times(1)).getData(VcsDataKeys.VIRTUAL_FILE_ARRAY)
        verify(event, times(1)).getData(VcsDataKeys.CHANGES)
    }

    @Test
    fun testUpdate() {
        val presentation = mock(com.intellij.openapi.actionSystem.Presentation::class.java)
        `when`(event.presentation).thenReturn(presentation)

        action.update(event)

        verify(presentation).isEnabledAndVisible = true
    }
}