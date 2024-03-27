package com.github.simiacryptus.aicoder.util

import javax.swing.JCheckBox
import javax.swing.JPanel

/**
 * Manages tasks represented as checkboxes within a UI component.
 */
class CheckboxTaskManager {
    private val taskCheckboxes: MutableMap<String, JCheckBox> = mutableMapOf()

    /**
     * Adds a task with a unique identifier.
     */
    fun addTask(taskId: String, taskLabel: String) {
        val checkBox = JCheckBox(taskLabel)
        taskCheckboxes[taskId] = checkBox
        // Additional setup for the checkbox can be added here
    }

    /**
     * Removes a task by its unique identifier.
     */
    fun removeTask(taskId: String) {
        taskCheckboxes.remove(taskId)
        // Additional cleanup for the task can be added here
    }

    /**
     * Updates the UI component to display the current tasks.
     * @param panel The JPanel to which the checkboxes should be added.
     */
    fun updateUI(panel: JPanel) {
        panel.removeAll()
        taskCheckboxes.values.forEach { checkBox ->
            panel.add(checkBox)
        }
        panel.revalidate()
        panel.repaint()
    }

    /**
     * Retrieves the state of a specific task.
     * @return Boolean indicating whether the task is checked or not.
     */
    fun isTaskChecked(taskId: String): Boolean {
        return taskCheckboxes[taskId]?.isSelected ?: false
    }
}
