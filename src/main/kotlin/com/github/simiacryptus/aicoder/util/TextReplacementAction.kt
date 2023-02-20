package com.github.simiacryptus.aicoder.util

/**
 * TextReplacementAction is an abstract class that extends the AnAction class.
 * It provides a static method create() that takes in a text, description, icon, and an ActionTextEditorFunction.
 * It also provides an actionPerformed() method that is called when the action is performed.
 * This method gets the editor, caret model, and primary caret from the AnActionEvent.
 * It then calls the edit() method, which is implemented by the subclasses, and replaces the selected text with the new text.
 * The ActionTextEditorFunction is a functional interface that takes in an AnActionEvent and a String and returns a String.
 */
@Suppress("unused")
class TextReplacementAction


