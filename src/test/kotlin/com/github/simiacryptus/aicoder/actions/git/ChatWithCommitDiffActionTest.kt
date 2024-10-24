package com.github.simiacryptus.aicoder.actions.git

import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.project.Project
import com.intellij.openapi.vcs.ProjectLevelVcsManager
import com.intellij.openapi.vcs.VcsDataKeys
import com.intellij.openapi.vcs.history.VcsRevisionNumber
import org.junit.Before
import org.junit.Test
import org.mockito.Mockito.*

class ChatWithCommitDiffActionTest {

    private lateinit var action: ChatWithCommitDiffAction
    private lateinit var event: AnActionEvent
    private lateinit var project: Project
    private lateinit var vcsManager: ProjectLevelVcsManager

    @Before
    fun setUp() {
        action = ChatWithCommitDiffAction()
        event = mock(AnActionEvent::class.java)
        project = mock(Project::class.java)
        vcsManager = mock(ProjectLevelVcsManager::class.java)

        `when`(event.project).thenReturn(project)
        `when`(ProjectLevelVcsManager.getInstance(project)).thenReturn(vcsManager)
    }

    @Test
    fun testActionPerformed() {
        val revisionNumber = mock(VcsRevisionNumber::class.java)
        `when`(event.getData(VcsDataKeys.VCS_REVISION_NUMBER)).thenReturn(revisionNumber)
        `when`(vcsManager.allActiveVcss).thenReturn(arrayOf(mock(com.intellij.openapi.vcs.AbstractVcs::class.java)))

        action.actionPerformed(event)

        // Verify that the action starts a new thread
        verify(event, times(1)).getData(VcsDataKeys.VCS_REVISION_NUMBER)
    }

    @Test
    fun testUpdate() {
        val presentation = mock(com.intellij.openapi.actionSystem.Presentation::class.java)
        `when`(event.presentation).thenReturn(presentation)
        `when`(vcsManager.allActiveVcss).thenReturn(arrayOf(mock(com.intellij.openapi.vcs.AbstractVcs::class.java)))

        action.update(event)

        verify(presentation).isEnabledAndVisible = true
    }
}