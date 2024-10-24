package com.github.simiacryptus.aicoder.ui

import com.intellij.openapi.project.Project
import com.intellij.openapi.wm.StatusBar
import com.intellij.openapi.wm.StatusBarWidget
import org.junit.Before
import org.junit.Test
import org.mockito.Mockito.*

class SettingsWidgetFactoryTest {

    private lateinit var factory: SettingsWidgetFactory
    private lateinit var project: Project
    private lateinit var statusBar: StatusBar

    @Before
    fun setUp() {
        factory = SettingsWidgetFactory()
        project = mock(Project::class.java)
        statusBar = mock(StatusBar::class.java)
    }

    @Test
    fun testGetId() {
        assert(factory.getId() == SettingsWidgetFactory.ID)
    }

    @Test
    fun testCreateWidget() {
        val widget = factory.createWidget(project)
        assert(widget is StatusBarWidget.TextPresentation)
    }

    @Test
    fun testDispose() {
        // This method is empty in the original class, but we can test that it doesn't throw an exception
        factory.disposeWidget(mock(StatusBarWidget::class.java))
    }

    @Test
    fun testCanBeEnabledOn() {
        assert(factory.canBeEnabledOn(statusBar))
    }
}