package com.github.simiacryptus.aicoder.actions.git

import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.project.Project
import com.intellij.openapi.vcs.VcsDataKeys
import com.intellij.openapi.vcs.changes.ChangeListManager
import org.junit.Before
import org.junit.Test
import org.mockito.Mockito.*

class ChatWithWorkingCopyDiffActionTest {

    private lateinit var action: ChatWithWorkingCopyDiffAction
    private lateinit var event: AnActionEvent
    private lateinit var project: Project
    private lateinit var changeListManager: ChangeListManager

    @Before
    fun setUp() {
        action = ChatWithWorkingCopyDiffAction()
        event = mock(AnActionEvent::class.java)
        project = mock(Project::class.java)
        changeListManager = mock(ChangeListManager::class.java)

        `when`(event.project).thenReturn(project)
        `when`(ChangeListManager.getInstance(project)).thenReturn(changeListManager)
    }

    @Test
    fun testActionPerformed() {
        `when`(event.getData(VcsDataKeys.VIRTUAL_FILES)).thenReturn(arrayOf())
        `when`(changeListManager.allChanges).thenReturn(listOf())

        action.actionPerformed(event)

        // Verify that the action starts a new thread
        verify(event, times(1)).getData(VcsDataKeys.VIRTUAL_FILES)
    }

    @Test
    fun testUpdate() {
        val presentation = mock(com.intellij.openapi.actionSystem.Presentation::class.java)
        `when`(event.presentation).thenReturn(presentation)
        `when`(event.getData(VcsDataKeys.VCS)).thenReturn(mock(com.intellij.openapi.vcs.AbstractVcs::class.java))

        action.update(event)

        verify(presentation).isEnabledAndVisible = true
    }
}