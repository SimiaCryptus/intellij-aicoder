Here's a best practices document for actions in this project:

# AI Coder Action Best Practices

## Action Types

The project has several base action types that should be used appropriately:

1. `BaseAction` - Base class for all actions
2. `SelectionAction<T>` - For actions that operate on selected text
3. `FileContextAction<T>` - For actions that operate on files/folders

Choose the appropriate base class based on your action's needs.

## Core Principles

### 1. Thread Safety

- Always run long operations in background threads
- Use `WriteCommandAction` for document modifications
- Protect shared resources with appropriate synchronization


```kotlin
Thread {
  try {
    UITools.redoableTask(e) {
      // Long running operation
    }
  } catch (e: Throwable) {
    UITools.error(log, "Error", e) 
  }
}.start()
```

### 2. Error Handling

- Wrap operations in try-catch blocks
- Log errors appropriately using the logger
- Show user-friendly error messages
- Make actions fail gracefully


```kotlin
try {
  // Action logic
} catch (e: Exception) {
  log.error("Error in action", e)
  UITools.showErrorDialog(project, e.message, "Error")
}
```


### 3. Progress Indication

- Use progress indicators for long operations
- Show meaningful progress messages
- Allow cancellation where appropriate


```kotlin
UITools.run(project, "Operation Name", true) { progress ->
  progress.text = "Step description..."
  progress.fraction = 0.5
  // Operation logic
}
```

### 4. Configuration

- Use `getConfig()` to get user input/settings
- Validate configurations before proceeding
- Store recent configurations where appropriate
- Use `AppSettingsState` for persistent settings


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

- Implement `isEnabled()` to control when action is available
- Check for null safety
- Verify required context (files, selection, etc)
- Consider language support requirements


```kotlin
override fun isEnabled(event: AnActionEvent): Boolean {
  if (!super.isEnabled(event)) return false
  val file = UITools.getSelectedFile(event) ?: return false
  return isLanguageSupported(getComputerLanguage(event))
}
```

### 6. UI Integration

- Use IntelliJ UI components and dialogs
- Follow IntelliJ UI guidelines
- Provide appropriate feedback to users
- Support undo/redo operations

### 7. AI Integration

- Use appropriate models for the task
- Handle API errors gracefully
- Provide meaningful prompts
- Process AI responses appropriately


```kotlin
val response = ChatProxy(
  clazz = API::class.java,
  api = api,
  model = AppSettingsState.instance.smartModel.chatModel(),
  temperature = AppSettingsState.instance.temperature
).create()
```

### 8. Code Organization

- Keep actions focused on a single responsibility
- Extract common functionality to utility classes
- Use appropriate inheritance hierarchy
- Follow Kotlin coding conventions

### 9. Documentation

- Document action purpose and usage
- Include example usage where helpful
- Document configuration options
- Explain any special requirements

### 10. Testing

- Test action enablement logic
- Test configuration validation
- Test error handling
- Test with different file types/languages

## Common Patterns

### Chat Actions

- Use `SessionProxyServer` for chat interfaces
- Configure appropriate chat models
- Handle chat session lifecycle
- Support conversation context

### File Operations

- Use `FileContextAction` base class
- Handle file system operations safely
- Support undo/redo
- Refresh file system after changes

### Code Modifications

- Use `SelectionAction` for code changes
- Support appropriate languages
- Handle code formatting
- Preserve indentation

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

- [ ] Extends appropriate base class
- [ ] Implements proper error handling
- [ ] Shows progress for long operations
- [ ] Validates configurations
- [ ] Implements proper enablement logic
- [ ] Follows UI guidelines
- [ ] Handles AI integration properly
- [ ] Includes proper documentation
- [ ] Supports undo/redo
- [ ] Includes appropriate tests
