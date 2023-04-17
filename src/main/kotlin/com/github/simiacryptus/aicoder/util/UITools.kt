@file:Suppress("UNNECESSARY_SAFE_CALL")

package com.github.simiacryptus.aicoder.util

import com.github.simiacryptus.aicoder.config.AppSettingsState
import com.github.simiacryptus.aicoder.config.Name
import com.github.simiacryptus.aicoder.openai.*
import com.github.simiacryptus.aicoder.openai.async.AsyncAPI
import com.github.simiacryptus.aicoder.openai.ui.CompletionRequestWithModel
import com.github.simiacryptus.aicoder.openai.ui.InteractiveCompletionRequest
import com.github.simiacryptus.aicoder.openai.ui.InteractiveEditRequest
import com.github.simiacryptus.aicoder.openai.ui.OpenAI_API
import com.simiacryptus.openai.ChatRequest
import com.simiacryptus.openai.CompletionRequest
import com.simiacryptus.openai.EditRequest
import com.simiacryptus.openai.ModerationException
import com.google.common.util.concurrent.FutureCallback
import com.google.common.util.concurrent.Futures
import com.google.common.util.concurrent.ListenableFuture
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.actionSystem.PlatformDataKeys
import com.intellij.openapi.command.WriteCommandAction
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.editor.Caret
import com.intellij.openapi.editor.Document
import com.intellij.openapi.fileEditor.FileDocumentManager
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.progress.Task
import com.intellij.openapi.progress.util.AbstractProgressIndicatorBase
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.ComboBox
import com.intellij.openapi.ui.DialogWrapper
import com.intellij.openapi.util.TextRange
import com.intellij.openapi.vfs.VirtualFile
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
import kotlin.reflect.KMutableProperty
import kotlin.reflect.KProperty1
import kotlin.reflect.full.memberProperties
import kotlin.reflect.jvm.isAccessible
import kotlin.reflect.jvm.javaField
import kotlin.reflect.jvm.javaType

object UITools {
    private val log = Logger.getInstance(UITools::class.java)
    val retry = WeakHashMap<Document, Runnable>()
    fun redoableRequest(
        request: CompletionRequest,
        indent: CharSequence,
        event: AnActionEvent,
        action: Function<CharSequence, Runnable>
    ) {
        redoableRequest(request, indent, event, { x: CharSequence -> x }, action)
    }

    fun redoableRequest(
        request: ChatRequest,
        indent: CharSequence,
        event: AnActionEvent,
        action: Function<CharSequence, Runnable>
    ) {
        redoableRequest(request, indent, event, { x: CharSequence -> x }, action)
    }

    fun startProgress(): ProgressIndicator? {
        if (1 == 1) return null
        if (AppSettingsState.instance.suppressProgress) return null
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
        postFilter: (CharSequence) -> CharSequence
    ) {
        redoableRequest(
            request,
            indent,
            event,
            transformCompletion,
            action,
            OpenAI_API.getCompletion(event.project!!, request, postFilter)
        )
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
        resultFuture: ListenableFuture<CharSequence> = OpenAI_API.getCompletion(event.project!!, request, indent),
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
        }, AsyncAPI.pool)
    }

    fun redoableRequest(
        request: ChatRequest,
        indent: CharSequence,
        event: AnActionEvent,
        transformCompletion: Function<CharSequence, CharSequence>,
        action: Function<CharSequence, Runnable>,
        resultFuture: ListenableFuture<CharSequence> = OpenAI_API.getChat(event.project!!, request),
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
        }, AsyncAPI.pool)
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
    fun getRetry(
        request: CompletionRequest,
        indent: CharSequence?,
        event: AnActionEvent,
        action: Function<CharSequence, Runnable>,
        undo: Runnable,
        transformCompletion: Function<CharSequence, CharSequence>
    ): Runnable {
        val document = Objects.requireNonNull(event.getData(CommonDataKeys.EDITOR))!!.document
        return Runnable {
            val progressIndicator = startProgress()
            Futures.addCallback(
                OpenAI_API.getCompletion(event.project!!, request, indent!!),
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
                        retry[document] =
                            getRetry(request, indent, event, action, nextUndo.get()!!, transformCompletion)
                    }

                    override fun onFailure(t: Throwable) {
                        progressIndicator?.cancel()
                        handle(t)
                    }
                },
                AsyncAPI.pool
            )
        }
    }

    fun getRetry(
        request: ChatRequest,
        indent: CharSequence?,
        event: AnActionEvent,
        action: Function<CharSequence, Runnable>,
        undo: Runnable,
        transformCompletion: Function<CharSequence, CharSequence>
    ): Runnable {
        val document = Objects.requireNonNull(event.getData(CommonDataKeys.EDITOR))!!.document
        return Runnable {
            val progressIndicator = startProgress()
            Futures.addCallback(
                OpenAI_API.getChat(event.project!!, request) { it },
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
                        retry[document] =
                            getRetry(request, indent, event, action, nextUndo.get()!!, transformCompletion)
                    }

                    override fun onFailure(t: Throwable) {
                        progressIndicator?.cancel()
                        handle(t)
                    }
                },
                AsyncAPI.pool
            )
        }
    }


    fun redoableRequest(
        request: EditRequest,
        indent: CharSequence,
        event: AnActionEvent,
        action: Function<CharSequence, Runnable>
    ) {
        redoableRequest(request, indent, event, { x: CharSequence -> x }, action)
    }

    fun redoableRequest(
        request: EditRequest,
        indent: CharSequence,
        event: AnActionEvent,
        transformCompletion: Function<CharSequence, CharSequence>,
        action: Function<CharSequence, Runnable>
    ) {
        val editor = event.getData(CommonDataKeys.EDITOR)
        val document = Objects.requireNonNull(editor)!!.document
        val progressIndicator = startProgress()
        val resultFuture = OpenAI_API.edit(event.project!!, request.uiIntercept(), indent)
        Futures.addCallback(resultFuture, object : FutureCallback<CharSequence?> {
            override fun onSuccess(result: CharSequence?) {
                progressIndicator?.cancel()
                val undo = AtomicReference<Runnable>()
                WriteCommandAction.runWriteCommandAction(event.project) {
                    undo.set(
                        action.apply(
                            transformCompletion.apply(
                                result.toString()
                            )
                        )
                    )
                }
                retry[document] = getRetry(request, indent, event, action, undo.get())
            }

            override fun onFailure(t: Throwable) {
                progressIndicator?.cancel()
                handle(t)
            }
        }, AsyncAPI.pool)
    }

    private fun getRetry(
        request: EditRequest,
        indent: CharSequence?,
        event: AnActionEvent,
        action: Function<CharSequence, Runnable>,
        undo: Runnable
    ): Runnable {
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
            }, AsyncAPI.pool)
        }
    }

    /**
     * Get an instruction with a style
     *
     * @param instruction The instruction to be returned
     * @return A string containing the instruction and the style
     */
    fun getInstruction(instruction: String): String {
        val style: CharSequence = AppSettingsState.instance.style
        return if (style.isEmpty()) instruction else String.format("%s (%s)", instruction, style)
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
        logEdit(
            String.format(
                "FWD replaceString from %s to %s (%s->%s): %s",
                startOffset,
                endOffset,
                endOffset - startOffset,
                newText.length,
                newText
            )
        )
        return Runnable {
            val verifyTxt = document.getText(TextRange(startOffset, startOffset + newText.length))
            if (verifyTxt != newText) {
                val msg = String.format(
                    "The text range from %d to %d does not match the expected text \"%s\" and is instead \"%s\"",
                    startOffset,
                    startOffset + newText.length,
                    newText,
                    verifyTxt
                )
                throw IllegalStateException(msg)
            }
            document.replaceString(startOffset, startOffset + newText.length, oldText)
            logEdit(
                String.format(
                    "REV replaceString from %s to %s (%s->%s): %s",
                    startOffset,
                    startOffset + newText.length,
                    newText.length,
                    oldText.length,
                    oldText
                )
            )
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
                val message = String.format(
                    "The text range from %d to %d does not match the expected text \"%s\" and is instead \"%s\"",
                    startOffset,
                    startOffset + newText.length,
                    newText,
                    verifyTxt
                )
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
        if (lines.isEmpty()) return ""
        return IndentedText.fromString(lines[Math.max(lineNumber, 0).coerceAtMost(lines.size - 1)]).indent
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
        val indent: CharSequence = if (null == caret) {
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
                options[1]
            ) == JOptionPane.OK_OPTION
        ) {
            val password = pass.password
            java.lang.String(password)
        } else {
            null
        }
    }

    fun <T : Any> readJavaUI(component: Any, settings: T) {
        val componentClass: Class<*> = component.javaClass
        val declaredUIFields =
            Arrays.stream(componentClass.fields).map { obj: Field -> obj.name }.collect(Collectors.toSet())
        for (settingsField in settings.javaClass.fields) {
            if (Modifier.isStatic(settingsField.modifiers)) continue
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
                            newSettingsValue =
                                findValue(settingsField.type as Class<out Enum<*>?>, item.toString())
                        }
                    }
                }
                settingsField[settings] = newSettingsValue
            } catch (e: Throwable) {
                RuntimeException("Error processing $settingsField", e).printStackTrace()
            }
        }
    }

    fun <T : Any, R : Any> readKotlinUI(component: R, settings: T) {
        val componentClass: Class<*> = component.javaClass
        val declaredUIFields =
            componentClass.kotlin.memberProperties.map { it.name }.toSet()
        for (settingsField in settings.javaClass.kotlin.memberProperties) {
            if (settingsField is KMutableProperty<*>) {
                settingsField.isAccessible = true
                val settingsFieldName = settingsField.name
                try {
                    var newSettingsValue: Any? = null
                    if (!declaredUIFields.contains(settingsFieldName)) continue
                    val uiField: KProperty1<R, *> =
                        (componentClass.kotlin.memberProperties.find { it.name == settingsFieldName } as KProperty1<R, *>?)!!
                    var uiVal = uiField.get(component)
                    if (uiVal is JScrollPane) {
                        uiVal = uiVal.viewport.view
                    }
                    when (settingsField.returnType.javaType.typeName) {
                        "java.lang.String" -> if (uiVal is JTextComponent) {
                            newSettingsValue = uiVal.text
                        } else if (uiVal is ComboBox<*>) {
                            newSettingsValue = uiVal.item
                        }

                        "int", "java.lang.Integer" -> if (uiVal is JTextComponent) {
                            newSettingsValue = uiVal.text.toInt()
                        }

                        "long" -> if (uiVal is JTextComponent) {
                            newSettingsValue = uiVal.text.toLong()
                        }

                        "double", "java.lang.Double" -> if (uiVal is JTextComponent) {
                            newSettingsValue = uiVal.text.toDouble()
                        }

                        "boolean" -> if (uiVal is JCheckBox) {
                            newSettingsValue = uiVal.isSelected
                        } else if (uiVal is JTextComponent) {
                            newSettingsValue = java.lang.Boolean.parseBoolean(uiVal.text)
                        }

                        else ->
                            if (Enum::class.java.isAssignableFrom(settingsField.returnType.javaType as Class<*>)) {
                                if (uiVal is ComboBox<*>) {
                                    val comboBox = uiVal
                                    val item = comboBox.item
                                    val enumClass = settingsField.returnType.javaType as Class<out Enum<*>?>
                                    val string = item.toString()
                                    newSettingsValue =
                                        findValue(enumClass, string)
                                }
                            }
                    }
                    settingsField.setter.call(settings, newSettingsValue)
                } catch (e: Throwable) {
                    RuntimeException("Error processing $settingsField", e).printStackTrace()
                }
            }
        }
    }

    fun findValue(enumClass: Class<out Enum<*>?>, string: String): Enum<*>? {
        enumClass.enumConstants?.filter { it?.name?.compareTo(string, true) == 0 }?.forEach { return it }
        return java.lang.Enum.valueOf(
            enumClass,
            string
        )
    }

    fun <T : Any> writeJavaUI(component: Any, settings: T) {
        val componentClass: Class<*> = component.javaClass
        val declaredUIFields =
            Arrays.stream(componentClass.fields).map { obj: Field -> obj.name }.collect(Collectors.toSet())
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
                        uiVal.item = settingsVal.toString()
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
                        uiVal.text = (settingsVal as Double).toString()
                    }

                    else -> if (uiVal is ComboBox<*>) {
                        uiVal.item = settingsVal.toString()
                    }
                }
            } catch (e: Throwable) {
                RuntimeException("Error processing $settingsField", e).printStackTrace()
            }
        }
    }

    fun <T : Any, R : Any> writeKotlinUI(component: R, settings: T) {
        val componentClass: Class<*> = component.javaClass
        val declaredUIFields =
            componentClass.kotlin.memberProperties.map { it.name }.toSet()
        for (settingsField in settings.javaClass.kotlin.memberProperties) {
            val fieldName = settingsField.name
            try {
                if (!declaredUIFields.contains(fieldName)) continue
                val uiField: KProperty1<R, *> =
                    (componentClass.kotlin.memberProperties.find { it.name == fieldName } as KProperty1<R, *>?)!!
                val settingsVal = settingsField.get(settings) ?: continue
                var uiVal = uiField.get(component)
                if (uiVal is JScrollPane) {
                    uiVal = uiVal.viewport.view
                }
                when (settingsField.returnType.javaType.typeName) {
                    "java.lang.String" -> if (uiVal is JTextComponent) {
                        uiVal.text = settingsVal.toString()
                    } else if (uiVal is ComboBox<*>) {
                        uiVal.item = settingsVal.toString()
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
                        uiVal.text = (settingsVal as Double).toString()
                    }

                    else -> if (uiVal is ComboBox<*>) {
                        uiVal.item = settingsVal.toString()
                    }
                }
            } catch (e: Throwable) {
                RuntimeException("Error processing $settingsField", e).printStackTrace()
            }
        }
    }

    fun <T> addJavaFields(ui: Any, formBuilder: FormBuilder) {
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

    fun <T : Any> addKotlinFields(ui: T, formBuilder: FormBuilder) {
        var first = true
        for (field in ui.javaClass.kotlin.memberProperties) {
            if (field.javaField == null) continue
            try {
                val nameAnnotation = field.annotations.find { it is Name } as Name?
                val component = field.get(ui) as JComponent
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
            options, options[0]
        )
        pane.initialValue = options[0]
        pane.componentOrientation = JOptionPane.getRootFrame().componentOrientation
        JOptionPane.getRootFrame()
        val dialog: JDialog = JDialog(JOptionPane.getRootFrame(), title, true)
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
        textArea.preferredSize = Dimension(
            (fontMetrics.charWidth(' ') * textArea.columns * 1.2).toInt(),
            (fontMetrics.height * textArea.rows * 1.2).toInt()
        )
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
            checkboxMap[checkboxIds[i]] = checkbox
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
            radioButtonMap[radioButtonDescriptions[i].toString()] = radioButton
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

    /**
     * Enforces foreign policy using AI-generated code.
     * If this effects you, you can either compile this yourself, or overthrow your government.
     * Glory to Ukraine! Glory to the Defenders!
     *
     * @return true if the user is sanctioned, false otherwise.
     */
    fun isSanctioned(): Boolean {
        // Due to the invasion of Ukraine, Russia and allied groups are currently sanctioned.
        // Slava Ukraini!
        val locale = Locale.getDefault()
        // ISO 3166 - Russia
        if (locale.country.compareTo("RU", true) == 0) return true
        // ISO 3166 - Belarus
        if (locale.country.compareTo("BY", true) == 0) return true
        // ISO 639 - Russian
        if (locale.language.compareTo("ru", true) == 0) {
            // ISO 3166 - Ukraine
            if (locale.country.compareTo("UA", true) == 0) return false
            // ISO 3166 - United States
            if (locale.country.compareTo("US", true) == 0) return false
            // ISO 3166 - Britian
            if (locale.country.compareTo("GB", true) == 0) return false
            // ISO 3166 - United Kingdom
            if (locale.country.compareTo("UK", true) == 0) return false
            // ISO 3166 - Georgia
            if (locale.country.compareTo("GE", true) == 0) return false
            // ISO 3166 - Kazakhstan
            if (locale.country.compareTo("KZ", true) == 0) return false
            // ISO 3166 - Germany
            if (locale.country.compareTo("DE", true) == 0) return false
            // ISO 3166 - Poland
            if (locale.country.compareTo("PL", true) == 0) return false
            // ISO 3166 - Latvia
            if (locale.country.compareTo("LV", true) == 0) return false
            // ISO 3166 - Lithuania
            if (locale.country.compareTo("LT", true) == 0) return false
            // ISO 3166 - Estonia
            if (locale.country.compareTo("EE", true) == 0) return false
            // ISO 3166 - Moldova
            if (locale.country.compareTo("MD", true) == 0) return false
            // ISO 3166 - Armenia
            if (locale.country.compareTo("AM", true) == 0) return false
            // ISO 3166 - Azerbaijan
            if (locale.country.compareTo("AZ", true) == 0) return false
            // ISO 3166 - Kyrgyzstan
            if (locale.country.compareTo("KG", true) == 0) return false
            // ISO 3166 - Tajikistan
            if (locale.country.compareTo("TJ", true) == 0) return false
            // ISO 3166 - Turkmenistan
            if (locale.country.compareTo("TM", true) == 0) return false
            // ISO 3166 - Uzbekistan
            if (locale.country.compareTo("UZ", true) == 0) return false
            // ISO 3166 - Mongolia
            if (locale.country.compareTo("MN", true) == 0) return false
            return true
        }
        return false
    }

    fun <T : Any> build(
        component: T,
        formBuilder: FormBuilder = FormBuilder.createFormBuilder()
    ): JPanel? {
        addKotlinFields(component, formBuilder)
        return formBuilder.addComponentFillVertically(JPanel(), 0).panel
    }

    fun <T : Any, C : Any> showDialog(
        e: AnActionEvent,
        uiClass: Class<T>,
        configClass: Class<C>,
        onComplete: (C) -> Unit
    ) {
        val project = e.project
        val component = uiClass.getConstructor().newInstance()
        val config = configClass.getConstructor().newInstance()
        val dialog = object : DialogWrapper(project) {
            init {
                this.init()
                this.title = "Generate Project"
                this.setOKButtonText("Generate")
                this.setCancelButtonText("Cancel")
                this.setResizable(true)
                //this.setPreferredFocusedComponent(this)
                //this.setContent(this)
            }

            override fun createCenterPanel(): JComponent? {
                return build(component)
            }
        }
        dialog.show()
        if (dialog.isOK) {
            readKotlinUI(component, config)
            onComplete(config)
        }
    }

    fun getSelectedFolder(e: AnActionEvent): VirtualFile? {
        val project = e.project
        val dataContext = e.dataContext
        val data = PlatformDataKeys.VIRTUAL_FILE.getData(dataContext)
        if (data != null && data.isDirectory) {
            return data
        }
        val editor = PlatformDataKeys.EDITOR.getData(dataContext)
        if (editor != null) {
            val file = FileDocumentManager.getInstance().getFile(editor.document)
            if (file != null) {
                return file.parent
            }
        }
        return project?.baseDir
    }

    fun isInterruptedException(e: Throwable?): Boolean {
        if (e is InterruptedException) return true
        return if (e!!.cause != null && e.cause !== e) isInterruptedException(e.cause) else false
    }

    fun <T> run(
        project: Project?,
        title: String,
        canBeCancelled: Boolean,
        retries: Int = 3,
        suppressProgress: Boolean = false,
        task: (ProgressIndicator) -> T
    ): T {
        return run(object : Task.WithResult<T, Exception?>(project, title, canBeCancelled) {
            override fun compute(indicator: ProgressIndicator): T {
                return task(indicator)
            }
        }, retries, suppressProgress)
    }

    fun <T> run(task: Task.WithResult<T, Exception?>, retries: Int = 3, suppressProgress: Boolean = false): T {
        return try {
            if (!suppressProgress) {
                ProgressManager.getInstance().run(task)
            } else {
                task.run(AbstractProgressIndicatorBase())
                task.result
            }
        } catch (e: RuntimeException) {
            if (isInterruptedException(e)) throw e
            if (retries > 0) {
                AsyncAPI.log.warn("Retrying request", e)
                run(task = task, retries - 1)
            } else {
                throw e
            }
        } catch (e: InterruptedException) {
            throw RuntimeException(e)
        } catch (e: Exception) {
            if (isInterruptedException(e)) throw RuntimeException(e)
            if (retries > 0) {
                AsyncAPI.log.warn("Retrying request", e)
                try {
                    Thread.sleep(15000)
                } catch (ex: InterruptedException) {
                    Thread.currentThread().interrupt()
                }
                run(task = task, retries - 1)
            } else {
                throw RuntimeException(e)
            }
        }
    }

    fun EditRequest.uiIntercept(): EditRequest {
        return if (AppSettingsState.instance.devActions) {
            showEditDialog(this)
        } else {
            this
        }
    }

    private fun showEditDialog(edit: EditRequest): EditRequest {
        val formBuilder = FormBuilder.createFormBuilder()
        val withModel = EditRequest(edit)
        val ui = InteractiveEditRequest()
        addKotlinFields<Any>(ui, formBuilder)
        writeKotlinUI(ui, withModel)
        val mainPanel = formBuilder.panel
        return if (showOptionDialog(mainPanel, arrayOf("OK"), title = "Completion Request") == 0) {
            readKotlinUI(ui, withModel)
            withModel
        } else {
            withModel
        }
    }

    fun CompletionRequest.uiIntercept(): CompletionRequestWithModel {
        return if (this !is CompletionRequestWithModel) {
            val settingsState = AppSettingsState.instance
            if (!settingsState.devActions) {
                CompletionRequestWithModel(this, settingsState.model_completion)
            } else {
                showCompletionDialog(this)
            }
        } else {
            this
        }
    }

    private fun showCompletionDialog(completion: CompletionRequest): CompletionRequestWithModel {
        val formBuilder = FormBuilder.createFormBuilder()
        val instance = AppSettingsState.instance
        val withModel = CompletionRequestWithModel(completion, instance.model_completion)
        val ui = InteractiveCompletionRequest(withModel)
        addKotlinFields<Any>(ui, formBuilder)
        writeKotlinUI(ui, withModel)
        val mainPanel = formBuilder.panel
        return if (showOptionDialog(mainPanel, arrayOf<Any>("OK"), title = "Completion Request") == 0) {
            readKotlinUI(ui, withModel)
            withModel
        } else {
            withModel
        }
    }

}
