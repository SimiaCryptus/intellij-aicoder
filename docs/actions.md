# code\CommentsAction.kt


## CommentsAction Plugin Documentation


### Overview

The `CommentsAction` plugin is designed to enhance your coding experience by automatically adding comments to your code. This feature is particularly useful for understanding complex code segments or for documentation purposes. The plugin leverages the capabilities of AI through a virtual API to analyze your code and generate insightful comments.


### Features

- **Language Support**: The plugin supports a variety of programming languages, ensuring broad applicability across different coding projects. It excludes plain text to maintain focus on actual code.
- **AI-Powered Comments**: Utilizes an AI model to generate comments, providing context and explanations for each line of code.
- **Customizable Settings**: Integrates with `AppSettingsState` to allow customization of AI behavior, including temperature settings and model selection, to tailor the comments to your preference.


### How to Use

1. **Installation**: Ensure the plugin is installed and enabled in your development environment.
2. **Selection**: Highlight the code segment you wish to comment within your project.
3. **Activation**: Trigger the `CommentsAction` either through a designated shortcut or menu option.
4. **Configuration**: The first time you use the plugin, you might need to configure settings such as the AI temperature and preferred model in `AppSettingsState`.
5. **Execution**: Once activated, the plugin communicates with the AI via the `ChatProxy`, sending the selected code and receiving the commented version.


### API Integration


#### CommentsAction_VirtualAPI

This interface defines the `editCode` method, which is responsible for sending the code to the AI and receiving the commented version. It requires the following parameters:

- `code`: The selected code segment.
- `operations`: A description of the desired operation, in this case, "Add comments to each line explaining the code".
- `computerLanguage`: The programming language of the code.
- `humanLanguage`: The language in which the comments should be written.


#### CommentsAction_ConvertedText

This class encapsulates the response from the AI, containing the commented code and the language of the comments.


### Configuration

The plugin uses settings from `AppSettingsState` for customization:

- `temperature`: Controls the creativity of the AI. A higher temperature may result in more creative comments.
- `defaultChatModel()`: Specifies the AI model to use for generating comments.
- `humanLanguage`: Sets the language for the comments, allowing for localization of comments.


### Limitations

- **Language Support**: The plugin does not support plain text as it focuses on actual code.
- **AI Dependence**: The quality of comments is dependent on the AI model's understanding of the code and the provided settings.


### Conclusion

The `CommentsAction` plugin is a powerful tool for developers looking to enhance their code with insightful comments automatically. By leveraging AI, it simplifies the documentation process and aids in code comprehension. Customize the settings to fit your needs and enjoy a more informative coding experience.

# BaseAction.kt


## BaseAction Class Documentation

The `BaseAction` class is an abstract class designed for creating actions within the IntelliJ IDEA plugin developed by the `com.github.simiacryptus.aicoder` project. This class extends the `AnAction` class provided by the IntelliJ Platform SDK, allowing developers to implement custom actions within the IDE. Below is a detailed explanation of the `BaseAction` class, its constructors, methods, and usage.


### Overview

The `BaseAction` class provides a structured way to define actions with optional name, description, and icon. It encapsulates common functionalities needed for actions, such as logging, error handling, and API access, making it easier for developers to implement their specific action logic.


### Constructors

The `BaseAction` class constructor accepts three optional parameters:

- `name`: A `String` representing the name of the action. This name appears in menus or toolbars where the action is displayed.
- `description`: A `String` providing a short description of what the action does. This can be shown as a tooltip or in other contexts within the IDE.
- `icon`: An `Icon` object representing the icon to be displayed alongside the action's name in menus or toolbars.


### Properties

- `api`: Provides access to the `OpenAIClient` instance, allowing actions to interact with OpenAI services.


### Methods


#### `update(event: AnActionEvent)`

This method is called to determine whether the action is available and visible to the user. It sets the action's visibility and availability based on the `isEnabled(event: AnActionEvent)` method.

- `event`: The `AnActionEvent` object representing the current action event.


#### `handle(e: AnActionEvent)`

This abstract method must be implemented by subclasses to define the action's behavior.

- `e`: The `AnActionEvent` object representing the current action event.


#### `actionPerformed(e: AnActionEvent)`

Executes the action. It logs the action's execution, sets the last event in `IdeaOpenAIClient`, and calls the `handle(e: AnActionEvent)` method. If an error occurs during the action's execution, it is caught and logged.

- `e`: The `AnActionEvent` object representing the current action event.


#### `isEnabled(event: AnActionEvent): Boolean`

Determines whether the action is enabled and visible based on the current context. By default, it returns `true`, making the action always available. This method can be overridden in subclasses to provide context-specific availability.

- `event`: The `AnActionEvent` object representing the current action event.


### Usage

To create a custom action using the `BaseAction` class, you need to extend this class and implement the `handle(e: AnActionEvent)` method to define what the action does. Optionally, you can override the `isEnabled(event: AnActionEvent)` method to control the action's availability based on the context.

```kotlin
class MyCustomAction : BaseAction("My Action", "This is my custom action", MyIcon) {
    override fun handle(e: AnActionEvent) {
        // Implement action logic here
    }
}
```

In this example, `MyCustomAction` is a custom action with a specified name, description, and icon. The action's behavior is defined within the `handle` method.


### Conclusion

The `BaseAction` class provides a convenient base for creating actions within the IntelliJ IDEA plugin, handling common functionalities such as logging and error management. By extending this class and implementing the required methods, developers can easily add custom actions to enhance the IDE's functionality.

# code\DescribeAction.kt


#### DescribeAction Plugin Documentation

The `DescribeAction` plugin is a powerful tool designed to enhance your coding experience by providing automatic code descriptions within your IDE. This plugin leverages the capabilities of AI to generate human-readable explanations for code snippets, making it easier to understand and document your codebase. Below is a detailed guide on how to use the `DescribeAction` plugin and understand its components.


##### Features

- **Automatic Code Descriptions**: Generate descriptions for selected code snippets in your preferred human language.
- **Support for Multiple Languages**: Works with various programming languages, adapting the comment style accordingly.
- **Customizable**: Leverage the settings from `AppSettingsState` to customize the behavior, such as the AI's temperature and the model used for generating descriptions.


##### Components


###### DescribeAction_VirtualAPI

An interface that defines the method `describeCode`, which is responsible for sending the code snippet to an AI service and receiving a human-readable description. It also includes an inner class `DescribeAction_ConvertedText` to encapsulate the response, containing the text of the description and its language.


###### DescribeAction

The main class that extends `SelectionAction<String>`, implementing the logic to interact with the user's selection, process it through the AI service, and insert the generated description into the code.


##### How to Use

1. **Selection**: Highlight the code snippet you want to describe within your IDE.
2. **Activation**: Trigger the `DescribeAction` plugin through your IDE's action or shortcut keys.
3. **Configuration**: (Optional) Before using the plugin, you can adjust settings like AI temperature and preferred human language in `AppSettingsState`.
4. **Description Insertion**: The plugin processes your selected code, communicates with the AI service, and inserts the generated description as a comment directly above the selected code snippet.


##### Customization

- **AI Temperature**: Adjust the creativity of the AI. A higher temperature may result in more creative descriptions, while a lower temperature tends to generate more straightforward and concise explanations.
- **Human Language**: Set your preferred language for the descriptions to ensure the output is understandable and accessible to your project's audience.
- **Model Selection**: Choose the AI model that best fits your needs, depending on the complexity and domain of your code.


##### Example

Given a code snippet, the plugin will generate a description and insert it as a comment. For instance, if the selected code is a function to calculate Fibonacci numbers, the plugin might insert a comment like:

```java
// This function calculates the nth Fibonacci number using recursion.
public int fibonacci(int n) {
    if (n <= 1) return n;
    return fibonacci(n-1) + fibonacci(n-2);
}
```


##### Conclusion

The `DescribeAction` plugin is an invaluable tool for developers looking to improve their code documentation and comprehension. By automating the generation of code descriptions, it saves time and enhances the readability of your codebase. Customize the plugin settings to fit your project's needs and enjoy a more streamlined coding experience.

# code\ImplementStubAction.kt


## ImplementStubAction Plugin Documentation

The `ImplementStubAction` plugin is designed to assist developers in automatically implementing stubs in their codebase. This plugin leverages a virtual API to edit and implement code stubs based on the user's selection within the IDE. Below is a detailed guide on how to use this plugin and understand its components.


### Features

- **Language Support**: The plugin supports various programming languages, excluding plain text. It automatically detects the language of the selected code stub and processes it accordingly.
- **Automatic Stub Implementation**: By selecting a code stub, the plugin communicates with a virtual API to generate and insert the implementation directly into your codebase.
- **Customizable Settings**: Users can adjust settings such as the model's temperature and the desired output human language through the `AppSettingsState` configuration.


### Components


#### VirtualAPI Interface

The `VirtualAPI` interface is a crucial component that defines the method `editCode`, which is responsible for sending the code stub to the virtual API and receiving the implemented code. It also includes an inner class `ConvertedText` to encapsulate the response, including the implemented code and its language.


#### ImplementStubAction Class

The `ImplementStubAction` class extends `SelectionAction<String>` and is the main class that users interact with. It provides several key functionalities:

- **Language Support Check**: Through the `isLanguageSupported` method, it checks if the selected code's language is supported for stub implementation.
- **Default Selection**: The `defaultSelection` method determines the default code selection for stub implementation based on the editor's state.
- **Process Selection**: The `processSelection` method captures the user's selection, prepares the code stub, and communicates with the `VirtualAPI` to implement the stub. It then inserts the implemented code back into the editor.


#### Usage

1. **Select a Code Stub**: In your IDE, highlight the code stub you wish to implement.
2. **Invoke the ImplementStubAction**: Use the designated shortcut or menu option to trigger the `ImplementStubAction`.
3. **Review and Save**: The plugin will automatically replace the selected stub with the implemented code. Review the changes and save your file.


### Configuration

Users can customize the behavior of the `ImplementStubAction` plugin through the `AppSettingsState` configuration. This includes setting the desired output human language, adjusting the model's temperature for code generation, and specifying the default chat model.


### Conclusion

The `ImplementStubAction` plugin is a powerful tool for developers looking to streamline the process of implementing code stubs. By leveraging a virtual API and customizable settings, it offers a seamless integration into your development workflow, saving time and enhancing code quality.

# code\CustomEditAction.kt


## CustomEditAction Plugin Documentation

The `CustomEditAction` plugin is designed to enhance your coding experience by allowing you to perform custom edits on your code through a simple and intuitive interface. This plugin leverages a virtual API to interact with a code editing service, enabling you to modify code based on specific instructions in a natural language. Below is a detailed guide on how to use the `CustomEditAction` plugin within your projects.


### Features

- **Custom Code Edits**: Perform edits on your code by specifying instructions in natural language.
- **Support for Multiple Languages**: The plugin can handle code and instructions in various programming and human languages.
- **Integration with AI Services**: Utilizes an AI backend (via `ChatProxy`) for processing edit instructions and generating edited code.


### How to Use


#### Prerequisites

Ensure you have the plugin installed in your development environment. The plugin requires access to the `ChatProxy` service for processing edit requests.


#### Performing a Custom Edit

1. **Select Code**: Highlight the code snippet you wish to edit in your editor.
2. **Invoke Custom Edit Action**: Trigger the `CustomEditAction` from the context menu or assigned keyboard shortcut.
3. **Enter Instruction**: A dialog box will appear prompting you to enter the edit instruction. Input your instruction in natural language, describing how you want the code to be modified.
4. **Review and Apply**: The plugin processes your instruction and returns the edited code. Review the changes and apply them to your codebase.


#### Example

If you have the following code:

```java
System.out.println("Hello, World!");
```

And you wish to add a comment above this line, you would:

1. Select the line of code.
2. Trigger the `CustomEditAction`.
3. Enter the instruction: "Add a comment saying this prints a greeting message".
4. The plugin processes the request and returns the edited code:

```java
// This prints a greeting message
System.out.println("Hello, World!");
```


### Configuration

The plugin's behavior can be customized via the `AppSettingsState` configuration, where you can set preferences such as:

- **Temperature**: Controls the creativity of the AI service.
- **Model**: Selects the AI model used for processing instructions.
- **Human Language**: Sets the default language for instructions.


### VirtualAPI

The `VirtualAPI` interface is a crucial component of the plugin, facilitating the interaction with the AI backend. It defines the `editCode` method, which takes the code snippet, edit instruction, programming language, and human language as inputs and returns the edited code.


#### EditedText Data Class

The `EditedText` data class represents the edited code, containing the modified code snippet and its language.


### Conclusion

The `CustomEditAction` plugin offers a powerful and flexible way to edit code using natural language instructions. By integrating AI capabilities, it streamlines the code editing process, making it more efficient and user-friendly. Whether you're looking to refactor code, add comments, or perform other custom edits, this plugin provides an innovative solution to enhance your coding workflow.

# code\DocAction.kt


#### DocAction Plugin Documentation

The `DocAction` plugin is designed to enhance your coding experience by automatically generating documentation for selected code blocks within your project. This plugin leverages a virtual API to process the code and generate detailed documentation in the desired human language. Below is a comprehensive guide on how to use the `DocAction` plugin within your projects.


##### Features

- **Automatic Documentation Generation**: Automatically generates documentation for selected code blocks.
- **Language Support**: Supports multiple programming languages, with the ability to specify the documentation style based on the language.
- **Customizable**: Offers customization options through `AppSettingsState`, such as setting the default chat model, temperature for the generation process, and the human language for the documentation.


##### How to Use

1. **Installation**: Ensure the `DocAction` plugin is installed and enabled in your IDE.

2. **Selection of Code**: Highlight the code block for which you want to generate documentation.

3. **Activation**: Activate the `DocAction` process, which can typically be done through a context menu or a keyboard shortcut. The exact activation method may vary based on your IDE's configuration.

4. **Documentation Generation**: Upon activation, the plugin processes the selected code block and automatically generates the documentation, appending it directly above the selected code block in the editor.


##### Configuration

The plugin behavior can be customized through the `AppSettingsState` configuration, which includes:

- **Default Chat Model**: Sets the default model used by the chat proxy for generating documentation.
- **Temperature**: Adjusts the creativity/variability of the generated documentation.
- **Human Language**: Specifies the language in which the documentation should be generated.


##### API Overview

The plugin defines a `DocAction_VirtualAPI` interface for processing code blocks, with the following key method:

- `processCode(code: String, operation: String, computerLanguage: String, humanLanguage: String)`: Processes the given code block and returns a `DocAction_ConvertedText` object containing the generated documentation.


##### Customization Example

To customize the plugin, you can adjust the settings in `AppSettingsState`. For example, to change the human language for documentation to Spanish, you would modify the `humanLanguage` property accordingly.


##### Language Support

The plugin supports multiple programming languages. However, it requires that the language has a defined documentation style (`docStyle`). Text blocks or languages without a defined `docStyle` are not supported.


##### Editing Selection

The plugin intelligently adjusts the selection to encompass the entire code block, ensuring that the documentation is generated for the complete block rather than a partial selection.


#### Conclusion

The `DocAction` plugin is a powerful tool for developers looking to automate the generation of documentation for their code. By leveraging this plugin, you can significantly reduce the time and effort required to document your code, ensuring that your projects are well-documented and maintainable.

# code\InsertImplementationAction.kt


## InsertImplementationAction Plugin Documentation


### Overview

The `InsertImplementationAction` plugin is designed to assist developers by automatically generating code implementations based on comments or selected text within the IntelliJ IDEA environment. This plugin leverages a Virtual API, powered by AI models, to interpret the specifications provided in natural language and produce corresponding code snippets in the desired programming language.


### Features

- **Automatic Code Generation**: Generate code implementations directly from comments or selected text.
- **Support for Multiple Languages**: Works with various programming languages, adapting to the specific syntax and idioms of each.
- **Customizable AI Parameters**: Utilize settings from `AppSettingsState` to customize the behavior of the AI, including the choice of model and temperature settings for code generation.
- **Integration with IntelliJ IDEA**: Seamlessly integrates with the IntelliJ IDEA environment, providing a smooth workflow for developers.


### How to Use

1. **Select Text or Comment**: Begin by selecting a piece of text or a comment in your code that describes the functionality you wish to implement. The plugin can interpret natural language specifications to generate code.

2. **Invoke the Plugin**: Use the designated shortcut or menu option to activate the `InsertImplementationAction`. The plugin will analyze the selected text or comment.

3. **Review Generated Code**: The plugin will communicate with the Virtual API to generate a code snippet based on the provided specification. Once the code is generated, it will be inserted into your codebase at the appropriate location. You can review and adjust the generated code as necessary.


### Configuration

The plugin behavior can be customized through the `AppSettingsState` configuration, where you can set the preferred human and computer languages, adjust the AI model, and set the temperature for code generation. These settings allow you to tailor the code generation process to your specific needs and preferences.


### Supported Languages

While the plugin aims to support a wide range of programming languages, it explicitly excludes Text and Markdown from its supported languages due to their nature. The plugin is continuously updated to expand its language support and improve its code generation capabilities.


### Integration Points

- **VirtualAPI**: The core interface through which the plugin communicates with the AI models to generate code. It defines the `implementCode` method, which takes in specifications in both human and computer languages and returns generated code.
- **PsiClassContext**: Utilizes IntelliJ IDEA's PSI (Program Structure Interface) to understand the context of the code, enhancing the relevance and accuracy of the generated code snippets.


### Limitations

- The accuracy and relevance of the generated code depend significantly on the clarity and specificity of the provided specifications.
- Currently, the plugin may not support all programming languages with the same level of efficacy. The effectiveness can vary based on the AI model's training and the language's complexity.


### Conclusion

The `InsertImplementationAction` plugin offers a powerful tool for developers looking to streamline their coding workflow by automating the generation of code snippets from natural language specifications. By integrating AI capabilities directly into the IntelliJ IDEA environment, it provides a unique solution to reduce manual coding effort and enhance productivity.

# code\RecentCodeEditsAction.kt


## RecentCodeEditsAction Plugin Documentation


### Overview

The `RecentCodeEditsAction` plugin is designed to enhance your coding experience within the IntelliJ IDEA environment by providing quick access to your most recent custom code edits. This feature allows you to execute frequently used or recent custom code modifications with ease, streamlining your development workflow.


### Features

- **Quick Access to Recent Edits**: Displays a list of the most recently used custom code edits, allowing you to reapply them with a single click.
- **Dynamic Action List**: Generates action items dynamically based on your recent custom edits history, ensuring that the most relevant actions are always at your fingertips.
- **Language Sensitivity**: The plugin is sensitive to the programming language of the current context, enabling it for use only when appropriate and ensuring compatibility with your project's language.


### How to Use

1. **Installation**: Ensure the plugin is installed in your IntelliJ IDEA environment. This might require downloading it from the JetBrains Marketplace or a custom plugin repository.

2. **Accessing Recent Edits**: Navigate to the action or menu where `RecentCodeEditsAction` is integrated. This could be a toolbar, context menu, or any other UI component designed to host actions.

3. **Executing an Edit**: Click on one of the dynamically listed actions representing your recent code edits. The selected edit will be applied to the current context or selection in your code.

4. **Navigating Through Actions**: Actions are prefixed with numbers for easier identification. Actions numbered 1 through 9 are also prefixed with an underscore (_) for quicker access.


### Requirements

- **IntelliJ IDEA**: This plugin is designed to work within the IntelliJ IDEA environment. Ensure you have a compatible version installed.
- **Selection**: A selection in the editor is required for the plugin to be enabled. The plugin checks for an active selection to ensure that the actions can be applied meaningfully.
- **Language Compatibility**: The plugin is disabled for plain text files and is only enabled for supported programming languages as determined by the `ComputerLanguage` utility.


### Troubleshooting

- **Plugin Not Enabled**: Ensure you have a selection in your editor. The plugin requires an active selection to operate.
- **No Actions Displayed**: If no actions are displayed, it might be because there are no recent custom edits recorded. Start making custom edits using the plugin's features, and they should appear as you build up a history.
- **Language Support**: If the plugin is not enabled for your file, check if the file's language is supported. The plugin does not activate for plain text files.


### Conclusion

The `RecentCodeEditsAction` plugin is a powerful tool for developers looking to optimize their coding workflow in IntelliJ IDEA. By providing quick access to recent custom code edits, it helps reduce repetitive tasks, allowing you to focus more on creative aspects of your development work. Ensure to follow the usage guidelines and requirements for the best experience.

# code\PasteAction.kt


## PasteAction Plugin Documentation

The `PasteAction` plugin is designed to enhance your coding experience by allowing you to easily convert and paste code snippets from one programming language to another directly within your IDE. This document provides a comprehensive guide on how to use the `PasteAction` plugin, including its features, requirements, and step-by-step instructions.


### Features

- **Clipboard Content Conversion**: Automatically detects and converts the content of your clipboard from one programming language to another.
- **Language Autodetection**: Capable of identifying the source language of the clipboard content for seamless conversion.
- **Custom API Integration**: Utilizes a virtual API for the conversion process, ensuring accurate and efficient language translation.
- **Support for Multiple Languages**: Except for plain text, the plugin supports a wide range of programming languages, making it versatile for various coding projects.


### Requirements

- **IDE Compatibility**: This plugin is developed for use with the IntelliJ IDEA platform. Ensure your IDE version is compatible.
- **API Access**: The plugin leverages a virtual API (`VirtualAPI`) for code conversion. Ensure you have access to this API and have configured the necessary settings in `AppSettingsState`.


### Getting Started


#### Installation

1. Download the `PasteAction` plugin from the designated repository or marketplace.
2. Open your IntelliJ IDEA IDE and navigate to `File` > `Settings` > `Plugins`.
3. Click on `Install Plugin from Disk...` and select the downloaded plugin file.
4. Restart your IDE to activate the plugin.


#### Configuration

Before using the plugin, you need to configure the default settings in `AppSettingsState`, including the default chat model and temperature settings for the conversion API.

1. Navigate to `AppSettingsState` in your project settings.
2. Set the `defaultChatModel` to your preferred model for code conversion.
3. Adjust the `temperature` setting according to your conversion accuracy preference.


#### Usage

1. **Copy Code Snippet**: Copy the code snippet you wish to convert from its source.
2. **Trigger PasteAction**: Use the designated shortcut or navigate through the IDE's action menu to activate `PasteAction`.
3. **Conversion and Paste**: The plugin automatically detects the language of the copied code, converts it to the target language based on your current file or project settings, and pastes the converted code into your editor.


#### Supported Languages

The plugin supports a variety of programming languages for conversion. However, it does not support plain text (`ComputerLanguage.Text`) as a source or target language for conversion.


### Troubleshooting

- **Clipboard Detection Issues**: Ensure your clipboard contains text or code. The plugin may not detect non-text content correctly.
- **Conversion Errors**: Check your `AppSettingsState` configuration, especially the API settings, to ensure they are correct and active.
- **Language Support**: If the conversion does not work as expected, verify that the source and target languages are supported and correctly identified by the plugin.


### Conclusion

The `PasteAction` plugin is a powerful tool for developers looking to streamline their coding workflow by effortlessly converting and pasting code snippets across different programming languages. By following this guide, you should be able to install, configure, and effectively use the plugin within your IDE.

# code\RenameVariablesAction.kt


## Rename Variables Action Plugin Documentation


### Overview

The Rename Variables Action plugin is a powerful tool designed to assist developers in refactoring their code by suggesting and applying variable name changes. This plugin leverages AI technology to provide meaningful and context-aware renaming suggestions, making your code more readable and maintainable.


### Features

- **AI-Powered Suggestions:** Utilizes an AI model to suggest variable name changes based on the context and semantics of your code.
- **Support for Multiple Languages:** Capable of suggesting renames in various programming languages, ensuring wide applicability.
- **Interactive Selection:** Allows users to review and select which suggested renames to apply, giving you full control over the refactoring process.
- **Integration with IntelliJ Platform:** Seamlessly integrates with the IntelliJ IDE, providing a smooth and efficient user experience.


### How to Use

1. **Select Code:** Begin by selecting the block of code or the specific variables you wish to rename within your project.
2. **Activate Plugin:** With the code selected, activate the Rename Variables Action plugin. This can typically be done through a context menu or a dedicated shortcut, depending on your IDE setup.
3. **Review Suggestions:** The plugin will present a list of suggested renames based on the selected code. Each suggestion will display the original variable name and the proposed new name.
4. **Choose Renames:** From the list of suggestions, choose the renames you wish to apply. You can select all suggestions or only specific ones according to your preference.
5. **Apply Changes:** Once you've made your selections, confirm to apply the changes. The plugin will refactor the selected code, replacing the original variable names with your chosen new names.


### Configuration

No additional configuration is required to start using the Rename Variables Action plugin. However, the plugin's behavior can be influenced by the settings found in `AppSettingsState`, such as the AI model used for suggestions and the desired level of suggestion creativity (temperature).


### Supported Languages

The plugin supports a variety of programming languages but excludes plain text. The exact list of supported languages can evolve over time, so please refer to the plugin's documentation or settings for the most current information.


### Troubleshooting

- **No Suggestions Generated:** Ensure that the selected code block contains variables that can be renamed. Also, check if the selected programming language is supported by the plugin.
- **Plugin Not Responding:** Verify that your IDE is up to date and that there are no conflicts with other installed plugins. Restarting the IDE can also resolve many issues.


### Conclusion

The Rename Variables Action plugin is an invaluable tool for developers looking to improve their code quality through better naming conventions. By leveraging AI to suggest contextually relevant renames, this plugin not only saves time but also enhances code readability and maintainability.

# dev\InternalCoderAction.kt


## IntelliJ Internal Coder Plugin Documentation

The IntelliJ Internal Coder Plugin is designed to enhance your coding experience by integrating advanced coding assistance directly into your IntelliJ IDE. This document provides an overview of the plugin's features, setup instructions, and how to use it effectively.


### Features

- **Seamless Integration**: Works within the IntelliJ environment, providing a smooth workflow without the need to switch between applications.
- **Coding Assistance**: Leverages the power of AI to offer coding suggestions, helping you to code more efficiently and effectively.
- **Customizable Sessions**: Each coding session is unique, with support for custom symbols and editor states to tailor the experience to your needs.
- **Interactive UI**: Offers an interactive web-based UI for an enhanced coding experience, accessible directly from your browser.


### Prerequisites

Before you can use the IntelliJ Internal Coder Plugin, ensure you have the following:

- IntelliJ IDE installed on your system.
- Java Development Kit (JDK) installed and configured.
- Access to the internet for downloading the plugin and its dependencies.


### Installation

1. **Download the Plugin**: Download the IntelliJ Internal Coder Plugin from the JetBrains Marketplace or the GitHub repository.
2. **Install in IntelliJ**: Open IntelliJ IDE, navigate to `Settings` > `Plugins`, and choose `Install Plugin from Disk...`. Select the downloaded plugin file and restart IntelliJ.


### Getting Started

After installation, the plugin is ready for use. Here's how to get started:

1. **Enable Developer Actions**: Ensure that developer actions are enabled in the plugin settings under `AppSettingsState`.
2. **Access the Plugin**: Find the plugin under the `Tools` menu or search for `Internal Coder Action` in the action search bar (`Ctrl+Shift+A`).
3. **Start a Session**: Initiate a coding session. The plugin will handle the setup, including server initialization and session management.


### Usage

- **Coding Assistance**: Once a session is started, the plugin provides AI-powered coding suggestions based on the context of your project and the current file.
- **Interactive UI**: Access the interactive UI through your web browser by following the URL provided by the plugin. This UI allows for real-time interaction with the coding agent.
- **Custom Symbols**: Enhance your coding session by adding custom symbols related to your project or the specific task at hand.


### Troubleshooting

- **Browser Issues**: If you encounter issues opening the interactive UI in your browser, ensure your default browser is set correctly and supports modern web technologies.
- **Connectivity**: Ensure you have a stable internet connection for the plugin to communicate with its backend services.


### Conclusion

The IntelliJ Internal Coder Plugin offers a powerful addition to your development toolkit, integrating AI-powered coding assistance directly into your IDE. By following the steps outlined in this document, you can enhance your coding efficiency and enjoy a more interactive coding experience.

For further assistance or to report issues, please visit the plugin's GitHub repository or contact the support team.

# dev\PrintTreeAction.kt


## PrintTreeAction Plugin Documentation


### Overview

The PrintTreeAction plugin is a powerful tool designed for developers working within the IntelliJ IDE. It provides a simple yet effective way to visualize the tree structure of a PsiFile directly in the log. This feature can be incredibly useful for understanding the organization and hierarchy of code files, aiding in debugging, and enhancing overall code comprehension.


### Getting Started

Before you can use the PrintTreeAction plugin, ensure that it is installed and properly configured in your IntelliJ environment. Follow these steps to get started:

1. **Enable Dev Actions**: The plugin requires the "devActions" setting to be enabled. This can be done by navigating to the plugin's settings under `AppSettingsState` and ensuring that the `devActions` flag is set to `true`.

2. **Open a File**: Open the file for which you want to print the tree structure. The plugin works with any file that can be represented as a PsiFile within IntelliJ, covering a wide range of programming languages and file types.

3. **Access the Plugin**: Right-click within the open file to bring up the editor context menu. Look for the "PrintTreeAction" action and select it. This action triggers the plugin to print the tree structure of the current file to the log.


### Features

- **Tree Structure Visualization**: Quickly prints the hierarchical tree structure of the selected PsiFile to the IntelliJ log, making it easier to understand the file's organization.
- **Easy Access**: Can be triggered directly from the editor context menu, providing a seamless and intuitive user experience.
- **Development Tool**: Especially useful for developers looking to debug or get a better grasp of the file structure within their projects.


### Usage

To use the PrintTreeAction plugin, follow these simple steps:

1. Ensure the `devActions` setting is enabled in `AppSettingsState`.
2. Open the file you wish to analyze.
3. Right-click to open the context menu and select "PrintTreeAction".
4. Check the IntelliJ log to view the printed tree structure of the file.


### Requirements

- IntelliJ IDE: The plugin is designed to work within the IntelliJ ecosystem.
- `devActions` Enabled: This setting must be enabled for the plugin to function.


### Troubleshooting

- **Action Not Visible**: If the "PrintTreeAction" is not visible in the context menu, ensure that the `devActions` setting is enabled and that you have the necessary permissions to modify plugin settings.
- **Null Pointer Exception**: If you encounter a null pointer exception in the log, ensure that the file you are trying to analyze is fully loaded and recognized as a PsiFile by IntelliJ.


### Conclusion

The PrintTreeAction plugin is a valuable tool for developers seeking to enhance their understanding of code structure within the IntelliJ IDE. By providing a straightforward method to visualize the tree structure of PsiFiles, it aids in debugging, code analysis, and overall project comprehension. Ensure that the `devActions` setting is enabled and start leveraging the power of PrintTreeAction to streamline your development workflow.

# dev\AppServer.kt


## AppServer Plugin Documentation

The `AppServer` plugin is designed to facilitate the integration and management of web applications within a development environment. It provides a seamless way to host and dynamically manage web applications, specifically chat applications, using the Jetty server. This documentation outlines how to utilize the `AppServer` class within your project.


### Features

- **Dynamic Application Management**: Easily add or remove web applications at runtime without needing to restart the server.
- **WebSocket Support**: Integrated support for WebSocket applications, making it suitable for real-time applications like chat servers.
- **Project Integration**: Designed to work within a project scope, allowing for project-specific server instances.
- **Progress Monitoring**: Includes a mechanism to monitor and manage the server's running state within the project's context.


### Getting Started

To use the `AppServer` within your project, ensure you have the necessary dependencies for Jetty server and WebSocket support in your project's build configuration.


#### Initialization

The `AppServer` is designed as a singleton within the scope of a project, ensuring only one instance runs at any given time. To get or start the server, use:

```kotlin
val server = AppServer.getServer(project)
```

Where `project` is an instance of `com.intellij.openapi.project.Project`. This method checks if an instance already exists and is running; if not, it creates and starts a new server instance.


#### Adding Applications

To add a new web application (e.g., a chat server) to the server:

```kotlin
server.addApp("/path", chatServerInstance)
```

- `/path` is the context path where the application will be accessible.
- `chatServerInstance` is an instance of `ChatServer`, which is your WebSocket server implementation.

This method dynamically adds the new application to the server. If the server is already running, it will be restarted to reflect the changes.


#### Stopping the Server

To stop the server, you can call:

```kotlin
AppServer.stop()
```

This method stops the server if it's running and clears the current instance, allowing a new one to be created later.


### Advanced Usage


#### Customizing WebAppContext

The `newWebAppContext` method is used internally to configure each web application context added to the server. It sets up WebSocket support and other necessary configurations. You can customize this method if you need to adjust the context settings for your applications.


#### Monitoring Server Progress

The server's running state is monitored in a separate thread, which allows for actions like cancellation or restart based on the server's state. This is particularly useful for integrating the server's lifecycle with your project's UI, providing feedback to the user about the server's status.


### Conclusion

The `AppServer` plugin provides a robust and flexible way to integrate web applications, including real-time WebSocket applications, into your development environment. By following the instructions outlined in this documentation, you can effectively manage and interact with web applications within your project.

# generic\GenerateRelatedFileAction.kt


## Analogue File Action Plugin Documentation

The Analogue File Action plugin is a powerful tool designed to assist developers in generating new files based on existing ones, guided by natural language directives. This plugin integrates seamlessly with the IntelliJ IDEA environment, leveraging AI to understand the context and content of your files and create analogues that match your specified criteria.


### Features

- **Contextual File Generation**: Generate new files that are contextually similar to selected ones, based on natural language instructions.
- **Intelligent Directive Processing**: Utilizes AI to interpret directives and produce relevant file content and structure.
- **Seamless Integration**: Works within the IntelliJ IDEA environment, providing a smooth workflow for developers.
- **Customizable Directives**: Users can specify their directives for file generation, allowing for a wide range of applications.


### Getting Started

To use the Analogue File Action plugin, follow these steps:

1. **Installation**: Ensure the plugin is installed in your IntelliJ IDEA environment. This can typically be done through the IntelliJ IDEA marketplace.

2. **Select a File**: In your project, select the file you wish to base your new file on. This file should not be a directory.

3. **Invoke the Plugin**: With the file selected, invoke the Analogue File Action plugin. This can usually be done from the context menu or a dedicated menu for plugins.

4. **Specify Directive**: A dialog will appear, prompting you to enter a directive. This directive should be a natural language instruction describing what you want the new file to contain or how it should be structured.

5. **Generation**: After entering your directive, the plugin will process your request and generate a new file based on the provided context and instructions.


### Example Use Case

Suppose you have a Java class file, and you need to create a test class for it with specific test cases. You can select the original class file, invoke the Analogue File Action plugin, and enter a directive like "Create a test class with test cases for methods X, Y, and Z."


### Advanced Usage

- **Custom Directives**: Experiment with different directives to see how the plugin interprets your instructions. The AI is capable of understanding a wide range of requests.
- **Multiple Files**: You can generate analogues for multiple files by selecting them and applying the same directive, provided the plugin supports batch processing.


### Troubleshooting

- **Plugin Not Responding**: Ensure your IntelliJ IDEA environment is up to date and that there are no conflicts with other plugins.
- **Unexpected File Output**: Refine your directives for clarity and specificity. The AI's interpretation may vary based on the input.


### Conclusion

The Analogue File Action plugin is a versatile tool that can significantly enhance your development workflow in IntelliJ IDEA. By understanding and leveraging its capabilities, you can automate the creation of contextually similar files, saving time and ensuring consistency across your project.

# FileContextAction.kt


## FileContextAction Plugin Documentation

The `FileContextAction` plugin is designed to extend the functionality of the IntelliJ IDE, providing developers with a powerful tool to automate actions based on the context of selected files or folders within their project. This document outlines how to use the plugin, its features, and how to extend it for custom actions.


### Features

- **Contextual Actions**: Perform actions based on the selected file or folder within the IDE.
- **Support for Files and Folders**: Can be configured to support actions on both files and folders.
- **Customizable Actions**: Abstract class allows for the creation of custom actions by extending it.
- **Project and File Selection State**: Passes the current project and selected file or folder information to the custom action.
- **Asynchronous Execution**: Actions are performed in a separate thread, ensuring the IDE remains responsive.
- **Error Handling**: Includes error handling and logging capabilities.


### How to Use


#### Prerequisites

- IntelliJ IDE
- Basic understanding of Kotlin programming language
- Familiarity with IntelliJ plugin development


#### Steps to Extend `FileContextAction`

1. **Create a New Action Class**: Extend the `FileContextAction` abstract class. Implement the abstract method `processSelection` to define the action's behavior.

    ```kotlin
    class MyCustomAction : FileContextAction<MyConfigType>(supportsFiles = true, supportsFolders = false) {
        override fun processSelection(state: SelectionState, config: MyConfigType?): Array<File> {
            // Implement your custom action logic here
        }
    }
    ```

2. **Implement `processSelection` Method**: This method is where you define what happens when your action is triggered. Use the `SelectionState` parameter to access the selected file and the project root.

3. **Configure Action in `plugin.xml`**: Define your action in the `plugin.xml` file to make it available in the IDE.

    ```xml
    <actions>
        <action id="MyCustomAction" class="com.github.myplugin.MyCustomAction" text="My Custom Action" description="Description of my custom action">
            <!-- Additional configuration here -->
        </action>
    </actions>
    ```

4. **Implement Optional Methods**: Override `getConfig` if your action requires configuration specific to the project. Override `isEnabled` to control when your action is available.


#### Running Your Action

Once implemented and configured, your custom action will be available in the IDE. It can be triggered based on the context (file or folder selection) and the conditions you've defined in your `isEnabled` method.


### Advanced Features

- **Asynchronous Execution**: The `handle` method runs the action in a separate thread. You can adjust this behavior as needed.
- **Error Handling**: Use the `UITools.error` method to log errors that occur during the execution of your action.
- **File Operations**: The `open` method in the companion object demonstrates how to programmatically open files in the editor, which can be adapted for other file operations.


### Conclusion

The `FileContextAction` plugin provides a flexible framework for creating context-sensitive actions within the IntelliJ IDE. By extending this abstract class, developers can automate tasks, enhance productivity, and create a more personalized development environment.

# generic\CodeChatAction.kt


## Code Chat Plugin Documentation

The Code Chat Plugin is an innovative tool designed to enhance your coding experience by integrating a real-time chat feature directly into your development environment. This document provides a comprehensive guide on how to use the Code Chat Plugin, including its features, setup, and operation.


### Features

- **Real-Time Code Collaboration:** Share and discuss code snippets with your team in real-time, directly from your IDE.
- **Language Support:** Automatically detects the programming language of the shared code snippet for syntax highlighting and relevant discussions.
- **Persistent Sessions:** Sessions are maintained, allowing for ongoing discussions and code reviews even after closing the IDE.
- **Easy Access:** Launches a chat session in your default browser with a single click, providing a seamless transition from coding to collaboration.


### Prerequisites

Before you can use the Code Chat Plugin, ensure you have the following:

- A compatible IDE (The plugin is designed for use with IntelliJ-based IDEs).
- Java Development Kit (JDK) installed on your machine.
- The Code Chat Plugin installed in your IDE.


### Getting Started

1. **Installation:**
   - Open your IDE and navigate to the plugin marketplace.
   - Search for "Code Chat Plugin" and install it.
   - Restart your IDE to activate the plugin.

2. **Launching a Chat Session:**
   - Open a project and navigate to the code file you wish to discuss.
   - Highlight the specific code snippet or simply place your cursor within the file to share the entire content.
   - Right-click and select the "Start Code Chat" option from the context menu, or use the designated shortcut if available.

3. **Using the Chat Interface:**
   - Upon initiating a chat session, your default web browser will open a new tab directed to the chat interface.
   - The interface will display the shared code snippet, highlighting the syntax based on the detected programming language.
   - You can start typing your messages in the chat box provided. Messages can include further code snippets, questions, or general discussion points.
   - Collaborators can join the session using the session link provided. Each participant will need to have access to the same IDE project and the Code Chat Plugin installed.


### Advanced Features

- **Session Management:** Sessions are identified by a unique session ID. You can manage active sessions from the plugin interface in your IDE, allowing you to switch between discussions or revisit previous ones.
- **Customization:** The plugin settings allow you to customize various aspects, such as notification preferences, default chat model, and more, to tailor the chat experience to your needs.


### Troubleshooting

- **Browser Not Launching:** Ensure your default web browser is set correctly in your system settings. If the issue persists, manually navigate to the provided URL.
- **Session Not Found:** Verify that the session ID is correct and that the server hosting the chat is operational. Restarting the IDE and initiating a new session may resolve the issue.


### Conclusion

The Code Chat Plugin offers a powerful way to enhance collaboration and streamline the code review process directly within your IDE. By following this guide, you should be well-equipped to start using the plugin to its full potential. Happy coding and collaborating!

# generic\CreateFileFromDescriptionAction.kt


## CreateFileFromDescriptionAction Plugin Documentation

The `CreateFileFromDescriptionAction` plugin is a powerful tool designed to automate the process of file creation within a project, leveraging natural language directives to generate the necessary code. This document provides a comprehensive guide on how to use the `CreateFileFromDescriptionAction` plugin effectively.


### Overview

The `CreateFileFromDescriptionAction` plugin integrates with the project environment to interpret natural language instructions and generate files accordingly. It utilizes the OpenAI API to understand the directives and create files with the specified content and at the desired location within the project structure.


### Key Components

- **ProjectFile**: Represents the file to be created, containing the path and the code as strings.
- **SettingsUI**: Provides a user interface component to input the directive for file creation.
- **Settings**: Holds the directive as a string that guides the file creation process.


### How It Works

1. **Directive Input**: The user inputs a directive through the `SettingsUI` component. This directive should describe the file to be created, including its purpose and content.
2. **File Generation**: Based on the provided directive, the plugin communicates with the OpenAI API to generate the appropriate file content and determine the file's path relative to the project root.
3. **File Creation**: The plugin then creates the file at the specified path within the project structure, ensuring that the file does not overwrite any existing files.


### Usage

1. **Configure the Directive**: Access the `SettingsUI` component and input your directive in the provided text area. For example, "Create a default log4j configuration file".
2. **Select the Target Location**: In your project, select the location where you want the new file to be created. This selection will determine the base path for the new file.
3. **Execute the Action**: Run the `CreateFileFromDescriptionAction` plugin. It will process your directive and the selected location to generate and create the new file accordingly.


### Example

Suppose you want to create a new log4j configuration file in your Java project. You would:

1. Open the `SettingsUI` and input the directive: "Create a default log4j configuration file".
2. Select the `src/main/resources` directory in your project as the target location.
3. Execute the `CreateFileFromDescriptionAction`. The plugin will then generate a log4j configuration file based on your directive and place it in the specified directory.


### Handling Existing Files

If the plugin detects that the generated file's intended path already exists, it will automatically adjust the filename to prevent overwriting. This is done by appending a numeric suffix to the original filename until an available name is found.


### Conclusion

The `CreateFileFromDescriptionAction` plugin offers a convenient and efficient way to automate file creation within your projects, leveraging natural language processing to interpret directives and generate the necessary files. By following the steps outlined in this document, you can harness the power of this plugin to streamline your development workflow.

# generic\AppendTextWithChatAction.kt


## AppendTextWithChatAction Plugin Documentation


### Overview

The `AppendTextWithChatAction` plugin is designed to enhance your coding experience by allowing you to automatically append generated text to a selected piece of text within your code editor. This functionality is particularly useful for developers looking for quick ways to extend their code snippets based on AI-generated suggestions.


### Features

- **AI-Powered Text Generation**: Utilizes an AI model to generate text that logically follows the selected text in your code editor.
- **Customizable Settings**: Leverages the `AppSettingsState` to customize the behavior of the AI, including the model used and the temperature setting for text generation.
- **Seamless Integration**: Designed to integrate smoothly with your coding environment, providing an intuitive and hassle-free user experience.


### How It Works

1. **Selection**: The user selects a piece of text in their code editor that they wish to extend.
2. **AI Generation**: Upon activation, the plugin sends the selected text to an AI model specified in the application settings. The AI model then generates a continuation of the text based on its training.
3. **Appending**: The generated text is appended to the original selection, taking care to avoid duplication if the generated text starts with the selected text.


### Usage

To use the `AppendTextWithChatAction` plugin, follow these steps:

1. **Select Text**: Highlight the text in your code editor that you want to append generated text to.
2. **Activate AppendTextWithChatAction**: Use the designated shortcut or menu option to activate the `AppendTextWithChatAction` plugin.
3. **Review and Accept**: The plugin will automatically append the AI-generated text to your selection. Review the appended text and make any necessary adjustments.


### Configuration

The behavior of the `AppendTextWithChatAction` plugin can be customized through the `AppSettingsState`. Key settings include:

- **Default Chat Model**: Specifies the AI model used for generating text. This can be adjusted to use different models based on your preference or the specific needs of your project.
- **Temperature**: Controls the creativity of the generated text. A higher temperature results in more creative (but potentially less predictable) outputs, while a lower temperature produces more conservative and predictable text.


### Requirements

- **IntelliJ Platform**: The plugin is designed for use within the IntelliJ platform.
- **API Access**: Requires access to the AI model's API for generating text. Ensure that your `AppSettingsState` is configured with the necessary API keys and settings.


### Conclusion

The `AppendTextWithChatAction` plugin offers a powerful way to extend your code with AI-generated text, streamlining your development process and sparking creativity. By customizing the plugin settings, you can tailor the text generation to meet your specific needs and preferences.

# generic\VoiceToTextAction.kt


## VoiceToTextAction Plugin Documentation

The `VoiceToTextAction` plugin is designed to enhance your coding experience by allowing you to dictate code and comments directly into your IDE. This document provides an overview of the plugin's functionality, setup instructions, and usage guidelines.


### Overview

The `VoiceToTextAction` plugin captures audio through your microphone, processes the audio to convert speech to text, and then inserts the transcribed text into your code editor at the current cursor position or replaces the selected text. It operates in the background, starting with a simple action trigger and stopping when you close a specific dialog window.


### Features

- **Continuous Dictation**: Dictate code and comments without taking your hands off the keyboard.
- **Background Processing**: Audio recording, processing, and transcription run on separate threads, ensuring smooth IDE performance.
- **Adaptive Text Insertion**: The plugin intelligently inserts text at the cursor's position or replaces the selected text.
- **Customizable Prompt Support**: Supports using the last dictated text as a prompt for improving transcription context.


### Prerequisites

Before using the `VoiceToTextAction` plugin, ensure you have:

- A compatible IDE (IntelliJ-based IDEs).
- A working microphone set up and configured on your system.
- Java Development Kit (JDK) installed.


### Installation

1. Download the `VoiceToTextAction` plugin from the plugin marketplace or the provided source.
2. In your IDE, navigate to `Settings` > `Plugins`.
3. Click on `Install Plugin from Disk...` and select the downloaded plugin file.
4. Restart your IDE to activate the plugin.


### Usage

To start dictating:

1. Ensure your microphone is on and properly configured.
2. Trigger the `VoiceToTextAction` from the IDE's action menu or use the assigned shortcut.
3. A dialog titled "Dictation" will appear, indicating that dictation is active. Keep this dialog open while dictating.
4. Speak clearly into your microphone. Your spoken words will be transcribed and inserted into your editor in real-time.
5. To stop dictation, simply close the "Dictation" dialog window.


### Troubleshooting

- **Microphone Not Detected**: Ensure your microphone is properly connected and configured in your system settings.
- **Poor Transcription Quality**: Speak clearly and directly into the microphone. Avoid background noise.
- **Plugin Not Responding**: Check if your IDE is up to date and restart the IDE. If the issue persists, reinstall the plugin.


### Support

For support, feature requests, or to report bugs, please visit the plugin's GitHub repository or contact the development team through the plugin marketplace.


### Conclusion

The `VoiceToTextAction` plugin offers a convenient way to enhance your coding efficiency through voice commands. By following the setup instructions and usage guidelines provided in this document, you can seamlessly integrate dictation into your development workflow.

# generic\GenerateDocumentationAction.kt


#### Documentation Compiler Action Plugin

The Documentation Compiler Action plugin is a powerful tool designed for IntelliJ-based IDEs to assist developers in automatically generating documentation for their projects. This plugin leverages natural language processing to transform code comments and structure into comprehensive markdown documentation, making it easier to maintain and share knowledge about the codebase.


##### Features

- **Automatic Documentation Generation**: Converts code and comments into a structured markdown file.
- **Customizable Transformation Message**: Allows users to specify a message that guides the documentation transformation process.
- **Support for Multiple Files**: Processes all eligible files within a selected directory.
- **Concurrency Support**: Utilizes a fixed thread pool for efficient processing of multiple files.
- **Dynamic Output File Naming**: Automatically resolves naming conflicts by appending a numeric suffix to the output filename if a file with the same name already exists.


##### How to Use

1. **Select a Directory**: Right-click on a directory in your project that you wish to document.
2. **Invoke the Plugin**: Find and click on the "Compile Documentation" action from the context menu.
3. **Configure Settings (Optional)**: In the dialog that appears, you can customize the transformation message. This message can guide the documentation generation process. By default, it is set to "Create user documentation".
4. **Generate Documentation**: Click "OK" to start the documentation compilation process. The plugin will process all eligible files within the selected directory and generate a markdown file named `compiled_documentation.md` in the same directory. If a file with the same name already exists, it will append a numeric suffix to the new file's name to avoid overwriting.


##### Requirements

- IntelliJ IDEA or any IntelliJ-based IDE.
- Java Development Kit (JDK) version 8 or above.


##### Installation

To install the Documentation Compiler Action plugin, follow these steps:

1. Open your IntelliJ-based IDE.
2. Navigate to `Settings` > `Plugins`.
3. Click on the `Marketplace` tab and search for "Documentation Compiler Action".
4. Click on the "Install" button and restart the IDE if prompted.


##### Configuration

The plugin utilizes settings from `AppSettingsState` for configuring the API model and temperature for content transformation. These settings can be adjusted in the plugin's configuration file or through the IDE's plugin settings interface, depending on how the plugin is designed.


##### Opening the Generated Documentation

Once the documentation is compiled, the plugin automatically attempts to open the generated markdown file in the IDE. If the file does not open immediately, it may be due to the IDE not being ready for file operations. The plugin will retry opening the file at intervals until successful.


##### Troubleshooting

- **Plugin Not Appearing in Context Menu**: Ensure that you have right-clicked on a directory and not an individual file. The action is only enabled for directories.
- **Documentation Not Generating**: Check if the selected directory contains eligible files for documentation. The plugin processes regular files and ignores directories.
- **IDE Not Opening Generated File**: Wait a few moments and try manually navigating to and opening the `compiled_documentation.md` file in your project directory.

For further assistance, please refer to the plugin's support forum or issue tracker on its GitHub repository.

# generic\DiffChatAction.kt


## DiffChatAction Plugin Documentation


### Overview

The `DiffChatAction` plugin is designed to enhance the coding experience by integrating a chat-based interface that allows developers to discuss code changes in real-time. It leverages the concept of diffs (differences) to suggest and apply code modifications directly within the IDE. This document provides a comprehensive guide on how to use the plugin, its features, and how it can streamline the code review and collaboration process.


### Features

- **Real-Time Chat Interface**: Engage in discussions about code changes with team members or AI assistants directly within your IDE.
- **Diff Suggestions**: Receive code change suggestions in diff format, which clearly shows additions and deletions.
- **One-Click Patch Application**: Apply suggested diffs to your code with a single click, streamlining the code modification process.
- **Contextual Code Selection**: The plugin intelligently selects the relevant portion of code for discussion, whether it's a specific selection or the entire document.


### Getting Started


#### Prerequisites

- Ensure you have the compatible IDE installed.
- The plugin requires access to a server component (`AppServer`) for managing sessions and chat interactions.


#### Installation

1. Download the `DiffChatAction` plugin from the designated plugin repository.
2. Install the plugin through your IDE's plugin management interface.
3. Restart your IDE to activate the plugin.


#### Usage

1. **Initiate a Chat Session**: With a code file open, trigger the `DiffChatAction` from the action menu or use the assigned shortcut. This action will start a new chat session related to the code in focus.

2. **Discuss Code Changes**: Engage in the chat interface that appears, discussing potential improvements or changes. The discussion can involve human collaborators or AI, depending on your setup.

3. **Apply Diffs**: When a code change suggestion is made in the diff format, a link labeled "Apply Diff" will appear next to it. Clicking this link will apply the change to your code.

4. **Review and Continue**: After applying changes, you can continue the discussion, request further modifications, or conclude the session.


### Best Practices

- **Clear Context**: When initiating a chat session, ensure the selected code or document provides enough context for meaningful discussion.
- **Review Diffs Carefully**: Before applying diffs, review them to ensure they align with your project's goals and coding standards.
- **Test Changes**: After applying diffs, test your code thoroughly to catch any issues introduced by the changes.


### Troubleshooting

- **Session Not Starting**: Ensure your IDE has access to the `AppServer` and that it's running correctly. Check network configurations if necessary.
- **Diffs Not Applying**: Verify that the selected text or document is not locked or read-only. Ensure there are no syntax errors that might prevent the application of diffs.


### Conclusion

The `DiffChatAction` plugin offers a powerful way to streamline code reviews and collaboration within the IDE. By integrating real-time chat and diff application capabilities, it facilitates a more interactive and efficient coding process. Follow the guidelines provided in this document to make the most out of your plugin experience.

# generic\RedoLast.kt


## RedoLast Action Documentation


### Overview

The `RedoLast` action is a feature provided by the AI Coder IntelliJ plugin, designed to enhance your coding experience by allowing you to quickly redo the last action you performed in the editor. This functionality is particularly useful for developers who frequently experiment with code changes and need an efficient way to revert to their previous state.


### Getting Started

To utilize the `RedoLast` action, ensure you have the AI Coder plugin installed in your IntelliJ IDE. Once installed, the action is readily available in the editor context menu, making it easily accessible during your coding sessions.


### How to Use RedoLast

1. **Perform an Action:** Begin by performing any action in the editor that you might want to redo later. This could be typing code, formatting, or any other change.

2. **Open Context Menu:** Right-click within the editor to open the context menu. This menu provides various options and actions related to the editor and your code.

3. **Select RedoLast:** Look for the `RedoLast` action in the context menu and select it. This action will only be enabled if there is a last action recorded that can be redone.

4. **Action Redone:** Upon selection, the `RedoLast` action will redo the last action you performed in the editor. If the action cannot be redone, the `RedoLast` option will be disabled.


### Availability

The `RedoLast` action is available whenever there is a last action recorded by the AI Coder plugin that can be redone. If no such action exists, or if the action cannot be redone for any reason, the `RedoLast` option will be disabled in the context menu.


### Technical Details

- **Class Name:** `RedoLast`
- **Package:** `com.github.simiacryptus.aicoder.actions.generic`
- **Superclass:** `BaseAction`
- **Method:** `handle(e: AnActionEvent)`: This method is invoked when the `RedoLast` action is selected. It retrieves the current document from the event context and executes the redo operation if available.
- **Method:** `isEnabled(event: AnActionEvent)`: This method checks if the `RedoLast` action should be enabled or disabled based on the availability of a last action to redo.


### Conclusion

The `RedoLast` action is a convenient tool for developers looking to streamline their coding workflow within the IntelliJ IDE. By allowing users to quickly redo their last performed action, it enhances productivity and facilitates a smoother coding experience.

# markdown\MarkdownImplementActionGroup.kt


## Markdown Implement Action Group Plugin Documentation

The Markdown Implement Action Group plugin is designed to enhance your experience with markdown files in IntelliJ-based IDEs. This plugin provides a set of actions that allow you to automatically implement code snippets in various programming languages directly into your markdown files. Below is a comprehensive guide to help you understand and utilize this plugin effectively.


### Features

- **Language Support**: The plugin supports a wide range of programming languages including SQL, Java, C, C++, Python, Ruby, and many more. This allows you to work with the most common programming languages seamlessly.
- **Automatic Implementation**: With just a few clicks, you can insert code snippets into your markdown files, making your documentation richer and more informative.
- **Easy to Use**: The plugin integrates directly into the IntelliJ action system, making it accessible through the usual action menus and shortcuts.


### How to Use


#### Prerequisites

Ensure you have an IntelliJ-based IDE installed and that your project contains markdown (.md) files.


#### Enabling the Plugin

1. Open your IntelliJ IDE and navigate to the plugin settings.
2. Search for the "Markdown Implement Action Group" plugin and install it.
3. Restart your IDE to activate the plugin.


#### Using the Plugin

1. **Select Text**: Highlight the text in your markdown file where you want to implement a code snippet.
2. **Open Action Menu**: Right-click to open the context menu and navigate to the "Markdown Implement" action group, or use the corresponding shortcut to access the action group directly.
3. **Choose Language**: Select the programming language for your code snippet from the list of supported languages.
4. **Automatic Implementation**: The plugin will automatically generate and insert the code snippet into your markdown file, wrapped in the appropriate code block syntax for the selected language.


### Supported Languages

The plugin supports a variety of programming languages, including but not limited to:

- SQL
- Java
- C/C++
- Python
- Ruby
- JavaScript
- PHP
- Swift
- Rust

For a complete list of supported languages, refer to the `markdownLanguages` list within the plugin's source code.


### Customization and Settings

You can customize the behavior of the plugin through the IntelliJ settings menu, under the "AppSettingsState" configuration. Here, you can adjust settings such as the default chat model and temperature for the code generation proxy.


### Troubleshooting

If you encounter any issues while using the plugin, ensure that:

- Your IntelliJ IDE is up to date.
- The plugin is correctly installed and enabled.
- You have an active internet connection if the plugin requires online services for code generation.

For further assistance, consult the plugin's documentation or reach out to the support community.


### Conclusion

The Markdown Implement Action Group plugin is a powerful tool for developers and writers working with markdown files in IntelliJ-based IDEs. By automating the insertion of code snippets, it enhances the documentation process and improves productivity. Explore its features and make the most out of your markdown documentation projects.

# markdown\MarkdownListAction.kt


## Markdown List Action Plugin Documentation

The Markdown List Action plugin is designed to enhance your productivity when working with Markdown files in IntelliJ-based IDEs. This plugin provides a convenient way to automatically generate and insert new list items into your existing Markdown lists, leveraging AI technology to predict and create relevant list items based on the context of your current list.


### Features

- **Automatic List Item Generation**: Generate new list items based on the existing ones in your Markdown list.
- **Context-Aware Suggestions**: Utilizes AI to provide suggestions that are relevant to the context of your current list.
- **Customizable Bullet Types**: Supports different bullet types for list items, including `- [ ]` (task list), `-` (dash), and `*` (asterisk).
- **Easy Integration**: Seamlessly integrates with IntelliJ-based IDEs, providing a smooth user experience.


### How to Use

1. **Installation**: First, ensure that the plugin is installed in your IntelliJ-based IDE. You can find it in the plugin marketplace under the name "Markdown List Action".

2. **Open a Markdown File**: Navigate to a Markdown file where you want to add new list items.

3. **Select a List**: Highlight the list (or a part of it) where you want the new items to be inserted. The plugin works with lists marked by `- [ ]`, `-`, and `*`.

4. **Activate the Plugin**: With the list selected, activate the plugin by either using a predefined shortcut or by finding the "Markdown List Action" in the context menu or action menu.

5. **Wait for Generation**: The plugin will then use AI to generate new list items based on the context of the selected list. The number of items generated is twice the number of selected items.

6. **Review and Edit**: The new list items will be inserted at the end of the selected list. You can review and edit these items as needed.


### Requirements

- IntelliJ-based IDE (e.g., IntelliJ IDEA, PyCharm, WebStorm, etc.)
- An active internet connection for AI-based features


### Customization and Settings

You can customize the behavior of the Markdown List Action plugin through the AppSettingsState configuration. This includes setting the default chat model for the AI and the number of retries for deserialization.


### Troubleshooting

- **Plugin Not Working**: Ensure that your IDE is supported and that you have an active internet connection. Also, check if the plugin is enabled in your IDE's plugin settings.
- **Incorrect Suggestions**: The AI generates suggestions based on the context of the selected list. For more accurate suggestions, select list items that clearly convey the context.


### Support

For support, feature requests, or to report bugs, please visit the plugin's GitHub repository or contact the plugin developers through the support channels provided in the plugin marketplace.

---

This documentation aims to provide you with all the necessary information to get started with the Markdown List Action plugin. Enhance your Markdown editing experience by leveraging the power of AI to generate list items effortlessly.

# generic\ReplaceWithSuggestionsAction.kt


## ReplaceWithSuggestionsAction Plugin Documentation


### Overview

The `ReplaceWithSuggestionsAction` plugin is designed to enhance your coding experience within the IntelliJ IDEA environment by providing smart text replacement suggestions. Leveraging advanced AI models, this plugin analyzes the context of your selected text and offers a list of suggestions to replace it, aiming to improve code quality, readability, or simply to offer creative alternatives.


### Features

- **Context-Aware Suggestions:** Utilizes the surrounding text to provide relevant replacement options for the selected text.
- **AI-Powered:** Integrates with AI models to generate suggestions, ensuring high-quality and contextually appropriate options.
- **Customizable:** Leverages settings from `AppSettingsState` to allow customization of the AI model and response generation behavior (e.g., temperature setting for creativity control).
- **Interactive Selection:** Offers a user-friendly dialog with radio buttons to choose the preferred replacement option from the AI-generated suggestions.


### How to Use

1. **Select Text:** Begin by selecting the text in your code that you wish to replace.
2. **Activate Action:** Trigger the `ReplaceWithSuggestionsAction` by using its assigned shortcut or by finding it in the context menu.
3. **Choose Replacement:** A dialog will appear with a list of suggestions. Select the one that best fits your needs and confirm your choice.
4. **Review Changes:** The selected text in your code will be replaced with your chosen suggestion. Review the change to ensure it meets your expectations.


### Requirements

- **IntelliJ IDEA:** This plugin is designed to work within the IntelliJ IDEA environment.
- **Project Configuration:** Ensure your project is properly set up and recognized by IntelliJ IDEA to enable plugin functionality.
- **Internet Connection:** Since suggestions are generated using an AI model, an active internet connection is required to fetch the suggestions.


### Customization

To tailor the behavior of the `ReplaceWithSuggestionsAction` plugin to your preferences, you can adjust the settings in `AppSettingsState`. This includes:

- **Default Chat Model:** Choose the AI model that best suits your coding style or the specific language/framework you are working with.
- **Temperature:** Adjust the creativity level of the suggestions. A higher temperature results in more creative (but potentially less accurate) suggestions, while a lower temperature favors more conservative and contextually accurate options.


### Troubleshooting

- **No Suggestions Generated:** Ensure your internet connection is stable and that the AI model and temperature settings are correctly configured in `AppSettingsState`.
- **Plugin Not Responding:** Verify that your IntelliJ IDEA environment is up to date and that there are no conflicts with other installed plugins.


### Conclusion

The `ReplaceWithSuggestionsAction` plugin offers a powerful way to enhance your coding experience by integrating AI-powered text replacement suggestions directly into your workflow. By understanding and utilizing its features, you can significantly improve the efficiency and quality of your code writing process.

# SelectionAction.kt


## SelectionAction Plugin Documentation

The `SelectionAction` plugin is designed to enhance the development experience within the IntelliJ IDEA by providing a framework for actions that operate on selected text or code segments. This document serves as a guide for users to understand and utilize the functionalities provided by the plugin.


### Overview

The `SelectionAction` plugin offers a base class for creating actions that can be performed on a user's selection within the editor. These actions can range from simple text manipulations to more complex code transformations, depending on the specific implementation. The plugin is designed to be flexible, allowing for customization and extension to fit various programming languages and use cases.


### Key Features

- **Selection-Based Actions**: Perform operations based on the currently selected text or code block.
- **Language Support**: Extendable to support various programming languages through the `ComputerLanguage` utility.
- **Context Awareness**: Utilizes the PSI (Program Structure Interface) to understand the context of the selection within the codebase.
- **Custom Configuration**: Allows for action-specific configurations to be defined and utilized during the action's execution.


### How to Use

To use the `SelectionAction` plugin, follow these steps:

1. **Extend the SelectionAction Class**: Create a new class that extends the `SelectionAction` abstract class. Implement the abstract methods to define the behavior of your custom action.

2. **Implement Action Logic**:
    - **processSelection**: This is where the main logic of your action should be implemented. It receives the current selection state and any custom configuration as parameters.
    - **getConfig**: Optionally override this method to provide custom configuration for your action.

3. **Register the Action**: Define your action in the plugin's `plugin.xml` file to make it available within the IntelliJ IDEA.

4. **Execute the Action**: Once registered, your action will be available for execution. Depending on how you've configured it, it can be triggered from a menu, a shortcut, or any other action point defined in `plugin.xml`.


### Example

Here's a simple example of an action that converts the selected text to uppercase:

```kotlin
class ToUpperCaseAction : SelectionAction<String>() {
    override fun processSelection(state: SelectionState, config: String?): String {
        return state.selectedText?.toUpperCase() ?: ""
    }
}
```

In this example, the `processSelection` method simply converts the selected text to uppercase. There's no custom configuration needed, so the `getConfig` method is not overridden.


### Conclusion

The `SelectionAction` plugin provides a powerful framework for extending the IntelliJ IDEA with custom actions that operate on selected text or code segments. By following the steps outlined in this document, users can create and utilize custom actions to enhance their development workflow.

