package com.github.simiacryptus.aicoder.ui

import com.intellij.openapi.editor.Editor
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.wm.StatusBar
import com.intellij.openapi.wm.StatusBarWidget
import org.junit.Before
import org.junit.Test
import org.mockito.Mockito.*

class TokenCountWidgetFactoryTest {

    private lateinit var factory: TokenCountWidgetFactory
    private lateinit var project: Project
    private lateinit var statusBar: StatusBar
    private lateinit var fileEditorManager: FileEditorManager

    @Before
    fun setUp() {
        factory = TokenCountWidgetFactory()
        project = mock(Project::class.java)
        statusBar = mock(StatusBar::class.java)
        fileEditorManager = mock(FileEditorManager::class.java)
        `when`(project.getService(FileEditorManager::class.java)).thenReturn(fileEditorManager)
    }

    @Test
    fun testGetId() {
        assert(factory.getId() == TokenCountWidgetFactory.ID)
    }

    @Test
    fun testCreateWidget() {
        val widget = factory.createWidget(project)
        assert(widget is StatusBarWidget.TextPresentation)
    }

    @Test
    fun testDispose() {
        val widget = mock(StatusBarWidget::class.java)
        factory.disposeWidget(widget)
        // Verify that the widget's dispose method is called
        verify(widget).dispose()
    }

    @Test
    fun testCanBeEnabledOn() {
        assert(factory.canBeEnabledOn(statusBar))
    }

    @Test
    fun testUpdateWidget() {
        val widget = factory.createWidget(project) as TokenCountWidgetFactory.TokenCountWidget
        val editor = mock(Editor::class.java)
        `when`(fileEditorManager.selectedTextEditor).thenReturn(editor)
        
        widget.update()
        
        // Verify that the update method doesn't throw an exception
        // In a real scenario, we would check if the token count is updated correctly
    }
}