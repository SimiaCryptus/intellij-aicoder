package com.github.simiacryptus.aicoder.util

import com.github.simiacryptus.aicoder.openai.OpenAI_API
import com.github.simiacryptus.aicoder.config.AppSettingsState
import com.github.simiacryptus.aicoder.config.Name
import com.github.simiacryptus.aicoder.openai.CompletionRequest
import com.github.simiacryptus.aicoder.openai.EditRequest
import com.github.simiacryptus.aicoder.openai.ModerationException
import com.google.common.util.concurrent.FutureCallback
import com.google.common.util.concurrent.Futures
import com.google.common.util.concurrent.ListenableFuture
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.command.WriteCommandAction
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.editor.Caret
import com.intellij.openapi.editor.Document
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.ui.ComboBox
import com.intellij.openapi.util.TextRange
import com.intellij.ui.components.JBLabel
import com.intellij.ui.components.JBScrollPane
import com.intellij.ui.components.JBTextArea
import com.intellij.util.ui.FormBuilder
import java.awt.BorderLayout
import java.awt.Component
import java.awt.Dimension
import java.awt.Toolkit
import java.awt.event.ComponentAdapter
import java.awt.event.ComponentEvent
import java.awt.event.WindowAdapter
import java.awt.event.WindowEvent
import java.beans.PropertyChangeEvent
import java.lang.reflect.Field
import java.lang.reflect.Modifier
import java.util.*
import java.util.concurrent.atomic.AtomicReference
import java.util.function.Function
import java.util.stream.Collectors
import javax.swing.*
import javax.swing.text.JTextComponent

object UITools {
    private val log = Logger.getInstance(UITools::class.java)
    val retry = WeakHashMap<Document, Runnable>()
    fun redoableRequest(request: CompletionRequest, indent: CharSequence, event: AnActionEvent, action: Function<CharSequence, Runnable>) {
        redoableRequest(request, indent, event, { x: CharSequence -> x }, action)
    }

    fun startProgress(): ProgressIndicator? {
        if(1==1) return null;
        if (AppSettingsState.getInstance().suppressProgress) return null
        val progressIndicator = ProgressManager.getInstance().progressIndicator
        if (null != progressIndicator) {
            progressIndicator.isIndeterminate = true
            progressIndicator.text = "Talking to OpenAI..."
        }
        return progressIndicator
    }
    fun redoableRequest(
        request: CompletionRequest,
        indent: CharSequence,
        event: AnActionEvent,
        transformCompletion: Function<CharSequence, CharSequence>,
        action: Function<CharSequence, Runnable>,
        postFilter: (CharSequence) -> CharSequence) {
        redoableRequest(request, indent, event, transformCompletion, action, OpenAI_API.complete(event.project!!, request, postFilter))
    }

    /**
     * This method is responsible for making a redoable request.
     *
     * @param request The completion request to be made.
     * @param indent  The indentation to be used.
     * @param event   The project to be used.
     * @param action  The action to be taken when the request is completed.
     * @return A [Runnable] that can be used to redo the request.
     */
    fun redoableRequest(
        request: CompletionRequest,
        indent: CharSequence,
        event: AnActionEvent,
        transformCompletion: Function<CharSequence, CharSequence>,
        action: Function<CharSequence, Runnable>,
        resultFuture: ListenableFuture<CharSequence> = OpenAI_API.complete(event.project!!, request, indent?:""),
        progressIndicator: ProgressIndicator? = startProgress()
    ) {
        Futures.addCallback(resultFuture, object : FutureCallback<CharSequence?> {
            override fun onSuccess(result: CharSequence?) {
                progressIndicator?.cancel()
                val actionFn = AtomicReference<Runnable?>()
                WriteCommandAction.runWriteCommandAction(event.project) {
                    actionFn.set(
                        action.apply(
                            transformCompletion.apply(
                                result.toString()
                            )
                        )
                    )
                }
                if (null != actionFn.get()) {
                    val undo = getRetry(request, indent, event, action, actionFn.get()!!, transformCompletion)
                    val document = event.getRequiredData(CommonDataKeys.EDITOR).document
                    retry[document] = undo
                }
            }

            override fun onFailure(t: Throwable) {
                progressIndicator?.cancel()
                handle(t)
            }
        }, OpenAI_API.pool)
    }

    /**
     * Get a retry Runnable for the given [CompletionRequest].
     *
     *
     * This method will create a [Runnable] that will attempt to complete the given [CompletionRequest]
     * with the given `indent`. If the completion is successful, the given `action` will be applied to the
     * result after the given `undo` is run.
     *
     * @param request             the [CompletionRequest] to complete
     * @param indent              the indent to use for the completion
     * @param event               the [Project] to use for the completion
     * @param action              the [Function] to apply to the result of the completion
     * @param undo                the [Runnable] to run if the completion is successful
     * @param transformCompletion
     * @return a [Runnable] that will attempt to complete the given [CompletionRequest]
     */
    fun getRetry(request: CompletionRequest, indent: CharSequence?, event: AnActionEvent, action: Function<CharSequence, Runnable>, undo: Runnable, transformCompletion: Function<CharSequence, CharSequence>): Runnable {
        val document = Objects.requireNonNull(event.getData(CommonDataKeys.EDITOR))!!.document
        return Runnable {
            val progressIndicator = startProgress()
            Futures.addCallback(
                OpenAI_API.complete(event.project!!, request, indent!!),
                object : FutureCallback<CharSequence?> {
                    override fun onSuccess(result: CharSequence?) {
                        progressIndicator?.cancel()
                        WriteCommandAction.runWriteCommandAction(event.project) { undo?.run() }
                        val nextUndo = AtomicReference<Runnable?>()
                        WriteCommandAction.runWriteCommandAction(event.project) {
                            nextUndo.set(
                                action.apply(
                                    transformCompletion.apply(result.toString())
                                )
                            )
                        }
                        retry[document] = getRetry(request, indent, event, action, nextUndo.get()!!, transformCompletion)
                    }

                    override fun onFailure(t: Throwable) {
                        progressIndicator?.cancel()
                        handle(t)
                    }
                },
                OpenAI_API.pool
            )
        }
    }

    fun redoableRequest(request: EditRequest, indent: CharSequence, event: AnActionEvent, action: Function<CharSequence, Runnable>) {
        redoableRequest(request, indent, event, { x: CharSequence -> x }, action)
    }

    fun redoableRequest(request: EditRequest, indent: CharSequence, event: AnActionEvent, transformCompletion: Function<CharSequence, CharSequence>, action: Function<CharSequence, Runnable>) {
        val editor = event.getData(CommonDataKeys.EDITOR)
        val document = Objects.requireNonNull(editor)!!.document
        val progressIndicator = startProgress()
        val resultFuture = OpenAI_API.edit(event.project!!, request.uiIntercept(), indent!!)
        Futures.addCallback(resultFuture, object : FutureCallback<CharSequence?> {
            override fun onSuccess(result: CharSequence?) {
                progressIndicator?.cancel()
                val undo = AtomicReference<Runnable>()
                WriteCommandAction.runWriteCommandAction(event.project) { undo.set(action.apply(transformCompletion.apply(result.toString()))) }
                retry[document] = getRetry(request, indent, event, action, undo.get())
            }

            override fun onFailure(t: Throwable) {
                progressIndicator?.cancel()
                handle(t)
            }
        }, OpenAI_API.pool)
    }

    private fun getRetry(request: EditRequest, indent: CharSequence?, event: AnActionEvent, action: Function<CharSequence, Runnable>, undo: Runnable): Runnable {
        val document = Objects.requireNonNull(event.getData(CommonDataKeys.EDITOR))!!.document
        return Runnable {
            val progressIndicator = startProgress()
            val retryFuture = OpenAI_API.edit(event.project!!, request.uiIntercept(), indent!!)
            Futures.addCallback(retryFuture, object : FutureCallback<CharSequence?> {
                override fun onSuccess(result: CharSequence?) {
                    progressIndicator?.cancel()
                    WriteCommandAction.runWriteCommandAction(event.project) { undo?.run() }
                    val nextUndo = AtomicReference<Runnable>()
                    WriteCommandAction.runWriteCommandAction(event.project) { nextUndo.set(action.apply(result.toString())) }
                    retry[document] = getRetry(request, indent, event, action, nextUndo.get())
                }

                override fun onFailure(t: Throwable) {
                    progressIndicator?.cancel()
                    handle(t)
                }
            }, OpenAI_API.pool)
        }
    }

    /**
     * Get an instruction with a style
     *
     * @param instruction The instruction to be returned
     * @return A string containing the instruction and the style
     */
    fun getInstruction(instruction: String): String {
        val style: CharSequence = AppSettingsState.getInstance().style
        return if (style.length == 0) instruction else String.format("%s (%s)", instruction, style)
    }

    /**
     * Replaces a string in a document with a new string.
     *
     * @param document    The document to replace the string in.
     * @param startOffset The start offset of the string to be replaced.
     * @param endOffset   The end offset of the string to be replaced.
     * @param newText     The new string to replace the old string.
     * @return A Runnable that can be used to undo the replacement.
     */
    fun replaceString(document: Document, startOffset: Int, endOffset: Int, newText: CharSequence): Runnable {
        val oldText: CharSequence = document.getText(TextRange(startOffset, endOffset))
        document.replaceString(startOffset, endOffset, newText)
        logEdit(String.format("FWD replaceString from %s to %s (%s->%s): %s", startOffset, endOffset, endOffset - startOffset, newText.length, newText))
        return Runnable {
            val verifyTxt = document.getText(TextRange(startOffset, startOffset + newText.length))
            if (verifyTxt != newText) {
                val msg = String.format("The text range from %d to %d does not match the expected text \"%s\" and is instead \"%s\"", startOffset, startOffset + newText.length, newText, verifyTxt)
                throw IllegalStateException(msg)
            }
            document.replaceString(startOffset, startOffset + newText.length, oldText)
            logEdit(String.format("REV replaceString from %s to %s (%s->%s): %s", startOffset, startOffset + newText.length, newText.length, oldText.length, oldText))
        }
    }

    /**
     * Inserts a string into a document at a given offset and returns a Runnable to undo the insertion.
     *
     * @param document    The document to insert the string into.
     * @param startOffset The offset at which to insert the string.
     * @param newText     The string to insert.
     * @return A Runnable that can be used to undo the insertion.
     */
    fun insertString(document: Document, startOffset: Int, newText: CharSequence): Runnable {
        document.insertString(startOffset, newText)
        logEdit(String.format("FWD insertString @ %s (%s): %s", startOffset, newText.length, newText))
        return Runnable {
            val verifyTxt = document.getText(TextRange(startOffset, startOffset + newText.length))
            if (verifyTxt != newText) {
                val message = String.format("The text range from %d to %d does not match the expected text \"%s\" and is instead \"%s\"", startOffset, startOffset + newText.length, newText, verifyTxt)
                throw AssertionError(message)
            }
            document.deleteString(startOffset, startOffset + newText.length)
            logEdit(String.format("REV deleteString from %s to %s", startOffset, startOffset + newText.length))
        }
    }

    private fun logEdit(message: String) {
        log.debug(message)
    }

    @Suppress("unused")
    fun deleteString(document: Document, startOffset: Int, endOffset: Int): Runnable {
        val oldText: CharSequence = document.getText(TextRange(startOffset, endOffset))
        document.deleteString(startOffset, endOffset)
        return Runnable {
            document.insertString(startOffset, oldText)
            logEdit(String.format("REV insertString @ %s (%s): %s", startOffset, oldText.length, oldText))
        }
    }

    fun getIndent(caret: Caret?): CharSequence {
        if (null == caret) return ""
        val document = caret.editor.document
        val documentText = document.text
        val lineNumber = document.getLineNumber(caret.selectionStart)
        val lines = documentText.split("\n".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
        if(lines.isEmpty()) return ""
        return IndentedText.fromString(lines[Math.min(Math.max(lineNumber, 0), lines.size - 1)]).indent
    }

    @Suppress("unused")
    fun hasSelection(e: AnActionEvent): Boolean {
        val caret = e.getData(CommonDataKeys.CARET)
        return null != caret && caret.hasSelection()
    }

    fun handle(ex: Throwable) {
        if (ex !is ModerationException) log.error(ex)
        JOptionPane.showMessageDialog(null, ex.message, "Warning", JOptionPane.WARNING_MESSAGE)
    }

    fun getIndent(event: AnActionEvent): CharSequence {
        val caret = event.getData(CommonDataKeys.CARET)
        val indent: CharSequence
        indent = if (null == caret) {
            ""
        } else {
            getIndent(caret)
        }
        return indent
    }

    fun queryAPIKey(): CharSequence? {
        val panel = JPanel()
        val label = JLabel("Enter OpenAI API Key:")
        val pass = JPasswordField(100)
        panel.add(label)
        panel.add(pass)
        val options = arrayOf<Any>("OK", "Cancel")
        return if (JOptionPane.showOptionDialog(
                        null,
                        panel,
                        "API Key",
                        JOptionPane.NO_OPTION,
                        JOptionPane.PLAIN_MESSAGE,
                        null,
                        options,
                        options[1]) == JOptionPane.OK_OPTION) {
            val password = pass.password
            java.lang.String(password)
        } else {
            null
        }
    }

    fun <T : Any> readUI(component: Any, settings: T) {
        val componentClass: Class<*> = component.javaClass
        val declaredUIFields = Arrays.stream(componentClass.fields).map { obj: Field -> obj.name }.collect(Collectors.toSet())
        for (settingsField in settings.javaClass.fields) {
            settingsField.isAccessible = true
            val settingsFieldName = settingsField.name
            try {
                var newSettingsValue: Any? = null
                if (!declaredUIFields.contains(settingsFieldName)) continue
                val uiField = componentClass.getDeclaredField(settingsFieldName)
                var uiVal = uiField[component]
                if (uiVal is JScrollPane) {
                    uiVal = uiVal.viewport.view
                }
                when (settingsField.type.name) {
                    "java.lang.String" -> if (uiVal is JTextComponent) {
                        newSettingsValue = uiVal.text
                    } else if (uiVal is ComboBox<*>) {
                        newSettingsValue = uiVal.item
                    }

                    "int" -> if (uiVal is JTextComponent) {
                        newSettingsValue = uiVal.text.toInt()
                    }

                    "long" -> if (uiVal is JTextComponent) {
                        newSettingsValue = uiVal.text.toLong()
                    }

                    "double" -> if (uiVal is JTextComponent) {
                        newSettingsValue = uiVal.text.toDouble()
                    }

                    "boolean" -> if (uiVal is JCheckBox) {
                        newSettingsValue = uiVal.isSelected
                    } else if (uiVal is JTextComponent) {
                        newSettingsValue = java.lang.Boolean.parseBoolean(uiVal.text)
                    }

                    else -> if (Enum::class.java.isAssignableFrom(settingsField.type)) {
                        if (uiVal is ComboBox<*>) {
                            val comboBox = uiVal
                            val item = comboBox.item
                            newSettingsValue = java.lang.Enum.valueOf(settingsField.type as Class<out Enum<*>?>, item.toString())
                        }
                    }
                }
                settingsField[settings] = newSettingsValue
            } catch (e: Throwable) {
                RuntimeException("Error processing $settingsField", e).printStackTrace()
            }
        }
    }

    fun <T : Any> writeUI(component: Any, settings: T) {
        val componentClass: Class<*> = component.javaClass
        val declaredUIFields = Arrays.stream(componentClass.fields).map { obj: Field -> obj.name }.collect(Collectors.toSet())
        for (settingsField in settings.javaClass.fields) {
            val fieldName = settingsField.name
            try {
                if (!declaredUIFields.contains(fieldName)) continue
                val uiField = componentClass.getDeclaredField(fieldName)
                val settingsVal = settingsField.get(settings) ?: continue
                var uiVal = uiField[component]
                if (uiVal is JScrollPane) {
                    uiVal = uiVal.viewport.view
                }
                when (settingsField.type.name) {
                    "java.lang.String" -> if (uiVal is JTextComponent) {
                        uiVal.text = settingsVal.toString()
                    } else if (uiVal is ComboBox<*>) {
                        (uiVal as ComboBox<CharSequence?>).item = settingsVal.toString()
                    }

                    "int", "java.lang.Integer" -> if (uiVal is JTextComponent) {
                        uiVal.text = (settingsVal as Int).toString()
                    }

                    "long" -> if (uiVal is JTextComponent) {
                        uiVal.text = (settingsVal as Int).toLong().toString()
                    }

                    "boolean" -> if (uiVal is JCheckBox) {
                        uiVal.isSelected = (settingsVal as Boolean)
                    } else if (uiVal is JTextComponent) {
                        uiVal.text = java.lang.Boolean.toString((settingsVal as Boolean))
                    }

                    "double", "java.lang.Double" -> if (uiVal is JTextComponent) {
                        uiVal.text = java.lang.Double.toString((settingsVal as Double))
                    }

                    else -> if (uiVal is ComboBox<*>) {
                        (uiVal as ComboBox<CharSequence?>).item = settingsVal.toString()
                    }
                }
            } catch (e: Throwable) {
                RuntimeException("Error processing $settingsField", e).printStackTrace()
            }
        }
    }

    fun <T> addFields(ui: Any, formBuilder: FormBuilder) {
        var first = true
        for (field in ui.javaClass.fields) {
            if (Modifier.isStatic(field.modifiers)) continue
            try {
                val nameAnnotation = field.getDeclaredAnnotation(Name::class.java)
                val component = field[ui] as JComponent
                if (nameAnnotation != null) {
                    if (first) {
                        first = false
                        formBuilder.addLabeledComponentFillVertically(nameAnnotation.value + ": ", component)
                    } else {
                        formBuilder.addLabeledComponent(JBLabel(nameAnnotation.value + ": "), component, 1, false)
                    }
                } else {
                    formBuilder.addComponentToRightColumn(component, 1)
                }
            } catch (e: IllegalAccessException) {
                throw RuntimeException(e)
            } catch (e: Throwable) {
                log.warn("Error processing " + field.name, e)
            }
        }
    }

    fun getMaximumSize(factor: Double): Dimension {
        val screenSize = Toolkit.getDefaultToolkit().screenSize
        return Dimension((screenSize.getWidth() * factor).toInt(), (screenSize.getHeight() * factor).toInt())
    }

    fun showOptionDialog(mainPanel: JPanel?, vararg options: Any, title: String): Int {
        val pane = JOptionPane(
                mainPanel, JOptionPane.PLAIN_MESSAGE,
                JOptionPane.NO_OPTION, null,
                options, options[0])
        pane.initialValue = options[0]
        pane.componentOrientation = JOptionPane.getRootFrame().componentOrientation
        val dialog: JDialog
        JOptionPane.getRootFrame()
        dialog = JDialog(JOptionPane.getRootFrame(), title, true)
        dialog.componentOrientation = JOptionPane.getRootFrame().componentOrientation
        val contentPane = dialog.contentPane
        contentPane.layout = BorderLayout()
        contentPane.add(pane, BorderLayout.CENTER)
        if (JDialog.isDefaultLookAndFeelDecorated() && UIManager.getLookAndFeel().supportsWindowDecorations) {
            dialog.isUndecorated = true
            pane.rootPane.windowDecorationStyle = JRootPane.PLAIN_DIALOG
        }
        dialog.isResizable = true
        dialog.maximumSize = getMaximumSize(0.9)
        dialog.pack()
        dialog.setLocationRelativeTo(null as Component?)
        val adapter: WindowAdapter = object : WindowAdapter() {
            private var gotFocus = false
            override fun windowClosing(we: WindowEvent) {
                pane.value = null
            }

            override fun windowClosed(e: WindowEvent) {
                pane.removePropertyChangeListener { event: PropertyChangeEvent ->
                    // Let the defaultCloseOperation handle the closing
                    // if the user closed the window without selecting a button
                    // (newValue = null in that case).  Otherwise, close the dialog.
                    if (dialog.isVisible && event.source === pane && event.propertyName == JOptionPane.VALUE_PROPERTY && event.newValue != null && event.newValue !== JOptionPane.UNINITIALIZED_VALUE) {
                        dialog.isVisible = false
                    }
                }
                dialog.contentPane.removeAll()
            }

            override fun windowGainedFocus(we: WindowEvent) {
                // Once window gets focus, set initial focus
                if (!gotFocus) {
                    pane.selectInitialValue()
                    gotFocus = true
                }
            }
        }
        dialog.addWindowListener(adapter)
        dialog.addWindowFocusListener(adapter)
        dialog.addComponentListener(object : ComponentAdapter() {
            override fun componentShown(ce: ComponentEvent) {
                // reset value to ensure closing works properly
                pane.value = JOptionPane.UNINITIALIZED_VALUE
            }
        })
        pane.addPropertyChangeListener { event: PropertyChangeEvent ->
            if (dialog.isVisible && event.source === pane && event.propertyName == JOptionPane.VALUE_PROPERTY && event.newValue != null && event.newValue !== JOptionPane.UNINITIALIZED_VALUE) {
                dialog.isVisible = false
            }
        }
        pane.selectInitialValue()
        dialog.isVisible = true
        dialog.dispose()
        return getSelectedValue(pane, options)
    }

    private fun getSelectedValue(pane: JOptionPane, options: Array<out Any>): Int {
        val selectedValue = pane.value ?: return JOptionPane.CLOSED_OPTION
        var counter = 0
        val maxCounter = options.size
        while (counter < maxCounter) {
            if (options[counter] == selectedValue) return counter
            counter++
        }
        return JOptionPane.CLOSED_OPTION
    }

    fun configureTextArea(textArea: JBTextArea): JBTextArea {
        val font = textArea.font
        val fontMetrics = textArea.getFontMetrics(font)
        textArea.preferredSize = Dimension((fontMetrics.charWidth(' ') * textArea.columns * 1.2).toInt(), (fontMetrics.height * textArea.rows * 1.2).toInt())
        textArea.autoscrolls = true
        return textArea
    }

    fun wrapScrollPane(promptArea: JBTextArea?): JBScrollPane {
        val scrollPane = JBScrollPane(promptArea)
        scrollPane.horizontalScrollBarPolicy = JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED
        scrollPane.verticalScrollBarPolicy = JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED
        return scrollPane
    }

    /**
     *
     *  Displays a dialog with a list of checkboxMap and an OK button.
     *  When OK is pressed, returns with an array of the selected IDs
     *
     *  @param promptMessage The promptMessage to display in the dialog.
     *  @param checkboxIds The checkboxIds of the checkboxMap.
     *  @param checkboxDescriptions The checkboxDescriptions of the checkboxMap.
     *  @return An array of the checkboxIds of the checkboxMap that were checked.
     */
    fun showCheckboxDialog(
        promptMessage: String,
        checkboxIds: Array<String>,
        checkboxDescriptions: Array<String>
    ): Array<String> {
        val formBuilder = FormBuilder.createFormBuilder()
        val checkboxMap = HashMap<String, JCheckBox>()
        for (i in checkboxIds.indices) {
            val checkbox = JCheckBox(checkboxDescriptions[i], null as Icon?, true)
            checkboxMap.put(checkboxIds[i], checkbox)
            formBuilder.addComponent(checkbox)
        }
        val dialogResult = showOptionDialog(formBuilder.panel, "OK", title = promptMessage)
        val selectedIds = ArrayList<String>()
        if (dialogResult == 0) {
            for ((checkboxId, checkbox) in checkboxMap) {
                if (checkbox.isSelected) {
                    selectedIds.add(checkboxId)
                }
            }
        }
        return selectedIds.toTypedArray()
    }

    fun showRadioButtonDialog(
        promptMessage: CharSequence,
        vararg radioButtonDescriptions: CharSequence
    ): CharSequence? {
        val formBuilder = FormBuilder.createFormBuilder()
        val radioButtonMap = HashMap<String, JRadioButton>()
        val buttonGroup = ButtonGroup()
        for (i in radioButtonDescriptions.indices) {
            val radioButton = JRadioButton(radioButtonDescriptions[i].toString(), null as Icon?, true)
            radioButtonMap.put(radioButtonDescriptions[i].toString(), radioButton)
            buttonGroup.add(radioButton)
            formBuilder.addComponent(radioButton)
        }
        val dialogResult = showOptionDialog(formBuilder.panel, "OK", title = promptMessage.toString())
        if (dialogResult == 0) {
            for ((radioButtonId, radioButton) in radioButtonMap) {
                if (radioButton.isSelected) {
                    return radioButtonId
                }
            }
        }
        return null
    }
}
