# BaseAction.kt

The `BaseAction` class serves as an abstract foundation for creating actions within an IntelliJ IDEA plugin, providing a structured way to define actions with optional names, descriptions, and icons. It extends the `AnAction` class from the IntelliJ Platform SDK, allowing it to integrate seamlessly with the IDE's action system. This class encapsulates common functionalities and configurations needed for actions, including logging, API access, and UI interactions.


#### Key Components and Logic


##### Constructor Parameters
- `name`: An optional parameter for the name of the action. If provided, it will be displayed in the UI.
- `description`: An optional parameter for a short description of what the action does. This can be shown in the UI to provide more context to the user.
- `icon`: An optional parameter for an icon to represent the action visually in the IDE.


##### Properties
- `log`: A lazy-initialized logger instance specific to the subclass, used for logging messages and errors.
- `api`: A getter property that provides access to an instance of `OpenAIClient`, facilitating interactions with OpenAI's API through a custom client tailored for IntelliJ IDEA.


##### Overridden Methods
- `update(event: AnActionEvent)`: This method is called to determine whether the action is available and visible based on the current context. It sets the `isEnabledAndVisible` property of the action's presentation according to the result of `isEnabled(event)`.
- `actionPerformed(e: AnActionEvent)`: The main method that is invoked when the action is triggered. It logs the action, sets the last event in `IdeaOpenAIClient`, and calls the `handle(e: AnActionEvent)` method. If an exception occurs during execution, it is caught and logged as an error.


##### Abstract Methods
- `handle(e: AnActionEvent)`: An abstract method that subclasses must implement to define the action's behavior when it is executed.


##### Utility Methods
- `isEnabled(event: AnActionEvent)`: An open method that determines whether the action should be enabled based on the given `AnActionEvent`. By default, it returns `true`, indicating that the action is enabled in all contexts. Subclasses can override this method to provide context-specific logic.


##### Companion Object
- Contains a static logger instance for the class and a scheduled thread pool with a single thread. The thread pool can be used for executing tasks asynchronously.


#### Usage
To create a custom action using the `BaseAction` class, one must extend this class and implement the `handle(e: AnActionEvent)` method to define what the action does. Optionally, the `isEnabled(event: AnActionEvent)` method can be overridden to control the availability of the action based on the context in which it is invoked.

This class simplifies the process of creating actions for IntelliJ IDEA plugins by providing a structured approach to handle common requirements such as logging, API interactions, and UI updates.

# code\DescribeAction.kt

The `DescribeAction` class is part of a package designed to enhance coding environments with AI-powered features. It extends the `SelectionAction<String>` class, indicating it performs an action based on a selected portion of text within the code editor and returns a `String` result. This class is specifically tasked with generating descriptions for selected code snippets, leveraging a virtual API to communicate with an AI model for natural language processing.


#### Key Components and Their Functions


##### DescribeAction_VirtualAPI Interface
- **Purpose**: Serves as a contract for the AI-powered code description service. It defines the `describeCode` method that the `DescribeAction` class must implement. This method takes a piece of code, the programming language of the code, and the desired human language for the description as inputs, and returns a `DescribeAction_ConvertedText` object containing the AI-generated description.
- **DescribeAction_ConvertedText Class**: Nested within the `DescribeAction_VirtualAPI` interface, this class is a simple data holder for the AI-generated description (`text`) and the language of the description (`language`).


##### Proxy Property
- **Purpose**: Provides access to an instance of the `DescribeAction_VirtualAPI`, created via a `ChatProxy`. This proxy setup allows for dynamic interaction with an AI model specified in the `AppSettingsState` configuration, including parameters like temperature and model choice.
- **Configuration**: Utilizes settings from `AppSettingsState` for customizing the AI's behavior, such as response generation temperature and the choice of AI model.


##### getConfig Method
- **Purpose**: Overrides a method from the parent class to provide configuration for the action. In its current implementation, it returns an empty string, indicating no additional configuration is required or it's yet to be implemented.


##### processSelection Method
- **Purpose**: The core logic of the `DescribeAction` class, responsible for processing the selected text, generating a description via the AI proxy, and formatting the description according to the code's comment style.
- **Steps**:
  1. Extracts and trims the selected text.
  2. Calls the `describeCode` method of the proxy to get an AI-generated description of the code snippet.
  3. Formats the description to fit within a specified character width (120 characters in this case) and determines the appropriate comment style based on the number of lines in the description and the programming language of the selected text.
  4. Builds and returns a string that combines the formatted description and the original selected text, properly indented and commented.


#### Summary
The `DescribeAction` class introduces an AI-powered feature into the coding environment, allowing developers to generate natural language descriptions for code snippets directly within their IDE. This can enhance code readability and documentation efforts, especially in collaborative settings. The class leverages a virtual API to interact with an AI model, dynamically adjusting its behavior based on user-configured settings.

# code\CustomEditAction.kt

The `CustomEditAction` class extends the `SelectionAction` class, specifically tailored to handle custom code editing actions within an IDE environment. This class is part of a larger system designed to integrate AI-powered code editing capabilities into the development workflow. Below is a detailed explanation of its components, focusing on the prompt text, configuration, and the closely related logic.


#### Overview

`CustomEditAction` is designed to allow users to apply custom edits to their code using a virtual AI assistant. The class interfaces with a virtual API to process code editing requests based on user instructions.


#### Key Components


##### VirtualAPI Interface

- **Purpose**: Defines the contract for the AI-powered code editing service.
- **Methods**:
  - `editCode`: Accepts the original code, an operation (instruction), the programming language of the code, and the human language of the instruction. It returns an `EditedText` object containing the edited code and its language.

- **EditedText Data Class**:
  - Holds the edited code (`code`) and its language (`language`). These fields are nullable, allowing for the possibility that the editing process might not result in changes.


##### Proxy Property

- **Purpose**: Provides access to the virtual AI assistant through a `ChatProxy` instance.
- **Configuration**:
  - The `ChatProxy` is configured with the `VirtualAPI` class, API settings from `AppSettingsState`, and model preferences. It also includes an example to guide the AI in understanding the expected output format and context.


##### getConfig Method

- **Purpose**: Fetches a user-provided instruction for editing the code.
- **Implementation**:
  - Displays an input dialog where the user can enter the editing instruction.
  - The method uses `UITools.showInputDialog` to present the dialog, with "Edit Code" as the title and "Instruction:" as the prompt text.
  - The input (instruction) is returned as a `String`. If no input is provided, an empty string is returned.


##### processSelection Method

- **Purpose**: Processes the selected code based on the user's instruction.
- **Logic**:
  - Checks if the instruction is null or blank. If so, it returns the originally selected text.
  - Retrieves the current settings and the preferred human language from `AppSettingsState`.
  - Records the instruction in the application's history for future reference.
  - Calls the `editCode` method of the `proxy` to request code editing based on the instruction, selected text, and language settings.
  - Returns the edited code if available; otherwise, it falls back to the originally selected text.


#### Conclusion

The `CustomEditAction` class encapsulates the logic for integrating AI-powered custom code edits into the development workflow. It leverages a virtual API to process editing instructions, providing an interface for users to enhance their code with the assistance of AI. Through a combination of user input and automated processing, it aims to streamline the code editing process, making it more efficient and tailored to the user's needs.

# code\CommentsAction.kt

This Kotlin class, `CommentsAction`, extends the `SelectionAction<String>` to provide functionality for adding comments to a selected block of code within an IDE environment. The class is part of a package designed to integrate AI capabilities into coding workflows, specifically for generating comments in code. Below is a detailed breakdown of its components and functionalities:


#### Class Overview
- **Package Name:** `com.github.simiacryptus.aicoder.actions.code`
- **Dependencies:** The class imports necessary modules for its operation, including project configuration (`AppSettingsState`), utility classes (`ComputerLanguage`), and IntelliJ project components (`Project`). It also utilizes a `ChatProxy` for interacting with an AI model.


#### Key Methods and Interfaces


##### `getConfig(project: Project?): String`
- **Description:** Retrieves configuration settings for the action. In its current implementation, it returns an empty string, indicating no specific configuration is required or implemented.
- **Parameters:** 
  - `project`: An optional `Project` instance representing the current project context.
- **Returns:** A `String` representing the action's configuration. Currently, returns an empty string.


##### `isLanguageSupported(computerLanguage: ComputerLanguage?): Boolean`
- **Description:** Determines if the action supports the provided programming language.
- **Parameters:**
  - `computerLanguage`: An optional `ComputerLanguage` enum value representing the programming language of the code selection.
- **Returns:** A `Boolean` value indicating whether the action supports the specified programming language. It returns `true` for all languages except `ComputerLanguage.Text`, indicating it's designed for programming languages rather than plain text.


##### `processSelection(state: SelectionState, config: String?): String`
- **Description:** Processes the selected code block to add comments. It utilizes a `ChatProxy` to communicate with an AI model specified in the application settings, sending the selected text and receiving the commented code.
- **Parameters:**
  - `state`: A `SelectionState` object containing details about the selected code block, including the text and programming language.
  - `config`: An optional `String` representing additional configuration. Currently, it is not used in the method.
- **Returns:** A `String` containing the commented code. If the AI model fails to generate comments, an empty string is returned.


##### `CommentsAction_VirtualAPI` Interface
- **Purpose:** Defines the contract for interacting with the AI model to edit code.
- **Method:** `editCode(code: String, operations: String, computerLanguage: String, humanLanguage: String): CommentsAction_ConvertedText`
  - **Parameters:**
    - `code`: The code block to be commented.
    - `operations`: A `String` describing the operation to be performed, e.g., "Add comments to each line explaining the code".
    - `computerLanguage`: The programming language of the code.
    - `humanLanguage`: The language in which the comments should be written.
  - **Returns:** An instance of `CommentsAction_ConvertedText`, containing the commented code.


##### `CommentsAction_ConvertedText` Class
- **Description:** Serves as a data container for the AI-generated commented code.
- **Fields:**
  - `code`: A nullable `String` holding the commented code.
  - `language`: A nullable `String` indicating the language of the comments (not actively used in the current implementation).


#### Usage Scenario
This class is designed to be used within an IDE plugin or extension where users can select a block of code and trigger the action to automatically generate and insert comments explaining the code, leveraging AI capabilities. The action checks if the selected code's language is supported (not plain text) and then communicates with an AI model to perform the commenting.

# code\InsertImplementationAction.kt

The `InsertImplementationAction` class is part of a system designed to enhance coding efficiency by automatically generating code implementations based on given specifications. This class extends `SelectionAction<String>`, indicating it operates on selections within the editor and returns a `String` result. The core functionality revolves around interpreting selected text or comments as specifications for code to be implemented, then using an AI model to generate the corresponding code. Below is a detailed breakdown of its components and logic:


#### VirtualAPI Interface
- **Purpose**: Defines the contract for implementing code generation. It specifies the `implementCode` method, which takes a specification, a prefix, computer language, and human language as inputs and returns a `ConvertedText` object containing the generated code and its language.
- **ConvertedText Class**: Nested within `VirtualAPI`, this class holds the generated code (`code`) and the language of the code (`language`).


#### getProxy Method
- **Functionality**: Creates and returns an instance of `VirtualAPI` using `ChatProxy`, configured with settings from `AppSettingsState` (such as the AI model and temperature for code generation).


#### getConfig Method
- **Purpose**: Overrides the `getConfig` method from `SelectionAction`. Currently, it returns an empty string, indicating no additional configuration is required for this action.


#### defaultSelection and editSelection Methods
- **Functionality**: These methods determine the default text selection for code generation based on the editor's state. They prioritize the smallest comment block intersecting with the current selection or cursor position as the source of the specification for code generation.


#### processSelection Method
- **Core Logic**: Processes the selected text to generate code implementation. It performs several steps:
  1. Determines the human and computer languages from the current context.
  2. Extracts the specification from the selected text or the largest intersecting comment.
  3. Converts the specification into a format suitable for the AI model.
  4. Calls the `implementCode` method of the `VirtualAPI` proxy with the prepared specification and context information.
  5. Inserts the generated code into the editor, following the selected text.


#### getPsiClassContextActionParams Method
- **Purpose**: Extracts and prepares parameters related to the PSI (Program Structure Interface) class context, which are required for generating code in the correct context.


#### isLanguageSupported Method
- **Functionality**: Determines if the action supports the given computer language, excluding Text and Markdown languages to ensure the action is used for actual code generation.

This class leverages IntelliJ Platform's infrastructure, such as `runReadAction` for thread-safe operations and `UITools` for UI-related tasks, to integrate AI-powered code generation seamlessly into the development workflow.

# code\ImplementStubAction.kt

The `ImplementStubAction` class is designed to facilitate the implementation of stub methods in code by leveraging a virtual API to edit and transform code snippets. This class is part of a larger system aimed at enhancing coding efficiency and automation within an IDE environment. Below is a detailed explanation of its components and functionalities:


#### Overview

- **Package and Imports**: The class is part of the `com.github.simiacryptus.aicoder.actions.code` package and imports various utilities and configurations necessary for its operation, including project settings, language utilities, and proxy services for interacting with an external API.

- **Class Definition**: `ImplementStubAction` extends `SelectionAction<String>`, indicating it performs an action based on a selected portion of text within the editor and returns a `String` result.


#### Inner Interface: `VirtualAPI`

- **Purpose**: Defines a contract for interacting with a virtual API that can edit code based on specified parameters.
- **Method `editCode`**: Takes a code snippet, an operation type, the computer language of the code, and the human language for instructions. It returns a `ConvertedText` object containing the edited code and its language.


#### Class Components


##### Private Methods

- **`getProxy()`**: Creates and returns a proxy instance of `VirtualAPI`, configured with settings from `AppSettingsState` and using `ChatProxy` for communication.


##### Overridden Methods

- **`isLanguageSupported(computerLanguage: ComputerLanguage?)`**: Determines if the given programming language is supported by the action, excluding plain text.
- **`defaultSelection(editorState: EditorState, offset: Int)`**: Calculates and returns the default selection range within the editor, prioritizing code elements.
- **`getConfig(project: Project?)`**: Returns a configuration string for the action, currently implemented to return an empty string.
- **`processSelection(state: SelectionState, config: String?)`**: The core method where the selected code is processed. It extracts the relevant code snippet, determines the smallest intersecting method context, and calls the `editCode` method of the virtual API to implement the stub based on the selected code.


#### Logic Flow

1. **Initialization**: When an instance of `ImplementStubAction` is created, it is ready to interact with the virtual API through a proxy.
2. **Action Invocation**: Upon invoking the action (e.g., through a user command in the IDE), the `processSelection` method is called with the current selection state and configuration.
3. **Selection Processing**:
   - Extracts the selected text and additional context (e.g., the smallest intersecting method).
   - Cleans up the declaration by removing the intersecting method context and any trailing characters.
4. **API Interaction**: Calls the `editCode` method of the virtual API proxy with the cleaned-up declaration and additional parameters (operation type, languages).
5. **Result Handling**: The edited code returned by the API is then made available for further use, such as replacing the original selection in the editor.


#### Configuration and Customization

- The behavior of `ImplementStubAction` can be influenced by settings in `AppSettingsState`, such as the default chat model and temperature for the API interaction.
- The supported languages and default selections can be adjusted by modifying the `isLanguageSupported` and `defaultSelection` methods, respectively.

This class exemplifies how modern IDE functionalities can be extended with AI and external APIs to automate and assist in code development tasks.

# code\RecentCodeEditsAction.kt

This Kotlin class, `RecentCodeEditsAction`, extends `ActionGroup` and is designed for use within an IntelliJ platform-based IDE. It dynamically generates a list of actions based on the user's recent custom code edits, allowing quick access and execution of these edits. Below is a detailed breakdown of its components and functionality:


#### Class Overview
- **Package**: `com.github.simiacryptus.aicoder.actions.code`
- **Imports**: Utilizes various imports from the IntelliJ platform SDK, the project's configuration settings, and utility classes.
- **Class Definition**: `RecentCodeEditsAction` inherits from `ActionGroup`, indicating it is not a singular action but a group of actions.


#### Methods


##### `update(e: AnActionEvent)`
- **Purpose**: Updates the visibility and enabled state of the action group based on certain conditions.
- **Parameters**: `e: AnActionEvent` - The event that triggered the update.
- **Logic**:
  - Calls `isEnabled(e)` to determine if the action group should be enabled and visible. This depends on whether there is a selection in the IDE and if the current language is not plain text.
  - Invokes `super.update(e)` to ensure any additional update logic defined in the superclass is executed.


##### `getChildren(e: AnActionEvent?): Array<AnAction>`
- **Purpose**: Dynamically generates the child actions based on the user's recent custom code edits.
- **Parameters**: `e: AnActionEvent?` - The event that may trigger this method. It can be `null`.
- **Returns**: An array of `AnAction` objects, each representing a recent custom code edit.
- **Logic**:
  - Checks if the event is `null` and returns an empty array if true.
  - Retrieves the user's recent commands tagged as "customEdits" from `AppSettingsState`.
  - Iterates over these commands, creating a new `CustomEditAction` for each, setting its text to include a numeric identifier and the command itself.
  - These actions are collected and returned as an array.


##### `isEnabled(e: AnActionEvent): Boolean`
- **Purpose**: Determines if the action group should be enabled based on the current context.
- **Parameters**: `e: AnActionEvent` - The event that triggered the check.
- **Returns**: `Boolean` indicating whether the action group is enabled.
- **Logic**:
  - Checks if there is a current selection in the IDE using `UITools.hasSelection(e)`.
  - Determines the current programming language and checks if it is not plain text.
  - Returns `true` if there is a selection and the language is not plain text, otherwise `false`.


#### Configuration and Logic
- The class makes use of `AppSettingsState` to retrieve the user's recent custom code edits, demonstrating how to access and utilize IDE settings.
- It employs a companion object to contain shared logic (`isEnabled`) that determines the action group's availability based on the current context, such as the selected text and programming language.
- The `getChildren` method showcases how to dynamically create and configure actions within the IntelliJ platform, including setting their presentation properties (text, description, icon).

This class is a practical example of extending IntelliJ platform functionality to provide users with quick access to their recent custom code edits, enhancing productivity and streamlining the development workflow.

# code\DocAction.kt

The `DocAction` class extends the `SelectionAction` class to provide functionality for generating documentation comments for selected code blocks within an IDE. This action utilizes a virtual API, represented by the `DocAction_VirtualAPI` interface, to process the selected code and generate the corresponding documentation text. The documentation process involves interaction with a chat model proxy, which is configured to generate documentation in both the computer's programming language and the user's preferred human language.


#### Key Components and Logic


##### `DocAction_VirtualAPI` Interface
- **Purpose**: Defines the contract for processing code to generate documentation.
- **Key Method**: `processCode(code: String, operation: String, computerLanguage: String, humanLanguage: String)`: Takes the selected code block, the type of documentation operation, the programming language of the code, and the preferred human language for the documentation. It returns an instance of `DocAction_ConvertedText`, which contains the generated documentation text and its language.


##### `DocAction_ConvertedText` Class
- **Purpose**: Serves as a container for the generated documentation text and its language.
- **Fields**:
  - `text: String?`: The generated documentation text.
  - `language: String?`: The language of the generated documentation (e.g., English).


##### `proxy: DocAction_VirtualAPI`
- **Purpose**: Lazily initialized proxy for interacting with the chat model to generate documentation.
- **Configuration**: Utilizes `ChatProxy` with settings from `AppSettingsState` (e.g., default chat model, temperature) and retries logic. It also includes an example to guide the model in generating documentation.


##### `processSelection(state: SelectionState, config: String?)`
- **Purpose**: Processes the selected code block to generate and prepend documentation.
- **Logic**:
  1. Extracts the selected text from the `SelectionState`.
  2. Converts the selected text into an `IndentedText` object to handle indentation properly.
  3. Calls the `proxy.processCode` method with the indented code block, operation description, programming language, and human language to generate the documentation.
  4. Prepends the generated documentation to the original code block.


##### `isLanguageSupported(computerLanguage: ComputerLanguage?)`
- **Purpose**: Determines if the given programming language is supported for documentation generation.
- **Logic**: Checks if the language is not `Text`, has a defined `docStyle`, and the `docStyle` is not blank.


##### `editSelection(state: EditorState, start: Int, end: Int)`
- **Purpose**: Adjusts the selection range to include the entire code block for documentation.
- **Logic**: Utilizes `PsiUtil.getCodeElement` to find the code block within the PSI tree and adjusts the selection range to cover the entire block.


#### Configuration and Usage
- **Configuration**: The action requires no explicit configuration string (`getConfig` returns an empty string).
- **Usage**: Triggering this action in an IDE with a supported programming language selected will generate and insert documentation comments based on the selected code block. The generated documentation language and style depend on the IDE project settings and the user's preferences set in `AppSettingsState`.

This class demonstrates an innovative approach to integrating AI-based documentation generation within development environments, enhancing developer productivity by automating the creation of descriptive comments for code blocks.

# code\PasteAction.kt


#### Class Overview

The `PasteAction` class extends the `SelectionAction` class, specifically designed for handling paste actions within the context of the AI Coder plugin. This action facilitates the conversion of clipboard content into a different programming language using an external API, represented by the `VirtualAPI` interface. The conversion process is tailored to the target language of the current selection state.


#### Key Components


##### VirtualAPI Interface

- **Purpose**: Serves as a contract for implementing the conversion of text from one programming language to another.
- **Methods**: 
  - `convert(text: String, from_language: String, to_language: String)`: Converts the given text from the source language to the target language. Returns an instance of `ConvertedText`.
- **Inner Class**: `ConvertedText` holds the result of the conversion, including the converted code (`code`) and the language of the converted code (`language`).


##### Overridden Methods

- `getConfig(project: Project?)`: Returns a configuration string for the action. In this implementation, it returns an empty string as the configuration is not utilized.
  
- `processSelection(state: SelectionState, config: String?)`: Processes the selected text for conversion. It uses the `ChatProxy` to communicate with the external API, passing the clipboard content, auto-detecting the source language, and specifying the target language based on the current selection state. Returns the converted code or an empty string if conversion fails.

- `isLanguageSupported(computerLanguage: ComputerLanguage?)`: Determines if the paste action supports the specified programming language. Returns `false` for `ComputerLanguage.Text` and `true` for all other languages.

- `isEnabled(event: AnActionEvent)`: Checks if the paste action should be enabled based on the presence of supported content in the clipboard. Returns `true` if the clipboard contains text data; otherwise, returns `false`.


#### Clipboard Handling

- **Clipboard Content Check (`hasClipboard`)**: Checks if the system clipboard contains data that can be interpreted as text. Supports both plain text and Unicode text flavors.
  
- **Retrieving Clipboard Content (`getClipboard`)**: Retrieves the content from the system clipboard if it supports the required data flavors (plain text or Unicode text). Returns the clipboard content as an `Any?` type, or `null` if the content is not supported.


#### Usage

This class is designed to be used within an IDE environment where the AI Coder plugin is installed. It enhances the development experience by allowing developers to easily convert and paste code snippets from one programming language to another, directly from the clipboard, with minimal configuration required from the user's end.

# code\RenameVariablesAction.kt


#### RenameVariablesAction Class Documentation

The `RenameVariablesAction` class extends the `SelectionAction` class, specifically designed for renaming variables within a selected piece of code in an IDE environment. This action utilizes an external API for suggesting new variable names that are more descriptive or appropriate according to the context of the code and the conventions of the programming language.


##### Key Components

- **RenameAPI Interface**: Defines the contract for the external service that suggests new names for variables. It includes a single method, `suggestRenames`, which takes the code snippet, the computer language of the code, and the human language for the suggestions. It returns a `SuggestionResponse` containing a list of suggested renames.

- **SuggestionResponse and Suggestion Classes**: Nested within the `RenameAPI`, these classes are used to encapsulate the response from the rename suggestion API. `SuggestionResponse` contains a list of `Suggestion` objects, each of which holds an `originalName` and a `suggestedName`.

- **proxy Property**: A lazy-initialized property that creates a proxy to the rename suggestion API using the `ChatProxy` class. It is configured with settings from `AppSettingsState`, such as the default chat model and temperature for generating suggestions.


##### Methods

- **getConfig**: This method is overridden from the `SelectionAction` class but is not utilized in this implementation, returning an empty string.

- **processSelection**: The core logic of the action, which processes the selected text to generate and apply rename suggestions. It performs the following steps:
  1. Calls the rename suggestion API through the `proxy` to get suggestions for the selected text.
  2. Filters out suggestions where either the original name or the suggested name is null.
  3. Presents the user with a dialog to choose which suggestions to apply.
  4. Applies the selected suggestions to the selected text and returns the modified text.

- **choose**: Presents a dialog with checkboxes for each rename suggestion, allowing the user to select which variables to rename. Returns a set of selected original variable names.

- **isLanguageSupported**: Checks if the action supports the given computer language. It returns `false` for plain text (`ComputerLanguage.Text`) and `true` for all other languages.


##### Configuration and Logic

- The rename suggestions are generated based on the context of the selected code snippet, the programming language of the code (`state.language`), and the user's preferred human language for suggestions (`AppSettingsState.instance.humanLanguage`).

- The `ChatProxy` used for the `proxy` property is configured with parameters from `AppSettingsState`, such as the model and temperature, which influence how the suggestions are generated.

- The `processSelection` method uses `UITools.run` to execute the suggestion fetching and application on the UI thread, ensuring that the IDE remains responsive.

- The `choose` method utilizes `UITools.showCheckboxDialog` to present the user with a choice of which variable names to rename, enhancing the interactivity of the action.

This class demonstrates a sophisticated integration of external AI-based services for code refactoring tasks within an IDE, leveraging user settings and preferences to tailor the suggestions.

# dev\PrintTreeAction.kt


#### Class Documentation: PrintTreeAction


##### Overview
The `PrintTreeAction` class extends the functionality of `BaseAction` to provide a specific action within the IntelliJ IDE. This action allows developers to print the tree structure of a PsiFile, which is particularly useful for understanding the organization and hierarchy of elements within a file. The action is contingent upon a specific setting (`devActions`) being enabled in the application's settings.


##### Usage
To utilize the `PrintTreeAction`, follow these steps:
1. Ensure that the `devActions` setting is enabled in the application's settings. This setting is crucial for the action to be available.
2. Open the file within IntelliJ for which you wish to print the tree structure.
3. Access the "PrintTreeAction" from the editor's context menu.
4. Upon selection, the tree structure of the currently opened file will be printed to the log.


##### Implementation Details


###### Methods

- `handle(e: AnActionEvent)`: This method is overridden from the `BaseAction` class. It is the core function that executes the action's primary operation. When invoked, it retrieves the largest contained entity within the current file using `PsiUtil.getLargestContainedEntity(e)` and prints its tree structure to the log using `PsiUtil.printTree(...)`.
  
- `isEnabled(event: AnActionEvent)`: Also overridden from `BaseAction`, this method determines whether the `PrintTreeAction` is available to the user. It checks the `AppSettingsState.instance.devActions` configuration to see if developer actions are enabled. The action is only enabled and visible in the context menu if this setting returns `true`.


###### Configuration
- The action's availability is controlled by the `devActions` setting found in `AppSettingsState`. This setting must be enabled for the action to be accessible.


###### Logging
- The class utilizes SLF4J for logging purposes, with a private static logger instantiated specifically for `PrintTreeAction`. This logger is used to output the tree structure of the PsiFile to the log, facilitating debugging and analysis by developers.


##### Conclusion
The `PrintTreeAction` class is a developer-oriented feature within the IntelliJ plugin that leverages the application's settings and PSI utilities to provide insights into the structure of PsiFiles. By printing the tree structure to the log, it aids developers in understanding and navigating the complexity of their codebase.

# FileContextAction.kt

This Kotlin code defines an abstract class `FileContextAction` that extends `BaseAction`, designed to be a part of an IntelliJ IDEA plugin. The class is intended to provide a framework for actions that operate on files or folders within the IDE, allowing for the processing of selected files or folders based on specific configurations. Below is a detailed explanation of the key components related to prompt text, configuration, and related logic within the class.


#### Class Definition

- `FileContextAction<T : Any>`: An abstract class that takes a generic type `T`, representing the configuration type for the action. It requires two boolean parameters, `supportsFiles` and `supportsFolders`, to indicate whether the action supports file and/or folder operations.


#### Inner Data Class

- `SelectionState`: A data class that holds the currently selected file and the project root directory as `File` objects. This class is used to pass the selection context to the `processSelection` method.


#### Abstract Method

- `processSelection(state: SelectionState, config: T?): Array<File>`: An abstract method that subclasses must implement. It processes the selected file or folder based on the provided configuration of type `T` and returns an array of `File` objects that result from this processing.


#### Configuration Method

- `getConfig(project: Project?, e: AnActionEvent): T?`: An open method that returns a configuration object of type `T` for the action. By default, it returns `null`, but subclasses can override this method to provide specific configurations based on the project and the action event.


#### Action Handling

- The `handle(e: AnActionEvent)` method is the entry point for the action. It retrieves the configuration using `getConfig`, determines the selected file or folder, and then processes the selection in a separate thread. It uses utility methods from `UITools` for task management and file operations within the IDE.


#### Utility Methods

- `isEnabled(event: AnActionEvent): Boolean`: Overrides the `isEnabled` method to determine if the action should be enabled based on the current selection (file or folder) and the `isDevAction` flag in conjunction with the application settings.

- `open(project: Project, outputPath: Path)`: A companion object method that attempts to open a file in the IDE editor. If the file is not immediately available, it schedules retries using a scheduled executor service.


#### Logging and Error Handling

- The class includes a `log` object for logging purposes and implements error handling in the `handle` method, logging any exceptions that occur during the processing of the selection.

This class serves as a foundation for creating IntelliJ IDEA plugin actions that operate on files or folders, providing mechanisms for configuration, selection processing, and integration with the IDE's file editor. Implementers are required to define the specific processing logic and configuration by extending this class and overriding the abstract and open methods.

# dev\AppServer.kt


#### Class Overview

The `AppServer` class is part of the `com.github.simiacryptus.aicoder.actions.dev` package and is designed to manage a web server within a development environment, specifically tailored for applications that require real-time communication, such as chat applications. It leverages the Jetty server framework to dynamically add and manage web application contexts, facilitating the deployment of chat services.


#### Key Components and Functionality

- **Initialization**: The server is initialized with a local name and port, along with an optional `Project` instance. This setup allows the server to bind to a specific address and listen for incoming connections.

- **Server Management**: At its core, the class manages a Jetty `Server` instance, which is lazily instantiated. The server's handler is set to a `ContextHandlerCollection`, which aggregates multiple web application contexts.

- **Dynamic Context Handling**: The class supports the dynamic addition of web application contexts through the `addApp` method. This method allows for the registration of `ChatServer` instances under specific paths, facilitating the deployment of multiple chat applications on the same server.

- **WebApp Context Configuration**: For each chat application, a new `WebAppContext` is created and configured. This includes setting up WebSocket support, defining the base resource, class loader, context path, and welcome files. The chat server's specific configuration is also applied to the context.

- **Server Lifecycle Management**: The class includes mechanisms to start and stop the server, as well as to monitor its running state. A separate thread is used to track the server's progress and handle its lifecycle based on user actions or system events.

- **Singleton Pattern**: The `AppServer` class implements a singleton pattern through its companion object. This ensures that only one instance of the server is active at any given time, providing a centralized point of control. The `getServer` method is used to obtain the current instance or create a new one if necessary, while the `stop` method facilitates server shutdown.


#### Usage

1. **Initialization**: Create or obtain an instance of the `AppServer` using the `getServer` method, providing the necessary configuration parameters.

2. **Adding Applications**: Use the `addApp` method to dynamically add chat applications to the server. Specify the application's path and provide an instance of `ChatServer` configured for the application.

3. **Server Control**: Start the server using the `start` method. Monitor its state and manage its lifecycle as needed. Use the `stop` method to shut down the server when it is no longer needed.


#### Conclusion

The `AppServer` class provides a flexible and efficient way to manage a web server for real-time communication applications within a development environment. Its ability to dynamically add and configure web application contexts makes it particularly suited for scenarios where multiple chat services need to be deployed and managed concurrently.

# dev\InternalCoderAction.kt

The `InternalCoderAction` class is part of a plugin designed to integrate an internal coding agent into the IntelliJ IDE. This class extends `BaseAction` and is responsible for initializing and managing a coding session within the IDE. The action is triggered by user interaction, such as clicking a menu item or a button. Below is a detailed explanation of the key components and logic within the `InternalCoderAction` class.


#### Key Components

- **Path Configuration**: The `path` variable is set to `"/internalCoder"`, which specifies the endpoint for the internal coding agent within the application server.

- **Action Handling (`handle` method)**: This method is invoked when the action is triggered. It performs several key operations:
  - **Session Initialization**: A new global session ID is generated using `StorageInterface.newGlobalID()`.
  - **Server and Application Initialization**: Retrieves the application server instance for the current project and initializes the coding application on the server with the specified path.
  - **Socket Manager**: A new session is created for the coding application, and a socket manager is associated with this session.
  - **Symbol Collection**: Collects various symbols from the action event, such as the editor, file, project, and others, and stores them in a map for later use.
  - **Coding Agent Initialization**: Initializes a `CodingAgent` with the collected symbols, session information, and other configurations. This agent is responsible for handling coding tasks.
  - **Browser Navigation**: Opens the user's default browser and navigates to the URL of the coding session after a brief delay.

- **Action Availability (`isEnabled` method)**: Determines whether the action should be enabled or disabled based on the application's settings. Specifically, it checks if the development actions are enabled in the `AppSettingsState`.

- **Application Initialization (`initApp` method)**: A helper method that initializes the coding application on the server if it hasn't been initialized already. It creates a new `ApplicationServer` instance with a specific application name and path, and overrides the `userMessage` method to handle messages from users.


#### Configuration and Logic

- **Dynamic Symbol Handling**: The action dynamically collects context information (symbols) from the IDE and passes it to the coding agent. This allows the agent to operate with a rich context, enhancing its coding suggestions and operations.

- **Session Management**: Each coding session is uniquely identified by a session ID, allowing multiple coding sessions to be managed independently.

- **Application Server Integration**: The action integrates closely with the application server, adding a new application for the coding agent and handling user messages through the server's infrastructure.

- **User Interface Interaction**: The action interacts with the user's desktop environment to open a browser window, directing the user to the coding session's UI. This provides a seamless transition from the IDE to the coding agent's interface.

- **Development Mode Check**: The action's availability is contingent on the development mode setting, ensuring that it is only accessible when appropriate for the development environment.

This class exemplifies how to extend IDE functionality with external services (like a coding agent) by integrating with the IDE's action system, managing sessions, and interacting with application servers and user interfaces.

# generic\GenerateRelatedFileAction.kt


#### CreateFileFromTemplateAction Class Documentation

The `CreateFileFromTemplateAction` class is designed to facilitate the creation of new files within a project, based on a given directive and the content of an existing file. This action is context-sensitive, meaning it is only enabled when a non-directory file is selected in the IDE. The class extends `FileContextAction` and is tailored for use within IntelliJ-based IDEs.


##### Key Components

- **isEnabled(event: AnActionEvent): Boolean**  
  Determines if the action should be enabled based on the current context. It checks if the selected item is not a directory.

- **SettingsUI Class**  
  Defines the UI component for collecting user input. It includes a JTextArea for entering directives for file creation.

- **UserSettings Class**  
  Holds the user-provided settings, specifically the directive for file creation.

- **Settings Class**  
  Encapsulates the settings necessary for file creation, including the `UserSettings` and the current `Project`.

- **getConfig(project: Project?, e: AnActionEvent): Settings**  
  Configures and returns the settings for file creation, including collecting user input through a dialog.

- **processSelection(state: SelectionState, config: Settings?): Array<File>**  
  Main logic for processing the selected file and creating a new file based on the provided directive. It involves generating the new file's content and path, ensuring no naming conflicts, and writing the content to the new file.

- **generateFile(baseFile: ProjectFile, directive: String): ProjectFile**  
  Generates the new file's content and path based on a given directive and the content of an existing file. It utilizes an AI model to combine the directive with the base file's content to produce the output.

- **open(project: Project, outputPath: Path)**  
  Opens the newly created file in the editor, ensuring the file is visible and accessible to the user.

- **getModuleRootForFile(file: File): File**  
  Utility function to find the root directory of the module to which the given file belongs, identified by the presence of a `.git` directory.


##### Usage

This action is intended to be triggered from the IDE's context menu when a file is selected. Upon activation, it presents a dialog for the user to enter a directive for the new file's creation. The action then processes the selected file and the directive to generate a new file, ensuring it does not overwrite existing files. The new file is then opened in the editor for immediate access.


##### Implementation Details

- The action leverages the IntelliJ Platform SDK for UI components and file manipulation.
- It uses an AI model (via `ApiModel.ChatRequest`) to generate the new file's content based on natural language instructions and the content of an existing file.
- The action ensures compatibility with the project's file structure and adheres to best practices for file creation within an IDE environment.

This class is a part of the `com.github.simiacryptus.aicoder.actions.generic` package and requires several dependencies, including the IntelliJ Platform SDK, Apache Commons IO, and the `com.simiacryptus.jopenai` package for AI model interaction.

# generic\AppendTextWithChatAction.kt


#### Class Documentation: `AppendTextWithChatAction`

`AppendTextWithChatAction` is a specialized action class that extends the functionality of `SelectionAction<String>`. It is designed to append text to the end of a user's selected text within an IntelliJ IDEA project environment. This class leverages the OpenAI API to generate the text that will be appended, using a model specified in the application's settings.


##### Configuration

The `getConfig` method is designed to fetch configuration settings for the action, but in the current implementation, it returns an empty string. This method can be overridden to provide specific configuration strings based on the project context if needed.


##### Process Selection

The core functionality of the `AppendTextWithChatAction` class is encapsulated in the `processSelection` method. This method takes a `SelectionState` and a configuration string as inputs and returns a string that represents the original selected text with additional text appended to it. The method performs the following steps:

1. **Fetch Settings**: It retrieves the current application settings using `AppSettingsState.instance`, which includes the default chat model and temperature settings for generating text.

2. **Prepare Chat Request**: It constructs a `ChatRequest` object with the model specified in the settings and a temperature value. The request includes two messages:
   - A system message indicating the action to be performed ("Append text to the end of the user's prompt").
   - A user message containing the text currently selected by the user.

3. **API Call**: It makes a call to the OpenAI API using the `api.chat` method, passing the prepared `ChatRequest` and the model specified in the settings.

4. **Append Text**: It retrieves the response from the API and appends the generated text to the original selected text. If the generated text starts with the original selected text, it ensures that the original text is not duplicated in the appended result.


##### Usage

This class is intended to be used within the context of an IntelliJ IDEA plugin, where it can be invoked to append generated text to a user's selected text in the editor. The actual appending logic is dependent on the response from the OpenAI API, making it versatile for various text augmentation tasks.


#### Important Considerations

- The `processSelection` method relies on the OpenAI API, and as such, requires a valid API key and network connectivity.
- The method ensures that the original selected text is not duplicated in the appended result, which is a crucial detail for maintaining text integrity.
- The current implementation does not utilize the `config` parameter in `processSelection`, but it is available for future enhancements or custom configurations.

This class demonstrates a practical application of AI in enhancing developer tools, specifically in automating text-related tasks within an IDE.

# generic\VoiceToTextAction.kt

The `VoiceToTextAction` class is designed to facilitate speech-to-text functionality within an IDE, leveraging audio recording and processing to convert spoken words into text. This process involves capturing audio, processing it for clarity, and then converting the audio into text which is inserted at the current caret position or replaces the selected text in the editor. A significant part of this functionality revolves around the management and use of a "prompt" text, which is crucial for understanding and documenting how the dictation action operates.


#### Prompt Text Configuration and Logic

The prompt text plays a critical role in the speech-to-text conversion process, especially in the context of continuous dictation where the context of previously dictated text can significantly enhance the accuracy of the transcription. The `DictationPump` inner class is primarily responsible for handling the audio buffer and converting its contents into text, with the prompt text being a key component of this process.


##### Initialization and Updates

- **Initialization**: The prompt text is initialized in the `DictationPump` constructor, where it can be set to the text currently selected by the user (if any). This allows the speech-to-text API to have context right from the start, potentially improving the accuracy of the transcription.
  
- **Updates**: After each successful transcription, the prompt text is updated with the most recent transcription result. This is achieved by appending the new text to the existing prompt, splitting the prompt into individual words, and then keeping only the last 32 words. This updated prompt is then used for the next transcription request, ensuring that the context is maintained throughout the dictation session.


##### Usage

- The prompt text is used as part of the transcription request to the speech-to-text API (`api.transcription(recordAudio, prompt)`). Providing this context allows the API to better understand the intended meaning of the spoken words, especially in cases where the dictation involves complex or domain-specific language.


#### Closing Remarks

The management of the prompt text within the `VoiceToTextAction` class showcases a thoughtful approach to enhancing the accuracy and usability of speech-to-text functionality in an IDE setting. By maintaining and updating a context-aware prompt, the implementation ensures that users can enjoy a more seamless and accurate dictation experience, even during extended dictation sessions. This feature underscores the importance of context in natural language processing applications and demonstrates a practical application of such principles in software development tools.

# generic\CodeChatAction.kt

The `CodeChatAction` class is designed to facilitate a code chat feature within an IDE, leveraging various components for session management, language detection, and web socket communication. This class extends `BaseAction`, indicating it is an action that can be triggered within the IDE environment. Below is a detailed breakdown of its functionality, configuration, and related logic:


#### Overview

- **Purpose**: To open a code chat session for the currently selected code or the entire document if no selection is made.
- **Key Features**:
  - Detects the programming language of the current document.
  - Opens a web-based chat interface in the default browser, specifically designed for discussing code.
  - Utilizes web sockets for real-time communication.
  - Stores session information for continuity and reference.


#### Configuration and Properties

- **`path`**: A string representing the endpoint for the code chat application. It is set to `"/codeChat"` and used to construct the URL for the web interface.
- **`root`**: A file path that points to the storage location for code chat data. It is derived from the plugin's home directory combined with `"code_chat"`.


#### Key Methods and Logic


##### `handle(e: AnActionEvent)`

- **Parameters**: Receives an `AnActionEvent` object which provides context about the action event, including access to the current editor and project.
- **Functionality**:
  - Retrieves the current editor and aborts if not available.
  - Generates a new global session ID.
  - Determines the programming language of the current document.
  - Extracts the filename and the selected text (or entire text if no selection is made) from the editor.
  - Initializes a `CodeChatSocketManager` and stores it in a session-specific map for managing web socket communication.
  - Sets up a `ChatServer` instance for the session and opens the chat interface in the default web browser.


##### `isEnabled(event: AnActionEvent)`

- Always returns `true`, indicating that this action is always enabled.


##### `initApp(server: AppServer, path: String)`

- **Purpose**: To initialize the chat server application if it hasn't been already.
- **Parameters**:
  - `server`: An instance of `AppServer` which manages server-side applications.
  - `path`: The endpoint path for the code chat application.
- **Functionality**:
  - Checks if the application is already registered with the server. If so, it returns the existing instance.
  - If not registered, it creates a new `ApplicationServer` instance specifically for code chat, configuring it with properties like `applicationName`, `singleInput`, and `stickyInput`.
  - Registers the new chat server application with the `AppServer`.


#### Logging

- Utilizes SLF4J for logging, particularly for capturing any issues that occur when attempting to open the web browser to display the chat interface.


#### Session Management

- Manages sessions through a combination of `StorageInterface.newGlobalID()` for session ID generation and a custom map (`agents`) for storing `SocketManager` instances associated with each session.


#### Conclusion

The `CodeChatAction` class is a comprehensive solution for integrating a code-focused chat feature within an IDE. It handles the initialization of necessary components, manages sessions, and ensures seamless communication between the IDE and the web-based chat interface.

# generic\MultiStepPatchAction.kt

The `MultiStepPatchAction` class is part of a larger system designed to automate development tasks within a software project. It leverages AI models to interpret user requests, generate action plans, and apply changes to the codebase. This documentation focuses on the prompt text, configuration, and related logic within the `MultiStepPatchAction` class.


#### Overview

The `MultiStepPatchAction` class initiates an automated development assistant that interacts with the user through a web interface. It creates a session for each user request, processes the input, and utilizes AI agents to generate and execute a series of development tasks based on the user's instructions.


#### Key Components


##### 1. AutoDevApp

- **Purpose**: Serves as the main application server for handling user messages and initiating the AI-driven development process.
- **Configuration**:
  - `applicationName`: The name of the application, defaulting to "Auto Dev Assistant v1.1".
  - `symbols`: A map of symbols used within the application, initially empty.
  - `temperature`: Controls the creativity of the AI responses. A lower value (e.g., 0.1) results in more predictable outputs.
  - `event`: The `AnActionEvent` triggering the action.


##### 2. AutoDevAgent

- **Purpose**: Acts as the core agent system for processing user messages and executing development tasks.
- **Actor Types**:
  - `DesignActor`: Translates user directives into an actionable plan, breaking down the request into a list of simple tasks.
  - `TaskCodingActor`: Implements the changes to the codebase as described in the task list, using code patches in diff format.
- **Configuration**:
  - `api`: The API interface for interacting with AI models.
  - `model`: The AI model used for generating responses, with a default of `ChatModels.GPT35Turbo`.
  - `tools`: A list of tools or technologies relevant to the task at hand, initially empty.
  - `actorMap`: Maps actor types to their corresponding actor implementations.


##### 3. Prompts

- **DesignActor Prompt**:
  - Asks the AI to translate user directives into an action plan, specifying files to be modified and describing the changes.
- **TaskCodingActor Prompt**:
  - Requests the AI to implement code changes based on the task list, using diff format for code patches. It includes instructions for formatting the response and an example.


#### Logic Flow

1. **Initialization**: Upon handling an `AnActionEvent`, the `MultiStepPatchAction` class initializes a session and sets up the `AutoDevApp`.
2. **User Interaction**: The `AutoDevApp` listens for user messages and, upon receiving one, invokes the `AutoDevAgent`.
3. **Task Generation**: The `AutoDevAgent` uses the `DesignActor` to break down the user's request into a list of tasks.
4. **Task Execution**: For each task, the `TaskCodingActor` generates code patches to implement the required changes.
5. **Output**: The changes are presented to the user, who can review and apply them to the codebase.


#### Conclusion

The `MultiStepPatchAction` class and its components represent a sophisticated system for automating development tasks using AI. By interpreting user requests, generating actionable plans, and executing code changes, it aims to streamline the development process and reduce manual effort.

# generic\CreateFileFromDescriptionAction.kt

This Kotlin class, `CreateFileFromDescriptionAction`, is part of a larger system designed to automate file creation within a project structure based on natural language directives. It extends `FileContextAction` with specific settings for creating files. The action utilizes an AI model to interpret directives and generate the necessary file content and path. Below is a detailed breakdown of its components and functionality:


#### Class Overview

- **ProjectFile**: A simple data class holding information about a file to be created, including its path and the code content.
- **SettingsUI**: Defines the user interface for configuring the action. It includes a text area for entering the directive that guides the file creation process.
- **Settings**: Holds the configuration settings for the action, primarily the directive as a string.
- **processSelection**: The core method that processes the selected file or directory and generates a new file based on the provided directive and the context of the selection.


#### Configuration and Logic

- **Directive**: A key component of the `Settings` and `SettingsUI` classes, representing the natural language instructions for generating the new file. This directive is used by the AI model to understand what file needs to be created and what its contents should be.
- **generateFile**: A private method that takes a base path and a directive as inputs. It communicates with an AI model to generate the path and content for the new file based on the directive. The method constructs a `ChatRequest` to the AI, including the directive and contextual information about the desired file location. The AI's response is parsed to extract the file path and content.


#### AI Model Interaction

- The interaction with the AI model is facilitated through the `generateFile` method, which constructs a `ChatRequest` object. This request includes:
  - The model name and temperature settings from `AppSettingsState`.
  - A system message that sets the context for the AI, explaining the task of interpreting natural language to create a file.
  - A user message containing the directive and additional context about the file location.

- The AI's response is expected to include a header line indicating the file path, followed by the file content. The method parses this response to extract the necessary information and create a `ProjectFile` object.


#### File Creation Logic

- The `processSelection` method determines the appropriate path for the new file based on the current selection and the AI-generated path. It handles potential conflicts by checking for existing files and, if necessary, generating a unique filename to avoid overwriting.
- Once the final path is determined, the method writes the AI-generated code to the new file and returns the file as part of an array, indicating successful creation.

This class demonstrates a sophisticated integration of AI capabilities into a development environment, leveraging natural language processing to automate routine tasks like file creation based on user-provided directives.

# generic\LineFilterChatAction.kt

The `LineFilterChatAction` class is designed to facilitate a unique interaction within an IDE, where it enables users to engage in a chat session focused on discussing and understanding specific segments of code. This functionality is particularly useful for developers seeking assistance or clarification on code snippets. Below is a detailed explanation of the key components related to the prompt text, configuration, and the closely related logic within the `LineFilterChatAction` class.


#### Prompt Text Configuration

The class configures two main types of prompts for the chat session: the `userInterfacePrompt` and the `systemPrompt`. These prompts are designed to guide the interaction between the user and the AI within the chat environment.

- **`userInterfacePrompt`**: This prompt is displayed to the user and includes the filename, the programming language of the code, and the code snippet itself. It is formatted using markdown to enhance readability. The purpose of this prompt is to present the code in question clearly and concisely to the user.

- **`systemPrompt`**: This prompt is designed for the AI's understanding. It informs the AI that its role is to assist with coding questions related to the provided code snippet. The prompt includes the filename, the programming language, and the code snippet with line numbers added for reference. This detailed prompt enables the AI to reference specific lines in its responses, facilitating a more focused and helpful discussion.


#### Logic for Prompt Preparation

The preparation of the prompts involves several steps:

1. **Retrieving and Formatting Code**: The action retrieves the code either from the selected text in the editor or the entire document if no text is selected. It then formats the code by adding line numbers, which are crucial for the AI to reference specific lines in its responses.

2. **Determining Language and Filename**: The action identifies the programming language of the code and the filename. This information is essential for formatting the `userInterfacePrompt` and `systemPrompt` correctly and ensuring that the AI understands the context of the discussion.

3. **Creating ChatSocketManager Instance**: An instance of `ChatSocketManager` is created with the prepared prompts. This instance is responsible for managing the chat session, including sending prompts to the AI and rendering the AI's responses. The `renderResponse` method within this instance is particularly noteworthy as it processes the AI's responses, translating line references into actual code lines or keeping the original response text as appropriate.


#### Chat Session Initialization

After configuring the prompts and related logic, the action initiates a chat session by:

1. **Creating a New Session**: A new session is created with a unique session ID. This session is associated with the `ChatSocketManager` instance prepared earlier.

2. **Launching the Chat Interface**: The action opens a web browser to the chat interface, allowing the user to start interacting with the AI immediately. This step is performed in a separate thread to avoid blocking the main application thread.


#### Summary

The `LineFilterChatAction` class leverages detailed prompt configuration and sophisticated logic to facilitate an interactive chat session focused on code discussion. By providing clear prompts and managing the chat session efficiently, it offers a valuable tool for developers seeking assistance with their code.

# generic\GenerateDocumentationAction.kt

The `GenerateDocumentationAction` class is designed to automate the process of compiling documentation from selected files within an IntelliJ IDEA project. It leverages the OpenAI API to transform the content of these files into a more structured and user-friendly documentation format. Below is a detailed explanation of the key components and logic related to the configuration and processing of this action.


#### Configuration


##### SettingsUI Class
- **Transformation Message (`transformationMessage`)**: A `JTextArea` component that allows the user to input a custom message or instructions that will guide the transformation of the file content into documentation. This message is passed to the OpenAI API as part of the request.
- **Output Filename (`outputFilename`)**: Another `JTextArea` component where the user specifies the name of the output file that will contain the compiled documentation. The default value is `compiled_documentation.md`.
- **Files to Process (`filesToProcessScrollPane`)**: A `JBScrollPane` that contains a `CheckBoxList<Path>` displaying all the files eligible for processing. The user can select or deselect files from this list.


##### UserSettings Class
Holds the user-configurable settings:
- `transformationMessage`: Stores the transformation message input by the user.
- `outputFilename`: Stores the output filename specified by the user.
- `filesToProcess`: A list of `Path` objects representing the files selected by the user for processing.


##### Settings Class
Encapsulates the `UserSettings` and the current `Project` instance. It is used to pass these settings to the processing logic.


#### Processing Logic


##### isEnabled Method
Determines if the action should be enabled based on the current context. It checks if the selected item is not a directory, ensuring that the action is only available for file selections.


##### getConfig Method
Responsible for displaying the configuration UI to the user and collecting the settings. It walks through the file system starting from the selected folder, listing all regular files (excluding directories) and populating the `filesToProcess` list. The method then displays a dialog where the user can adjust settings and select files for processing.


##### processSelection Method
Executes the main logic for compiling documentation. It performs the following steps:
1. Determines the output path for the compiled documentation, creating a new file if one already exists with the specified name.
2. Initializes an executor service for parallel processing.
3. Partitions the files into those selected for processing and others.
4. For each selected file, reads its content, sends it to the OpenAI API along with the transformation message, and appends the transformed content to the markdown output.
5. Writes the compiled markdown content to the output file.
6. Opens the output file in the IDE.


##### transformContent Method
Sends the file content and transformation message to the OpenAI API, requesting a transformation based on natural language instructions and the provided code example. It constructs a `ChatRequest` with the file content and transformation message as user messages, and the instruction to document code as a system message. The response is then used to generate the documentation content.


##### open Method (Companion Object)
A utility method that attempts to open the compiled documentation file in the IDE. It repeatedly checks if the file exists and is not already open, attempting to open it and scheduling retries with a delay if necessary.

This class and its methods demonstrate a sophisticated integration with the IntelliJ Platform SDK, leveraging both the IDE's UI components for configuration and external APIs for content transformation.

# generic\DiffChatAction.kt

The `DiffChatAction` class is part of a larger application designed to facilitate code discussions and reviews directly within an IDE (Integrated Development Environment). This class specifically handles the initiation of a "diff chat" session, where users can discuss changes to code in a chat interface that supports diff formatting. Below is a detailed explanation of the key components related to the prompt text, configuration, and the logic closely associated with these elements.


#### Key Components


##### 1. System Prompt Configuration
The system prompt is a crucial part of the user interaction within the diff chat. It instructs users on how to format their code patches using the diff format within the chat interface. The prompt is defined in the `systemPrompt` property of the anonymous `CodeChatSocketManager` class instance created within the `handle` method. The prompt text encourages users to provide code patches in diff format, using `+` for line additions and `-` for line deletions, and to include sufficient context around changes.

```kotlin
override val systemPrompt: String
  get() = super.systemPrompt + """
    Provide code patches in diff format within ```diff code blocks.
    The diff format should use + for line additions, - for line deletions.
    The diff should include sufficient context before every change to identify the location.
  """.trimIndent()
```


##### 2. Response Rendering
The `renderResponse` method is responsible for rendering the chatbot's response. It first processes the response to add links that allow applying the suggested diffs directly to the code. This is achieved through the `addApplyDiffLinks` utility function, which takes the original code, the response containing the diffs, and a callback function to apply the new code. The processed response is then converted to HTML using the `renderMarkdown` function, ensuring that the chat interface can display it properly.

```kotlin
override fun renderResponse(response: String): String {
  val withLinks = addApplyDiffLinks(rawText, response) { newCode ->
    WriteCommandAction.runWriteCommandAction(e.project) {
      document.replaceString(selectionStart, selectionEnd, newCode)
    }
  }
  val html = renderMarkdown(withLinks)
  return """<div>$html</div>"""
}
```


##### 3. Chat Session Initialization
The chat session is initialized in the `handle` method, where a new `CodeChatSocketManager` instance is created and associated with a session ID. This setup involves configuring the session with the selected code (or the entire document if no selection is made), the programming language, and other relevant details. The session is then registered with an `AppServer` instance, which manages the web interface for the chat.

```kotlin
val server = AppServer.getServer(e.project)
val app = initApp(server, path)
app.sessions[session] = app.newSession(null, session)
```


##### 4. Browser Interaction
After initializing the chat session, the class attempts to open the user's default web browser to the chat interface using the `Desktop.getDesktop().browse` method. This step is performed in a separate thread to avoid blocking the main application thread.

```kotlin
Thread {
  Thread.sleep(500)
  try {
    Desktop.getDesktop().browse(server.server.uri.resolve("$path/#$session"))
  } catch (e: Throwable) {
    log.warn("Error opening browser", e)
  }
}.start()
```


#### Conclusion
The `DiffChatAction` class integrates with an IDE to provide a unique interface for discussing code changes using diff formatting. It leverages Kotlin's capabilities to interact with the IDE's editor, manage sessions, and render interactive content in a web interface. This class is a part of a larger system designed to enhance the code review and collaboration process directly within the development environment.

# generic\RedoLast.kt


#### RedoLast Action

The `RedoLast` class is a custom IntelliJ action designed to allow users to redo the last AI Coder action they executed within the IntelliJ editor. This functionality is particularly useful for developers working with AI Coder, enabling them to quickly revert and reapply changes made by the AI.


##### Configuration and Usage

To utilize the `RedoLast` action, users must first be within the IntelliJ editor where they previously performed an AI Coder action. The action can be triggered by selecting "RedoLast" from the editor's context menu. This initiates the redo operation for the last action executed in that specific editor session.


##### Implementation Details

The `RedoLast` class extends `BaseAction`, inheriting its basic action handling capabilities. The core functionality is implemented within two overridden methods:

- `handle(e: AnActionEvent)`: This method is responsible for executing the redo operation. It retrieves the current editor's document from the event object `e` using `e.getRequiredData(CommonDataKeys.EDITOR).document`. It then checks if there is a retry operation available for this document in the `retry` map. If available, it executes the `run()` method of the corresponding retry operation.

- `isEnabled(event: AnActionEvent)`: This method determines whether the RedoLast action should be enabled or disabled in the context menu. It checks if there is a retry operation available for the current editor's document in the `retry` map. The action is enabled if a retry operation exists; otherwise, it is disabled.


##### Key Components

- `retry`: A map that stores retry operations for documents. Each document in the IntelliJ editor has a corresponding retry operation if an AI Coder action has been performed on it. This map is crucial for tracking which documents have actions that can be redone.

- `AnActionEvent`: This class from the IntelliJ Platform SDK provides context information about the action event, including data about the current editor and document. It is used to retrieve the document for which the redo operation needs to be performed.


##### Conclusion

The `RedoLast` action enhances the AI Coder experience in IntelliJ by providing a straightforward mechanism to redo the last performed action. By integrating with the IntelliJ action system and utilizing the `retry` map for tracking actions, it offers a seamless way for developers to manage their AI-assisted coding changes.

# generic\ReplaceWithSuggestionsAction.kt

The `ReplaceWithSuggestionsAction` class, part of the `com.github.simiacryptus.aicoder.actions.generic` package, extends the functionality of `SelectionAction<String>` to provide a mechanism for suggesting text replacements within an IDE environment. This class is designed to interact with a virtual API to generate text suggestions based on the context of the selected text in the editor. The core functionality revolves around generating suggestions for replacing a selected piece of text with alternatives, potentially improving or altering the code or documentation in meaningful ways.


#### Key Components and Logic


##### VirtualAPI Interface
- **Purpose**: Defines a contract for suggesting text replacements. It contains a single method, `suggestText`, which takes a template string and a list of examples to generate suggestions.
- **Suggestions Class**: Nested within the `VirtualAPI` interface, this class holds the suggestions returned by the `suggestText` method, encapsulated in a `List<String>`.


##### Proxy Initialization
- The `proxy` property lazily initializes an instance of `ChatProxy` tailored to interact with the `VirtualAPI`. It configures the proxy using settings from `AppSettingsState`, such as the default chat model and temperature settings for generating suggestions.


##### processSelection Method
- **Purpose**: Overrides the `processSelection` method from `SelectionAction<String>`. It's responsible for processing the text selection and generating replacement suggestions.
- **Logic**:
  - Retrieves the selected text and calculates an ideal context length based on the length of the selected text.
  - Extracts the text before and after the selection, ensuring the context is of a manageable size for the suggestion engine.
  - Calls the `proxy.suggestText` method with a template that includes placeholders for the selected text and the surrounding context. This template is used to generate contextually relevant suggestions.
  - Returns the chosen suggestion from a list of options presented to the user.


##### choose Method
- **Purpose**: Presents the generated suggestions to the user through a radio button dialog, allowing them to choose the most appropriate replacement.
- **Implementation**: Utilizes `UITools.showRadioButtonDialog` to display the options and capture the user's selection.


#### Configuration and Usage
- The `getConfig` method currently returns an empty string, indicating no additional configuration is required for this action. However, this method can be overridden in subclasses to provide specific configurations.
- The class is designed to be used within an IDE environment where actions can be triggered by the user. It requires access to the current project context (`Project`) and the action event (`AnActionEvent`) to function correctly.


#### Summary
`ReplaceWithSuggestionsAction` leverages a virtual API to enhance text editing capabilities by suggesting contextually relevant replacements for selected text. It demonstrates a practical application of machine learning models, encapsulated by the `ChatProxy`, to improve developer productivity within an IDE.

# generic\WebDevelopmentAssistantAction.kt

The `WebDevelopmentAssistantAction` class is part of a larger system designed to assist in web development tasks by leveraging AI-driven agents. This system is structured to handle user requests, generate web development artifacts (like HTML, CSS, and JavaScript files), and provide architectural guidance for web applications. The core components facilitating these functionalities are the `WebDevApp` and `WebDevAgent` classes, along with their nested classes and enums.


#### WebDevApp Class

The `WebDevApp` class extends `ApplicationServer` and is configured to serve as a web development assistant. It is initialized with a default application name ("Web Dev Assistant v1.1") and a path ("/webdev"). This class handles user messages by creating instances of `WebDevAgent` and starting the agent with the user's message.


##### Settings Nested Class

The `Settings` data class within `WebDevApp` holds configuration settings for the application, including a budget for API usage, a list of tools, and the model to be used for chat interactions (defaulting to `ChatModels.GPT4Turbo`).


#### WebDevAgent Class

`WebDevAgent` is an `ActorSystem` that manages different types of actors to perform specific tasks related to web development. It defines an enum `ActorTypes` to categorize these actors into roles such as HTML, JavaScript, and CSS coding, architecture discussion, code review, and more.


##### Actor Prompts and Configuration

Each actor type is associated with a specific prompt and configuration, guiding the AI to generate appropriate responses for different aspects of web development. For example:

- **HTMLCodingActor**: Translates user requests into a skeleton HTML file.
- **JavascriptCodingActor**: Generates a JavaScript file based on the user's description.
- **CssCodingActor**: Creates a CSS file for styling the web application.
- **ArchitectureDiscussionActor**: Discusses and outlines a detailed architecture for the web application, suggesting frameworks, libraries, and coding patterns.
- **CodeReviewer**: Reviews code provided by the user, looks for bugs, and suggests fixes in a diff format.

These actors are initialized with prompts that describe their specific tasks in detail, ensuring that the AI's responses are aligned with the requirements of web development projects.


#### Handling User Messages and Generating Code

When a user sends a message to the `WebDevApp`, a `WebDevAgent` instance is created and tasked with handling the message. The agent uses the defined actors to generate architectural plans, draft code files, and review existing code based on the user's input. The process involves iterative interactions with the AI, where the agent sends prompts to the AI models and processes their responses to produce the desired outputs.


#### Summary

The `WebDevelopmentAssistantAction` system is a sophisticated tool that leverages AI to assist in various aspects of web development, from planning and architecture to coding and code review. By defining specific roles for AI-driven agents and configuring them with detailed prompts, the system can generate accurate and useful responses to facilitate the web development process.

# markdown\MarkdownImplementActionGroup.kt

This Kotlin code defines a plugin action group for IntelliJ-based IDEs, specifically targeting the enhancement of Markdown files with automatically generated code snippets in various programming languages. The primary functionality is encapsulated within the `MarkdownImplementActionGroup` class and its nested `MarkdownImplementAction` class. Below is a detailed explanation of the key components and their functionality:


#### MarkdownImplementActionGroup Class

- **Purpose**: Serves as a container for actions that can be performed on Markdown files. It dynamically generates a list of actions based on supported programming languages, allowing users to insert code snippets in these languages into their Markdown documents.

- **markdownLanguages**: A list of strings representing the programming languages supported for code snippet generation. This includes languages like SQL, Java, C++, Python, and many others.

- **update(e: AnActionEvent)**: This method is overridden to control the visibility and availability of the action group based on specific conditions. It checks if the current file is a Markdown file and if there is a text selection within it.

- **isEnabled(e: AnActionEvent)**: A companion object function that determines whether the action group should be enabled. It checks if the current file's language is Markdown and if there is a selected text.

- **getChildren(e: AnActionEvent?)**: This method generates the child actions (one for each programming language in `markdownLanguages`) that can be executed. These actions are instances of the `MarkdownImplementAction` class, each configured with a specific programming language.


#### MarkdownImplementAction Class

- **Purpose**: Represents an individual action that can be performed; specifically, generating and inserting a code snippet in the specified programming language into a Markdown document.

- **language**: A private property that stores the programming language for which the code snippet will be generated.

- **ConversionAPI Interface**: Defines the contract for a service that can convert a given text into a code snippet in a specified programming language. It includes a method `implement` that takes the text to be converted, the human language of the input text, and the target programming language, returning a `ConvertedText` object containing the generated code snippet.

- **getProxy()**: This method creates a proxy to the conversion API, configured with settings from `AppSettingsState`, such as the model to use and the temperature for the generation process.

- **processSelection(state: SelectionState, config: String?)**: Overrides a method from the `SelectionAction` class to process the selected text in the Markdown document. It uses the conversion API to generate a code snippet in the specified programming language and formats it as a Markdown code block.

This code is designed to enhance the functionality of Markdown editing in IntelliJ-based IDEs by allowing users to easily insert code snippets in various programming languages, leveraging an external code generation API.

# generic\MultiDiffChatAction.kt

The `MultiDiffChatAction` class is designed to facilitate a chat-based interface that assists users with coding by providing suggestions and modifications to their code. This is achieved through a multi-file diff chat action, where the AI analyzes the provided code and offers improvements or fixes in the form of diffs. Below is a detailed explanation of the key components related to the prompt text, configuration, and the logic closely associated with these elements.


#### Key Components


##### Prompt Configuration

The AI's interaction with the user is guided by two main prompts: the `userInterfacePrompt` and the `systemPrompt`. These prompts are configured as follows:

- **`userInterfacePrompt`**: This prompt is presented to the user and includes a summary of the code files being discussed. It is generated by the `codeSummary()` function, which compiles the code from all files into a markdown format, with each code snippet enclosed in a code block and preceded by the file path.

- **`systemPrompt`**: This prompt is designed for the AI and includes instructions on how the AI should assist the user. It provides a context that the AI is a helpful entity for coding-related queries and specifies that the responses should include code patches in diff format. The prompt also includes the same code summary as the `userInterfacePrompt`, ensuring that the AI has all the necessary information about the code being discussed.


##### Code Summary Generation

The `codeSummary()` function plays a crucial role in generating the content for both prompts. It iterates over the `codeFiles` map, which contains paths and code content of the files involved, and formats this information into a markdown string. Each file's code is presented with its path as a header and the code itself in a fenced code block, with the appropriate language syntax highlighting based on the file extension.


##### Chat Socket Manager Initialization

The `agents` map is used to store instances of `ChatSocketManager`, which are initialized with the session ID, model, prompts, and other configurations necessary for the chat functionality. The `ChatSocketManager` is responsible for handling the chat interactions, rendering responses, and applying any code modifications suggested by the AI.


##### Response Rendering and Code Modification

The `renderResponse` method of the `ChatSocketManager` is overridden to process the AI's responses. It uses the `renderMarkdown` function to convert the AI's response, which includes diffs and explanations, into HTML. The `addApplyDiffLinks` function is called to process the diffs, apply them to the code, and generate links for updating the code files. This method ensures that any modifications suggested by the AI are reflected in the actual code files and the user interface.


#### Conclusion

The `MultiDiffChatAction` class integrates with the IntelliJ platform to provide a unique coding assistance experience through a chat interface. By leveraging detailed prompts and dynamic response rendering, it facilitates an interactive environment where users can receive and apply coding suggestions from an AI. The careful configuration of prompts and the logic for generating and processing code summaries and diffs are central to the functionality of this feature.

# SelectionAction.kt

The `SelectionAction` class is an abstract class designed to facilitate the creation of actions within the IntelliJ platform that operate on a selected portion of text within an editor. It provides a framework for handling text selection, processing the selected text, and optionally modifying it based on a specific configuration. This class is part of a larger system aimed at enhancing coding productivity and automation within the IDE.


#### Key Components


##### Configuration (`T`)

- The generic type `T` represents the configuration object for the action. This configuration can hold any necessary settings or parameters required to process the selection.
- The `getConfig(project: Project?): T?` method is designed to retrieve the configuration object. It can be overridden to provide a custom configuration based on the current project context.


##### Selection Processing

- The `processSelection(event: AnActionEvent?, selectionState: SelectionState, config: T?): String` method is an open method intended to be overridden by subclasses. It defines how the selected text should be processed. The method takes an `AnActionEvent`, a `SelectionState` object containing details about the current text selection, and the configuration object of type `T`. It returns a `String` which is the processed text that may replace the original selection.
- The `SelectionState` data class encapsulates various details about the current selection, including the selected text, selection offset, document information, and more. This information aids in processing the selection accurately.


##### Selection Handling

- The `handle(e: AnActionEvent)` method is the entry point for the action. It retrieves the necessary context from the `AnActionEvent`, such as the editor and project, and manages the selection processing workflow. This includes adjusting the selection if necessary, processing the selected text, and applying any changes to the document.
- The `retarget` method adjusts the selection boundaries based on the current selection and the action's requirements. It ensures that there is a valid selection to work with, according to the action's `requiresSelection` property.


##### Utility Methods

- `editorState(editor: Editor): EditorState` retrieves the current state of the editor, including the text, cursor position, and other relevant details.
- `contextRanges(psiFile: PsiFile?, editor: Editor): Array<ContextRange>` analyzes the PSI (Program Structure Interface) tree of the current file to determine contextually relevant ranges around the cursor. This can be useful for actions that need to understand the surrounding code structure.


##### Enabling the Action

- The `isEnabled(event: AnActionEvent): Boolean` method determines whether the action should be enabled based on the current context, such as whether there is a valid selection or if the language of the file is supported.


#### Usage

To use the `SelectionAction` class, one must extend it and implement the abstract methods, particularly `processSelection`, to define the action's behavior. The configuration type `T` allows for flexible customization of the action's processing logic, making it adaptable to various use cases within the IntelliJ platform.

# markdown\MarkdownListAction.kt

The `MarkdownListAction` class extends `BaseAction` and is designed to enhance the functionality of markdown files within an IDE environment. This action, when triggered, automatically generates and appends a list of new items to an existing markdown list based on the context and content of the selected list. The generation of new list items is facilitated through a proxy to an external AI service, which is configured and invoked within the class.


#### Key Components and Logic


##### ListAPI Interface
- **Purpose**: Defines the contract for the AI service to generate new list items.
- **Methods**: 
  - `newListItems(items: List<String?>?, count: Int)`: Takes a list of existing items and a desired count for new items to generate a structured response containing the new list items.


##### Proxy Configuration
- **Purpose**: Configures and initializes the proxy to the external AI service.
- **Key Elements**:
  - Utilizes `ChatProxy` to create a proxy instance conforming to the `ListAPI` interface.
  - Configures the proxy with necessary parameters such as the API model, deserializer retries, and an example to guide the AI's response format.


##### Action Logic (`handle` Method)
- **Trigger**: Activated when the action is invoked in the IDE.
- **Process Flow**:
  1. Validates the presence of a caret and PSI (Program Structure Interface) file in the event context.
  2. Identifies the smallest intersecting markdown list element based on the caret's selection.
  3. Extracts and trims the text of each item within the identified list.
  4. Determines the indentation and bullet type for the list items.
  5. Invokes the AI service through the proxy to generate new list items based on the existing ones.
  6. Constructs the new list string with appropriate indentation and bullet types.
  7. Inserts the new list items into the document at the specified location.


##### Enabling the Action
- **Purpose**: Determines whether the action should be enabled based on the current context.
- **Logic**:
  - Checks if the current file is a markdown file and if there is a markdown list element intersecting with the current selection or caret position.
  - The action is enabled only if these conditions are met, ensuring relevance and applicability of the action to the user's context.


#### Configuration and Invocation
The action's functionality is heavily reliant on the external AI service, which is abstracted through the `ListAPI` interface and accessed via the `ChatProxy`. The proxy is configured with specific parameters, including the AI model to use and an example to guide the generation of new list items. This setup allows for dynamic and context-aware augmentation of markdown lists within the IDE, enhancing productivity and leveraging AI capabilities for content generation.

