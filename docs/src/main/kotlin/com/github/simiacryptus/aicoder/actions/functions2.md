# Implementation Roadmap

1. FileSystemUtils (High Priority)
- getFiles: Recursively collect file paths from VirtualFiles or Paths, excluding hidden and .gitignore files
  - Signature: fun getFiles(virtualFiles: Array<out VirtualFile>?): MutableSet<Path>
  - Signature: fun getFiles(virtualFiles: Array<out Path>?): MutableSet<Path>
  - Implementation: Traverse directories, use isGitignore to check for excluded files, handle both VirtualFile and Path inputs
  - Used in: CommandAutofixAction.kt, CreateImageAction.kt, MultiCodeChatAction.kt, MultiDiffChatAction.kt, TestResultAutofixAction.kt
- expand: Recursively expand VirtualFiles, including directory contents
  - Signature: fun expand(data: Array<VirtualFile>?): Array<VirtualFile>?
  - Implementation: Recursively process directories, flatten into a single array of VirtualFiles
  - Used in: ChatWithCommitAction.kt, ReplicateCommitAction.kt
- findGitRoot: Find the root directory of a Git repository
  - Signature: fun findGitRoot(path: Path?): Path?
  - Signature: fun findGitRoot(virtualFile: VirtualFile?): VirtualFile?
  - Implementation: Traverse up the directory tree, looking for a .git folder
  - Used in: AnalyzeProblemAction.kt, TestResultAutofixAction.kt
- getProjectStructure: Generate a string representation of the project structure
  - Signature: fun getProjectStructure(projectPath: VirtualFile?): String
  - Signature: fun getProjectStructure(root: Path): String
  - Implementation: Recursively list files and directories, format as a tree-like string
  - Used in: AnalyzeProblemAction.kt, TestResultAutofixAction.kt
- isGitignore: Check if a file or directory should be ignored based on .gitignore rules
  - Signature: fun isGitignore(file: VirtualFile): Boolean
  - Signature: fun isGitignore(path: Path): Boolean
  - Implementation: Parse .gitignore files, apply rules to check if a file should be ignored
  - Used in: CommandAutofixAction.kt
- toPaths: Convert a string path (potentially with wildcards) to a list of actual file paths
  - Signature: fun toPaths(root: Path, it: String): Iterable<Path>
  - Implementation: Handle wildcard expansion, resolve relative paths against the root
  - Used in: ReplicateCommitAction.kt, SimpleCommandAction.kt

2. UIUtils (High Priority)
- openBrowserToUri: Open the default web browser to a specified URI
  - Signature: fun openBrowserToUri(uri: URI)
  - Implementation: Use Desktop.browse() if supported, fallback to OS-specific commands if needed
  - Used in: CodeChatAction.kt, DiffChatAction.kt, GenericChatAction.kt, MultiCodeChatAction.kt, MultiDiffChatAction.kt, OpenWebPageAction.kt
- showOptionsDialog: Display a dialog with radio button options for user selection
  - Signature: fun showOptionsDialog(title: String, options: Array<String>): String?
  - Implementation: Create a JOptionPane with radio buttons, return selected option
  - Used in: ReplaceWithSuggestionsAction.kt
- createStatusDialog: Create a status dialog to indicate ongoing processes
  - Signature: fun createStatusDialog(message: String, location: Point): JFrame
  - Implementation: Create a non-modal JFrame with a message, position at given location
  - Used in: VoiceToTextAction.kt
- redoableTask: Execute a task that can be redone/undone in the IntelliJ environment
  - Signature: fun redoableTask(e: AnActionEvent, task: () -> Unit)
  - Implementation: Wrap task in a CommandProcessor.getInstance().executeCommand() call
  - Used in: MarkdownListAction.kt, SelectionAction.kt
- run: Run a task with a progress indicator
  - Signature: fun run(project: Project?, title: String, canBeCancelled: Boolean, task: () -> Unit)
  - Implementation: Use ProgressManager to show a modal progress dialog while executing the task
  - Used in: MarkdownListAction.kt

3. CodeProcessingUtils (Medium Priority)
- extractCode: Extract code from a string that may contain markdown code blocks
  - Signature: fun extractCode(code: String): String
  - Implementation: Use regex to remove markdown code block delimiters if present
  - Used in: ReactTypescriptWebDevelopmentAssistantAction.kt, WebDevelopmentAssistantAction.kt
- codeSummary: Generate a summary of code files in the project
  - Signature: fun codeSummary(codeFiles: Set<Path>, root: File): String
  - Implementation: Read file contents, format with file paths as headers
  - Used in: CreateImageAction.kt, WebDevelopmentAssistantAction.kt
- formatCodeWithLineNumbers: Format code by adding line numbers
  - Signature: fun formatCodeWithLineNumbers(code: String): String
  - Implementation: Split code into lines, prepend line numbers, join back into a string
  - Used in: LineFilterChatAction.kt
- getSuffixForContext: Get a suffix of a given string with a specified ideal length
  - Signature: fun getSuffixForContext(text: String, idealLength: Int): String
  - Implementation: Calculate ideal length, extract suffix, replace newlines with spaces
  - Used in: ReplaceWithSuggestionsAction.kt
- getPrefixForContext: Get a prefix of a given string with a specified ideal length
  - Signature: fun getPrefixForContext(text: String, idealLength: Int): String
  - Implementation: Calculate ideal length, extract prefix, replace newlines with spaces
  - Used in: ReplaceWithSuggestionsAction.kt

4. ChatProxyUtils (Medium Priority)
- createChatProxy: Create a ChatProxy instance for various API interfaces
  - Signature: fun <T> createChatProxy(api: Any, clazz: Class<T>): T
  - Implementation: Use ChatProxy.create() with appropriate settings from AppSettingsState
  - Used in: CommentsAction.kt, ReplaceWithSuggestionsAction.kt
- createCodeChatSession: Create a new code chat session
  - Signature: fun createCodeChatSession(session: String, language: String, codeSelection: String, filename: String, api: API, model: String, storage: StorageInterface): CodeChatSocketManager
  - Implementation: Initialize CodeChatSocketManager with given parameters
  - Used in: CodeChatAction.kt
- setupChatSession: Set up a chat session with given parameters
  - Signature: fun setupChatSession(session: String, language: String, codeSelection: String, filename: String): CodeChatSocketManager
  - Implementation: Create and configure a CodeChatSocketManager instance
  - Used in: ChatWithCommitDiffAction.kt

5. LanguageUtils (Medium Priority)
- isLanguageSupported: Check if a given computer language is supported for various actions
  - Signature: fun isLanguageSupported(computerLanguage: ComputerLanguage?): Boolean
  - Implementation: Check against a list of supported languages or specific criteria
  - Used in: CommentsAction.kt, RenameVariablesAction.kt, SelectionAction.kt
- getComputerLanguage: Retrieve the computer language for the current context
  - Signature: fun getComputerLanguage(e: AnActionEvent): ComputerLanguage?
  - Implementation: Extract language information from the file type or PSI
  - Used in: LineFilterChatAction.kt, CodeChatAction.kt

6. DiffUtils (Medium Priority)
- generateDiffInfo: Generate a diff information string from a list of changes
  - Signature: fun generateDiffInfo(files: Array<VirtualFile>?, changes: Array<out Change>?): String
  - Implementation: Process changes, format as a readable diff string
  - Used in: ReplicateCommitAction.kt
- formatChangesForChat: Format VCS changes into a readable diff format for chat
  - Signature: fun formatChangesForChat(changes: List<Change>, files: Array<VirtualFile>?): String
  - Implementation: Process changes, create a formatted string with file paths and diff content
  - Used in: ChatWithCommitAction.kt
- addApplyFileDiffLinks: Add interactive links to apply file diffs in the UI
  - Signature: fun addApplyFileDiffLinks(root: Path, code: () -> Map<Path, String>, response: String, handle: (Map<Path, String>) -> Unit, ui: ApplicationInterface): String
  - Implementation: Parse diff, add HTML links to apply changes, integrate with UI
  - Used in: DiffChatAction.kt, MassPatchAction.kt, MultiStepPatchAction.kt

7. AudioUtils (Low Priority)
- recordAudio: Record audio input from the system's microphone
  - Signature: fun recordAudio(buffer: ConcurrentLinkedDeque<ByteArray>, continueFn: () -> Boolean)
  - Implementation: Use javax.sound.sampled to capture audio data
  - Used in: VoiceToTextAction.kt
- processAudio: Process raw audio data into a format suitable for speech-to-text conversion
  - Signature: fun processAudio(inputBuffer: ConcurrentLinkedDeque<ByteArray>, outputBuffer: ConcurrentLinkedDeque<ByteArray>, continueFn: () -> Boolean)
  - Implementation: Convert to WAV format, apply loudness windowing
  - Used in: VoiceToTextAction.kt
- convertSpeechToText: Convert processed audio data into text
  - Signature: fun convertSpeechToText(audioBuffer: Deque<ByteArray>, api: ApiClient, editor: Editor, offsetStart: Int, continueFn: () -> Boolean)
  - Implementation: Send audio to API, receive text, insert into editor
  - Used in: VoiceToTextAction.kt
- isMicrophoneAvailable: Check if a microphone is available for recording
  - Signature: fun isMicrophoneAvailable(): Boolean
  - Implementation: Attempt to get a TargetDataLine for audio input
  - Used in: VoiceToTextAction.kt

8. ImageUtils (Low Priority)
- write: Write an image to a ByteArray
  - Signature: fun write(code: ImageResponse, path: Path): ByteArray
  - Implementation: Convert ImageResponse to BufferedImage, write to ByteArrayOutputStream
  - Used in: CreateImageAction.kt, ReactTypescriptWebDevelopmentAssistantAction.kt
- draftImage: Draft an image file using an AI actor
  - Signature: fun draftImage(task: SessionTask, request: Array<ApiModel.ChatMessage>, actor: ImageActor, path: Path)
  - Implementation: Use AI to generate image, save to file, update UI
  - Used in: ReactTypescriptWebDevelopmentAssistantAction.kt, WebDevelopmentAssistantAction.kt

9. MarkdownUtils (Low Priority)
- renderMarkdown: Render markdown content to HTML
  - Signature: fun renderMarkdown(markdown: String, ui: ApplicationInterface? = null): String
  - Implementation: Use a markdown parser library to convert to HTML
  - Used in: DiffChatAction.kt, GenerateDocumentationAction.kt, LineFilterChatAction.kt, MultiDiffChatAction.kt
- renderMarkdownFileList: Render a list of files with their token counts as markdown
  - Signature: fun renderMarkdownFileList(root: File, codeFiles: Set<Path>, codex: GPT4Tokenizer): String
  - Implementation: Generate markdown list, estimate token counts, format as string
  - Used in: MultiCodeChatAction.kt

10. PsiUtils (Low Priority)
- getSmallestIntersecting: Find the smallest PSI element of a specific type that intersects with a given range
  - Signature: fun getSmallestIntersecting(psiFile: PsiFile, start: Int, end: Int, type: String): PsiElement?
  - Implementation: Traverse PSI tree, find elements of specified type within range
  - Used in: MarkdownListAction.kt
- getAll: Retrieve all PSI elements of a specific type within a given PSI element
  - Signature: fun getAll(element: PsiElement, type: String): List<PsiElement>
  - Implementation: Recursively search for elements of specified type
  - Used in: MarkdownListAction.kt
- contextRanges: Extract context ranges from a PsiFile based on the current editor position
  - Signature: fun contextRanges(psiFile: PsiFile?, editor: Editor): Array<ContextRange>
  - Implementation: Find PSI elements containing cursor, create ContextRange objects
  - Used in: SelectionAction.kt

# ApplyPatchAction.kt


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
- **Functionality:** Takes a VirtualFile, patch content, and project, then applies the patch to the file's content
- **Location and Accessibility:** Currently a private method in ApplyPatchAction class. Refactoring needed to make it a public static method.
- **Signature:** 
  ```kotlin
  fun applyPatch(file: VirtualFile, patchContent: String, project: Project)
  ```
- **Dependencies:** 
  - WriteCommandAction
  - PsiManager
  - IterativePatchUtil


#### Function 2: getUserInputPatch
- **Description:** Prompts user to input patch content
- **Functionality:** Shows a multi-line input dialog for the user to enter patch content
- **Location and Accessibility:** Currently part of the handle method. Needs to be extracted and made into a separate public static method.
- **Signature:** 
  ```kotlin
  fun getUserInputPatch(project: Project): String?
  ```
- **Dependencies:** 
  - Messages

These functions could be useful across multiple components that deal with applying patches or getting user input for patches. By extracting and generalizing these functions, they can be reused in other parts of the codebase that require similar functionality.# code\DescribeAction.kt


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
- **Description:** Generates a description of the given code using AI.
- **Functionality:** Takes code, computer language, and human language as input and returns a description.
- **Location and Accessibility:** Currently part of the DescribeAction_VirtualAPI interface. Could be extracted as a standalone function.
- **Signature:** 
  ```kotlin
  fun describeCode(code: String, computerLanguage: String, humanLanguage: String): String
  ```
- **Dependencies:** AI model (ChatProxy)


#### Function 2: Comment Wrapping
- **Description:** Wraps the generated description in appropriate comment syntax based on the number of lines.
- **Functionality:** Determines whether to use line or block comments based on the description length.
- **Location and Accessibility:** Currently embedded in processSelection method. Can be extracted as a separate function.
- **Signature:** 
  ```kotlin
  fun wrapInComments(description: String, language: ComputerLanguage?, indent: String): String
  ```
- **Dependencies:** StringUtil.lineWrapping, ComputerLanguage (for comment syntax)


#### Function 3: Indentation Handling
- **Description:** Handles indentation of the generated comment and original code.
- **Functionality:** Applies proper indentation to maintain code structure.
- **Location and Accessibility:** Currently part of processSelection method. Can be extracted for reuse.
- **Signature:** 
  ```kotlin
  fun applyIndentation(text: String, indent: String): String
  ```
- **Dependencies:** None

These functions represent core functionalities that could be useful across multiple components in the project. Extracting them as standalone, public static methods would improve code reusability and maintainability. The code description generation, in particular, could be valuable in various code analysis and documentation tasks throughout the project.# BaseAction.kt


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
- **Functionality:** Returns a boolean indicating if the action is enabled
- **Location and Accessibility:** Already an open function in BaseAction class
- **Signature:** `open fun isEnabled(event: AnActionEvent): Boolean`
- **Dependencies:** AnActionEvent


#### Function 2: logAction
- **Description:** Logs the action being performed
- **Functionality:** Logs the action name and any relevant details
- **Location and Accessibility:** Currently part of actionPerformed, could be extracted to a separate function
- **Signature:** `fun logAction(actionName: String)`
- **Dependencies:** UITools


#### Function 3: handleActionError
- **Description:** Handles errors that occur during action execution
- **Functionality:** Logs the error and displays an error message to the user
- **Location and Accessibility:** Currently part of actionPerformed, could be extracted to a separate function
- **Signature:** `fun handleActionError(actionName: String, error: Throwable)`
- **Dependencies:** UITools, LoggerFactory


#### Function 4: getOpenAIClient
- **Description:** Retrieves the OpenAI client instance
- **Functionality:** Returns the OpenAI client for API interactions
- **Location and Accessibility:** Currently a property, could be converted to a function for more flexibility
- **Signature:** `fun getOpenAIClient(): OpenAIClient`
- **Dependencies:** IdeaOpenAIClient


#### Function 5: updateActionPresentation
- **Description:** Updates the action's presentation based on its enabled state
- **Functionality:** Sets the action's enabled and visible state
- **Location and Accessibility:** Currently part of update function, could be extracted for reuse
- **Signature:** `fun updateActionPresentation(event: AnActionEvent, isEnabled: Boolean)`
- **Dependencies:** AnActionEvent

These functions represent common functionality that could be useful across multiple components. Some refactoring would be needed to extract them into independent public static methods, but doing so could improve code reusability and maintainability.# code\CustomEditAction.kt


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
- **Description:** Edits code based on given instructions and language settings
- **Functionality:** Takes input code, operation instructions, computer language, and human language, and returns edited code
- **Location and Accessibility:** Currently part of the VirtualAPI interface. Could be extracted as a standalone function.
- **Signature:** 
  ```kotlin
  fun editCode(
      code: String,
      operation: String,
      computerLanguage: String,
      humanLanguage: String
  ): EditedText
  ```
- **Dependencies:** Requires access to AI model (ChatProxy)


#### Function 2: showInputDialog
- **Description:** Displays an input dialog to get user instructions
- **Functionality:** Shows a dialog box to prompt the user for input
- **Location and Accessibility:** Currently used within getConfig method. Could be extracted as a standalone utility function.
- **Signature:** 
  ```kotlin
  fun showInputDialog(
      parentComponent: Component?,
      message: String,
      title: String,
      messageType: Int
  ): String?
  ```
- **Dependencies:** javax.swing.JOptionPane


#### Function 3: addInstructionToHistory
- **Description:** Adds a user instruction to the history of recent commands
- **Functionality:** Updates the list of recent commands for a specific category
- **Location and Accessibility:** Currently used within processSelection method. Could be extracted as a standalone function in AppSettingsState.
- **Signature:** 
  ```kotlin
  fun addInstructionToHistory(category: String, instruction: String)
  ```
- **Dependencies:** AppSettingsState


#### Function 4: createChatProxy
- **Description:** Creates a ChatProxy instance for the VirtualAPI
- **Functionality:** Initializes a ChatProxy with specific settings and examples
- **Location and Accessibility:** Currently part of the proxy property. Could be extracted as a standalone function.
- **Signature:** 
  ```kotlin
  fun createChatProxy(api: OpenAIClient, settings: AppSettingsState): ChatProxy<VirtualAPI>
  ```
- **Dependencies:** ChatProxy, AppSettingsState, OpenAIClient

These functions represent common logic that could potentially be shared across multiple components in the project. Extracting them as standalone utility functions would improve code reusability and maintainability.# code\PasteAction.kt


## Shared Functionality Analysis: PasteAction.kt


### Code Overview
- **Language & Frameworks:** Kotlin, IntelliJ Platform SDK
- **Dependencies:** 
  - com.github.simiacryptus.aicoder.actions.SelectionAction
  - com.github.simiacryptus.aicoder.config.AppSettingsState
  - com.github.simiacryptus.aicoder.util.ComputerLanguage
  - com.intellij.openapi.actionSystem.ActionUpdateThread
  - com.intellij.openapi.actionSystem.AnActionEvent
  - com.intellij.openapi.project.Project
  - com.simiacryptus.jopenai.proxy.ChatProxy
  - java.awt.Toolkit
  - java.awt.datatransfer.DataFlavor


### Common Logic


#### Function 1: hasClipboard
- **Description:** Checks if the system clipboard contains supported text data.
- **Functionality:** Determines if the clipboard content is supported (string or plain text Unicode).
- **Location and Accessibility:** This function is private in the PasteAction class. It should be refactored into a public static method in a utility class for broader use.
- **Signature:** 
  ```kotlin
  fun hasClipboard(): Boolean
  ```
- **Dependencies:** java.awt.Toolkit, java.awt.datatransfer.DataFlavor


#### Function 2: getClipboard
- **Description:** Retrieves the content of the system clipboard.
- **Functionality:** Extracts and returns the clipboard content if it's in a supported format.
- **Location and Accessibility:** This function is private in the PasteAction class. It should be refactored into a public static method in a utility class for broader use.
- **Signature:** 
  ```kotlin
  fun getClipboard(): Any?
  ```
- **Dependencies:** java.awt.Toolkit, java.awt.datatransfer.DataFlavor


#### Function 3: isLanguageSupported
- **Description:** Checks if a given computer language is supported for the paste action.
- **Functionality:** Determines if the language is supported (not null and not plain text).
- **Location and Accessibility:** This function is already public in the PasteAction class but could be moved to a utility class for broader use.
- **Signature:** 
  ```kotlin
  fun isLanguageSupported(computerLanguage: ComputerLanguage?): Boolean
  ```
- **Dependencies:** com.github.simiacryptus.aicoder.util.ComputerLanguage

These functions provide utility for clipboard operations and language support checking, which could be useful in various parts of the application dealing with text manipulation and code conversion. Refactoring them into a separate utility class would make them more accessible and reusable across the project.# code\RecentCodeEditsAction.kt


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
- **Description:** Determines if the action should be enabled based on the current context.
- **Functionality:** Checks if there's a selection and if the computer language is not plain text.
- **Location and Accessibility:** Already a companion object function, can be made public if not already.
- **Signature:** 
  ```kotlin
  fun isEnabled(e: AnActionEvent): Boolean
  ```
- **Dependencies:** 
  - UITools.hasSelection
  - ComputerLanguage.getComputerLanguage


#### Function 2: getRecentCommands
- **Description:** Retrieves recent commands from AppSettingsState.
- **Functionality:** Gets the most used history of custom edits.
- **Location and Accessibility:** This is not a function in the provided code, but it's used and could be extracted from AppSettingsState for more general use.
- **Signature:** 
  ```kotlin
  fun getRecentCommands(category: String): List<Pair<String, Int>>
  ```
- **Dependencies:** AppSettingsState


#### Function 3: createActionFromInstruction
- **Description:** Creates an AnAction object from an instruction string.
- **Functionality:** Generates a CustomEditAction with the given instruction.
- **Location and Accessibility:** This functionality is currently inline in getChildren method, but could be extracted as a separate function for reuse.
- **Signature:** 
  ```kotlin
  fun createActionFromInstruction(instruction: String, id: Int): AnAction
  ```
- **Dependencies:** CustomEditAction

These functions represent common logic that could potentially be useful across multiple components in the project. The `isEnabled` function is already well-positioned for reuse, while `getRecentCommands` and `createActionFromInstruction` would need to be extracted and refactored from their current inline implementations to be more generally applicable.# code\RedoLast.kt


## Shared Functionality Analysis: RedoLast.kt


### Code Overview
- **Language & Frameworks:** Kotlin, IntelliJ Platform SDK
- **Dependencies:** 
  - com.github.simiacryptus.aicoder.actions.BaseAction
  - com.github.simiacryptus.aicoder.util.UITools.retry
  - com.intellij.openapi.actionSystem.*
  - com.intellij.openapi.actionSystem.CommonDataKeys


### Common Logic

The RedoLast class doesn't contain any specific functions that could be easily extracted for shared functionality. However, there are some common patterns and concepts that could be useful across multiple components:


#### Action Update Thread Handling
- **Description:** Setting the action update thread to background (BGT)
- **Functionality:** Ensures that action updates are performed on a background thread
- **Location and Accessibility:** This is already a method override, but could be extracted to a base class or interface
- **Signature:** `override fun getActionUpdateThread() = ActionUpdateThread.BGT`
- **Dependencies:** com.intellij.openapi.actionSystem.ActionUpdateThread


#### Document Retrieval from AnActionEvent
- **Description:** Retrieving the document from the AnActionEvent
- **Functionality:** Gets the current document being edited
- **Location and Accessibility:** This is embedded in the handle and isEnabled methods, but could be extracted to a utility function
- **Signature:** `fun getDocumentFromEvent(e: AnActionEvent): Document`
- **Dependencies:** com.intellij.openapi.actionSystem.AnActionEvent, com.intellij.openapi.actionSystem.CommonDataKeys


#### Retry Functionality
- **Description:** Accessing and executing a retry action
- **Functionality:** Retrieves and executes a previously stored action for the current document
- **Location and Accessibility:** This functionality is provided by the UITools.retry object, which could be made more accessible or generalized
- **Signature:** N/A (part of UITools.retry)
- **Dependencies:** com.github.simiacryptus.aicoder.util.UITools.retry


#### Action Enablement Check
- **Description:** Checking if the action should be enabled
- **Functionality:** Determines if there's a retry action available for the current document
- **Location and Accessibility:** This is a method override, but the logic could be extracted to a utility function
- **Signature:** `fun isRetryAvailable(document: Document): Boolean`
- **Dependencies:** com.github.simiacryptus.aicoder.util.UITools.retry

While these aren't standalone functions in the current implementation, they represent common patterns that could be abstracted and shared across multiple action classes. Refactoring these into utility functions or base class methods could improve code reuse and maintainability.# dev\LineFilterChatAction.kt


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
- **Functionality:** Determines the programming language of the file being edited
- **Location and Accessibility:** This is already a static method in ComputerLanguage class
- **Signature:** `fun getComputerLanguage(e: AnActionEvent): ComputerLanguage?`
- **Dependencies:** AnActionEvent


#### Function 2: renderMarkdown
- **Description:** Renders markdown text
- **Functionality:** Converts markdown formatted text to HTML or another readable format
- **Location and Accessibility:** This is already a static method in MarkdownUtil class
- **Signature:** `fun renderMarkdown(text: String): String`
- **Dependencies:** None apparent in this file


#### Function 3: openBrowserToUri
- **Description:** Opens a web browser to a specific URI
- **Functionality:** Launches the default web browser and navigates to a given URI
- **Location and Accessibility:** This functionality is currently embedded in a Thread within the handle method. It could be extracted into a separate utility function.
- **Signature:** `fun openBrowserToUri(uri: URI)`
- **Dependencies:** java.awt.Desktop, java.net.URI


#### Function 4: createChatSocketManager
- **Description:** Creates a ChatSocketManager instance with specific configurations
- **Functionality:** Initializes a ChatSocketManager with custom settings for code chat
- **Location and Accessibility:** This functionality is currently part of the handle method. It could be extracted into a separate function for reuse.
- **Signature:** `fun createChatSocketManager(session: String, filename: String, language: String, code: String): ChatSocketManager`
- **Dependencies:** ChatSocketManager, AppSettingsState, ApplicationServices


#### Function 5: formatCodeWithLineNumbers
- **Description:** Formats code by adding line numbers
- **Functionality:** Takes a code string and returns it with line numbers prepended to each line
- **Location and Accessibility:** This functionality is currently embedded in the handle method. It could be extracted into a utility function.
- **Signature:** `fun formatCodeWithLineNumbers(code: String): String`
- **Dependencies:** None

These functions represent common logic that could be useful across multiple components of the plugin. Extracting them into separate utility classes or a shared module would improve code reusability and maintainability.# dev\PrintTreeAction.kt


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

The PrintTreeAction class doesn't contain any public static functions that could be directly shared across multiple components. However, it does use some functionality from other classes that could potentially be useful in other parts of the application. Here are some functions that could be extracted or are already available for shared use:


#### Function 1: PsiUtil.printTree
- **Description:** Prints the tree structure of a PSI (Program Structure Interface) element
- **Functionality:** Converts the PSI structure into a string representation
- **Location and Accessibility:** Already available in PsiUtil class
- **Signature:** `fun printTree(psiElement: PsiElement): String`
- **Dependencies:** IntelliJ Platform SDK (PsiElement)


#### Function 2: PsiUtil.getLargestContainedEntity
- **Description:** Retrieves the largest PSI entity contained within the current context
- **Functionality:** Finds and returns the most significant PSI element in the current action context
- **Location and Accessibility:** Already available in PsiUtil class
- **Signature:** `fun getLargestContainedEntity(e: AnActionEvent): PsiElement?`
- **Dependencies:** IntelliJ Platform SDK (AnActionEvent, PsiElement)


#### Function 3: isDevActionEnabled
- **Description:** Checks if developer actions are enabled in the application settings
- **Functionality:** Retrieves the devActions flag from AppSettingsState
- **Location and Accessibility:** Could be extracted as a public static method in a utility class
- **Signature:** `fun isDevActionEnabled(): Boolean`
- **Dependencies:** AppSettingsState

```kotlin
object DevActionUtil {
    fun isDevActionEnabled(): Boolean {
        return AppSettingsState.instance.devActions
    }
}
```

This function could be useful for other developer-specific actions that need to be conditionally enabled based on the same setting.

While the PrintTreeAction class itself doesn't provide directly shareable functions, it demonstrates a pattern for creating developer-specific actions that could be replicated for other debugging or development tools within the IDE plugin.# FileContextAction.kt


## Shared Functionality Analysis: FileContextAction.kt


### Code Overview
- **Language & Frameworks:** Kotlin, IntelliJ Platform SDK
- **Dependencies:** com.intellij.openapi.actionSystem, com.intellij.openapi.application, com.intellij.openapi.fileEditor, com.intellij.openapi.project, com.intellij.openapi.vfs, org.slf4j


### Common Logic


#### Function 1: open
- **Description:** Opens a file in the IntelliJ IDE
- **Functionality:** Refreshes the file system, finds the file, and opens it in the IDE editor
- **Location and Accessibility:** Already a companion object function, can be made public static
- **Signature:** 
  ```kotlin
  fun open(project: Project, outputPath: Path)
  ```
- **Dependencies:** IntelliJ Platform SDK (ApplicationManager, LocalFileSystem, FileEditorManager)


#### Function 2: processSelection
- **Description:** Abstract function to process the selected file or folder
- **Functionality:** Takes a SelectionState and configuration, returns an array of Files
- **Location and Accessibility:** Already an abstract function, can be made public
- **Signature:** 
  ```kotlin
  abstract fun processSelection(state: SelectionState, config: T?): Array<File>
  ```
- **Dependencies:** None specific to this function


#### Function 3: getConfig
- **Description:** Retrieves configuration for the action
- **Functionality:** Returns configuration object of type T
- **Location and Accessibility:** Already an open function, can be made public
- **Signature:** 
  ```kotlin
  open fun getConfig(project: Project?, e: AnActionEvent): T?
  ```
- **Dependencies:** AnActionEvent


#### Function 4: isEnabled
- **Description:** Checks if the action is enabled based on selected file/folder and settings
- **Functionality:** Determines if the action should be enabled in the UI
- **Location and Accessibility:** Already overridden from BaseAction, can be made public
- **Signature:** 
  ```kotlin
  override fun isEnabled(event: AnActionEvent): Boolean
  ```
- **Dependencies:** AppSettingsState, UITools


#### Function 5: handle
- **Description:** Handles the execution of the action
- **Functionality:** Processes the selected file/folder and performs the action
- **Location and Accessibility:** Already final override from BaseAction, can be made protected
- **Signature:**
  ```kotlin
 override fun handle(e: AnActionEvent)
  ```
- **Dependencies:** UITools, LocalFileSystem

These functions provide core functionality for file-based actions in the IntelliJ plugin context. They can be extracted and refactored to be more general-purpose utilities for other components that need to interact with files, project structure, and the IntelliJ IDE environment.# generic\CodeChatAction.kt


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
  - LoggerFactory


### Common Logic


#### Function 1: getComputerLanguage
- **Description:** Retrieves the computer language for the current context
- **Functionality:** Determines the programming language of the current file or selection
- **Location and Accessibility:** This is currently a method call on ComputerLanguage. It could be extracted and made into a standalone utility function.
- **Signature:** `fun getComputerLanguage(e: AnActionEvent): ComputerLanguage?`
- **Dependencies:** AnActionEvent, ComputerLanguage


#### Function 2: createCodeChatSession
- **Description:** Creates a new code chat session
- **Functionality:** Sets up a new CodeChatSocketManager with the necessary parameters
- **Location and Accessibility:** This functionality is currently embedded in the handle method. It could be extracted into a separate function for reuse.
- **Signature:** 
  ```kotlin
  fun createCodeChatSession(
    session: String,
    language: String,
    codeSelection: String,
    filename: String,
    api: API,
    model: String,
    storage: StorageInterface
  ): CodeChatSocketManager
  ```
- **Dependencies:** CodeChatSocketManager, API, StorageInterface


#### Function 3: openBrowserToSession
- **Description:** Opens the default browser to the chat session URL
- **Functionality:** Constructs the session URL and uses Desktop.browse to open it
- **Location and Accessibility:** This is currently an anonymous thread in the handle method. It could be extracted into a utility function.
- **Signature:** `fun openBrowserToSession(server: AppServer, session: String)`
- **Dependencies:** AppServer, Desktop


#### Function 4: getSelectedTextOrDocumentText
- **Description:** Retrieves the selected text or the entire document text if nothing is selected
- **Functionality:** Provides the code content for the chat session
- **Location and Accessibility:** This logic is embedded in the handle method. It could be extracted into a utility function.
- **Signature:** `fun getSelectedTextOrDocumentText(editor: Editor): String`
- **Dependencies:** Editor

These functions represent common logic that could be useful across multiple components of the plugin. Extracting them into standalone utility functions would improve code reusability and maintainability.# generic\CommandAutofixAction.kt


## Shared Functionality Analysis: CommandAutofixAction.kt


### Code Overview
- **Language & Frameworks:** Kotlin, IntelliJ Platform SDK
- **Dependencies:** 
  - com.github.simiacryptus.aicoder
  - com.intellij.openapi
  - com.simiacryptus.jopenai
  - com.simiacryptus.skyenet
  - java.awt
  - java.io
  - java.nio.file
  - javax.swing
  - org.slf4j


### Common Logic


#### Function 1: getFiles
- **Description:** Recursively retrieves all files from given virtual files, excluding certain directories and files.
- **Functionality:** Traverses directories and collects file paths, skipping hidden directories and those matched by .gitignore.
- **Location and Accessibility:** Currently a private method in CommandAutofixAction. Could be refactored to a public static method in a utility class.
- **Signature:** 
  ```kotlin
  fun getFiles(virtualFiles: Array<out VirtualFile>?): MutableSet<Path>
  ```
- **Dependencies:** VirtualFile from IntelliJ Platform SDK, java.nio.file.Path


#### Function 2: isGitignore
- **Description:** Checks if a file or directory should be ignored based on .gitignore rules.
- **Functionality:** Traverses up the directory tree, checking .gitignore files and matching patterns.
- **Location and Accessibility:** Currently a companion object method. Could be moved to a utility class for broader use.
- **Signature:** 
  ```kotlin
  fun isGitignore(file: VirtualFile): Boolean
  fun isGitignore(path: Path): Boolean
  ```
- **Dependencies:** VirtualFile from IntelliJ Platform SDK, java.nio.file.Path


#### Function 3: htmlEscape (Extension Property)
- **Description:** Escapes HTML special characters in a StringBuilder.
- **Functionality:** Replaces special characters with their HTML entity equivalents.
- **Location and Accessibility:** Currently an extension property on StringBuilder in the companion object. Could be moved to a string utility class.
- **Signature:** 
  ```kotlin
  val StringBuilder.htmlEscape: String
  ```
- **Dependencies:** None


#### Function 4: renderMarkdown
- **Description:** Renders markdown content to HTML.
- **Functionality:** Converts markdown to HTML for display in the UI.
- **Location and Accessibility:** Not defined in this file, but used multiple times. Could be centralized in a utility class if not already.
- **Signature:** 
  ```kotlin
  fun renderMarkdown(content: String, ui: ApplicationInterface? = null): String
  ```
- **Dependencies:** ApplicationInterface (likely from a custom UI framework)


#### Function 5: addApplyFileDiffLinks
- **Description:** Adds interactive links to apply file diffs in the UI.
- **Functionality:** Processes diff content and creates clickable links for applying changes.
- **Location and Accessibility:** Not defined in this file, but used. Could be part of a diff utility class.
- **Signature:** 
  ```kotlin
  fun addApplyFileDiffLinks(root: Path, code: () -> Map<Path, String>, response: String, handle: (Map<Path, String>) -> Unit, ui: ApplicationInterface): String
  ```
- **Dependencies:** ApplicationInterface, java.nio.file.Path


#### Function 6: addSaveLinks
- **Description:** Adds save links to the UI for modified files.
- **Functionality:** Creates clickable links to save changes made to files.
- **Location and Accessibility:** Not defined in this file, but used. Could be part of a UI utility class.
- **Signature:** 
  ```kotlin
  fun addSaveLinks(root: Path, response: String, task: SessionTask, ui: ApplicationInterface): String
  ```
- **Dependencies:** ApplicationInterface, SessionTask, java.nio.file.Path

These functions represent common functionality that could be extracted and centralized for use across multiple components of the application. Refactoring them into utility classes would improve code organization and reusability.# generic\CreateFileFromDescriptionAction.kt


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
- **Functionality:** 
  - Uses OpenAI API to generate file content based on user input.
  - Interprets the API response to extract file path and content.
- **Location and Accessibility:** This function is currently private within the CreateFileFromDescriptionAction class. Refactoring is needed to make it an independent public static method.
- **Signature:** 
  ```kotlin
  fun generateFile(basePath: String, directive: String): ProjectFile
  ```
- **Dependencies:** 
  - AppSettingsState
  - OpenAI API client


#### Function 2: processSelection
- **Description:** Processes the user's file selection and generates a new file.
- **Functionality:**
  - Calculates relative paths and module roots.
  - Calls generateFile to create new file content.
  - Handles file naming conflicts.
  - Writes the generated content to a file.
- **Location and Accessibility:** This function is currently an override within the CreateFileFromDescriptionAction class. Refactoring is needed to extract the core logic into a separate public static method.
- **Signature:** 
  ```kotlin
  fun processFileCreation(projectRoot: File, selectedFile: File, directive: String): File
  ```
- **Dependencies:** 
  - java.io.File
  - generateFile function


#### Function 3: parseGeneratedFileResponse
- **Description:** Parses the API response to extract file path and content.
- **Functionality:**
  - Extracts file path from the response header.
  - Removes code block markers from the content.
- **Location and Accessibility:** This functionality is currently embedded within the generateFile function. It should be extracted into a separate public static method.
- **Signature:**
  ```kotlin
  fun parseGeneratedFileResponse(response: String): Pair<String, String>
  ```
- **Dependencies:** None

These functions encapsulate core functionality that could be useful across multiple components dealing with file generation and manipulation based on AI-generated content. Extracting and refactoring them into public static methods would improve code reusability and maintainability.# generic\CreateImageAction.kt


## Shared Functionality Analysis: CreateImageAction.kt


### Code Overview
- **Language & Frameworks:** Kotlin, IntelliJ Platform SDK
- **Dependencies:** 
  - IntelliJ Platform SDK
  - Skyenet library
  - JOpenAI library
  - SLF4J for logging


### Common Logic


#### Function 1: write
- **Description:** Writes an image to a file and returns the byte array of the image.
- **Functionality:** Converts an ImageResponse to a byte array and saves it to a specified path.
- **Location and Accessibility:** This is a private method in the CreateImageAction class. It could be refactored into a public static method in a utility class.
- **Signature:** 
  ```kotlin
  fun write(code: ImageResponse, path: Path): ByteArray
  ```
- **Dependencies:** Java ImageIO, ByteArrayOutputStream


#### Function 2: getFiles
- **Description:** Recursively collects file paths from a given set of virtual files.
- **Functionality:** Traverses directories and collects relative paths of files.
- **Location and Accessibility:** This is a private method in the CreateImageAction class. It could be refactored into a public static method in a utility class.
- **Signature:** 
  ```kotlin
  fun getFiles(virtualFiles: Array<out VirtualFile>?, root: Path): MutableSet<Path>
  ```
- **Dependencies:** IntelliJ VirtualFile API


#### Function 3: codeSummary
- **Description:** Generates a summary of code files in Markdown format.
- **Functionality:** Reads contents of files and formats them into a Markdown string with file paths and code blocks.
- **Location and Accessibility:** This is a local function within the `handle` method. It could be extracted and generalized into a public static method.
- **Signature:** 
  ```kotlin
  fun codeSummary(): String
  ```
- **Dependencies:** Java File API


#### Function 4: openBrowserToUri
- **Description:** Opens the default web browser to a specified URI.
- **Functionality:** Uses Desktop API to open a browser with a given URI.
- **Location and Accessibility:** This functionality is embedded in a Thread within the `handle` method. It could be extracted into a public static utility method.
- **Signature:** 
  ```kotlin
  fun openBrowserToUri(uri: URI)
  ```
- **Dependencies:** Java Desktop API

These functions could be extracted and refactored into utility classes to promote code reuse across different components of the plugin. For example, `write` and `getFiles` could be part of a FileUtils class, `codeSummary` could be part of a CodeSummaryUtils class, and `openBrowserToUri` could be part of a BrowserUtils class.# generic\DiffChatAction.kt


## Shared Functionality Analysis: DiffChatAction.kt


### Code Overview
- **Language & Frameworks:** Kotlin, IntelliJ Platform SDK
- **Dependencies:** 
  - com.github.simiacryptus.aicoder.*
  - com.intellij.openapi.*
  - com.simiacryptus.skyenet.*
  - java.awt.Desktop


### Common Logic


#### Function 1: addApplyDiffLinks
- **Description:** Adds apply links to diff blocks in the response
- **Functionality:** Processes the AI response, adds interactive links to apply code changes
- **Location and Accessibility:** Currently embedded in renderResponse method. Should be extracted as a public static method.
- **Signature:** 
  ```kotlin
  fun addApplyDiffLinks(
    code: () -> String,
    response: String,
    handle: (String) -> Unit,
    task: SessionTask,
    ui: ApplicationInterface
  ): String
  ```
- **Dependencies:** ApplicationInterface, SessionTask


#### Function 2: renderMarkdown
- **Description:** Renders markdown content to HTML
- **Functionality:** Converts markdown text to HTML for display
- **Location and Accessibility:** Imported from MarkdownUtil. Already a public static method.
- **Signature:** 
  ```kotlin
  fun renderMarkdown(markdown: String): String
  ```
- **Dependencies:** None (assuming it's a standalone utility function)


#### Function 3: getComputerLanguage
- **Description:** Determines the programming language of the current file
- **Functionality:** Extracts language information from the AnActionEvent
- **Location and Accessibility:** Static method in ComputerLanguage class. Already accessible.
- **Signature:** 
  ```kotlin
  fun getComputerLanguage(e: AnActionEvent): ComputerLanguage?
  ```
- **Dependencies:** AnActionEvent, ComputerLanguage


#### Function 4: openBrowserToUri
- **Description:** Opens the default web browser to a specific URI
- **Functionality:** Launches the system's default browser with a given URI
- **Location and Accessibility:** Currently embedded in a thread within handle method. Should be extracted as a public static method.
- **Signature:** 
  ```kotlin
  fun openBrowserToUri(uri: URI)
  ```
- **Dependencies:** java.awt.Desktop, java.net.URI

These functions represent common functionality that could be useful across multiple components of the plugin. Extracting them into separate utility classes or a shared library would improve code reusability and maintainability.# generic\GenerateDocumentationAction.kt


## Shared Functionality Analysis: GenerateDocumentationAction.kt


### Code Overview
- **Language & Frameworks:** Kotlin, IntelliJ Platform SDK
- **Dependencies:** Apache Commons IO, JOpenAI, Java Concurrent Utilities


### Common Logic


#### Function 1: findGitRoot
- **Description:** Finds the root directory of a Git repository.
- **Functionality:** Traverses up the directory tree from a given path until it finds a .git directory.
- **Location and Accessibility:** Already a public method in the companion object, but could be moved to a utility class for broader use.
- **Signature:** `fun findGitRoot(path: Path?): Path?`
- **Dependencies:** Java NIO


#### Function 2: open
- **Description:** Opens a file or directory in the IntelliJ IDE.
- **Functionality:** Refreshes the file system, finds the file, and opens it in the editor.
- **Location and Accessibility:** Currently in the companion object, could be moved to a utility class for reuse.
- **Signature:** `fun open(project: Project, outputPath: Path)`
- **Dependencies:** IntelliJ Platform SDK, Java Concurrent Utilities


#### Function 3: transformContent
- **Description:** Transforms file content using AI-powered chat completion.
- **Functionality:** Sends file content and instructions to an AI model and returns the transformed content.
- **Location and Accessibility:** Currently a private method, could be extracted and generalized for broader use.
- **Signature:** `fun transformContent(path: Path, fileContent: String, transformationMessage: String): String`
- **Dependencies:** JOpenAI, AppSettingsState


#### Function 4: processSelection
- **Description:** Processes selected files to generate documentation.
- **Functionality:** Handles file processing, content transformation, and output generation.
- **Location and Accessibility:** Currently part of the action class, could be refactored into a separate service.
- **Signature:** `fun processSelection(state: SelectionState, config: Settings?): Array<File>`
- **Dependencies:** Java NIO, Java Concurrent Utilities, Apache Commons IO


#### Function 5: getConfig
- **Description:** Retrieves user configuration for documentation generation.
- **Functionality:** Creates and displays a dialog for user input, then processes the results.
- **Location and Accessibility:** Part of the action class, could be generalized for other configuration dialogs.
- **Signature:** `fun getConfig(project: Project?, e: AnActionEvent): Settings`
- **Dependencies:** IntelliJ Platform SDK, Custom UI components


#### Function 6: items (Extension property)
- **Description:** Extension property for CheckBoxList to get all items.
- **Functionality:** Retrieves all items from a CheckBoxList, regardless of selection state.
- **Location and Accessibility:** Currently at file level, could be moved to a utility class for broader use.
- **Signature:** `val <T> CheckBoxList<T>.items: List<T>`
- **Dependencies:** IntelliJ Platform SDK (CheckBoxList)

These functions and properties contain logic that could be useful across multiple components of the plugin or even in other IntelliJ IDEA plugins. Refactoring them into separate utility classes or services would improve code organization and reusability.# generic\GenericChatAction.kt


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


### Common Logic


#### Function 1: getServer
- **Description:** Retrieves the AppServer instance for a given project
- **Functionality:** Provides access to the server instance associated with the project
- **Location and Accessibility:** Currently part of AppServer class, could be refactored into a utility class
- **Signature:** `fun getServer(project: Project?): AppServer`
- **Dependencies:** AppServer, Project


#### Function 2: openBrowserToUri
- **Description:** Opens the default browser to a specified URI
- **Functionality:** Launches the system's default web browser and navigates to a given URI
- **Location and Accessibility:** Currently embedded in handle method, should be extracted to a utility class
- **Signature:** `fun openBrowserToUri(uri: URI)`
- **Dependencies:** java.awt.Desktop


#### Function 3: createChatSocketManager
- **Description:** Creates a new ChatSocketManager instance
- **Functionality:** Initializes a ChatSocketManager with specified parameters
- **Location and Accessibility:** Currently embedded in handle method, could be extracted and generalized
- **Signature:** 
  ```kotlin
  fun createChatSocketManager(
    session: String,
    model: String,
    initialAssistantPrompt: String,
    userInterfacePrompt: String,
    systemPrompt: String,
    api: OpenAIClient,
    storage: StorageInterface
  ): ChatSocketManager
  ```
- **Dependencies:** ChatSocketManager, OpenAIClient, StorageInterface


#### Function 4: getActionUpdateThread
- **Description:** Specifies the thread for action updates
- **Functionality:** Returns the appropriate ActionUpdateThread for the action
- **Location and Accessibility:** Already a separate method, could be moved to a base class for all actions
- **Signature:** `fun getActionUpdateThread(): ActionUpdateThread`
- **Dependencies:** ActionUpdateThread


#### Function 5: isEnabled
- **Description:** Determines if the action is enabled
- **Functionality:** Checks if the action should be enabled based on the current context
- **Location and Accessibility:** Already a separate method, could be moved to a base class for all actions
- **Signature:** `fun isEnabled(event: AnActionEvent): Boolean`
- **Dependencies:** AnActionEvent

These functions represent common functionality that could be shared across multiple components in the project. Extracting and refactoring them into utility classes or base classes would improve code reusability and maintainability.# generic\GenerateRelatedFileAction.kt


## Shared Functionality Analysis: GenerateRelatedFileAction.kt


### Code Overview
- **Language & Frameworks:** Kotlin, IntelliJ Platform SDK
- **Dependencies:** Apache Commons IO, JOpenAI


### Common Logic


#### Function 1: generateFile
- **Description:** Generates a new file based on a given file and directive using AI.
- **Functionality:** 
  - Takes a base file and a directive as input
  - Uses OpenAI API to generate new file content
  - Parses the API response to extract file path and content
- **Location and Accessibility:** Currently a private method in GenerateRelatedFileAction class. Could be refactored into a public static method in a utility class.
- **Signature:** 
  ```kotlin
  fun generateFile(baseFile: ProjectFile, directive: String): ProjectFile
  ```
- **Dependencies:** AppSettingsState, ApiModel, OpenAI API client


#### Function 2: open
- **Description:** Opens a file in the IntelliJ IDE.
- **Functionality:**
  - Takes a Project and Path as input
  - Refreshes the file system
  - Opens the file in the IDE editor
- **Location and Accessibility:** Currently a companion object function. Already accessible as a static method.
- **Signature:**
  ```kotlin
  fun open(project: Project, outputPath: Path)
  ```
- **Dependencies:** IntelliJ Platform SDK (ApplicationManager, LocalFileSystem, FileEditorManager)


#### Function 3: getModuleRootForFile
- **Description:** Gets the module root for a given file.
- **Functionality:** Determines the root directory of the module containing the specified file.
- **Location and Accessibility:** Not visible in the provided code snippet. Likely a utility method that could be made public and static.
- **Signature:** 
  ```kotlin
  fun getModuleRootForFile(file: File): File
  ```
- **Dependencies:** IntelliJ Platform SDK (potentially ProjectRootManager or similar)


#### Function 4: isEnabled
- **Description:** Checks if the action should be enabled.
- **Functionality:** Verifies if exactly one file is selected.
- **Location and Accessibility:** Override method in GenerateRelatedFileAction. Could be generalized into a utility method for actions that require a single file selection.
- **Signature:**
  ```kotlin
  fun isEnabled(event: AnActionEvent): Boolean
  ```
- **Dependencies:** IntelliJ Platform SDK (AnActionEvent)

These functions represent common logic that could be useful across multiple components in the project. By extracting and refactoring them into public static methods in utility classes, they can be more easily reused and maintained across the codebase.# generic\MassPatchAction.kt

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
- **Functionality:** Displays a dialog for user to select files and enter instructions
- **Location and Accessibility:** Currently a method in MassPatchAction class. Could be refactored into a separate utility class.
- **Signature:** 
  ```kotlin
  fun getConfig(project: Project?, e: AnActionEvent): Settings
  ```
- **Dependencies:** 
  - UITools
  - ConfigDialog (inner class)
  - Settings (data class)


#### Function 2: handle
- **Description:** Handles the action event, prepares code summary, and initiates the patch server
- **Functionality:** Processes selected files, creates a code summary, and starts a MassPatchServer
- **Location and Accessibility:** Currently a method in MassPatchAction class. Core logic could be extracted.
- **Signature:** 
  ```kotlin
  override fun handle(e: AnActionEvent)
  ```
- **Dependencies:** 
  - AppServer
  - MassPatchServer


#### Function 3: createCenterPanel
- **Description:** Creates the main panel for the configuration dialog
- **Functionality:** Sets up UI components for file selection and instruction input
- **Location and Accessibility:** Currently a method in ConfigDialog inner class. Could be generalized for reuse.
- **Signature:** 
  ```kotlin
  override fun createCenterPanel(): JComponent
  ```
- **Dependencies:** 
  - javax.swing components


#### Function 4: newSession
- **Description:** Initializes a new session for the MassPatchServer
- **Functionality:** Sets up tabs for each file and schedules processing tasks
- **Location and Accessibility:** Currently a method in MassPatchServer class. Core logic could be extracted.
- **Signature:** 
  ```kotlin
  override fun newSession(user: User?, session: Session): SocketManager
  ```
- **Dependencies:** 
  - ApplicationSocketManager
  - TabbedDisplay
  - Discussable


#### Function 5: addApplyFileDiffLinks
- **Description:** Adds links to apply file diffs in the UI
- **Functionality:** Processes diff responses and adds interactive links
- **Location and Accessibility:** Referenced as an extension function. Could be moved to a utility class.
- **Signature:** 
  ```kotlin
  fun SocketManager?.addApplyFileDiffLinks(root: Path, code: () -> Map<Path, String>, response: String, handle: (Map<Path, String>) -> Unit, ui: ApplicationInterface): String?
  ```
- **Dependencies:** 
  - SocketManager
  - ApplicationInterface

These functions represent core pieces of functionality that could potentially be shared across different components of the plugin. Refactoring them into more general, public static methods in utility classes would increase their reusability and make the codebase more modular.# generic\MultiStepPatchAction.kt


## Shared Functionality Analysis: MultiStepPatchAction.kt


### Code Overview
- **Language & Frameworks:** Kotlin, IntelliJ Platform SDK
- **Dependencies:** 
  - AppServer
  - BaseAction
  - AppSettingsState
  - UITools
  - Various Skyenet and JOpenAI classes


### Common Logic


#### Function 1: getServer
- **Description:** Retrieves the AppServer instance for a given project
- **Functionality:** Provides access to the application server
- **Location and Accessibility:** Currently part of AppServer class, could be extracted as a standalone utility function
- **Signature:** `fun getServer(project: Project?): AppServer`
- **Dependencies:** AppServer, Project


#### Function 2: getSelectedFolder
- **Description:** Retrieves the selected folder from an AnActionEvent
- **Functionality:** Extracts the selected folder from the event context
- **Location and Accessibility:** Currently part of UITools, already accessible as a utility function
- **Signature:** `fun getSelectedFolder(e: AnActionEvent): VirtualFile?`
- **Dependencies:** AnActionEvent, VirtualFile


#### Function 3: addApplyFileDiffLinks
- **Description:** Adds apply links to file diffs in the response
- **Functionality:** Enhances the response with clickable links to apply changes
- **Location and Accessibility:** Currently part of a SocketManager extension, could be extracted as a standalone utility function
- **Signature:** `fun addApplyFileDiffLinks(root: Path, code: () -> Map<Path, String>, response: String, handle: (Map<Path, String>) -> Unit, ui: ApplicationInterface): String`
- **Dependencies:** Path, ApplicationInterface


#### Function 4: renderMarkdown
- **Description:** Renders markdown content
- **Functionality:** Converts markdown to HTML for display
- **Location and Accessibility:** Already a utility function in MarkdownUtil
- **Signature:** `fun renderMarkdown(markdown: String, ui: ApplicationInterface, tabs: Boolean = true): String`
- **Dependencies:** ApplicationInterface


#### Function 5: toContentList
- **Description:** Converts a string to a list of API content objects
- **Functionality:** Prepares string content for API communication
- **Location and Accessibility:** Already a utility function in ClientUtil
- **Signature:** `fun String.toContentList(): List<ApiModel.Content>`
- **Dependencies:** ApiModel.Content


#### Function 6: toJson
- **Description:** Converts an object to its JSON representation
- **Functionality:** Serializes objects to JSON format
- **Location and Accessibility:** Already a utility function in JsonUtil
- **Signature:** `fun toJson(obj: Any): String`
- **Dependencies:** None specific to this function

These functions represent common operations that could be useful across multiple components of the application. Some are already accessible as utility functions, while others might benefit from being extracted and refactored for more general use.# generic\MultiCodeChatAction.kt


## Shared Functionality Analysis: MultiCodeChatAction.kt


### Code Overview
- **Language & Frameworks:** Kotlin, IntelliJ Platform SDK
- **Dependencies:** 
  - com.github.simiacryptus.aicoder.*
  - com.intellij.openapi.actionSystem.*
  - com.intellij.openapi.vfs.VirtualFile
  - com.simiacryptus.jopenai.*
  - com.simiacryptus.skyenet.*
  - java.awt.Desktop
  - java.io.File
  - java.nio.file.Path


### Common Logic


#### Function 1: getFiles
- **Description:** Recursively collects file paths from given virtual files.
- **Functionality:** Traverses directory structure and adds file paths to a set.
- **Location and Accessibility:** Currently a private method in MultiCodeChatAction class. Can be refactored to a public static method.
- **Signature:** 
  ```kotlin
  fun getFiles(virtualFiles: Array<out VirtualFile>?, root: Path): Set<Path>
  ```
- **Dependencies:** com.intellij.openapi.vfs.VirtualFile, java.nio.file.Path


#### Function 2: codeSummary
- **Description:** Generates a markdown summary of code files.
- **Functionality:** Reads content of files and formats them into a markdown string.
- **Location and Accessibility:** Currently a local function in handle method. Can be refactored to a public static method.
- **Signature:** 
  ```kotlin
  fun codeSummary(root: Path, codeFiles: Set<Path>): String
  ```
- **Dependencies:** java.nio.file.Path, java.io.File


#### Function 3: openBrowserToUri
- **Description:** Opens the default web browser to a specified URI.
- **Functionality:** Uses Desktop API to open a browser with a given URI.
- **Location and Accessibility:** Currently embedded in a thread in handle method. Can be refactored to a public static method.
- **Signature:** 
  ```kotlin
  fun openBrowserToUri(uri: URI)
  ```
- **Dependencies:** java.awt.Desktop, java.net.URI


#### Function 4: renderMarkdownFileList
- **Description:** Renders a list of files with their token counts as markdown.
- **Functionality:** Generates a markdown list of files with estimated token counts.
- **Location and Accessibility:** Currently embedded in userMessage method of PatchApp inner class. Can be refactored to a public static method.
- **Signature:** 
  ```kotlin
  fun renderMarkdownFileList(root: File, codeFiles: Set<Path>, codex: GPT4Tokenizer): String
  ```
- **Dependencies:** java.io.File, java.nio.file.Path, com.simiacryptus.jopenai.GPT4Tokenizer

These functions represent common logic that could be useful across multiple components of the project. Refactoring them into public static methods would improve code reusability and maintainability. They could be placed in a utility class for easy access from other parts of the application.# generic\MultiDiffChatAction.kt


## Shared Functionality Analysis: MultiDiffChatAction.kt


### Code Overview
- **Language & Frameworks:** Kotlin, IntelliJ Platform SDK
- **Dependencies:** 
  - com.github.simiacryptus.aicoder.*
  - com.intellij.openapi.actionSystem.*
  - com.simiacryptus.jopenai.*
  - com.simiacryptus.skyenet.*
  - java.awt.Desktop
  - java.io.File
  - java.nio.file.Path
  - java.util.concurrent.*


### Common Logic

Several functions and pieces of logic in this file could be useful across multiple components. Here's an analysis of potential shared functionality:


#### Function 1: getFiles
- **Description:** Recursively collects file paths from given virtual files.
- **Functionality:** Traverses directory structure and collects file paths relative to a root directory.
- **Location and Accessibility:** Currently a private method in MultiDiffChatAction. Should be refactored to a public static method in a utility class.
- **Signature:** 
  ```kotlin
  fun getFiles(virtualFiles: Array<out VirtualFile>?, root: Path): Set<Path>
  ```
- **Dependencies:** com.intellij.openapi.vfs.VirtualFile, java.nio.file.Path


#### Function 2: codeSummary
- **Description:** Generates a markdown summary of code files.
- **Functionality:** Creates a formatted string containing file paths and their contents.
- **Location and Accessibility:** Currently a local function in handle method. Should be extracted and refactored to a public static method.
- **Signature:** 
  ```kotlin
  fun codeSummary(root: Path, codeFiles: Set<Path>): String
  ```
- **Dependencies:** java.nio.file.Path


#### Function 3: openBrowserToUri
- **Description:** Opens the default browser to a specified URI.
- **Functionality:** Uses Desktop API to open a browser with a given URI.
- **Location and Accessibility:** Currently embedded in a thread in the handle method. Should be extracted to a utility class.
- **Signature:** 
  ```kotlin
  fun openBrowserToUri(uri: URI)
  ```
- **Dependencies:** java.awt.Desktop, java.net.URI


#### Class: PatchApp
- **Description:** Handles multi-file patch chat functionality.
- **Functionality:** Manages chat sessions, code summaries, and patch applications.
- **Location and Accessibility:** Inner class of MultiDiffChatAction. Could be extracted to a separate file and made more generic.
- **Signature:** 
  ```kotlin
  class PatchApp(
      override val root: File,
      val codeSummary: () -> String,
      val codeFiles: Set<Path> = setOf()
  ) : ApplicationServer
  ```
- **Dependencies:** Various com.simiacryptus.skyenet.* classes


#### Function 4: renderMarkdown
- **Description:** Renders markdown to HTML.
- **Functionality:** Converts markdown text to HTML for display.
- **Location and Accessibility:** Imported from com.simiacryptus.skyenet.webui.util.MarkdownUtil. Could be centralized in a utility class if used across multiple components.
- **Signature:** 
  ```kotlin
  fun renderMarkdown(markdown: String): String
  ```
- **Dependencies:** com.simiacryptus.skyenet.webui.util.MarkdownUtil

These shared functionalities could be extracted and refactored into utility classes or more generic components to promote code reuse across the project. This would improve maintainability and reduce duplication in the codebase.# generic\PlanAheadAction.kt


## Shared Functionality Analysis: PlanAheadAction.kt


### Code Overview
- **Language & Frameworks:** Kotlin, IntelliJ IDEA Plugin Development
- **Dependencies:** 
  - IntelliJ Platform SDK
  - AppSettingsState
  - UITools
  - Simiacryptus libraries (jopenai, skyenet)
  - SLF4J for logging


### Common Logic

Several functions and classes in this file could be useful across multiple components. Here's an analysis of the potential shared functionality:


#### Function 1: expandFileList
- **Description:** Expands a list of VirtualFile objects, filtering out certain file types and directories.
- **Functionality:** Recursively processes an array of VirtualFile objects, excluding hidden files, large files, certain file extensions, and expanding directories.
- **Location and Accessibility:** This is already a companion object function in the PlanAheadAgent class. It could be moved to a separate utility class for broader use.
- **Signature:** 
  ```kotlin
  fun expandFileList(data: Array<VirtualFile>): Array<VirtualFile>
  ```
- **Dependencies:** VirtualFile from IntelliJ Platform SDK


#### Function 2: executionOrder
- **Description:** Determines the execution order of tasks based on their dependencies.
- **Functionality:** Takes a map of tasks and returns a list of task IDs in the order they should be executed, considering dependencies.
- **Location and Accessibility:** This is already a companion object function in the PlanAheadAgent class. It could be moved to a separate utility class for task management.
- **Signature:** 
  ```kotlin
  fun executionOrder(tasks: Map<String, Task>): List<String>
  ```
- **Dependencies:** Task data class


#### Function 3: buildMermaidGraph
- **Description:** Builds a Mermaid graph representation of tasks and their dependencies.
- **Functionality:** Generates a Mermaid graph string from a map of tasks, including task descriptions and dependencies.
- **Location and Accessibility:** This is a private function in the PlanAheadAgent class. It could be extracted and made public in a utility class for graph generation.
- **Signature:** 
  ```kotlin
  fun buildMermaidGraph(subTasks: Map<String, Task>): String
  ```
- **Dependencies:** Task data class


#### Function 4: sanitizeForMermaid
- **Description:** Sanitizes strings for use in Mermaid graphs.
- **Functionality:** Escapes special characters and formats strings for Mermaid compatibility.
- **Location and Accessibility:** This is a private function in the PlanAheadAgent class. It could be extracted and made public in a utility class for string manipulation.
- **Signature:** 
  ```kotlin
  fun sanitizeForMermaid(input: String): String
  ```
- **Dependencies:** None


#### Function 5: escapeMermaidCharacters
- **Description:** Escapes special characters for Mermaid graph labels.
- **Functionality:** Escapes quotation marks and wraps the string in quotes.
- **Location and Accessibility:** This is a private function in the PlanAheadAgent class. It could be extracted and made public in a utility class for string manipulation.
- **Signature:** 
  ```kotlin
  fun escapeMermaidCharacters(input: String): String
  ```
- **Dependencies:** None


#### Class: PlanAheadSettings
- **Description:** Data class for storing settings for the PlanAhead action.
- **Functionality:** Holds configuration options for the PlanAhead action.
- **Location and Accessibility:** This is already a data class within PlanAheadAction. It could be moved to a separate file for better organization.
- **Signature:** 
  ```kotlin
  data class PlanAheadSettings(
      var model: String = AppSettingsState.instance.smartModel,
      var temperature: Double = AppSettingsState.instance.temperature,
      var enableTaskPlanning: Boolean = false,
      var enableShellCommands: Boolean = true
  )
  ```
- **Dependencies:** AppSettingsState


#### Class: GenState
- **Description:** Data class for managing the state of task generation and execution.
- **Functionality:** Holds various maps and lists to track task states, results, and processing queues.
- **Location and Accessibility:** This is a data class within PlanAheadAgent. It could be extracted to a separate file for better modularity.
- **Signature:** 
  ```kotlin
  data class GenState(
      val subTasks: Map<String, Task>,
      val tasksByDescription: MutableMap<String?, Task> = ...,
      val taskIdProcessingQueue: MutableList<String> = ...,
      val taskResult: MutableMap<String, String> = mutableMapOf(),
      val completedTasks: MutableList<String> = mutableListOf(),
      val taskFutures: MutableMap<String, Future<*>> = mutableMapOf(),
      val uitaskMap: MutableMap<String, SessionTask> = mutableMapOf()
  )
  ```
- **Dependencies:** Task data class, SessionTask

These functions and classes could be refactored into separate utility files or a shared library to promote code reuse across different parts of the plugin or even in other projects. This would improve modularity and make the codebase more maintainable.# generic\SessionProxyApp.kt


## Shared Functionality Analysis: SessionProxyApp.kt


### Code Overview
- **Language & Frameworks:** Kotlin, likely using a web framework (possibly Spring Boot or Ktor)
- **Dependencies:** 
  - com.simiacryptus.skyenet.core.platform
  - com.simiacryptus.skyenet.webui.application
  - com.simiacryptus.skyenet.webui.chat
  - com.simiacryptus.skyenet.webui.session


### Common Logic

This file doesn't contain many standalone functions that could be easily shared across components. However, there are some elements that could potentially be useful in other parts of the application:


#### SessionProxyServer class
- **Description:** A server class that manages sessions and chat functionality
- **Functionality:** Creates new sessions, manages agents and chats
- **Location and Accessibility:** Already a public class, but could be made more modular
- **Signature:** `class SessionProxyServer : ApplicationServer`
- **Dependencies:** ApplicationServer, User, Session, SocketManager, ChatServer

While this class itself isn't a shared function, it contains logic that could be extracted and made more reusable:


#### Potential Shared Function: createNewSession
- **Description:** Creates a new session for a user
- **Functionality:** Determines whether to create a new chat session or use an existing agent
- **Location and Accessibility:** Currently part of the SessionProxyServer class, could be extracted as a standalone function
- **Potential Signature:** 
  ```kotlin
  fun createNewSession(user: User?, session: Session, chats: Map<Session, ChatServer>, agents: Map<Session, SocketManager>): Any
  ```
- **Dependencies:** User, Session, ChatServer, SocketManager


#### Companion Object
- **Description:** Contains shared state for agents and chats
- **Functionality:** Stores mappings of sessions to SocketManagers and ChatServers
- **Location and Accessibility:** Already accessible as a companion object
- **Signature:** N/A (it's an object, not a function)
- **Dependencies:** Session, SocketManager, ChatServer

While not a function, this shared state management could be useful in other parts of the application. It might be worth considering turning this into a more generic session management utility class.

To make the code more modular and reusable, you could consider:

1. Extracting the session creation logic into a separate utility class.
2. Creating a more generic session management class that could be used across different parts of the application.
3. Implementing interfaces for the SessionProxyServer to make it easier to swap out different implementations if needed.

These refactorings would make the code more flexible and easier to maintain, while also providing opportunities for shared functionality across the application.# generic\ShellCommandAction.kt


## Shared Functionality Analysis: ShellCommandAction.kt


### Code Overview
- **Language & Frameworks:** Kotlin, IntelliJ Platform SDK
- **Dependencies:** 
  - AppServer
  - BaseAction
  - AppSettingsState
  - UITools
  - Various IntelliJ Platform classes
  - Skyenet libraries (com.simiacryptus.skyenet)
  - Java AWT and Swing


### Common Logic


#### Function 1: getSelectedFolder
- **Description:** Retrieves the selected folder from an AnActionEvent
- **Functionality:** Extracts the selected folder from the event context
- **Location and Accessibility:** Currently part of UITools, could be made public static
- **Signature:** `fun getSelectedFolder(event: AnActionEvent): VirtualFile?`
- **Dependencies:** AnActionEvent, VirtualFile


#### Function 2: newGlobalID
- **Description:** Generates a new global ID for a session
- **Functionality:** Creates a unique identifier for a new session
- **Location and Accessibility:** Part of StorageInterface, already accessible
- **Signature:** `fun newGlobalID(): String`
- **Dependencies:** None


#### Function 3: createApplicationServer
- **Description:** Creates a new ApplicationServer instance
- **Functionality:** Initializes an ApplicationServer with specific configurations
- **Location and Accessibility:** Could be extracted and made public static
- **Signature:** `fun createApplicationServer(name: String, path: String, showMenubar: Boolean): ApplicationServer`
- **Dependencies:** ApplicationServer


#### Function 4: openBrowserToUri
- **Description:** Opens the default browser to a specific URI
- **Functionality:** Uses Desktop API to open a browser with a given URI
- **Location and Accessibility:** Could be extracted and made public static
- **Signature:** `fun openBrowserToUri(uri: URI)`
- **Dependencies:** java.awt.Desktop, java.net.URI


#### Function 5: createCodingAgent
- **Description:** Creates a CodingAgent instance for shell command execution
- **Functionality:** Initializes a CodingAgent with specific configurations for shell command execution
- **Location and Accessibility:** Could be extracted and made more generic
- **Signature:** `fun createCodingAgent(api: API, dataStorage: StorageInterface, session: Session, user: User?, ui: ApplicationInterface, workingDir: String, shellCommand: String): CodingAgent<ProcessInterpreter>`
- **Dependencies:** CodingAgent, ProcessInterpreter, AppSettingsState, various Skyenet classes

These functions represent common logic that could be potentially shared across different components of the plugin. Extracting and refactoring them into public static methods would improve code reusability and maintainability.# generic\ReactTypescriptWebDevelopmentAssistantAction.kt


## Shared Functionality Analysis: ReactTypescriptWebDevelopmentAssistantAction.kt


### Code Overview
- **Language & Frameworks:** Kotlin, IntelliJ IDEA Plugin Development, React, TypeScript
- **Dependencies:** 
  - IntelliJ Platform SDK
  - Simiacryptus libraries (jopenai, skyenet)
  - Java AWT
  - SLF4J for logging


### Common Logic


#### Function 1: extractCode
- **Description:** Extracts code from a string that may contain markdown code blocks.
- **Functionality:** Removes surrounding markdown code block syntax if present.
- **Location and Accessibility:** Already a private function in the WebDevAgent class. Could be made public and static for wider use.
- **Signature:** 
  ```kotlin
  fun extractCode(code: String): String
  ```
- **Dependencies:** None


#### Function 2: write (for images)
- **Description:** Writes an image to a ByteArray.
- **Functionality:** Converts an ImageResponse to a ByteArray for file saving.
- **Location and Accessibility:** Private function in WebDevAgent class. Could be made public and static.
- **Signature:** 
  ```kotlin
  fun write(code: ImageResponse, path: Path): ByteArray
  ```
- **Dependencies:** Java ImageIO, ByteArrayOutputStream


#### Function 3: codeSummary
- **Description:** Generates a summary of code files in the project.
- **Functionality:** Concatenates the content of all non-image files in the project with file headers.
- **Location and Accessibility:** Function in WebDevAgent class. Could be extracted and made public static.
- **Signature:** 
  ```kotlin
  fun codeSummary(): String
  ```
- **Dependencies:** File I/O operations


#### Function 4: draftResourceCode
- **Description:** Drafts code for a specific file using an AI actor.
- **Functionality:** Generates code content for a given file path using AI assistance.
- **Location and Accessibility:** Private function in WebDevAgent class. Could be generalized and made public static.
- **Signature:** 
  ```kotlin
  fun draftResourceCode(
    task: SessionTask,
    request: Array<ApiModel.ChatMessage>,
    actor: SimpleActor,
    path: Path,
    vararg languages: String
  )
  ```
- **Dependencies:** Simiacryptus libraries, SessionTask, SimpleActor


#### Function 5: draftImage
- **Description:** Drafts an image file using an AI actor.
- **Functionality:** Generates image content for a given file path using AI assistance.
- **Location and Accessibility:** Private function in WebDevAgent class. Could be generalized and made public static.
- **Signature:** 
  ```kotlin
  fun draftImage(
    task: SessionTask,
    request: Array<ApiModel.ChatMessage>,
    actor: ImageActor,
    path: Path
  )
  ```
- **Dependencies:** Simiacryptus libraries, SessionTask, ImageActor


#### Function 6: iterateCode
- **Description:** Refines code through multiple iterations using AI review.
- **Functionality:** Applies AI-based code review and refinement to the project's code files.
- **Location and Accessibility:** Private function in WebDevAgent class. Could be extracted and made more generic.
- **Signature:** 
  ```kotlin
  fun iterateCode(task: SessionTask)
  ```
- **Dependencies:** Simiacryptus libraries, SessionTask, codeReviewer actor

These functions represent core functionalities that could be useful across different components of the plugin or even in other similar projects. Extracting and refactoring them into public static methods in a utility class would improve reusability and maintainability of the codebase.# generic\WebDevelopmentAssistantAction.kt


## Shared Functionality Analysis: WebDevelopmentAssistantAction.kt


### Code Overview
- **Language & Frameworks:** Kotlin, IntelliJ IDEA Plugin Development
- **Dependencies:** 
  - IntelliJ Platform SDK
  - Skyenet (custom library)
  - JOpenAI (custom library)
  - SLF4J for logging


### Common Logic


#### Function 1: extractCode
- **Description:** Extracts code from a string that may contain markdown-style code blocks.
- **Functionality:** Removes surrounding code block markers and trims whitespace.
- **Location and Accessibility:** Currently a private method in WebDevAgent class. Can be refactored to be a public static method.
- **Signature:** 
  ```kotlin
  fun extractCode(code: String): String
  ```
- **Dependencies:** None


#### Function 2: codeSummary
- **Description:** Generates a summary of code files in the project.
- **Functionality:** Concatenates the content of all non-image files in the project with file headers.
- **Location and Accessibility:** Currently a method in WebDevAgent class. Can be refactored to be a public static method.
- **Signature:** 
  ```kotlin
  fun codeSummary(codeFiles: Set<Path>, root: File): String
  ```
- **Dependencies:** Java NIO for file operations


#### Function 3: draftResourceCode
- **Description:** Drafts code for a specific resource file using an AI actor.
- **Functionality:** Generates code, extracts it from the AI response, and saves it to a file.
- **Location and Accessibility:** Currently a private method in WebDevAgent class. Can be refactored to be more generic and public.
- **Signature:** 
  ```kotlin
  fun draftResourceCode(
    task: SessionTask,
    request: Array<ApiModel.ChatMessage>,
    actor: SimpleActor,
    path: Path,
    vararg languages: String
  )
  ```
- **Dependencies:** Skyenet SessionTask, JOpenAI ApiModel, custom SimpleActor class


#### Function 4: draftImage
- **Description:** Drafts an image file using an AI actor.
- **Functionality:** Generates an image, saves it to a file, and displays it in the UI.
- **Location and Accessibility:** Currently a private method in WebDevAgent class. Can be refactored to be more generic and public.
- **Signature:** 
  ```kotlin
  fun draftImage(
    task: SessionTask,
    request: Array<ApiModel.ChatMessage>,
    actor: ImageActor,
    path: Path
  )
  ```
- **Dependencies:** Skyenet SessionTask, JOpenAI ApiModel, custom ImageActor class, Java ImageIO


#### Function 5: iterateCode
- **Description:** Iteratively refines code using an AI code reviewer.
- **Functionality:** Summarizes code, sends it to a code reviewer, and applies suggested changes.
- **Location and Accessibility:** Currently a private method in WebDevAgent class. Can be refactored to be more generic and public.
- **Signature:** 
  ```kotlin
  fun iterateCode(task: SessionTask, codeFiles: Set<Path>, root: File, codeReviewer: SimpleActor, ui: ApplicationInterface)
  ```
- **Dependencies:** Skyenet SessionTask, custom SimpleActor class, ApplicationInterface

These functions represent core functionalities that could be useful across multiple components of the plugin or even in other similar projects. Refactoring them into public static methods in a utility class would improve their reusability and maintainability.# git\ChatWithCommitAction.kt


## Shared Functionality Analysis: ChatWithCommitAction.kt


### Code Overview
- **Language & Frameworks:** Kotlin, IntelliJ Platform SDK
- **Dependencies:** 
  - Various IntelliJ Platform APIs
  - Custom utility classes (AppServer, SessionProxyServer, CodeChatSocketManager, IdeaOpenAIClient)
  - SkyeNet library components


### Common Logic


#### Function 1: String.isBinary
- **Description:** Extension property to check if a string is likely binary content
- **Functionality:** Determines if a string contains a significant proportion of non-printable characters
- **Location and Accessibility:** Already defined as a top-level extension property, can be easily reused
- **Signature:** `val String.isBinary: Boolean`
- **Dependencies:** None


#### Function 2: expand
- **Description:** Recursively expands an array of VirtualFiles, including contents of directories
- **Functionality:** Flattens a directory structure into a list of all contained files
- **Location and Accessibility:** Currently a private method in ChatWithCommitAction, should be refactored to a utility class
- **Signature:** `fun expand(data: Array<VirtualFile>?): Array<VirtualFile>?`
- **Dependencies:** IntelliJ Platform's VirtualFile


#### Function 3: openChatWithDiff
- **Description:** Opens a chat interface with diff information
- **Functionality:** Sets up a chat session with commit changes and opens it in a browser
- **Location and Accessibility:** Currently a private method in ChatWithCommitAction, could be generalized and moved to a utility class
- **Signature:** `fun openChatWithDiff(e: AnActionEvent, diffInfo: String)`
- **Dependencies:** 
  - SessionProxyServer
  - CodeChatSocketManager
  - IdeaOpenAIClient
  - AppSettingsState
  - ApplicationServices
  - ApplicationServer
  - AppServer


#### Function 4: formatChangesForChat
- **Description:** Formats VCS changes into a readable diff format for chat
- **Functionality:** Processes VCS changes and generates a formatted string representation
- **Location and Accessibility:** Not currently extracted, but can be refactored from the actionPerformed method
- **Signature:** `fun formatChangesForChat(changes: List<Change>, files: Array<VirtualFile>?): String`
- **Dependencies:** 
  - IntelliJ Platform's VCS APIs
  - DiffUtil

These functions represent common logic that could be useful across multiple components of the plugin. The `isBinary` property is already easily reusable. The `expand`, `openChatWithDiff`, and `formatChangesForChat` functions would need to be refactored into utility classes to make them more accessible and reusable across the plugin.# git\ChatWithCommitDiffAction.kt


## Shared Functionality Analysis: ChatWithCommitDiffAction.kt


### Code Overview
- **Language & Frameworks:** Kotlin, IntelliJ Platform SDK, Git4Idea
- **Dependencies:** AppServer, SessionProxyServer, AppSettingsState, CodeChatSocketManager, IdeaOpenAIClient, ApplicationServices, StorageInterface, ApplicationServer, Git4Idea


### Common Logic


#### Function 1: getChangesBetweenCommits
- **Description:** Retrieves the diff between a selected commit and the current HEAD.
- **Functionality:** Executes a git diff command and returns the output as a string.
- **Location and Accessibility:** This function can be extracted and made public static.
- **Signature:** 
  ```kotlin
  fun getChangesBetweenCommits(repository: GitRepository, selectedCommit: VcsRevisionNumber): String
  ```
- **Dependencies:** Git4Idea


#### Function 2: openChatWithDiff
- **Description:** Opens a chat session with the diff information.
- **Functionality:** Sets up a CodeChatSocketManager, configures the session, and opens a browser to the chat interface.
- **Location and Accessibility:** This function can be extracted and made public static, but may need some refactoring to remove dependencies on AnActionEvent.
- **Signature:** 
  ```kotlin
  fun openChatWithDiff(project: Project, diffInfo: String)
  ```
- **Dependencies:** AppServer, SessionProxyServer, AppSettingsState, CodeChatSocketManager, IdeaOpenAIClient, ApplicationServices, StorageInterface, ApplicationServer


#### Function 3: setupChatSession
- **Description:** Sets up a chat session with given parameters.
- **Functionality:** Creates a CodeChatSocketManager and configures the ApplicationServer session.
- **Location and Accessibility:** This function can be extracted from openChatWithDiff and made public static.
- **Signature:** 
  ```kotlin
  fun setupChatSession(session: String, language: String, codeSelection: String, filename: String): CodeChatSocketManager
  ```
- **Dependencies:** AppSettingsState, CodeChatSocketManager, IdeaOpenAIClient, ApplicationServices, StorageInterface, ApplicationServer


#### Function 4: openBrowserToSession
- **Description:** Opens a browser to the chat session URL.
- **Functionality:** Constructs the session URL and opens the default browser.
- **Location and Accessibility:** This function can be extracted from openChatWithDiff and made public static.
- **Signature:** 
  ```kotlin
  fun openBrowserToSession(server: AppServer, session: String)
  ```
- **Dependencies:** AppServer, Desktop API

These functions represent common logic that could be useful across multiple components dealing with Git diffs, chat sessions, and browser interactions within the IntelliJ plugin ecosystem. Extracting and refactoring these functions would improve code reusability and maintainability.# generic\SimpleCommandAction.kt


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
- **Functionality:** Traverses directories and collects file paths, excluding certain files and directories based on specific criteria
- **Location and Accessibility:** Already a public static method in the companion object
- **Signature:** 
  ```kotlin
  fun getFiles(virtualFiles: Array<out VirtualFile>?): MutableSet<Path>
  ```
- **Dependencies:** VirtualFile from IntelliJ Platform SDK


#### Function 2: toPaths
- **Description:** Converts a string path (potentially with wildcards) to a list of actual file paths
- **Functionality:** Expands wildcards in file paths and returns matching file paths
- **Location and Accessibility:** Already a public static method in the companion object
- **Signature:** 
  ```kotlin
  fun toPaths(root: Path, it: String): Iterable<Path>
  ```
- **Dependencies:** java.nio.file.Path, kotlin.io.path.ExperimentalPathApi


#### Function 3: codeSummary
- **Description:** Generates a summary of code files
- **Functionality:** Creates a markdown-formatted summary of specified code files
- **Location and Accessibility:** Currently a method in the PatchApp inner class. Could be refactored into a standalone function.
- **Signature:** 
  ```kotlin
  fun codeSummary(paths: List<Path>): String
  ```
- **Dependencies:** None


#### Function 4: projectSummary
- **Description:** Generates a summary of the project structure
- **Functionality:** Creates a list of project files with their sizes
- **Location and Accessibility:** Currently a method in the PatchApp inner class. Could be refactored into a standalone function.
- **Signature:** 
  ```kotlin
  fun projectSummary(): String
  ```
- **Dependencies:** None


#### Function 5: getUserSettings
- **Description:** Retrieves user settings based on the current action event
- **Functionality:** Determines the working directory and selected files from the action event
- **Location and Accessibility:** Currently a private method in SimpleCommandAction. Could be made public and static.
- **Signature:** 
  ```kotlin
  fun getUserSettings(event: AnActionEvent?): Settings?
  ```
- **Dependencies:** AnActionEvent from IntelliJ Platform SDK, UITools from the project

These functions provide common functionality for file handling, project summarization, and user settings retrieval, which could be useful across multiple components in the project. Some refactoring might be needed to make them more accessible and reusable across the codebase.# git\ChatWithWorkingCopyDiffAction.kt


## Shared Functionality Analysis: ChatWithWorkingCopyDiffAction.kt


### Code Overview
- **Language & Frameworks:** Kotlin, IntelliJ Platform SDK
- **Dependencies:** Git4Idea, OpenAI API, SkyeNet, Swing


### Common Logic


#### Function 1: getChangesBetweenHeadAndWorkingCopy
- **Description:** Retrieves the differences between the HEAD and the working copy of a Git repository.
- **Functionality:** Executes a Git diff command and returns the output as a string.
- **Location and Accessibility:** Currently a private method in ChatWithWorkingCopyDiffAction. Could be refactored into a public static method in a utility class.
- **Signature:** 
  ```kotlin
  fun getChangesBetweenHeadAndWorkingCopy(repository: GitRepository): String
  ```
- **Dependencies:** Git4Idea


#### Function 2: openChatWithDiff
- **Description:** Opens a chat interface with the diff information.
- **Functionality:** Sets up a CodeChatSocketManager, configures the ApplicationServer, and opens a browser to the chat interface.
- **Location and Accessibility:** Currently a private method in ChatWithWorkingCopyDiffAction. Could be refactored into a more general method for opening chats with different types of content.
- **Signature:** 
  ```kotlin
  fun openChatWithDiff(e: AnActionEvent, diffInfo: String)
  ```
- **Dependencies:** AppServer, SessionProxyServer, CodeChatSocketManager, IdeaOpenAIClient, AppSettingsState, ApplicationServices, Desktop


#### Function 3: actionPerformed
- **Description:** Handles the action when the user triggers the ChatWithWorkingCopyDiffAction.
- **Functionality:** Retrieves the Git repository, gets the diff, and opens the chat interface.
- **Location and Accessibility:** This is the main action method and should remain in the ChatWithWorkingCopyDiffAction class.
- **Signature:** 
  ```kotlin
  override fun actionPerformed(e: AnActionEvent)
  ```
- **Dependencies:** VcsDataKeys, GitRepositoryManager


#### Function 4: update
- **Description:** Updates the visibility and enabled state of the action based on the current context.
- **Functionality:** Checks if the current VCS is Git and enables/disables the action accordingly.
- **Location and Accessibility:** This method is specific to the action and should remain in the ChatWithWorkingCopyDiffAction class.
- **Signature:** 
  ```kotlin
  override fun update(e: AnActionEvent)
  ```
- **Dependencies:** VcsDataKeys, GitVcs

The `getChangesBetweenHeadAndWorkingCopy` and `openChatWithDiff` functions could potentially be refactored into utility classes to be used across multiple components dealing with Git diffs and chat interfaces, respectively. This would improve code reusability and maintainability across the project.# legacy\AppendTextWithChatAction.kt


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

The AppendTextWithChatAction class doesn't contain any public static functions that could be easily extracted for shared functionality. However, there are some logical bits that could potentially be useful across multiple components if refactored:


#### Function 1: Create Chat Request
- **Description:** Creates a ChatRequest object with system and user messages
- **Functionality:** Prepares a chat request with predefined system message and user-provided text
- **Location and Accessibility:** Currently part of processSelection method, needs refactoring
- **Signature:** 
  ```kotlin
  fun createChatRequest(systemMessage: String, userMessage: String, model: String, temperature: Double): ChatRequest
  ```
- **Dependencies:** AppSettingsState, ApiModel.ChatRequest, ApiModel.ChatMessage, ApiModel.Role


#### Function 2: Process Chat Response
- **Description:** Processes the chat response and appends it to the original text
- **Functionality:** Extracts the generated content from the chat response and appends it to the original text, avoiding duplication
- **Location and Accessibility:** Currently part of processSelection method, needs refactoring
- **Signature:** 
  ```kotlin
  fun processAndAppendChatResponse(originalText: String, chatResponse: ChatResponse): String
  ```
- **Dependencies:** ApiModel.ChatResponse


#### Function 3: Check Legacy Action Enablement
- **Description:** Checks if legacy actions are enabled in the application settings
- **Functionality:** Returns a boolean indicating whether legacy actions should be enabled
- **Location and Accessibility:** Currently part of isEnabled method, could be made static
- **Signature:** 
  ```kotlin
  fun isLegacyActionEnabled(): Boolean
  ```
- **Dependencies:** AppSettingsState

These functions would need to be extracted and refactored to be more general and reusable across different components. They could potentially be placed in a utility class or a shared service within the project structure.# legacy\CommentsAction.kt


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

The CommentsAction class doesn't contain many standalone functions that could be easily extracted for shared use. However, there are some logical components that could potentially be generalized and shared across similar actions:


#### Function 1: Language Support Check
- **Description:** Checks if a given computer language is supported for the action
- **Functionality:** Determines if the action should be enabled for a specific language
- **Location and Accessibility:** Currently part of the CommentsAction class, could be extracted to a utility class
- **Signature:** `fun isLanguageSupported(computerLanguage: ComputerLanguage?): Boolean`
- **Dependencies:** ComputerLanguage enum


#### Function 2: ChatProxy Creation
- **Description:** Creates a ChatProxy instance with specific settings
- **Functionality:** Initializes a ChatProxy with application-specific settings
- **Location and Accessibility:** Currently part of the processSelection method, could be extracted to a utility class
- **Signature:** `fun createChatProxy(api: API, clazz: Class<*>): ChatProxy`
- **Dependencies:** AppSettingsState, ChatProxy, API interface


#### Function 3: Code Editing Request
- **Description:** Sends a request to edit code with specific instructions
- **Functionality:** Formats and sends a request to modify code based on given parameters
- **Location and Accessibility:** Currently part of the processSelection method, could be generalized for various code modification actions
- **Signature:** `fun editCode(code: String, instructions: String, language: String, humanLanguage: String): String`
- **Dependencies:** ChatProxy, CommentsAction_VirtualAPI interface

These functions would need to be extracted and refactored to be more general-purpose and independent of the specific CommentsAction class. They could then be placed in a utility class or a base class for similar actions, allowing for easier reuse across multiple components in the project.# git\ReplicateCommitAction.kt


## Shared Functionality Analysis: ReplicateCommitAction.kt


### Code Overview
- **Language & Frameworks:** Kotlin, IntelliJ Platform SDK
- **Dependencies:** 
  - IntelliJ Platform SDK
  - com.simiacryptus.jopenai
  - com.simiacryptus.skyenet
  - com.simiacryptus.diff


### Common Logic


#### Function 1: generateDiffInfo
- **Description:** Generates a diff information string from a list of changes.
- **Functionality:** Processes an array of Changes and VirtualFiles to create a formatted diff string.
- **Location and Accessibility:** This function is currently private within ReplicateCommitAction. It could be refactored into a public static method in a utility class.
- **Signature:** 
  ```kotlin
  fun generateDiffInfo(files: Array<VirtualFile>?, changes: Array<out Change>?): String
  ```
- **Dependencies:** 
  - com.intellij.openapi.vcs.changes.Change
  - com.intellij.openapi.vfs.VirtualFile
  - com.simiacryptus.diff.DiffUtil


#### Function 2: getFiles
- **Description:** Recursively collects all file paths from given virtual files.
- **Functionality:** Traverses directories and collects file paths, excluding certain directories and files.
- **Location and Accessibility:** This function is currently private within ReplicateCommitAction. It could be refactored into a public static method in a utility class.
- **Signature:** 
  ```kotlin
  fun getFiles(virtualFiles: Array<out VirtualFile>?): MutableSet<Path>
  ```
- **Dependencies:** 
  - com.intellij.openapi.vfs.VirtualFile
  - java.nio.file.Path


#### Function 3: expand
- **Description:** Expands an array of VirtualFiles, including all files within directories.
- **Functionality:** Recursively expands directories into individual files.
- **Location and Accessibility:** This function is currently private within ReplicateCommitAction. It could be refactored into a public static method in a utility class.
- **Signature:** 
  ```kotlin
  fun expand(data: Array<VirtualFile>?): Array<VirtualFile>?
  ```
- **Dependencies:** 
  - com.intellij.openapi.vfs.VirtualFile


#### Function 4: toPaths
- **Description:** Converts a string path (potentially with wildcards) to a list of actual file paths.
- **Functionality:** Handles wildcard expansion in file paths.
- **Location and Accessibility:** This function is already a companion object method, making it effectively static. It could be moved to a separate utility class for broader use.
- **Signature:** 
  ```kotlin
  fun toPaths(root: Path, it: String): Iterable<Path>
  ```
- **Dependencies:** 
  - java.nio.file.Path

These functions provide utility for file handling, diff generation, and path manipulation, which could be useful across multiple components in a project dealing with version control systems and file operations within an IDE environment.# legacy\DocAction.kt


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
- **Functionality:** Takes code, operation, computer language, and human language as input and returns converted text
- **Location and Accessibility:** Part of DocAction_VirtualAPI interface, could be extracted as a standalone function
- **Signature:** 
  ```kotlin
  fun processCode(
      code: String,
      operation: String,
      computerLanguage: String,
      humanLanguage: String
  ): DocAction_ConvertedText
  ```
- **Dependencies:** None


#### Function 2: isLanguageSupported
- **Description:** Checks if a given computer language is supported for documentation
- **Functionality:** Verifies if the language is not Text and has a non-empty docStyle
- **Location and Accessibility:** Already a public method in DocAction class, could be made static
- **Signature:** 
  ```kotlin
  fun isLanguageSupported(computerLanguage: ComputerLanguage?): Boolean
  ```
- **Dependencies:** ComputerLanguage


#### Function 3: editSelection
- **Description:** Edits the selection range based on the PSI structure
- **Functionality:** Finds the appropriate code block and returns its start and end offsets
- **Location and Accessibility:** Already a public method in DocAction class, could be made static
- **Signature:** 
  ```kotlin
  fun editSelection(state: EditorState, start: Int, end: Int): Pair<Int, Int>
  ```
- **Dependencies:** PsiUtil


#### Function 4: createChatProxy
- **Description:** Creates a ChatProxy for the DocAction_VirtualAPI
- **Functionality:** Initializes a ChatProxy with specific settings and examples
- **Location and Accessibility:** Currently part of the lazy initialization of 'proxy', could be extracted as a standalone function
- **Signature:** 
  ```kotlin
  fun createChatProxy(): DocAction_VirtualAPI
  ```
- **Dependencies:** ChatProxy, AppSettingsState

These functions represent common logic that could potentially be shared across multiple components. They would need to be refactored to be more general and independent of the specific DocAction class to be truly reusable across the project.# legacy\ImplementStubAction.kt


## Shared Functionality Analysis: ImplementStubAction.kt


### Code Overview
- **Language & Frameworks:** Kotlin, IntelliJ Platform SDK
- **Dependencies:** 
  - com.github.simiacryptus.aicoder.actions.SelectionAction
  - com.github.simiacryptus.aicoder.config.AppSettingsState
  - com.github.simiacryptus.aicoder.util.ComputerLanguage
  - com.github.simiacryptus.aicoder.util.psi.PsiUtil
  - com.simiacryptus.jopenai.proxy.ChatProxy
  - com.simiacryptus.jopenai.util.StringUtil


### Common Logic


#### Function 1: getProxy
- **Description:** Creates and returns a ChatProxy instance for the VirtualAPI interface.
- **Functionality:** Initializes a ChatProxy with specific settings from AppSettingsState.
- **Location and Accessibility:** Already a private method, could be made public static if needed.
- **Signature:** 
  ```kotlin
  fun getProxy(): VirtualAPI
  ```
- **Dependencies:** AppSettingsState, ChatProxy, VirtualAPI interface


#### Function 2: isLanguageSupported
- **Description:** Checks if a given computer language is supported by the action.
- **Functionality:** Verifies that the language is not null and not ComputerLanguage.Text.
- **Location and Accessibility:** Already an override method, could be extracted as a public static utility.
- **Signature:** 
  ```kotlin
  fun isLanguageSupported(computerLanguage: ComputerLanguage?): Boolean
  ```
- **Dependencies:** ComputerLanguage enum


#### Function 3: defaultSelection
- **Description:** Determines the default text selection range for the action.
- **Functionality:** Finds the smallest code range within the editor context.
- **Location and Accessibility:** Already an override method, could be extracted as a public static utility.
- **Signature:** 
  ```kotlin
  fun defaultSelection(editorState: EditorState, offset: Int): Pair<Int, Int>
  ```
- **Dependencies:** PsiUtil


#### Function 4: processSelection
- **Description:** Processes the selected text to implement a stub.
- **Functionality:** Extracts relevant code context and uses a VirtualAPI to generate stub implementation.
- **Location and Accessibility:** Already an override method, core logic could be extracted as a public static utility.
- **Signature:** 
  ```kotlin
  fun processSelection(state: SelectionState, config: String?): String
  ```
- **Dependencies:** AppSettingsState, StringUtil, VirtualAPI

These functions contain logic that could be useful across multiple components, especially for actions that involve code selection, language support checking, and stub implementation. To make them more reusable, they could be extracted into a separate utility class, with some refactoring to remove dependencies on specific action classes where possible.# legacy\InsertImplementationAction.kt


## Shared Functionality Analysis: InsertImplementationAction.kt


### Code Overview
- **Language & Frameworks:** Kotlin, IntelliJ Platform SDK
- **Dependencies:** 
  - AppSettingsState
  - SelectionAction
  - ComputerLanguage
  - TextBlock
  - UITools
  - PsiClassContext
  - PsiUtil
  - ChatProxy


### Common Logic


#### Function 1: getProxy()
- **Description:** Creates and returns a ChatProxy instance for the VirtualAPI interface.
- **Functionality:** Initializes a ChatProxy with specific parameters for API communication.
- **Location and Accessibility:** Already a private method, could be made public static if needed elsewhere.
- **Signature:** 
  ```kotlin
  fun getProxy(): VirtualAPI
  ```
- **Dependencies:** AppSettingsState, ChatProxy, VirtualAPI interface


#### Function 2: getPsiClassContextActionParams()
- **Description:** Extracts PsiClassContextActionParams from a SelectionState.
- **Functionality:** Processes selection state to create parameters for PsiClassContext.
- **Location and Accessibility:** Private method, could be refactored to be public static.
- **Signature:** 
  ```kotlin
  fun getPsiClassContextActionParams(state: SelectionState): PsiClassContextActionParams
  ```
- **Dependencies:** SelectionState, PsiUtil, ContextRange


#### Function 3: processSelection()
- **Description:** Processes the selected text to insert an implementation.
- **Functionality:** Extracts context, calls API to generate code, and formats the result.
- **Location and Accessibility:** Override method, core logic could be extracted to a separate utility function.
- **Signature:** 
  ```kotlin
  override fun processSelection(state: SelectionState, config: String?): String
  ```
- **Dependencies:** AppSettingsState, UITools, PsiClassContext, VirtualAPI


#### Function 4: defaultSelection()
- **Description:** Determines the default selection range for the action.
- **Functionality:** Finds the smallest comment in the context or falls back to the current line.
- **Location and Accessibility:** Override method, logic could be extracted to a utility function.
- **Signature:** 
  ```kotlin
  override fun defaultSelection(editorState: EditorState, offset: Int): Pair<Int, Int>
  ```
- **Dependencies:** PsiUtil


#### Function 5: editSelection()
- **Description:** Adjusts the selection range based on the context.
- **Functionality:** Similar to defaultSelection, but used for editing existing selections.
- **Location and Accessibility:** Override method, logic could be combined with defaultSelection in a utility function.
- **Signature:** 
  ```kotlin
  override fun editSelection(state: EditorState, start: Int, end: Int): Pair<Int, Int>
  ```
- **Dependencies:** PsiUtil

These functions contain logic that could be useful in other actions or components dealing with code selection, context extraction, and API-based code generation. Refactoring them into more general utility functions could enhance reusability across the project.# legacy\RenameVariablesAction.kt


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
- **Description:** Suggests variable renames based on the given code and language context.
- **Functionality:** Uses AI to analyze code and suggest improved variable names.
- **Location and Accessibility:** Currently part of the RenameAPI interface. Could be extracted as a standalone utility function.
- **Signature:** 
  ```kotlin
  fun suggestRenames(code: String, computerLanguage: String, humanLanguage: String): SuggestionResponse
  ```
- **Dependencies:** Requires access to AI model (currently using ChatProxy)


#### Function 2: choose
- **Description:** Presents a dialog for users to select which variable renames to apply.
- **Functionality:** Displays a checkbox dialog with rename suggestions and returns the selected items.
- **Location and Accessibility:** Currently a method in RenameVariablesAction. Could be extracted as a utility function.
- **Signature:** 
  ```kotlin
  fun choose(renameSuggestions: Map<String, String>): Set<String>
  ```
- **Dependencies:** UITools.showCheckboxDialog


#### Function 3: processSelection
- **Description:** Processes the selected text to apply chosen variable renames.
- **Functionality:** Gets rename suggestions, lets user choose which to apply, and applies the selected renames.
- **Location and Accessibility:** Currently a method in RenameVariablesAction. Core logic could be extracted as a utility function.
- **Signature:** 
  ```kotlin
  fun processSelection(event: AnActionEvent?, state: SelectionState, config: String?): String
  ```
- **Dependencies:** suggestRenames, choose, UITools.run


#### Function 4: isLanguageSupported
- **Description:** Checks if the given computer language is supported for variable renaming.
- **Functionality:** Currently only excludes plain text.
- **Location and Accessibility:** Could be extracted as a utility function for language support checks.
- **Signature:** 
  ```kotlin
  fun isLanguageSupported(computerLanguage: ComputerLanguage?): Boolean
  ```
- **Dependencies:** ComputerLanguage enum

These functions represent core functionalities that could be useful across multiple components dealing with code analysis, refactoring, and user interaction in the context of an IDE plugin. Extracting them as standalone utility functions would improve reusability and maintainability of the codebase.# legacy\ReplaceWithSuggestionsAction.kt


## Shared Functionality Analysis: ReplaceWithSuggestionsAction.kt


### Code Overview
- **Language & Frameworks:** Kotlin, IntelliJ Platform SDK
- **Dependencies:** 
  - com.github.simiacryptus.aicoder.actions.SelectionAction
  - com.github.simiacryptus.aicoder.config.AppSettingsState
  - com.github.simiacryptus.aicoder.util.UITools
  - com.intellij.openapi.actionSystem.ActionUpdateThread
  - com.intellij.openapi.actionSystem.AnActionEvent
  - com.intellij.openapi.project.Project
  - com.simiacryptus.jopenai.proxy.ChatProxy
  - com.simiacryptus.jopenai.util.StringUtil


### Common Logic


#### Function 1: getSuffixForContext
- **Description:** Gets a suffix of a given string with a specified ideal length.
- **Functionality:** Extracts a suffix from a string, replacing newlines with spaces.
- **Location and Accessibility:** Currently part of the processSelection method. Needs to be extracted and made public static.
- **Signature:** `fun getSuffixForContext(text: String, idealLength: Int): String`
- **Dependencies:** None


#### Function 2: getPrefixForContext
- **Description:** Gets a prefix of a given string with a specified ideal length.
- **Functionality:** Extracts a prefix from a string, replacing newlines with spaces.
- **Location and Accessibility:** Currently part of the processSelection method. Needs to be extracted and made public static.
- **Signature:** `fun getPrefixForContext(text: String, idealLength: Int): String`
- **Dependencies:** None


#### Function 3: calculateIdealLength
- **Description:** Calculates an ideal length based on the input text length.
- **Functionality:** Uses a logarithmic formula to determine an ideal length for context.
- **Location and Accessibility:** Currently part of the processSelection method. Needs to be extracted and made public static.
- **Signature:** `fun calculateIdealLength(textLength: Int): Int`
- **Dependencies:** kotlin.math (pow, ceil, ln)


#### Function 4: createChatProxy
- **Description:** Creates a ChatProxy instance for the VirtualAPI interface.
- **Functionality:** Initializes a ChatProxy with specific settings from AppSettingsState.
- **Location and Accessibility:** Currently part of the proxy property. Could be extracted and made public static.
- **Signature:** `fun createChatProxy(api: Any, clazz: Class<T>): T`
- **Dependencies:** com.simiacryptus.jopenai.proxy.ChatProxy, com.github.simiacryptus.aicoder.config.AppSettingsState


#### Function 5: showOptionsDialog
- **Description:** Displays a dialog with radio button options for user selection.
- **Functionality:** Shows a dialog with given choices and returns the selected option.
- **Location and Accessibility:** Currently part of the choose method. Could be extracted and made public static.
- **Signature:** `fun showOptionsDialog(title: String, options: Array<String>): String?`
- **Dependencies:** com.github.simiacryptus.aicoder.util.UITools

These functions represent common logic that could be useful across multiple components in the project. Extracting and refactoring them into public static methods would improve code reusability and maintainability.# markdown\MarkdownImplementActionGroup.kt


## Shared Functionality Analysis: MarkdownImplementActionGroup.kt


### Code Overview
- **Language & Frameworks:** Kotlin, IntelliJ Platform SDK
- **Dependencies:** OpenAI API (via ChatProxy)


### Common Logic


#### Function 1: isEnabled
- **Description:** Checks if the action should be enabled based on the current context
- **Functionality:** Verifies if the current language is Markdown and if there's a text selection
- **Location and Accessibility:** Already a companion object function, can be made public if needed
- **Signature:** `fun isEnabled(e: AnActionEvent): Boolean`
- **Dependencies:** ComputerLanguage, UITools


#### Function 2: getProxy
- **Description:** Creates a proxy for the ConversionAPI interface using ChatProxy
- **Functionality:** Sets up a ChatProxy with specific parameters for text conversion
- **Location and Accessibility:** Currently a private method in MarkdownImplementAction, could be extracted and made more generic
- **Signature:** `fun getProxy(): ConversionAPI`
- **Dependencies:** ChatProxy, AppSettingsState


#### Function 3: implement (ConversionAPI)
- **Description:** Converts text from one language to another
- **Functionality:** Takes input text, source language, and target language to produce converted text
- **Location and Accessibility:** Interface method, could be extracted into a separate utility class
- **Signature:** `fun implement(text: String, humanLanguage: String, computerLanguage: String): ConvertedText`
- **Dependencies:** None (interface method)


#### Function 4: processSelection
- **Description:** Processes the selected text and converts it to the target programming language
- **Functionality:** Uses the ConversionAPI to convert text and formats it as a Markdown code block
- **Location and Accessibility:** Currently part of MarkdownImplementAction, could be generalized for other similar actions
- **Signature:** `fun processSelection(state: SelectionState, config: String?): String`
- **Dependencies:** ConversionAPI


#### Function 5: getChildren
- **Description:** Generates a list of actions for different programming languages
- **Functionality:** Creates MarkdownImplementAction instances for each supported language
- **Location and Accessibility:** Already a public method in MarkdownImplementActionGroup
- **Signature:** `fun getChildren(e: AnActionEvent?): Array<AnAction>`
- **Dependencies:** MarkdownImplementAction

These functions represent core functionalities that could be useful across multiple components, especially for actions dealing with text conversion, Markdown processing, and dynamic action generation based on supported languages. Some refactoring might be needed to make them more generic and accessible as independent utility functions.# legacy\VoiceToTextAction.kt


## Shared Functionality Analysis: VoiceToTextAction.kt


### Code Overview
- **Language & Frameworks:** Kotlin, IntelliJ Platform SDK
- **Dependencies:** 
  - com.github.simiacryptus.aicoder (custom package)
  - com.intellij.openapi.actionSystem
  - com.simiacryptus.jopenai.audio
  - javax.sound.sampled
  - javax.swing
  - java.util.concurrent


### Common Logic


#### Function 1: Audio Recording
- **Description:** Records audio input from the system's microphone
- **Functionality:** Captures raw audio data and stores it in a buffer
- **Location and Accessibility:** Currently embedded in the `handle` method. Could be extracted as a separate function.
- **Signature:** `fun recordAudio(buffer: ConcurrentLinkedDeque<ByteArray>, continueFn: () -> Boolean)`
- **Dependencies:** AudioRecorder, ConcurrentLinkedDeque


#### Function 2: Audio Processing
- **Description:** Processes raw audio data into a format suitable for speech-to-text conversion
- **Functionality:** Converts raw audio data into WAV format and applies loudness windowing
- **Location and Accessibility:** Currently embedded in the `handle` method. Could be extracted as a separate function.
- **Signature:** `fun processAudio(inputBuffer: ConcurrentLinkedDeque<ByteArray>, outputBuffer: ConcurrentLinkedDeque<ByteArray>, continueFn: () -> Boolean)`
- **Dependencies:** LookbackLoudnessWindowBuffer, ConcurrentLinkedDeque


#### Function 3: Speech-to-Text Conversion
- **Description:** Converts processed audio data into text
- **Functionality:** Sends audio data to an API for transcription and inserts the resulting text into the editor
- **Location and Accessibility:** Currently implemented in the `DictationPump` inner class. Could be refactored into a standalone function.
- **Signature:** `fun convertSpeechToText(audioBuffer: Deque<ByteArray>, api: ApiClient, editor: Editor, offsetStart: Int, continueFn: () -> Boolean)`
- **Dependencies:** ApiClient (assumed), Editor, Deque


#### Function 4: Status Dialog Creation
- **Description:** Creates a status dialog to indicate ongoing dictation
- **Functionality:** Displays a JFrame with a message about the dictation process
- **Location and Accessibility:** Currently implemented as `statusDialog` method. Could be made more generic for reuse.
- **Signature:** `fun createStatusDialog(message: String, location: Point): JFrame`
- **Dependencies:** JFrame, JLabel


#### Function 5: Microphone Availability Check
- **Description:** Checks if a microphone is available for recording
- **Functionality:** Attempts to get a TargetDataLine for audio input
- **Location and Accessibility:** Currently implemented in the `isEnabled` method and `companion object`. Could be extracted as a standalone utility function.
- **Signature:** `fun isMicrophoneAvailable(): Boolean`
- **Dependencies:** AudioSystem, TargetDataLine

These functions represent common logic that could potentially be shared across multiple components dealing with audio recording, processing, and speech-to-text conversion. Extracting them into separate, reusable functions would improve modularity and facilitate their use in other parts of the application or in different projects altogether.# markdown\MarkdownListAction.kt


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
- **Functionality:** Searches for a PSI element of type "MarkdownListImpl" that intersects with the current selection.
- **Location and Accessibility:** This function is from PsiUtil and is already accessible as a static method.
- **Signature:** `fun getSmallestIntersecting(psiFile: PsiFile, start: Int, end: Int, type: String): PsiElement?`
- **Dependencies:** IntelliJ Platform SDK (PSI)


#### Function 2: getAll
- **Description:** Retrieves all PSI elements of a specific type within a given PSI element.
- **Functionality:** Used to get all "MarkdownListItemImpl" and "MarkdownParagraphImpl" elements within a list.
- **Location and Accessibility:** This function is from PsiUtil and is already accessible as a static method.
- **Signature:** `fun getAll(element: PsiElement, type: String): List<PsiElement>`
- **Dependencies:** IntelliJ Platform SDK (PSI)


#### Function 3: getIndent
- **Description:** Retrieves the indentation at the current caret position.
- **Functionality:** Used to maintain the indentation of the existing list when adding new items.
- **Location and Accessibility:** This function is from UITools and is already accessible as a static method.
- **Signature:** `fun getIndent(caret: Caret): String`
- **Dependencies:** IntelliJ Platform SDK (Editor)


#### Function 4: insertString
- **Description:** Inserts a string into a document at a specified offset.
- **Functionality:** Used to insert the new list items into the document.
- **Location and Accessibility:** This function is from UITools and is already accessible as a static method.
- **Signature:** `fun insertString(document: Document, offset: Int, text: String)`
- **Dependencies:** IntelliJ Platform SDK (Editor)


#### Function 5: redoableTask
- **Description:** Wraps a task in a redoable command.
- **Functionality:** Ensures that the list generation action can be undone/redone.
- **Location and Accessibility:** This function is from UITools and is already accessible as a static method.
- **Signature:** `fun redoableTask(e: AnActionEvent, task: () -> Unit)`
- **Dependencies:** IntelliJ Platform SDK (Command system)


#### Function 6: run
- **Description:** Runs a task with a progress indicator.
- **Functionality:** Used to show progress while generating new list items.
- **Location and Accessibility:** This function is from UITools and is already accessible as a static method.
- **Signature:** `fun run(project: Project?, title: String, canBeCancelled: Boolean, task: () -> Unit)`
- **Dependencies:** IntelliJ Platform SDK (Progress API)

These functions provide common functionality that could be useful across multiple components, especially for working with Markdown documents, PSI elements, and IntelliJ's editor and action system. They are already accessible as static methods, so no refactoring is needed to make them independent public static methods.# OpenWebPageAction.kt


## Shared Functionality Analysis: OpenWebPageAction.kt


### Code Overview
- **Language & Frameworks:** Kotlin, IntelliJ Platform SDK
- **Dependencies:** Java AWT Desktop, java.net.URI


### Common Logic

The `OpenWebPageAction` class contains functionality that could be generalized and shared across multiple components. Here's an analysis of the potential shared functionality:


#### Function 1: openWebPage
- **Description:** Opens a specified URL in the default web browser.
- **Functionality:** Checks if desktop browsing is supported and opens the given URL.
- **Location and Accessibility:** This functionality is currently embedded in the `actionPerformed` method. It should be extracted into a separate public static method for reuse.
- **Signature:** 
  ```kotlin
  fun openWebPage(url: String): Boolean
  ```
- **Dependencies:** java.awt.Desktop, java.net.URI

To make this functionality more reusable, we can refactor the code as follows:

```kotlin
companion object {
    fun openWebPage(url: String): Boolean {
        return if (Desktop.isDesktopSupported()) {
            val desktop = Desktop.getDesktop()
            if (desktop.isSupported(Desktop.Action.BROWSE)) {
                desktop.browse(URI(url))
                true
            } else {
                false
            }
        } else {
            false
        }
    }
}
```

This refactored version:
1. Extracts the web page opening logic into a separate method.
2. Makes it a companion object function, allowing it to be called without instantiating the class.
3. Returns a boolean to indicate success or failure, which can be useful for error handling in other parts of the application.
4. Accepts a URL as a parameter, making it more flexible for use with different web pages.

The `OpenWebPageAction` class could then use this shared functionality like this:

```kotlin
override fun actionPerformed(event: AnActionEvent) {
    openWebPage("http://apps.simiacrypt.us/")
}
```

This refactoring makes the web page opening functionality more modular and reusable across different parts of the application or even in other projects.# SelectionAction.kt


## Shared Functionality Analysis: SelectionAction.kt


### Code Overview
- **Language & Frameworks:** Kotlin, IntelliJ Platform SDK
- **Dependencies:** Various IntelliJ Platform classes (e.g., AnActionEvent, Editor, PsiFile)


### Common Logic


#### Function 1: retarget
- **Description:** Adjusts the selection range based on the current editor state and selection.
- **Functionality:** Determines the appropriate selection range, considering whether there's an existing selection or if a default selection should be used.
- **Location and Accessibility:** Currently a private method in SelectionAction. Could be refactored to be a protected or public static method.
- **Signature:** 
  ```kotlin
  fun retarget(editorState: EditorState, selectedText: String?, selectionStart: Int, selectionEnd: Int): Pair<Int, Int>?
  ```
- **Dependencies:** Requires EditorState, which could be made into a separate class.


#### Function 2: editorState
- **Description:** Creates an EditorState object from the current Editor.
- **Functionality:** Extracts relevant information from the Editor to create an EditorState object.
- **Location and Accessibility:** Currently a private method in SelectionAction. Could be refactored to be a public static method.
- **Signature:** 
  ```kotlin
  fun editorState(editor: Editor): EditorState
  ```
- **Dependencies:** Requires Editor and PsiFile classes from IntelliJ Platform SDK.


#### Function 3: contextRanges
- **Description:** Extracts context ranges from a PsiFile based on the current editor position.
- **Functionality:** Traverses the PSI tree to find elements that contain the current cursor position and creates ContextRange objects for them.
- **Location and Accessibility:** Currently a private method in SelectionAction. Could be refactored to be a public static method.
- **Signature:** 
  ```kotlin
  fun contextRanges(psiFile: PsiFile?, editor: Editor): Array<ContextRange>
  ```
- **Dependencies:** Requires PsiFile and Editor classes from IntelliJ Platform SDK.


#### Function 4: isLanguageSupported
- **Description:** Checks if a given computer language is supported by the action.
- **Functionality:** Currently a simple null check, but could be extended to support more complex language checks.
- **Location and Accessibility:** Already a protected method in SelectionAction. Could be made public static if needed.
- **Signature:** 
  ```kotlin
  fun isLanguageSupported(computerLanguage: ComputerLanguage?): Boolean
  ```
- **Dependencies:** Requires ComputerLanguage class.


#### Function 5: UITools.redoableTask
- **Description:** Executes a task that can be redone/undone in the IntelliJ environment.
- **Functionality:** Wraps the given task in a redoable/undoable command.
- **Location and Accessibility:** Already a public static method in UITools class.
- **Signature:** 
  ```kotlin
  fun redoableTask(e: AnActionEvent, task: () -> Unit)
  ```
- **Dependencies:** Requires AnActionEvent class from IntelliJ Platform SDK.

These functions provide common functionality that could be useful across multiple components, especially for actions that involve text selection and manipulation in the IntelliJ environment. Some refactoring might be needed to make them more generally accessible and reusable.# problems\AnalyzeProblemAction.kt


## Shared Functionality Analysis: AnalyzeProblemAction.kt


### Code Overview
- **Language & Frameworks:** Kotlin, IntelliJ Platform SDK
- **Dependencies:** 
  - IntelliJ Platform SDK (com.intellij.openapi.*)
  - AppServer
  - SessionProxyServer
  - IdeaOpenAIClient
  - Skyenet library (com.simiacryptus.skyenet.*)
  - JsonUtil


### Common Logic


#### Function 1: findGitRoot
- **Description:** Finds the Git root directory for a given file
- **Functionality:** Traverses up the directory tree to find the Git root
- **Location and Accessibility:** Already a companion object method in TestResultAutofixAction, could be moved to a utility class
- **Signature:** `fun findGitRoot(file: VirtualFile): VirtualFile?`
- **Dependencies:** IntelliJ VirtualFile


#### Function 2: getProjectStructure
- **Description:** Generates a string representation of the project structure
- **Functionality:** Traverses the project directory and creates a tree-like structure
- **Location and Accessibility:** Already a companion object method in TestResultAutofixAction, could be moved to a utility class
- **Signature:** `fun getProjectStructure(root: VirtualFile): String`
- **Dependencies:** IntelliJ VirtualFile


#### Function 3: buildProblemInfo
- **Description:** Builds a string containing detailed information about a problem
- **Functionality:** Gathers information about the file, problem, and surrounding context
- **Location and Accessibility:** Currently part of actionPerformed, could be extracted as a separate method
- **Signature:** `fun buildProblemInfo(project: Project, file: VirtualFile, item: ProblemNode, gitRoot: VirtualFile?): String`
- **Dependencies:** IntelliJ Project, VirtualFile, ProblemNode


#### Function 4: openAnalysisSession
- **Description:** Opens a new analysis session for a problem
- **Functionality:** Creates a new session and opens a browser window for analysis
- **Location and Accessibility:** Already a separate private method, could be made public and static
- **Signature:** `fun openAnalysisSession(project: Project, problemInfo: String, gitRoot: VirtualFile?)`
- **Dependencies:** AppServer, SessionProxyServer, ProblemAnalysisApp


#### Function 5: analyzeProblem
- **Description:** Analyzes a problem and suggests fixes
- **Functionality:** Uses AI to analyze the problem, identify errors, and suggest fixes
- **Location and Accessibility:** Currently part of ProblemAnalysisApp, could be extracted as a separate utility method
- **Signature:** `fun analyzeProblem(ui: ApplicationInterface, task: SessionTask, problemInfo: String, gitRoot: VirtualFile?)`
- **Dependencies:** IdeaOpenAIClient, Skyenet library, JsonUtil


#### Function 6: generateAndAddResponse
- **Description:** Generates a response with suggested fixes and adds it to the UI
- **Functionality:** Uses AI to generate fixes and formats them as clickable links in the UI
- **Location and Accessibility:** Currently part of ProblemAnalysisApp, could be extracted as a separate utility method
- **Signature:** `fun generateAndAddResponse(ui: ApplicationInterface, task: SessionTask, error: ParsedError, summary: String, filesToFix: List<String>, root: File): String`
- **Dependencies:** IdeaOpenAIClient, Skyenet library, AppSettingsState

These functions represent common logic that could be useful across multiple components of the plugin. Some refactoring would be needed to make them more general and accessible, possibly by moving them to a shared utility class.# test\TestResultAutofixAction.kt


## Shared Functionality Analysis: TestResultAutofixAction.kt


### Code Overview
- **Language & Frameworks:** Kotlin, IntelliJ IDEA Plugin Development
- **Dependencies:** 
  - IntelliJ Platform SDK
  - SkyeNet library
  - JOpenAI library


### Common Logic


#### Function 1: getFiles
- **Description:** Recursively retrieves all files from a given set of virtual files or paths.
- **Functionality:** Traverses directories and collects file paths, excluding hidden files and those in .gitignore.
- **Location and Accessibility:** Already a companion object method, can be made public static.
- **Signature:** 
  ```kotlin
  fun getFiles(virtualFiles: Array<out VirtualFile>?): MutableSet<Path>
  fun getFiles(virtualFiles: Array<out Path>?): MutableSet<Path>
  ```
- **Dependencies:** VirtualFile (IntelliJ SDK), Path (Java NIO)


#### Function 2: getProjectStructure
- **Description:** Generates a string representation of the project structure.
- **Functionality:** Lists all files in the project with their sizes, excluding large files.
- **Location and Accessibility:** Already a companion object method, can be made public static.
- **Signature:** 
  ```kotlin
  fun getProjectStructure(projectPath: VirtualFile?): String
  fun getProjectStructure(root: Path): String
  ```
- **Dependencies:** VirtualFile (IntelliJ SDK), Path (Java NIO)


#### Function 3: findGitRoot
- **Description:** Finds the root directory of a Git repository.
- **Functionality:** Traverses up the directory tree to find the .git folder.
- **Location and Accessibility:** Already a companion object method, can be made public static.
- **Signature:** 
  ```kotlin
  fun findGitRoot(path: Path?): Path?
  fun findGitRoot(virtualFile: VirtualFile?): VirtualFile?
  ```
- **Dependencies:** Path (Java NIO), VirtualFile (IntelliJ SDK)


#### Function 4: getTestInfo
- **Description:** Extracts information from a test proxy object.
- **Functionality:** Collects test name, duration, error message, and stacktrace.
- **Location and Accessibility:** Currently a private method, can be refactored to be public static.
- **Signature:** 
  ```kotlin
  fun getTestInfo(testProxy: SMTestProxy): String
  ```
- **Dependencies:** SMTestProxy (IntelliJ SDK)


#### Function 5: openAutofixWithTestResult
- **Description:** Opens a new session for test result autofix.
- **Functionality:** Creates a new session, sets up the application server, and opens a browser.
- **Location and Accessibility:** Currently a private method, may need significant refactoring to be generalized.
- **Signature:** 
  ```kotlin
  fun openAutofixWithTestResult(e: AnActionEvent, testInfo: String, projectStructure: String)
  ```
- **Dependencies:** AnActionEvent (IntelliJ SDK), AppServer, SessionProxyServer, ApplicationServer

These functions provide useful utilities for file handling, project structure analysis, Git repository detection, and test result processing. They could be extracted into a separate utility class to be used across multiple components of the plugin.