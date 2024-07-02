# BaseAction.kt


## Shared Functionality Analysis: BaseAction.kt


### Code Overview
- **Language & Frameworks:** Kotlin, IntelliJ Platform SDK
- **Dependencies:** 
  - com.github.simiacryptus.aicoder.util.IdeaOpenAIClient
  - com.github.simiacryptus.aicoder.util.UITools
  - com.intellij.openapi.actionSystem.AnAction
  - com.intellij.openapi.actionSystem.AnActionEvent
  - com.simiacryptus.jopenai.OpenAIClient
  - org.slf4j.LoggerFactory


### Common Logic


#### Function 1: isEnabled
- **Description:** Determines if the action should be enabled and visible
- **Purpose:** To control the availability of the action in the UI
- **Functionality:** Returns a boolean value indicating if the action is enabled
- **Location and Accessibility:** Already a public method in BaseAction class
- **Dependencies:** AnActionEvent


#### Function 2: handle
- **Description:** Abstract method to handle the action when performed
- **Purpose:** To define the specific behavior of each action
- **Functionality:** Implements the core logic of the action
- **Location and Accessibility:** Already an abstract method in BaseAction class
- **Dependencies:** AnActionEvent


#### Function 3: logAction
- **Description:** Logs the action being performed
- **Purpose:** To keep track of user actions for debugging or analytics
- **Functionality:** Logs the action name using UITools.logAction
- **Location and Accessibility:** Currently part of actionPerformed, could be extracted as a separate method
- **Dependencies:** UITools


#### Function 4: errorHandling
- **Description:** Handles and logs errors that occur during action execution
- **Purpose:** To provide consistent error handling across all actions
- **Functionality:** Catches exceptions and logs them using UITools.error
- **Location and Accessibility:** Currently part of actionPerformed, could be extracted as a separate method
- **Dependencies:** UITools, LoggerFactory


#### Function 5: getOpenAIClient
- **Description:** Provides access to the OpenAI client instance
- **Purpose:** To centralize access to the OpenAI client
- **Functionality:** Returns the IdeaOpenAIClient instance
- **Location and Accessibility:** Currently a property (api), could be refactored as a static method
- **Dependencies:** IdeaOpenAIClient

These functions represent common functionality that could be useful across multiple components. Some refactoring might be beneficial to make them more accessible and reusable, particularly for the error handling and action logging functions.# code\DescribeAction.kt


## Shared Functionality Analysis: DescribeAction.kt


### Code Overview
- **Language & Frameworks:** Kotlin, IntelliJ Platform SDK
- **Dependencies:** 
  - com.github.simiacryptus.aicoder.actions.SelectionAction
  - com.github.simiacryptus.aicoder.config.AppSettingsState
  - com.github.simiacryptus.aicoder.util.IndentedText
  - com.intellij.openapi.actionSystem.ActionUpdateThread
  - com.intellij.openapi.project.Project
  - com.simiacryptus.jopenai.proxy.ChatProxy
  - com.simiacryptus.jopenai.util.StringUtil


### Common Logic


#### Function 1: Code Description Generation
- **Description:** Generates a description of the given code snippet
- **Purpose:** To provide a human-readable explanation of code functionality
- **Functionality:** 
  - Takes code, computer language, and human language as input
  - Uses a ChatProxy to generate a description
  - Formats the description with proper indentation and comment style
- **Location and Accessibility:** Currently part of the `processSelection` method. Could be extracted into a separate utility function.
- **Dependencies:** AppSettingsState, ChatProxy, StringUtil


#### Function 2: Comment Style Selection
- **Description:** Selects appropriate comment style based on description length
- **Purpose:** To format code descriptions using the most suitable comment style
- **Functionality:**
  - Determines if the description is single or multi-line
  - Chooses between line comment and block comment styles accordingly
- **Location and Accessibility:** Part of the `processSelection` method. Could be extracted into a utility function.
- **Dependencies:** Language-specific comment style information


#### Function 3: Indented Text Processing
- **Description:** Handles indentation of text blocks
- **Purpose:** To maintain proper code formatting when inserting descriptions
- **Functionality:**
  - Converts selected text to IndentedText
  - Applies appropriate indentation to the description and original code
- **Location and Accessibility:** Used within `processSelection`. Already utilizes the `IndentedText` utility class.
- **Dependencies:** IndentedText utility class

These functions represent core functionalities that could be useful across multiple components of the plugin, especially for actions that involve code analysis, description generation, and text formatting. Extracting these into separate utility functions or classes could improve code reusability and maintainability.# ApplyPatchAction.kt


## Shared Functionality Analysis: ApplyPatchAction.kt


### Code Overview
- **Language & Frameworks:** Kotlin, IntelliJ Platform SDK
- **Dependencies:** 
  - com.github.simiacryptus.aicoder.util.UITools
  - com.intellij.openapi.actionSystem.AnActionEvent
  - com.intellij.openapi.command.WriteCommandAction
  - com.intellij.openapi.ui.Messages
  - com.intellij.openapi.vfs.VirtualFile
  - com.intellij.psi.PsiManager
  - com.simiacryptus.diff.IterativePatchUtil


### Common Logic


#### Function 1: applyPatch
- **Description:** Applies a patch to a given file
- **Purpose:** To modify the content of a file based on a provided patch
- **Functionality:** 
  1. Runs a write command action
  2. Finds the PSI file corresponding to the given VirtualFile
  3. Applies the patch to the file's content using IterativePatchUtil
  4. Updates the file's content with the patched version
- **Location and Accessibility:** Currently a private method in ApplyPatchAction class. Should be refactored into a utility class for broader use.
- **Dependencies:** 
  - com.intellij.openapi.command.WriteCommandAction
  - com.intellij.psi.PsiManager
  - com.simiacryptus.diff.IterativePatchUtil


#### Function 2: promptForPatchContent
- **Description:** Prompts the user to input patch content
- **Purpose:** To get patch content from the user through a dialog
- **Functionality:** 
  1. Shows a multi-line input dialog
  2. Returns the user's input or null if cancelled
- **Location and Accessibility:** This functionality is currently embedded in the handle method. It should be extracted into a separate method for reusability.
- **Dependencies:** 
  - com.intellij.openapi.ui.Messages


#### Function 3: getSelectedFile
- **Description:** Retrieves the currently selected file
- **Purpose:** To get the VirtualFile object of the file the user is working on
- **Functionality:** 
  1. Uses UITools to get the selected file from the AnActionEvent
- **Location and Accessibility:** This is already a utility method in UITools class, which is good for reusability.
- **Dependencies:** 
  - com.github.simiacryptus.aicoder.util.UITools

These functions represent common operations that could be useful across multiple components in the project. Refactoring them into utility classes or shared services would improve code reusability and maintainability.# code\CustomEditAction.kt


## Shared Functionality Analysis: CustomEditAction.kt


### Code Overview
- **Language & Frameworks:** Kotlin, IntelliJ Platform SDK
- **Dependencies:** 
  - com.github.simiacryptus.aicoder.actions.SelectionAction
  - com.github.simiacryptus.aicoder.config.AppSettingsState
  - com.github.simiacryptus.aicoder.util.UITools
  - com.intellij.openapi.actionSystem.ActionUpdateThread
  - com.intellij.openapi.project.Project
  - com.simiacryptus.jopenai.proxy.ChatProxy


### Common Logic


#### Function 1: editCode
- **Description:** A method to edit code based on given instructions and language parameters.
- **Purpose:** To provide a generic way to modify code snippets using AI-powered editing.
- **Functionality:** 
  - Takes input code, operation instructions, computer language, and human language.
  - Returns edited code and potentially the language of the edited code.
- **Location and Accessibility:** Currently part of the VirtualAPI interface. Could be extracted and made more general.
- **Dependencies:** Requires an AI model capable of understanding and modifying code.


#### Function 2: showInputDialog
- **Description:** A utility method to display an input dialog to the user.
- **Purpose:** To prompt the user for input in a standardized way across the application.
- **Functionality:** 
  - Displays a dialog with a given message and title.
  - Returns the user's input as a String.
- **Location and Accessibility:** Currently used within getConfig method. Could be extracted to a utility class.
- **Dependencies:** javax.swing.JOptionPane


#### Function 3: addInstructionToHistory
- **Description:** A method to add user instructions to a history list.
- **Purpose:** To maintain a record of recent user commands for potential reuse.
- **Functionality:** 
  - Adds a given instruction to a list of recent commands.
  - Potentially limits the size of the history list.
- **Location and Accessibility:** Currently used within processSelection method. Could be extracted to a utility class.
- **Dependencies:** AppSettingsState


#### Function 4: createChatProxy
- **Description:** A method to create a ChatProxy instance with specific settings.
- **Purpose:** To standardize the creation of ChatProxy instances across the application.
- **Functionality:** 
  - Creates a ChatProxy with specified class, API, temperature, and model.
  - Adds example interactions to the proxy.
- **Location and Accessibility:** Currently part of the proxy getter. Could be extracted and made more general.
- **Dependencies:** com.simiacryptus.jopenai.proxy.ChatProxy, AppSettingsState

These functions represent common patterns and functionalities that could be useful across multiple components of the application. Extracting and refactoring them into more general, reusable forms could improve code organization and reduce duplication.# code\RecentCodeEditsAction.kt


## Shared Functionality Analysis: RecentCodeEditsAction.kt


### Code Overview
- **Language & Frameworks:** Kotlin, IntelliJ Platform SDK
- **Dependencies:** 
  - com.github.simiacryptus.aicoder.config.AppSettingsState
  - com.github.simiacryptus.aicoder.util.ComputerLanguage
  - com.github.simiacryptus.aicoder.util.UITools
  - Various IntelliJ Platform classes (ActionGroup, AnAction, AnActionEvent, etc.)


### Common Logic


#### Function 1: isEnabled
- **Description:** Checks if the action should be enabled based on the current context.
- **Purpose:** Determines whether the action should be available to the user.
- **Functionality:** 
  1. Checks if there's a selection in the editor.
  2. Retrieves the computer language of the current file.
  3. Returns true if the language is not plain text.
- **Location and Accessibility:** Currently a companion object function in RecentCodeEditsAction. Could be refactored into a utility class for broader use.
- **Dependencies:** UITools, ComputerLanguage


#### Function 2: getRecentCommands
- **Description:** Retrieves recent custom edit commands from the application settings.
- **Purpose:** Provides a list of recently used custom edit instructions.
- **Functionality:** Accesses the AppSettingsState to get the most used history of custom edits.
- **Location and Accessibility:** Currently accessed through AppSettingsState.instance. Could be refactored into a separate utility function for easier reuse.
- **Dependencies:** AppSettingsState


#### Function 3: createCustomEditAction
- **Description:** Creates a new CustomEditAction with a specific instruction.
- **Purpose:** Generates action items for the recent edits menu.
- **Functionality:** 
  1. Creates a new CustomEditAction object.
  2. Sets the action's text, description, and icon.
  3. Overrides the getConfig method to return the specific instruction.
- **Location and Accessibility:** Currently implemented inline in getChildren. Could be refactored into a separate function for reuse in similar contexts.
- **Dependencies:** CustomEditAction

These functions represent common logic that could be useful across multiple components in the plugin. Refactoring them into separate utility classes or functions would improve code reusability and maintainability. For example, isEnabled could be part of a general ActionEnabler utility, while getRecentCommands and createCustomEditAction could be part of a CustomEditUtility class.# code\PasteAction.kt


## Shared Functionality Analysis: PasteAction.kt


### Code Overview
- **Language & Frameworks:** Kotlin, IntelliJ Platform SDK
- **Dependencies:** 
  - com.github.simiacryptus.aicoder.actions.SelectionAction
  - com.github.simiacryptus.aicoder.config.AppSettingsState
  - com.github.simiacryptus.aicoder.util.ComputerLanguage
  - com.intellij.openapi.actionSystem
  - com.simiacryptus.jopenai.proxy.ChatProxy
  - java.awt.Toolkit
  - java.awt.datatransfer.DataFlavor


### Common Logic


#### Function 1: hasClipboard
- **Description:** Checks if the system clipboard contains supported text data.
- **Purpose:** To determine if there's valid clipboard content for pasting.
- **Functionality:** 
  - Retrieves system clipboard contents
  - Checks if the content supports string or plain text Unicode flavors
- **Location and Accessibility:** Private method in PasteAction class. Could be refactored into a utility class for broader use.
- **Dependencies:** java.awt.Toolkit, java.awt.datatransfer.DataFlavor


#### Function 2: getClipboard
- **Description:** Retrieves the content of the system clipboard.
- **Purpose:** To access the clipboard data for pasting.
- **Functionality:**
  - Retrieves system clipboard contents
  - Returns the data as a string if it's in a supported format
- **Location and Accessibility:** Private method in PasteAction class. Could be refactored into a utility class for broader use.
- **Dependencies:** java.awt.Toolkit, java.awt.datatransfer.DataFlavor


#### Function 3: isLanguageSupported
- **Description:** Checks if a given computer language is supported for the paste action.
- **Purpose:** To determine if the paste action should be enabled for a specific language.
- **Functionality:**
  - Checks if the language is not null and not plain text
- **Location and Accessibility:** Public method in PasteAction class. Could be moved to a more general utility class for language support checks.
- **Dependencies:** com.github.simiacryptus.aicoder.util.ComputerLanguage


#### Function 4: convert (VirtualAPI interface)
- **Description:** Converts text from one language to another.
- **Purpose:** To transform clipboard content into the target language.
- **Functionality:**
  - Takes input text, source language, and target language
  - Returns converted text and detected language
- **Location and Accessibility:** Part of the VirtualAPI interface. Could be extracted into a separate language conversion utility.
- **Dependencies:** None directly in the interface definition

These functions represent common operations that could be useful across multiple components in the project, particularly for clipboard operations and language-related tasks. Refactoring them into separate utility classes would improve their reusability and maintainability.# dev\LineFilterChatAction.kt


## Shared Functionality Analysis: LineFilterChatAction.kt


### Code Overview
- **Language & Frameworks:** Kotlin, IntelliJ Platform SDK
- **Dependencies:** 
  - com.github.simiacryptus.aicoder.*
  - com.intellij.openapi.actionSystem.*
  - com.simiacryptus.skyenet.*
  - java.awt.Desktop


### Common Logic


#### Function 1: getComputerLanguage
- **Description:** Retrieves the computer language of the current file
- **Purpose:** To determine the programming language of the code being analyzed
- **Functionality:** Uses the ComputerLanguage utility to get the language based on the AnActionEvent
- **Location and Accessibility:** Currently used inline, could be extracted to a utility class
- **Dependencies:** ComputerLanguage, AnActionEvent


#### Function 2: renderMarkdown
- **Description:** Renders markdown content
- **Purpose:** To format the AI's response in markdown
- **Functionality:** Converts markdown text to HTML
- **Location and Accessibility:** Used in the renderResponse method, could be moved to a utility class
- **Dependencies:** MarkdownUtil


#### Function 3: createChatSocketManager
- **Description:** Creates a ChatSocketManager for handling chat sessions
- **Purpose:** To set up the chat interface for code analysis
- **Functionality:** Initializes a ChatSocketManager with specific settings and overrides
- **Location and Accessibility:** Currently inline in handle method, could be extracted to a separate method
- **Dependencies:** ChatSocketManager, AppSettingsState, ApplicationServices


#### Function 4: openBrowserToSession
- **Description:** Opens the default browser to the chat session URL
- **Purpose:** To provide easy access to the chat interface
- **Functionality:** Uses Desktop.browse to open the URL
- **Location and Accessibility:** Currently inline in a separate thread, could be extracted to a utility method
- **Dependencies:** Desktop, AppServer


#### Function 5: prepareCodeForChat
- **Description:** Prepares the selected or full code for chat analysis
- **Purpose:** To format the code with line numbers for easy reference in the chat
- **Functionality:** Splits the code into lines and adds line numbers
- **Location and Accessibility:** Currently inline in handle method, could be extracted to a utility method
- **Dependencies:** None

These functions represent common logic that could be useful across multiple components of the plugin. Extracting them into separate utility classes or methods would improve code organization and reusability.# code\RedoLast.kt


## Shared Functionality Analysis: RedoLast.kt


### Code Overview
- **Language & Frameworks:** Kotlin, IntelliJ Platform SDK
- **Dependencies:** 
  - com.github.simiacryptus.aicoder.actions.BaseAction
  - com.github.simiacryptus.aicoder.util.UITools.retry
  - com.intellij.openapi.actionSystem.*
  - com.intellij.openapi.editor.Document


### Common Logic

The RedoLast class doesn't contain any public static functions that could be directly shared across multiple components. However, it does utilize some shared functionality and patterns that could be useful in other parts of the application:


#### Action Update Thread Handling
- **Description:** The `getActionUpdateThread()` method specifies how the action should be updated.
- **Purpose:** To ensure that action updates are performed on the background thread.
- **Functionality:** Returns `ActionUpdateThread.BGT` for background thread updates.
- **Location and Accessibility:** This is already a method in the class, but similar logic could be extracted to a shared utility if needed across multiple actions.
- **Dependencies:** com.intellij.openapi.actionSystem.ActionUpdateThread


#### Retry Functionality
- **Description:** The `handle()` method uses a retry mechanism from UITools.
- **Purpose:** To re-execute the last AI Coder action performed in the editor.
- **Functionality:** Retrieves and runs the last action associated with the current document.
- **Location and Accessibility:** The retry functionality is already in a shared utility (UITools), which is good for reuse.
- **Dependencies:** com.github.simiacryptus.aicoder.util.UITools.retry


#### Action Enablement Check
- **Description:** The `isEnabled()` method checks if the action should be enabled.
- **Purpose:** To determine if the RedoLast action can be performed in the current context.
- **Functionality:** Checks if there's a retry action associated with the current document.
- **Location and Accessibility:** This logic is specific to RedoLast, but the pattern of checking conditions for action enablement could be generalized.
- **Dependencies:** com.intellij.openapi.actionSystem.AnActionEvent, com.intellij.openapi.actionSystem.CommonDataKeys

While this class doesn't provide directly shareable functions, it demonstrates patterns that could be useful in other actions:
1. Handling action update threads
2. Utilizing shared utilities for common operations (like retry)
3. Checking conditions for action enablement

These patterns could be further abstracted into shared utilities or base classes if similar functionality is needed across multiple actions in the AI Coder plugin.# dev\PrintTreeAction.kt


## Shared Functionality Analysis: PrintTreeAction.kt


### Code Overview
- **Language & Frameworks:** Kotlin, IntelliJ Platform SDK
- **Dependencies:** 
  - com.github.simiacryptus.aicoder.actions.BaseAction
  - com.github.simiacryptus.aicoder.config.AppSettingsState
  - com.github.simiacryptus.aicoder.util.psi.PsiUtil
  - com.intellij.openapi.actionSystem.ActionUpdateThread
  - com.intellij.openapi.actionSystem.AnActionEvent
  - org.slf4j.LoggerFactory


### Common Logic


#### Function 1: PsiUtil.printTree
- **Description:** Prints the tree structure of a PsiElement
- **Purpose:** Debugging and development tool to visualize the structure of a PsiFile
- **Functionality:** Converts the PsiElement tree structure into a string representation
- **Location and Accessibility:** Located in PsiUtil class, likely accessible as a public static method
- **Dependencies:** PsiElement


#### Function 2: PsiUtil.getLargestContainedEntity
- **Description:** Retrieves the largest PsiElement contained within the current context
- **Purpose:** To find the most relevant PsiElement for the current action context
- **Functionality:** Analyzes the AnActionEvent to determine the largest relevant PsiElement
- **Location and Accessibility:** Located in PsiUtil class, likely accessible as a public static method
- **Dependencies:** AnActionEvent


#### Function 3: AppSettingsState.instance.devActions
- **Description:** Checks if developer actions are enabled in the application settings
- **Purpose:** To control access to developer-specific actions
- **Functionality:** Returns a boolean indicating whether dev actions are enabled
- **Location and Accessibility:** Located in AppSettingsState class, accessible as a property of the singleton instance
- **Dependencies:** None

These functions provide useful utilities for working with PSI (Program Structure Interface) elements and managing developer-specific features. They could be valuable in other actions or components that need to interact with the PSI tree or respect developer settings.# generic\CodeChatAction.kt


## Shared Functionality Analysis: CodeChatAction.kt


### Code Overview
- **Language & Frameworks:** Kotlin, IntelliJ Platform SDK
- **Dependencies:** 
  - AppServer
  - BaseAction
  - AppSettingsState
  - CodeChatSocketManager
  - ComputerLanguage
  - ApplicationServices
  - StorageInterface
  - ApplicationServer
  - Various IntelliJ Platform classes


### Common Logic


#### Function 1: getComputerLanguage
- **Description:** Retrieves the computer language for the current context
- **Purpose:** Determine the programming language of the current file or selection
- **Functionality:** Uses ComputerLanguage utility to get the language based on the AnActionEvent
- **Location and Accessibility:** Currently part of ComputerLanguage class, could be extracted as a utility function
- **Dependencies:** ComputerLanguage, AnActionEvent


#### Function 2: createCodeChatSession
- **Description:** Creates a new code chat session
- **Purpose:** Set up a new chat session for code-related discussions
- **Functionality:** 
  - Generates a new session ID
  - Creates a CodeChatSocketManager instance
  - Sets up session information in ApplicationServer
- **Location and Accessibility:** Currently embedded in handle method, could be extracted as a separate function
- **Dependencies:** StorageInterface, CodeChatSocketManager, ApplicationServer, AppSettingsState


#### Function 3: openBrowserToSession
- **Description:** Opens the default web browser to the chat session URL
- **Purpose:** Provide easy access to the newly created chat session
- **Functionality:** 
  - Constructs the session URL
  - Uses Desktop API to open the default browser
- **Location and Accessibility:** Currently embedded in a separate thread in handle method, could be extracted as a utility function
- **Dependencies:** Desktop API, AppServer


#### Function 4: getSessionUrl
- **Description:** Constructs the URL for a chat session
- **Purpose:** Generate the correct URL for accessing a specific chat session
- **Functionality:** Combines the server URI with the session ID
- **Location and Accessibility:** Currently embedded in openBrowserToSession logic, could be extracted as a utility function
- **Dependencies:** AppServer

These functions represent common logic that could be useful across multiple components of the plugin. Extracting them into separate utility functions or a dedicated service class would improve code organization and reusability. This refactoring would also make the CodeChatAction class more focused on its primary responsibility of handling the action event.# FileContextAction.kt


## Shared Functionality Analysis: FileContextAction.kt


### Code Overview
- **Language & Frameworks:** Kotlin, IntelliJ Platform SDK
- **Dependencies:** 
  - com.github.simiacryptus.aicoder.config.AppSettingsState
  - com.github.simiacryptus.aicoder.util.UITools
  - Various IntelliJ Platform classes (AnActionEvent, FileEditorManager, LocalFileSystem, etc.)


### Common Logic


#### Function 1: open
- **Description:** Opens a file in the IntelliJ IDE editor
- **Purpose:** To provide a way to open generated or modified files in the IDE
- **Functionality:** 
  - Checks if the file exists
  - Refreshes the file system to ensure the file is visible
  - Opens the file in the IDE editor
  - Schedules retries if the file is not immediately available
- **Location and Accessibility:** Already a companion object function, accessible as is
- **Dependencies:** IntelliJ Platform SDK (Project, FileEditorManager, LocalFileSystem)


#### Function 2: processSelection (abstract)
- **Description:** Processes the selected file or folder
- **Purpose:** To define the core logic for each specific FileContextAction implementation
- **Functionality:** 
  - Takes a SelectionState and configuration as input
  - Returns an array of Files (presumably new or modified files)
- **Location and Accessibility:** Abstract method, needs to be implemented in subclasses
- **Dependencies:** Depends on the specific implementation


#### Function 3: getConfig (open)
- **Description:** Retrieves configuration for the action
- **Purpose:** To allow subclasses to provide custom configuration
- **Functionality:** 
  - Takes a Project and AnActionEvent as input
  - Returns a configuration object of type T or null
- **Location and Accessibility:** Open method, can be overridden in subclasses
- **Dependencies:** None specific to this method


#### Function 4: isEnabled
- **Description:** Determines if the action should be enabled
- **Purpose:** To control when the action is available in the IDE
- **Functionality:** 
  - Checks if the action is generally enabled
  - Verifies if it's a dev action and if dev actions are allowed
  - Ensures the selected item is a file or folder as appropriate
- **Location and Accessibility:** Override of BaseAction method, accessible as is
- **Dependencies:** AppSettingsState, UITools


#### Function 5: handle
- **Description:** Handles the execution of the action
- **Purpose:** To orchestrate the action's workflow
- **Functionality:** 
  - Retrieves configuration and selected file/folder
  - Processes the selection in a background thread
  - Manages UI updates and error handling
- **Location and Accessibility:** Override of BaseAction method, accessible as is
- **Dependencies:** UITools, various IntelliJ Platform SDK classes

These functions provide a robust framework for file-based actions in the IntelliJ IDE. The abstract nature of the class allows for easy extension and customization for specific use cases, while providing common functionality for file handling, UI interaction, and action management.# generic\CommandAutofixAction.kt


## Shared Functionality Analysis: CommandAutofixAction.kt


### Code Overview
- **Language & Frameworks:** Kotlin, IntelliJ Platform SDK
- **Dependencies:** 
  - com.github.simiacryptus.aicoder
  - com.intellij.openapi
  - com.simiacryptus.jopenai
  - com.simiacryptus.skyenet
  - javax.swing
  - java.awt
  - java.io
  - java.nio
  - org.slf4j


### Common Logic


#### Function 1: getFiles
- **Description:** Recursively collects file paths from given virtual files.
- **Purpose:** To gather a set of file paths for processing.
- **Functionality:** 
  - Recursively traverses directories
  - Filters out hidden directories and those matched by .gitignore
  - Collects file paths into a set
- **Location and Accessibility:** Private method in CommandAutofixAction class. Could be refactored to be more general and public.
- **Dependencies:** VirtualFile from IntelliJ Platform SDK


#### Function 2: isGitignore
- **Description:** Checks if a file or path is ignored by Git.
- **Purpose:** To filter out files that should not be processed according to Git rules.
- **Functionality:** 
  - Traverses up the directory tree looking for .gitignore files
  - Checks if the given file matches any pattern in the .gitignore files
- **Location and Accessibility:** Companion object method. Already accessible but could be moved to a utility class.
- **Dependencies:** java.nio.file.Path


#### Function 3: htmlEscape (extension property)
- **Description:** Escapes HTML special characters in a StringBuilder.
- **Purpose:** To safely display text content in HTML context.
- **Functionality:** Replaces special characters with their HTML entity equivalents.
- **Location and Accessibility:** Extension property in Companion object. Could be moved to a general utility class.
- **Dependencies:** None


#### Function 4: codeSummary
- **Description:** Generates a summary of code files.
- **Purpose:** To provide a concise overview of the project's code files.
- **Functionality:** 
  - Filters and reads content of specified files
  - Formats the content with file paths and language indicators
- **Location and Accessibility:** Abstract method in PatchApp inner class. Could be refactored into a separate utility class.
- **Dependencies:** java.nio.file.Path, Settings class


#### Function 5: projectSummary
- **Description:** Generates a summary of the project structure.
- **Purpose:** To provide an overview of the project's file structure.
- **Functionality:** 
  - Lists all code files in the project
  - Includes file paths and sizes
- **Location and Accessibility:** Abstract method in PatchApp inner class. Could be refactored into a separate utility class.
- **Dependencies:** Settings class

These functions provide core functionality that could be useful across different components of the plugin. They handle file operations, Git-related checks, and project summarization, which are likely to be needed in various parts of the application. Refactoring them into a separate utility class would improve their reusability and maintainability.# generic\CreateFileFromDescriptionAction.kt


## Shared Functionality Analysis: CreateFileFromDescriptionAction.kt


### Code Overview
- **Language & Frameworks:** Kotlin, IntelliJ Platform SDK
- **Dependencies:** 
  - com.github.simiacryptus.aicoder.actions.FileContextAction
  - com.github.simiacryptus.aicoder.config.AppSettingsState
  - com.intellij.openapi.actionSystem.ActionUpdateThread
  - com.simiacryptus.jopenai.ApiModel
  - com.simiacryptus.jopenai.util.ClientUtil
  - java.io.File


### Common Logic


#### Function 1: generateFile
- **Description:** Generates a new file based on a given directive and base path.
- **Purpose:** To create a new file with content based on user input and AI generation.
- **Functionality:** 
  - Takes a base path and a directive as input
  - Uses OpenAI API to generate file content based on the directive
  - Parses the API response to extract the file path and content
  - Returns a ProjectFile object containing the generated path and code
- **Location and Accessibility:** Currently a private method in CreateFileFromDescriptionAction class. Could be refactored into a utility class for broader use.
- **Dependencies:** 
  - AppSettingsState
  - OpenAI API client


#### Function 2: processSelection
- **Description:** Processes the user's file selection and generates a new file.
- **Purpose:** To handle the action of creating a new file from a description in the context of the selected file.
- **Functionality:**
  - Calculates relative paths and module roots
  - Calls generateFile to create new file content
  - Handles file naming conflicts
  - Creates directories if needed
  - Writes the generated content to the file
- **Location and Accessibility:** Currently part of CreateFileFromDescriptionAction class. Could be generalized for other file creation actions.
- **Dependencies:**
  - File system operations
  - generateFile function


#### Function 3: Path Resolution Logic
- **Description:** Logic for resolving relative paths and handling ".." segments.
- **Purpose:** To correctly determine the target location for the new file.
- **Functionality:**
  - Splits the input path into segments
  - Handles ".." segments to determine the correct module root
  - Calculates the relative file path
- **Location and Accessibility:** Currently embedded in processSelection. Could be extracted into a separate utility function.
- **Dependencies:** None


#### Function 4: File Naming Conflict Resolution
- **Description:** Logic for handling naming conflicts when creating new files.
- **Purpose:** To ensure that new files don't overwrite existing ones.
- **Functionality:**
  - Checks if a file with the generated name already exists
  - If it does, appends a numeric index to the filename
  - Increments the index until an available filename is found
- **Location and Accessibility:** Currently embedded in processSelection. Could be extracted into a separate utility function.
- **Dependencies:** File system operations

These functions and logical components could be refactored into utility classes or shared services to be used across multiple actions or components in the plugin, improving code reusability and maintainability.# generic\CreateImageAction.kt


## Shared Functionality Analysis: CreateImageAction.kt


### Code Overview
- **Language & Frameworks:** Kotlin, IntelliJ Platform SDK
- **Dependencies:** 
  - IntelliJ Platform SDK
  - SkyeNet library
  - JOpenAI library
  - Java AWT
  - Java ImageIO


### Common Logic


#### Function 1: getFiles
- **Description:** Recursively collects file paths from given virtual files
- **Purpose:** To gather all relevant files for code summary
- **Functionality:** 
  - Traverses directory structure
  - Collects relative paths of files
- **Location and Accessibility:** Already a private method, could be made public static
- **Dependencies:** IntelliJ VirtualFile API


#### Function 2: codeSummary
- **Description:** Generates a markdown summary of code files
- **Purpose:** To provide context for image generation
- **Functionality:**
  - Reads file contents
  - Formats code with file paths and language-specific markdown
- **Location and Accessibility:** Currently a local function, could be extracted and made public static
- **Dependencies:** Java File I/O


#### Function 3: write
- **Description:** Writes an image to a file and returns its byte array
- **Purpose:** To save generated images and prepare them for display
- **Functionality:**
  - Writes image to a ByteArrayOutputStream
  - Determines file format from path
  - Returns byte array of the image
- **Location and Accessibility:** Already a private method, could be made public static
- **Dependencies:** Java ImageIO, Java I/O


#### Function 4: renderMarkdown
- **Description:** Renders markdown content with image links
- **Purpose:** To display generated images in the UI
- **Functionality:**
  - Creates HTML img tags for PNG and JPG versions of the image
  - Utilizes the ApplicationInterface to render markdown
- **Location and Accessibility:** Part of the PatchAgent class, could be extracted and made public static
- **Dependencies:** SkyeNet ApplicationInterface

These functions provide core functionality that could be useful in other parts of the application, especially for file handling, code summarization, image processing, and UI rendering. Extracting and refactoring them into a utility class would improve their reusability across the project.# generic\DiffChatAction.kt


## Shared Functionality Analysis: DiffChatAction.kt


### Code Overview
- **Language & Frameworks:** Kotlin, IntelliJ Platform SDK
- **Dependencies:** 
  - com.github.simiacryptus.aicoder.*
  - com.simiacryptus.skyenet.*
  - com.intellij.openapi.*
  - org.slf4j.LoggerFactory


### Common Logic


#### Function 1: addApplyDiffLinks
- **Description:** A function that adds apply links to diff blocks in the response.
- **Purpose:** To allow users to easily apply code changes suggested in the diff format.
- **Functionality:** 
  - Processes the response text
  - Identifies diff blocks
  - Adds clickable links to apply the changes
  - Handles the application of changes when links are clicked
- **Location and Accessibility:** This function is currently used within the `renderResponse` method. It could be extracted and made into a standalone utility function.
- **Dependencies:** 
  - Editor instance
  - Document
  - WriteCommandAction


#### Function 2: renderMarkdown
- **Description:** A function that renders markdown text to HTML.
- **Purpose:** To convert markdown-formatted text to displayable HTML.
- **Functionality:** Converts markdown to HTML
- **Location and Accessibility:** This function is imported from MarkdownUtil and used within the `renderResponse` method. It's already a standalone utility function.
- **Dependencies:** None visible in this file


#### Function 3: getComputerLanguage
- **Description:** A function that determines the programming language of the current file or selection.
- **Purpose:** To provide context about the code language to the AI model.
- **Functionality:** Extracts language information from the AnActionEvent
- **Location and Accessibility:** This is a method of the ComputerLanguage class. It could potentially be made more accessible as a standalone utility function.
- **Dependencies:** AnActionEvent


#### Function 4: openBrowserToSession
- **Description:** A function that opens a web browser to a specific session URL.
- **Purpose:** To provide a user interface for the diff chat functionality.
- **Functionality:** 
  - Constructs a session URL
  - Opens the default web browser to that URL
- **Location and Accessibility:** This functionality is currently embedded in a Thread within the `handle` method. It could be extracted into a separate utility function.
- **Dependencies:** 
  - java.awt.Desktop
  - AppServer

These functions represent common operations that could be useful across multiple components of the plugin. Extracting them into standalone utility functions would improve code reusability and maintainability.# generic\GenerateDocumentationAction.kt


## Shared Functionality Analysis: GenerateDocumentationAction.kt


### Code Overview
- **Language & Frameworks:** Kotlin, IntelliJ Platform SDK
- **Dependencies:** Apache Commons IO, JOpenAI, Swing, Java NIO


### Common Logic


#### Function 1: findGitRoot
- **Description:** Finds the root directory of a Git repository
- **Purpose:** To locate the base directory of a Git project
- **Functionality:** Traverses up the directory tree until it finds a .git folder
- **Location and Accessibility:** Already a public method in the companion object, easily accessible
- **Dependencies:** Java NIO (Path)


#### Function 2: open
- **Description:** Opens a file or directory in the IntelliJ IDE
- **Purpose:** To display the generated documentation to the user
- **Functionality:** Refreshes the file system and opens the file in the IDE editor
- **Location and Accessibility:** Already a public method in the companion object, easily accessible
- **Dependencies:** IntelliJ Platform SDK (ApplicationManager, LocalFileSystem, FileEditorManager)


#### Function 3: transformContent
- **Description:** Transforms file content using AI-generated documentation
- **Purpose:** To generate documentation for a given file
- **Functionality:** Sends file content to an AI model and receives transformed documentation
- **Location and Accessibility:** Private method, needs refactoring to be more general and accessible
- **Dependencies:** JOpenAI, AppSettingsState


#### Function 4: getProjectStructure
- **Description:** Retrieves the structure of the project
- **Purpose:** To provide context for AI documentation generation
- **Functionality:** Not implemented in this file, but referenced. Likely returns a string representation of the project structure
- **Location and Accessibility:** Referenced from TestResultAutofixAction.Companion, may need to be made more accessible
- **Dependencies:** Unknown, likely file system operations


#### Function 5: processSelection
- **Description:** Processes selected files to generate documentation
- **Purpose:** Main logic for documentation generation
- **Functionality:** Handles file selection, content transformation, and output generation
- **Location and Accessibility:** Part of the GenerateDocumentationAction class, may need refactoring for general use
- **Dependencies:** Java NIO, ExecutorService, AppSettingsState

These functions provide core functionality that could be useful across multiple components, especially in plugins dealing with file operations, Git repositories, and AI-assisted documentation generation. Some refactoring may be needed to make them more general and accessible, particularly for `transformContent` and `processSelection`. The `getProjectStructure` function, while not implemented in this file, seems to be a good candidate for shared functionality and might benefit from being made more accessible.# generic\GenericChatAction.kt


## Shared Functionality Analysis: GenericChatAction.kt


### Code Overview
- **Language & Frameworks:** Kotlin, IntelliJ Platform SDK
- **Dependencies:** 
  - AppServer
  - BaseAction
  - AppSettingsState
  - ApplicationServices
  - StorageInterface
  - ApplicationServer
  - ChatSocketManager
  - SLF4J (for logging)


### Common Logic


#### Function 1: getServer
- **Description:** Retrieves or creates an AppServer instance for a given project
- **Purpose:** To provide a centralized way to access the application server
- **Functionality:** Returns an AppServer instance associated with the project
- **Location and Accessibility:** Currently accessed via `AppServer.getServer(e.project)`. Could be refactored into a separate utility class for broader accessibility.
- **Dependencies:** AppServer, Project


#### Function 2: openBrowserToUri
- **Description:** Opens the default web browser to a specified URI
- **Purpose:** To provide a convenient way to open web interfaces for the plugin
- **Functionality:** Uses Desktop.getDesktop().browse() to open the default browser
- **Location and Accessibility:** Currently embedded in the handle method. Should be extracted into a utility class.
- **Dependencies:** java.awt.Desktop


#### Function 3: createChatSocketManager
- **Description:** Creates a new ChatSocketManager instance
- **Purpose:** To set up a chat session with specific parameters
- **Functionality:** Initializes a ChatSocketManager with various configuration options
- **Location and Accessibility:** Currently embedded in the handle method. Could be extracted and parameterized for reuse.
- **Dependencies:** ChatSocketManager, AppSettingsState, ApplicationServices


#### Function 4: getActionUpdateThread
- **Description:** Specifies the thread on which the action should be updated
- **Purpose:** To ensure proper threading for action updates
- **Functionality:** Returns ActionUpdateThread.BGT
- **Location and Accessibility:** Already a separate method, but could be moved to a base class if used across multiple actions
- **Dependencies:** ActionUpdateThread


#### Function 5: isEnabled
- **Description:** Determines whether the action should be enabled
- **Purpose:** To control when the action can be performed
- **Functionality:** Currently always returns true
- **Location and Accessibility:** Already a separate method, but could be moved to a base class if used across multiple actions
- **Dependencies:** AnActionEvent

These functions represent common patterns and functionality that could be useful across multiple components of the plugin. Refactoring them into separate utility classes or a base action class could improve code reusability and maintainability.# generic\GenerateRelatedFileAction.kt


## Shared Functionality Analysis: GenerateRelatedFileAction.kt


### Code Overview
- **Language & Frameworks:** Kotlin, IntelliJ Platform SDK
- **Dependencies:** Apache Commons IO, JOpenAI


### Common Logic


#### Function 1: open
- **Description:** Opens a file in the IntelliJ IDEA editor
- **Purpose:** To open a newly generated file in the IDE
- **Functionality:** 
  - Checks if the file exists
  - Refreshes the file system
  - Opens the file in the editor
  - Retries if the file is not immediately available
- **Location and Accessibility:** Currently a companion object function in GenerateRelatedFileAction. Could be refactored into a utility class for broader use.
- **Dependencies:** IntelliJ Platform SDK (ApplicationManager, FileEditorManager, LocalFileSystem)


#### Function 2: generateFile
- **Description:** Generates a new file based on an existing file and a directive
- **Purpose:** To create a related file (e.g., test cases) based on an existing file
- **Functionality:**
  - Uses OpenAI API to generate new file content
  - Parses the API response to extract file path and content
- **Location and Accessibility:** Private method in GenerateRelatedFileAction. Could be refactored into a separate service class for reuse.
- **Dependencies:** AppSettingsState, JOpenAI


#### Function 3: getModuleRootForFile
- **Description:** Retrieves the module root for a given file
- **Purpose:** To determine the base directory for file operations
- **Functionality:** Not visible in the provided code snippet, but likely returns the root directory of the module containing the file
- **Location and Accessibility:** Likely a utility method, could be moved to a shared utility class
- **Dependencies:** IntelliJ Platform SDK


#### Function 4: processSelection
- **Description:** Processes the selected file to generate a related file
- **Purpose:** Main logic for the action, coordinates file generation and saving
- **Functionality:**
  - Gets the module root
  - Generates the new file content
  - Determines the output path
  - Writes the new file
  - Opens the new file in the editor
- **Location and Accessibility:** Override method in GenerateRelatedFileAction. Core logic could be extracted into a service class.
- **Dependencies:** Apache Commons IO, custom utility methods

These functions represent core functionality that could be useful across multiple components of the plugin. Refactoring them into separate utility or service classes would improve reusability and maintainability of the codebase.# generic\MultiCodeChatAction.kt


## Shared Functionality Analysis: MultiCodeChatAction.kt


### Code Overview
- **Language & Frameworks:** Kotlin, IntelliJ Platform SDK
- **Dependencies:** 
  - AppServer
  - BaseAction
  - AppSettingsState
  - UITools
  - Various Simiacryptus libraries (jopenai, skyenet)


### Common Logic


#### Function 1: getFiles
- **Description:** Recursively collects file paths from given virtual files.
- **Purpose:** To gather all relevant files for code analysis.
- **Functionality:** 
  - Traverses directory structure
  - Collects file paths relative to a root directory
- **Location and Accessibility:** Already a private method in MultiCodeChatAction class. Could be extracted and made public static for wider use.
- **Dependencies:** VirtualFile, Path


#### Function 2: codeSummary
- **Description:** Generates a markdown-formatted summary of code files.
- **Purpose:** To provide a concise overview of multiple code files.
- **Functionality:**
  - Reads content of multiple files
  - Formats file content with syntax highlighting
- **Location and Accessibility:** Currently a local function in handle method. Should be extracted and made more general for reuse.
- **Dependencies:** Path, File


#### Function 3: openBrowserToSession
- **Description:** Opens a web browser to a specific session URL.
- **Purpose:** To provide user access to the chat interface.
- **Functionality:**
  - Constructs session URL
  - Opens default web browser
- **Location and Accessibility:** Currently embedded in a Thread within handle method. Could be extracted as a utility function.
- **Dependencies:** Desktop, URI


#### Function 4: renderFileList
- **Description:** Renders a markdown-formatted list of files with token counts.
- **Purpose:** To provide an overview of files being analyzed.
- **Functionality:**
  - Estimates token count for each file
  - Formats file paths and token counts as markdown list
- **Location and Accessibility:** Currently embedded in userMessage method of PatchApp inner class. Could be extracted as a utility function.
- **Dependencies:** GPT4Tokenizer, Path, File

These functions represent common operations that could be useful across multiple components of the plugin. Extracting and generalizing these functions could improve code reusability and maintainability. They could be placed in a utility class for easy access from other parts of the application.# generic\MassPatchAction.kt

Here's an analysis of shared functionality for the given MassPatchAction.kt file:


## Shared Functionality Analysis: MassPatchAction.kt


### Code Overview
- **Language & Frameworks:** Kotlin, IntelliJ Platform SDK
- **Dependencies:** 
  - com.github.simiacryptus.aicoder.*
  - com.intellij.openapi.*
  - com.simiacryptus.jopenai.*
  - com.simiacryptus.skyenet.*
  - java.awt.*
  - java.nio.file.*
  - javax.swing.*


### Common Logic


#### Function 1: getConfig
- **Description:** Creates a configuration dialog and returns user settings
- **Purpose:** To gather user input for the mass patch operation
- **Functionality:** 
  - Displays a dialog with file selection and AI instruction input
  - Returns a Settings object with user choices
- **Location and Accessibility:** Currently part of MassPatchAction class. Could be extracted to a utility class.
- **Dependencies:** Project, AnActionEvent, ConfigDialog


#### Function 2: handle
- **Description:** Handles the action event and initiates the mass patch process
- **Purpose:** To start the mass patch operation based on user configuration
- **Functionality:**
  - Gets configuration from user
  - Prepares code summary
  - Initializes and starts a MassPatchServer
  - Opens a browser to display results
- **Location and Accessibility:** Part of MassPatchAction class. Core logic could be extracted.
- **Dependencies:** AnActionEvent, AppServer, MassPatchServer


#### Function 3: newSession (in MassPatchServer)
- **Description:** Creates a new session for processing files
- **Purpose:** To handle the processing of multiple files in parallel
- **Functionality:**
  - Creates a tabbed display for results
  - Processes each file using a Discussable object
  - Applies patches and updates UI
- **Location and Accessibility:** Part of MassPatchServer class. Could be generalized for other multi-file operations.
- **Dependencies:** User, Session, ApplicationSocketManager


#### Function 4: addApplyFileDiffLinks (referenced, not defined in this file)
- **Description:** Adds links to apply file diffs in the UI
- **Purpose:** To allow easy application of generated patches
- **Functionality:** Modifies markdown to include clickable links for applying diffs
- **Location and Accessibility:** Likely in a utility class. Could be made more accessible.
- **Dependencies:** Path, String, ApplicationInterface


#### Function 5: addSaveLinks (referenced, not defined in this file)
- **Description:** Adds links to save changes in the UI
- **Purpose:** To allow easy saving of applied changes
- **Functionality:** Modifies markdown to include clickable links for saving changes
- **Location and Accessibility:** Likely in a utility class. Could be made more accessible.
- **Dependencies:** Path, String, ApplicationInterface

These functions represent core functionality that could be useful across multiple components of the plugin. Some refactoring might be beneficial to make these functions more accessible and reusable, possibly by moving them to utility classes or creating a shared service for file operations and UI updates.# generic\MultiDiffChatAction.kt

Here's an analysis of shared functionality for the given MultiDiffChatAction.kt file:


## Shared Functionality Analysis: MultiDiffChatAction.kt


### Code Overview
- **Language & Frameworks:** Kotlin, IntelliJ Platform SDK
- **Dependencies:** AppServer, BaseAction, UITools, Jopenai, Skyenet


### Common Logic


#### Function 1: getFiles
- **Description:** Recursively collects file paths from given virtual files
- **Purpose:** To gather all relevant files for code summarization and diff generation
- **Functionality:** 
  - Traverses directory structure
  - Collects file paths relative to a root directory
- **Location and Accessibility:** Currently a private method in MultiDiffChatAction class. Could be refactored into a utility class for broader use.
- **Dependencies:** VirtualFile from IntelliJ Platform SDK, java.nio.file.Path


#### Function 2: codeSummary
- **Description:** Generates a markdown-formatted summary of code files
- **Purpose:** To provide a concise overview of multiple code files for AI processing
- **Functionality:**
  - Reads content of multiple files
  - Formats file content with syntax highlighting in markdown
- **Location and Accessibility:** Currently a local function in handle method. Could be extracted and generalized for reuse.
- **Dependencies:** java.nio.file.Path, java.io.File


#### Function 3: renderMarkdown
- **Description:** Renders markdown content to HTML
- **Purpose:** To display formatted text in the UI
- **Functionality:** Converts markdown syntax to HTML
- **Location and Accessibility:** Imported from MarkdownUtil. Already shared functionality.
- **Dependencies:** MarkdownUtil from Skyenet


#### Function 4: addApplyFileDiffLinks
- **Description:** Adds interactive links to apply file diffs
- **Purpose:** To allow users to apply code changes directly from the chat interface
- **Functionality:**
  - Parses diff content
  - Generates interactive links
  - Handles file updates when links are clicked
- **Location and Accessibility:** Extension function on SocketManager. Could be generalized further if needed.
- **Dependencies:** Skyenet, java.nio.file.Path


#### Function 5: addSaveLinks
- **Description:** Adds save links to the response
- **Purpose:** To allow users to save changes to files
- **Functionality:** Generates interactive save links for file changes
- **Location and Accessibility:** Extension function on SocketManager. Could be generalized further if needed.
- **Dependencies:** Skyenet, java.nio.file.Path

These functions represent core functionality that could be useful across multiple components of the plugin. Some refactoring might be beneficial to make them more accessible and reusable, particularly getFiles and codeSummary. The other functions are already somewhat shared but could potentially be generalized further if needed in other parts of the application.# generic\MultiStepPatchAction.kt


## Shared Functionality Analysis: MultiStepPatchAction.kt


### Code Overview
- **Language & Frameworks:** Kotlin, IntelliJ Platform SDK
- **Dependencies:** 
  - IntelliJ Platform SDK
  - Simiacryptus libraries (jopenai, skyenet)
  - Java AWT (Desktop)
  - SLF4J (LoggerFactory)


### Common Logic


#### Function 1: getSelectedFolder
- **Description:** Retrieves the selected folder from an AnActionEvent
- **Purpose:** To get the context of where the action is being performed
- **Functionality:** Extracts the selected virtual file from the event's data context
- **Location and Accessibility:** Currently part of UITools, could be made more accessible
- **Dependencies:** IntelliJ Platform SDK (AnActionEvent, VirtualFile)


#### Function 2: renderMarkdown
- **Description:** Renders markdown text
- **Purpose:** To display formatted text in the UI
- **Functionality:** Converts markdown to HTML or another displayable format
- **Location and Accessibility:** Part of MarkdownUtil, seems to be accessible
- **Dependencies:** ApplicationInterface (for UI rendering)


#### Function 3: addApplyFileDiffLinks
- **Description:** Adds links to apply file diffs
- **Purpose:** To allow easy application of code changes
- **Functionality:** Processes diff output and adds interactive links
- **Location and Accessibility:** Part of a socket manager, might need refactoring for general use
- **Dependencies:** File system operations, UI interaction


#### Function 4: toContentList
- **Description:** Converts a string to a list of API content
- **Purpose:** To prepare text for API communication
- **Functionality:** Transforms string input into a format suitable for API requests
- **Location and Accessibility:** Part of ClientUtil, seems accessible
- **Dependencies:** ApiModel


#### Function 5: toJson
- **Description:** Converts an object to JSON string
- **Purpose:** For data serialization
- **Functionality:** Serializes objects to JSON format
- **Location and Accessibility:** Part of JsonUtil, accessible
- **Dependencies:** JSON library (not specified in the code)


#### Function 6: displayMapInTabs
- **Description:** Displays a map of content in tabs
- **Purpose:** To organize and present multiple pieces of information
- **Functionality:** Creates a tabbed interface from a map of content
- **Location and Accessibility:** Part of AgentPatterns, might need refactoring for general use
- **Dependencies:** UI framework (not specified in the code)

These functions represent common operations that could be useful across different components of the plugin. Some, like `getSelectedFolder` and `renderMarkdown`, are already in utility classes and could be easily reused. Others, like `addApplyFileDiffLinks` and `displayMapInTabs`, might need to be extracted and refactored to be more generally applicable. The JSON and content list conversion functions are likely already in utility classes and ready for reuse.# generic\ReactTypescriptWebDevelopmentAssistantAction.kt


## Shared Functionality Analysis: ReactTypescriptWebDevelopmentAssistantAction.kt


### Code Overview
- **Language & Frameworks:** Kotlin, IntelliJ IDEA Plugin Development, React, TypeScript
- **Dependencies:** 
  - IntelliJ Platform SDK
  - Skyenet (custom library for AI-assisted development)
  - JOpenAI (Java client for OpenAI API)
  - SLF4J (logging)


### Common Logic

Several logical components in this file could be useful across multiple components of the plugin or even in other similar AI-assisted development tools.


#### Function 1: extractCode
- **Description:** Extracts code from a string that may contain markdown-style code blocks.
- **Purpose:** To clean up AI-generated code responses by removing markdown formatting.
- **Functionality:** 
  - Trims the input string
  - Uses regex to find and extract code within triple backticks
- **Location and Accessibility:** Currently a private method in WebDevAgent class. Should be refactored into a utility class.
- **Dependencies:** None


#### Function 2: draftResourceCode
- **Description:** Generates and saves code for a specific file in the project.
- **Purpose:** To create initial drafts of various file types (HTML, CSS, TypeScript, etc.) based on AI suggestions.
- **Functionality:**
  - Uses AI to generate code based on project requirements
  - Allows for iterative refinement through user interaction
  - Saves the generated code to the appropriate file
- **Location and Accessibility:** Private method in WebDevAgent class. Could be generalized and moved to a separate service class.
- **Dependencies:** SessionTask, SimpleActor, API, ApplicationInterface


#### Function 3: draftImage
- **Description:** Generates and saves image files for the project.
- **Purpose:** To create images based on AI suggestions and project requirements.
- **Functionality:**
  - Uses AI to generate image descriptions
  - Converts descriptions to actual images (likely using DALL-E or similar)
  - Saves the generated images to the appropriate file
- **Location and Accessibility:** Private method in WebDevAgent class. Could be generalized and moved to a separate service class.
- **Dependencies:** SessionTask, ImageActor, API, ApplicationInterface


#### Function 4: codeSummary
- **Description:** Generates a summary of all code files in the project.
- **Purpose:** To provide an overview of the project's codebase for AI analysis or user review.
- **Functionality:**
  - Collects all non-image files in the project
  - Formats their content into a markdown-style summary
- **Location and Accessibility:** Method in WebDevAgent class. Could be moved to a utility class for broader use.
- **Dependencies:** None


#### Function 5: iterateCode
- **Description:** Applies code review and refinement to the entire project.
- **Purpose:** To improve and maintain code quality through AI-assisted review.
- **Functionality:**
  - Summarizes the current codebase
  - Uses AI to suggest improvements
  - Applies approved changes to the files
- **Location and Accessibility:** Private method in WebDevAgent class. Could be generalized into a CodeReviewService.
- **Dependencies:** SessionTask, SimpleActor (codeReviewer), API, ApplicationInterface


#### Function 6: start
- **Description:** Initiates the web development assistant process.
- **Purpose:** To orchestrate the entire process of generating a web application based on user input.
- **Functionality:**
  - Generates project architecture
  - Creates individual files (HTML, CSS, TypeScript, images)
  - Applies code review and refinement
- **Location and Accessibility:** Method in WebDevAgent class. Core logic could be extracted into a separate service.
- **Dependencies:** Various actors (ArchitectureDiscussionActor, HtmlCodingActor, etc.), API, ApplicationInterface

These functions represent core functionalities that could be useful in various AI-assisted development scenarios. Refactoring them into more generalized, standalone services or utility classes would improve code reusability and maintainability across the plugin and potentially in other similar tools.# generic\PlanAheadAction.kt


## Shared Functionality Analysis: PlanAheadAction.kt


### Code Overview
- **Language & Frameworks:** Kotlin, IntelliJ IDEA Plugin Development
- **Dependencies:** 
  - IntelliJ Platform SDK
  - com.github.simiacryptus.aicoder
  - com.simiacryptus.jopenai
  - com.simiacryptus.skyenet


### Common Logic

The file contains several functions and classes that could be useful across multiple components. Here's an analysis of the potentially reusable logic:


#### Function 1: expandFileList
- **Description:** Expands a list of VirtualFile objects, filtering out certain file types and directories.
- **Purpose:** To create a filtered list of files for processing, excluding unwanted file types and directories.
- **Functionality:** 
  - Recursively expands directories
  - Filters out hidden files, .gitignore files, large files, and certain file extensions
  - Returns an array of VirtualFile objects
- **Location and Accessibility:** This function is already defined as a companion object method in the PlanAheadAgent class. It could be extracted to a utility class for broader use.
- **Dependencies:** com.intellij.openapi.vfs.VirtualFile


#### Function 2: executionOrder
- **Description:** Determines the execution order of tasks based on their dependencies.
- **Purpose:** To create a topologically sorted list of tasks for execution.
- **Functionality:** 
  - Takes a map of tasks and their dependencies
  - Returns a list of task IDs in the order they should be executed
  - Detects circular dependencies
- **Location and Accessibility:** This function is already defined as a companion object method in the PlanAheadAgent class. It could be extracted to a utility class for broader use.
- **Dependencies:** None (uses standard Kotlin collections)


#### Function 3: buildMermaidGraph
- **Description:** Generates a Mermaid graph representation of the task dependencies.
- **Purpose:** To visualize the task dependencies in a Mermaid graph format.
- **Functionality:** 
  - Takes a map of tasks
  - Generates a Mermaid graph string representation
  - Includes task descriptions and dependencies
  - Applies different styles based on task types
- **Location and Accessibility:** This is a private method in the PlanAheadAgent class. It could be extracted and made public in a utility class for broader use.
- **Dependencies:** None (uses string manipulation)


#### Function 4: sanitizeForMermaid
- **Description:** Sanitizes strings for use in Mermaid graphs.
- **Purpose:** To ensure that strings are properly formatted for Mermaid graphs.
- **Functionality:** 
  - Replaces spaces with underscores
  - Escapes special characters
- **Location and Accessibility:** This is a private method in the PlanAheadAgent class. It could be extracted and made public in a utility class for broader use.
- **Dependencies:** None (uses string manipulation)


#### Function 5: escapeMermaidCharacters
- **Description:** Escapes special characters for Mermaid graph labels.
- **Purpose:** To ensure that strings are properly formatted for Mermaid graph labels.
- **Functionality:** 
  - Escapes double quotes
  - Wraps the string in double quotes
- **Location and Accessibility:** This is a private method in the PlanAheadAgent class. It could be extracted and made public in a utility class for broader use.
- **Dependencies:** None (uses string manipulation)


#### Class: PlanAheadSettings
- **Description:** Data class for storing settings for the PlanAheadAction.
- **Purpose:** To encapsulate configuration options for the PlanAheadAction.
- **Functionality:** 
  - Stores model, temperature, and feature flags
- **Location and Accessibility:** This class is already defined within PlanAheadAction and could be extracted to a separate file for broader use.
- **Dependencies:** None


#### Class: PlanAheadConfigDialog
- **Description:** Dialog for configuring PlanAheadAction settings.
- **Purpose:** To provide a user interface for modifying PlanAheadAction settings.
- **Functionality:** 
  - Creates a dialog with input fields for various settings
  - Handles user input and updates the PlanAheadSettings object
- **Location and Accessibility:** This class is already defined within PlanAheadAction and could be extracted to a separate file for broader use.
- **Dependencies:** 
  - javax.swing
  - com.intellij.openapi.ui.DialogWrapper

These functions and classes provide general utility for task management, graph generation, and configuration handling. They could be refactored into separate utility classes to make them more accessible and reusable across different parts of the plugin or even in other projects.# generic\ShellCommandAction.kt


## Shared Functionality Analysis: ShellCommandAction.kt


### Code Overview
- **Language & Frameworks:** Kotlin, IntelliJ Platform SDK
- **Dependencies:** 
  - IntelliJ Platform SDK
  - Skyenet library
  - JOpenAI library
  - Custom AppServer and AppSettingsState classes


### Common Logic


#### Function 1: getSelectedFolder
- **Description:** Retrieves the selected folder from the action event
- **Purpose:** To get the directory where the shell command will be executed
- **Functionality:** Uses UITools to get the selected folder from the AnActionEvent
- **Location and Accessibility:** Currently part of the handle method, could be extracted to a separate function
- **Dependencies:** UITools class


#### Function 2: createSessionProxyServer
- **Description:** Creates a new ApplicationServer for handling shell commands
- **Purpose:** To set up a server that can execute shell commands and display results
- **Functionality:** 
  - Creates a new ApplicationServer with custom settings
  - Implements userMessage method to handle user input and execute shell commands
- **Location and Accessibility:** Currently part of the handle method, could be extracted to a separate function
- **Dependencies:** ApplicationServer, CodingAgent, ProcessInterpreter


#### Function 3: openBrowserToSession
- **Description:** Opens the default browser to the session URL
- **Purpose:** To provide a user interface for interacting with the shell command executor
- **Functionality:** 
  - Gets the server URI
  - Constructs the session URL
  - Opens the default browser to that URL
- **Location and Accessibility:** Currently an anonymous thread in the handle method, could be extracted to a separate function
- **Dependencies:** AppServer, Desktop class


#### Function 4: isWindows
- **Description:** Checks if the operating system is Windows
- **Purpose:** To determine which shell command to use (PowerShell or Bash)
- **Functionality:** Checks if the OS name contains "windows" (case-insensitive)
- **Location and Accessibility:** Currently a companion object property, could be moved to a utility class
- **Dependencies:** None

These functions represent common logic that could be useful across multiple components. Extracting them into separate, more general functions would improve code reusability and maintainability. For example, the getSelectedFolder function could be useful in other actions that need to operate on a selected directory. The createSessionProxyServer and openBrowserToSession functions could be generalized to work with different types of ApplicationServers, not just for shell commands.# generic\SessionProxyApp.kt


## Shared Functionality Analysis: SessionProxyApp.kt


### Code Overview
- **Language & Frameworks:** Kotlin
- **Dependencies:** 
  - com.simiacryptus.skyenet.core.platform
  - com.simiacryptus.skyenet.webui.application
  - com.simiacryptus.skyenet.webui.chat
  - com.simiacryptus.skyenet.webui.session


### Common Logic

The `SessionProxyServer` class extends `ApplicationServer` and provides a proxy for managing sessions. While there are no explicit public static functions in this file, there are some components that could be potentially useful across multiple components:


#### Companion Object
- **Description:** A companion object containing shared mutable maps and a logger.
- **Purpose:** To store and manage shared state across instances of `SessionProxyServer`.
- **Functionality:** 
  - Stores `agents` and `chats` as mutable maps
  - Provides a logger for the class
- **Location and Accessibility:** Already accessible as part of the companion object
- **Dependencies:** None


#### newSession Function
- **Description:** An overridden function that creates a new session.
- **Purpose:** To create and return a new session based on the user and session information.
- **Functionality:** 
  - Checks if a chat exists for the session and creates a new session using it
  - If no chat exists, it returns the agent associated with the session
- **Location and Accessibility:** Currently part of the `SessionProxyServer` class. Could potentially be extracted and made more generic.
- **Dependencies:** Relies on the `chats` and `agents` maps from the companion object

While there are no standalone functions that can be immediately shared, the overall structure and approach of this class could be useful in other parts of the application that need to manage sessions or provide a proxy for different types of servers (chat or agent-based). 

To make the functionality more shareable:

1. The `newSession` function could be extracted and generalized to accept the maps as parameters, making it usable in other contexts.

2. The concept of maintaining separate maps for different types of session handlers (agents and chats) could be abstracted into a more generic session management utility.

3. The approach of extending `ApplicationServer` and customizing its behavior could be documented as a pattern for creating specialized server types in the application.

These abstractions would need to be carefully designed to maintain the current functionality while providing flexibility for other use cases within the application.# git\ChatWithCommitDiffAction.kt


## Shared Functionality Analysis: ChatWithCommitDiffAction.kt


### Code Overview
- **Language & Frameworks:** Kotlin, IntelliJ Platform SDK
- **Dependencies:** Git4Idea, OpenAI API, SkyeNet, Swing


### Common Logic


#### Function 1: getChangesBetweenCommits
- **Description:** Retrieves the diff between a selected commit and the current HEAD.
- **Purpose:** To obtain the changes made between two Git commits.
- **Functionality:** 
  - Takes a GitRepository and a VcsRevisionNumber as input.
  - Uses Git command to get the diff between the selected commit and HEAD.
  - Returns the diff as a string.
- **Location and Accessibility:** Currently a private method in ChatWithCommitDiffAction. Could be refactored into a utility class for broader use.
- **Dependencies:** Git4Idea


#### Function 2: openChatWithDiff
- **Description:** Opens a chat interface with the commit diff information.
- **Purpose:** To provide a user interface for discussing commit changes.
- **Functionality:**
  - Creates a new session with the diff information.
  - Sets up a CodeChatSocketManager with the diff details.
  - Configures the ApplicationServer with session information.
  - Opens a browser window to the chat interface.
- **Location and Accessibility:** Currently a private method in ChatWithCommitDiffAction. Could be generalized and moved to a utility class for reuse with different types of diffs or code snippets.
- **Dependencies:** AppServer, SessionProxyServer, CodeChatSocketManager, ApplicationServer


#### Function 3: IdeaOpenAIClient.instance
- **Description:** Singleton instance of IdeaOpenAIClient.
- **Purpose:** To provide a consistent interface for OpenAI API calls within the IDE.
- **Functionality:** Not visible in this file, but likely provides methods for interacting with OpenAI's API.
- **Location and Accessibility:** Already accessible as a singleton, could be used in other parts of the plugin.
- **Dependencies:** OpenAI API


#### Function 4: AppSettingsState.instance
- **Description:** Singleton instance of AppSettingsState.
- **Purpose:** To provide access to application settings.
- **Functionality:** Stores and retrieves plugin configuration settings.
- **Location and Accessibility:** Already accessible as a singleton, used throughout the plugin.
- **Dependencies:** None specific to this file

These functions and components represent shared functionality that could be useful across multiple parts of the plugin. Refactoring them into utility classes or services could improve code organization and reusability.# generic\SimpleCommandAction.kt


## Shared Functionality Analysis: SimpleCommandAction.kt


### Code Overview
- **Language & Frameworks:** Kotlin, IntelliJ Platform SDK
- **Dependencies:** 
  - com.github.simiacryptus.aicoder
  - com.intellij.openapi.actionSystem
  - com.simiacryptus.jopenai
  - com.simiacryptus.skyenet
  - java.awt.Desktop
  - java.io.File
  - java.nio.file


### Common Logic


#### Function 1: getFiles
- **Description:** Recursively collects file paths from given virtual files
- **Purpose:** To gather a set of file paths for processing
- **Functionality:** 
  - Recursively traverses directories
  - Filters out certain files and directories based on conditions
  - Returns a set of Path objects
- **Location and Accessibility:** Already a companion object function, accessible as is
- **Dependencies:** VirtualFile from IntelliJ Platform SDK


#### Function 2: toPaths
- **Description:** Converts a string path (potentially with wildcards) to a list of actual file paths
- **Purpose:** To expand wildcard paths and convert string paths to Path objects
- **Functionality:**
  - Handles wildcard expansion
  - Converts string paths to Path objects
- **Location and Accessibility:** Already a companion object function, accessible as is
- **Dependencies:** java.nio.file.Path


#### Function 3: getUserSettings
- **Description:** Retrieves user settings based on the current action event
- **Purpose:** To gather necessary settings for the action execution
- **Functionality:**
  - Determines the working directory
  - Collects selected files or all files in the working directory
- **Location and Accessibility:** Private function, could be made public and static for broader use
- **Dependencies:** AnActionEvent from IntelliJ Platform SDK, UITools from the project


#### Function 4: codeSummary
- **Description:** Generates a summary of code files
- **Purpose:** To create a textual representation of multiple code files
- **Functionality:**
  - Reads content of multiple files
  - Formats the content with file paths and language indicators
- **Location and Accessibility:** Part of PatchApp class, could be extracted and made more general
- **Dependencies:** java.nio.file.Path, java.io.File


#### Function 5: projectSummary
- **Description:** Generates a summary of the project structure
- **Purpose:** To create a list of files in the project with their sizes
- **Functionality:**
  - Lists all code files in the project
  - Includes file paths and sizes
- **Location and Accessibility:** Part of PatchApp class, could be extracted and made more general
- **Dependencies:** java.nio.file.Path, java.io.File

These functions provide useful utilities for file handling, project structure analysis, and settings management. They could be extracted into a separate utility class to make them more accessible and reusable across different parts of the project.# git\ChatWithCommitAction.kt


## Shared Functionality Analysis: ChatWithCommitAction.kt


### Code Overview
- **Language & Frameworks:** Kotlin, IntelliJ IDEA Plugin Development
- **Dependencies:** 
  - IntelliJ Platform API
  - AppServer
  - SessionProxyServer
  - AppSettingsState
  - CodeChatSocketManager
  - IdeaOpenAIClient
  - DiffUtil
  - ApplicationServices
  - StorageInterface
  - ApplicationServer


### Common Logic


#### Function 1: isBinary (String extension property)
- **Description:** Determines if a string represents binary content
- **Purpose:** To identify if the content of a file is binary or text
- **Functionality:** 
  - Counts the number of non-printable ASCII characters
  - Returns true if more than 10% of characters are non-printable
- **Location and Accessibility:** Already accessible as an extension property on String
- **Dependencies:** None


#### Function 2: expand
- **Description:** Recursively expands directories into a list of files
- **Purpose:** To process all files in a directory structure
- **Functionality:**
  - Takes an array of VirtualFile objects
  - If a file is a directory, it recursively calls itself on the children
  - If a file is not a directory, it adds it to the result list
- **Location and Accessibility:** Private method in ChatWithCommitAction, could be refactored to be more general
- **Dependencies:** IntelliJ's VirtualFile API


#### Function 3: openChatWithDiff
- **Description:** Opens a chat session with diff information
- **Purpose:** To initiate a chat session based on code changes
- **Functionality:**
  - Creates a new session ID
  - Sets up a CodeChatSocketManager with the diff information
  - Configures the ApplicationServer with session details
  - Opens a browser to the chat session URL
- **Location and Accessibility:** Private method in ChatWithCommitAction, could be refactored to be more general
- **Dependencies:** 
  - SessionProxyServer
  - CodeChatSocketManager
  - IdeaOpenAIClient
  - AppSettingsState
  - ApplicationServices
  - ApplicationServer
  - AppServer


#### Function 4: formatDiffInfo
- **Description:** Formats diff information for multiple files
- **Purpose:** To create a human-readable representation of code changes
- **Functionality:**
  - Processes a list of file changes
  - Formats each change as a diff, including file name and change type (added, deleted, modified)
  - Handles binary files differently
- **Location and Accessibility:** Not explicitly defined, but the logic is present in the actionPerformed method. Could be extracted into a separate function for reuse.
- **Dependencies:** 
  - DiffUtil
  - VcsDataKeys

These functions represent common logic that could be useful across multiple components of the plugin, especially for features related to version control integration and code diff visualization. Refactoring some of these into more general, public utility functions could improve code reusability and maintainability.# generic\WebDevelopmentAssistantAction.kt


## Shared Functionality Analysis: WebDevelopmentAssistantAction.kt


### Code Overview
- **Language & Frameworks:** Kotlin, IntelliJ IDEA Plugin Development
- **Dependencies:** 
  - IntelliJ Platform SDK
  - OpenAI API (com.simiacryptus.jopenai)
  - Skyenet (com.simiacryptus.skyenet)
  - Java AWT (for Desktop.getDesktop())


### Common Logic


#### Function 1: extractCode
- **Description:** Extracts code from a string that may contain markdown-style code blocks.
- **Purpose:** To clean up code responses from AI models, removing any surrounding markdown formatting.
- **Functionality:** 
  1. Trims the input string
  2. Uses a regex to find code blocks (```...```)
  3. If found, extracts the content within the code block
- **Location and Accessibility:** Currently a private method in WebDevAgent class. Should be refactored to a utility class for broader use.
- **Dependencies:** None, uses standard Kotlin string manipulation.


#### Function 2: draftResourceCode
- **Description:** Generates and saves code for a specific file type using an AI actor.
- **Purpose:** To create initial drafts of various file types (HTML, CSS, JavaScript, etc.) based on project specifications.
- **Functionality:**
  1. Uses a Discussable object to interact with an AI actor
  2. Generates code based on the file path and project context
  3. Extracts the generated code from the AI response
  4. Saves the code to a file and updates the UI
- **Location and Accessibility:** Currently a private method in WebDevAgent class. Could be generalized and moved to a utility class for broader use.
- **Dependencies:** SessionTask, SimpleActor, API, ApplicationInterface


#### Function 3: codeSummary
- **Description:** Generates a summary of all code files in the project, excluding image files.
- **Purpose:** To create a comprehensive overview of the project's code for review or further processing.
- **Functionality:**
  1. Filters out image files from the list of project files
  2. Reads the content of each code file
  3. Formats the content with file paths and appropriate code block markers
- **Location and Accessibility:** Currently a method in WebDevAgent class. Could be refactored into a utility class for project-wide use.
- **Dependencies:** Access to project file structure and file reading capabilities.


#### Function 4: iterateCode
- **Description:** Initiates a code review and refinement process using an AI actor.
- **Purpose:** To improve and refine the generated code through iterative AI feedback.
- **Functionality:**
  1. Uses a Discussable object to interact with a code review AI actor
  2. Presents the current code summary for review
  3. Applies suggested changes to the project files
  4. Updates the UI with the changes
- **Location and Accessibility:** Currently a private method in WebDevAgent class. Could be generalized for use in other code review scenarios.
- **Dependencies:** SessionTask, SimpleActor, API, ApplicationInterface, codeSummary function

These functions represent core functionalities that could be useful across multiple components of the plugin or even in other similar projects. Refactoring them into a utility class or a shared service would improve code reusability and maintainability.# git\ChatWithWorkingCopyDiffAction.kt


## Shared Functionality Analysis: ChatWithWorkingCopyDiffAction.kt


### Code Overview
- **Language & Frameworks:** Kotlin, IntelliJ Platform SDK
- **Dependencies:** Git4Idea, OpenAI API, SkyeNet, ApplicationServices


### Common Logic


#### Function 1: getChangesBetweenHeadAndWorkingCopy
- **Description:** Retrieves the diff between HEAD and the working copy for a given Git repository.
- **Purpose:** To obtain the changes made in the working copy that haven't been committed yet.
- **Functionality:** 
  - Uses Git.getInstance().diff() to run a git diff command
  - Returns the diff output as a string
- **Location and Accessibility:** Currently a private method in ChatWithWorkingCopyDiffAction. Could be refactored into a utility class for broader use.
- **Dependencies:** Git4Idea


#### Function 2: openChatWithDiff
- **Description:** Opens a chat interface with the diff information.
- **Purpose:** To allow users to interact with an AI about the changes in their working copy.
- **Functionality:**
  - Creates a new CodeChatSocketManager instance
  - Sets up session information
  - Opens a browser to the chat interface
- **Location and Accessibility:** Currently a private method in ChatWithWorkingCopyDiffAction. Could be generalized and moved to a utility class for reuse with different types of diffs or code snippets.
- **Dependencies:** AppServer, SessionProxyServer, CodeChatSocketManager, IdeaOpenAIClient, ApplicationServices


#### Function 3: setupSessionInfo
- **Description:** Sets up session information for the chat interface.
- **Purpose:** To configure the chat session with appropriate parameters.
- **Functionality:**
  - Creates a new global ID for the session
  - Sets up the CodeChatSocketManager with necessary parameters
  - Configures ApplicationServer session info
- **Location and Accessibility:** This functionality is currently embedded in the openChatWithDiff method. It could be extracted into a separate utility function for reuse in other chat-based actions.
- **Dependencies:** StorageInterface, SessionProxyServer, CodeChatSocketManager, ApplicationServer


#### Function 4: openBrowserToChat
- **Description:** Opens the default web browser to the chat interface URL.
- **Purpose:** To provide a convenient way for users to access the chat interface.
- **Functionality:**
  - Constructs the URL for the chat session
  - Uses Desktop.getDesktop().browse() to open the browser
- **Location and Accessibility:** This functionality is currently embedded in the openChatWithDiff method. It could be extracted into a separate utility function for reuse in other actions that need to open a browser.
- **Dependencies:** Desktop API

These functions represent common logic that could be useful across multiple components in the plugin. Refactoring them into separate utility classes would improve code reusability and maintainability.# legacy\AppendTextWithChatAction.kt


## Shared Functionality Analysis: AppendTextWithChatAction.kt


### Code Overview
- **Language & Frameworks:** Kotlin, IntelliJ Platform SDK
- **Dependencies:** 
  - com.github.simiacryptus.aicoder.actions.SelectionAction
  - com.github.simiacryptus.aicoder.config.AppSettingsState
  - com.intellij.openapi.actionSystem
  - com.simiacryptus.jopenai.ApiModel
  - com.simiacryptus.jopenai.util.ClientUtil


### Common Logic


#### Function 1: processSelection
- **Description:** Processes the selected text using a chat model and appends the result.
- **Purpose:** To generate and append text based on the user's selection.
- **Functionality:** 
  1. Creates a ChatRequest with system and user messages
  2. Sends the request to the API
  3. Appends the generated text to the original selection
- **Location and Accessibility:** Currently part of AppendTextWithChatAction class. Could be refactored into a utility class for broader use.
- **Dependencies:** AppSettingsState, OpenAI API client


#### Function 2: createChatRequest
- **Description:** Creates a ChatRequest object with predefined settings and messages.
- **Purpose:** To standardize the creation of chat requests across different actions.
- **Functionality:** 
  1. Sets up the model, temperature, and messages for the chat request
  2. Uses AppSettingsState for configuration
- **Location and Accessibility:** This functionality is currently embedded in processSelection. It could be extracted into a separate utility function.
- **Dependencies:** AppSettingsState, ApiModel.ChatRequest


#### Function 3: appendGeneratedText
- **Description:** Appends generated text to the original selection, avoiding duplication.
- **Purpose:** To ensure smooth integration of generated text with existing content.
- **Functionality:** 
  1. Checks if the generated text starts with the original selection
  2. Appends only the new part if there's overlap
- **Location and Accessibility:** This logic is currently part of processSelection. It could be extracted into a utility function for reuse in similar actions.
- **Dependencies:** None

These functions represent common logic that could be useful across multiple components in the project. Extracting them into a shared utility class would improve code reusability and maintainability.# git\ReplicateCommitAction.kt


## Shared Functionality Analysis: ReplicateCommitAction.kt


### Code Overview
- **Language & Frameworks:** Kotlin, IntelliJ Platform SDK
- **Dependencies:** 
  - IntelliJ Platform API
  - Simiacryptus libraries (jopenai, skyenet)
  - Java AWT (for Desktop functionality)
  - SLF4J (for logging)


### Common Logic


#### Function 1: generateDiffInfo
- **Description:** Generates a formatted diff information string from a list of changes.
- **Purpose:** To create a human-readable representation of code changes.
- **Functionality:** 
  - Filters changes based on the provided virtual files
  - Formats each change into a string representation
  - Handles binary files, added files, and deleted files differently
- **Location and Accessibility:** Currently a private method in ReplicateCommitAction class. Could be refactored into a utility class for broader use.
- **Dependencies:** IntelliJ VCS API, DiffUtil


#### Function 2: getFiles
- **Description:** Recursively collects all file paths from given virtual files.
- **Purpose:** To gather a complete list of files for processing.
- **Functionality:**
  - Recursively traverses directories
  - Filters out hidden directories and gitignore files
  - Collects file paths into a set
- **Location and Accessibility:** Private method in ReplicateCommitAction class. Could be moved to a utility class for reuse.
- **Dependencies:** IntelliJ VFS API


#### Function 3: expand
- **Description:** Expands an array of virtual files, including all files within directories.
- **Purpose:** To flatten a file structure for easier processing.
- **Functionality:**
  - Recursively expands directories into individual files
  - Returns a flattened array of virtual files
- **Location and Accessibility:** Private method in ReplicateCommitAction class. Could be refactored into a utility method.
- **Dependencies:** IntelliJ VFS API


#### Function 4: toPaths
- **Description:** Converts a string path (potentially with wildcards) to a list of actual file paths.
- **Purpose:** To resolve wildcard patterns in file paths.
- **Functionality:**
  - Handles wildcard expansion
  - Uses Java NIO for file walking
- **Location and Accessibility:** Currently a companion object function. Could be moved to a file utility class.
- **Dependencies:** Java NIO


#### Function 5: codeSummary
- **Description:** Generates a markdown-formatted summary of code files.
- **Purpose:** To create a human-readable overview of code content.
- **Functionality:**
  - Reads file content
  - Formats content into markdown code blocks
- **Location and Accessibility:** Abstract method in PatchApp inner class. Could be extracted and generalized.
- **Dependencies:** None specific


#### Function 6: projectSummary
- **Description:** Generates a summary of the project structure.
- **Purpose:** To provide an overview of files in the project.
- **Functionality:**
  - Lists files with their sizes
  - Filters and sorts the file list
- **Location and Accessibility:** Abstract method in PatchApp inner class. Could be extracted and generalized.
- **Dependencies:** None specific

These functions provide core functionality for file handling, diff generation, and project summarization. They could be refactored into utility classes to enhance reusability across different components of the plugin.# legacy\CommentsAction.kt


## Shared Functionality Analysis: CommentsAction.kt


### Code Overview
- **Language & Frameworks:** Kotlin, IntelliJ Platform SDK
- **Dependencies:** 
  - com.github.simiacryptus.aicoder.actions.SelectionAction
  - com.github.simiacryptus.aicoder.config.AppSettingsState
  - com.github.simiacryptus.aicoder.util.ComputerLanguage
  - com.intellij.openapi.actionSystem
  - com.simiacryptus.jopenai.proxy.ChatProxy


### Common Logic

The CommentsAction class doesn't contain any public static functions that could be directly shared across multiple components. However, there are some logical bits that could be extracted and refactored for more general use:


#### Function 1: Language Support Check
- **Description:** Check if a given computer language is supported for commenting
- **Purpose:** Determine if the action should be enabled for a specific language
- **Functionality:** Checks if the language is not null and not plain text
- **Location and Accessibility:** Currently part of the `isLanguageSupported` method. Could be extracted into a utility class.
- **Dependencies:** ComputerLanguage enum

```kotlin
fun isLanguageSupportedForCommenting(computerLanguage: ComputerLanguage?): Boolean {
    return computerLanguage != null && computerLanguage != ComputerLanguage.Text
}
```


#### Function 2: Chat Proxy Creation
- **Description:** Create a ChatProxy instance for AI-assisted code editing
- **Purpose:** Set up the AI model for processing code
- **Functionality:** Configures and creates a ChatProxy with specific settings
- **Location and Accessibility:** Currently part of the `processSelection` method. Could be extracted into a utility class.
- **Dependencies:** ChatProxy, AppSettingsState, API interface

```kotlin
fun createCodeEditingChatProxy(api: Any, virtualApiClass: Class<*>): ChatProxy {
    return ChatProxy(
        clazz = virtualApiClass,
        api = api,
        temperature = AppSettingsState.instance.temperature,
        model = AppSettingsState.instance.smartModel.chatModel(),
        deserializerRetries = 5
    )
}
```


#### Function 3: Code Editing Request
- **Description:** Generate a request for AI-assisted code editing
- **Purpose:** Prepare the input for the AI model to process code
- **Functionality:** Formats the code, operation, and language information for the AI request
- **Location and Accessibility:** Currently part of the `processSelection` method. Could be extracted into a utility class.
- **Dependencies:** AppSettingsState

```kotlin
fun generateCodeEditingRequest(
    code: String,
    operation: String,
    computerLanguage: String
): Pair<String, String, String, String> {
    return Pair(
        code,
        operation,
        computerLanguage,
        AppSettingsState.instance.humanLanguage
    )
}
```

These functions could be refactored into a utility class to be shared across different actions that involve AI-assisted code editing or language-specific operations. This would improve code reusability and maintainability across the project.# legacy\DocAction.kt


## Shared Functionality Analysis: DocAction.kt


### Code Overview
- **Language & Frameworks:** Kotlin, IntelliJ Platform SDK
- **Dependencies:** 
  - com.github.simiacryptus.aicoder.actions.SelectionAction
  - com.github.simiacryptus.aicoder.config.AppSettingsState
  - com.github.simiacryptus.aicoder.util.ComputerLanguage
  - com.github.simiacryptus.aicoder.util.IndentedText
  - com.github.simiacryptus.aicoder.util.psi.PsiUtil
  - com.intellij.openapi.actionSystem.ActionUpdateThread
  - com.intellij.openapi.actionSystem.AnActionEvent
  - com.intellij.openapi.project.Project
  - com.simiacryptus.jopenai.proxy.ChatProxy


### Common Logic


#### Function 1: processCode
- **Description:** Processes code to generate documentation
- **Purpose:** To generate documentation for a given code block
- **Functionality:** 
  - Takes code, operation description, computer language, and human language as input
  - Returns a DocAction_ConvertedText object containing the generated documentation
- **Location and Accessibility:** Part of DocAction_VirtualAPI interface, could be extracted and made more general
- **Dependencies:** ChatProxy, AppSettingsState


#### Function 2: isLanguageSupported
- **Description:** Checks if a given computer language is supported for documentation generation
- **Purpose:** To determine if the DocAction can be applied to a specific language
- **Functionality:**
  - Checks if the language is not Text
  - Verifies if the language has a non-null and non-blank docStyle
- **Location and Accessibility:** Already a separate method, could be made static and more general
- **Dependencies:** ComputerLanguage


#### Function 3: editSelection
- **Description:** Adjusts the selection range to encompass the entire code block
- **Purpose:** To ensure the entire relevant code block is selected for documentation
- **Functionality:**
  - Uses PsiUtil to get the code element at the current selection
  - Adjusts the selection range to match the code element's range
- **Location and Accessibility:** Could be extracted and made more general for other actions that work on code blocks
- **Dependencies:** PsiUtil, EditorState


#### Function 4: processSelection
- **Description:** Processes the selected code to add documentation
- **Purpose:** To generate and insert documentation for the selected code
- **Functionality:**
  - Extracts the selected text and language
  - Calls the processCode method to generate documentation
  - Prepends the generated documentation to the original code
- **Location and Accessibility:** Could be refactored to be more general, possibly as a template method
- **Dependencies:** IndentedText, AppSettingsState, DocAction_VirtualAPI

These functions represent core functionality that could be useful across multiple components, especially for actions that involve code analysis, documentation generation, and selection manipulation. Refactoring them into more general, static utility methods could improve code reuse and maintainability across the project.# legacy\ImplementStubAction.kt


## Shared Functionality Analysis: ImplementStubAction.kt


### Code Overview
- **Language & Frameworks:** Kotlin, IntelliJ Platform SDK
- **Dependencies:** 
  - com.github.simiacryptus.aicoder.actions.SelectionAction
  - com.github.simiacryptus.aicoder.config.AppSettingsState
  - com.github.simiacryptus.aicoder.util.ComputerLanguage
  - com.github.simiacryptus.aicoder.util.psi.PsiUtil
  - com.intellij.openapi.actionSystem
  - com.simiacryptus.jopenai.proxy.ChatProxy
  - com.simiacryptus.jopenai.util.StringUtil


### Common Logic


#### Function 1: getProxy()
- **Description:** Creates and returns a ChatProxy instance for the VirtualAPI interface.
- **Purpose:** To provide a proxy for making API calls to implement code stubs.
- **Functionality:** Initializes a ChatProxy with specific settings from AppSettingsState.
- **Location and Accessibility:** Already a private method in the class, could be made protected for wider use.
- **Dependencies:** AppSettingsState, ChatProxy, VirtualAPI interface


#### Function 2: isLanguageSupported(computerLanguage: ComputerLanguage?)
- **Description:** Checks if a given computer language is supported for stub implementation.
- **Purpose:** To determine if the action should be enabled for a specific language.
- **Functionality:** Returns true for all languages except ComputerLanguage.Text.
- **Location and Accessibility:** Already a public method, could be moved to a utility class for wider use.
- **Dependencies:** ComputerLanguage enum


#### Function 3: defaultSelection(editorState: EditorState, offset: Int)
- **Description:** Determines the default selection range for the action.
- **Purpose:** To provide a sensible default selection when the action is invoked.
- **Functionality:** Finds the smallest code range or falls back to the current line.
- **Location and Accessibility:** Could be extracted to a utility class for reuse in other actions.
- **Dependencies:** EditorState, PsiUtil


#### Function 4: processSelection(state: SelectionState, config: String?)
- **Description:** Processes the selected code to implement a stub.
- **Purpose:** Core functionality of the action to generate implemented code from a stub.
- **Functionality:** 
  - Extracts relevant code context
  - Prepares the code declaration
  - Calls the API to implement the stub
- **Location and Accessibility:** Could be refactored into smaller, more reusable functions.
- **Dependencies:** SelectionState, AppSettingsState, PsiUtil, StringUtil, VirtualAPI


#### Function 5: stripSuffix (from StringUtil)
- **Description:** Removes a suffix from a string if present.
- **Purpose:** To clean up code declarations.
- **Functionality:** Removes a specified suffix from the end of a string.
- **Location and Accessibility:** Already in StringUtil, widely accessible.
- **Dependencies:** None

These functions represent core functionality that could be useful across multiple components of the plugin, especially for actions that involve code manipulation and API interactions. Some refactoring might be beneficial to make these functions more generally accessible and reusable.# legacy\InsertImplementationAction.kt


## Shared Functionality Analysis: InsertImplementationAction.kt


### Code Overview
- **Language & Frameworks:** Kotlin, IntelliJ Platform SDK
- **Dependencies:** OpenAI API, PSI (Program Structure Interface)


### Common Logic


#### Function 1: getProxy()
- **Description:** Creates a proxy for the VirtualAPI interface using ChatProxy.
- **Purpose:** To facilitate communication with the AI model for code implementation.
- **Functionality:** 
  - Creates and returns a ChatProxy instance for the VirtualAPI interface.
  - Uses AppSettingsState to configure the proxy with the appropriate model and temperature.
- **Location and Accessibility:** Currently a private method in InsertImplementationAction. Could be refactored into a utility class for broader use.
- **Dependencies:** AppSettingsState, ChatProxy, VirtualAPI interface


#### Function 2: getPsiClassContextActionParams()
- **Description:** Extracts PSI context information for the current selection.
- **Purpose:** To provide context for the AI when implementing code.
- **Functionality:**
  - Determines the selection range and finds the largest intersecting comment.
  - Returns a PsiClassContextActionParams object with this information.
- **Location and Accessibility:** Currently a private method in InsertImplementationAction. Could be refactored into a utility class for PSI-related operations.
- **Dependencies:** PsiUtil


#### Function 3: processSelection()
- **Description:** Processes the selected text to generate and insert an implementation.
- **Purpose:** Core functionality of the InsertImplementationAction.
- **Functionality:**
  - Extracts the specification from comments or selected text.
  - Uses the VirtualAPI to generate code implementation.
  - Combines the original selection with the generated code.
- **Location and Accessibility:** Part of the SelectionAction abstract class. Could be refactored to separate the AI interaction logic for reuse in other actions.
- **Dependencies:** AppSettingsState, PsiClassContext, VirtualAPI


#### Function 4: isLanguageSupported()
- **Description:** Checks if the action supports the given computer language.
- **Purpose:** To filter out unsupported languages like plain text and Markdown.
- **Functionality:** 
  - Checks if the language is null, Text, or Markdown and returns false for these cases.
  - Delegates to the superclass for other languages.
- **Location and Accessibility:** Override of a method in SelectionAction. Could be moved to a utility class for language support checks.
- **Dependencies:** ComputerLanguage enum

These functions represent core functionalities that could be useful across multiple components in the plugin. Refactoring them into utility classes or shared services would improve code reusability and maintainability. For example, the proxy creation logic could be part of a general AI service, while PSI-related functions could be part of a PSI utility class.# markdown\MarkdownImplementActionGroup.kt


## Shared Functionality Analysis: MarkdownImplementActionGroup.kt


### Code Overview
- **Language & Frameworks:** Kotlin, IntelliJ Platform SDK
- **Dependencies:** 
  - com.github.simiacryptus.aicoder.actions.SelectionAction
  - com.github.simiacryptus.aicoder.config.AppSettingsState
  - com.github.simiacryptus.aicoder.util.ComputerLanguage
  - com.github.simiacryptus.aicoder.util.UITools
  - com.intellij.openapi.actionSystem.*
  - com.simiacryptus.jopenai.proxy.ChatProxy


### Common Logic


#### Function 1: isEnabled
- **Description:** Checks if the action should be enabled based on the current context.
- **Purpose:** To determine if the MarkdownImplementActionGroup should be visible and enabled.
- **Functionality:** 
  - Checks if the current language is Markdown
  - Verifies if there's a text selection
- **Location and Accessibility:** Already a companion object function, can be used as is.
- **Dependencies:** ComputerLanguage, UITools


#### Function 2: getProxy
- **Description:** Creates a ChatProxy instance for the ConversionAPI interface.
- **Purpose:** To provide an API for implementing markdown text in various programming languages.
- **Functionality:**
  - Creates a ChatProxy with specific settings (model, temperature, etc.)
- **Location and Accessibility:** Currently a private method in MarkdownImplementAction. Could be refactored to be more general and accessible.
- **Dependencies:** ChatProxy, AppSettingsState


#### Function 3: processSelection
- **Description:** Processes the selected text and converts it to code in the specified language.
- **Purpose:** To implement the core functionality of converting markdown to code.
- **Functionality:**
  - Uses the ConversionAPI to implement the selected text in the specified language
  - Formats the result as a markdown code block
- **Location and Accessibility:** Currently part of MarkdownImplementAction. Could be refactored to be more general.
- **Dependencies:** ConversionAPI


#### Function 4: ConversionAPI interface
- **Description:** Defines the API for converting text between human and computer languages.
- **Purpose:** To provide a clear interface for text conversion operations.
- **Functionality:**
  - Defines the `implement` method for converting text
- **Location and Accessibility:** Already defined as an interface, can be used as is or moved to a separate file for broader use.
- **Dependencies:** None


#### Function 5: getChildren
- **Description:** Generates a list of actions for different programming languages.
- **Purpose:** To populate the action group with language-specific implementation actions.
- **Functionality:**
  - Creates MarkdownImplementAction instances for each supported language
- **Location and Accessibility:** Part of MarkdownImplementActionGroup. Could be generalized for other similar action groups.
- **Dependencies:** MarkdownImplementAction

These functions and interfaces provide core functionality that could be useful across multiple components, especially for actions dealing with language conversion and markdown processing. Some refactoring might be needed to make them more accessible and general-purpose, potentially moving them to utility classes or shared modules within the project.# legacy\ReplaceWithSuggestionsAction.kt


## Shared Functionality Analysis: ReplaceWithSuggestionsAction.kt


### Code Overview
- **Language & Frameworks:** Kotlin, IntelliJ Platform SDK
- **Dependencies:** 
  - com.github.simiacryptus.aicoder.actions.SelectionAction
  - com.github.simiacryptus.aicoder.config.AppSettingsState
  - com.github.simiacryptus.aicoder.util.UITools
  - com.intellij.openapi.actionSystem
  - com.simiacryptus.jopenai.proxy.ChatProxy
  - com.simiacryptus.jopenai.util.StringUtil


### Common Logic


#### Function 1: getSuffixForContext
- **Description:** Gets a suffix of a given string with a specified ideal length.
- **Purpose:** To provide context before the selected text.
- **Functionality:** Extracts a suffix from a string, replacing newlines with spaces.
- **Location and Accessibility:** Currently part of the processSelection method. Should be extracted to a utility class.
- **Dependencies:** StringUtil


#### Function 2: getPrefixForContext
- **Description:** Gets a prefix of a given string with a specified ideal length.
- **Purpose:** To provide context after the selected text.
- **Functionality:** Extracts a prefix from a string, replacing newlines with spaces.
- **Location and Accessibility:** Currently part of the processSelection method. Should be extracted to a utility class.
- **Dependencies:** StringUtil


#### Function 3: calculateIdealLength
- **Description:** Calculates an ideal length based on the length of the selected text.
- **Purpose:** To determine an appropriate context length for suggestions.
- **Functionality:** Uses a logarithmic formula to calculate the ideal length.
- **Location and Accessibility:** Currently inline in the processSelection method. Should be extracted to a utility class.
- **Dependencies:** kotlin.math


#### Function 4: createChatProxy
- **Description:** Creates a ChatProxy instance for the VirtualAPI interface.
- **Purpose:** To provide an abstraction layer for API communication.
- **Functionality:** Initializes a ChatProxy with specific settings from AppSettingsState.
- **Location and Accessibility:** Currently part of the proxy property. Could be extracted to a utility class for reuse.
- **Dependencies:** ChatProxy, AppSettingsState


#### Function 5: showChoiceDialog
- **Description:** Displays a dialog for the user to choose from a list of options.
- **Purpose:** To allow user interaction for selecting a suggestion.
- **Functionality:** Shows a radio button dialog with given choices.
- **Location and Accessibility:** Currently part of the choose method. Could be generalized and moved to UITools.
- **Dependencies:** UITools

These functions represent common logic that could be useful across multiple components of the plugin. Extracting them into separate utility classes would improve code organization and reusability.# markdown\MarkdownListAction.kt


## Shared Functionality Analysis: MarkdownListAction.kt


### Code Overview
- **Language & Frameworks:** Kotlin, IntelliJ Platform SDK
- **Dependencies:** 
  - com.github.simiacryptus.aicoder.*
  - com.intellij.openapi.actionSystem.*
  - com.intellij.openapi.application.ApplicationManager
  - com.simiacryptus.jopenai.proxy.ChatProxy
  - com.simiacryptus.jopenai.util.StringUtil


### Common Logic


#### Function 1: getSmallestIntersecting
- **Description:** Finds the smallest PSI element of a specific type that intersects with a given range.
- **Purpose:** To locate specific Markdown elements (like lists) within a file.
- **Functionality:** Takes a PsiFile, start and end offsets, and an element type name, and returns the smallest matching PSI element.
- **Location and Accessibility:** Currently used within the `isEnabled` and `handle` methods. Could be extracted to a utility class for broader use.
- **Dependencies:** Depends on PsiUtil, which might need to be made more accessible.


#### Function 2: getAll
- **Description:** Retrieves all PSI elements of a specific type within a parent element.
- **Purpose:** To extract specific components (like list items) from a larger Markdown structure.
- **Functionality:** Takes a parent PSI element and a type name, and returns a list of matching child elements.
- **Location and Accessibility:** Used within the `handle` method. Could be moved to a utility class for reuse.
- **Dependencies:** Depends on PsiUtil, which might need to be made more accessible.


#### Function 3: getIndent
- **Description:** Determines the indentation at a specific caret position.
- **Purpose:** To maintain consistent indentation when adding new list items.
- **Functionality:** Takes a Caret object and returns the indentation as a string.
- **Location and Accessibility:** Currently part of UITools. Already accessible for reuse.
- **Dependencies:** UITools class.


#### Function 4: insertString
- **Description:** Inserts a string into a document at a specified offset.
- **Purpose:** To add new content (like list items) to the existing document.
- **Functionality:** Takes a Document, an offset, and a string to insert.
- **Location and Accessibility:** Part of UITools. Already accessible for reuse.
- **Dependencies:** UITools class.


#### Function 5: redoableTask
- **Description:** Wraps an action in a redoable/undoable command.
- **Purpose:** To ensure that actions can be undone or redone by the user.
- **Functionality:** Takes an AnActionEvent and a lambda to execute.
- **Location and Accessibility:** Part of UITools. Already accessible for reuse.
- **Dependencies:** UITools class.


#### Function 6: run
- **Description:** Runs a task with a progress indicator.
- **Purpose:** To provide user feedback during potentially long-running operations.
- **Functionality:** Takes a Project, a title string, a boolean for cancellability, and a lambda to execute.
- **Location and Accessibility:** Part of UITools. Already accessible for reuse.
- **Dependencies:** UITools class.

These functions represent common operations that could be useful across multiple components dealing with Markdown processing, PSI manipulation, and UI interactions within the IntelliJ platform. Some are already part of utility classes (UITools), while others (like the PSI-related functions) might benefit from being extracted into a dedicated utility class for easier reuse across the project.# legacy\RenameVariablesAction.kt


## Shared Functionality Analysis: RenameVariablesAction.kt


### Code Overview
- **Language & Frameworks:** Kotlin, IntelliJ Platform SDK
- **Dependencies:** 
  - com.github.simiacryptus.aicoder.actions.SelectionAction
  - com.github.simiacryptus.aicoder.config.AppSettingsState
  - com.github.simiacryptus.aicoder.util.ComputerLanguage
  - com.github.simiacryptus.aicoder.util.UITools
  - com.intellij.openapi.actionSystem.ActionUpdateThread
  - com.intellij.openapi.actionSystem.AnActionEvent
  - com.intellij.openapi.project.Project
  - com.simiacryptus.jopenai.proxy.ChatProxy


### Common Logic


#### Function 1: suggestRenames
- **Description:** Suggests variable renames based on the given code and language context
- **Purpose:** To provide intelligent variable renaming suggestions
- **Functionality:** 
  - Takes code, computer language, and human language as input
  - Returns a list of renaming suggestions (original name to suggested name)
- **Location and Accessibility:** Currently part of the RenameAPI interface. Could be extracted into a separate utility class for broader use.
- **Dependencies:** Requires access to the AI model (ChatProxy)


#### Function 2: choose
- **Description:** Presents a dialog for users to select which renaming suggestions to apply
- **Purpose:** To allow user interaction in the renaming process
- **Functionality:**
  - Takes a map of original names to suggested names
  - Shows a checkbox dialog for user selection
  - Returns a set of selected original names
- **Location and Accessibility:** Currently a method in RenameVariablesAction. Could be generalized and moved to a UI utility class.
- **Dependencies:** UITools.showCheckboxDialog


#### Function 3: processSelection
- **Description:** Processes the selected text to apply the chosen renaming suggestions
- **Purpose:** To perform the actual variable renaming in the code
- **Functionality:**
  - Gets renaming suggestions from the AI model
  - Allows user to choose which suggestions to apply
  - Applies the selected renamings to the code
- **Location and Accessibility:** Part of RenameVariablesAction. Core logic could be extracted into a separate utility function for reuse in similar refactoring actions.
- **Dependencies:** suggestRenames, choose, UITools.run


#### Function 4: isLanguageSupported
- **Description:** Checks if the given computer language is supported for this action
- **Purpose:** To determine if the action should be enabled for a particular language
- **Functionality:** 
  - Takes a ComputerLanguage as input
  - Returns true if the language is supported (not plain text)
- **Location and Accessibility:** Could be generalized and moved to a utility class for language-specific action support.
- **Dependencies:** ComputerLanguage enum

These functions represent core functionalities that could be useful across multiple components, especially for actions involving code analysis, refactoring, and user interaction. Extracting and generalizing these functions could improve code reusability and maintainability across the project.# legacy\VoiceToTextAction.kt


## Shared Functionality Analysis: VoiceToTextAction.kt


### Code Overview
- **Language & Frameworks:** Kotlin, IntelliJ Platform SDK
- **Dependencies:** 
  - com.github.simiacryptus.aicoder
  - com.intellij.openapi.actionSystem
  - com.simiacryptus.jopenai.audio
  - javax.sound.sampled
  - javax.swing


### Common Logic


#### Function 1: Audio Recording
- **Description:** Records audio input from the system's microphone
- **Purpose:** Capture user's voice for transcription
- **Functionality:** 
  - Uses AudioRecorder class to record audio
  - Stores raw audio data in a ConcurrentLinkedDeque
- **Location and Accessibility:** Currently embedded in handle() method, could be extracted into a separate function
- **Dependencies:** AudioRecorder, ConcurrentLinkedDeque


#### Function 2: Audio Processing
- **Description:** Processes raw audio data for speech recognition
- **Purpose:** Prepare audio data for transcription
- **Functionality:** 
  - Uses LookbackLoudnessWindowBuffer to process raw audio
  - Stores processed audio data in a ConcurrentLinkedDeque
- **Location and Accessibility:** Currently embedded in handle() method, could be extracted into a separate function
- **Dependencies:** LookbackLoudnessWindowBuffer, ConcurrentLinkedDeque


#### Function 3: Speech-to-Text Conversion
- **Description:** Converts processed audio to text
- **Purpose:** Transcribe user's speech
- **Functionality:** 
  - Uses an API (likely OpenAI's) to transcribe audio
  - Inserts transcribed text into the editor
- **Location and Accessibility:** Currently in DictationPump inner class, could be extracted and generalized
- **Dependencies:** OpenAI API (or similar)


#### Function 4: UI Status Dialog
- **Description:** Creates and displays a status dialog
- **Purpose:** Provide user feedback and control for the dictation process
- **Functionality:** 
  - Creates a JFrame with a label
  - Positions the dialog relative to the current context component
- **Location and Accessibility:** statusDialog() method, could be made more general for reuse
- **Dependencies:** javax.swing


#### Function 5: Audio Device Availability Check
- **Description:** Checks if a suitable audio input device is available
- **Purpose:** Determine if the action should be enabled
- **Functionality:** 
  - Attempts to get a TargetDataLine for the system's audio input
  - Uses a timeout to avoid blocking indefinitely
- **Location and Accessibility:** Currently in isEnabled() method and companion object, could be extracted into a utility function
- **Dependencies:** javax.sound.sampled.AudioSystem

These functions represent core functionalities that could potentially be useful in other parts of the application or in related plugins. Extracting and generalizing these functions could improve code reusability and maintainability.# OpenWebPageAction.kt


## Shared Functionality Analysis: OpenWebPageAction.kt


### Code Overview
- **Language & Frameworks:** Kotlin, IntelliJ Platform SDK
- **Dependencies:** Java AWT Desktop, java.net.URI


### Common Logic

The `OpenWebPageAction` class is a simple action that opens a specific web page. While it doesn't contain any explicitly shared functionality, we can extract some common logic that could be useful across multiple components.


#### Function 1: openWebPage
- **Description:** A function to open a web page using the default browser.
- **Purpose:** To provide a reusable method for opening web pages from within the IDE.
- **Functionality:** 
  - Checks if Desktop is supported
  - Checks if browsing is supported
  - Opens the specified URL in the default browser
- **Location and Accessibility:** This functionality needs to be extracted from the `actionPerformed` method and refactored into a separate, public static function.
- **Dependencies:** Java AWT Desktop, java.net.URI

Here's how the refactored function could look:

```kotlin
companion object {
    fun openWebPage(url: String) {
        if (Desktop.isDesktopSupported()) {
            val desktop = Desktop.getDesktop()
            if (desktop.isSupported(Desktop.Action.BROWSE)) {
                desktop.browse(URI(url))
            }
        }
    }
}
```

This refactored function could be used in the `OpenWebPageAction` class and other parts of the application that need to open web pages.


#### Function 2: isWebBrowsingSupported
- **Description:** A function to check if web browsing is supported on the current system.
- **Purpose:** To provide a quick way to check if the application can open web pages.
- **Functionality:** 
  - Checks if Desktop is supported
  - Checks if browsing is supported
- **Location and Accessibility:** This functionality needs to be extracted from the `actionPerformed` method and refactored into a separate, public static function.
- **Dependencies:** Java AWT Desktop

Here's how the refactored function could look:

```kotlin
companion object {
    fun isWebBrowsingSupported(): Boolean {
        return Desktop.isDesktopSupported() && Desktop.getDesktop().isSupported(Desktop.Action.BROWSE)
    }
}
```

This function could be used to determine whether to enable or disable web browsing-related features in the application.

By extracting these functions, we create reusable components that can be used across the application, improving code organization and reducing duplication.# problems\AnalyzeProblemAction.kt


## Shared Functionality Analysis: AnalyzeProblemAction.kt


### Code Overview
- **Language & Frameworks:** Kotlin, IntelliJ IDEA Plugin Development
- **Dependencies:** 
  - IntelliJ Platform API
  - OpenAI API (via IdeaOpenAIClient)
  - Skyenet library
  - Custom utilities (e.g., AppServer, SessionProxyServer)


### Common Logic


#### Function 1: findGitRoot
- **Description:** Finds the Git root directory for a given file
- **Purpose:** To locate the base directory of the Git repository
- **Functionality:** Traverses up the directory tree to find the .git folder
- **Location and Accessibility:** Currently imported from TestResultAutofixAction.Companion. Could be refactored into a separate utility class.
- **Dependencies:** IntelliJ VirtualFile API


#### Function 2: getProjectStructure
- **Description:** Generates a string representation of the project structure
- **Purpose:** To provide context about the project layout
- **Functionality:** Likely traverses the project directory and creates a tree-like structure
- **Location and Accessibility:** Currently imported from TestResultAutofixAction.Companion. Could be refactored into a separate utility class.
- **Dependencies:** Possibly File or VirtualFile API


#### Function 3: renderMarkdown
- **Description:** Renders markdown content for display in the UI
- **Purpose:** To convert markdown to HTML for better readability
- **Functionality:** Parses markdown and converts it to HTML
- **Location and Accessibility:** Imported from MarkdownUtil. Seems to be already in a utility class.
- **Dependencies:** Skyenet library


#### Function 4: addApplyFileDiffLinks
- **Description:** Adds interactive links to apply file diffs
- **Purpose:** To allow users to easily apply suggested changes
- **Functionality:** Processes diff content and adds clickable links
- **Location and Accessibility:** Extension function on SocketManager. Could be refactored into a separate utility class.
- **Dependencies:** Skyenet library, custom diff utilities


#### Function 5: addSaveLinks
- **Description:** Adds save links to the response
- **Purpose:** To allow users to save changes easily
- **Functionality:** Processes the response and adds save functionality
- **Location and Accessibility:** Extension function on SocketManager. Could be refactored into a separate utility class.
- **Dependencies:** Skyenet library


#### Function 6: openAnalysisSession
- **Description:** Opens a new analysis session in the browser
- **Purpose:** To start the problem analysis process
- **Functionality:** Creates a new session, sets up the application, and opens the browser
- **Location and Accessibility:** Private method in AnalyzeProblemAction. Could be generalized and moved to a utility class for reuse in similar actions.
- **Dependencies:** AppServer, SessionProxyServer, Desktop API

These functions represent common functionality that could be useful across multiple components of the plugin. Some refactoring might be needed to make them more general and accessible, possibly by moving them to dedicated utility classes.# test\TestResultAutofixAction.kt


## Shared Functionality Analysis: TestResultAutofixAction.kt


### Code Overview
- **Language & Frameworks:** Kotlin, IntelliJ IDEA Plugin Development
- **Dependencies:** 
  - IntelliJ Platform SDK
  - SkyeNet library
  - JOpenAI library
  - Various custom utilities and actions


### Common Logic


#### Function 1: getFiles
- **Description:** Recursively collects file paths from given virtual files or paths
- **Purpose:** To gather a set of file paths for analysis or processing
- **Functionality:** 
  - Filters out hidden files and those in .gitignore
  - Recursively traverses directories
  - Returns a set of Path objects
- **Location and Accessibility:** Already static in companion object, accessible
- **Dependencies:** None specific


#### Function 2: getProjectStructure
- **Description:** Generates a string representation of the project file structure
- **Purpose:** To provide an overview of the project's file structure for analysis
- **Functionality:** 
  - Uses getFiles to collect paths
  - Filters files by size (< 0.5MB)
  - Formats the output as a string with file paths and sizes
- **Location and Accessibility:** Already static in companion object, accessible
- **Dependencies:** getFiles function


#### Function 3: findGitRoot
- **Description:** Finds the root directory of a Git repository
- **Purpose:** To locate the base directory of a Git project
- **Functionality:** 
  - Traverses up the directory tree
  - Checks for the presence of a .git directory
  - Returns the root path if found, null otherwise
- **Location and Accessibility:** Already static in companion object, accessible
- **Dependencies:** None specific


#### Function 4: getTestInfo
- **Description:** Extracts information from a test proxy object
- **Purpose:** To gather details about a failed test for analysis
- **Functionality:** 
  - Collects test name, duration, error message, and stack trace
  - Formats the information as a string
- **Location and Accessibility:** Private method, could be refactored to be more general and accessible
- **Dependencies:** SMTestProxy class


#### Function 5: openAutofixWithTestResult
- **Description:** Initiates the autofix process for a failed test
- **Purpose:** To start the analysis and suggestion process for fixing a failed test
- **Functionality:** 
  - Creates a new session
  - Sets up the TestResultAutofixApp
  - Opens a browser to display the results
- **Location and Accessibility:** Private method, could be refactored to be more general and accessible
- **Dependencies:** Various custom classes and utilities

These functions provide core functionality that could be useful across multiple components of the plugin. Some refactoring might be needed to make them more accessible and general-purpose, particularly for the last two functions which are currently private methods.# SelectionAction.kt


## Shared Functionality Analysis: SelectionAction.kt


### Code Overview
- **Language & Frameworks:** Kotlin, IntelliJ Platform SDK
- **Dependencies:** Various IntelliJ Platform APIs (e.g., AnActionEvent, Editor, PsiFile)


### Common Logic


#### Function 1: retarget
- **Description:** Adjusts the selection range based on the current editor state and selection.
- **Purpose:** To ensure a valid selection range for further processing.
- **Functionality:** 
  - Handles cases with and without existing selections.
  - Applies default selection if no text is selected.
  - Ensures the selection is within valid document bounds.
- **Location and Accessibility:** Currently a private method in SelectionAction. Could be refactored to be more accessible.
- **Dependencies:** EditorState


#### Function 2: editorState
- **Description:** Creates an EditorState object from the current editor.
- **Purpose:** To encapsulate the current state of the editor for easier manipulation.
- **Functionality:**
  - Extracts text, cursor position, line information, and PSI file from the editor.
  - Generates context ranges for the current cursor position.
- **Location and Accessibility:** Currently a private method in SelectionAction. Could be made public and static for wider use.
- **Dependencies:** Editor, PsiFile


#### Function 3: contextRanges
- **Description:** Generates an array of ContextRange objects for the current cursor position.
- **Purpose:** To provide context information about the code structure around the cursor.
- **Functionality:**
  - Traverses the PSI tree and collects elements that contain the cursor position.
- **Location and Accessibility:** Currently a private method in SelectionAction. Could be extracted as a utility function.
- **Dependencies:** PsiFile, Editor


#### Function 4: isLanguageSupported
- **Description:** Checks if a given computer language is supported by the action.
- **Purpose:** To determine if the action should be enabled for the current file type.
- **Functionality:** 
  - Currently a simple null check, but can be extended for specific language support.
- **Location and Accessibility:** Already a public method, but could be made static and moved to a utility class.
- **Dependencies:** ComputerLanguage


#### Function 5: UITools.redoableTask
- **Description:** Executes a task that can be undone/redone in the IDE.
- **Purpose:** To wrap action execution in a redoable context.
- **Functionality:**
  - Provides undo/redo functionality for the action's changes.
- **Location and Accessibility:** Already accessible through UITools, but could be documented more clearly.
- **Dependencies:** AnActionEvent


#### Function 6: UITools.writeableFn
- **Description:** Executes a function in a writable context.
- **Purpose:** To ensure that document modifications are performed in a writable state.
- **Functionality:**
  - Wraps document modifications in a writable command.
- **Location and Accessibility:** Already accessible through UITools, but could be documented more clearly.
- **Dependencies:** AnActionEvent

These functions provide core functionality that could be useful across multiple components in the plugin. Some refactoring might be needed to make them more accessible and reusable, such as moving them to utility classes or making them public static methods.