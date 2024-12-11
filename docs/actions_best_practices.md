# AI Coder Action Best Practices

## 1. Choosing the Right Base Class

Choose the appropriate base class for your action:

### BaseAction

Basic usage:

```kotlin
class SimpleAction : BaseAction(
    name = "My Action",
    description = "Does something simple"
) {
    override fun handle(event: AnActionEvent) {
        val project = event.project ?: return
        UITools.run(project, "Processing", true) { progress ->
            // Action logic here with progress indication
            progress.text = "Doing work..."
        }
    }
}
```

### SelectionAction<T>

* Provides automatic selection handling and language detection
* Example implementations: ApplyPatchAction, CodeFormatterAction
  Advanced usage:

```kotlin
class CodeFormatterAction : SelectionAction<FormatterConfig>() {
    // Custom configuration class
    data class FormatterConfig(
        val style: String,
        val indent: Int
    )

    override fun getConfig(project: Project?): FormatterConfig? {
        return FormatterConfig(
            style = "google",
            indent = 4
        )
    }

    override fun processSelection(state: SelectionState, config: FormatterConfig?): String {
        // Access selection context
        val code = state.selectedText ?: return ""
        val lang = state.language ?: return code
        val indent = state.indent?.toString() ?: "    "
        // Process with configuration
        return formatCode(code, lang, config?.style, config?.indent ?: 4)
    }
}
```

### FileContextAction<T>

* Provides progress indication and cancellation support
* Manages file refresh and editor updates
  Complete example:

```kotlin
class FileProcessorAction : FileContextAction<ProcessorConfig>() {
    data class ProcessorConfig(
        val outputDir: String,
        val options: Map<String, String>
    )

    override fun getConfig(project: Project?, e: AnActionEvent): ProcessorConfig? {
        val dir = UITools.chooseDirectory(project, "Select Output Directory")
        return dir?.let { ProcessorConfig(it.path, mapOf()) }
    }

    override fun processSelection(state: SelectionState, config: ProcessorConfig?, progress: ProgressIndicator): Array<File> {
        progress.text = "Processing ${state.selectedFile.name}"
        val outputFile = File(config?.outputDir, "processed_${state.selectedFile.name}")
        outputFile.parentFile.mkdirs()
        // Process file with progress updates
        progress.isIndeterminate = false
        processFileWithProgress(state.selectedFile, outputFile, progress)
        return arrayOf(outputFile)
    }
}
```

## Core Principles

### 1. Thread Safety

Best practices for thread management:

* Use WriteCommandAction for document modifications
* Avoid blocking EDT (Event Dispatch Thread)
* Handle background tasks properly

```kotlin
// Good example * proper thread handling:
WriteCommandAction.runWriteCommandAction(project) {
    try {
        UITools.run(project, "Processing", true) { progress ->
            progress.isIndeterminate = false
            progress.text = "Processing..."
            ApplicationManager.getApplication().executeOnPooledThread {
                // Long running operation
                progress.fraction = 0.5
            }
        }
    } catch (e: Throwable) {
        UITools.error(log, "Error", e)
    }
}

// Bad example * avoid:
Thread {
    document.setText("new text") // Don't modify documents outside WriteCommandAction
}.start() 
```

### 2. Error Handling

Comprehensive error handling strategy:

* Use structured error handling
* Provide user feedback
* Log errors appropriately

```kotlin
// Good example:
try {
    processFile(file)
} catch (e: IOException) {
    log.error("Failed to process file: ${file.path}", e)
    val choice = UITools.showErrorDialog(
        project,
        "Failed to process file: ${e.message}\nWould you like to retry?",
        "Error",
        arrayOf("Retry", "Cancel")
    )
    if (choice == 0) {
        // Retry logic
        processFile(file)
    }
} finally {
    cleanup()
}
```

### 3. Progress Indication

Guidelines for progress feedback:

* Update progress frequently
* Include operation details in progress text

```kotlin
UITools.run(project, "Processing Files", true) { progress ->
    progress.text = "Initializing..."
    files.forEachIndexed { index, file ->
        if (progress.isCanceled) throw InterruptedException()
        progress.fraction = index.toDouble() / files.size
        progress.text = "Processing ${file.name} (${index + 1}/${files.size})"
        processFile(file)
    }
}
```

### 4. Configuration

* Use `getConfig()` to get user input/settings
* Validate configurations before proceeding
* Store recent configurations where appropriate
* Use `AppSettingsState` for persistent settings

```kotlin
override fun getConfig(project: Project?, e: AnActionEvent): Settings {
    return Settings(
        UITools.showDialog(
            project,
            SettingsUI::class.java,
            UserSettings::class.java,
            "Dialog Title"
        ),
        project
    )
}
```

### 5. Action Enablement

* Implement `isEnabled()` to control when action is available
* Check for null safety
* Verify required context (files, selection, etc)
* Consider language support requirements

```kotlin
override fun isEnabled(event: AnActionEvent): Boolean {
    if (!super.isEnabled(event)) return false
    val file = UITools.getSelectedFile(event) ?: return false
    return isLanguageSupported(getComputerLanguage(event))
}
```

### 6. UI Integration

* Use IntelliJ UI components and dialogs
* Follow IntelliJ UI guidelines
* Provide appropriate feedback to users
* Support undo/redo operations

### 7. AI Integration

* Use appropriate models for the task
* Handle API errors gracefully
* Provide meaningful prompts
* Process AI responses appropriately

```kotlin
val response = ChatProxy(
    clazz = API::class.java,
    api = api,
    model = AppSettingsState.instance.smartModel.chatModel(),
    temperature = AppSettingsState.instance.temperature
).create()
```

### 8. Code Organization

* Keep actions focused on a single responsibility
* Extract common functionality to utility classes
* Use appropriate inheritance hierarchy
* Follow Kotlin coding conventions

### 9. Documentation

* Document action purpose and usage
* Include example usage where helpful
* Document configuration options
* Explain any special requirements

### 10. Testing

* Test action enablement logic
* Test configuration validation
* Test error handling
* Test with different file types/languages

## Common Patterns

### Chat Actions

* Use `SessionProxyServer` for chat interfaces
* Configure appropriate chat models
* Handle chat session lifecycle
* Support conversation context

### File Operations

* Use `FileContextAction` base class
* Handle file system operations safely
* Support undo/redo
* Refresh file system after changes

### Code Modifications

* Use `SelectionAction` for code changes
* Support appropriate languages
* Handle code formatting
* Preserve indentation

## Specific Guidelines

### For AI Code Generation

1. Use appropriate temperature settings
2. Validate generated code
3. Format code appropriately
4. Handle partial generations

### For File Operations

1. Check file existence
2. Handle file permissions
3. Support large files
4. Refresh file system

### For UI Integration

1. Use progress indicators
2. Show meaningful messages
3. Support cancellation
4. Follow IntelliJ guidelines

## Best Practices Checklist

* [ ] Extends appropriate base class
* [ ] Implements proper error handling
* [ ] Shows progress for long operations
* [ ] Validates configurations
* [ ] Implements proper enablement logic
* [ ] Follows UI guidelines
* [ ] Handles AI integration properly
* [ ] Includes proper documentation
* [ ] Supports undo/redo
* [ ] Includes appropriate tests