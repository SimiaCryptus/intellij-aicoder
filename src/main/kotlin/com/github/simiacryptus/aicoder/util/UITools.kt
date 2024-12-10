package com.github.simiacryptus.aicoder.util

import com.github.simiacryptus.aicoder.actions.generic.toFile
import com.github.simiacryptus.aicoder.config.AppSettingsState
import com.github.simiacryptus.aicoder.config.Name
import com.github.simiacryptus.aicoder.util.BrowseUtil.browse
import com.google.common.util.concurrent.*
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.actionSystem.PlatformDataKeys
import com.intellij.openapi.command.WriteCommandAction
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
import com.intellij.openapi.ui.Messages
import com.intellij.openapi.util.TextRange
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.ui.components.JBLabel
import com.intellij.ui.components.JBPasswordField
import com.intellij.ui.components.JBScrollPane
import com.intellij.ui.components.JBTextArea
import com.intellij.util.ui.FormBuilder
import com.simiacryptus.jopenai.OpenAIClient
import com.simiacryptus.jopenai.exceptions.ModerationException
import com.simiacryptus.jopenai.models.APIProvider
import org.jdesktop.swingx.JXButton
import org.slf4j.LoggerFactory
import java.awt.BorderLayout
import java.awt.Component
import java.awt.Dimension
import java.awt.Toolkit
import java.awt.event.ComponentAdapter
import java.awt.event.ComponentEvent
import java.awt.event.WindowAdapter
import java.awt.event.WindowEvent
import java.beans.PropertyChangeEvent
import java.io.IOException
import java.io.PrintWriter
import java.io.StringWriter
import java.net.URI
import java.util.*
import java.util.concurrent.*
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicReference
import java.util.function.Supplier
import javax.swing.*
import javax.swing.text.JTextComponent
import kotlin.math.max
import kotlin.reflect.KClass
import kotlin.reflect.KMutableProperty
import kotlin.reflect.KProperty1
import kotlin.reflect.KVisibility
import kotlin.reflect.full.memberProperties
import kotlin.reflect.jvm.isAccessible
import kotlin.reflect.jvm.javaType

object UITools {
    val retry = WeakHashMap<Document, Runnable>()

    fun showError(project: Project?, message: String, title: String = "Error") {
        Messages.showErrorDialog(project, message, title)
    }

    fun showWarning(project: Project?, message: String, title: String = "Warning") {
        Messages.showWarningDialog(project, message, title)
    }

    fun getLanguageFromFile(fileName: String): String {
        return when {
            fileName.endsWith(".kt") -> "kotlin"
            fileName.endsWith(".java") -> "java"
            fileName.endsWith(".py") -> "python"
            fileName.endsWith(".js") -> "javascript"
            fileName.endsWith(".ts") -> "typescript"
            fileName.endsWith(".html") -> "html"
            fileName.endsWith(".css") -> "css"
            fileName.endsWith(".xml") -> "xml"
            fileName.endsWith(".json") -> "json"
            else -> "text"
        }
    }
    private val log = LoggerFactory.getLogger(UITools::class.java)
    private val threadFactory: ThreadFactory = ThreadFactoryBuilder().setNameFormat("API Thread %d").build()
    private val pool: ListeningExecutorService by lazy {
        MoreExecutors.listeningDecorator(
            ThreadPoolExecutor(
                /* corePoolSize = */ AppSettingsState.instance.apiThreads,
                /* maximumPoolSize = */AppSettingsState.instance.apiThreads,
                /* keepAliveTime = */ 0L,
                /* unit = */ TimeUnit.MILLISECONDS,
                /* workQueue = */ LinkedBlockingQueue(),
                /* threadFactory = */ threadFactory,
                /* handler = */ ThreadPoolExecutor.AbortPolicy()
            )
        )
    }
    private val scheduledPool: ListeningScheduledExecutorService by lazy {
        MoreExecutors.listeningDecorator(ScheduledThreadPoolExecutor(1, threadFactory))
    }
    private val errorLog = mutableListOf<Pair<String, Throwable>>()
    private val actionLog = mutableListOf<String>()
    private val singleThreadPool = Executors.newSingleThreadExecutor()

    fun getRoot(e: AnActionEvent) : String {
      return getSelectedFolder(e)?.toFile?.absolutePath ?: getSelectedFile(e)?.toFile?.parent ?: ""
    }

    fun redoableTask(
        event: AnActionEvent,
        request: Supplier<Runnable>,
    ) {
        log.debug("Starting redoableTask with event: ${event}, request: ${request}")
        Futures.addCallback(pool.submit<Runnable> {
            request.get()
        }, futureCallback(event, request), pool)
        log.debug("Submitted redoableTask for execution")
    }

    private fun futureCallback(
        event: AnActionEvent,
        request: Supplier<Runnable>,
    ) = object : FutureCallback<Runnable> {
        override fun onSuccess(undo: Runnable) {
            val requiredData = event.getData(CommonDataKeys.EDITOR) ?: return
            val document = requiredData.document
            retry[document] = getRetry(event, request, undo)
        }

        override fun onFailure(t: Throwable) {
            error(log, "Error", t)
        }
    }

    fun getRetry(
        event: AnActionEvent,
        request: Supplier<Runnable>,
        undo: Runnable,
    ): Runnable = Runnable {
        Futures.addCallback(
            pool.submit<Runnable> {
                WriteCommandAction.runWriteCommandAction(event.project) { undo.run() }
                request.get()
            }, futureCallback(event, request), pool
        )
    }

    fun replaceString(document: Document, startOffset: Int, endOffset: Int, newText: CharSequence): Runnable {
        log.debug("Invoking replaceString with startOffset: $startOffset, endOffset: $endOffset, newText: $newText")
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
            log.debug("Verifying text after replaceString: expected: $newText, actual: $verifyTxt")
            if (verifyTxt != newText) {
                val msg = String.format(
                    "The text range from %d to %d does not match the expected text \"%s\" and is instead \"%s\"",
                    startOffset,
                    startOffset + newText.length,
                    newText,
                    verifyTxt
                )
                log.error("Verification failed after replaceString: $msg")
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
        return IndentedText.fromString(lines[max(lineNumber, 0).coerceAtMost(lines.size - 1)]).indent
    }

    @Suppress("unused")
    fun hasSelection(e: AnActionEvent): Boolean {
        val caret = e.getData(CommonDataKeys.CARET)
        return null != caret && caret.hasSelection()
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

    fun <T : Any, R : Any> readKotlinUIViaReflection(
        settings: T,
        component: R,
        componentClass: KClass<*> = component::class,
    ) {

        val declaredUIFields = componentClass.memberProperties.map { it.name }.toSet()
        for (settingsField in settings.javaClass.kotlin.memberProperties) {
            if (settingsField is KMutableProperty<*>) {
                settingsField.isAccessible = true
                val settingsFieldName = settingsField.name
                try {
                    var newSettingsValue: Any? = null
                    if (!declaredUIFields.contains(settingsFieldName)) continue
                    val uiField: KProperty1<R, *> =
                        (componentClass.memberProperties.find { it.name == settingsFieldName } as KProperty1<R, *>?)!!
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
                            newSettingsValue = if (uiVal.text.isBlank()) -1 else uiVal.text.toInt()
                        }

                        "long" -> if (uiVal is JTextComponent) {
                            newSettingsValue = if (uiVal.text.isBlank()) -1 else uiVal.text.toLong()
                        }

                        "double", "java.lang.Double" -> if (uiVal is JTextComponent) {
                            newSettingsValue = if (uiVal.text.isBlank()) 0.0 else uiVal.text.toDouble()
                        }

                        "boolean" -> if (uiVal is JCheckBox) {
                            newSettingsValue = uiVal.isSelected
                        } else if (uiVal is JTextComponent) {
                            newSettingsValue = java.lang.Boolean.parseBoolean(uiVal.text)
                        }

                        else -> if (Enum::class.java.isAssignableFrom(settingsField.returnType.javaType as Class<*>)) {
                            if (uiVal is ComboBox<*>) {
                                val comboBox = uiVal
                                val item = comboBox.item
                                val enumClass = settingsField.returnType.javaType as Class<out Enum<*>?>
                                val string = item.toString()
                                newSettingsValue = findValue(enumClass, string)
                            }
                        }
                    }
                    settingsField.setter.call(settings, newSettingsValue)
                } catch (e: Throwable) {
                    throw RuntimeException("Error processing $settingsField", e)
                }
            }
        }
    }

    private fun findValue(enumClass: Class<out Enum<*>?>, string: String): Enum<*>? {
        enumClass.enumConstants?.filter { it?.name?.compareTo(string, true) == 0 }?.forEach { return it }
        return java.lang.Enum.valueOf(
            enumClass, string
        )
    }

    fun <T : Any, R : Any> writeKotlinUIViaReflection(
        settings: T,
        component: R,
        componentClass: KClass<*>,
        settingsClass: KClass<*>
    ) {
        val declaredUIFields = componentClass.memberProperties.map { it.name }.toSet()
        val memberProperties = settings.javaClass.kotlin.memberProperties
        val publicProperties = memberProperties.filter {
            it.visibility == KVisibility.PUBLIC //&& it is KMutableProperty<*>
        }
        for (settingsField in publicProperties) {
            val fieldName = settingsField.name
            try {
                if (!declaredUIFields.contains(fieldName)) {
                    log.warn("Field not found: $fieldName")
                    continue
                }
                val uiField: KProperty1<R, Any> =
                    (componentClass.memberProperties.find { it.name == fieldName } as KProperty1<R, Any>?)!!
                val settingsVal = settingsField.get(settings) ?: continue
                var uiVal = uiField.get(component)
                if (uiVal is JScrollPane) {
                    uiVal = uiVal.viewport.view
                }
                when (settingsField.returnType.javaType.typeName) {
                    "java.lang.String" -> if (uiVal is JTextComponent) {
                        uiVal.text = settingsVal.toString()
                    } else if (uiVal is ComboBox<*>) {
                        (uiVal as ComboBox<String>).item = settingsVal.toString()
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
                        (uiVal as ComboBox<String>).item = settingsVal.toString()
                    }
                }
            } catch (e: Throwable) {
                throw RuntimeException("Error processing $settingsField", e)
            }
        }
    }

    private fun <T : Any> addKotlinFields(ui: T, formBuilder: FormBuilder, fillVertically: Boolean) {
        var first = true
        for (field in ui.javaClass.kotlin.memberProperties.filterNotNull()) {
            try {
                val nameAnnotation = field.annotations.find { it is Name } as Name?
                val component = field.get(ui) as JComponent
                if (nameAnnotation != null) {
                    if (first && fillVertically) {
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
                error(log, "Error processing " + field.name, e)
            }
        }
    }

    private fun getMaximumSize(factor: Double): Dimension {
        val screenSize = Toolkit.getDefaultToolkit().screenSize
        return Dimension((screenSize.getWidth() * factor).toInt(), (screenSize.getHeight() * factor).toInt())
    }

    private fun showOptionDialog(mainPanel: JPanel?, vararg options: Any, title: String, modal: Boolean = true): Int {
        val pane = getOptionPane(mainPanel, options)
        val rootFrame = JOptionPane.getRootFrame()
        pane.componentOrientation = rootFrame.componentOrientation
        val dialog = JDialog(rootFrame, title, modal)
        dialog.componentOrientation = rootFrame.componentOrientation

        val latch = if (!modal) CountDownLatch(1) else null
        configure(dialog, pane, latch)
        dialog.isVisible = true
        if (!modal) latch?.await()

        dialog.dispose()
        return getSelectedValue(pane, options)
    }

    private fun getOptionPane(
        mainPanel: JPanel?,
        options: Array<out Any>,
    ): JOptionPane {
        val pane = JOptionPane(
            mainPanel, JOptionPane.PLAIN_MESSAGE, JOptionPane.NO_OPTION, null, options, options[0]
        )
        pane.initialValue = options[0]
        return pane
    }

    private fun configure(dialog: JDialog, pane: JOptionPane, latch: CountDownLatch? = null) {
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
        val adapter: WindowAdapter = windowAdapter(pane, dialog)
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
                latch?.countDown()
            }
        }

        pane.selectInitialValue()
    }

    private fun windowAdapter(pane: JOptionPane, dialog: JDialog): WindowAdapter {
        val adapter: WindowAdapter = object : WindowAdapter() {
            private var gotFocus = false
            override fun windowClosing(we: WindowEvent) {
                pane.value = null
            }

            override fun windowClosed(e: WindowEvent) {
                pane.removePropertyChangeListener { event: PropertyChangeEvent ->
                    if (dialog.isVisible && event.source === pane && event.propertyName == JOptionPane.VALUE_PROPERTY && event.newValue != null && event.newValue !== JOptionPane.UNINITIALIZED_VALUE) {
                        dialog.isVisible = false
                    }
                }
                dialog.contentPane.removeAll()
            }

            override fun windowGainedFocus(we: WindowEvent) {
                if (!gotFocus) {
                    pane.selectInitialValue()
                    gotFocus = true
                }
            }
        }
        return adapter
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

    private fun wrapScrollPane(promptArea: JBTextArea?): JBScrollPane {
        val scrollPane = JBScrollPane(promptArea)
        scrollPane.horizontalScrollBarPolicy = JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED
        scrollPane.verticalScrollBarPolicy = JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED
        return scrollPane
    }

    fun showCheckboxDialog(
        promptMessage: String,
        checkboxIds: Array<String>,
        checkboxDescriptions: Array<String>,
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
        vararg radioButtonDescriptions: CharSequence,
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

    fun <T : Any> buildFormViaReflection(
        component: T,
        fillVertically: Boolean = true,
        formBuilder: FormBuilder = FormBuilder.createFormBuilder(),
    ): JPanel? {
        addKotlinFields(component, formBuilder, fillVertically)
        return formBuilder.addComponentFillVertically(JPanel(), 0).panel
    }

    fun <T : Any, C : Any> showDialog(
        project: Project?,
        uiClass: Class<T>,
        configClass: Class<C>,
        title: String = "Generate Project",
        onComplete: (C) -> Unit = { _ -> },
    ): C = showDialog<C, T>(
        project,
        uiClass.getConstructor().newInstance(),
        configClass.getConstructor().newInstance(),
        title,
        onComplete
    )

    fun <C : Any, T : Any> showDialog(
        project: Project?,
        component: T,
        config: C,
        title: String,
        onComplete: (C) -> Unit
    ): C {
        log.debug("Showing dialog with title: $title")
        val dialog = object : DialogWrapper(project) {
            init {
                this.init()
                this.title = title
                this.setOKButtonText("Generate")
                this.setCancelButtonText("Cancel")
                this.isResizable = true
                //this.setPreferredFocusedComponent(this)
                //this.setContent(this)
            }

            override fun createCenterPanel(): JComponent? {
                log.debug("Creating center panel for dialog")
                return buildFormViaReflection(component)
            }
        }
        dialog.show()
        log.debug("Dialog shown with result: ${dialog.isOK}")
        if (dialog.isOK) {
            readKotlinUIViaReflection(
                settings = config,
                component = component,
                componentClass = component::class
            )
            log.debug("Reading UI via reflection completed")
            onComplete(config)
            log.debug("onComplete callback executed")
        }
        return config
    }

    fun getSelectedFolder(e: AnActionEvent): VirtualFile? {
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
        return null
    }

    fun getSelectedFile(e: AnActionEvent): VirtualFile? {
        val dataContext = e.dataContext
        val data = PlatformDataKeys.VIRTUAL_FILE.getData(dataContext)
        if (data != null && !data.isDirectory) {
            return data
        }
        return null
    }

    fun getSelectedFiles(e: AnActionEvent): List<VirtualFile> {
        val dataContext = e.dataContext
        val data = PlatformDataKeys.VIRTUAL_FILE_ARRAY.getData(dataContext)
        if (null != data) return data.toList()
        val editor = PlatformDataKeys.EDITOR.getData(dataContext)
        if (editor != null) {
            val file = FileDocumentManager.getInstance().getFile(editor.document)
            if (file != null) { return listOf(file) }
        }
        return emptyList()
    }

    fun writeableFn(
        event: AnActionEvent,
        fn: () -> Runnable,
    ): Runnable {
        val runnable = AtomicReference<Runnable>()
        WriteCommandAction.runWriteCommandAction(event.project) { runnable.set(fn()) }
        return runnable.get()
    }

    class ModalTask<T>(
        project: Project, title: String, canBeCancelled: Boolean, val task: (ProgressIndicator) -> T
    ) : Task.WithResult<T, Exception>(project, title, canBeCancelled), Supplier<T> {
        private val taskLog = LoggerFactory.getLogger(ModalTask::class.java)
        private val result = AtomicReference<T>()
        private val isError = AtomicBoolean(false)
        private val error = AtomicReference<Throwable>()
        private val semaphore = Semaphore(0)
        private val completed = AtomicBoolean(false)
        private val threadList = Collections.synchronizedList(ArrayList<Thread>())
        private val cancelled = AtomicBoolean(false)
        private val started = AtomicBoolean(false)
        private val lock = Object()

        override fun compute(indicator: ProgressIndicator): T? {
            taskLog.debug("Starting compute() for ModalTask: $title")
            synchronized(lock) {
                taskLog.debug("Checking task state - started: ${started.get()}, completed: ${completed.get()}, cancelled: ${cancelled.get()}")
                if (!started.compareAndSet(false, true)) return null
                if (completed.get() || cancelled.get()) {
                    taskLog.debug("Task already completed or cancelled, releasing semaphore")
                    semaphore.release()
                    return null
                }
            }
            val currentThread = Thread.currentThread()
            taskLog.debug("Adding thread ${currentThread.name} to threadList")
            threadList.add(currentThread)
            val scheduledFuture = scheduledPool.scheduleAtFixedRate({
                if (indicator.isCanceled) {
                    taskLog.debug("Indicator cancelled, interrupting threads")
                    cancelled.set(true)
                    threadList.forEach { it.interrupt() }
                }
            }, 0, 1, TimeUnit.SECONDS)
            return try {
                synchronized(lock) {
                    if (completed.get() || cancelled.get()) {
                        taskLog.debug("Task completed or cancelled during execution")
                        semaphore.release()
                        return null
                    }
                }
                taskLog.debug("Executing task")
                result.set(task(indicator))
                taskLog.debug("Task completed successfully")
                result.get()
            } catch (e: Throwable) {
                taskLog.error("Error executing task", e)
                log.info("Error running task", e)
                isError.set(true)
                error.set(e)
                null
            } finally {
                synchronized(lock) {
                    taskLog.debug("Finalizing task execution")
                    completed.set(true)
                    semaphore.release()
                }
                taskLog.debug("Removing thread ${currentThread.name} from threadList")
                threadList.remove(currentThread)
                scheduledFuture.cancel(true)
            }
        }

        override fun get(): T {
            taskLog.debug("Attempting to get task result")
            try {
                val acquired = semaphore.tryAcquire(30, TimeUnit.SECONDS)
                taskLog.debug("Semaphore acquired: $acquired")
                synchronized(lock) {
                    if (!started.get() || !acquired) {
                        taskLog.error("Task timed out or never started")
                        cancelled.set(true)
                        throw TimeoutException("Task timed out after 30 seconds")
                    }
                }
            } finally {
                semaphore.release()
            }
            synchronized(lock) {
                taskLog.debug("Checking final task state - completed: ${completed.get()}, error: ${isError.get()}, cancelled: ${cancelled.get()}")
                if (!completed.get()) {
                    throw IllegalStateException(
                        "Task not completed" +
                                (if (cancelled.get()) " (cancelled)" else "")
                    )
                }
                if (isError.get()) {
                    val e = error.get() ?: RuntimeException("Unknown error occurred")
                    taskLog.error("Task failed with error", e)
                    throw e
                }
                if (cancelled.get()) {
                    taskLog.debug("Task was cancelled")
                    throw InterruptedException("Task was cancelled")
                }
                taskLog.debug("Returning successful task result")
                return result.get() ?: throw IllegalStateException("No result available")
            }
        }

        override fun onCancel() {
            taskLog.debug("Task cancelled")
            super.onCancel()
            synchronized(lock) {
                cancelled.set(true)
                threadList.forEach { it.interrupt() }
                semaphore.release()
            }
        }

    }

    class BgTask<T>(
        project: Project, title: String, canBeCancelled: Boolean, val task: (ProgressIndicator) -> T
    ) : Task.Backgroundable(project, title, canBeCancelled, DEAF), Supplier<T> {
        private val taskLog = LoggerFactory.getLogger(BgTask::class.java)

        private val result = AtomicReference<T>()
        private val isError = AtomicBoolean(false)
        private val error = AtomicReference<Throwable>()
        private val startSemaphore = Semaphore(0)
        private val completeSemaphore = Semaphore(0)
        private val completed = AtomicBoolean(false)
        private val threadList = Collections.synchronizedList(ArrayList<Thread>())
        private val cancelled = AtomicBoolean(false)
        private val started = AtomicBoolean(false)
        private val lock = Object()

        override fun run(indicator: ProgressIndicator) {
            taskLog.debug("Starting run() for BgTask: $title")
            synchronized(lock) {
                taskLog.debug("Checking task state - started: ${started.get()}, completed: ${completed.get()}, cancelled: ${cancelled.get()}")
                if (!started.compareAndSet(false, true)) return
                if (completed.get() || cancelled.get()) {
                    taskLog.debug("Task already completed or cancelled, releasing semaphore")
                    startSemaphore.release()
                    completeSemaphore.release()
                    return
                }
            }
            startSemaphore.release()
            val currentThread = Thread.currentThread()
            taskLog.debug("Adding thread ${currentThread.name} to threadList")
            threadList.add(currentThread)
            val scheduledFuture = scheduledPool.scheduleAtFixedRate({
                if (indicator.isCanceled) {
                    taskLog.debug("Indicator cancelled, interrupting threads")
                    cancelled.set(true)
                    threadList.forEach { it.interrupt() }
                }
            }, 0, 1, TimeUnit.SECONDS)
            try {
                synchronized(lock) {
                    if (completed.get() || cancelled.get()) {
                        taskLog.debug("Task completed or cancelled during execution")
                        completeSemaphore.release()
                        return
                    }
                }
                taskLog.debug("Executing task")
                val result = task(indicator)
                this.result.set(result)
                taskLog.debug("Task completed successfully")
            } catch (e: Throwable) {
                taskLog.error("Error executing task", e)
                log.info("Error running task", e)
                error.set(e)
                isError.set(true)
            } finally {
                synchronized(lock) {
                    taskLog.debug("Finalizing task execution")
                    completed.set(true)
                    completeSemaphore.release()
                }
                taskLog.debug("Removing thread ${currentThread.name} from threadList")
                threadList.remove(currentThread)
                scheduledFuture.cancel(true)
            }
        }

        override fun get(): T {
            taskLog.debug("Attempting to get task result")
            try {
                // Wait for task to start
                val startAcquired = startSemaphore.tryAcquire(5, TimeUnit.SECONDS)
                taskLog.debug("Start semaphore acquired: $startAcquired")
                synchronized(lock) {
                    if (!started.get() || !startAcquired) {
                        taskLog.error("Task timed out or never started")
                        cancelled.set(true)
                        throw TimeoutException("Task failed to start after 5 seconds")
                    }
                }
                // Wait for task to complete
                val completeAcquired = completeSemaphore.tryAcquire(3000, TimeUnit.SECONDS)
                taskLog.debug("Complete semaphore acquired: $completeAcquired")
                if (!completeAcquired) {
                    taskLog.error("Task execution timed out")
                    cancelled.set(true)
                    throw TimeoutException("Task execution timed out after 30 seconds")
                }
            } finally {
                startSemaphore.release()
                completeSemaphore.release()
            }
            synchronized(lock) {
                taskLog.debug("Checking final task state - completed: ${completed.get()}, error: ${isError.get()}, cancelled: ${cancelled.get()}")
                if (!completed.get()) {
                    throw IllegalStateException(
                        "Task not completed" +
                                (if (cancelled.get()) " (cancelled)" else "")
                    )
                }
                if (isError.get()) {
                    val e = error.get() ?: RuntimeException("Unknown error occurred")
                    taskLog.error("Task failed with error", e)
                    throw e
                }
                if (cancelled.get()) {
                    taskLog.debug("Task was cancelled")
                    throw InterruptedException("Task was cancelled")
                }
                taskLog.debug("Returning successful task result")
                return result.get() ?: throw IllegalStateException("No result available")
            }
        }

        override fun onCancel() {
            taskLog.debug("Task cancelled")
            super.onCancel()
            synchronized(lock) {
                cancelled.set(true)
                threadList.forEach { it.interrupt() }
                startSemaphore.release()
                completeSemaphore.release()
            }
        }
    }

    fun <T : Any> run(
        project: Project?,
        title: String?,
        canBeCancelled: Boolean = true,
        suppressProgress: Boolean = true,
        task: (ProgressIndicator) -> T,
    ): T {
        return if (project == null || suppressProgress == AppSettingsState.instance.editRequests) {
            checkApiKey()
            task(AbstractProgressIndicatorBase())
        } else {
            checkApiKey()
            val t = if (AppSettingsState.instance.modalTasks) ModalTask(project, title ?: "", canBeCancelled, task)
            else BgTask(project, title ?: "", canBeCancelled, task)
            ProgressManager.getInstance().run(t)
            t.get()
        }
    }


    fun runAsync(
        project: Project?,
        title: String?,
        canBeCancelled: Boolean = true,
        suppressProgress: Boolean = true,
        task: (ProgressIndicator) -> Unit,
    ) {
        Thread {
            try {
                if (project == null || suppressProgress == AppSettingsState.instance.editRequests) {
                    checkApiKey()
                    task(AbstractProgressIndicatorBase())
                } else {
                    checkApiKey()
                    val t = if (AppSettingsState.instance.modalTasks) ModalTask(project, title ?: "", canBeCancelled, task)
                    else BgTask(project, title ?: "", canBeCancelled, task)
                    ProgressManager.getInstance().run(t)
                    t.get()
                }
            } catch (e: Throwable) {
                error(log, "Error running task", e)
                showError(project, "Failed to initialize chat: ${e.message}")
            }
        }.apply {
            name = title
        }.start()
    }

    fun checkApiKey(k: String = AppSettingsState.instance.apiKey?.values?.firstOrNull() ?: ""): String {
//    var key = k
//    if (key.isEmpty() || key != AppSettingsState.instance.apiKey) {
//      synchronized(OpenAIClient::class.java) {
//        key = AppSettingsState.instance.apiKey
//        if (key.isEmpty()) {
//          key = queryAPIKey()?.toString() ?: ""
//          if (key.isNotEmpty()) AppSettingsState.instance.apiKey = key
//        }
//      }
//    }
        return k
    }


    fun <I : Any?, O : Any?> map(
        moderateAsync: ListenableFuture<I>,
        o: com.google.common.base.Function<in I, out O>,
    ): ListenableFuture<O> = Futures.transform(moderateAsync, o::apply, pool)


    fun logAction(message: String) {
        actionLog += message
    }


    fun error(log: org.slf4j.Logger, msg: String, e: Throwable) {
        log.error(msg, e)
        errorLog += Pair(msg, e)
        singleThreadPool.submit {
            if (AppSettingsState.instance.suppressErrors) {
                return@submit
            } else if (e.matches { ModerationException::class.java.isAssignableFrom(it.javaClass) }) {
                JOptionPane.showMessageDialog(
                    null, e.message, "This request was rejected by OpenAI Moderation", JOptionPane.WARNING_MESSAGE
                )
            } else if (e.matches {
                    java.lang.InterruptedException::class.java.isAssignableFrom(it.javaClass) && it.message?.contains(
                        "sleep interrupted"
                    ) == true
                }) {
                JOptionPane.showMessageDialog(
                    null,
                    "This request was cancelled by the user",
                    "User Cancelled Request",
                    JOptionPane.WARNING_MESSAGE
                )
            } else if (e.matches { IOException::class.java.isAssignableFrom(it.javaClass) && it.message?.contains("Incorrect API key") == true }) {

                val formBuilder = FormBuilder.createFormBuilder()

                formBuilder.addLabeledComponent(
                    "Error", JLabel("The API key was rejected by the server.")
                )

                val apiKeyInput = JBPasswordField()
                //bugReportTextArea.rows = 40
                apiKeyInput.columns = 80
                apiKeyInput.isEditable = true
                //apiKeyInput.text = """""".trimMargin()
                formBuilder.addLabeledComponent("API Key", apiKeyInput)

                val openAccountButton = JXButton("Open Account Page")
                openAccountButton.addActionListener {
                    browse(URI("https://platform.openai.com/account/api-keys"))
                }
                formBuilder.addLabeledComponent("OpenAI Account", openAccountButton)

                val testButton = JXButton("Test Key")
                testButton.addActionListener {
                    val apiKey = apiKeyInput.password.joinToString("")
                    try {
                        OpenAIClient(
                            key = mapOf(
                                APIProvider.OpenAI to apiKey
                            )
                        ).listModels()
                        JOptionPane.showMessageDialog(
                            null,
                            "The API key was accepted by the server. The new value will be saved.",
                            "Success",
                            JOptionPane.INFORMATION_MESSAGE
                        )
                        AppSettingsState.instance.apiKey = mapOf(APIProvider.OpenAI.name to apiKey).toMutableMap()
                    } catch (e: Exception) {
                        JOptionPane.showMessageDialog(
                            null, "The API key was rejected by the server.", "Failure", JOptionPane.WARNING_MESSAGE
                        )
                        return@addActionListener
                    }
                }
                formBuilder.addLabeledComponent("Validation", testButton)
                val showOptionDialog = showOptionDialog(
                    formBuilder.panel, "Dismiss", title = "Error", modal = true
                )
                log.info("showOptionDialog = $showOptionDialog")
            } else {
                val formBuilder = FormBuilder.createFormBuilder()

                formBuilder.addLabeledComponent(
                    "Error",
                    JLabel("Oops! Something went wrong. An error report has been generated. You can copy and paste the report below into a new issue on our Github page.")
                )

                val bugReportTextArea = JBTextArea()
                bugReportTextArea.rows = 40
                bugReportTextArea.columns = 80
                bugReportTextArea.isEditable = false
                bugReportTextArea.text = """
          |Log Message: $msg
          |Error Message: ${e.message}
          |Error Type: ${e.javaClass.name}
          |API Base: ${AppSettingsState.instance.apiBase}
          |
          |OS: ${System.getProperty("os.name")} / ${System.getProperty("os.version")} / ${System.getProperty("os.arch")}
          |Locale: ${Locale.getDefault().country} / ${Locale.getDefault().language}
          |
          |Error Details:
          |```
          |${toString(e)/*.indent("  ")*/}
          |```
          |
          |Action History:
          |
          |${actionLog.joinToString("\n") { "* ${it.replace("\n", "\n  ")}" }}
          |
          |Error History:
          |
          |${
                    errorLog.filter { it.second != e }.joinToString("\n") {
                        """
            |${it.first}
            |```
            |${toString(it.second)/*.indent("  ")*/}
            |```
            |""".trimMargin()
                    }
                }
                |""".trimMargin()
                formBuilder.addLabeledComponent("System Report", wrapScrollPane(bugReportTextArea))

                val openButton = JXButton("Open New Issue on our Github page")
                openButton.addActionListener {
                    browse(URI("https://github.com/SimiaCryptus/intellij-aicoder/issues/new"))
                }
                formBuilder.addLabeledComponent("Report Issue/Request Help", openButton)

                val supressFutureErrors = JCheckBox("Suppress Future Error Popups")
                supressFutureErrors.isSelected = false
                formBuilder.addComponent(supressFutureErrors)

                val showOptionDialog = showOptionDialog(
                    formBuilder.panel, "Dismiss", title = "Error", modal = true
                )
                log.info("showOptionDialog = $showOptionDialog")
                if (supressFutureErrors.isSelected) {
                    AppSettingsState.instance.suppressErrors = true
                }
            }
        }
    }

    private fun Throwable.matches(matchFn: (Throwable) -> Boolean): Boolean {
        if (matchFn(this)) return true
        if (this.cause != null && this.cause !== this) return this.cause!!.matches(matchFn)
        return false
    }

    fun Throwable.get(matchFn: (Throwable) -> Boolean): Throwable? {
        if (matchFn(this)) return this
        if (this.cause != null && this.cause !== this) return this.cause!!.get(matchFn)
        return null
    }

    fun toString(e: Throwable): String {
        val sw = StringWriter()
        val pw = PrintWriter(sw)
        e.printStackTrace(pw)
        return sw.toString()
    }

    fun showInputDialog(
        parentComponent: Component?, message: Any?, title: String?, messageType: Int
    ): Any? {
        val icon = null
        val selectionValues = null
        val initialSelectionValue = null
        val pane = JOptionPane(
            message, messageType, JOptionPane.OK_CANCEL_OPTION, icon, null, null
        )
        pane.wantsInput = true
        pane.selectionValues = selectionValues
        pane.initialSelectionValue = initialSelectionValue
        //pane.isComponentOrientationLeftToRight = true
        val dialog = pane.createDialog(parentComponent, title)
        pane.selectInitialValue()
        dialog.isVisible = true
        dialog.dispose()
        val value = pane.inputValue
        return if (value == JOptionPane.UNINITIALIZED_VALUE) null else value
    }

    fun showErrorDialog(project: Project?, errorMessage: String, title: String) {
        val formBuilder = FormBuilder.createFormBuilder()
        formBuilder.addComponent(JLabel(errorMessage))
        showOptionDialog(formBuilder.panel, "OK", title = title, modal = true)
    }

    fun showErrorDetails(project: Project?, e: Throwable) {
        val sw = StringWriter()
        e.printStackTrace(PrintWriter(sw))
        val formBuilder = FormBuilder.createFormBuilder()
        val textArea = JBTextArea(sw.toString())
        textArea.isEditable = false
        formBuilder.addComponent(JBScrollPane(textArea))
        showOptionDialog(formBuilder.panel, "OK", title = "Error Details", modal = true)
    }

    fun showInfoMessage(project: Project?, message: String, title: String) {
        val formBuilder = FormBuilder.createFormBuilder()
        formBuilder.addComponent(JLabel(message))
        showOptionDialog(formBuilder.panel, "OK", title = title, modal = true)
    }
}