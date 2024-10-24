package com.github.simiacryptus.aicoder.actions.problems

import com.intellij.analysis.problemsView.toolWindow.ProblemNode
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.actionSystem.PlatformDataKeys
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import org.junit.Before
import org.junit.Test
import org.mockito.Mockito.*

class AnalyzeProblemActionTest {

    private lateinit var action: AnalyzeProblemAction
    private lateinit var event: AnActionEvent
    private lateinit var project: Project
    private lateinit var virtualFile: VirtualFile
    private lateinit var problemNode: ProblemNode

    @Before
    fun setUp() {
        action = AnalyzeProblemAction()
        event = mock(AnActionEvent::class.java)
        project = mock(Project::class.java)
        virtualFile = mock(VirtualFile::class.java)
        problemNode = mock(ProblemNode::class.java)

        `when`(event.project).thenReturn(project)
        `when`(event.getData(CommonDataKeys.VIRTUAL_FILE)).thenReturn(virtualFile)
        `when`(event.getData(PlatformDataKeys.SELECTED_ITEM)).thenReturn(problemNode)
    }

    @Test
    fun testActionPerformed() {
        // Mock necessary components
        `when`(virtualFile.path).thenReturn("/test/path/file.kt")
        `when`(problemNode.text).thenReturn("Test problem")
        `when`(problemNode.line).thenReturn(10)
        `when`(problemNode.column).thenReturn(5)

        // Execute the action
        action.actionPerformed(event)

        // Verify that the action starts a new thread
        // Note: This is a basic test and doesn't verify the thread's behavior
        verify(event, times(1)).getData(CommonDataKeys.VIRTUAL_FILE)
        verify(event, times(1)).getData(PlatformDataKeys.SELECTED_ITEM)
    }

    @Test
    fun testUpdateEnabled() {
        val presentation = mock(com.intellij.openapi.actionSystem.Presentation::class.java)
        `when`(event.presentation).thenReturn(presentation)

        action.update(event)

        verify(presentation).isEnabledAndVisible = true
    }

    @Test
    fun testUpdateDisabled() {
        val presentation = mock(com.intellij.openapi.actionSystem.Presentation::class.java)
        `when`(event.presentation).thenReturn(presentation)
        `when`(event.project).thenReturn(null)

        action.update(event)

        verify(presentation).isEnabledAndVisible = false
    }
}