# AI Coder Plugin

The AI Coder Plugin is a powerful IntelliJ IDEA plugin that leverages artificial intelligence to enhance your coding
experience. With this plugin, you can harness the capabilities of AI models to generate code, refactor existing code,
generate documentation, and more, all within the familiar IntelliJ IDE environment.

By integrating AI into your development workflow, the AI Coder Plugin aims to boost your productivity, improve code
quality, and streamline various coding tasks. Whether you're working on a new project or maintaining an existing
codebase, this plugin can assist you in various ways, from generating boilerplate code to optimizing complex algorithms.

Key features of the AI Coder Plugin include:

- **Code Generation**: Generate code snippets, classes, or entire files based on natural language descriptions or
  examples.
- **Code Refactoring**: Refactor existing code to improve its structure, readability, performance, and maintainability.
- **Documentation Generation**: Automatically generate comprehensive documentation for your code, including
  descriptions, usage examples, and API references.
- **Code Explanation**: Get clear and concise explanations of complex code snippets, helping you understand their
  functionality and purpose.
- **And more**: The plugin is designed to be extensible, allowing for the addition of new AI-powered actions and
  capabilities.

With the AI Coder Plugin, you can leverage the power of artificial intelligence to enhance your coding experience, save
time, and write better code. Get started today and explore the possibilities of AI-assisted development!

# Actions

 Action                       | Description                                                                                                                                               | Usage                                                               | Dependencies                                                                                                                                            |
------------------------------|-----------------------------------------------------------------------------------------------------------------------------------------------------------|---------------------------------------------------------------------|---------------------------------------------------------------------------------------------------------------------------------------------------------|
 `DescribeAction`             | Generates natural language descriptions for selected code snippets using an AI model.                                                                     | Select code, invoke action, description is inserted as a comment.   | `com.github.simiacryptus.aicoder.actions.SelectionAction`                                                                                               |
 `DocAction`                  | Generates documentation comments (e.g., JavaDoc, KDoc) for selected code blocks using an AI model.                                                        | Select code, invoke action, documentation is generated.             | `com.github.simiacryptus.aicoder.actions.SelectionAction`                                                                                               |
 `CustomEditAction`           | Allows editing code using natural language instructions via an AI model.                                                                                  | Select code, invoke action, provide instructions, code is modified. | `com.github.simiacryptus.aicoder.actions.SelectionAction`, `com.simiacryptus.jopenai.proxy`                                                             |
 `CommentsAction`             | Adds comments to each line of the selected code, explaining its functionality using an AI model.                                                          | Select code, invoke action, comments are added.                     | `com.github.simiacryptus.aicoder.actions.SelectionAction`, `com.simiacryptus.jopenai.proxy`                                                             |
 `InsertImplementationAction` | Generates code implementation based on a natural language specification or code comments using an AI model.                                               | Select text or comment, invoke action, implementation is generated. | `com.github.simiacryptus.aicoder.actions.SelectionAction`, `com.simiacryptus.jopenai.proxy`                                                             |
 `PasteAction`                | Converts the text content of the system clipboard from one programming language to another using an AI model.                                             | Copy code, invoke action, converted code is returned.               | `com.github.simiacryptus.aicoder.actions.SelectionAction`, `com.simiacryptus.jopenai.proxy`                                                             |
 `ImplementStubAction`        | Generates code implementations for code stubs or declarations using an AI language model.                                                                 | Select code stub, invoke action, implementation is generated.       | `com.github.simiacryptus.aicoder.actions.SelectionAction`, `com.simiacryptus.jopenai.proxy`                                                             |
 `RecentCodeEditsAction`      | Provides a menu for quickly applying recent custom code edits to the selected code.                                                                       | Select code, invoke action, choose recent edit from menu.           | `com.github.simiacryptus.aicoder.actions.ActionGroup`, `com.github.simiacryptus.aicoder.config.AppSettingsState`                                        |
 `RenameVariablesAction`      | Suggests variable name renames for the selected code using an AI model.                                                                                   | Select code, invoke action, choose renames to apply.                | `com.github.simiacryptus.aicoder.actions.SelectionAction`, `com.simiacryptus.jopenai.proxy`                                                             |
 `PrintTreeAction`            | Prints the tree structure of a PsiFile (Program Structure Interface file) to the log.                                                                     | Open file, invoke action, tree structure is printed to log.         | `com.github.simiacryptus.aicoder.actions.BaseAction`, `com.github.simiacryptus.aicoder.util.PsiUtil`                                                    |
 `InternalCoderAction`        | Launches a web-based interface for an internal coding agent that assists with coding tasks using natural language processing and machine learning models. | Invoke action, interact with coding agent via web interface.        | `com.github.simiacryptus.aicoder.ApplicationEvents`, `com.simiacryptus.skyenet.apps.coding.CodingAgent`, `com.simiacryptus.skyenet.webui.application.*` |

# code\DescribeAction.kt

Sure, here's a README file that explains the purpose and usage of the `DescribeAction` class:

## DescribeAction

The `DescribeAction` class is a part of the AI Coder plugin for IntelliJ IDEA. It provides a functionality to
automatically generate code documentation by describing the selected code snippet using natural language.

### Usage

1. Open your project in IntelliJ IDEA.
2. Select the code snippet you want to document.
3. Right-click on the selected code and choose "AI Coder" > "Describe Code" from the context menu.

The plugin will send the selected code, along with the detected programming language and the configured human language,
to an AI model. The AI model will then generate a natural language description of the code, which will be inserted as a
comment above the selected code.

### Implementation Details

The `DescribeAction` class extends the `SelectionAction` class and overrides the `processSelection` method. Here's a
breakdown of the key parts:

1. The `DescribeAction_VirtualAPI` interface defines a `describeCode` method that takes the code snippet, programming
   language, and human language as input, and returns a `DescribeAction_ConvertedText` object containing the generated
   description and its language.

2. The `proxy` property creates an instance of the `DescribeAction_VirtualAPI` using the `ChatProxy` class, which
   communicates with an AI model (specified by `AppSettingsState.instance.defaultChatModel()`).

3. The `processSelection` method:
    - Calls the `describeCode` method on the `proxy` instance, passing the selected code, programming language, and
      human language.
    - Wraps the generated description text to a maximum line length of 120 characters.
    - Determines the appropriate comment style (single-line or multi-line) based on the number of lines in the
      description.
    - Constructs a new string that includes the comment with the description, followed by the original selected code.

The `DescribeAction` class relies on the `IndentedText` and `StringUtil` classes from the project's utility classes for
text manipulation and formatting.

Note: This README assumes that the `DescribeAction` class is part of a larger project or plugin. Some details, such as
the configuration of the AI model and the implementation of the `ChatProxy` class, are not covered here.

# code\DocAction.kt

Sure, here's a README file that explains the purpose and usage of the `DocAction` plugin:

## DocAction Plugin

The `DocAction` plugin is an IntelliJ IDEA plugin that helps developers automatically generate documentation for their
code. It leverages the power of OpenAI's language models to analyze the selected code and generate detailed
documentation comments (e.g., JavaDoc, KDoc, etc.) based on the programming language and the user's preferences.

### Features

- Automatically generate documentation comments for selected code blocks
- Supports multiple programming languages (as long as they have a defined documentation style)
- Customizable settings for the OpenAI language model and temperature
- Integrates seamlessly with IntelliJ IDEA's code editor

### Usage

1. Install the plugin in IntelliJ IDEA.
2. Open a project and navigate to the code file you want to document.
3. Select the code block you want to generate documentation for.
4. Right-click on the selected code and choose "Generate Documentation" (or use the corresponding keyboard shortcut).
5. The plugin will analyze the selected code and generate the documentation comments, which will be inserted above the
   code block.

### Configuration

The plugin's behavior can be configured through the IntelliJ IDEA settings:

1. Go to `File` > `Settings` > `Tools` > `AI Coder`.
2. In the "AI Coder" settings panel, you can adjust the following options:
    - **Default Chat Model**: Select the OpenAI language model to use for generating documentation.
    - **Temperature**: Adjust the randomness of the generated text (higher values produce more diverse but potentially
      less coherent output).
    - **Human Language**: Set the language in which the documentation should be generated.

### Requirements

- IntelliJ IDEA (or other JetBrains IDEs based on the IntelliJ Platform)
- An OpenAI API key (required for using the language models)

### Contributing

Contributions to the `DocAction` plugin are welcome! If you encounter any issues or have suggestions for improvements,
please open an issue or submit a pull request on the project's GitHub repository.

### License

The `DocAction` plugin is released under the [MIT License](LICENSE).

# code\CustomEditAction.kt

Sure, here's a README file for the `CustomEditAction` plugin:

## CustomEditAction

The `CustomEditAction` plugin is an IntelliJ IDEA plugin that allows you to edit code using natural language
instructions. It leverages the power of OpenAI's language model to understand your instructions and modify the code
accordingly.

### Features

- **Natural Language Code Editing**: Provide natural language instructions to edit your code, such as "Add comments
  explaining the code," "Refactor this function to use a loop," or "Convert this code to Python."
- **Language Support**: The plugin supports editing code in various programming languages, including Java, Python,
  JavaScript, and more.
- **Customizable Settings**: Adjust settings like the OpenAI model to use, temperature, and human language for output.
- **Recent Commands History**: The plugin keeps track of your recent editing instructions, making it easy to reuse them.

### Usage

1. Install the plugin in your IntelliJ IDEA IDE.
2. Select the code you want to edit.
3. Right-click and choose "Edit Code" from the context menu (or use the keyboard shortcut, if configured).
4. Enter your natural language instruction in the dialog box.
5. The plugin will process your instruction and modify the selected code accordingly.

### Configuration

You can configure the plugin settings by navigating to `File > Settings > Tools > CustomEditAction`. Here, you can
adjust the following settings:

- **OpenAI API Key**: Enter your OpenAI API key to enable the plugin's functionality.
- **Temperature**: Adjust the temperature value to control the randomness of the generated output.
- **Default Chat Model**: Select the OpenAI model to use for code editing.
- **Human Language**: Choose the language in which you want to provide instructions and receive output.

### Contributing

Contributions to the `CustomEditAction` plugin are welcome! If you encounter any issues or have suggestions for
improvements, please open an issue or submit a pull request on the project's GitHub repository.

### License

The `CustomEditAction` plugin is released under the [MIT License](LICENSE).

# code\CommentsAction.kt

Sure, here's a README for the provided code:

## Comments Action

The `CommentsAction` class is an implementation of the `SelectionAction` interface in
the `com.github.simiacryptus.aicoder.actions.code` package. This action is designed to add comments to each line of the
selected code, explaining its functionality.

### Usage

1. Select the code you want to add comments to in your IDE.
2. Invoke the `CommentsAction` through the appropriate menu or keyboard shortcut.
3. The action will use the OpenAI API to generate comments for each line of the selected code.
4. The commented code will be displayed in the IDE.

### Implementation Details

#### `getConfig()`

This method returns an empty string, as no additional configuration is required for this action.

#### `isLanguageSupported()`

This method checks if the selected code is in a supported programming language. It returns `true` if
the `computerLanguage` is not null and not `ComputerLanguage.Text`.

#### `processSelection()`

This is the main method that performs the commenting operation. It creates an instance of the `ChatProxy` class, which
is responsible for interacting with the OpenAI API. The `ChatProxy` is configured with the following parameters:

- `clazz`: The `CommentsAction_VirtualAPI` interface, which defines the API contract for the commenting operation.
- `api`: The OpenAI API key.
- `temperature`: The temperature value for the OpenAI model, obtained from the `AppSettingsState` instance.
- `model`: The default chat model, obtained from the `AppSettingsState` instance.
- `deserializerRetries`: The number of retries for deserialization, set to 5.

The `ChatProxy` instance is then used to call the `editCode` method, passing the following arguments:

- `code`: The selected code.
- `operations`: The instruction for the OpenAI model, which is "Add comments to each line explaining the code".
- `computerLanguage`: The programming language of the selected code.
- `humanLanguage`: The language in which the comments should be generated, obtained from the `AppSettingsState`
  instance.

The `editCode` method returns a `CommentsAction_ConvertedText` object, which contains the commented code and the
language. The `processSelection` method returns the commented code.

#### `CommentsAction_VirtualAPI`

This interface defines the contract for the `editCode` method, which is used by the `ChatProxy` to interact with the
OpenAI API. It takes the following parameters:

- `code`: The code to be commented.
- `operations`: The instructions for the OpenAI model.
- `computerLanguage`: The programming language of the code.
- `humanLanguage`: The language in which the comments should be generated.

The `editCode` method returns a `CommentsAction_ConvertedText` object, which contains the commented code and the
language.

#### `CommentsAction_ConvertedText`

This class is a simple data holder for the commented code and the language. It has two properties:

- `code`: The commented code.
- `language`: The programming language of the code.

# code\InsertImplementationAction.kt

Sure, here's a README for the `InsertImplementationAction` class:

## InsertImplementationAction

The `InsertImplementationAction` class is an implementation of the `SelectionAction` interface. It is responsible for
generating code implementation based on a natural language specification provided in the form of code comments or
selected text.

### Functionality

The `InsertImplementationAction` class uses the OpenAI API to generate code implementation based on the provided
specification. Here's how it works:

1. The user selects a code comment or a piece of text in the editor.
2. The action extracts the natural language specification from the selected text or the code comment.
3. It sends the specification, along with the current file context and programming language information, to the OpenAI
   API.
4. The API generates the corresponding code implementation.
5. The generated code is inserted below the selected text or comment in the editor.

### Key Methods

#### `getProxy()`

This method returns an instance of the `VirtualAPI` interface, which is used to communicate with the OpenAI API. It
creates a `ChatProxy` object using the `VirtualAPI` class, the configured API key, the default chat model, temperature,
and deserialization retries.

#### `processSelection()`

This is the main method that handles the code generation process. It extracts the natural language specification from
the selected text or code comment, sends it to the OpenAI API via the `getProxy().implementCode()` method, and returns
the generated code.

#### `getPsiClassContextActionParams()`

This method retrieves the necessary parameters for the `PsiClassContext` class, which is used to provide the current
file context to the OpenAI API.

#### `isLanguageSupported()`

This method checks if the current programming language is supported by the action. It returns `false` for plain text or
Markdown files, and defers to the superclass implementation for other languages.

### Usage

The `InsertImplementationAction` class is likely integrated into an IDE plugin or a code editor extension. Users can
invoke the action through a keyboard shortcut or a menu item, which will trigger the code generation process based on
the selected text or code comment.

# code\PasteAction.kt

Sure, here's a README file that explains the functionality of the `PasteAction` class:

## PasteAction

The `PasteAction` class is an implementation of the `SelectionAction` class, which is used to perform actions on
selected text in the IntelliJ IDE. This particular action is designed to convert the text content of the system
clipboard from one programming language to another.

### Functionality

When the `PasteAction` is triggered, it performs the following steps:

1. Checks if the system clipboard contains any text data.
2. Retrieves the text content from the clipboard.
3. Detects the programming language of the clipboard text (using the `autodetect` option).
4. Converts the clipboard text to the target programming language specified by the current editor file type.
5. Returns the converted code as a string.

The conversion process is handled by a virtual API (`VirtualAPI`) that is implemented using the `ChatProxy` class from
the `com.simiacryptus.jopenai.proxy` package. This API provides a `convert` method that takes the input text, source
language, and target language as arguments and returns a `ConvertedText` object containing the converted code and the
target language.

### Usage

The `PasteAction` is designed to be used in the IntelliJ IDE as a context menu action or a keyboard shortcut. When
triggered, it will replace the selected text in the editor with the converted code from the clipboard.

### Requirements

- The `PasteAction` class requires the `com.simiacryptus.jopenai.proxy` package to be available in the project.
- The system clipboard must contain text data in a supported programming language.
- The target programming language must be supported by the conversion API.

### Limitations

- The `PasteAction` does not support converting plain text (non-code) content from the clipboard.
- The accuracy of the language conversion depends on the capabilities of the underlying conversion API.

### Configuration

The `PasteAction` class uses the `AppSettingsState` class to retrieve the default chat model and temperature settings
for the conversion process. These settings can be configured through the application's preferences or settings.

# code\ImplementStubAction.kt

Sure, here's a README for the `ImplementStubAction` class:

## ImplementStubAction

The `ImplementStubAction` class is an implementation of the `SelectionAction` interface in
the `com.github.simiacryptus.aicoder.actions.code` package. It is designed to generate code implementations for code
stubs or declarations using an AI language model.

### Functionality

The `ImplementStubAction` class provides the following functionality:

1. **Language Support**: It supports generating code implementations for various programming languages, except for plain
   text files.

2. **Selection Handling**: When no code selection is provided, it automatically selects the smallest code range within
   the editor context.

3. **Code Generation**: It uses the `VirtualAPI` interface to interact with an AI language model (currently using
   the `ChatProxy` implementation). The selected code declaration is sent to the language model, along with instructions
   to "Implement Stub". The generated code implementation is then returned.

4. **Configuration**: The class does not require any specific configuration.

### Dependencies

The `ImplementStubAction` class relies on the following dependencies:

- `com.github.simiacryptus.aicoder.actions.SelectionAction`: The base class for selection-based actions.
- `com.github.simiacryptus.aicoder.config.AppSettingsState`: Provides access to application settings, such as the
  default chat model and temperature.
- `com.github.simiacryptus.aicoder.util.ComputerLanguage`: Represents supported programming languages.
- `com.github.simiacryptus.aicoder.util.psi.PsiUtil`: Utility class for working with PSI (Program Structure Interface)
  elements.
- `com.simiacryptus.jopenai.proxy.ChatProxy`: A proxy implementation for interacting with an AI language model.
- `com.simiacryptus.jopenai.util.StringUtil`: Utility class for string manipulation.

### Usage

To use the `ImplementStubAction`, follow these steps:

1. Ensure that the required dependencies are available in your project.
2. Create an instance of the `ImplementStubAction` class.
3. Call the `processSelection` method with the appropriate `SelectionState` object, which contains the selected code and
   context information.
4. The generated code implementation will be returned as a `String`.

Example:

```kotlin
val action = ImplementStubAction()
val generatedCode = action.processSelection(selectionState, null)
```

Note that the `ImplementStubAction` class is designed to be used within the context of
the `com.github.simiacryptus.aicoder` project and may require additional setup or configuration depending on the project
structure and dependencies.

# code\RecentCodeEditsAction.kt

Sure, here's a README file that explains the purpose and usage of the `RecentCodeEditsAction` class:

```

## RecentCodeEditsAction

The `RecentCodeEditsAction` class is an ActionGroup in the IntelliJ IDEA plugin that provides a menu for quickly applying recent code edits to the selected code in the editor. It dynamically generates menu items based on the user's recent custom edit history, allowing them to quickly re-apply those edits to the current selection.


### Usage

1. Open a file in the IntelliJ IDEA editor and select a portion of code.
2. Right-click on the selected code and navigate to the "Recent Code Edits" submenu under the plugin's main menu item.
3. The submenu will display a list of recent custom edits, with each item prefixed by a number (e.g., "_1: Edit description", "2: Another edit description").
4. Choose the desired edit from the list by clicking on it.
5. The selected code will be updated with the chosen edit.


### Implementation Details

The `RecentCodeEditsAction` class extends the `ActionGroup` class from the IntelliJ Platform SDK. It overrides the `update` method to enable or disable the menu item based on whether there is a valid code selection in the editor.

The `getChildren` method is responsible for dynamically generating the menu items for recent custom edits. It retrieves the recent command history from the `AppSettingsState` and creates a `CustomEditAction` instance for each command. The `CustomEditAction` is a custom class that implements the actual code edit logic.

The `isEnabled` companion function checks if the current editor selection is valid for applying code edits. It uses the `UITools` and `ComputerLanguage` utilities to ensure that the selection is not plain text.


### Dependencies

This class relies on the following classes and utilities:

- `AppSettingsState`: Manages the application settings and recent command history.
- `ComputerLanguage`: Utility class for determining the programming language of the selected code.
- `UITools`: Utility class for working with the IntelliJ IDEA user interface.
- `AnAction`, `AnActionEvent`, `ActionGroup`: Classes from the IntelliJ Platform SDK for creating custom actions and action groups.
- `Project`: Class from the IntelliJ Platform SDK representing an IntelliJ IDEA project.

```

This README provides an overview of the `RecentCodeEditsAction` class, explaining its purpose, usage, implementation
details, and dependencies. It should help developers understand how this class fits into the overall plugin and how to
use and extend it if needed.

# code\RenameVariablesAction.kt

Sure, here's a README file that explains the functionality of the `RenameVariablesAction` plugin:

## RenameVariablesAction Plugin

The `RenameVariablesAction` plugin is an IntelliJ IDEA plugin that provides a convenient way to rename variables in your
code. It leverages the power of OpenAI's language model to suggest better variable names based on the context of your
code.

### Features

- Suggests variable name renames for the selected code
- Supports multiple programming languages (except plain text)
- Allows you to choose which variable renames to apply
- Applies the selected renames to the code

### Usage

1. Select the code for which you want to rename variables.
2. Right-click and choose the "Rename Variables" option from the context menu (or use the appropriate keyboard
   shortcut).
3. A dialog will appear, displaying the suggested variable renames.
4. Select the renames you want to apply by checking the corresponding checkboxes.
5. Click "OK" to apply the selected renames to your code.

### Configuration

The plugin uses the default settings from the `AppSettingsState` class for the OpenAI model and temperature. You can
modify these settings in the plugin's configuration if needed.

### Implementation Details

The `RenameVariablesAction` class extends the `SelectionAction` class and implements the `processSelection` method. It
uses the `ChatProxy` class to interact with the OpenAI language model and retrieve variable rename suggestions.

The `RenameAPI` interface defines the contract for the rename suggestion API. The `suggestRenames` method takes the
code, programming language, and human language as input and returns a `SuggestionResponse` object containing a list
of `Suggestion` objects. Each `Suggestion` object contains the original variable name and the suggested new name.

The `choose` method displays a dialog with checkboxes, allowing the user to select which variable renames to apply. The
selected renames are then applied to the code in the `processSelection` method.

### Dependencies

- `com.github.simiacryptus.aicoder.actions.SelectionAction`
- `com.github.simiacryptus.aicoder.config.AppSettingsState`
- `com.github.simiacryptus.aicoder.util.ComputerLanguage`
- `com.github.simiacryptus.aicoder.util.UITools`
- `com.simiacryptus.jopenai.proxy.ChatProxy`

### Contributing

Contributions to the `RenameVariablesAction` plugin are welcome! If you find any issues or have suggestions for
improvements, please open an issue or submit a pull request on the project's repository.

# dev\PrintTreeAction.kt

Sure, here's a README for the `PrintTreeAction` plugin:

## PrintTreeAction

The `PrintTreeAction` is an IntelliJ IDEA plugin that allows developers to print the tree structure of a PsiFile (
Program Structure Interface file) to the log. This can be useful for debugging and understanding the structure of a
file.

### Usage

To use the `PrintTreeAction` plugin, follow these steps:

1. Make sure the "devActions" setting is enabled in your IntelliJ IDEA settings.
2. Open the file you want to print the tree structure of.
3. Right-click in the editor and select the "PrintTreeAction" option from the context menu.

The tree structure of the file will be printed to the log.

### Implementation

The `PrintTreeAction` plugin is implemented in the `PrintTreeAction` class, which extends the `BaseAction` class. Here's
a breakdown of the class:

#### `handle` method

The `handle` method is called when the action is triggered. It uses the `PsiUtil` class to get the largest contained
entity (e.g., a file or a class) from the current editor context. It then calls the `printTree` method of `PsiUtil` to
print the tree structure of the entity to the log.

```kotlin
override fun handle(e: AnActionEvent) {
  log.warn(PsiUtil.printTree(PsiUtil.getLargestContainedEntity(e)!!))
}
```

#### `isEnabled` method

The `isEnabled` method checks if the "devActions" setting is enabled in the `AppSettingsState`. If it is, the action is
enabled and can be triggered.

```kotlin
override fun isEnabled(event: AnActionEvent): Boolean {
  return AppSettingsState.instance.devActions
}
```

#### Companion object

The `PrintTreeAction` class has a companion object that initializes a logger instance using SLF4J.

```kotlin
companion object {
  private val log = LoggerFactory.getLogger(PrintTreeAction::class.java)
}
```

### Dependencies

The `PrintTreeAction` plugin depends on the following classes and utilities:

- `BaseAction`: A base class for IntelliJ actions.
- `AppSettingsState`: A class that manages the application settings.
- `PsiUtil`: A utility class for working with the Program Structure Interface (PSI) in IntelliJ.

### Contributing

If you find any issues or have suggestions for improvements, feel free to open an issue or submit a pull request on the
project's GitHub repository.

# dev\InternalCoderAction.kt

Sure, here's a README file that explains the purpose and usage of the `InternalCoderAction` class:

## InternalCoderAction

The `InternalCoderAction` class is an action in the IntelliJ IDEA plugin that provides an internal coding agent. This
agent is designed to assist developers with coding tasks by leveraging natural language processing and machine learning
models.

### Purpose

The primary purpose of the `InternalCoderAction` is to provide an interactive coding assistant within the IntelliJ IDEA
environment. When triggered, it launches a web-based interface where developers can communicate with the coding agent
using natural language instructions. The agent can then interpret these instructions and provide code suggestions,
explanations, or even generate code snippets based on the developer's requirements.

### Usage

To use the `InternalCoderAction`, follow these steps:

1. Open your IntelliJ IDEA project.
2. Navigate to the "Tools" menu and select the "Internal Coder" option (or use the corresponding keyboard shortcut).
3. A new browser window or tab will open, displaying the web-based interface for the coding agent.
4. In the interface, you can enter your natural language instructions or queries related to your coding task.
5. The coding agent will process your input and provide relevant responses, such as code suggestions, explanations, or
   generated code snippets.
6. You can continue the conversation with the agent, refining your instructions or asking follow-up questions as needed.

### Key Features

- **Natural Language Processing**: The coding agent can understand and interpret natural language instructions, making
  it easier for developers to communicate their coding requirements.
- **Code Generation**: Based on the provided instructions, the agent can generate code snippets or complete solutions in
  the appropriate programming language.
- **Code Explanation**: The agent can provide explanations for existing code, helping developers understand the logic
  and functionality of the code.
- **Interactive Conversation**: The web-based interface allows for an interactive conversation between the developer and
  the coding agent, enabling iterative refinement of instructions and responses.

### Configuration

The `InternalCoderAction` class can be configured through the `AppSettingsState` class, which manages various settings
for the plugin. Specifically, the `defaultChatModel` method in `AppSettingsState` determines the machine learning model
used by the coding agent.

### Dependencies

The `InternalCoderAction` class relies on the following dependencies:

- `com.github.simiacryptus.aicoder.ApplicationEvents`
- `com.github.simiacryptus.aicoder.actions.BaseAction`
- `com.github.simiacryptus.aicoder.config.AppSettingsState`
- `com.github.simiacryptus.aicoder.util.IdeaKotlinInterpreter`
- `com.simiacryptus.jopenai.API`
- `com.simiacryptus.skyenet.apps.coding.CodingAgent`
- `com.simiacryptus.skyenet.core.platform.*`
- `com.simiacryptus.skyenet.webui.application.*`

Please ensure that these dependencies are properly included in your project for the `InternalCoderAction` to function
correctly.

# FileContextAction.kt

The provided code is a Kotlin class named `FileContextAction` that extends the `BaseAction` class. It is an abstract
class that is designed to work with files and folders in the context of an IntelliJ IDEA plugin. Here's a breakdown of
the class and its components:

1. **Class Definition**:
    - The class is parameterized with a generic type `T` which represents the configuration type for the action.
    - It takes two boolean parameters in the constructor: `supportsFiles` (default: `true`) and `supportsFolders` (
      default: `true`), which determine whether the action supports files and folders, respectively.

2. **Data Class**:
    - The class contains a nested data class named `SelectionState` that holds information about the selected file and
      the project root.

3. **Abstract Method**:
    - The `processSelection` method is an abstract method that must be implemented by subclasses.
    - It takes two parameters: `state` (an instance of `SelectionState`) and `config` (an optional instance of the
      generic type `T`).
    - It returns an array of `File` objects, which represent the files that should be processed by the action.

4. **Handle Method**:
    - The `handle` method is the entry point for the action.
    - It retrieves the selected file or folder from the event, as well as the project and project root.
    - It then spawns a new thread to execute the `processSelection` method and handle the resulting files.
    - The `UITools` class is used to perform various UI-related tasks, such as running a task with progress indication,
      writing files, and opening files in the editor.

5. **Configuration Retrieval**:
    - The `getConfig` method is an open method that can be overridden by subclasses to provide a configuration object of
      type `T`.

6. **Enabled State**:
    - The `isEnabled` method checks if the action should be enabled based on various conditions, such as whether the
      selected item is a file or folder, and whether the action is a development action (controlled by
      the `AppSettingsState`).

7. **Companion Object**:
    - The companion object contains a static `log` field for logging purposes.
    - It also includes an `open` function that opens a file in the IntelliJ IDEA editor.
    - The `open` function uses a scheduled thread pool to periodically check if the file exists and can be opened in the
      editor.

Overall, this class provides a framework for creating file-based actions in an IntelliJ IDEA plugin. Subclasses can
extend this class and implement the `processSelection` method to define the specific behavior of the action. The class
handles the UI interactions, file selection, and opening of generated files in the editor.

# generic\AppendAction.kt

Sure, here's a README for the `AppendAction` class:

## AppendAction

The `AppendAction` class is an implementation of the `SelectionAction` interface in
the `com.github.simiacryptus.aicoder.actions.generic` package. It is designed to append text to the end of a user's
selected text using OpenAI's chat API.

### Usage

1. The user selects a piece of text in the IDE.
2. The `AppendAction` is triggered (e.g., through a keyboard shortcut or menu item).
3. The selected text is sent to the OpenAI chat API as a user prompt, along with a system prompt instructing the model
   to append text to the end of the user's prompt.
4. The API response is processed, and the original selected text is concatenated with the appended text from the API
   response.
5. The resulting text is returned and can be used to replace the original selection or for further processing.

### Implementation Details

#### `getConfig(project: Project?): String`

This method is required by the `SelectionAction` interface but is not used in the `AppendAction` implementation. It
returns an empty string.

#### `processSelection(state: SelectionState, config: String?): String`

This is the main method of the `AppendAction` class. It performs the following steps:

1. Retrieves the current app settings from `AppSettingsState.instance`.
2. Constructs a `ChatRequest` object with the following properties:
    - `model`: The default chat model specified in the app settings.
    - `temperature`: The temperature value specified in the app settings.
    - `messages`: A list containing two `ChatMessage` objects:
        - A system message instructing the model to append text to the end of the user's prompt.
        - A user message containing the selected text.
3. Sends the `ChatRequest` to the OpenAI API using the `api.chat` method and the default chat model from the app
   settings.
4. Retrieves the response from the API and extracts the appended text from the first choice's message content.
5. Concatenates the original selected text with the appended text, ensuring that any overlap between the two is removed.
6. Returns the resulting text.

### Dependencies

The `AppendAction` class relies on the following dependencies:

- `com.github.simiacryptus.aicoder.actions.SelectionAction`
- `com.github.simiacryptus.aicoder.config.AppSettingsState`
- `com.intellij.openapi.project.Project`
- `com.simiacryptus.jopenai.ApiModel.*`
- `com.simiacryptus.jopenai.util.ClientUtil.toContentList`

Make sure these dependencies are properly imported and available in your project.

# generic\AnalogueFileAction.kt

Sure, here's a README for the `AnalogueFileAction` plugin:

## AnalogueFileAction

The `AnalogueFileAction` is an IntelliJ IDEA plugin that allows you to generate a new file based on an existing file and
a natural language directive. This can be useful for creating test cases, examples, or other related files based on your
existing code.

### Usage

1. Open the file you want to use as a base for generating the new file.
2. Right-click on the file in the Project view and select `Create Analogue File` from the context menu.
3. In the dialog that appears, enter your directive in the "Directive" text area. This should be a natural language
   instruction describing the file you want to generate.
4. Click "OK" to generate the new file.

The plugin will use the OpenAI API to generate the new file based on your directive and the code in the selected file.
The generated file will be saved in the same directory as the base file, with a unique filename.

### Configuration

The plugin uses the OpenAI API for generating the new files. You can configure the API key and other settings in the
IntelliJ IDEA settings under `Tools > AI Coder`.

### Example

Let's say you have a Java class `Calculator.java` with methods for basic arithmetic operations:

```java
public class Calculator {
    public int add(int a, int b) {
        return a + b;
    }

    public int subtract(int a, int b) {
        return a - b;
    }

    // ... other methods
}
```

To generate test cases for this class, you can use the directive "Create test cases for the Calculator class" in
the `AnalogueFileAction` dialog. The plugin will generate a new file `CalculatorTest.java` with test cases for the `add`
and `subtract` methods.

### Contributing

Contributions to the `AnalogueFileAction` plugin are welcome! If you find any issues or have suggestions for
improvements, please open an issue or submit a pull request on the project's GitHub repository.

# generic\AutoDevAction.kt

Sure, here's a README file that documents the code you provided:

## AutoDevAction

`AutoDevAction` is an IntelliJ IDEA plugin action that provides an automated development assistant for code modification
tasks. It allows users to provide natural language instructions, and the plugin generates an action plan, breaks it down
into tasks, and applies code changes based on the instructions.

### Features

- **Natural Language Instructions**: Users can provide high-level instructions in natural language to modify their
  codebase.
- **Action Plan Generation**: The plugin translates the user's instructions into an action plan, breaking it down into a
  list of tasks with descriptions and associated files.
- **Code Modification**: For each task, the plugin generates code patches (diffs) to implement the requested changes in
  the relevant files.
- **Interactive UI**: The plugin provides an interactive web UI where users can review the action plan, tasks, and code
  changes, and apply the modifications to their codebase.

### Usage

1. Open your project in IntelliJ IDEA.
2. Select the files or folders you want to modify.
3. Right-click and choose the "Auto Dev Assistant" action from the context menu.
4. In the web UI that opens, provide your natural language instructions for modifying the codebase.
5. Review the generated action plan, tasks, and code changes.
6. Apply the desired code modifications by clicking the provided links.

### Implementation Details

The `AutoDevAction` class extends `BaseAction` and handles the user interaction and plugin setup. It creates an instance
of the `AutoDevApp` class, which manages the application server and user sessions.

The `AutoDevApp` class is responsible for processing user messages and delegating the work to the `AutoDevAgent`.
The `AutoDevAgent` uses a set of actors (`ParsedActor` and `SimpleActor`) to generate the action plan and code changes
based on the user's instructions and the selected codebase.

The `TaskList` and `Task` data classes represent the action plan and individual tasks, respectively.

### Dependencies

The plugin relies on the following external libraries:

- `com.simiacryptus.jopenai`: A Java library for interacting with the OpenAI API.
- `com.simiacryptus.skyenet`: A framework for building intelligent systems and applications.

### Contributing

Contributions to the AutoDevAction plugin are welcome! If you find any issues or have suggestions for improvements,
please open an issue or submit a pull request on the project's GitHub repository.

# generic\CreateFileAction.kt

```
File: README.md
```

## AI Coder Plugin

The AI Coder Plugin is a powerful tool that leverages advanced natural language processing and code generation
capabilities to assist developers in their coding tasks. This plugin seamlessly integrates with your favorite IDE,
providing a convenient and efficient way to generate code snippets, files, and even entire projects based on natural
language instructions.

### Features

- **Code Generation**: Describe your coding requirements in plain English, and the plugin will generate the
  corresponding code for you. Whether you need a specific function, class, or an entire file, the AI Coder Plugin can
  handle it.

- **File Creation**: Need a new file for your project? Simply provide a natural language description, and the plugin
  will create the file with the appropriate code based on your requirements.

- **Project Generation**: For larger tasks, you can instruct the plugin to generate an entire project structure,
  complete with necessary files and code, based on your high-level project description.

- **Context Awareness**: The plugin takes into account the context of your existing codebase, ensuring that the
  generated code is consistent with your project's coding style, conventions, and dependencies.

### Getting Started

1. Install the AI Coder Plugin in your preferred IDE.
2. Open a project or create a new one.
3. Access the plugin's interface (e.g., through a menu or toolbar button).
4. Enter your natural language instructions or requirements for the desired code, file, or project.
5. Review the generated code and make any necessary adjustments.
6. Save or integrate the generated code into your project.

### Contributing

We welcome contributions from the community to enhance the AI Coder Plugin's capabilities and improve its performance.
If you encounter any issues or have suggestions for new features, please submit them through the project's issue
tracker. Pull requests with bug fixes or enhancements are also greatly appreciated.

### License

The AI Coder Plugin is released under the [MIT License](LICENSE). Feel free to use, modify, and distribute the plugin as
per the terms of the license.

# generic\CodeChatAction.kt

Sure, here's a README file that explains the purpose and usage of the `CodeChatAction` class:

## CodeChatAction

The `CodeChatAction` class is an IntelliJ IDEA plugin action that allows users to initiate a code chat session with an
AI assistant. This action is designed to help developers get assistance with their code by providing a conversational
interface where they can ask questions, get explanations, and receive suggestions from the AI.

### Usage

To use the `CodeChatAction`, follow these steps:

1. Open an IntelliJ IDEA project with a file containing code you want to discuss.
2. Select the code you want to discuss or leave the cursor in the file if you want to discuss the entire file.
3. Go to the "Tools" menu and select "Code Chat" (or use the corresponding keyboard shortcut).
4. A new browser window or tab will open, displaying the code chat interface.
5. In the chat interface, you can start a conversation with the AI assistant by typing your questions or comments
   related to the selected code.
6. The AI assistant will respond with relevant information, explanations, or suggestions based on the provided code and
   your input.

### Features

- Automatically detects the programming language of the selected code.
- Provides a conversational interface for interacting with the AI assistant.
- Supports sticky input, allowing you to continue the conversation without re-entering the code.
- Integrates with the IntelliJ IDEA editor, enabling easy access to the code chat functionality.

### Configuration

The `CodeChatAction` class uses the following configuration options:

- `AppSettingsState.instance.defaultChatModel()`: Specifies the default AI model to use for the code chat session.
- `ApplicationServices.dataStorageFactory(root)`: Configures the data storage location for the code chat session.

### Dependencies

The `CodeChatAction` class relies on the following dependencies:

- `com.github.simiacryptus.aicoder` library for AI-related functionality.
- `com.simiacryptus.skyenet.core.platform` library for platform-specific services.
- `com.simiacryptus.skyenet.webui` library for web-based user interfaces.

### Contributing

Contributions to the `CodeChatAction` plugin are welcome. If you encounter any issues or have suggestions for
improvements, please open an issue or submit a pull request on the project's repository.

# generic\DictationAction.kt

Sure, here's a README file that explains the functionality of the `DictationAction` class:

## DictationAction

The `DictationAction` class is an IntelliJ IDEA plugin action that allows users to dictate text into their code editor
using speech recognition. When the action is triggered, it starts recording audio from the user's microphone and sends
the recorded audio to an AI-powered speech-to-text service for transcription. The transcribed text is then inserted into
the code editor at the current cursor position or replaces the selected text if there is a selection.

### How it Works

1. When the action is triggered, a status dialog is displayed to indicate that the recording and dictation process has
   started.
2. Three separate threads are created:
    - `dication-audio-recorder`: This thread continuously records audio from the user's microphone and stores the raw
      audio data in a buffer.
    - `dictation-audio-processor`: This thread processes the raw audio data from the buffer, applying noise reduction
      and other audio processing techniques to improve speech recognition accuracy.
    - `dictation-api-processor`: This thread takes the processed audio data and sends it to an AI-powered speech-to-text
      service for transcription. The transcribed text is then inserted into the code editor.
3. The `DictationPump` inner class handles the interaction with the speech-to-text service and the insertion of the
   transcribed text into the code editor.
4. The dictation process continues until the user closes the status dialog window.

### Dependencies

This plugin relies on the following external libraries:

- `com.simiacryptus.jopenai`: A Java library for interacting with the OpenAI API, which is used for speech-to-text
  transcription.
- `javax.sound.sampled`: A Java library for audio recording and processing.

### Configuration

The plugin does not require any additional configuration out of the box. However, you may need to set up an OpenAI API
key and configure the `com.simiacryptus.jopenai` library accordingly.

### Usage

To use the dictation feature, follow these steps:

1. Open an IntelliJ IDEA project.
2. Open a code editor window.
3. Trigger the `DictationAction` by going to the `Tools` menu and selecting the "Dictation" option, or by using the
   keyboard shortcut (if configured).
4. The status dialog window will appear, indicating that the recording and dictation process has started.
5. Start speaking into your microphone. The transcribed text will be inserted into the code editor at the current cursor
   position or replace the selected text if there is a selection.
6. To stop the dictation process, close the status dialog window.

Note: Ensure that your microphone is properly configured and working before using the dictation feature.

# generic\LineFilterChatAction.kt

Sure, here's a README file that explains the purpose and usage of the `LineFilterChatAction` class:

## LineFilterChatAction

The `LineFilterChatAction` class is an IntelliJ IDEA plugin action that allows you to interact with an AI assistant for
coding assistance. It provides a chat interface where you can ask questions about the selected code or the entire file,
and the AI assistant will respond with relevant information and explanations.

### Usage

1. Open an IntelliJ IDEA project with a code file.
2. Select the code you want to discuss with the AI assistant, or leave it unselected to discuss the entire file.
3. Right-click and choose the "Code Chat" action from the context menu, or use the keyboard shortcut (if configured).
4. A new browser window or tab will open, displaying a chat interface with the selected code or file content.
5. Type your question or comment in the chat input field and press Enter.
6. The AI assistant will analyze the code and provide a relevant response.
7. Continue the conversation by asking follow-up questions or providing additional context.

### Features

- **Code Context**: The AI assistant has access to the selected code or the entire file content, allowing it to provide
  context-aware responses.
- **Line References**: You can reference specific lines of code in your responses by including the line number (
  e.g., `\nLINE\n`). The AI assistant will replace these references with the actual code lines.
- **Markdown Support**: Responses from the AI assistant can include Markdown formatting for better readability and
  formatting.
- **Persistent Session**: The chat session is persistent, allowing you to continue the conversation even after closing
  and reopening the chat interface.

### Configuration

The `LineFilterChatAction` class uses the `AppSettingsState` to configure the default chat model. You can modify
the `defaultChatModel()` method in the `AppSettingsState` class to change the AI model used for code assistance.

### Dependencies

This plugin relies on the following dependencies:

- `com.github.simiacryptus:aicoder` - The main AI coding assistant library.
- `com.simiacryptus:skyenet-core` - The core library for the SkyeNet AI platform.
- `com.simiacryptus:skyenet-webui` - The web user interface library for the SkyeNet AI platform.

### Contributing

Contributions to this plugin are welcome! If you find any issues or have suggestions for improvements, please open an
issue or submit a pull request on the project's GitHub repository.

# generic\DiffChatAction.kt

Sure, here's a README file that documents the code you provided:

## DiffChatAction

`DiffChatAction` is an IntelliJ IDEA plugin action that allows users to chat with an AI assistant and receive code
suggestions in the form of diffs (line additions and deletions). The action opens a web interface where the user can
interact with the AI assistant and apply the suggested code changes directly to their code editor.

### How it Works

1. The user selects some code in their IntelliJ IDEA editor or has an editor open with code.
2. The user invokes the `DiffChatAction` (e.g., via a keyboard shortcut or menu item).
3. The action creates a new `CodeChatSocketManager` instance, which manages the communication between the client (web
   interface) and the AI assistant.
4. The `CodeChatSocketManager` is configured to provide code suggestions in the diff format, with line additions and
   deletions marked with `+` and `-`, respectively.
5. A new session is created, and the selected code (or the entire file if no selection) is sent to the AI assistant.
6. The user's default web browser is opened, pointing to the web interface hosted by the `AppServer`.
7. The user can chat with the AI assistant in the web interface, and the assistant will provide code suggestions in the
   diff format.
8. The web interface includes links to apply the suggested code changes directly to the user's code editor.

### Key Components

#### `DiffChatAction`

The main action class that handles the user's action and sets up the necessary components.

#### `CodeChatSocketManager`

Manages the communication between the client (web interface) and the AI assistant. It sends the user's code and prompts
to the AI assistant and receives the assistant's responses.

#### `AppServer`

Hosts the web interface and manages the communication between the client and the server.

#### `ChatServer`

Represents the web application that handles the chat sessions and interactions with the AI assistant.

### Usage

To use the `DiffChatAction`, you need to have the plugin installed in your IntelliJ IDEA environment. Once installed,
you can invoke the action via a keyboard shortcut or menu item (depending on your plugin configuration).

After invoking the action, your default web browser will open, and you can start chatting with the AI assistant in the
web interface. The assistant will provide code suggestions in the diff format, and you can apply the suggested changes
directly to your code editor by clicking on the provided links.

### Configuration

The `DiffChatAction` uses the `AppSettingsState` class to retrieve the default chat model for the AI assistant. You can
configure the default chat model by modifying the `AppSettingsState` class or providing a different implementation.

### Dependencies

The `DiffChatAction` relies on the following dependencies:

- `com.github.simiacryptus.aicoder` (main project)
- `com.simiacryptus.skyenet.core.platform` (for platform-specific functionality)
- `com.simiacryptus.skyenet.webui` (for web UI components)
- `org.slf4j` (for logging)

Make sure to include these dependencies in your project for the `DiffChatAction` to work correctly.

# generic\DocumentationCompilerAction.kt

Sure, here's a README file that documents the code you provided:

## Documentation Compiler Action

The `DocumentationCompilerAction` is an IntelliJ IDEA plugin action that allows you to generate documentation for a set
of code files using OpenAI's language model. It provides a user interface to select the files to process, specify the AI
instruction for generating the documentation, and set the output file name.

### Usage

1. Open the project in IntelliJ IDEA.
2. Right-click on the folder containing the files you want to document.
3. Select the "Compile Documentation" action from the context menu.
4. In the "Compile Documentation" dialog:
    - Select the files you want to include in the documentation by checking the corresponding checkboxes in the "Files
      to Process" list.
    - Provide the AI instruction for generating the documentation in the "AI Instruction" field (e.g., "Create user
      documentation").
    - Specify the name of the output file in the "Output File" field (e.g., "compiled_documentation.md").
5. Click "OK" to start the documentation generation process.

The generated documentation will be saved in the specified output file within the selected folder.

### Implementation Details

The `DocumentationCompilerAction` class extends the `FileContextAction` class and provides the following functionality:

1. `isEnabled(event: AnActionEvent)`: Checks if the selected file is a directory. If not, the action is disabled.
2. `getConfig(project: Project?, e: AnActionEvent)`: Displays the "Compile Documentation" dialog and collects the user
   settings (files to process, AI instruction, and output file name).
3. `processSelection(state: SelectionState, config: Settings?)`: Performs the actual documentation generation process.
    - It creates a fixed thread pool for parallel processing of files.
    - For each selected file, it sends the file content and the AI instruction to the OpenAI API for generating the
      documentation.
    - The generated documentation is appended to a StringBuilder and written to the specified output file.
    - The output file is opened in the IntelliJ IDEA editor.

The `DocumentationCompilerDialog` class is responsible for displaying the "Compile Documentation" dialog and collecting
the user settings.

The `open(project: Project, outputPath: Path)` companion function is used to open the generated documentation file in
the IntelliJ IDEA editor.

### Dependencies

The `DocumentationCompilerAction` relies on the following dependencies:

- `com.github.simiacryptus.aicoder.actions.FileContextAction`
- `com.github.simiacryptus.aicoder.config.AppSettingsState`
- `com.github.simiacryptus.aicoder.config.Name`
- `com.github.simiacryptus.aicoder.util.UITools`
- `com.intellij.openapi.actionSystem.AnActionEvent`
- `com.intellij.openapi.application.ApplicationManager`
- `com.intellij.openapi.fileEditor.FileEditorManager`
- `com.intellij.openapi.project.Project`
- `com.intellij.openapi.ui.DialogWrapper`
- `com.intellij.openapi.vfs.LocalFileSystem`
- `com.intellij.ui.CheckBoxList`
- `com.intellij.ui.components.JBScrollPane`
- `com.intellij.ui.components.JBTextField`
- `com.simiacryptus.jopenai.ApiModel`
- `com.simiacryptus.jopenai.util.ClientUtil`
- `org.apache.commons.io.IOUtils`

Note: This README assumes familiarity with the IntelliJ IDEA plugin development environment and the dependencies used in
the provided code.

# generic\MultiDiffChatAction.kt

Sure, here's a README file that explains the purpose and usage of the `MultiDiffChatAction` plugin:

## MultiDiffChatAction Plugin

The `MultiDiffChatAction` plugin is an IntelliJ IDEA plugin that provides a convenient way to interact with an AI
assistant for modifying multiple code files simultaneously. It allows you to select a set of files, and then opens a
web-based chat interface where you can provide natural language instructions to the AI assistant. The assistant will
generate code diffs (additions and deletions) for the selected files based on your instructions, and you can apply these
diffs directly to the files within the IntelliJ IDE.

### Features

- Select multiple files in the IntelliJ project view
- Open a web-based chat interface with an AI assistant
- Provide natural language instructions to modify the selected files
- Receive code diffs from the AI assistant, formatted with context lines
- Apply the generated diffs directly to the files within IntelliJ

### Usage

1. Open your IntelliJ IDEA project containing the files you want to modify.
2. In the project view, select the files you want to work with.
3. Right-click on the selected files and choose "Multi-file Diff Chat" from the context menu (or use the corresponding
   keyboard shortcut).
4. A web browser window will open, displaying a chat interface with the selected files' contents.
5. Type your natural language instructions in the chat input field and press Enter.
6. The AI assistant will generate code diffs based on your instructions and display them in the chat window.
7. Review the generated diffs, and if you're satisfied, click on the "Apply Diff" links to apply the changes directly to
   the corresponding files in your IntelliJ project.

### Example

Let's say you have a set of JavaScript files containing functions for handling user authentication. You want to modify
these functions to use a different authentication method. Here's how you could use the `MultiDiffChatAction` plugin:

1. Select the relevant JavaScript files in your IntelliJ project.
2. Open the "Multi-file Diff Chat" interface.
3. In the chat, provide instructions like: "Change all functions that use the `authenticateWithPassword` method to use
   the `authenticateWithToken` method instead."
4. The AI assistant will generate code diffs showing the necessary changes to the selected files.
5. Review the diffs and apply them to update your code accordingly.

### Installation

To install the `MultiDiffChatAction` plugin, follow these steps:

1. In IntelliJ IDEA, go to `File` > `Settings` > `Plugins`.
2. Click on the "Marketplace" tab and search for "MultiDiffChatAction".
3. Click the "Install" button and follow the prompts to complete the installation.
4. Restart IntelliJ IDEA for the changes to take effect.

### Contributing

If you find any issues or have suggestions for improvements, please feel free to open an issue or submit a pull request
on the project's GitHub repository.

# generic\ReplaceOptionsAction.kt

Sure, here's a README file that explains the purpose and usage of the `ReplaceOptionsAction` plugin:

## ReplaceOptionsAction Plugin

The `ReplaceOptionsAction` plugin is an IntelliJ IDEA plugin that provides a convenient way to replace selected text
with one of the suggested options generated by an AI language model. This plugin leverages the OpenAI API to generate
relevant and contextual suggestions based on the selected text and its surrounding context.

### Features

- Generates multiple options for replacing the selected text using an AI language model
- Considers the context before and after the selected text to provide relevant suggestions
- Displays a dialog box with the generated options, allowing the user to choose the desired replacement

### Usage

1. Install the plugin in your IntelliJ IDEA IDE.
2. Open a code file or document where you want to replace some text.
3. Select the text you want to replace.
4. Right-click on the selected text and choose "Replace Options" from the context menu (or use the appropriate keyboard
   shortcut).
5. A dialog box will appear with multiple options generated by the AI language model.
6. Select the desired option from the list, and it will replace the selected text in your code or document.

### Configuration

The `ReplaceOptionsAction` plugin uses the OpenAI API to generate suggestions. You need to provide your OpenAI API key
in the plugin settings. Additionally, you can configure the following options:

- **Default Chat Model**: Specify the AI language model to use for generating suggestions.
- **Temperature**: Adjust the randomness or creativity of the generated suggestions.

### Requirements

- IntelliJ IDEA IDE
- OpenAI API key (obtain one from the OpenAI website)

### Contributing

Contributions to the `ReplaceOptionsAction` plugin are welcome! If you encounter any issues or have suggestions for
improvements, please open an issue or submit a pull request on the project's GitHub repository.

### License

This plugin is released under the [MIT License](LICENSE).

# generic\RedoLast.kt

```markdown

## AI Coder Plugin for IntelliJ IDEA

The AI Coder plugin for IntelliJ IDEA is a powerful tool that integrates artificial intelligence into your coding
workflow. This plugin allows you to leverage the capabilities of AI to generate, modify, and refactor code, making your
development process more efficient and productive.

### Features

- **Code Generation**: Use natural language prompts to generate code snippets, classes, or entire files. The AI will
  understand your requirements and generate the corresponding code.
- **Code Refactoring**: Let the AI analyze your existing code and suggest refactoring opportunities to improve
  readability, performance, and maintainability.
- **Code Explanation**: Struggling to understand a piece of code? Use the AI to provide clear and concise explanations
  of what the code does and how it works.
- **Code Completion**: The AI can assist you with intelligent code completion suggestions, saving you time and effort.

### Installation

1. Open IntelliJ IDEA and navigate to `File > Settings > Plugins`.
2. Click on the `Marketplace` tab and search for "AI Coder".
3. Click on the "Install" button next to the AI Coder plugin.
4. Restart IntelliJ IDEA after the installation is complete.

### Usage

After installing the plugin, you can access its features through the dedicated toolbar or context menus within the
editor.

#### Code Generation

1. Open the file or class where you want to generate code.
2. Right-click and select "AI Coder > Generate Code".
3. Provide a natural language prompt describing the code you want to generate.
4. The AI will analyze your prompt and generate the corresponding code.

#### Code Refactoring

1. Select the code you want to refactor.
2. Right-click and select "AI Coder > Refactor Code".
3. The AI will analyze your code and suggest refactoring opportunities.
4. Review the suggestions and apply the desired changes.

#### Code Explanation

1. Select the code you want to understand.
2. Right-click and select "AI Coder > Explain Code".
3. The AI will provide a clear and concise explanation of what the code does and how it works.

#### Code Completion

1. While typing code, the AI will provide intelligent code completion suggestions based on the context.
2. Use the standard code completion shortcut (e.g., Ctrl+Space) to see the AI-powered suggestions.

### Contributing

We welcome contributions to the AI Coder plugin! If you encounter any issues or have suggestions for improvements,
please open an issue on the project's GitHub repository. Pull requests with bug fixes or new features are also
appreciated.

### License

The AI Coder plugin is released under the [MIT License](LICENSE).

### Support

If you have any questions or need further assistance, please reach out to our support team
at [support@aicoder.com](mailto:support@aicoder.com).
```

This README provides an overview of the AI Coder plugin for IntelliJ IDEA, including its features, installation
instructions, usage examples, contributing guidelines, license information, and support contact details. Feel free to
customize it further based on your specific requirements.

# generic\WebDevAction.kt

```markdown

## Web Dev Assistant Plugin

This plugin provides a tool for developing web applications within the IntelliJ IDE. It leverages the power of AI to
assist with various aspects of web development, including architecture design, code generation, and code review.

### Features

- **Architecture Design**: Describe your web application idea, and the plugin will generate a detailed architecture
  plan, including suggested frameworks/libraries, user interactions, coding styles, and a list of required files with
  descriptions.

- **Code Generation**: Based on the architecture plan, the plugin can generate skeleton code for HTML, JavaScript, CSS,
  and other file types needed for your web application.

- **Code Review**: Submit your code, and the plugin will analyze it, identify potential issues, and provide code patches
  with fixes and improvements.

- **Iterative Development**: The plugin supports an iterative development workflow, allowing you to provide feedback and
  refine the generated code through a conversational interface.

### Usage

1. Open the IntelliJ IDE and navigate to the project or directory where you want to develop your web application.

2. Right-click on the project or directory and select the "Web Dev Assistant" action from the context menu.

3. In the dialog that appears, describe your web application idea or requirements.

4. The plugin will generate an architecture plan and initial code files based on your input.

5. Review the generated code and provide feedback or additional instructions through the conversational interface.

6. The plugin will refine the code based on your feedback and generate code patches or updates.

7. Repeat steps 5 and 6 until you are satisfied with the code.

### Requirements

- IntelliJ IDEA (or other JetBrains IDE)
- An active internet connection (for communicating with the AI model)

### Contributing

Contributions to this plugin are welcome! If you encounter any issues or have suggestions for improvements, please open
an issue or submit a pull request on the project's GitHub repository.

### License

This plugin is released under the [MIT License](LICENSE).
```

This README provides an overview of the Web Dev Assistant plugin, including its features, usage instructions,
requirements, and information about contributing and licensing. It aims to give users a clear understanding of the
plugin's capabilities and how to utilize it effectively for web development within the IntelliJ IDE.

# markdown\MarkdownImplementActionGroup.kt

Sure, here's a README file that explains the functionality of the `MarkdownImplementActionGroup` plugin:

## Markdown Implement Action Group

The `MarkdownImplementActionGroup` is an IntelliJ IDEA plugin that provides a convenient way to convert selected text in
Markdown files into code snippets for various programming languages. This plugin is particularly useful when you need to
include code examples or snippets within your Markdown documentation.

### Features

- Supports a wide range of programming languages, including Java, Python, C++, JavaScript, Ruby, and many more.
- Converts selected text in Markdown files into code snippets with proper syntax highlighting.
- Utilizes OpenAI's language model to intelligently convert natural language instructions into code.
- Configurable settings for temperature and model selection.

### Usage

1. Open a Markdown file in IntelliJ IDEA.
2. Select the text you want to convert into code.
3. Right-click on the selected text and navigate to the "Markdown Implement" submenu.
4. Choose the desired programming language from the list.
5. The selected text will be converted into a code snippet with the appropriate syntax highlighting for the chosen
   language.

### Configuration

The plugin uses the settings from the `AppSettingsState` class to configure the OpenAI model and temperature used for
code generation. You can modify these settings in the plugin's configuration panel (if available) or by directly editing
the `AppSettingsState` class.

### Contributing

Contributions to this plugin are welcome! If you encounter any issues or have suggestions for improvements, please open
an issue or submit a pull request on the project's GitHub repository.

### License

This plugin is released under the [MIT License](LICENSE).

# generic\TaskRunnerAction.kt

```markdown

## Task Runner Plugin

The Task Runner Plugin is an IntelliJ plugin designed to assist developers in task planning, code generation,
documentation, and file management. It integrates with a web UI and utilizes AI models to break down tasks, generate
code, create documentation, and handle inquiries.

### Features

- **Task Planning**: Break down high-level user requests into smaller, actionable tasks suitable for software
  development.
- **Code Generation**: Generate new files or modify existing files based on specified requirements and context.
- **Documentation**: Automatically generate detailed documentation for code, covering purpose, functionality, inputs,
  outputs, and design decisions.
- **Inquiries**: Provide comprehensive information and insights on specified topics, leveraging AI models to compile
  relevant knowledge.
- **Web UI Integration**: Interact with the plugin through a web-based user interface, enabling remote collaboration and
  task management.

### Architecture

The Task Runner Plugin follows a modular architecture, consisting of the following key components:

1. **IntelliJ Plugin**: The main entry point for developers, allowing them to initiate the task planning process and
   interact with the web UI.
2. **Web UI**: A web-based user interface that facilitates user input, task management, and displaying generated
   outputs.
3. **Task Runner Agent**: The central processor that handles user messages and delegates tasks to specific actors based
   on the task type.
4. **Actor System**: A collection of actors responsible for executing various tasks, such as task breakdown,
   documentation generation, file creation, patching, and inquiries.
5. **AI Models**: Leverages OpenAI's language models (e.g., GPT-4, GPT-3.5) to power the task breakdown, code
   generation, and inquiry handling capabilities.

### Getting Started

1. Install the Task Runner Plugin in your IntelliJ IDE.
2. Open a project or select a set of files you want to work with.
3. Initiate the task planning process by invoking the plugin action (e.g., via a keyboard shortcut or menu item).
4. Enter your high-level task or request in the web UI.
5. The plugin will break down the task into smaller, actionable steps and guide you through the process of code
   generation, documentation, and inquiries.

### Contributing

Contributions to the Task Runner Plugin are welcome! If you encounter any issues or have suggestions for improvements,
please open an issue or submit a pull request on the project's GitHub repository.

### License

The Task Runner Plugin is released under the [MIT License](LICENSE).
```

This README provides an overview of the Task Runner Plugin, its features, architecture, and instructions for getting
started. It also includes information about contributing to the project and the license under which it is released.

# markdown\MarkdownListAction.kt

Sure, here's a README for the `MarkdownListAction` plugin:

## MarkdownListAction

The `MarkdownListAction` is an IntelliJ IDEA plugin that provides a convenient way to generate new list items for an
existing Markdown list. It leverages OpenAI's language model to intelligently suggest relevant and contextual list items
based on the existing list.

### Features

- Automatically detects Markdown lists in the current editor
- Generates new list items based on the existing list content
- Supports different bullet styles (-, *, and numbered lists)
- Preserves indentation and formatting of the original list

### Usage

1. Open a Markdown file in IntelliJ IDEA.
2. Select or place the cursor within an existing Markdown list.
3. Right-click and choose "Generate List Items" from the context menu, or use the keyboard shortcut (if configured).
4. The plugin will generate new list items and append them to the existing list.

### Requirements

- IntelliJ IDEA (or other JetBrains IDEs with Markdown support)
- An OpenAI API key (configured in the plugin settings)

### Configuration

Before using the plugin, you'll need to configure your OpenAI API key in the plugin settings:

1. Go to `File` > `Settings` > `Tools` > `AI Coder`.
2. Enter your OpenAI API key in the "API Key" field.
3. Optionally, adjust other settings as needed (e.g., default chat model).

### Implementation Details

The `MarkdownListAction` plugin uses the following components:

- `MarkdownListAction`: The main action class that handles user interactions and triggers the list item generation
  process.
- `ListAPI`: An interface that defines the contract for generating new list items using OpenAI's language model.
- `ChatProxy`: A utility class that facilitates communication with the OpenAI API and handles deserialization of
  responses.
- `PsiUtil`: A utility class that provides methods for working with the IntelliJ Platform's Program Structure
  Interface (PSI).
- `UITools`: A utility class that provides helper methods for interacting with the IntelliJ user interface and
  performing read/write operations on documents.

The plugin leverages the OpenAI API to generate new list items based on the existing list content. It uses
the `ChatProxy` class to send a request to the OpenAI API with the existing list items and the desired number of new
items. The API response is then parsed and appended to the original list in the Markdown file.

### Contributing

Contributions to the `MarkdownListAction` plugin are welcome! If you encounter any issues or have suggestions for
improvements, please open an issue or submit a pull request on the project's GitHub repository.

# SelectionAction.kt

Sure, here's a README file that documents the code you provided:

```

## AI Coder Plugin

The AI Coder Plugin is an IntelliJ IDEA plugin that provides various code editing actions powered by artificial intelligence. It allows you to perform tasks such as code generation, refactoring, and documentation directly within the IDE.


### Features

- **Code Generation**: Generate code snippets based on natural language descriptions or examples.
- **Code Refactoring**: Refactor existing code to improve its structure, readability, or performance.
- **Code Documentation**: Automatically generate documentation for code based on its structure and context.
- **And more**: The plugin is designed to be extensible, allowing for the addition of new AI-powered actions.


### Usage

1. Install the AI Coder Plugin in your IntelliJ IDEA.
2. Open a project or file you want to work with.
3. Select the code you want to process or place the cursor where you want to generate new code.
4. Right-click and choose the desired action from the "AI Coder" submenu.
5. Follow the prompts or provide any necessary input.
6. The plugin will process your request and apply the changes to your code.


### Contributing

Contributions to the AI Coder Plugin are welcome! If you find any issues or have suggestions for improvements, please open an issue on the project's GitHub repository. If you'd like to contribute code, follow these steps:

1. Fork the repository.
2. Create a new branch for your feature or bug fix.
3. Make your changes and commit them with descriptive commit messages.
4. Push your changes to your forked repository.
5. Submit a pull request to the main repository.


### License

The AI Coder Plugin is released under the [MIT License](LICENSE).


### Acknowledgments

The AI Coder Plugin was developed by [Your Name or Organization] and utilizes various open-source libraries and technologies, including:

- [Library 1]
- [Library 2]
- [...]

Special thanks to the developers and contributors of these projects.
```

This README provides an overview of the AI Coder Plugin, its features, usage instructions, contributing guidelines,
license information, and acknowledgments. Feel free to modify it as needed to better fit your project's specific
details.

