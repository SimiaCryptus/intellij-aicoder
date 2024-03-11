# Project User Documentation

Welcome to the user documentation for our IntelliJ IDEA plugin suite, designed to enhance your development experience
with a variety of actions. This document provides an overview of the available actions, categorized for easy reference,
and instructions on how to use them to improve your coding efficiency.

## Table of Categorized Actions

- `DiffChatAction`: Engages in a chat session to generate and apply code diffs directly within the IDE.
- **Collaboration and Review**
    - `DiffChatAction`: Initiates a chat session for collaborative code review and diff generation.
    - `MultiDiffChatAction`: Allows for collaborative code review and diff generation across multiple files.
    - `LineFilterChatAction`: Provides an interactive chat interface for discussing specific lines of code.
    - `CodeChatAction`: Offers a real-time chat interface for discussing code snippets and receiving AI-powered coding assistance.

## How to Use Actions

- **Code Editing Actions**
    - `CustomEditAction`: Trigger this action to open a dialog where you can input a natural language instruction for
      editing your selected code.
    - `ImplementStubAction`: Automatically selects code elements that match stub patterns and implements them based on
      AI suggestions.
    - `PasteAction`: Simply copy text to your clipboard and use this action to paste it as converted code in your
      editor.
    - `RenameVariablesAction`: Select a block of code and trigger this action to receive suggestions for renaming
      variables.
- **Documentation Actions**
    - `DocAction`: Select a code block and activate this action to generate and insert documentation above the selected
      block.
    - `DocumentationCompilerAction`: Select files or folders in your project, trigger this action, and specify output
      settings to compile documentation.
- **Code Comments Actions**
    - `CommentsAction`: Highlight a code block and activate this action to automatically add comments explaining the
      code.
    - `DescribeAction`: Use this action to generate a detailed description of the selected code, enhancing its
      documentation.
- **Code Generation Actions**
    - `InternalCoderAction`: Start a coding session with this action to receive coding assistance directly in the IDE.
    - `WebDevAction`: Trigger this action for AI-powered assistance with web development tasks, including code
      generation and architecture suggestions.
    - `AutoDevAction`: Automates development tasks by translating user directives into actionable development tasks and code modifications.
- **Markdown Enhancement Actions**
    - `MarkdownImplementActionGroup`: In a Markdown file, select text and use this action to generate code snippets in
      various languages.
    - `MarkdownListAction`: Highlight a list in your Markdown file and activate this action to automatically generate
      and insert new list items.
- **Development Workflow Actions**
    - `RecentCodeEditsAction`: Access this action to see a list of your most recent code edits and quickly reapply any
      of them.
    - `RedoLast`: Use this action to redo the last AI Coder action you executed, enhancing your workflow efficiency.
- **Text Transformation Actions**
    - `AppendAction`: Select text and trigger this action to append AI-generated text, expanding on your initial
      selection.
    - `ReplaceOptionsAction`: Highlight text and activate this action to receive and choose from AI-generated text
      options for replacement.

Each action is designed to seamlessly integrate into your development workflow, offering a range of enhancements from
code editing to documentation and AI integration. Explore these actions to discover how they can improve your
productivity and coding experience in IntelliJ IDEA.

# code\CustomEditAction.kt

## CustomEditAction Documentation

### Overview

The `CustomEditAction` class extends the functionality of `SelectionAction` to provide a mechanism for editing code
based on user instructions. It leverages a virtual API to process the code modifications, which can include operations
like adding comments, refactoring, or any other code transformation specified by the user. This action is designed to
integrate with an IDE environment, allowing users to easily modify their code using natural language instructions.

### Key Components

#### VirtualAPI Interface

- **Purpose**: Defines the contract for the code editing service.
- **Methods**:
    - `editCode(code: String, operation: String, computerLanguage: String, humanLanguage: String)`: Takes the original
      code, an operation in natural language, the programming language of the code, and the human language of the
      operation. Returns an `EditedText` object containing the modified code.

- **EditedText Data Class**:
    - Holds the result of the code editing operation, including the modified code and its language.

#### CustomEditAction Class

- **Functionality**: Allows users to input natural language instructions to edit selected code within an IDE.
- **Key Methods**:
    - `getConfig(project: Project?)`: Displays a dialog to the user to input the editing instruction.
    - `processSelection(state: SelectionState, instruction: String?)`: Processes the user's instruction on the selected
      code and returns the edited code.

#### Proxy Property

- **Purpose**: Creates an instance of the `VirtualAPI` using a `ChatProxy`, pre-configured with examples and settings
  from `AppSettingsState`.
- **Functionality**: It demonstrates how to use the virtual API by adding an example of code editing and then creates
  the proxy instance that will be used for actual code editing operations.

### Usage

1. **Invoke CustomEditAction**: This action can be triggered within an IDE environment where it is integrated.
2. **Input Instruction**: When prompted, the user inputs a natural language instruction detailing how the selected code
   should be edited.
3. **Code Transformation**: The action processes the instruction and applies the specified edits to the selected code.

### Integration Points

- **AppSettingsState**: Utilizes application settings for configuring the chat proxy, including the model to use and the
  temperature setting for the AI's responses.
- **UITools**: Used for displaying the input dialog to the user.

### Example

If a user selects a piece of code and inputs the instruction "Add code comments explaining the
function", `CustomEditAction` will process this instruction, potentially resulting in the selected code being annotated
with comments that explain its functionality, based on the capabilities of the underlying virtual API.

### Conclusion

`CustomEditAction` provides a powerful and intuitive way for developers to edit and refactor their code using natural
language instructions, seamlessly integrating AI-powered code transformation into their development workflow.

# code\CommentsAction.kt

#### CommentsAction Class Documentation

The `CommentsAction` class is part of the AI Coder extension, designed to enhance code readability by automatically
adding comments to the selected code block within your IDE. This class extends the functionality
of `SelectionAction<String>`, allowing it to process selected text and utilize AI services to generate insightful
comments.

##### Features

- **Language Support**: The action is designed to work with a variety of programming languages, excluding plain text. It
  ensures that the feature is only applied to code, where commenting is beneficial.
- **AI-Powered**: Utilizes the `ChatProxy` class to communicate with an AI model, specifically tailored to generate
  comments for code. This integration allows for context-aware, meaningful comments that improve code understanding.
- **Customizable**: Leverages settings from `AppSettingsState` to allow customization of the AI's behavior, including
  the model's temperature and the choice of language model, adapting the comments to the user's preferences.

##### Key Methods

- **getConfig(Project?): String**: Returns a configuration string for the action. Currently, this method returns an
  empty string, serving as a placeholder for future configurations.

- **isLanguageSupported(ComputerLanguage?): Boolean**: Checks if the given programming language is supported by the
  action. It returns `true` for all programming languages except for plain text, ensuring the action is applied to
  actual code.

- **processSelection(SelectionState, String?): String**: The core method of the class, it takes the selected code block
  and processes it through the AI model to generate and insert comments. It utilizes the `ChatProxy` class to
  communicate with the AI, passing parameters such as the selected text, desired operations, and language settings.

##### CommentsAction_VirtualAPI Interface

An interface defining the `editCode` method, which is essential for the interaction with the AI model to edit and
comment on the code. It specifies the parameters required for the operation, including the code block, operation
description, computer language, and human language.

##### CommentsAction_ConvertedText Class

A nested class within the `CommentsAction_VirtualAPI` interface, designed to encapsulate the result of the AI's code
commenting process. It contains fields for the commented code (`code`) and the language of the code (`language`),
facilitating easy integration and use of the AI-generated comments within the IDE.

#### Usage

To use the `CommentsAction` feature, ensure your IDE project is set up with the AI Coder extension and configured
according to your preferences in `AppSettingsState`. Select a block of code in a supported language, and trigger
the `CommentsAction`. The AI will analyze the selected code and insert comments, enhancing the code's readability and
maintainability.

This documentation provides a concise overview of the `CommentsAction` class and its functionalities. For further
details or customization options, refer to the source code or extension documentation.

# code\DocAction.kt

## DocAction Documentation

### Overview

`DocAction` is a specialized action within the AI Coder plugin designed to automatically generate documentation for
selected code blocks within your project. It leverages a virtual API and a chat model to process and convert code into
well-documented text, enhancing readability and maintainability.

### Key Components

#### DocAction_VirtualAPI

An interface that defines the method `processCode`, which takes a code snippet and other parameters to return
a `DocAction_ConvertedText` object containing the generated documentation text and its language.

##### DocAction_ConvertedText

A class that holds the resulting documentation text (`text`) and the language of the documentation (`language`).

#### Proxy Initialization

Upon instantiation, `DocAction` initializes a `ChatProxy` with a predefined example to guide the documentation
generation process. This setup involves specifying the operation (e.g., "Write detailed KDoc prefix for code block"),
the code language, and the target human language.

### Functionality

#### processSelection

When a code selection is made, `processSelection` is invoked to generate documentation for the selected code block. It
formats the selected text, calls the `processCode` method through the proxy, and prepends the generated documentation to
the original code.

#### isLanguageSupported

Determines if the selected code's language is supported for documentation generation, based on the presence and
non-emptiness of the `docStyle` attribute in the `ComputerLanguage` object.

#### editSelection

Adjusts the selection range to encompass the entire code block identified by the PSI (Program Structure Interface) tree,
ensuring that the documentation is generated for the complete logical code block.

### Usage

To use `DocAction`, simply select a code block within your project and activate the action. The plugin will
automatically generate and insert the appropriate documentation based on the code's context, language, and specified
documentation style.

### Configuration

`DocAction` relies on the `AppSettingsState` for configuration, including the default chat model, temperature for the
chat model's responses, and the target human language for the documentation.

### Limitations

- The action does not support plain text (`ComputerLanguage.Text`) or languages without a defined documentation
  style (`docStyle`).
- The quality and accuracy of the generated documentation may vary based on the complexity of the code and the
  effectiveness of the provided examples to the chat model.

### Conclusion

`DocAction` enhances the development experience by automating the documentation process, making code easier to
understand and maintain. By integrating advanced AI models, it offers a sophisticated approach to code documentation,
tailored to the developers' needs.

# code\ImplementStubAction.kt

## ImplementStubAction Documentation

### Overview

The `ImplementStubAction` class is part of a larger framework designed to enhance coding efficiency by automating
certain tasks. This specific action focuses on assisting developers in implementing stubs—placeholders for
functionalities yet to be developed—by leveraging AI-powered code editing capabilities.

### Key Features

- **Language Support**: Not all programming languages are supported. The action excludes plain
  text (`ComputerLanguage.Text`) but supports a variety of other programming languages.
- **Automatic Selection**: It automatically selects the smallest code range that matches a code element within the
  editor's current state, providing a default selection for operation.
- **AI-Powered Code Editing**: Utilizes a virtual API to communicate with an AI model, editing code based on the
  operation "Implement Stub". This process considers both the computer and human languages set in the application
  settings.

### How It Works

1. **Language Check**: Initially, it checks if the programming language of the code is supported, excluding plain text.
2. **Selection Identification**: It identifies the optimal code selection for the operation by finding the smallest code
   range that matches a code element.
3. **Code Processing**: The selected code is processed to remove any suffixes and trimmed. This processed code is then
   sent to the AI-powered virtual API.
4. **AI Interaction**: Through the `VirtualAPI`, the action sends the code, operation type ("Implement Stub"), computer
   language, and human language to an AI model for processing.
5. **Result**: The AI model returns the edited code, which is then provided as the action's output.

### VirtualAPI

An interface within the `ImplementStubAction` class, `VirtualAPI` is crucial for the AI interaction. It defines the
method `editCode` for sending code to the AI model and receiving the edited code. The `ConvertedText` inner class is
used to encapsulate the result, containing the edited code and its language.

### Usage

This action is designed to be integrated into a larger system, likely an IDE plugin, where it can be triggered in the
context of editing code. Users do not interact with this action directly but benefit from its functionality during their
development workflow, particularly when implementing stubs or placeholders in their codebase.

### Configuration

The action requires minimal configuration, primarily relying on the application's settings for determining the human and
computer languages. The `AppSettingsState` singleton is used to fetch these settings, including the AI model's
temperature setting for processing.

### Conclusion

The `ImplementStubAction` enhances coding efficiency by automating the implementation of stubs through AI-powered code
editing. It supports various programming languages, automatically identifies the optimal code selection, and leverages a
virtual API for AI interaction, streamlining the development process.

# code\PasteAction.kt

## PasteAction Documentation

### Overview

The `PasteAction` class is designed to enhance the functionality of pasting text within the IDE. It automatically
converts the text from the clipboard into a specified programming language, leveraging a virtual API for the conversion
process. This feature is particularly useful for developers working with multiple programming languages or needing to
integrate code snippets from various sources seamlessly.

### Features

- **Automatic Language Detection:** The action can automatically detect the source language of the text in the
  clipboard.
- **Language Conversion:** Converts the clipboard text to the target programming language as specified by the user's
  current context or selection.
- **Clipboard Support:** Efficiently handles text from the clipboard, supporting both plain text and Unicode text
  flavors.
- **Language Support Check:** Ensures that the action is only available for supported programming languages, excluding
  plain text.

### Usage

1. **Clipboard Preparation:** Ensure that the text you wish to convert is copied to your system's clipboard.
2. **Triggering PasteAction:** Use the designated shortcut or menu option to trigger the `PasteAction` within your IDE.
3. **Automatic Conversion:** The action automatically detects the language of the text in the clipboard, converts it to
   the target language, and pastes it into your current editor window.

### Requirements

- The action requires access to a virtual API (`VirtualAPI`) for converting the text between languages.
- The IDE should have access to the `AppSettingsState` configuration for default model and temperature settings used
  during conversion.

### Limitations

- The action does not support plain text (`ComputerLanguage.Text`) as a target language for conversion.
- The functionality is dependent on the availability and response of the `VirtualAPI`.

### API Reference

#### VirtualAPI

An interface used for converting text between different programming languages.

##### Methods

- `convert(text: String, from_language: String, to_language: String): ConvertedText` - Converts the given text from one
  language to another.

##### Inner Class

- `ConvertedText` - Holds the result of a conversion, including the converted code and its language.

### Troubleshooting

- **Conversion Not Triggering:** Ensure that the clipboard contains text and that the text is in a supported format (
  plain text or Unicode text).
- **Unsupported Language Error:** Check if the target language is supported and not marked as plain text.

For further assistance, refer to the developer documentation or contact support.

# code\DescribeAction.kt

## DescribeAction Documentation

### Overview

The `DescribeAction` class is part of a larger system designed to enhance coding efficiency by automatically generating
descriptions for code snippets. This functionality is particularly useful within the context of an IDE (Integrated
Development Environment), where understanding and documenting code can significantly improve readability and
maintainability.

### Key Components

#### DescribeAction_VirtualAPI

This interface defines the core functionality for describing code. It includes a single method, `describeCode`, which
takes a code snippet, the programming language of the code, and the desired language for the description. It returns an
instance of `DescribeAction_ConvertedText`, which contains the generated description.

##### DescribeAction_ConvertedText

A simple data class that holds the result of the code description process. It has two properties: `text`, which is the
generated description of the code, and `language`, indicating the language used for the description.

#### DescribeAction Class

Extends `SelectionAction<String>` and utilizes the `DescribeAction_VirtualAPI` to generate descriptions for selected
code snippets within the IDE.

##### Key Methods

- `getConfig(project: Project?)`: Returns a configuration string for the action. Currently, this method returns an empty
  string.

- `processSelection(state: SelectionState, config: String?)`: Takes the current selection state and configuration,
  generates a description for the selected code, and formats it according to the code's comment style. The result is a
  string that combines the generated description with the original code, properly indented and commented.

#### Proxy Initialization

The `proxy` property lazily initializes an instance of `DescribeAction_VirtualAPI` using the `ChatProxy` class. This
setup allows for dynamic interaction with a backend service capable of generating code descriptions. The configuration
for this proxy includes settings for the AI model, temperature, and the number of retries for deserialization.

### Usage

To use `DescribeAction`, a user would typically select a portion of code within their IDE. The action then triggers
the `processSelection` method, which communicates with the backend service to generate a description for the selected
code. The description is formatted according to the code's comment style and inserted above the selected code snippet,
providing immediate, in-context documentation.

### Configuration

The behavior of `DescribeAction` can be influenced by settings in `AppSettingsState`, such as the desired human language
for descriptions, the AI model used for generating descriptions, and the temperature setting for the AI's responses.

### Conclusion

`DescribeAction` offers a convenient way to automatically generate descriptions for code snippets, facilitating better
documentation and understanding of code within projects. By leveraging AI through a backend service, it provides
accurate and context-aware descriptions, enhancing the development workflow.

# code\InsertImplementationAction.kt

## Insert Implementation Action Documentation

### Overview

The `InsertImplementationAction` class is part of a code generation tool designed to automatically insert code
implementations into your project. It leverages AI to generate code based on a given specification, which can be derived
from comments or selected text within your code editor. This action is integrated into an IDE environment, providing a
seamless experience for generating and inserting code.

### Key Features

- **AI-Powered Code Generation**: Utilizes an AI model to generate code implementations based on natural language
  specifications.
- **Support for Multiple Languages**: Capable of generating code for various programming languages, excluding Text and
  Markdown.
- **Context-Aware**: Takes into account the surrounding context of the selected text or comment to generate relevant
  code.
- **Customizable**: Leverages project-specific settings for human and computer languages, as well as AI model
  configurations.

### How It Works

1. **Selection**: The action can be triggered on a specific selection within your code editor. This selection can either
   be a comment or a block of code.
2. **Specification Extraction**: The tool extracts a specification from the selected text or the largest intersecting
   comment. This specification is then used as input for the AI model.
3. **AI Code Generation**: The extracted specification, along with contextual information about the surrounding code, is
   sent to an AI model. The model generates a code implementation based on this input.
4. **Insertion**: The generated code is automatically inserted into your codebase, directly following the selected text
   or comment.

### Usage

To use the `InsertImplementationAction`, follow these steps:

1. **Select Text or Comment**: In your code editor, select a block of text or a comment that describes the functionality
   you wish to implement.
2. **Trigger Action**: Trigger the `InsertImplementationAction` through your IDE's action or shortcut mechanism.
3. **Review and Save**: The generated code will be inserted automatically. Review the inserted code and make any
   necessary adjustments before saving your file.

### Requirements

- The action requires an IDE environment that supports the integration of custom actions.
- A configured AI model and API key are necessary for code generation. These can be set up in the project's settings.

### Supported Languages

The `InsertImplementationAction` supports various programming languages for code generation. However, it does not
support generating code for Text or Markdown files.

### Configuration

Project-specific configurations, such as the preferred human and computer languages and AI model settings, can be
adjusted in the project's settings. These settings influence the behavior of the code generation process.

### Conclusion

The `InsertImplementationAction` offers a powerful way to accelerate the development process by automatically generating
and inserting code implementations. By leveraging AI and understanding the context of your project, it provides relevant
and customizable code snippets, enhancing productivity and code quality.

# code\RecentCodeEditsAction.kt

## Recent Code Edits Action

### Overview

The `RecentCodeEditsAction` is an IntelliJ IDEA plugin component that provides users with a dynamic action group listing
their most recent custom code edits. This feature enhances the development environment by allowing quick access and
reapplication of frequently used custom edits, streamlining the coding process.

### Features

- **Dynamic Listing**: Displays a list of the most recent custom code edits as individual actions.
- **Quick Access**: Enables users to quickly reapply a recent edit from the list.
- **Visibility Control**: The action group is only visible and enabled when applicable, ensuring a clutter-free
  environment.

### Usage

#### Activation Conditions

- The action group becomes visible and enabled only when there is a selection in the editor, and the current file is
  recognized as a programming language file (not plain text).
- It is context-sensitive and tailored to enhance productivity by providing relevant actions based on the user's recent
  activities.

#### Accessing Recent Edits

1. **Selection Requirement**: Ensure that you have selected a portion of code in your editor. The action group is
   context-sensitive and requires a selection to operate.
2. **Navigate to Action**: Access the `RecentCodeEditsAction` from the designated menu or action search in IntelliJ
   IDEA.
3. **Choose an Edit**: A list of recent custom edits will be displayed, each prefixed with a number for easy
   identification. If the list contains fewer than ten items, they will be prefixed with an underscore and the item
   number (e.g., `_1: YourRecentEdit`). For ten or more items, they will be listed simply with the number (
   e.g., `10: YourRecentEdit`).
4. **Apply an Edit**: Click on the desired edit to apply it to the current selection.

### Customization

The actions listed are dynamically generated based on the user's recent custom code edits, ensuring that the most
relevant and frequently used edits are easily accessible. This list is managed through the `AppSettingsState`
configuration, where the history of commands is maintained.

### Limitations

- The action group is not available for plain text files, as it is designed to support programming languages recognized
  by IntelliJ IDEA.
- The visibility and availability of the action group depend on the current context, specifically requiring a text
  selection within the editor.

### Conclusion

The `RecentCodeEditsAction` enhances the IntelliJ IDEA development environment by providing a streamlined way to access
and reapply frequently used custom code edits. By integrating this feature, developers can significantly reduce the time
spent on repetitive coding tasks, focusing more on creative and complex aspects of their projects.

# code\RenameVariablesAction.kt

## Rename Variables Action Documentation

### Overview

The `RenameVariablesAction` class is an extension of the `SelectionAction` class designed to facilitate the renaming of
variables in code. It leverages an AI-based suggestion system to propose new names for variables, aiming to improve code
readability and maintainability.

### Key Components

#### RenameAPI Interface

- **Purpose**: Defines the contract for suggesting new names for variables.
- **Method**: `suggestRenames(code: String, computerLanguage: String, humanLanguage: String)`: Accepts the current code
  snippet, the programming language of the code, and the human language for the suggestions. It returns
  a `SuggestionResponse` containing a list of suggested renames.

#### SuggestionResponse Class

- **Purpose**: Encapsulates the response from the rename suggestion API.
- **Attributes**:
    - `suggestions`: A mutable list of `Suggestion` objects.

#### Suggestion Class

- **Purpose**: Represents a single rename suggestion.
- **Attributes**:
    - `originalName`: The original variable name.
    - `suggestedName`: The suggested new name for the variable.

### Usage

1. **Initialization**: The `RenameVariablesAction` class is instantiated as part of the plugin's action system.
2. **Configuration**: Override the `getConfig` method if you need to provide specific configurations for the action. By
   default, it returns an empty string.
3. **Processing Selection**: The `processSelection` method is the core of the action. It:
    - Retrieves rename suggestions for the selected text in the code editor.
    - Displays a dialog for the user to choose which suggestions to apply.
    - Applies the selected renames to the text.
4. **Choosing Suggestions**: The `choose` method displays a checkbox dialog with the suggested renames, allowing users
   to select which renames to apply.

### Supported Languages

The action supports all programming languages except plain text (`ComputerLanguage.Text`). The support is determined by
the `isLanguageSupported` method.

### Integration

This action is designed to be integrated into the IntelliJ IDEA platform. It utilizes the platform's action system and
UI tools for displaying dialogs and processing text selections.

### Example

When a user selects a block of code and triggers the `RenameVariablesAction`, the action will:

- Analyze the selected code to identify variable names.
- Use the `RenameAPI` to get suggestions for renaming these variables based on the code's and user's language.
- Present the user with a dialog to select which variables to rename.
- Apply the selected renames to the code.

This action streamlines the process of renaming variables, making code more readable and maintainable with minimal
effort from the user.

# dev\AppServer.kt

## AppServer Documentation

### Overview

The `AppServer` class is a core component designed to manage and serve web applications within a development
environment. It facilitates the dynamic addition of applications, handling their web contexts, and managing a web server
instance. This class is particularly useful for projects that require running and testing multiple web applications
simultaneously.

### Features

- **Dynamic Application Management:** Allows adding and removing web applications dynamically without needing to restart
  the server manually.
- **WebSocket Support:** Integrated support for WebSocket applications, enabling real-time bi-directional communication
  between clients and the server.
- **Progress Monitoring:** Includes functionality to monitor the server's running state and gracefully handle shutdowns
  or restarts.

### Key Components

- **Server Instance:** A Jetty server instance configured to listen on a specified port and address.
- **Application Registry:** A registry for managing the active web applications (ChatServer instances) and their paths.
- **Web Context Management:** Handles the creation and configuration of web contexts for each registered application.

### Usage

#### Starting the Server

To start the server, simply call the `start()` method. This initializes the server (if not already running) and begins
listening for incoming connections on the configured port and address.

```kotlin
val project: Project? = // Obtain your IntelliJ Project instance
val appServer = AppServer("localhost", 8080, project)
appServer.start()
```

#### Adding Applications

Applications can be added dynamically using the `addApp(path: String, socketServer: ChatServer)` method. This method
registers the application and its web context, making it accessible at the specified path.

```kotlin
val chatServer = ChatServer() // Your ChatServer instance
appServer.addApp("/chat", chatServer)
```

#### Stopping the Server

To stop the server, call the `stop()` method. This method stops the server and clears any registered applications,
preparing the server for a clean shutdown.

```kotlin
AppServer.stop()
```

### Singleton Access

The `AppServer` class provides a singleton access pattern through its companion object, allowing for easy management of
a single server instance across the application.

- **Get Server:** `AppServer.getServer(project: Project?)` returns the current server instance, starting it if
  necessary.
- **Stop Server:** `AppServer.stop()` stops the current server instance and clears it.

### Important Considerations

- The server automatically restarts upon adding a new application to reflect the updated context. Ensure that this
  behavior is accounted for in your application logic.
- The server uses lazy initialization for performance optimization and to avoid unnecessary resource allocation.

### Conclusion

The `AppServer` class offers a flexible and efficient way to manage web applications and their server contexts within a
development environment. Its dynamic application management, WebSocket support, and progress monitoring capabilities
make it a valuable tool for developers working on web-based projects.

# dev\PrintTreeAction.kt

## PrintTreeAction User Guide

### Overview

The `PrintTreeAction` is a powerful tool integrated into the IntelliJ IDE, designed for developers who need to visualize
the structure of their code files in a tree format. This action is particularly useful for understanding the
organization and hierarchy of classes, methods, and other elements within a file.

### Prerequisites

Before you can use the `PrintTreeAction`, ensure the following:

- You have IntelliJ IDE installed.
- The "devActions" setting is enabled in your project's `AppSettingsState`.

### How to Use PrintTreeAction

1. **Open the Desired File**: Navigate to and open the file you wish to analyze in IntelliJ.
2. **Access the Action**: Right-click within the editor window to open the context menu. Look for the "PrintTreeAction"
   option. If you do not see this option, ensure you have met all prerequisites.
3. **Execute the Action**: Click on "PrintTreeAction". The action will process the currently open file.
4. **View the Results**: The tree structure of the file will be printed to the IntelliJ log. You can access this log at
   the bottom of the IntelliJ window, typically in the "Run" or "Debug" tabs.

### Features

- **Tree Structure Visualization**: Quickly understand the hierarchical structure of your PsiFile, including classes,
  methods, and other code elements.
- **Easy Access**: Available directly from the editor's context menu, allowing for seamless integration into your
  development workflow.
- **Conditional Availability**: This action is only available when the "devActions" setting is enabled, preventing
  accidental usage in non-development environments.

### Troubleshooting

- **Action Not Visible**: If the "PrintTreeAction" is not visible in the context menu, ensure that the "devActions"
  setting is enabled in your `AppSettingsState`.
- **Null Pointer Exceptions**: Ensure the file you are trying to analyze is fully loaded and not corrupted to avoid null
  pointer exceptions during the action's execution.

### Conclusion

The `PrintTreeAction` is an invaluable tool for developers looking to gain insights into the structure of their code
files. By following the steps outlined in this guide, you can efficiently utilize this action to enhance your
development process within the IntelliJ IDE.

# FileContextAction.kt

## FileContextAction Documentation

### Overview

`FileContextAction` is an abstract class designed to extend the functionality of actions within a specific file or
folder context in an IntelliJ platform-based IDE. It allows developers to create actions that can process selected files
or folders, perform operations based on those selections, and integrate seamlessly with the IDE's UI and file system.

### Key Features

- **Flexible File and Folder Support**: Actions can be configured to support either files, folders, or both, depending
  on the needs of the specific action being implemented.
- **Configurable Processing**: Implementers can define custom processing logic for selected files or folders, allowing
  for a wide range of operations such as file generation, modification, or analysis.
- **IDE Integration**: Utilizes the IntelliJ platform's UI and file system APIs for selecting files/folders, displaying
  notifications, and managing file operations within the IDE environment.
- **Asynchronous Execution**: Operations are performed in a separate thread, ensuring that the IDE remains responsive
  during potentially time-consuming file processing tasks.

### Usage

To use `FileContextAction`, you need to create a subclass and implement the abstract methods. Here's a simplified
example:

```kotlin
class MyFileAction : FileContextAction<MyConfig>() {
  override fun processSelection(state: SelectionState, config: MyConfig?): Array<File> {
    // Implement your file processing logic here
    return arrayOf() // Return an array of files that were processed or generated
  }

  override fun getConfig(project: Project?, e: AnActionEvent): MyConfig? {
    // Optionally, provide configuration for the action based on the project or event context
    return MyConfig()
  }
}
```

#### Implementing `processSelection`

This method is where the main logic of your action should be implemented. It receives a `SelectionState` object
containing the selected file or folder and the project root directory. You can use this information to perform your
desired file operations.

#### Configuring Your Action

The `getConfig` method allows you to provide custom configuration for your action, which can be useful for tailoring its
behavior based on the project context or user preferences.

### Integration Points

- **isEnabled**: Determines whether the action is enabled based on the current context, such as the type of file
  selected or project settings.
- **handle**: The entry point for the action's execution. It sets up the necessary context and invokes
  the `processSelection` method.

### Best Practices

- Ensure that your file operations are efficient and handle potential errors gracefully to avoid disrupting the user's
  workflow.
- Use the provided IDE APIs for UI interactions and file system operations to ensure compatibility and a consistent user
  experience.

### Conclusion

`FileContextAction` provides a powerful framework for extending the functionality of IntelliJ platform-based IDEs with
custom file and folder actions. By implementing the abstract methods and following the best practices, developers can
create sophisticated tools that enhance the development experience.

# dev\InternalCoderAction.kt

## InternalCoderAction Documentation

### Overview

The `InternalCoderAction` class is part of a plugin designed to integrate coding assistance directly into the IntelliJ
IDE. It facilitates the creation of a coding session within the IDE, leveraging an internal coding agent to provide
suggestions and enhancements to the user's coding experience. This action is triggered through the IDE's action system
and is intended for developers looking for an integrated coding assistant.

### Features

- **Session Initialization**: Creates a unique session for each coding instance, ensuring personalized and isolated
  coding assistance.
- **Dynamic Symbol Resolution**: Collects and resolves symbols from the current editor context, including the editor,
  file, element, and project, to provide context-aware coding assistance.
- **Coding Agent Integration**: Utilizes a `CodingAgent` to process coding requests and provide suggestions based on the
  current context and user input.
- **Web UI Support**: Opens a browser window pointing to the coding session's UI, allowing for an interactive coding
  assistance experience.
- **Customizable Assistance**: Supports customization of the coding agent's behavior, including the response temperature
  and operational details.

### Usage

1. **Prerequisites**: Ensure that the plugin is installed and enabled in your IntelliJ IDE. The feature is available
   only if the development actions are enabled in the application settings (`AppSettingsState.instance.devActions`).

2. **Triggering the Action**: The action can be triggered through the IDE's action system, typically via a menu item or
   keyboard shortcut specific to the plugin.

3. **Coding Session**: Once triggered, the action initializes a new coding session, setting up the necessary environment
   and context for the coding agent to operate.

4. **Interaction**: Interact with the coding agent through the opened web UI or directly within the IDE, depending on
   the implementation details of the plugin.

5. **Session Closure**: The session remains active until manually closed or terminated by the user or the system.

### Configuration

- **Development Actions**: Enable or disable development actions through the `AppSettingsState` configuration.
- **Session Parameters**: Customize session parameters such as the response temperature and operational details directly
  within the `InternalCoderAction` class or through external configuration options.

### Troubleshooting

- **Browser Opening Failure**: If the system encounters an issue opening the browser automatically, check the system's
  default browser settings and ensure that the IDE has the necessary permissions.
- **Session Initialization Error**: Ensure that the plugin's server component (`AppServer`) is running and accessible.
  Check the IDE's log files for any error messages related to the plugin.

### Conclusion

The `InternalCoderAction` provides a seamless integration of coding assistance within the IntelliJ IDE, enhancing the
development experience through context-aware suggestions and interactive coding sessions. By leveraging advanced coding
agents and a dedicated UI, developers can improve their coding efficiency and quality directly within their preferred
development environment.

# generic\AppendAction.kt

## AppendAction Documentation

### Overview

The `AppendAction` class is a specialized action designed to append text to the end of a user's selected text within an
IDE environment. It leverages OpenAI's language model to generate contextually relevant text based on the user's
selection.

### How It Works

1. **Configuration Retrieval**: Initially, the action retrieves any necessary configuration from the project settings,
   though in its current implementation, it returns an empty string as it does not require specific configuration.

2. **Processing Selection**: The core functionality resides in the `processSelection` method, where it performs the
   following steps:
    - Retrieves the application's settings, particularly focusing on the default chat model and temperature settings for
      the OpenAI API request.
    - Constructs a `ChatRequest` object with the model and temperature settings, and includes two messages:
        - A system message indicating the action to be performed ("Append text to the end of the user's prompt").
        - A user message containing the selected text.
    - Sends the request to the OpenAI API and receives a chat response.
    - Appends the generated text to the original selected text, ensuring no duplication of the initial selection.

### Requirements

- **IntelliJ Platform**: This action is designed to work within the IntelliJ platform, requiring a project context to
  operate.
- **AppSettingsState**: The action depends on `AppSettingsState` for retrieving application-wide settings, such as the
  default chat model and temperature for API requests.

### Usage

To use the `AppendAction`, ensure it is properly integrated into your IntelliJ platform-based application or plugin. It
does not require manual configuration but relies on the application's settings for the OpenAI API parameters.

When triggered, it will automatically append contextually generated text to the user's selected text, enhancing or
completing their input based on the model's understanding and the provided prompt.

### Limitations

- **Configuration**: Currently, the `getConfig` method does not utilize project-specific configurations and returns an
  empty string.
- **Context Sensitivity**: The effectiveness of the appended text depends on the accuracy of the OpenAI model's
  understanding of the selected text and the provided prompt.

### Conclusion

The `AppendAction` offers a convenient way to extend user input with AI-generated text, seamlessly integrating with the
IntelliJ platform. By leveraging OpenAI's language models, it provides a powerful tool for enhancing user productivity
and creativity within the development environment.

# generic\AnalogueFileAction.kt

## Analogue File Action Documentation

### Overview

The Analogue File Action is a feature designed for IntelliJ-based IDEs that assists developers in automatically
generating new files based on existing ones, with modifications guided by user-provided directives. This action
leverages the power of AI to interpret directives and apply them to the selected file, creating a new, analogous file
that meets the specified requirements.

### Features

- **Context-Sensitive Activation**: The action is only enabled for non-directory files, ensuring it is contextually
  relevant.
- **Customizable Directives**: Users can input natural language directives to guide the creation of the new file.
- **AI-Powered Processing**: Utilizes an AI model to interpret directives and generate the new file content.
- **Automatic File Naming and Placement**: Generates a unique file name and path to avoid conflicts, placing the new
  file relative to the project root.
- **IDE Integration**: Automatically opens the newly created file in the IDE for immediate review and editing.

### How to Use

1. **Select a File**: In your project, select the file you want to base your new file on.
2. **Activate the Action**: Right-click and find the "Create Analogue File" option. This option is only available for
   non-directory files.
3. **Enter Directive**: In the popup dialog, enter your directive in the provided text area. This directive should
   describe how you want the new file to differ from the selected one.
4. **Generate File**: Click "OK" to generate the new file. The AI will process your directive, create the new file, and
   place it in an appropriate location within your project structure.
5. **Review and Edit**: The new file will automatically open in your IDE. Review the generated content and make any
   necessary adjustments.

### Settings

- **Directive**: A JTextArea where you input your natural language instructions for generating the new file.

### Technical Details

- **Settings Storage**: User settings, including the directive, are stored in a `UserSettings` class instance.
- **File Generation**: The `generateFile` method processes the base file and directive, interacting with an AI model to
  produce the new file content and path.
- **File Handling**: The action ensures the new file does not overwrite existing files by checking for conflicts and
  adjusting the file name as necessary.
- **IDE Integration**: Utilizes IntelliJ platform APIs for file operations and UI interactions, ensuring a seamless
  experience within the IDE.

### Requirements

- IntelliJ-based IDE (e.g., IntelliJ IDEA, PyCharm)
- Java Development Kit (JDK)

### Installation

This feature is packaged as part of a specific IntelliJ plugin. Install the plugin from the JetBrains Marketplace or
your IDE's plugin settings panel.

### Support

For issues, feature requests, or contributions, please refer to the project's GitHub repository or contact the
development team through the appropriate channels.

# generic\AutoDevAction.kt

## AutoDevAction Documentation

### Overview

The `AutoDevAction` class is part of a system designed to automate development tasks within a project. It integrates
with an IDE to provide a user-friendly interface for automating code modifications, leveraging AI models to interpret
user requests and generate actionable development tasks.

### Key Features

- **Automated Task Generation**: Translates user directives into a detailed action plan, breaking down requests into
  manageable tasks.
- **Code Modification Suggestions**: Generates code patches in diff format, suggesting specific changes to be made in
  the project files.
- **Interactive Web UI**: Opens a browser window to interact with the user, providing a session-based interface for task
  management and execution.
- **Integration with AI Models**: Utilizes GPT models for understanding user requests and generating code modifications.

### Usage

1. **Initialization**: The action is triggered within an IDE environment, starting a new session for the user.
2. **Selecting a Project Folder**: The user selects a folder within their project. This folder's path is stored and
   associated with the current session.
3. **Web UI Interaction**: A browser window is opened, directing the user to an interactive session where they can input
   their development requests.
4. **Task Generation and Execution**: The system interprets the user's requests, generating a list of tasks and
   suggesting code modifications to fulfill these tasks. The user can review and apply these suggestions directly from
   the web UI.

### Components

#### AutoDevApp

Represents the application server handling user sessions and messages. It processes user messages to generate
development tasks and code modification suggestions.

##### Key Methods

- `userMessage()`: Handles incoming messages from users, generating tasks and code modifications based on the content of
  the message.

#### AutoDevAgent

Responsible for processing individual development tasks, interacting with AI models to generate code patches and
suggestions.

##### Key Methods

- `start()`: Initiates the process of generating development tasks and code modifications based on the user's request.

### How It Works

1. The `AutoDevAction` class initiates a session and opens a web UI for user interaction.
2. The user inputs a development request in the web UI.
3. The `AutoDevApp` processes this request, utilizing `AutoDevAgent` to interact with AI models and generate a list of
   actionable tasks.
4. For each task, `AutoDevAgent` suggests specific code modifications in diff format.
5. The user reviews and applies these suggestions to their project files directly from the web UI.

### Prerequisites

- An IDE environment compatible with the system.
- Access to the web UI through a supported browser.

### Conclusion

The `AutoDevAction` system offers a powerful tool for automating development tasks, leveraging AI to streamline the
process of code modification. By providing a user-friendly interface and integrating with advanced AI models, it
simplifies the task management process, making it easier for developers to implement changes and enhancements to their
projects.

# generic\CreateFileAction.kt

## CreateFileAction Documentation

### Overview

The `CreateFileAction` class is designed to automate the process of generating and creating new files within a project
based on natural language directives. It leverages the OpenAI API to interpret these directives and generate the
corresponding file content and path. This action is part of a larger system aimed at enhancing developer productivity
through automation.

### How It Works

1. **Directive Input**: The user provides a natural language directive describing the file to be created. This can be as
   simple as "Create a default log4j configuration file" or more complex, depending on the needs.

2. **File Generation**: Based on the provided directive, the system uses the OpenAI API to generate both the content of
   the file and the suggested file path relative to the project root.

3. **File Creation**: The system then creates the file at the suggested path with the generated content. If a file with
   the same name already exists, it appends a unique index to the file name to avoid overwriting.

### Key Components

- **ProjectFile**: A simple data class holding the path and code of the generated file.
- **SettingsUI**: A user interface component that allows the user to input the directive in a text area.
- **Settings**: Holds the directive as a string for processing.
- **processSelection**: The main function that processes the user's selection and directive, generating and creating the
  file accordingly.

### Usage

1. **Input Directive**: Through the `SettingsUI`, the user inputs a directive describing the file they wish to create.
2. **Selection**: The user selects a file or directory within their project, which serves as a context for the file
   generation.
3. **Execution**: Upon executing the action, the system processes the directive and selection, generating and creating
   the new file as described.

### Example

If a user inputs the directive "Create a default log4j configuration file" and selects the root directory of their Java
project, the system might generate a file named `log4j.properties` with standard logging configurations, placing it in
an appropriate directory within the project.

### Conclusion

The `CreateFileAction` class streamlines the process of creating new files within a project, guided by natural language
directives. It simplifies tasks that would otherwise require manual file creation and content generation, thereby
enhancing developer productivity.

# generic\CodeChatAction.kt

## CodeChatAction Documentation

### Overview

The `CodeChatAction` class is part of a plugin designed to enhance coding productivity by integrating a code chat
feature directly into your development environment. This feature allows developers to engage in discussions about code
snippets, share insights, and collaborate more effectively.

### Key Features

- **Code Selection Sharing:** Share selected code snippets or entire files with collaborators in real-time.
- **Language Detection:** Automatically detects the programming language of the shared code for syntax highlighting and
  context-aware discussions.
- **Session Management:** Creates unique sessions for each code chat, ensuring discussions are organized and easily
  accessible.
- **Browser Integration:** Opens the code chat in the default web browser, providing a seamless transition from code
  editor to discussion platform.

### How It Works

1. **Initialization:** When the action is triggered, the plugin checks for an active editor and the selected text within
   it. If no text is selected, the entire document's text is used.
2. **Session Creation:** A unique session ID is generated for the code chat session. This session is associated with the
   selected code snippet or document, the detected programming language, and other relevant metadata.
3. **Code Chat Server:** The plugin interacts with an `AppServer` instance to register the code chat session and
   initialize the necessary backend services, including a `ChatServer` for managing chat sessions.
4. **Opening the Chat:** After a brief delay to ensure server readiness, the plugin attempts to open the code chat in
   the user's default web browser, directing them to the specific session URL.

### Requirements

- **IDE Support:** The plugin is designed to work within an IDE that supports the IntelliJ Platform, such as IntelliJ
  IDEA.
- **Desktop Environment:** Requires a desktop environment capable of opening web URLs in a browser.

### Usage

To use the `CodeChatAction`, follow these steps:

1. **Select Code:** In your IDE, select the code snippet you wish to discuss.
2. **Trigger Action:** Trigger the `CodeChatAction` through the designated shortcut or menu option.
3. **Engage in Discussion:** Once the code chat session opens in your browser, you can start discussing the code with
   your collaborators.

### Troubleshooting

- **Browser Not Opening:** Ensure your default web browser is set correctly and can be launched from your desktop
  environment.
- **Session Not Created:** Check your IDE's log for any errors related to the `CodeChatAction` or network issues that
  may prevent session creation.

For further assistance, consult the plugin's support resources or contact the development team.

# generic\DictationAction.kt

## DictationAction Plugin Documentation

### Overview

The DictationAction plugin is designed to enhance your coding experience by allowing you to dictate code and comments
directly into your IDE. This innovative tool captures your voice, processes the audio, and converts it into text,
inserting the transcribed text at the current cursor position or replacing the selected text in your editor.

### Features

- **Voice to Text Conversion**: Transcribe your voice into text directly in the IDE editor.
- **Continuous Dictation**: Dictate for as long as you need; the transcription stops when you close the status dialog.
- **Automatic Insertion**: The transcribed text is automatically inserted at the cursor's position or replaces the
  selected text.
- **Background Processing**: Audio recording, processing, and transcription run in separate threads, ensuring the IDE
  remains responsive.

### How to Use

1. **Start Dictation**: Trigger the DictationAction from the IDE's action menu. A status dialog appears indicating that
   dictation is active.
2. **Dictate Your Code or Comments**: Speak clearly into your microphone. Your voice is captured, processed, and
   transcribed into text in real-time.
3. **Stop Dictation**: Close the status dialog window to stop the dictation process. The transcription stops, and any
   remaining audio is processed and inserted into the editor.

### Requirements

- A microphone connected to your computer.
- The DictationAction plugin installed in your IDE.

### Troubleshooting

- **Dictation Not Starting**: Ensure your microphone is properly connected and recognized by your system.
- **Poor Transcription Quality**: Speak clearly and at a moderate pace. Background noise can affect transcription
  accuracy.
- **IDE Becomes Unresponsive**: Although designed to run in the background, extremely long dictation sessions may impact
  IDE performance. Consider dictating in shorter bursts.

### Support

For issues, suggestions, or contributions, please visit the DictationAction plugin repository on GitHub.

---

This documentation provides a concise overview of the DictationAction plugin's functionality and usage. For more
detailed information or to contribute to the project, please refer to the project's GitHub page.

# generic\DiffChatAction.kt

## DiffChatAction Documentation

### Overview

The `DiffChatAction` class is part of a plugin designed to enhance coding productivity by integrating a chat-based
interface for generating and applying code diffs directly within the IDE. This action allows users to select a portion
of code, initiate a chat session, and receive suggestions in the form of diffs. These diffs can then be applied directly
to the code with ease.

### Features

- **Code Selection**: Users can select a specific portion of code or use the entire document for the chat session.
- **Chat Session Initiation**: A unique session is created for each chat, allowing for focused and relevant suggestions.
- **Diff Suggestions**: The chat interface provides code modifications in the diff format, making it clear what changes
  are suggested.
- **Direct Application of Diffs**: Users can apply suggested diffs directly from the chat interface, streamlining the
  code improvement process.
- **Markdown Rendering**: The chat interface renders responses in HTML, providing a rich and user-friendly experience.

### Usage

1. **Select Code**: Highlight the code segment you wish to discuss or leave unselected for the entire document.
2. **Activate DiffChatAction**: Trigger the `DiffChatAction` from the IDE's action menu.
3. **Chat Session**: A chat session will open in your default web browser, connected to the selected code segment.
4. **Receive and Apply Diffs**: Engage with the chat to receive diff suggestions. Use provided links to apply diffs
   directly to your code.

### Requirements

- **IDE Support**: This action is designed for use within an IDE that supports the plugin, such as IntelliJ IDEA.
- **Desktop Environment**: A desktop environment capable of opening web links in a browser.

### Limitations

- **Selection Requirement**: For the action to initiate, a text selection or an open document must be present.
- **Browser Dependency**: The action requires a web browser to open the chat interface.

### Troubleshooting

- **Browser Not Opening**: Ensure your default web browser is set correctly and is capable of opening new tabs or
  windows from external applications.
- **Diff Application Issues**: If diffs are not applying correctly, check for any conflicts or syntax errors in the
  suggested changes.

### Conclusion

The `DiffChatAction` enhances coding efficiency by integrating a smart, chat-based diff suggestion and application
mechanism directly within the IDE. By streamlining the process of reviewing and applying code changes, it offers a novel
approach to code improvement and collaboration.

# generic\MultiDiffChatAction.kt

## MultiDiffChatAction Documentation

### Overview

The `MultiDiffChatAction` class is part of a larger system designed to facilitate coding assistance through a chat
interface. This action allows users to interact with an AI model that provides coding help in the form of code diffs.
Users can submit multiple files, and the AI will generate responses that include code modifications, explanations, and
suggestions.

### Key Features

- **Multi-File Support**: Users can submit multiple code files in different programming languages. The action processes
  these files and prepares them for analysis by the AI model.
- **Dynamic Code Analysis**: The AI model analyzes the submitted code and generates responses that may include code
  modifications, suggestions, and explanations.
- **Interactive Chat Interface**: Users interact with the AI through a chat interface, making it easier to ask questions
  and receive assistance.
- **Real-Time Code Updates**: The action supports applying suggested code changes directly to the source files, allowing
  users to quickly adopt the AI's recommendations.

### How It Works

1. **File Submission**: Users submit one or more code files through the interface. The action identifies the programming
   language of each file based on its extension and prepares the content for analysis.

2. **AI Interaction**: The submitted code is sent to an AI model designed to assist with coding tasks. The model
   generates responses based on the code analysis, which may include code diffs, explanations, and suggestions.

3. **Response Rendering**: The AI's responses are rendered in a chat interface. Code diffs are presented in a way that
   users can easily understand the suggested changes. The action also supports rendering the responses in Markdown
   format for better readability.

4. **Applying Changes**: Users have the option to apply the suggested code changes directly from the chat interface. The
   action updates the source files with the new code, reflecting the AI's recommendations.

### Usage

To use the `MultiDiffChatAction`, follow these steps:

1. **Initiate Action**: Trigger the action from within the supported environment (e.g., an IDE or a web interface).

2. **Submit Files**: Select and submit the code files you want assistance with. You can submit files in different
   programming languages.

3. **Interact with AI**: Use the chat interface to ask questions or request assistance. The AI will analyze your code
   and provide responses.

4. **Apply Suggestions**: Review the AI's suggestions and apply any desired code changes directly through the interface.

### Requirements

- Compatible IDE or web interface for initiating the action.
- Internet connection for interacting with the AI model and applying code changes.

### Conclusion

The `MultiDiffChatAction` offers a novel way to receive coding assistance through an interactive chat interface. By
leveraging AI models to analyze code and generate suggestions, users can improve their code quality and efficiency. This
documentation provides a basic understanding of how to use the action and benefit from its features.

# generic\LineFilterChatAction.kt

## LineFilterChatAction Documentation

### Overview

The `LineFilterChatAction` class is part of a plugin designed to enhance coding productivity by providing an interactive
chat interface. This interface allows users to ask questions and receive assistance with their code directly within
their IDE. The action integrates with a chat model to analyze and respond to queries based on the selected or entire
code in the current editor.

### Features

- **Code Contextual Chat**: Engage in a chat session where the AI understands the context of your code, including
  language and content.
- **Markdown Support**: Responses from the AI can include markdown formatting for better readability and structure.
- **Line Reference**: The AI can reference specific lines in its responses, making it easier to understand suggestions
  or corrections.

### How It Works

1. **Activation**: The action is triggered within the IDE. It requires an active editor window with code.
2. **Session Creation**: A unique chat session is created for the interaction.
3. **Code Analysis**: The action extracts the code from the current editor, either the selected text or the entire
   document if no text is selected.
4. **Chat Interface**: The user is directed to a web-based chat interface where they can ask questions and receive
   responses related to the code.
5. **Interactive Responses**: The AI model provides responses, potentially including markdown and references to specific
   lines in the code.

### Requirements

- An active editor window in the IDE with the code you want to discuss.
- Desktop environment capable of opening web browsers for the chat interface.

### Usage

1. **Select Code** (Optional): Select a specific portion of code in the editor if you want to focus the chat on that
   segment.
2. **Trigger Action**: Use the designated shortcut or menu option to activate the `LineFilterChatAction`.
3. **Chat Session**: A browser window/tab will open, directing you to the chat interface. Wait a moment if it doesn't
   open immediately.
4. **Ask Questions**: Start asking your questions or discussing your code with the AI in the chat interface.
5. **Review Responses**: The AI's responses may include markdown formatting and line references for clarity.

### Troubleshooting

- **Browser Not Opening**: Ensure your desktop environment supports opening web links and that no software is blocking
  the action.
- **No Response in Chat**: Verify your internet connection and ensure the server hosting the chat model is operational.

### Conclusion

The `LineFilterChatAction` enhances coding efficiency by providing an AI-powered chat interface for real-time code
assistance. It leverages the context of your code, including language and structure, to offer relevant and interactive
support.

# generic\DocumentationCompilerAction.kt

## Documentation Compiler Action

The Documentation Compiler Action is a feature designed for IntelliJ-based IDEs that assists developers in automatically
generating documentation for their projects. This action compiles documentation from selected files within a project,
leveraging natural language processing to enhance the documentation process.

### Features

- **Automatic Documentation Generation**: Automatically compiles documentation from selected project files.
- **Customizable Output**: Users can specify the output filename and the transformation message to tailor the
  documentation process.
- **File Selection**: Allows users to select specific files for documentation compilation.
- **Concurrency Support**: Utilizes multi-threading to speed up the documentation compilation process.

### How to Use

1. **Select Files**: Right-click on a folder or a selection of files in your project that you wish to document.
2. **Configure Settings**: Upon triggering the action, a settings dialog will appear. Here, you can configure:
    - **Transformation Message**: A custom message to guide the documentation transformation process.
    - **Output Filename**: The name of the file where the compiled documentation will be saved.
    - **Files to Process**: Select or deselect files to include in the documentation compilation.
3. **Compile Documentation**: After configuring the settings, proceed to compile the documentation. The action will
   process the selected files, applying natural language processing to generate or enhance the documentation content.

### Implementation Details

- **File Selection Validation**: The action is only enabled for directories, ensuring that documentation is compiled at
  a folder level.
- **Concurrency**: Utilizes a fixed thread pool to process multiple files concurrently, enhancing performance.
- **Dynamic Output File Naming**: If the specified output file already exists, the action automatically generates a new
  filename to prevent overwriting.
- **IDE Integration**: Seamlessly integrates with the IDE's file system and editor, automatically opening the generated
  documentation upon completion.

### Requirements

- IntelliJ-based IDE (e.g., IntelliJ IDEA, PyCharm)
- Java Development Kit (JDK)

### Installation

This action is part of a plugin package. To install:

1. Open your IDE and navigate to the plugin marketplace.
2. Search for the plugin package containing `DocumentationCompilerAction`.
3. Install the plugin and restart your IDE.

### Conclusion

The Documentation Compiler Action streamlines the process of generating project documentation, making it easier for
developers to maintain up-to-date documentation for their projects. By automating the documentation process and
integrating directly with the IDE, this action saves time and enhances the quality of project documentation.

# generic\RedoLast.kt

## RedoLast Action for IntelliJ

### Overview

The RedoLast action is a feature designed for IntelliJ users who are utilizing AI Coder. It enables users to easily redo
the last AI Coder action they executed within the editor. This functionality is particularly useful for quickly
reverting and reapplying changes made by AI Coder, enhancing productivity and workflow efficiency.

### How to Use

To utilize the RedoLast action, follow these simple steps:

1. **Open the Editor**: Ensure you are in the IntelliJ editor where you previously performed an AI Coder action.
2. **Access the Context Menu**: Right-click within the editor to open the context menu.
3. **Select RedoLast**: Look for the RedoLast action in the context menu and select it.

Upon selection, the RedoLast action will automatically redo the last AI Coder action that was performed in the editor.

### Availability

The RedoLast action is available only when there is a previous AI Coder action to redo. If no such action exists, the
RedoLast option will be disabled.

### Key Features

- **Ease of Use**: Quickly redo the last AI Coder action with a simple selection from the context menu.
- **Efficiency**: Saves time by allowing users to easily revert and reapply changes.
- **Integration**: Seamlessly works within the IntelliJ environment, enhancing the AI Coder experience.

### Requirements

To use the RedoLast action, you must have:

- IntelliJ IDE installed.
- AI Coder plugin enabled in your IntelliJ environment.

### Conclusion

The RedoLast action is a valuable tool for developers using AI Coder in IntelliJ, offering a quick and efficient way to
redo actions. By integrating this feature into your workflow, you can enhance your productivity and streamline your
development process.

# generic\ReplaceOptionsAction.kt

## ReplaceOptionsAction Documentation

### Overview

`ReplaceOptionsAction` is an IntelliJ IDEA plugin action designed to assist developers by suggesting alternative text
options for a selected piece of code or text within the IDE. This action leverages a virtual API to generate suggestions
based on the context surrounding the selected text, aiming to enhance code quality and developer productivity.

### Key Features

- **Context-Aware Suggestions:** Generates text suggestions based on the content before and after the selected text,
  ensuring relevance.
- **Customizable Suggestions:** Utilizes a virtual API, allowing for customization of the suggestion engine.
- **User-Friendly Interface:** Offers a simple dialog with radio buttons for users to choose from the generated
  suggestions.

### How It Works

1. **Selection:** The user selects a piece of text within their code.
2. **Context Analysis:** The plugin calculates an ideal length for context analysis and extracts the text before and
   after the selection.
3. **Suggestion Generation:** The virtual API is called with the contextual information to generate a list of
   suggestions.
4. **User Selection:** A dialog is presented to the user, allowing them to choose one of the suggested options to
   replace the selected text.

### Components

#### VirtualAPI Interface

Defines the contract for the suggestion engine, including the `suggestText` method which takes a template string and a
list of examples to generate suggestions.

##### Suggestions Class

A nested class within `VirtualAPI` that holds the generated suggestions.

#### Proxy

A property that initializes the virtual API proxy with configuration settings from `AppSettingsState`, such as the model
and temperature for generating suggestions.

### Usage

- **Initialization:** The action is initialized within the IntelliJ IDEA environment and listens for user selection.
- **Selection:** The user highlights the text they wish to replace.
- **Execution:** The action is triggered, either via a menu option or a shortcut, initiating the suggestion generation
  process.
- **Choice:** The user is presented with a dialog to choose one of the generated suggestions.
- **Replacement:** The selected suggestion replaces the original text.

### Configuration

The action utilizes settings from `AppSettingsState` for configuring the virtual API, including:

- The default chat model.
- The temperature setting for suggestion variability.

### Extensibility

Developers can extend `ReplaceOptionsAction` to customize the suggestion process or the user interface for selecting
suggestions. The `choose` method can be overridden to implement different mechanisms for presenting and selecting among
the suggestions.

### Conclusion

`ReplaceOptionsAction` is a powerful tool for IntelliJ IDEA users, offering smart, context-aware suggestions to improve
code quality and accelerate development workflows. Through its integration with a virtual API, it provides a flexible
and customizable solution for code enhancement.

# markdown\MarkdownImplementActionGroup.kt

## Markdown Implement Action Group Documentation

The `MarkdownImplementActionGroup` is an extension designed for IntelliJ-based IDEs that enhances your Markdown editing
capabilities by allowing you to automatically generate code snippets in various programming languages directly from your
Markdown files. This feature is particularly useful for developers, technical writers, and educators who frequently
create technical documentation or tutorials.

### Features

- **Multi-Language Support**: Supports a wide range of programming languages including SQL, Java, C, C++, Python, Ruby,
  and many more, allowing you to generate code snippets in the language of your choice.
- **Automatic Code Generation**: Utilizes a conversion API to transform selected text into code snippets, making it
  easier to include code examples in your Markdown files.
- **Easy Integration**: Seamlessly integrates with your development environment, providing a straightforward way to
  enhance your Markdown documents without leaving your IDE.

### How It Works

1. **Language Detection**: The action group first checks if the current file is a Markdown file and if there is a text
   selection within it.
2. **Action Visibility**: If the conditions are met, the action becomes visible and enabled in the IDE's context menu.
3. **Code Generation**: Upon selection of the desired programming language from the context menu, the extension
   communicates with a conversion API to generate a code snippet based on the selected text.
4. **Snippet Insertion**: The generated code snippet is then formatted and inserted into the Markdown document, wrapped
   in the appropriate code block syntax for the selected language.

### Usage

To use the `MarkdownImplementActionGroup`, follow these steps:

1. Open a Markdown file in your IntelliJ-based IDE.
2. Select the text you wish to convert into a code snippet.
3. Right-click to open the context menu and navigate to the `Markdown Implement Action Group` submenu.
4. Choose the programming language for your code snippet from the list.
5. The extension will automatically generate and insert the code snippet into your document.

### Requirements

- IntelliJ-based IDE (e.g., IntelliJ IDEA, PyCharm, WebStorm)
- Java Development Kit (JDK)

### Installation

This extension can be installed from the JetBrains Marketplace or by downloading the plugin JAR file and installing it
manually through your IDE's plugin settings.

### Conclusion

The `MarkdownImplementActionGroup` is a powerful tool for anyone involved in creating or editing Markdown documents that
include code snippets. By automating the code generation process, it not only saves time but also ensures consistency
and accuracy in your documentation.

# generic\WebDevAction.kt

## Web Development Assistant Plugin Documentation

### Overview

The Web Development Assistant is an IntelliJ IDEA plugin designed to streamline the process of developing web
applications. It leverages AI to assist in generating code, reviewing code, and suggesting architectural designs for web
projects directly within the IDE environment.

### Features

- **Code Generation**: Automatically generates HTML, CSS, and JavaScript code based on user input.
- **Code Review**: Analyzes code for potential issues and suggests improvements or fixes.
- **Architecture Suggestion**: Provides detailed architecture suggestions for web applications, including
  framework/library recommendations and CDN links.
- **Session Management**: Supports multiple development sessions with unique settings and resources.
- **Interactive Feedback**: Allows users to provide feedback on generated code and suggestions, facilitating iterative
  improvement.

### Getting Started

1. **Installation**: Ensure the plugin is installed in your IntelliJ IDEA environment.
2. **Accessing the Plugin**: Navigate to the plugin through IntelliJ IDEA's action or menu system.
3. **Starting a Session**: Initiate a new web development session by selecting a target folder for your project.
4. **Interacting with the Assistant**: Provide your requirements or queries to the assistant through the provided UI.
   The assistant will generate code, review existing code, or suggest architectural designs based on your input.
5. **Reviewing Suggestions**: Examine the assistant's output, which may include code snippets, architectural designs, or
   code review comments.
6. **Providing Feedback**: Use the interactive feedback system to refine the suggestions, asking for revisions or
   clarifications as needed.

### Key Components

- **WebDevAction**: The main class that handles user actions, initiating sessions, and opening the browser interface for
  interaction.
- **WebDevApp**: Represents a web development session, managing settings, user messages, and interactions with the AI.
- **WebDevAgent**: Acts as the intermediary between the user and the AI, handling specific tasks like code generation,
  code review, and architecture suggestion.
- **Session Management**: Sessions are uniquely identified and managed, allowing for persistent settings and
  interactions within a project.

### Usage Tips

- **Clear Requirements**: Provide clear and concise requirements to the assistant for more accurate suggestions.
- **Iterative Feedback**: Use the feedback loop effectively by reviewing suggestions and providing specific feedback for
  improvements.
- **Explore Architectural Suggestions**: Take advantage of the architectural suggestions for insights on structuring
  your web application and selecting appropriate technologies.

### Troubleshooting

- **Browser Issues**: If the browser does not open automatically, manually navigate to the provided URL.
- **Session Persistence**: Ensure your project folder is correctly selected to maintain session continuity.
- **Feedback Loop**: If feedback is not being correctly processed, ensure you are providing it in the expected format
  and context.

### Conclusion

The Web Development Assistant plugin offers a powerful toolset for accelerating web development projects by integrating
AI-driven code generation, review, and architectural suggestions directly into the IntelliJ IDEA environment. By
following the guidelines and making effective use of the features, developers can enhance their productivity and focus
on creative aspects of web development.

# markdown\MarkdownListAction.kt

## Markdown List Action Documentation

### Overview

The `MarkdownListAction` class is designed to enhance your Markdown editing experience in IntelliJ-based IDEs by
automatically generating and inserting list items into your Markdown files. This action is particularly useful when
you're looking to quickly expand lists with new, contextually relevant items without manually brainstorming and typing
each one.

### Features

- **Automatic List Item Generation:** Leverages AI to generate new list items based on the existing ones in your
  Markdown document.
- **Context-Aware:** Understands the context of your list to provide relevant suggestions.
- **Customizable Item Count:** Allows specifying the number of new items to generate.
- **Support for Different Bullet Types:** Works with various bullet types including `- [ ]` (task list), `-`, and `*`.

### How It Works

1. **Selection Identification:** The action identifies the list you're working on based on your text selection in a
   Markdown file.
2. **Item Extraction:** It extracts existing list items and sends them to an AI service, which then generates additional
   items based on the context.
3. **List Expansion:** The new items are inserted into your document, directly after the selected list, maintaining the
   original list's formatting and bullet type.

### Usage

To use the `MarkdownListAction`, follow these steps:

1. **Open a Markdown File:** Ensure you're working in a file recognized as Markdown by your IDE.
2. **Select a List:** Highlight a portion of the list you wish to expand. It's important that the selection includes
   part of the list recognized by the IDE as `MarkdownListImpl`.
3. **Activate the Action:** Trigger the `MarkdownListAction`. This can be done through a menu option or a keyboard
   shortcut, depending on how it's configured in your IDE.
4. **Wait for Generation:** The action communicates with an AI service to generate new items. Once the process is
   complete, the new items are automatically inserted into your document.

### Requirements

- IntelliJ-based IDE (e.g., IntelliJ IDEA, PyCharm, WebStorm)
- Active internet connection for AI service communication
- Plugin or configuration that recognizes `MarkdownListAction`

### Limitations

- The quality and relevance of the generated list items depend on the AI model's understanding of the context.
- Requires the document to be properly formatted as Markdown for accurate context recognition and bullet type matching.

### Conclusion

The `MarkdownListAction` is a powerful tool for enhancing productivity and creativity when working with Markdown lists
in IntelliJ-based IDEs. By automating the generation and insertion of list items, it allows users to focus more on
content creation and less on manual list management.

