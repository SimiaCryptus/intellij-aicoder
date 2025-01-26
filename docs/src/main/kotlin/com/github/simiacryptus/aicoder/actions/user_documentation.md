# ApplyPatchAction.kt

Here's the user documentation for the ApplyPatchAction class:

## Apply Patch Action

### Overview

The Apply Patch action allows you to apply patch content to one or more selected files in your project. This is useful when you want to make changes to files
using patch format rather than direct edits.

### Usage

1. Select one or more files in your project that you want to apply the patch to
2. Right-click and select "Apply Patch" from the context menu (or use the assigned shortcut if configured)
3. In the dialog that appears, enter or paste the patch content
4. Click OK to apply the patch

### Details

- The action will apply the same patch content to all selected files
- The patch should be in a standard patch/diff format
- If the patch cannot be applied cleanly, the operation will fail
- Changes are applied within a write command action to ensure proper undo/redo functionality
- The action runs in the background thread to avoid blocking the UI

### Requirements

- One or more files must be selected in the project
- You must have write permissions for the selected files
- The project must be open
- Valid patch content must be provided

### Tips

- Make sure your patch content is properly formatted before applying
- Test patches on a backup copy first if you're unsure about the changes
- Use version control to track changes and revert if needed
- The patch dialog supports multiline input for complex patches

### Error Handling

The action includes several safeguards:

- Validates project and file selection
- Requires non-empty patch content
- Executes changes in a write command action for safety
- Changes can be undone using standard IDE undo functionality

# OpenWebPageAction.kt

Here's the documentation for the OpenWebPageAction class:

## OpenWebPageAction

A simple action that opens a specific web page in the user's default browser.

### Overview

`OpenWebPageAction` is an IntelliJ IDEA plugin action that opens the SimiaCryptus applications website (http://apps.simiacrypt.us/) when triggered.

### Usage

This action can be triggered from anywhere within the IDE where actions are available (menus, toolbars, etc.). When activated, it will:

1. Open the user's default web browser
2. Navigate to http://apps.simiacrypt.us/

### Technical Details

- Extends `AnAction` from the IntelliJ Platform SDK
- Uses the `BrowseUtil.browse()` utility method to handle browser opening
- No configuration required - the URL is hardcoded

### Implementation

The action is implemented as a simple override of `actionPerformed()` that calls the browse utility with a fixed URI.

### Dependencies

- Requires `com.simiacryptus.aicoder.util.BrowseUtil` for browser handling
- Uses Java's `URI` class for URL representation

### Example

```kotlin
// The action will be triggered automatically by the IDE when the user activates it
val action = OpenWebPageAction()
```

# code\CustomEditAction.kt

Here's the user documentation for the CustomEditAction class:

## Custom Edit Action

The Custom Edit Action allows you to perform AI-powered custom edits on selected code in your IDE. This action provides a flexible way to modify code using
natural language instructions.

### Features

- Supports editing code in multiple programming languages
- Uses AI to interpret and apply custom editing instructions
- Preserves code context and language-specific formatting
- Maintains edit history for reuse of common instructions

### Usage

1. Select the code you want to edit in the editor
2. Trigger the Custom Edit action (via menu or shortcut)
3. Enter your editing instruction in the dialog that appears
4. The AI will process your instruction and apply the changes to the selected code

### Examples of Edit Instructions

- "Add documentation comments"
- "Optimize this code for performance"
- "Convert this loop to use streams"
- "Add error handling"
- "Refactor to follow clean code principles"

### Technical Details

- The action uses the configured AI model from your settings
- Editing temperature can be adjusted in settings to control creativity/consistency
- Supports the human language specified in your settings
- Previous edit instructions are saved in history for quick reuse

### Error Handling

- If an error occurs during processing, an error dialog will be shown
- The original code selection is preserved if the edit fails
- Errors are logged for troubleshooting

### Configuration

The action uses several settings from AppSettingsState:

- AI model selection
- Temperature setting
- Human language preference
- Edit history management

### Tips

- Be specific in your edit instructions for best results
- Use language-appropriate terminology in your instructions
- Review the changes before accepting them
- Use the edit history to reuse successful instructions

# code\DescribeAction.kt

Here's the user documentation for the DescribeAction class:

## DescribeAction Documentation

### Overview

The DescribeAction is an IntelliJ IDEA plugin action that generates natural language descriptions of selected code snippets. It helps developers understand code
by providing human-readable explanations of what the code does.

### Features

- Automatically detects the programming language of the selected code
- Generates descriptions in the user's configured human language
- Formats the description as appropriate code comments (line or block comments)
- Preserves code indentation
- Works with any programming language supported by IntelliJ IDEA

### Usage

1. Select a block of code in the editor that you want to describe
2. Invoke the action through:

- The context menu (right-click)
- A keyboard shortcut (if configured)
- The Actions menu

The action will:

1. Analyze the selected code
2. Generate a natural language description
3. Insert the description as a comment above the selected code
4. Preserve the original code indentation and formatting

### Requirements

- The action only works when there is text selected in the editor
- The file's programming language should be recognized by IntelliJ IDEA for optimal comment formatting

### Configuration

The action uses these settings from the plugin configuration:

- Human Language: The language used for generating descriptions
- Temperature: Controls the creativity/randomness of the generated descriptions
- Model: The AI model used for generating descriptions

### Example

```java
// This method calculates the factorial of a given number using recursion
public int factorial(int n) {
    if (n <= 1) return 1;
    return n * factorial(n-1);
}
```

### Notes

- The description format (line vs. block comments) is automatically chosen based on the length of the generated description
- Single-line descriptions use line comments
- Multi-line descriptions use block comments
- If the programming language is not recognized, the description will be inserted without comment formatting

### Error Handling

If an error occurs during description generation, an error message will be logged and the exception will be propagated to the user interface.

# code\PasteAction.kt

Here's the user documentation for the PasteAction code:

## Smart Paste Actions Documentation

### Overview

The Smart Paste functionality provides enhanced clipboard paste operations that automatically detect and convert content between different formats and
programming languages. There are two variants available:

- **Smart Paste**: Uses a more sophisticated AI model for higher quality conversions
- **Fast Paste**: Uses a simpler, faster AI model for quicker conversions

### Features

#### Clipboard Content Support

- Plain text
- HTML content (with automatic cleanup)
- Code snippets
- Rich text

#### Key Capabilities

- Automatic language detection of source content
- Conversion to target language/format based on current file type
- HTML content optimization and cleanup
- Support for all programming languages
- Preserves important formatting and structure

### Usage

1. Copy content to your clipboard from any source
2. Place cursor at desired paste location in editor
3. Use one of the paste actions:

- Smart Paste: For highest quality conversion
- Fast Paste: For quicker conversion of simpler content

The action will automatically:

1. Detect the content type and format
2. Clean up HTML if present
3. Convert to appropriate format for current file
4. Insert the converted content at cursor location

### Supported Scenarios

- Converting between programming languages
- Pasting HTML content as code
- Formatting text content to match current file
- Cleaning up and normalizing code snippets

### Notes

- Smart Paste provides better results but may be slower
- Fast Paste is optimized for speed but may produce simpler conversions
- HTML content is automatically cleaned and optimized before conversion
- The action is only enabled when valid content is available in clipboard
- Conversion quality depends on source content format and complexity

### Error Handling

- Invalid clipboard content will be ignored
- Conversion failures will be logged
- Maximum content size limits prevent processing extremely large content
- Malformed HTML will be automatically repaired when possible

The paste actions aim to provide seamless conversion of clipboard content while maintaining code quality and formatting standards of the target file.

# code\RecentCodeEditsAction.kt

Here's the user documentation for the RecentCodeEditsAction class:

## Recent Code Edits Action

The Recent Code Edits action provides quick access to your most recently used custom code edit commands through a dropdown menu in the IDE.

### Features

- Displays up to 10 of your most recently used custom edit commands
- Keyboard shortcuts for quick access (1-9 for first 9 items)
- Each menu item shows the command text and executes it on the current selection
- Only enabled when code is selected (not plain text)

### Usage

1. Select some code in the editor
2. Click the Recent Code Edits action button or use its shortcut
3. Choose a recent command from the dropdown menu:

- Use number keys 1-9 for quick access to first 9 items
- Click any item to execute that command
- Commands are shown with most recent first

### Requirements

- Must have text selected in the editor
- Selected text must be in a programming language file (not plain text)
- Must have previously used custom edit commands to populate the history

### Technical Details

- Commands are stored in the application settings under "customEdits"
- Up to 10 most recent unique commands are shown
- Menu items are numbered from 1-10 for reference
- Each menu item executes the command using CustomEditAction functionality

### Related Features

- CustomEditAction - For creating new custom edit commands
- AppSettingsState - Stores command history
- UITools - Handles selection state

This action helps improve productivity by providing quick access to your frequently used custom edit commands without having to retype them.

# code\RedoLast.kt

Here's the user documentation for the RedoLast action:

## RedoLast Action

### Overview

The RedoLast action allows you to repeat the most recent AI Coder operation that was performed in your editor. This is useful when you want to apply the same
AI-powered modification again without having to reconfigure the action.

### Usage

#### How to Access

You can access the RedoLast action in several ways:

1. Through the editor context menu (right-click menu)
2. Using the assigned keyboard shortcut (if configured)
3. Via the AI Coder actions menu

#### Prerequisites

- An active editor window must be open
- At least one previous AI Coder action must have been performed in the current editor session

#### Steps

1. Position your cursor in the editor where you want to redo the last action
2. Trigger the RedoLast action using one of the access methods mentioned above
3. The last AI Coder action will be automatically repeated at the current location

### Behavior

- The action will only be enabled if there is a previous AI Coder action available to redo
- The action operates on the current editor document
- The redo operation runs in the background to avoid blocking the UI

### Limitations

- Only the most recent AI Coder action can be redone
- The action state is specific to each editor document
- The redo history is not preserved between IDE sessions

### Tips

- Use this feature to quickly apply repetitive AI-powered modifications
- The action is particularly useful when you need to apply the same AI transformation to different parts of your code

### Related Features

- Other AI Coder editing actions
- Standard IDE undo/redo operations (these are separate from the AI Coder redo functionality)

# dev\LineFilterChatAction.kt

Here's the user documentation for the LineFilterChatAction:

## Line Filter Chat Action

The Line Filter Chat Action provides an interactive way to discuss and analyze code with an AI assistant, with special support for referencing specific lines of
code.

### Overview

This action opens a chat interface where you can discuss code with an AI assistant. The assistant has access to the full context of your selected code or file,
and can reference specific line numbers in its responses.

### Usage

1. Select code in the editor (optional - if no selection is made, the entire file will be used)
2. Invoke the action through:

- Search actions (Ctrl+Shift+A / ⌘⇧A) and search for "Line Filter Chat"
- Or through any configured keyboard shortcuts

A browser window will open with a chat interface where you can:

- Ask questions about the code
- Request explanations
- Get suggestions for improvements
- Reference specific lines by their number

### Features

- **Line Number References**: The AI can reference specific lines of code using line numbers, making discussions more precise
- **Code Context**: The AI has full context of your code file including:
  - File name
  - Programming language
  - Complete code content
- **Markdown Support**: Responses are formatted in Markdown for better readability
- **Persistent Sessions**: Chat sessions are saved and can be referenced later

### Example Usage

You can ask questions like:

- "Can you explain what lines 10-15 do?"
- "Is there a better way to implement the function at line 25?"
- "What's the purpose of the variable defined on line 8?"

The AI will respond with explanations that can include direct line references and formatted code blocks.

### Notes

- This feature is only available when developer actions are enabled in the plugin settings
- The chat interface opens in your default web browser
- Sessions are automatically named with a timestamp for easy reference

### Requirements

- Plugin must be properly configured with API access
- Developer actions must be enabled in plugin settings
- A working internet connection for AI communication

# dev\PrintTreeAction.kt

Here's the user documentation for the PrintTreeAction class:

## PrintTreeAction Documentation

### Overview

PrintTreeAction is a developer utility action in IntelliJ that allows you to print the PSI (Program Structure Interface) tree structure of the currently
selected code or file. This is particularly useful for developers who need to understand or debug the internal representation of code within the IntelliJ
platform.

### Prerequisites

- The "Developer Actions" feature must be enabled in the plugin settings
- An open file or selected code in the IntelliJ editor

### How to Use

1. **Enable Developer Actions**

- Go to Settings/Preferences
- Navigate to the AI Coder plugin settings
- Enable the "Developer Actions" option

2. **Access the Action**

- Right-click in the editor to open the context menu
- Look for the "Print Tree" action

3. **View Results**

- The PSI tree structure will be printed to the IDE's log
- You can view the output in the IDE's log window or console

### Features

- Prints detailed PSI tree structure of selected code
- Runs asynchronously to prevent UI freezing
- Provides progress indication during analysis
- Includes error handling and logging

### Common Use Cases

- Debugging code structure issues
- Understanding how IntelliJ parses your code
- Investigating PSI-related problems
- Learning about code structure representation

### Troubleshooting

If you encounter issues:

- Verify that "Developer Actions" is enabled
- Ensure you have valid code selected
- Check the IDE's log for error messages
- Make sure you have appropriate file permissions

### Notes

- The action only works when there is a valid PSI entity in the current context
- The operation runs in the background to maintain IDE responsiveness
- Output is logged at INFO level for successful operations and WARN level for issues

# generic\BaseAction.kt

Here's the documentation for the BaseAction class:

## BaseAction Class Documentation

`BaseAction` is an abstract base class that provides common functionality for IntelliJ IDEA plugin actions. It extends `AnAction` and includes utility methods
for error handling and write operations.

### Properties

- `api: ChatClient` - A lazy-initialized ChatClient instance used for API communications

### Methods

#### showError

```kotlin
protected fun showError(project: Project?, message: String)
```

Displays an error dialog to the user.

- Parameters:
  - `project`: The current project context (can be null)
  - `message`: The error message to display

#### showWarning

```kotlin
protected fun showWarning(project: Project?, message: String)
```

Displays a warning dialog to the user.

- Parameters:
  - `project`: The current project context (can be null)
  - `message`: The warning message to display

#### runWriteAction

```kotlin
protected fun runWriteAction(project: Project, action: () -> Unit)
```

Executes code that modifies the project/documents within a write action context.

- Parameters:
  - `project`: The current project context
  - `action`: Lambda containing the code to execute

### Usage

Extend this class to create new plugin actions that need:

- Error/warning dialogs
- Write access to documents
- Chat API functionality

Example:

```kotlin
class MyAction : BaseAction() {
    override fun actionPerformed(e: AnActionEvent) {
        val project = e.project
        try {
            runWriteAction(project) {
                // Perform document modifications
            }
        } catch (ex: Exception) {
            showError(project, "Failed to perform action: ${ex.message}")
        }
    }
}
```

The base class handles common boilerplate code and provides a consistent way to handle errors and document modifications across different actions.

# generic\CodeChatAction.kt

Here's the user documentation for the CodeChatAction class:

## Code Chat Action

The Code Chat Action provides an interactive chat interface for discussing and working with code selections or entire files in your IDE.

### Overview

This action opens a web-based chat interface that allows you to have an AI-assisted conversation about your code. It's useful for:

- Getting explanations about code
- Discussing potential improvements
- Asking questions about implementation details
- Getting suggestions for modifications

### Usage

1. Select code in the editor (optional - if no selection is made, the entire file will be used)
2. Trigger the Code Chat action through:

- The IDE menu
- A keyboard shortcut (if configured)
- The context menu

A browser window will automatically open with the chat interface where you can:

- Type messages to discuss the code
- See the AI's responses
- Have a continuous conversation about the code

### Features

- **Language-Aware**: Automatically detects the programming language of your code
- **Persistent Sessions**: Chat sessions are preserved and can be referenced later
- **Real-time Interaction**: Immediate responses through WebSocket communication
- **Context-Aware**: Maintains awareness of the file name and code context throughout the conversation

### Technical Details

- Uses the configured smart model from your settings
- Creates a unique session ID for each chat instance
- Stores chat history in your configured plugin home directory
- Runs the chat server locally through your IDE

### Requirements

- Active internet connection
- Properly configured API credentials in the plugin settings
- A modern web browser for the chat interface

### Notes

- The chat session will be named with the format "CodeChatAction @ HH:mm:ss"
- You can have multiple chat sessions open simultaneously
- The browser window should open automatically, but you may need to allow pop-ups

# generic\CommandAutofixAction.kt

Here's the user documentation for the CommandAutofixAction class:

## Command Autofix Action

The Command Autofix Action is a tool that helps automatically fix issues reported by command-line tools and build processes. It executes a specified command and
uses AI to analyze and fix any errors or issues that occur.

### Features

- Execute any command-line tool or build process
- Automatically detect and analyze command output
- AI-powered suggestions for fixing issues
- Optional auto-apply fixes
- Configurable working directory and arguments
- Support for different exit code handling strategies

### Usage

1. Right-click on a folder or file in your project
2. Select "Command Autofix" from the context menu
3. Configure the command settings in the dialog:

#### Settings Dialog Options

- **Executable**: Select or browse for the command/program to run
- **Arguments**: Enter command-line arguments (e.g. "run build")
- **Working Directory**: Set the directory where the command will execute
- **Exit Code Options**:
  - Patch nonzero exit code: Only fix when command fails
  - Patch 0 exit code: Fix when command succeeds
  - Patch any exit code: Always attempt fixes
- **Additional Instructions**: Provide custom instructions for the AI
- **Auto-apply fixes**: Automatically apply suggested fixes without confirmation

4. Click OK to run the command
5. A browser window will open showing:

- Command output
- Detected issues
- Suggested fixes
- Options to apply/modify fixes

### Tips

- The tool remembers recently used commands and arguments
- You can select specific files/folders to limit the scope
- Additional instructions help guide the AI's fix suggestions
- Auto-fix mode is useful for routine/known issues
- The working directory defaults to the selected folder or project root

### Requirements

- Project must be open in the IDE
- Selected folder or project base path must be accessible
- Specified command must be executable from the working directory

### Notes

- Fix suggestions are AI-generated and should be reviewed
- Complex build issues may require manual intervention
- Command history is preserved between sessions
- Browser interface provides interactive fix management

This tool is ideal for automating fixes for common build errors, linting issues, and other command-line tool outputs.

# generic\CreateFileFromDescriptionAction.kt

Here's the user documentation for the CreateFileFromDescriptionAction:

## Create File From Description Action

### Overview

The Create File From Description action allows you to generate new files in your project using natural language descriptions. This feature leverages AI to
interpret your requirements and create appropriate file content.

### Usage

1. Right-click in the project explorer or editor where you want to create a new file
2. Select "Create File From Description" from the context menu
3. In the dialog that appears, enter your description of the file you want to create
4. Click OK to generate the file

### Description Format

The description should include:

- What type of file you want to create
- The intended purpose or functionality
- Any specific requirements or features needed

Example descriptions:

```
Create a new React component for a login form with email and password fields
```

```
Create a Python utility class for handling file operations like read, write and append
```

### File Location

- The new file will be created relative to your currently selected location in the project
- If a file with the same name already exists, a numbered suffix will be added automatically
- The AI will suggest an appropriate file name and location based on standard conventions

### Notes

- The generated file content can be modified after creation if needed
- The action uses AI to interpret requirements, so being clear and specific in your description will yield better results
- File paths are automatically handled to ensure they are valid within your project structure

### Error Handling

- If file generation fails, an error dialog will be shown with details
- Common issues include:
  - Empty/invalid descriptions
  - Network/API connectivity problems
  - Permission issues when writing files

### Tips

- Include relevant technical details in your description for more accurate results
- Review the generated file content to ensure it meets your requirements
- The action works best with clear, focused descriptions of a single file's purpose

# generic\CreateImageAction.kt

Here's the user documentation for the CreateImageAction class:

## Create Image Action

### Overview

The Create Image Action is a feature that generates technical drawings or visual representations based on your code files. It uses AI to analyze your code and
create relevant images that can help visualize the code structure, architecture, or concepts.

### Features

- Generates images based on selected code files or directories
- Supports multiple file formats (PNG and JPG)
- Interactive chat interface for customizing image generation
- Automatically saves generated images to your project directory

### Supported File Types

The action works with the following file extensions:

- Kotlin (.kt)
- Java (.java)
- Python (.py)
- JavaScript (.js)
- TypeScript (.ts)
- HTML (.html)
- CSS (.css)
- XML (.xml)

### How to Use

1. **Select Content**

- Select one or more files in your project explorer
- Or select a directory to analyze multiple files at once

2. **Invoke the Action**

- Right-click on your selection
- Find "Create Image" in the context menu
- Or use the assigned keyboard shortcut if configured

3. **Interact with the Generator**

- A browser window will open with a chat interface
- Describe what kind of visualization you want
- The AI will generate images based on your code and requirements

4. **View and Save Results**

- Generated images will be displayed in the chat interface
- Images are automatically saved in both PNG and JPG formats
- Files are saved in your project directory with randomly generated names

### Technical Details

- Images are generated using AI models configured in your application settings
- Default image size is 1024x1024 pixels
- Generated files are saved with unique UUIDs as filenames
- The system creates a summary of your code files before generating images

### Troubleshooting

If you encounter issues:

- Ensure you have selected valid file types
- Check that your project directory is writable
- Verify your AI model settings in the application configuration
- Look for error messages in the IDE's event log

### Requirements

- Active project in the IDE
- Valid file selection
- Proper configuration of AI model settings
- Internet connection for AI model access

# generic\DiffChatAction.kt

Here's the user documentation for the DiffChatAction class:

## DiffChat Action

The DiffChat action provides an interactive chat interface for making code modifications using a diff-based format. It allows users to discuss and apply code
changes through a chat interface while maintaining precise control over the modifications.

### Features

- Interactive chat interface for code modifications
- Diff-based format for clear visualization of changes
- Supports both selected code and entire files
- Automatic language detection
- One-click application of suggested changes
- Context-aware modifications

### Usage

1. **Accessing the Action**

- Select code in the editor (optional - if no selection is made, the entire file will be used)
- Right-click to open the context menu
- Select "DiffChat" from the available actions

2. **Chat Interface**

- A browser window will open with the chat interface
- The selected code or file content will be available as context
- You can discuss modifications with the AI assistant

3. **Applying Changes**

- Changes will be suggested in a diff format showing:
  - Lines to be removed (prefixed with `-`)
  - Lines to be added (prefixed with `+`)
  - Context lines around the changes
- Click the "Apply" link next to a diff block to apply those specific changes

### Example Interaction

```
User: "Can you optimize this code for better performance?"

# generic\DocumentedMassPatchAction.kt

Here's the user documentation for the DocumentedMassPatchAction class:


## Documented Mass Patch Action

The Documented Mass Patch Action is a tool that helps you automatically review and update code files according to documentation standards by analyzing both documentation and source code files together.


### Features

- Select multiple documentation (.md) files and source code files for analysis
- Provide custom AI instructions for code review and updates
- Option to automatically apply suggested changes
- Web-based interface for reviewing changes
- Integration with project documentation standards


### Usage

1. Right-click in the project explorer to launch the Documented Mass Patch action

2. In the configuration dialog:
   - Select relevant documentation files (.md) that contain standards/requirements
   - Select source code files to be reviewed/updated
   - Enter AI instructions for how the code should be analyzed and modified
   - Optionally enable "Auto Apply Changes" to automatically implement suggestions
   - Click OK to proceed

3. A browser window will open showing:
   - The selected files and their contents
   - AI analysis and suggested changes
   - Options to review and apply changes

4. Review the suggested changes and:
   - Accept/reject individual changes
   - Apply all approved changes
   - Add comments or additional instructions


### Configuration Options

- **Documentation Files**: Select .md files containing documentation standards, requirements, or guidelines
- **Code Files**: Select source code files to be analyzed and potentially modified
- **AI Instruction**: Custom instructions for how the code should be reviewed/updated
- **Auto Apply**: Toggle automatic application of suggested changes


### Best Practices

1. Start with a small set of files for initial testing
2. Provide clear, specific AI instructions
3. Review all suggested changes carefully before applying
4. Keep documentation files focused and relevant to the code being modified
5. Use version control to track changes


### Technical Details

- Supports any text-based source code files
- Markdown (.md) files are used for documentation input
- Changes are made through a diff-based patch system
- Web interface runs on local server for security
- Session-based to support multiple concurrent operations


### Notes

- Large file sets may take longer to process
- Auto-apply should be used cautiously on critical code
- All changes can be reviewed before final application
- The tool respects project-specific file exclusions

This tool is ideal for ensuring code compliance with documentation standards, implementing coding standards across multiple files, and maintaining consistency between documentation and implementation.

# generic\DocumentedMassPatchServer.kt

Here's the user documentation for the DocumentedMassPatchServer class:


## DocumentedMassPatchServer

The DocumentedMassPatchServer is a server component that handles automated code review and patch generation based on documentation files and code files.


### Overview

This server facilitates:
- Reviewing code files against documentation requirements
- Generating suggested code improvements as patches
- Providing an interactive interface for reviewing and applying patches


### Key Features

- Processes multiple code files in parallel
- Compares code against documentation files
- Generates patches in standard diff format
- Optional automatic patch application
- Interactive web interface with tabbed display
- Logging of API interactions


### Configuration

The server requires:

1. `DocumentedMassPatchAction.Settings` containing:
   - Project configuration
   - Documentation file paths
   - Code file paths
   - Transformation message (optional)

2. `ChatClient` for AI interactions

3. `autoApply` flag to control automatic patch application


### Usage

1. The server processes each code file against the documentation:
   - Documentation files are read and combined
   - Each code file is analyzed in context of the documentation
   - AI generates suggested improvements as patches

2. For each file, the server:
   - Creates a new tab in the interface
   - Displays the analysis and suggested patches
   - Provides links to apply patches if approved
   - Logs the process and any errors

3. Patches are presented in standard diff format:
   - `+` indicates line additions
   - `-` indicates line deletions
   - Context lines are included before and after changes


### Interface

The web interface provides:
- Tabbed display for multiple files
- Markdown rendering of suggestions
- Interactive links for applying patches
- Progress tracking and error reporting
- API logging for debugging


### Error Handling

- Errors during file processing are logged and displayed in the UI
- Each file is processed independently to prevent total failure
- API interactions are logged for troubleshooting


### Technical Details

- Uses a semaphore-based discussion system
- Supports concurrent file processing
- Integrates with IDE file system
- Configurable AI model settings
- Extensible through the ApplicationServer framework

# generic\EnhancedChatSocketManager.kt

Here's the user documentation for the EnhancedChatSocketManager class:


## EnhancedChatSocketManager

The EnhancedChatSocketManager is an enhanced version of the ChatSocketManager that provides support for handling large output responses in chat interactions.


### Overview

This class extends the base ChatSocketManager to integrate with a LargeOutputActor, enabling better handling of lengthy responses from the language model.


### Constructor Parameters

- `session`: The Session object managing the current user session
- `model`: The ChatModel to be used for generating responses
- `userInterfacePrompt`: The prompt displayed to users in the interface
- `systemPrompt`: The system-level prompt that guides the model's behavior
- `api`: The ChatClient instance for communicating with the language model API
- `storage`: Optional StorageInterface for persisting chat data
- `applicationClass`: The ApplicationServer class type
- `largeOutputActor`: LargeOutputActor instance for handling large response generation


### Key Features

- Extends standard chat socket management functionality
- Integrates with LargeOutputActor for handling large responses
- Provides robust error handling for empty responses
- Maintains compatibility with base ChatSocketManager features


### Usage Example

```kotlin
val enhancedManager = EnhancedChatSocketManager(
    session = currentSession,
    model = selectedModel,
    userInterfacePrompt = "How can I help you?",
    systemPrompt = "You are a helpful assistant",
    api = chatClient,
    storage = dataStorage,
    applicationClass = MyAppServer::class.java,
    largeOutputActor = LargeOutputActor(...)
)
```

### Error Handling

The class will throw a RuntimeException if the language model returns an empty or null response.

### Dependencies

- Requires jopenai library for API communication
- Depends on skyenet-core and skyenet-webui components
- Needs a properly configured LargeOutputActor instance

This documentation provides an overview of the EnhancedChatSocketManager class and its key functionality for developers integrating chat capabilities with
support for large outputs in their applications.

# generic\GenerateDocumentationAction.kt

## GenerateDocumentationAction Documentation

This class provides functionality to generate documentation for files in a project using AI assistance. Here's a comprehensive overview of its features and
usage:

### Overview

`GenerateDocumentationAction` is a Kotlin class that extends `FileContextAction` and provides an interface for generating documentation from source files using
AI-powered content transformation.

### Key Features

1. **Batch Documentation Generation**

- Can process multiple files simultaneously
- Supports both single-file and multi-file output modes
- Uses parallel processing for improved performance

2. **Configurable Output**

- Option to generate a single consolidated documentation file
- Support for custom output directory structure
- Automatic file naming with conflict resolution

3. **User Interface**

- Interactive dialog for configuration
- File selection checklist
- Recent instructions history
- Customizable AI instructions
- Output path configuration

### How to Use

1. **Launch the Action**

- Select a directory in your project
- Right-click and select the documentation generation action

2. **Configure Settings**

- Choose files to process using the checkbox list
- Enter or select an AI instruction for documentation generation
- Specify output filename and directory
- Toggle single/multiple output file mode

3. **Output Options**

- Single File Mode: Generates one consolidated markdown file
- Multiple File Mode: Creates individual documentation files for each source file

### Configuration Options

- **Single Output File**: Toggle to combine all documentation into one file
- **Files to Process**: Select specific files for documentation
- **AI Instruction**: Custom instructions for the AI documentation generator
- **Output File**: Name of the output documentation file
- **Output Directory**: Target directory for generated documentation

### Error Handling

- Includes retry mechanism for failed file processing
- Timeout protection for long-running operations
- Validation for required input fields
- Automatic backup naming for existing files

### Technical Details

- Uses concurrent processing with configurable thread pool
- Supports Git repository structure awareness
- Integrates with IntelliJ's file system and editor
- Maintains history of recent documentation instructions

### Best Practices

1. **AI Instructions**

- Be specific about the documentation style needed
- Consider the target audience
- Include any special formatting requirements

2. **File Selection**

- Group related files for consistent documentation
- Consider dependencies between files
- Exclude generated or binary files

3. **Output Organization**

- Use meaningful file names
- Maintain a consistent directory structure
- Consider using the single-file mode for related components

### Limitations

- Requires valid project context
- Cannot process directories directly
- May have timeout limitations for very large files
- Requires network connectivity for AI processing

This documentation generator is particularly useful for maintaining up-to-date documentation for code bases and ensuring consistency across project
documentation.

# generic\GenerateRelatedFileAction.kt

Here's the user documentation for the GenerateRelatedFileAction class:

## Generate Related File Action

The Generate Related File action helps you automatically create related files (like test cases, implementations, or companion files) based on an existing source
file using AI assistance.

### Usage

1. Select a single file in your project
2. Right-click and select "Generate Related File" from the context menu (or use the assigned shortcut if configured)
3. In the dialog that appears:

- Enter your directive describing what kind of file you want to generate (e.g., "Create test cases", "Generate interface", etc.)
- Click OK to proceed

### Features

- Automatically generates a new file based on the selected source file and your directive
- Uses AI to analyze the source file and create appropriate related content
- Automatically determines appropriate file naming and placement
- Handles file naming conflicts by adding numerical suffixes
- Opens the generated file automatically in the editor

### Configuration

The action uses the following settings from your IDE configuration:

- AI model selection (uses the configured "smart model")
- Temperature setting for AI generation
- API credentials and settings

### Examples

Some example use cases:

- Generate unit tests for a class
- Create an interface from an implementation
- Generate a companion class
- Create documentation files
- Generate mock implementations

### Notes

- Works on single file selection only
- Generated files are placed relative to the project root
- If a file with the target name already exists, a numbered suffix will be added
- The action requires proper API configuration in the IDE settings

### Troubleshooting

If you encounter issues:

- Ensure you have selected only one file
- Verify your API credentials are configured correctly
- Check the IDE's event log for any error messages
- Make sure you have write permissions in the target directory

The action will automatically handle file system refreshing and opening the new file in the editor once generation is complete.

# generic\GenericChatAction.kt

Here's the user documentation for the GenericChatAction class:

## Generic Chat Action

The Generic Chat Action provides a simple way to initiate an AI-powered chat session within the IDE. This action opens a browser-based chat interface that
allows you to have natural language conversations with an AI assistant.

### Features

- Opens a dedicated chat window in your default web browser
- Uses the configured smart model from your application settings
- Creates a unique session for each chat instance
- Timestamps each chat session for easy reference
- Provides a persistent chat interface for ongoing conversations

### Usage

1. You can trigger the Generic Chat Action through:

- The IDE's action menu
- Keyboard shortcuts (if configured)
- The IDE's search actions (Ctrl+Shift+A / ⌘⇧A)

2. When activated:

- A new chat session will be initialized
- Your default browser will open automatically
- The chat interface will be ready for interaction

### Requirements

- An active project must be open in the IDE
- Valid API configuration in the application settings
- A working internet connection for AI model access

### Technical Details

- Uses the smart model configured in AppSettingsState
- Creates a unique session ID for each chat instance
- Runs asynchronously to prevent UI freezing
- Integrates with the IDE's project system

### Troubleshooting

If you encounter issues:

- Check your internet connection
- Verify your API configuration in the application settings
- Ensure your browser is not blocking pop-ups from the IDE
- Check the IDE's log for any error messages

### Notes

- Chat sessions are preserved until the IDE is closed
- Each chat session is labeled with a timestamp for easy identification
- The chat interface runs in your default web browser but communicates with the IDE

This action is ideal for general-purpose AI assistance and code-related discussions without requiring specific code context.

# generic\LargeOutputChatAction.kt

Here's the user documentation for the LargeOutputChatAction class:

## Enhanced Code Chat Action

The Enhanced Code Chat action provides an advanced chat interface optimized for handling large, complex coding discussions and explanations.

### Features

- Structured responses that break down complex information into clear sections
- Enhanced formatting using ellipsis notation for better readability
- Persistent chat sessions with timestamped names
- Browser-based interface for comfortable interaction
- Support for detailed code explanations and discussions

### Usage

1. Trigger the Enhanced Code Chat action from your IDE
2. A new browser window will open automatically with the chat interface
3. Enter your coding questions or requests in the chat input
4. Receive well-structured, detailed responses broken down into clear sections

### Key Benefits

- **Better Organization**: Complex explanations are automatically structured into digestible sections
- **Persistent Sessions**: Chat sessions are saved and can be referenced later
- **User-Friendly Interface**: Clean browser-based UI for comfortable interaction
- **Smart Response Generation**: Uses advanced AI model to provide detailed, relevant answers
- **Optimized for Code**: Specifically designed for programming-related discussions

### Technical Details

- Uses OpenAI's chat model for response generation
- Temperature setting of 0.3 for balanced creativity and accuracy
- Maximum of 3 iterations per response for optimal results
- Integrated with IDE's project context
- Sessions are uniquely identified and timestamped

### Notes

- Internet connection required for functionality
- Chat sessions are preserved in the plugin's home directory
- Browser access is required for the interface

The Enhanced Code Chat action is particularly useful for:

- Getting detailed code explanations
- Breaking down complex programming concepts
- Receiving structured coding advice
- Maintaining organized coding discussions

# generic\MassPatchAction.kt

Here's the documentation for the MassPatchAction and MassPatchServer classes:

## MassPatchAction Documentation

### Overview

MassPatchAction is an IntelliJ IDEA plugin action that allows batch processing and modification of multiple files using AI-powered suggestions. It provides a
user interface for selecting files and specifying transformation instructions.

### Key Features

#### File Selection

- Allows selecting multiple files or folders to process
- Automatically filters for compatible file types
- Provides a checkbox list interface for fine-grained file selection

#### Transformation Configuration

- Supports custom AI instructions for code transformation
- Maintains history of recent instructions for quick reuse
- Optional auto-apply setting for automatic patch application

#### User Interface

- Dialog-based configuration with:
  - File selection checklist
  - Instruction input area
  - Recent instructions dropdown
  - Auto-apply toggle

### Usage

1. Select files/folders in the project explorer
2. Invoke the MassPatchAction
3. In the configuration dialog:

- Select files to process
- Enter transformation instructions or select from recent ones
- Optionally enable auto-apply

4. Click OK to start processing
5. Review and apply suggested changes in the browser interface

## MassPatchServer Documentation

### Overview

MassPatchServer handles the backend processing for MassPatchAction, managing the AI interactions and file modifications through a web interface.

### Key Features

#### Session Management

- Creates unique sessions for each batch operation
- Maintains separate logs for API interactions
- Provides tabbed interface for reviewing multiple files

#### AI Integration

- Uses configured AI models for code analysis
- Supports interactive refinement of suggestions
- Generates patches in standard diff format

#### Output Handling

- Displays results in markdown format
- Provides clickable links for applying changes
- Supports both manual and automatic patch application

### Technical Details

#### Configuration Options

```kotlin
class Settings(
    val settings: UserSettings? = null,
    val project: Project? = null,
)

class UserSettings(
    var transformationMessage: String = "Review, fix, and improve",
    var filesToProcess: List<Path> = listOf(),
    var autoApply: Boolean = false,
)
```

#### Response Format

The AI generates responses in diff format:

```diff

#### path/to/file
```diff
 // Context lines
-// Removed lines
+// Added lines
 // More context
```

```


### Best Practices

1. **File Selection**
   - Review selected files before processing
   - Exclude sensitive or generated files
   - Process related files together

2. **Instructions**
   - Be specific about desired changes
   - Use clear, actionable language
   - Consider maintaining a list of proven instructions

3. **Review Process**
   - Always review changes before applying
   - Use auto-apply carefully
   - Test changes after application

4. **Performance**
   - Process files in manageable batches
   - Monitor system resources during large operations
   - Consider file size and complexity

This documentation provides a comprehensive overview of the mass patch functionality, helping users effectively utilize the feature for batch code modifications.

# generic\ModelSelectionDialog.kt

Here's the user documentation for the ModelSelectionDialog class:


## ModelSelectionDialog

A dialog component that allows users to select an AI language model from a list of available options.


### Overview

The ModelSelectionDialog presents a simple dropdown interface where users can choose from available ChatGPT/OpenAI models. This dialog is typically used when an action requires user selection of a specific AI model to process requests.


### Features

- Displays a dropdown list of available AI models
- Supports pre-selection of a default model
- Validates that a model is selected before proceeding
- Modal dialog that blocks interaction with the parent window until a selection is made


### Usage

```kotlin
// Create list of available models
val models = listOf(ChatModel(...), ChatModel(...))

// Create and show the dialog
val dialog = ModelSelectionDialog(
    project = currentProject,
    availableModels = models,
    initialSelection = defaultModel // Optional
)

if (dialog.showAndGet()) {
    // User clicked OK
    val selectedModel = dialog.selectedModel
    // Use the selected model...
} else {
    // User cancelled the dialog
}
```

### Parameters

- `project`: The current IntelliJ project context (can be null)
- `availableModels`: List of ChatModel objects representing available AI models
- `initialSelection`: (Optional) The default model to pre-select in the dropdown

### Return Value

After showing the dialog:

- Access `selectedModel` property to get the user's selection
- Returns null if no selection was made

### UI Elements

- **Model Dropdown**: Lists all available models by name
- **OK Button**: Confirms the selection (disabled if no model is selected)
- **Cancel Button**: Closes dialog without making a selection

### Validation

The dialog enforces that a model must be selected before the OK button can be clicked, preventing invalid states.

# generic\MultiCodeChatAction.kt

Here's the user documentation for the MultiCodeChatAction:

## MultiCodeChatAction Documentation

### Overview

MultiCodeChatAction is a feature that enables interactive code discussions with an AI assistant across multiple files. It allows developers to select multiple
files or folders and engage in a chat conversation about the code, with support for making code modifications through patches.

### Features

- Multi-file code discussion
- Interactive chat interface
- Code modification capabilities
- Support for viewing and applying code patches
- Token count estimation for selected files

### How to Use

#### 1. Initiating a Chat Session

1. Select one or more files/folders in your project explorer
2. Right-click to open the context menu
3. Select the Multi-Code Chat action
4. A browser window will automatically open with the chat interface

#### 2. Chat Interface

- The chat interface shows the selected code files at the start of the conversation
- You can type messages and questions about the code in the input field
- The AI assistant will respond with:
  - Code explanations
  - Suggestions for improvements
  - Potential modifications
  - Answers to your questions

#### 3. Code Modifications

- When the AI suggests code changes, they will appear as patches
- Clickable links will be provided to apply the suggested changes
- You can review the changes before applying them
- Modified files will be updated in your project

### Requirements

- Active project in the IDE
- Selected files must exist and be accessible
- Internet connection for AI communication

### Limitations

- Files starting with "." (hidden files) are excluded
- Performance may vary based on the size and number of selected files
- Token limits apply based on the selected AI model

### Tips

- Select related files for more contextual discussions
- Keep the number of files reasonable to stay within token limits
- Use specific questions to get more targeted responses
- Review suggested changes carefully before applying them

### Technical Notes

- Uses the project's root directory as the base path
- Supports multiple file formats
- Changes are tracked and can be managed through your version control system
- API logs are saved for debugging purposes

For additional support or questions, please refer to the plugin's documentation or contact support.

# generic\MultiDiffChatAction.kt

Here's the user documentation for the MultiDiffChatAction class:

## MultiDiffChatAction Documentation

### Overview

MultiDiffChatAction is an IntelliJ IDEA plugin action that enables AI-assisted code modifications across multiple files simultaneously. It provides an
interactive chat interface where users can discuss code changes and apply suggested modifications through diff patches.

### Features

- Supports multiple file selection for code review and modifications
- Provides an interactive chat interface with AI assistance
- Generates and applies code patches in diff format
- Maintains context across multiple files
- Supports both file and folder-level operations

### Usage

#### Starting the Action

1. Select one or more files/folders in your IntelliJ project
2. Right-click and select the MultiDiffChat action from the context menu

- Or use the assigned keyboard shortcut if configured

#### Chat Interface

Once initiated, the action will:

1. Open a web browser window with the chat interface
2. Display the selected files' content
3. Allow you to describe desired changes or ask questions about the code

#### Working with Patches

The AI will respond with:

- Explanatory text about proposed changes
- Code patches in diff format for each affected file
- Links to apply the suggested changes directly to your files

#### Patch Format Example

```diff

#### src/example/File.kt
 // Original code context
 function example() {
-  // Old implementation
+  // New implementation
 }
```

### Technical Details

- Supports all text-based file types (excludes binary files)
- Maintains file context with 2 lines before and after changes
- Generates proper diff format with + and - indicators
- Provides file-specific links for applying changes

### Requirements

- Active IntelliJ IDEA instance
- Selected files must be text-based (non-binary)
- Valid project configuration with write permissions

### Limitations

- Cannot process binary files
- Requires file write permissions
- Changes are applied per-file basis
- Requires network connectivity for AI interaction

### Best Practices

1. Review all suggested changes before applying
2. Keep file selections focused and relevant
3. Provide clear, specific instructions for desired changes
4. Test applied changes before committing

### Error Handling

- Invalid file selections will be disabled in the UI
- Error messages will be displayed for failed operations
- Network/API errors will be reported in the IDE

### Support

For issues or questions:

- Check the plugin documentation
- Submit issues through the plugin's issue tracker
- Contact plugin support channels

# generic\MultiStepPatchAction.kt

Here's the user documentation for the MultiStepPatchAction class:

## Multi-Step Patch Action

The Multi-Step Patch Action is an advanced code modification tool that helps automate complex code changes across multiple files in your project. It breaks down
user requests into discrete tasks and implements them systematically.

### Overview

This action provides an interactive interface that:

1. Analyzes your selected files/folders
2. Breaks down your requested changes into specific tasks
3. Generates and applies code patches for each task
4. Provides a web-based interface to review and control the process

### Usage

#### Getting Started

1. Select one or more files/folders in your project that you want to modify
2. Right-click and select "Multi-Step Patch" from the context menu (or use the assigned shortcut)
3. A browser window will open with the Auto Dev Assistant interface

#### Using the Interface

1. Enter your desired changes or requirements in natural language in the input field
2. The system will:

- Analyze your request and break it down into specific tasks
- Show you a task list with detailed descriptions
- Generate code patches for each task
- Provide options to review and apply the changes

#### Example Workflow

1. Select your project's source directory
2. Launch Multi-Step Patch
3. Enter a request like: "Add input validation to all public methods"
4. Review the proposed task breakdown
5. Review and approve/modify the generated patches for each task
6. Apply the changes to your codebase

### Features

- **Task Breakdown**: Automatically splits complex changes into manageable tasks
- **Multi-File Support**: Can handle changes across multiple files simultaneously
- **Interactive Review**: Provides a web interface to review and control changes
- **Diff Preview**: Shows exact code changes in diff format before applying
- **Context-Aware**: Considers existing code context when generating changes

### Configuration

The tool uses the following settings from your IDE's configuration:

- Smart Model: For complex analysis and task planning
- Fast Model: For parsing and simpler operations
- Plugin Home: For storing session data and logs

### Best Practices

1. **Scope Selection**: Select only the relevant files/folders for your change
2. **Clear Instructions**: Provide clear, specific instructions about desired changes
3. **Review Changes**: Always review generated patches before applying them
4. **Incremental Changes**: For large changes, consider breaking them into smaller requests

### Limitations

- Requires valid file selection before activation
- Depends on configured AI models for operation
- May require multiple attempts for complex changes
- Network connectivity required for AI operations

### Troubleshooting

If you encounter issues:

1. Check your file selection
2. Verify network connectivity
3. Review logs in the `.logs` directory
4. Ensure AI model settings are configured correctly

### Technical Details

- Uses OpenAI API for code analysis and generation
- Implements diff-based code modifications
- Supports multiple programming languages
- Maintains session history for review and rollback

For additional support or configuration options, refer to the plugin settings in your IDE.

# generic\OutlineAction.kt

Here's the user documentation for the OutlineAction class:

## OutlineAction Documentation

### Overview

The OutlineAction class provides functionality to launch an AI-powered outline creation tool within the IDE. It allows users to generate and work with outlines
using AI assistance through a web interface.

### Features

- Configurable AI model settings through a dialog
- Web-based interface for outline creation
- Integration with IDE project context
- Asynchronous processing to maintain IDE responsiveness

### Usage

1. **Accessing the Action**

- The outline tool can be accessed through the IDE's action system
- Look for "AI Outline Tool" in menus or use the assigned shortcut

2. **Configuration**

- When activated, a configuration dialog will appear
- Settings include:
  - Expansion steps with associated AI models
  - Temperature setting for AI response variation
- Click OK to proceed or Cancel to abort

3. **Working with the Tool**

- After configuration, a web browser window will open
- The interface provides a single input area for outline creation
- Work is automatically saved in the current session
- The session name includes a timestamp for reference

### Technical Details

- Runs in background thread to prevent UI freezing
- Creates a unique session ID for each use
- Integrates with the project's AppServer instance
- Uses SkyeNet's OutlineApp framework for AI processing

### Requirements

- Active project in IDE
- Valid AI model configuration
- Working internet connection for AI model access

### Troubleshooting

If you encounter issues:

- Check your internet connection
- Verify AI model settings
- Look for error messages in the IDE's log
- Ensure browser launch permissions are granted

### Notes

- Session data persists only for the duration of the IDE session
- Browser integration depends on system default browser settings
- Performance may vary based on AI model selection and system resources

# generic\OutlineConfigDialog.kt

Here's the user documentation for the OutlineConfigDialog class:

## Outline Configuration Dialog

The Outline Configuration Dialog allows users to customize settings for the outline generation tool. This dialog provides controls for configuring the AI models
used in the outline generation process and global temperature settings.

### Main Components

#### Outline Generation Steps

This section displays and manages the sequence of AI models used for generating outlines:

- A list showing all configured generation steps
- Each step displays the name of the AI model being used
- Controls to add, remove and edit steps:
  - **Add Step**: Opens a model selection dialog to add a new generation step
  - **Remove Step**: Removes the currently selected step from the sequence
  - **Edit Step**: Opens a model selection dialog to modify the selected step's model

#### Global Temperature

- A slider control that adjusts the temperature value from 0-100
- Temperature affects how creative/random the AI responses will be:
  - Lower values (closer to 0) produce more focused, deterministic results
  - Higher values (closer to 100) produce more varied, creative results

### Usage

1. **Adding Steps**:

- Click "Add Step"
- Select an AI model from the available options
- The new step will be added to the end of the sequence

2. **Removing Steps**:

- Select a step from the list
- Click "Remove Step"
- Note: At least one step must remain in the sequence

3. **Editing Steps**:

- Select a step from the list
- Click "Edit Step"
- Choose a different model from the dialog

4. **Adjusting Temperature**:

- Use the slider to set the desired temperature value
- Changes affect all generation steps

### Validation

The dialog enforces the following rules:

- At least one generation step must be configured
- Only models for which you have valid API keys will be available for selection

### Default Settings

If not previously configured, the dialog initializes with:

- Two identical steps using the default smart model
- Temperature value from global application settings

Click OK to save changes or Cancel to discard modifications.

# generic\SessionProxyApp.kt

Here's the user documentation for the SessionProxyServer class:

## SessionProxyServer Documentation

### Overview

SessionProxyServer is a specialized implementation of ApplicationServer that acts as a proxy for managing AI coding assistant sessions. It provides a web-based
interface for interacting with AI coding tools and manages user sessions and chat functionality.

### Key Features

- **Single Input Mode**: Configured to accept one input at a time
- **No Sticky Input**: Input fields are cleared after submission
- **Minimal UI**: Menu bar is hidden by default
- **Session Management**: Handles user sessions and associated chat/agent connections

### Configuration

The server is configured with these default settings:

- Application Name: "AI Coding Assistant"
- Base Path: "/"
- Menu Bar: Hidden
- Image Loading: Disabled
- Single Input Mode: Enabled
- Sticky Input: Disabled

### Usage

#### Creating a New Session

```kotlin
val server = SessionProxyServer()
val session = Session()
val user = User()
val socketManager = server.newSession(user, session)
```

#### Managing Sessions

The server maintains two types of session mappings:

- `agents`: Maps sessions to SocketManager instances
- `chats`: Maps sessions to ChatServer instances

### Storage

The server uses metadata storage configured through ApplicationServices for persisting session data.

### Error Handling

- Throws `IllegalStateException` if no agent is found for a session when attempting to create a new session

### Dependencies

- Requires SkyeNet WebUI framework
- Uses ApplicationServices for configuration and storage
- Depends on Session and User management components

### Technical Notes

- Thread-safe session management using concurrent collections
- Lazy initialization of metadata storage
- Supports both chat-based and agent-based session handling

This server is primarily used as an internal component of the AI Coding Assistant platform and is not typically interacted with directly by end users.

# generic\ShellCommandAction.kt

Here's the user documentation for the ShellCommandAction class:

## Shell Command Action

The Shell Command Action provides an interactive interface to execute shell commands in a selected directory through a web-based chat interface.

### Features

- Executes shell commands in a specified directory
- Supports both Windows (PowerShell) and Unix-based (Bash) systems
- Provides real-time command output feedback
- Handles command execution errors gracefully
- Integrates with browser-based interface

### Usage

1. **Activation**:

- Select a folder in your project explorer
- Trigger the Shell Command action from the IDE menu or toolbar

2. **Interface**:

- A browser window will automatically open with a chat interface
- Enter your shell commands in the input field
- View command output and results in the chat window

3. **Command Execution**:

- Commands are executed in the context of the selected directory
- For Windows systems, PowerShell is used as the shell
- For Unix-based systems, Bash is used as the shell

### Configuration

The action uses several settings from AppSettingsState:

- `shellCommand`: The default shell command to execute
- `temperature`: Controls the AI model's response randomness
- `smartModel`: Specifies the AI model to use for command processing

### Requirements

- A valid project must be open
- A folder must be selected in the project explorer
- Network access for the browser interface

### Error Handling

- Displays error messages if initialization fails
- Provides feedback for command execution errors
- Supports command cancellation

### Notes

- The session is uniquely identified and timestamped
- Commands are executed asynchronously to prevent UI freezing
- The interface supports both single commands and command sequences

This action is particularly useful for developers who need to execute shell commands within their project context while maintaining a clear record of commands
and their outputs.

# generic\SimpleCommandAction.kt

Here's the user documentation for the SimpleCommandAction class:

## SimpleCommandAction Documentation

### Overview

SimpleCommandAction is a powerful code assistance tool that allows users to interact with their codebase through natural language commands. It provides an
AI-powered interface for analyzing and modifying code across multiple files.

### Features

- Natural language code manipulation
- Multi-file code analysis and modification
- Intelligent file search and context gathering
- Automatic patch generation and application
- Web-based interactive interface

### Usage

#### Basic Operation

1. Select one or more files/folders in your IDE project view
2. Invoke the SimpleCommandAction (via menu or shortcut)
3. A web browser will open with an interactive chat interface
4. Enter your code-related request in natural language
5. The AI will analyze relevant files and propose changes

#### Example Commands

```
"Refactor this code to use dependency injection"
"Add error handling to all database operations"
"Convert these functions to use async/await"
"Add unit tests for this class"
"Optimize the performance of this algorithm"
```

#### How It Works

1. The action analyzes your selected files and project structure
2. It identifies relevant files based on your request
3. The AI processes your request in context of the codebase
4. Proposed changes are presented as diffs in the web interface
5. You can review and apply changes directly from the interface

### Features in Detail

#### File Selection

- Works with single files or multiple files/folders
- Automatically expands folder selections
- Intelligently limits file sizes for optimal performance
- Respects .gitignore rules

#### Context Awareness

- Analyzes related files for comprehensive understanding
- Maintains project structure awareness
- Considers file dependencies
- Supports wildcard file patterns

#### Change Management

- Presents changes in standard diff format
- Provides file-by-file modification review
- Allows selective application of changes
- Maintains change history within session

### Limitations

- Maximum file size limit of 512KB per file
- Total context size limitations based on model constraints
- May require multiple iterations for complex changes
- Browser-based interface required for interaction

### Best Practices

1. Be specific in your requests
2. Start with smaller, focused changes
3. Review generated patches carefully
4. Test applied changes thoroughly
5. Use project-specific terminology in requests

### Technical Notes

- Runs in background thread for UI responsiveness
- Supports retry mechanisms for reliability
- Includes progress indication for long operations
- Maintains session persistence for ongoing interactions

### Error Handling

- Provides clear error messages
- Includes fallback mechanisms
- Supports operation cancellation
- Maintains IDE stability during errors

### Security

- Respects project file access permissions
- Operates within IDE security context
- Maintains local code privacy
- Supports configurable API settings

This documentation provides a comprehensive overview of the SimpleCommandAction functionality. For specific use cases or additional details, please refer to the
inline code comments or contact support.

# generic\WebDevelopmentAssistantAction.kt

Here's the user documentation for the WebDevelopmentAssistantAction class:

## Web Development Assistant

The Web Development Assistant is an AI-powered tool that helps you create and manage web development projects. It provides an interactive interface to design
and generate web applications with HTML, CSS, JavaScript, and image assets.

### Features

- Automated web application architecture design
- Generation of HTML, CSS, and JavaScript files
- Image asset creation
- Code review and refinement
- Interactive development workflow

### Usage

1. **Launch the Assistant**

- Select a directory in your project where you want to create the web application
- Right-click and select "Web Development Assistant" from the context menu
- A browser window will open with the interactive interface

2. **Describe Your Project**

- Enter a description of the web application you want to create
- The assistant will analyze your request and create an architecture specification
- You'll see a tabbed view showing both the plain text description and JSON specification

3. **File Generation**

- The assistant will automatically generate the necessary files:
  - HTML files for structure
  - CSS files for styling
  - JavaScript files for functionality
  - Image assets (PNG/JPG) as needed
- Each file will be created based on the project requirements and best practices

4. **Code Review and Refinement**

- After initial file generation, the assistant performs an automated code review
- Suggestions for improvements are presented as code diffs
- You can accept or modify the suggested changes
- The code can be iteratively refined through the interactive interface

### File Types Supported

- HTML (.html)
- CSS (.css)
- JavaScript (.js)
- Images (.png, .jpg)
- Other web-related file types

### Key Components

- **Architecture Discussion**: Translates your requirements into a detailed project structure
- **Code Generation**: Creates individual files with appropriate code
- **Code Review**: Analyzes the generated code for improvements
- **Image Generation**: Creates image assets using AI
- **Interactive Refinement**: Allows iterative improvement of all generated files

### Requirements

- A project directory where files can be created
- Internet connection for AI services
- Proper IDE configuration with required permissions

### Tips

- Provide clear, detailed descriptions of your requirements
- Review generated files and suggest refinements as needed
- Use the code review feature to ensure best practices
- Take advantage of the iterative refinement process to perfect your web application

### Notes

- Generated files are saved in your selected project directory
- All code is reviewable and modifiable before final implementation
- The assistant maintains proper file organization and structure
- Changes can be undone through your normal version control system

This tool is designed to streamline web development workflow while maintaining full control over the final implementation.

# git\ChatWithCommitAction.kt

Here's the user documentation for the ChatWithCommitAction class:

## Chat With Commit Action

The Chat With Commit Action allows you to have an interactive chat discussion about changes made in a commit. This feature helps developers understand and
discuss code changes in a conversational interface.

### Features

- Analyzes differences between selected revisions and current working copy
- Handles both text and binary files
- Supports added, deleted, and modified files
- Opens an interactive chat interface to discuss the changes
- Preserves file context and change history

### Usage

1. Select one or more files/directories in your project
2. Right-click and select "Chat With Commit" from the context menu
3. The system will:

- Analyze the selected files for changes
- Generate a diff of the changes
- Open a chat interface in your browser
- Allow you to discuss the changes with an AI assistant

### Supported Content

- Text files: Shows detailed line-by-line changes
- Binary files: Indicates binary file changes without content diff
- Added files: Shows full new file content
- Deleted files: Shows removed file content
- Modified files: Shows diff of changes

### Technical Details

- Uses diff patch generation to show precise changes
- Integrates with your project's version control system
- Creates a unique session ID for each chat
- Supports multiple chat sessions simultaneously
- Uses configured AI model settings from your IDE preferences

### Requirements

- Project must be under version control
- Non-Git version control system (Git repositories are not supported)
- Active internet connection for AI chat functionality
- Browser access for chat interface

### Notes

- Chat sessions are preserved with timestamps for future reference
- Changes are displayed in a readable diff format
- Large diffs may take a moment to process
- Binary files are noted but their contents are not compared

This action is particularly useful for code reviews, understanding historical changes, and discussing code modifications with team members or AI assistance.

# git\ChatWithCommitDiffAction.kt

Here's the user documentation for the ChatWithCommitDiffAction:

## Chat with Commit Diff Action

### Overview

The "Chat with Commit Diff" action allows developers to interactively discuss and analyze Git commit differences through a chat interface. This feature helps in
understanding code changes between commits by presenting them in a readable diff format and enabling AI-assisted discussion about those changes.

### Features

- Compares selected commit with the current HEAD
- Displays file changes in a diff format
- Opens an interactive chat interface to discuss the changes
- Supports multiple file changes in a single commit
- Shows both additions and deletions in the code

### How to Use

1. **Access the Action**

- Navigate to a commit in your Git history
- Right-click or use the action menu to select "Chat with Commit Diff"

2. **View Changes**

- The system will automatically generate a diff between the selected commit and the current state
- Changes are displayed in a standard diff format:
  - Lines starting with `+` indicate additions
  - Lines starting with `-` indicate deletions

3. **Chat Interface**

- A browser window will open with a chat interface
- The diff information will be pre-loaded into the chat context
- You can ask questions or discuss specific aspects of the changes
- The AI will respond based on the context of the changes

### Requirements

- Active VCS (Version Control System) in the project
- Valid Git repository
- Selected commit for comparison

### Error Handling

- If no VCS is found, an error message will be displayed
- If there are no changes between commits, "No changes found" will be displayed
- Connection issues or other errors will show appropriate error messages

### Technical Notes

- Uses the project's configured VCS system
- Integrates with IntelliJ's VCS framework
- Supports standard diff formatting
- Chat sessions are uniquely identified and preserved

### Limitations

- Only works with Git repositories
- Requires active internet connection for chat functionality
- Limited to comparing with current HEAD state

This action is particularly useful for:

- Code review discussions
- Understanding historical changes
- Documenting the reasoning behind code changes
- Collaborative code analysis

# git\ChatWithWorkingCopyDiffAction.kt

Here's the user documentation for the ChatWithWorkingCopyDiffAction:

## Chat with Working Copy Changes

The Chat with Working Copy Changes action allows you to have an interactive chat discussion about the uncommitted changes in your Git working directory.

### Overview

This feature helps developers:

- Review and discuss pending changes before committing
- Get AI assistance in understanding code modifications
- Analyze the impact of working copy changes

### How to Use

1. Make some changes to files in your Git repository but don't commit them yet
2. Access the action through:

- Right-click menu in project view
- VCS menu
- Search for "Chat with Working Copy Changes" using Find Action (Ctrl+Shift+A / ⌘⇧A)

3. The action will:

- Collect all uncommitted changes in your working directory
- Generate a diff view comparing current state with HEAD
- Open a chat interface in your browser

4. In the chat interface, you can:

- Discuss the changes with the AI assistant
- Ask questions about specific modifications
- Get suggestions or feedback on the changes

### Requirements

- Project must be under Git version control
- At least one uncommitted change must exist in the working directory
- Valid API configuration in plugin settings

### Features

- Shows file-by-file diff information
- Displays both added and removed lines
- Maintains chat history during the session
- Supports markdown formatting in chat
- Browser-based interface for better readability

### Notes

- The diff view is read-only - changes must be made in your IDE
- Chat sessions are temporary and not persisted between IDE restarts
- Large diffs may take longer to process
- Internet connection required for AI chat functionality

### Troubleshooting

If the action is disabled (grayed out), check that:

- The project has Git VCS enabled
- You have uncommitted changes in your working directory
- The IDE has proper Git integration configured

For other issues, check the IDE's log files for error messages.

# git\ReplicateCommitAction.kt

Here's the user documentation for the ReplicateCommitAction class:

## Replicate Commit Action

The Replicate Commit Action is a powerful feature that allows you to replicate and modify Git commits based on user requirements. This action helps developers
adapt existing code changes to new contexts or requirements.

### Overview

This action analyzes selected Git changes and provides an AI-assisted interface to replicate and modify those changes according to specified requirements. It's
particularly useful when you want to:

- Apply similar changes across different parts of the codebase
- Modify existing commits with new requirements
- Understand and replicate complex code changes

### How to Use

1. **Select Changes**:

- Select one or more files with Git changes in your project
- The changes can be from the current working directory or committed changes

2. **Launch the Action**:

- Access via the IDE's action menu or keyboard shortcuts
- The action will only be enabled when valid Git changes are selected

3. **Interact with the Interface**:

- A browser window will open showing the selected changes
- Enter your requirements or modifications in the input field
- The AI will analyze the changes and propose modifications

4. **Review and Apply Changes**:

- Review the proposed changes in diff format
- Click on the provided links to apply specific changes
- Changes will be applied to your working directory

### Features

- **Smart Change Analysis**: Automatically analyzes Git changes and related code context
- **File Filtering**:
  - Automatically filters binary files
  - Handles file size limits (max 0.5MB per file)
  - Respects .gitignore rules

- **Interactive UI**:
  - Shows clear diff previews
  - Provides clickable links to apply changes
  - Displays both original and modified code

- **Project Context Awareness**:
  - Considers project structure
  - Analyzes related files for better context
  - Maintains code consistency

### Limitations

- File size limit of 0.5MB per file
- Binary files are not processed
- Requires valid Git changes to be selected
- Internet connection required for AI functionality

### Best Practices

1. **Select Relevant Changes**: Choose only the changes that are directly related to your replication needs

2. **Provide Clear Requirements**: Be specific about how you want the changes to be modified

3. **Review Changes Carefully**: Always review the proposed changes before applying them

4. **Test After Application**: Test the code after applying changes to ensure functionality

### Error Handling

The action includes error handling for common scenarios:

- Invalid working directory
- Missing Git changes
- File access issues
- Connection problems

Error messages will be displayed in the IDE's notification system.

### Technical Requirements

- Active IntelliJ-based IDE
- Git integration enabled
- Internet connection for AI functionality
- Sufficient file system permissions

### Support

For issues or questions:

- Check the error messages in the IDE's event log
- Ensure your Git integration is working correctly
- Verify file permissions and access rights
- Contact support if problems persist

This documentation provides a comprehensive overview of the ReplicateCommitAction functionality while remaining accessible to users with varying levels of
technical expertise.

# knowledge\CreateProjectorFromQueryIndexAction.kt

Here's the user documentation for the CreateProjectorFromQueryIndexAction:

## Create Projector from Query Index Action

This action creates an interactive visualization of document embeddings using TensorFlow Projector from query index data files.

### Overview

The Create Projector from Query Index action allows you to visualize document embeddings stored in .index.data files using TensorFlow Projector's interactive 3D
visualization interface. This is useful for exploring relationships between documents and understanding the semantic space of your document collection.

### Usage

1. In your IDE, select one or more .index.data files or folders containing .index.data files
2. Right-click and select "Create Projector from Query Index" from the context menu
3. Wait while the action:

- Reads the document records from the selected files
- Sets up the TensorFlow Projector visualization
- Opens your default web browser to display the interactive visualization

### Requirements

- The files must have a .index.data extension
- You must have developer actions enabled in the plugin settings
- The selected files must contain valid document embedding data

### Features

- Processes multiple index files at once
- Works with both individual files and folders
- Creates an interactive 3D visualization of document embeddings
- Allows exploration of semantic relationships between documents
- Opens automatically in your default web browser

### Error Handling

The action will show error dialogs if:

- No valid .index.data files are selected
- There are problems reading the data files
- Issues occur during projector creation

### Notes

- The visualization may take some time to load depending on the size of your dataset
- The projector interface opens in your default web browser
- The session is preserved and can be accessed later through the session history

### Technical Details

- Uses TensorFlow Projector for visualization
- Creates a unique session ID for each projection
- Runs the visualization on a local web server
- Preserves session metadata for future reference

For more information about TensorFlow Projector and how to interpret the visualizations, please refer to the TensorFlow documentation.

# knowledge\DocumentDataExtractorAction.kt

Here's the user documentation for the DocumentDataExtractorAction:

## Document Data Extractor

The Document Data Extractor is a powerful tool that uses AI to extract structured data from various document types. It helps you analyze and parse information
from files like PDFs, text documents, HTML files, and markdown files.

### Features

- Supports multiple file formats including:
  - PDF (.pdf)
  - Text files (.txt)
  - HTML files (.html, .htm)
  - Markdown files (.md)
  - Other text-based formats

- Batch processing capabilities for multiple files and directories
- Configurable parsing settings
- AI-powered data extraction
- Interactive web interface for viewing results

### Usage

1. **Select Files**

- Select one or more files in your project
- You can also select entire directories to process multiple files
- Invalid file types will be automatically filtered out

2. **Configure Settings**

- When you run the action, a configuration dialog will appear
- Adjust the parsing settings according to your needs:
  - Fast Mode: Toggle for quicker but potentially less detailed processing
  - Model Type: Select the appropriate parsing model for your documents

3. **View Results**

- After configuration, a web browser will open automatically
- The extracted data will be displayed in a structured format
- Results are saved in the same directory as the source files

### Requirements

- Developer actions must be enabled in the plugin settings
- Valid API credentials configured
- Internet connection for AI processing

### Notes

- Files ending with `.parsed.json` or `.data` are excluded from processing
- The tool creates a new session for each extraction process
- Results are stored locally and can be accessed later
- Processing time may vary depending on file size and complexity

### Troubleshooting

If you encounter any issues:

- Ensure you have selected valid file types
- Check your internet connection
- Verify API credentials are properly configured
- Look for error messages in the IDE's event log

For technical support or to report issues, please refer to the plugin's documentation or contact support.

# knowledge\DocumentDataExtractorConfigDialog.kt

Here's the user documentation for the DocumentDataExtractorConfigDialog class:

## DocumentDataExtractorConfigDialog

A configuration dialog for the Document Data Extractor feature that allows users to customize various settings for parsing and processing documents.

### Overview

This dialog provides a user interface to configure multiple settings related to document parsing, including:

- Parsing model selection
- Image and text processing options
- Output format and file saving preferences
- Performance and display settings

### Configuration Options

#### Basic Settings

- **Parsing Model**: Select the model type to be used for parsing the document
- **DPI**: Set the dots per inch resolution for image processing (must be positive, default: 300)
- **Max Pages**: Maximum number of pages to process (must be positive, default: 100)
- **Output Format**: Specify the desired output format for the processed document
- **Pages Per Batch**: Number of pages to process in each batch (must be positive, default: 10)

#### File Output Options

- **Save Image Files**: Enable/disable saving of extracted images
- **Save Text Files**: Enable/disable saving of extracted text
- **Save Final JSON**: Enable/disable saving the final output in JSON format

#### Display Options

- **Show Images**: Toggle the display of images during processing
- **Add Line Numbers**: Enable/disable line numbering in the output

#### Performance Options

- **Fast Mode**: Enable/disable fast processing mode (may affect quality)

### Validation

The dialog performs validation on numeric inputs:

- DPI must be a positive number
- Max Pages must be a positive integer
- Pages Per Batch must be a positive integer

Invalid inputs will display error messages and prevent the dialog from being confirmed.

### Usage

1. Open the Document Data Extractor configuration dialog
2. Adjust the settings according to your needs
3. Click OK to apply the settings or Cancel to discard changes
4. Invalid settings will be highlighted and must be corrected before proceeding

Note: All numeric fields must contain valid positive numbers. The dialog will not allow confirmation if any validation errors exist.

# knowledge\GoogleSearchAndDownloadAction.kt

Here's the user documentation for the GoogleSearchAndDownloadAction:

## Google Search and Download Action

### Overview

The Google Search and Download Action allows you to perform Google searches and automatically download the search results as HTML files to your project
directory. This feature is particularly useful for research, reference gathering, and content collection tasks.

### Prerequisites

To use this action, you need to have:

1. A valid Google API Key
2. A Google Custom Search Engine ID
3. Developer Actions enabled in the plugin settings

### Configuration

Before using the action, ensure you have configured the following in the plugin settings:

- Google API Key
- Google Custom Search Engine ID

### Usage

1. Select a target directory in your project where you want to save the downloaded files
2. Trigger the "Google Search and Download" action
3. In the dialog that appears, enter your search query
4. Click OK to start the search and download process

### Process

The action will:

1. Perform a Google search using your query
2. Download the top 10 search results as HTML files
3. Save the files in your selected directory with names formatted as: `[index]_[sanitized-title].html`

### Output

- Downloaded files are saved as HTML documents
- Each file is named using the pattern: `[number]_[page-title].html`
- File names are sanitized to remove invalid characters
- Maximum of 10 results are downloaded

### Error Handling

- Failed downloads are logged but won't stop the entire process
- Progress can be monitored in the IDE's progress indicator
- The operation can be cancelled at any time

### Limitations

- Maximum of 10 search results per query
- Only HTML content is downloaded
- Requires active internet connection
- Subject to Google API usage limits

### Troubleshooting

If the action is not available (grayed out), check that:

1. You have configured your Google API Key
2. You have configured your Google Search Engine ID
3. Developer Actions are enabled in the plugin settings
4. You have selected a valid target directory

### Notes

- Downloaded content is subject to Google's terms of service
- Be mindful of API usage limits and quotas
- Some websites may block automated downloads

# knowledge\SaveAsQueryIndexAction.kt

Here's the user documentation for the SaveAsQueryIndexAction:

## Save As Query Index Action

### Overview

The Save As Query Index Action is a utility that converts parsed JSON files into an indexed vector format for efficient querying. This action is particularly
useful for creating searchable knowledge bases from parsed documents.

### Features

- Batch processing of multiple JSON files
- Support for processing entire directories
- Multi-threaded processing for improved performance
- Progress tracking with cancellation support

### Requirements

- Input files must be in `.parsed.json` format
- Developer actions must be enabled in the application settings
- Valid OpenAI API configuration

### Usage

1. **File Selection**

- Select one or more `.parsed.json` files directly
- Or select directories containing `.parsed.json` files
- Right-click and select "Save As Query Index" from the context menu

2. **Processing**

- A progress bar will appear showing the indexing status
- The process can be cancelled at any time using the cancel button
- The system will automatically utilize multiple threads for faster processing

3. **Completion**

- A success message will appear when indexing is complete
- The resulting index can be used for vector-based queries

### Configuration

The action uses default settings:

- Thread Count: 8 threads
- Batch Size: 100 items

### Error Handling

- Displays error messages for invalid file selections
- Shows progress updates during processing
- Provides feedback for successful completion or cancellation
- Logs errors with detailed messages for troubleshooting

### Notes

- This action is only available when developer actions are enabled
- Processing large numbers of files may take significant time
- Ensure sufficient system resources are available for multi-threaded processing

# legacy\AppendTextWithChatAction.kt

Here's the user documentation for the AppendTextWithChatAction class:

## Append Text with AI

The Append Text action uses AI to intelligently continue and expand your selected text. This feature helps you generate natural continuations of existing
content by leveraging ChatGPT's language capabilities.

### Usage

1. Select some text in the editor that you want to continue/expand
2. Right-click and select "Append Text with AI" from the context menu (or use the assigned shortcut if configured)
3. The AI will analyze your selection and generate a natural continuation that flows from the original text

### Features

- Seamlessly continues your existing text while maintaining context and style
- Automatically removes any duplicate text if the AI response includes the original selection
- Uses temperature settings from your global configuration to control creativity/randomness
- Leverages the configured "smart model" (typically GPT-4 or similar) for high-quality results

### Configuration

This action uses the following settings from your global AI Coder configuration:

- Smart Model: The AI model used for text generation
- Temperature: Controls randomness/creativity of the generated continuation
- Legacy Actions: Must be enabled for this action to be available

### Notes

- The action is part of the legacy feature set and requires "Enable Legacy Actions" to be turned on in settings
- Quality of continuation depends on:
  - Length and clarity of selected text
  - Context provided in the selection
  - Model and temperature settings
- If an error occurs during generation, your original selection will be preserved unchanged

### Example

Original selection:

```
The quick brown fox
```

After append action:

```
The quick brown fox jumped over the lazy dog, its russet fur gleaming in the afternoon sun.
```

# legacy\CommentsAction.kt

Here's the user documentation for the CommentsAction class:

## CommentsAction

### Overview

CommentsAction is a legacy action that adds explanatory comments to each line of selected code. It works with various programming languages and helps improve
code readability by automatically generating inline comments.

### Features

- Adds explanatory comments to each line of selected code
- Supports multiple programming languages
- Uses AI to generate contextually relevant comments
- Preserves original code structure while adding comments

### Usage

1. Select the code you want to comment in the editor
2. Trigger the action via:

- Menu: Edit > Add Line Comments
- Keyboard shortcut (if configured)
- Right-click context menu

### Requirements

- Must be enabled in settings (Legacy Actions must be turned on)
- Requires valid text selection
- File must have a recognized programming language extension

### Settings

The following settings affect this action:

- Legacy Actions must be enabled in the plugin settings
- Temperature setting affects comment variation/creativity
- Model selection affects comment quality
- Human language setting determines comment language

### Example

```java
// Before:
int x = 5;
x += 10;
System.out.println(x);

// After:
int x = 5;           // Initialize variable x with value 5
x += 10;             // Add 10 to the value of x
System.out.println(x); // Print the final value of x to console
```

### Notes

- Part of legacy actions suite
- Comments are generated using AI, so results may vary
- Maintains original code formatting
- Works best with clear, well-structured code
- May require manual review of generated comments

### Troubleshooting

If the action is not working:

1. Verify Legacy Actions are enabled in settings
2. Ensure you have valid code selected
3. Check if the file type/language is supported
4. Verify API connectivity and settings

# legacy\DocAction.kt

Here's the user documentation for the DocAction class:

## DocAction - Documentation Generator

The DocAction class is a code documentation generator that automatically creates documentation comments for selected code blocks in your IDE.

### Features

- Generates documentation in the appropriate style for different programming languages (e.g., KDoc for Kotlin, JavaDoc for Java)
- Supports multiple human languages for documentation output
- Intelligently detects code blocks and adjusts selection to cover complete elements
- Uses AI to analyze code and generate meaningful documentation

### Usage

1. Select the code block you want to document in your IDE
2. Invoke the "Generate Documentation" action through:

- The IDE's action menu
- A keyboard shortcut (if configured)
- The context menu

The action will automatically:

- Analyze the selected code
- Generate appropriate documentation comments
- Insert the documentation above the selected code block
- Maintain proper indentation

### Requirements

- The action only works with programming languages that have defined documentation styles
- The language must be properly recognized by the IDE
- The "Legacy Actions" feature must be enabled in the plugin settings

### Configuration

The documentation generation can be customized through the plugin settings:

- Human language for documentation output
- AI model temperature (controls creativity vs consistency)
- Smart model selection for AI processing

### Limitations

- Does not work with plain text files
- Requires a valid documentation style to be defined for the programming language
- May require multiple attempts for complex code blocks

### Example

Input:

```kotlin
fun hello() {
    println("Hello, world!")
}
```

Output:

```kotlin
/**
 * Prints "Hello, world!" to the console
 */
fun hello() {
    println("Hello, world!")
}
```

# legacy\ImplementStubAction.kt

Here's the user documentation for the ImplementStubAction class:

## Implement Stub Action

The Implement Stub action helps developers automatically implement stub methods and classes using AI code generation. This feature saves time by generating
meaningful implementations for method/class declarations.

### Usage

1. Select a method or class stub/declaration in your code
2. Right-click and select "Implement Stub" from the context menu (or use the assigned shortcut)
3. The AI will analyze the declaration and generate an appropriate implementation

### Features

- Automatically detects the programming language of the selected code
- Preserves the original declaration while implementing the body
- Generates implementations that match the context and purpose of the stub
- Works with multiple programming languages (except plain text)
- Respects the project's coding style and patterns

### Requirements

- The action must be enabled in settings (Legacy Actions must be enabled)
- The selected code must be in a supported programming language
- A valid method or class declaration must be selected

### Examples

```java
// Before
public void processData(String input) {
    // TODO: Implement
}

// After selecting and running "Implement Stub"
public void processData(String input) {
    if (input == null || input.isEmpty()) {
        throw new IllegalArgumentException("Input cannot be null or empty");
    }
    
    // Process the input string
    String processed = input.trim().toLowerCase();
    
    // Perform necessary operations
    // ... AI generated implementation ...
}
```

### Configuration

- The implementation style can be influenced by the selected AI model in settings
- The output language can be configured through the human language setting
- Temperature setting affects how creative/conservative the implementations are

### Limitations

- Only works with code elements (not plain text)
- Quality of implementation depends on the clarity of the stub/declaration
- May require manual review and adjustments of generated code

### Troubleshooting

If you encounter issues:

- Ensure Legacy Actions are enabled in settings
- Check that you've selected a valid code declaration
- Verify the programming language is supported
- Review error messages for specific issues

For best results, provide clear and complete declarations with appropriate context.

# legacy\InsertImplementationAction.kt

Here's the user documentation for the InsertImplementationAction class:

## Insert Implementation Action

### Overview

The Insert Implementation Action is a code generation feature that automatically implements code based on comments or selected text. It uses AI to generate
appropriate code implementations while considering the context of your codebase.

### Features

- Generates code implementations from comments or selected text specifications
- Maintains context awareness of the surrounding code
- Supports multiple programming languages
- Preserves code indentation
- Integrates with project-specific settings

### Usage

1. **Select Text or Position Cursor**

- Place your cursor on a comment describing desired functionality, or
- Select text that describes the implementation you want
- The action will automatically find the nearest relevant comment if no specific selection is made

2. **Invoke the Action**

- Use the action shortcut or menu item to trigger the implementation generation
- The action will analyze the context and generate appropriate code

3. **Review Generated Code**

- The generated code will be inserted below your selection/comment
- The code maintains proper indentation matching the context

### Requirements

- A configured AI model in the plugin settings
- Supported programming language (excludes plain text and Markdown files)
- Legacy actions must be enabled in settings

### Example

```java
// Create a function that calculates the factorial of a number
```

After invoking the action, it might generate:

```java
// Create a function that calculates the factorial of a number
public int factorial(int n) {
    if (n <= 1) return 1;
    return n * factorial(n - 1);
}
```

### Troubleshooting

Common issues and solutions:

1. **No Code Generated**

- Ensure AI model is properly configured in settings
- Check if the comment/selection provides clear implementation instructions
- Verify the programming language is supported

2. **Configuration Errors**

- Make sure the AI model is selected in plugin settings
- Enable legacy actions if disabled

3. **Unexpected Results**

- Try providing more detailed specifications in the comment
- Check if the surrounding code context is properly detected

### Notes

- The action works best with clear, specific implementation descriptions
- Generated code considers the context of your existing codebase
- The feature is part of legacy actions and must be explicitly enabled
- Performance may vary based on the complexity of the implementation request

# legacy\RenameVariablesAction.kt

Here's the user documentation for the RenameVariablesAction:

## RenameVariablesAction Documentation

### Overview

The RenameVariablesAction is a code refactoring tool that helps improve code readability by suggesting better variable names using AI. It analyzes selected code
and provides intelligent suggestions for renaming variables based on their context and usage.

### Features

- AI-powered variable name suggestions
- Support for multiple programming languages
- Interactive selection of which variables to rename
- Batch renaming capability
- Preserves code structure while only modifying variable names

### How to Use

1. **Select Code**

- Highlight the code segment containing variables you want to rename
- The selection can include multiple variables and any surrounding code for context

2. **Trigger the Action**

- Access the action through the IDE menu or keyboard shortcut
- The tool will analyze your selected code

3. **Review Suggestions**

- A dialog will appear showing suggested renames
- Each suggestion will be displayed as: `originalName -> suggestedName`
- Check the boxes next to the suggestions you want to apply

4. **Apply Changes**

- Click OK to apply the selected rename suggestions
- The tool will automatically replace all instances of the selected variables within your selection

### Requirements

- The legacy actions feature must be enabled in the plugin settings
- An active internet connection for AI model access
- Sufficient context in the selected code for meaningful suggestions

### Notes

- If no suggestions are found, you'll receive a notification
- The tool respects your configured human language preference for generating suggestions
- All changes are applied within the selected text only
- The operation can be cancelled at any time

### Error Handling

- If any errors occur during the rename operation, an error dialog will display the details
- Failed operations will not modify your code
- You can retry the operation after addressing any reported issues

### Best Practices

- Select enough surrounding code to provide context for better suggestions
- Review suggestions carefully before applying them
- Consider the scope and impact of variable renames in your codebase
- Use this tool as part of a broader code cleanup strategy

# legacy\ReplaceWithSuggestionsAction.kt

Here's the user documentation for the ReplaceWithSuggestionsAction class:

## Replace With Suggestions Action

The Replace With Suggestions action helps you replace selected text with AI-generated alternatives that fit the surrounding context.

### Features

- Analyzes the text before and after your selection to understand the context
- Generates multiple alternative suggestions that could replace the selected text
- Presents suggestions in a radio button dialog for easy selection
- Maintains contextual relevance by considering surrounding text

### How to Use

1. Select the text you want to replace in the editor
2. Trigger the "Replace With Suggestions" action
3. Wait while the AI analyzes the context and generates suggestions
4. Choose from the presented alternatives in the radio button dialog
5. The selected suggestion will replace your original text

### Progress Indicators

The action shows progress through several stages:

- "Analyzing context..." - Initial analysis of selected text
- "Preparing context..." - Processing surrounding text for context
- "Generating suggestions..." - AI generation of alternatives

### Configuration

- Uses the configured Smart Model from settings
- Temperature setting affects variation in suggestions
- Can be enabled/disabled via "Enable Legacy Actions" setting

### Notes

- The action is part of the legacy feature set
- Context window size scales logarithmically with selection length
- Handles errors gracefully with user notifications
- Preserves original text if operation is cancelled

### Requirements

- Requires an active project
- Needs text to be selected
- Legacy actions must be enabled in settings

The action is designed to help you quickly explore alternative phrasings while maintaining contextual relevance to your document.

# legacy\VoiceToTextAction.kt

Here's the user documentation for the VoiceToTextAction class:

## Voice-to-Text Action

The Voice-to-Text action allows you to dictate text directly into the editor using your microphone. It provides real-time speech-to-text conversion and inserts
the transcribed text at the cursor position or selected text location.

### Features

- Real-time audio recording and speech recognition
- Continuous dictation with context awareness
- Visual status indicator
- Works with text selection or cursor position
- Automatic punctuation and formatting

### Requirements

- A working microphone connected to your system
- Proper audio system configuration
- Legacy actions enabled in the plugin settings

### Usage

1. Position your cursor where you want the dictated text to appear, or select existing text to append to
2. Activate the Voice-to-Text action from the editor context menu or using the assigned shortcut
3. A status window will appear indicating that dictation is active
4. Begin speaking clearly into your microphone
5. The transcribed text will appear in real-time at the cursor position
6. To stop dictation, close the status window

### Tips

- Speak clearly and at a moderate pace for best results
- The system maintains context of previous speech (up to 32 words) to improve accuracy
- You can dictate punctuation marks by speaking them (e.g., "period", "comma", etc.)
- The status window can be moved around but should remain visible while dictating

### Troubleshooting

If the action is disabled (grayed out), check:

- That your microphone is properly connected and configured
- Legacy actions are enabled in the plugin settings
- Your system's audio input settings
- The plugin has necessary permissions to access the microphone

### Technical Notes

- Uses system audio input at 16kHz sample rate
- Processes audio in chunks with noise filtering
- Maintains a rolling context window for improved transcription accuracy
- Supports both insertion at cursor and appending to selection

Note: This feature is part of the legacy actions set and may require explicit enablement in the plugin settings.

# markdown\MarkdownImplementActionGroup.kt

Here's the user documentation for the MarkdownImplementActionGroup class:

## Markdown Code Block Implementation Action

The Markdown Code Block Implementation Action is a feature that allows you to convert natural language descriptions into code blocks in various programming
languages within Markdown files.

### Overview

This action appears in the editor when:

- You are editing a Markdown file
- You have text selected in the editor

### Supported Languages

The action supports conversion to the following programming languages:

- SQL
- Java
- ASP
- C
- Clojure
- CoffeeScript
- C++
- C#
- CSS
- Bash
- Go
- JavaScript
- LESS
- Make
- MATLAB
- Objective-C
- Pascal
- PHP
- Perl
- Python
- Rust
- SCSS
- SVG
- Swift
- Ruby
- Smalltalk
- VHDL

### How to Use

1. Open a Markdown file in the editor
2. Select the text you want to convert to code
3. Right-click to open the context menu
4. Select the target programming language from the available options
5. The selected text will be converted into a code block in the chosen programming language

### Example

If you have selected text like:

```
Create a function that adds two numbers
```

And you choose "Python", it will be converted to:

````markdown
```python
def add_numbers(a, b):
    return a + b
```
````

### Notes

- The conversion uses AI to interpret your description and generate appropriate code
- The generated code will be wrapped in Markdown code block syntax with the appropriate language identifier
- If an error occurs during conversion, an error dialog will be shown and the original text will be preserved
- The conversion process runs in the background to avoid blocking the UI

### Configuration

The action uses the following settings from your IDE configuration:

- AI model settings from AppSettingsState
- Temperature setting for code generation
- API configuration for the AI service

### Error Handling

If the conversion fails:

- An error dialog will be displayed with details
- The original selected text will be preserved
- The error will be logged for troubleshooting

# markdown\MarkdownListAction.kt

Here's the user documentation for the MarkdownListAction:

## Markdown List Generator

The Markdown List Generator is a tool that helps you extend existing markdown lists by automatically generating additional list items using AI.

### Features

- Works with bullet lists (`-` or `*`) and checkbox lists (`- [ ]`)
- Maintains consistent list formatting and indentation
- Intelligently generates new items based on existing list context
- Supports configurable number of new items to generate

### Usage

1. Place your cursor anywhere within an existing markdown list
2. Trigger the action via:

- Menu: Tools > AI Coder > Markdown > Generate List Items
- Or use the assigned keyboard shortcut

3. Enter the number of new items you want to generate when prompted
4. The AI will analyze your existing list items and generate new ones in a similar style

### Examples

Starting with:

```markdown
- Item 1
- Item 2
- Item 3
```

After generating 3 more items:

```markdown
- Item 1
- Item 2
- Item 3
- Item 4
- Item 5
- Item 6
```

Works with checkbox lists too:

```markdown
- [ ] Task 1
- [ ] Task 2
```

### Notes

- The generator maintains the same list style (bullets, numbers, or checkboxes) as the original list
- Indentation level is preserved for nested lists
- Generated items aim to be contextually relevant to your existing list items
- If an error occurs during generation, an error dialog will be displayed

### Configuration

The generation process uses the following settings from your AI Coder configuration:

- Temperature: Controls the creativity/randomness of generated items
- Model: Uses your configured smart model for generation

### Requirements

- Must be used within a markdown file
- Requires an existing list with at least one item
- Requires valid AI Coder configuration and API access

# plan\AutoPlanChatAction.kt

Here's the user documentation for the AutoPlanChatAction class:

## AutoPlanChatAction Documentation

### Overview

AutoPlanChatAction is an IntelliJ IDEA plugin action that provides an AI-powered planning and chat interface for software development tasks. It allows users to
interact with an AI assistant that can help plan and execute development tasks while having full context of the project files.

### Features

- Interactive chat interface with AI assistant
- Project context awareness
- Code file analysis and summarization
- Configurable AI models and settings
- Shell command execution support (PowerShell/Bash)
- Integration with GitHub and Google APIs

### Usage

#### Starting the Chat

1. Select a file or folder in your project
2. Invoke the Auto Plan Chat action from the IDE
3. Configure settings in the dialog that appears:

- AI models for chat and parsing
- Temperature setting (0.0-1.0)
- Working directory
- GitHub token (optional)
- Google API credentials (optional)

#### Configuration Options

- **Default Model**: Main AI model for chat interactions
- **Parsing Model**: Secondary AI model for code parsing
- **Temperature**: Controls AI response randomness (0.0 = focused, 1.0 = creative)
- **Working Directory**: Project root directory
- **GitHub Token**: For GitHub API integration
- **Google API Key**: For Google search capabilities
- **Google Search Engine ID**: For custom search integration

#### Limitations

- Maximum file size limit: 512KB per file
- Performance may vary based on project size
- Requires valid API credentials for full functionality

#### Tips

- Select specific files/folders before starting chat for focused context
- For large projects, the system will provide a summary instead of full file contents
- The chat interface opens in your default web browser
- Session information is preserved for future reference

### Technical Requirements

- IntelliJ IDEA
- Internet connection for AI model access
- Appropriate API credentials configured
- Compatible operating system (Windows/Linux/Mac)

### Error Handling

- Invalid project root errors will be displayed
- API connection issues will show appropriate error messages
- File processing errors are logged and reported
- Browser launch failures are handled gracefully

This action is part of the AICoder plugin suite and integrates with other development tools and actions within the IDE.

# plan\PlanAheadAction.kt

Here's the user documentation for the PlanAheadAction class:

## PlanAheadAction Documentation

### Overview

PlanAheadAction is an IntelliJ IDEA action that provides an AI-powered planning assistant for development tasks. It helps developers break down and plan
implementation work by integrating with language models and development tools.

### Features

- Interactive planning dialog to configure settings
- Integration with OpenAI language models
- Support for both Windows (PowerShell) and Unix (Bash) environments
- Project-aware context handling
- Browser-based interface for interaction

### Usage

1. Trigger the action from IntelliJ IDEA (via menu or shortcut)
2. Configure planning settings in the dialog:

- Language models for planning and parsing
- Working directory
- Environment settings
- API keys for GitHub and Google services
- Temperature setting for AI responses

3. Click OK to launch the planning assistant
4. A browser window will open automatically with the planning interface

### Configuration Options

- **Default Model**: Main language model used for planning (configured via app settings)
- **Parsing Model**: Secondary model for parsing tasks (configured via app settings)
- **Working Directory**: Project root or selected folder
- **GitHub Token**: For GitHub API integration
- **Google API Key**: For Google search capabilities
- **Google Search Engine ID**: For custom search integration
- **Temperature**: Controls AI response randomness/creativity

### Requirements

- IntelliJ IDEA
- Valid API keys configured in app settings
- Internet connection for API access
- Compatible operating system (Windows/Unix)

### Technical Notes

- Runs background tasks on a separate thread (BGT)
- Creates unique session IDs for each planning instance
- Supports both PowerShell and Bash command environments
- Integrates with project file system for context awareness

### Troubleshooting

If the browser doesn't open automatically:

1. Check the IDE log for the URL
2. Wait a few seconds and try again
3. Ensure no firewall is blocking local connections
4. Verify API keys are properly configured

For any issues, check the IDE logs for detailed error messages.

# plan\PlanChatAction.kt

Here's the user documentation for the PlanChatAction class:

## PlanChatAction Documentation

### Overview

PlanChatAction is an IntelliJ IDEA plugin action that provides an interactive chat interface for executing and planning commands in your development
environment. It supports both Windows (PowerShell) and Unix (Bash) environments.

### Features

- Interactive command planning and execution interface
- Support for both PowerShell (Windows) and Bash (Unix) shells
- Configurable settings through a dialog
- Integration with project workspace
- Browser-based chat interface

### Usage

#### Prerequisites

- IntelliJ IDEA with the AI Coder plugin installed
- A selected folder or file in the project explorer
- Valid API credentials configured in settings (if using GitHub or Google APIs)

#### Steps to Use

1. **Activation**:

- Select a folder or file in your project
- Trigger the Plan Chat action from the IDE menu or toolbar

2. **Configuration Dialog**:
   A settings dialog will appear where you can configure:

- Default model for main operations
- Parsing model for command interpretation
- Temperature setting for AI responses
- Working directory
- GitHub token (optional)
- Google API credentials (optional)

3. **Chat Interface**:

- After configuration, a browser window will open with the chat interface
- Use the interface to plan and execute commands
- Interact with the AI to get assistance with command execution

#### Settings

The action uses several configuration parameters:

- `defaultModel`: Main AI model for chat interactions
- `parsingModel`: AI model for parsing commands
- `temperature`: Controls AI response randomness
- `workingDir`: Base directory for command execution
- `githubToken`: For GitHub API integration
- `googleApiKey` and `googleSearchEngineId`: For Google API integration

### Technical Details

- The action runs in a background thread (BGT)
- Creates a new session for each chat instance
- Automatically detects the operating system for shell selection
- Integrates with the project's module structure
- Uses a web-based interface served locally

### Troubleshooting

If you encounter issues:

1. Check if a folder or file is selected in the project explorer
2. Verify API credentials in settings if using external services
3. Ensure proper project permissions for command execution
4. Check IDE logs for detailed error messages

### Requirements

- Active project in IntelliJ IDEA
- Proper file/folder permissions
- Internet connection for AI model access
- Valid API credentials (if using external services)

# plan\PlanConfigDialog.kt

Here's the user documentation for the PlanConfigDialog class:

## Plan Configuration Dialog Documentation

### Overview

The Plan Configuration Dialog provides a comprehensive interface for configuring AI-powered task planning and execution settings. It allows users to manage task
configurations, set global parameters, and customize individual task behaviors.

### Main Features

#### Configuration Management

- **Save/Load Configurations**: Save and load named configuration presets
- **Delete Configurations**: Remove saved configuration presets
- **Configuration Naming**: Names must contain only letters, numbers, underscores and hyphens

#### Global Settings

- **Auto-fix**: Toggle automatic application of suggested fixes without confirmation
- **Allow Blocking**: Enable/disable UI blocking during task processing
- **Temperature**: Adjust AI response creativity (0-1 scale)
  - Lower values: More focused, deterministic responses
  - Higher values: More creative, varied responses

#### Task Configuration

##### Task List

- Shows all available task types with visual indicators:
  - **Bold**: Enabled tasks
  - *Italic*: Disabled tasks
- Hover over tasks to see detailed descriptions

##### Task Settings

Each task can be configured with:

- **Enable/Disable**: Toggle task activation
- **Model Selection**: Choose the AI model to use
- **Command Settings** (for Command Auto-Fix tasks only):
  - Enable/disable specific commands
  - Add new commands
  - Remove existing commands

#### Available Task Types

1. **Performance Analysis**

- Analyzes code performance
- Identifies bottlenecks
- Suggests optimizations

2. **Web Fetch and Transform**

- Downloads web content
- Converts to specified formats
- Handles content limitations

3. **Search Operations**

- GitHub Search
- Google Search
- Pattern-based Search
- Semantic Search

4. **Knowledge Management**

- Content Indexing
- Web Search and Index
- Documentation Generation

5. **Code Operations**

- File Modification
- Code Review
- Test Generation
- Optimization
- Security Audit
- Refactoring

6. **Task Management**

- Task Planning
- Command Sessions
- Selenium Sessions
- Shell Command Execution

### Usage Tips

1. **Configuration Management**

- Save frequently used configurations for quick access
- Confirm before overwriting existing configurations
- Validate configurations before saving

2. **Task Selection**

- Enable only needed tasks to optimize performance
- Ensure models are selected for enabled tasks
- Review task descriptions for optimal usage

3. **Model Selection**

- Choose appropriate models based on task requirements
- Verify API key availability for selected models
- Consider model capabilities and limitations

4. **Performance Optimization**

- Adjust temperature based on needed creativity level
- Use auto-fix carefully in production environments
- Consider blocking implications in UI-heavy workflows

### Error Handling

The dialog provides validation and error checking for:

- Model selection for enabled tasks
- Configuration name formatting
- Command path validity
- Unsaved changes protection

### Best Practices

1. **Configuration Management**

- Use descriptive names for configurations
- Regular backup of important configurations
- Document configuration purposes

2. **Task Setup**

- Start with minimal enabled tasks
- Test configurations in non-production environment
- Monitor task performance and adjust settings

3. **Model Selection**

- Match model capabilities to task requirements
- Consider cost implications of model choices
- Maintain backup model options

4. **Security Considerations**

- Review command permissions carefully
- Validate web content sources
- Monitor auto-fix behavior

This dialog is a powerful tool for managing AI-powered development tasks. Take time to understand each setting's impact on your workflow for optimal results.

# plan\PrePlanAction.kt

Here's the user documentation for the PrePlanAction class:

## PrePlanAction Documentation

### Overview

PrePlanAction is a UI action that allows users to initiate pre-planned development tasks by providing task breakdown information in JSON format. It integrates
with a planning system to help organize and execute development workflows.

### Features

- Accepts task breakdown specifications in JSON format
- Supports template variables with interactive form filling
- Configurable planning settings including models, working directory, and API keys
- Integrates with browser-based task execution interface

### Usage

1. **Initiating the Action**

- Trigger the PrePlanAction from your IDE
- A dialog will appear requesting JSON input for task breakdown

2. **JSON Input Format**
   The input should follow the TaskBreakdownWithPrompt format, which can include:

- Task descriptions
- Execution steps
- Required resources
- Template variables using `{{variableName}}` syntax

3. **Template Variables**

- If your JSON contains template variables (e.g., `{{projectName}}`)
- A form dialog will appear to collect values for each variable
- Enter values for each variable to customize the task breakdown

4. **Configuration**
   After providing the JSON input, you can configure:

- Default and parsing models
- Command shell (PowerShell for Windows, Bash for others)
- Temperature settings
- Working directory
- Environment variables
- GitHub token
- Google API credentials

5. **Execution**

- After configuration, a browser window will open
- The task breakdown will be displayed in a web interface
- You can monitor and control task execution from this interface

### Requirements

- Valid JSON input following TaskBreakdownWithPrompt structure
- Appropriate API credentials if using GitHub or Google services
- Project context for working directory
- Internet connection for web interface

### Error Handling

- Invalid JSON input will display an error message
- Configuration can be cancelled at any time
- Browser launch failures are logged but non-blocking

### Notes

- The action runs in background thread (BGT) to prevent UI freezing
- Session information is preserved for the duration of task execution
- Supports both Windows and Unix-based systems
- Default settings are pulled from application configuration

# plan\SingleTaskAction.kt

Here's the user documentation for the SingleTaskAction class:

## SingleTaskAction Documentation

### Overview

SingleTaskAction is an IntelliJ IDEA plugin action that provides a single-task interface for executing AI-assisted tasks in your project. It allows users to
configure and run individual tasks with customized settings and context awareness.

### Features

- Interactive configuration dialog for task settings
- Support for both Windows (PowerShell) and Unix (Bash) environments
- Context-aware task execution using selected files
- Integration with OpenAI models
- Browser-based task interface

### Usage

#### Basic Steps

1. Trigger the action from IntelliJ IDEA
2. Configure task settings in the dialog that appears:

- Select AI models for task execution and parsing
- Set temperature for AI responses (0.0-1.0)
- Configure working directory
- Add optional environment variables
- Set API tokens (GitHub, Google) if needed

3. Click OK to start the task
4. A browser window will automatically open with the task interface

#### Context Selection

- The action can use selected files as context
- Files under 512KB are included in the context
- Context information includes file paths, sizes, and token counts

#### Configuration Options

- Default Model: Main AI model for task execution
- Parsing Model: Secondary AI model for parsing
- Temperature: Controls AI response randomness
- Working Directory: Base directory for task execution
- Environment Variables: Custom environment settings
- API Tokens: Optional integration with external services

### Technical Details

- Uses Session-based execution
- Supports both PowerShell and Bash command environments
- Integrates with project file system
- Provides async task execution
- Includes progress indication
- Browser-based interface via local server

### Requirements

- IntelliJ IDEA
- Valid API credentials (if using external services)
- Internet connection for AI model access

### Notes

- Task sessions are uniquely identified
- File context is automatically gathered from selection
- Large files (>512KB) are excluded from context
- Task execution happens asynchronously
- Progress is displayed in IDE

### Error Handling

- Initialization failures are reported to the user
- Browser launch failures are logged but non-blocking
- File reading errors are handled gracefully

This action is part of the AI Coder plugin suite and provides a streamlined interface for single-task AI-assisted development operations.

# problems\AnalyzeProblemAction.kt

Here's the user documentation for the AnalyzeProblemAction class:

## Problem Analysis Action

The Problem Analysis Action is a powerful tool that helps developers analyze and fix code problems identified in the IDE's problem view. It provides detailed
analysis and suggested fixes for code issues.

### Features

- Analyzes problems/errors in your code
- Provides context-aware suggestions for fixes
- Generates code patches in diff format
- Allows direct application of suggested fixes
- Shows related files that may be relevant to the problem

### How to Use

1. Open your project in the IDE
2. Navigate to the Problems tool window
3. Select a problem/error you want to analyze
4. Right-click and select "Analyze Problem" from the context menu

### What Happens

When you trigger the action:

1. A new analysis session opens in your browser
2. The tool collects relevant information including:

- File path and type
- Problem description
- Code context around the error
- Project structure
- Related file contents

3. The AI analyzes the problem and provides:

- Detailed error analysis
- List of files that need fixing
- Related files for debugging context
- Specific code patches in diff format

4. You can review the suggested fixes and:

- View the changes in diff format
- Apply patches directly to your code
- Navigate to related files
- See the full analysis in a tabbed interface

### Key Components

- **Problem Context**: Shows the exact location (line and column) of the error with surrounding code context
- **Project Structure**: Provides relevant project structure information for context
- **File Content**: Shows complete content of affected files
- **Suggested Fixes**: Presents code patches in standard diff format
- **Interactive UI**: Allows direct application of fixes and navigation between files

### Requirements

- Active project in the IDE
- Selected problem in the Problems view
- Valid file context

### Notes

- The analysis runs in a background thread to prevent UI freezing
- Suggested fixes are presented as diffs that can be applied directly
- All changes can be reviewed before applying
- The tool integrates with your project's Git repository for better context

This tool is especially useful for:

- Understanding complex errors
- Getting AI-powered fix suggestions
- Reviewing multiple related files
- Applying fixes efficiently

# test\TestResultAutofixAction.kt

Here's the user documentation for the TestResultAutofixAction:

## Test Result Autofix Action

The Test Result Autofix Action is an intelligent assistant that helps analyze test failures and suggests fixes automatically. It provides an interactive
interface to review test failures and apply suggested code changes.

### Features

- Automatically analyzes test failures and error messages
- Identifies relevant files that need to be fixed
- Suggests specific code changes in diff format
- Provides one-click application of suggested fixes
- Shows file context and project structure

### When to Use

Use this action when:

- A test has failed and you want AI assistance in fixing it
- You need help understanding why a test is failing
- You want to quickly implement fixes for test failures

### How to Use

1. **Access the Action**

- Run your tests in IntelliJ IDEA
- When a test fails, select the failed test in the test runner window
- Right-click and select the "Test Result Autofix" action

2. **Review Analysis**

- The action will open a browser window with the analysis
- You'll see:
  - Test failure details
  - Identified errors
  - Affected files
  - Suggested fixes in diff format

3. **Apply Fixes**

- Review the suggested code changes
- Click the "Apply" links next to each diff to implement the changes
- Changes will be applied to your project files automatically

### Features in Detail

#### Test Analysis

- Shows test name and duration
- Displays complete error messages
- Includes stack traces when available
- Identifies related files that may be relevant

#### Fix Suggestions

- Provides context-aware code changes
- Shows changes in standard diff format
- Includes surrounding code context
- Allows selective application of fixes

#### Project Context

- Shows relevant project structure
- Lists related files that may need review
- Provides file content for context

### Tips

- Review all suggested fixes carefully before applying them
- Check related files mentioned in the analysis
- Consider the broader context of the test failure
- Use the file links to quickly navigate to relevant code

### Technical Notes

- The action works with IntelliJ's test framework
- Supports all test types that integrate with IntelliJ
- Changes are applied through IntelliJ's file system
- Uses AI to analyze and suggest fixes

### Limitations

- May not catch all possible fix scenarios
- Suggestions should be reviewed before applying
- Large files (>0.5MB) are excluded from analysis
- Requires an active internet connection

This documentation provides a comprehensive overview of the TestResultAutofixAction functionality and how to use it effectively in your development workflow.