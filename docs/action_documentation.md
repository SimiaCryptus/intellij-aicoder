# code\DocAction.kt

Sure, here's the documentation for the `DocAction` class:

```kotlin
/**
 * The `DocAction` class is an implementation of the `SelectionAction` interface that generates
 * documentation for a selected code block using OpenAI's language model.
 *
 * The class defines a virtual API interface `DocAction_VirtualAPI` that provides a single method
 * `processCode` to generate the documentation text. The `ChatProxy` class is used to create an
 * instance of this virtual API, which is initialized with an example input-output pair.
 *
 * The `processSelection` method is the main entry point for the action. It takes the selected text
 * from the editor, formats it as an `IndentedText` object, and passes it to the `processCode` method
 * of the virtual API along with the language and documentation style. The generated documentation
 * text is then prepended to the original code and returned.
 *
 * The `isLanguageSupported` method checks if the given `ComputerLanguage` is supported for
 * documentation generation by verifying that it is not plain text and has a non-empty documentation
 * style defined.
 *
 * The `editSelection` method is overridden to handle cases where the selected text is not the entire
 * code block. It uses the `PsiUtil` class to find the enclosing code element and adjusts the selection
 * range accordingly.
 *
 * @param project The current project instance (not used in this implementation).
 * @param state The `SelectionState` object containing the selected text and language information.
 * @param config An optional configuration string (not used in this implementation).
 * @return The generated documentation text prepended to the original code.
 */
class DocAction : SelectionAction<String>() { ... }
```

This documentation explains the purpose of the `DocAction` class, its main methods and their responsibilities, and the parameters and return values of the `processSelection` method. It also mentions the use of the `ChatProxy` class and the `PsiUtil` utility class.

# code\DescribeAction.kt

Sure, here's the documentation for the `DescribeAction` class:

```kotlin
/**
 * The DescribeAction class is an implementation of the SelectionAction interface.
 * It is responsible for generating a description of the selected code using OpenAI's language model.
 *
 * The class defines a virtual API interface called DescribeAction_VirtualAPI, which contains a single method:
 * - describeCode(code: String, computerLanguage: String, humanLanguage: String): DescribeAction_ConvertedText
 *   This method takes the selected code, the programming language of the code, and the human language for the description.
 *   It returns an instance of DescribeAction_ConvertedText, which contains the generated description and its language.
 *
 * The DescribeAction class uses the ChatProxy class to create an instance of the DescribeAction_VirtualAPI interface.
 * The ChatProxy is configured with the appropriate API key, temperature, and language model settings from the AppSettingsState.
 *
 * The processSelection method is the main entry point for the action. It takes a SelectionState object, which contains
 * the selected text, the programming language, and other context information. It then calls the describeCode method
 * of the DescribeAction_VirtualAPI instance to generate the description.
 *
 * The generated description is then wrapped to a maximum line length of 120 characters using the StringUtil.lineWrapping
 * method. Based on the number of lines in the wrapped description, the method determines whether to use a line comment
 * or a block comment style for the programming language.
 *
 * Finally, the method returns a string that combines the comment style with the original selected text, indented
 * appropriately.
 */
```

This class is part of the `com.github.simiacryptus.aicoder.actions.code` package and is used in the context of an IntelliJ IDEA plugin for generating code descriptions using OpenAI's language model. It leverages the `SelectionAction` interface and other utility classes from the project to interact with the user's code selection and generate a descriptive comment.

# code\CommentsAction.kt

Sure, here's the documentation for the `CommentsAction` class:

```kotlin
/**
 * The CommentsAction class is an implementation of the SelectionAction interface.
 * It is responsible for adding comments to the selected code in the IDE.
 *
 * The action uses the OpenAI API to generate comments for the selected code.
 * It sends the selected code, the desired operation (adding comments), the computer language,
 * and the human language to the OpenAI API, which generates the commented code.
 *
 * The action supports all computer languages except for plain text.
 */
class CommentsAction : SelectionAction<String>() {

    /**
     * Returns an empty string as the configuration for this action.
     *
     * @param project The current project.
     * @return An empty string.
     */
    override fun getConfig(project: Project?): String {
        return ""
    }

    /**
     * Checks if the given computer language is supported by this action.
     *
     * @param computerLanguage The computer language to check.
     * @return True if the language is not null and not plain text, false otherwise.
     */
    override fun isLanguageSupported(computerLanguage: ComputerLanguage?): Boolean {
        return computerLanguage != null && computerLanguage != ComputerLanguage.Text
    }

    /**
     * Processes the selected text by adding comments to each line of code.
     *
     * @param state The selection state containing the selected text and language.
     * @param config The configuration for this action (not used).
     * @return The commented code.
     */
    override fun processSelection(state: SelectionState, config: String?): String {
        return ChatProxy(
            clazz = CommentsAction_VirtualAPI::class.java,
            api = api,
            temperature = AppSettingsState.instance.temperature,
            model = AppSettingsState.instance.defaultChatModel(),
            deserializerRetries = 5
        ).create().editCode(
            state.selectedText ?: "",
            "Add comments to each line explaining the code",
            state.language.toString(),
            AppSettingsState.instance.humanLanguage
        ).code ?: ""
    }

    /**
     * The CommentsAction_VirtualAPI interface defines the contract for the OpenAI API call.
     */
    interface CommentsAction_VirtualAPI {
        /**
         * Edits the given code by performing the specified operations.
         *
         * @param code The code to edit.
         * @param operations The operations to perform on the code.
         * @param computerLanguage The computer language of the code.
         * @param humanLanguage The human language for the comments.
         * @return The edited code with comments.
         */
        fun editCode(
            code: String,
            operations: String,
            computerLanguage: String,
            humanLanguage: String
        ): CommentsAction_ConvertedText

        /**
         * The CommentsAction_ConvertedText class represents the result of the editCode operation.
         */
        class CommentsAction_ConvertedText {
            var code: String? = null
            var language: String? = null
        }
    }
}
```

This documentation explains the purpose of the `CommentsAction` class, the responsibilities of each method, and the interfaces it uses. It also provides information about the parameters and return values of each method.

# code\CustomEditAction.kt

Sure, here's the documentation for the `CustomEditAction` class:

```kotlin
/**
 * An action that allows the user to edit code using natural language instructions.
 * The action prompts the user for an instruction, and then uses the OpenAI API to generate
 * the edited code based on the user's selection and instruction.
 */
open class CustomEditAction : SelectionAction<String>() {

    /**
     * A virtual API interface that defines the `editCode` function.
     * This function takes the code, operation, computer language, and human language as input,
     * and returns an `EditedText` object containing the edited code and language.
     */
    interface VirtualAPI {
        fun editCode(
            code: String,
            operation: String,
            computerLanguage: String,
            humanLanguage: String
        ): EditedText

        data class EditedText(
            var code: String? = null,
            var language: String? = null
        )
    }

    /**
     * Returns a proxy instance of the `VirtualAPI` interface.
     * The proxy is configured with the OpenAI API, temperature, and model settings from the app settings.
     * It also includes an example of how to use the `editCode` function.
     */
    val proxy: VirtualAPI
        get() {
            // Implementation details omitted for brevity
        }

    /**
     * Prompts the user for an instruction using an input dialog.
     * @param project The current project.
     * @return The user's instruction, or an empty string if the user cancels the dialog.
     */
    override fun getConfig(project: Project?): String {
        // Implementation details omitted for brevity
    }

    /**
     * Processes the user's selection and instruction to generate the edited code.
     * @param state The selection state containing the selected text and language.
     * @param instruction The user's instruction for editing the code.
     * @return The edited code, or the original selected text if no instruction is provided.
     */
    override fun processSelection(state: SelectionState, instruction: String?): String {
        // Implementation details omitted for brevity
    }
}
```

This class extends the `SelectionAction` class and overrides the `getConfig` and `processSelection` methods. The `getConfig` method prompts the user for an instruction using an input dialog, while the `processSelection` method uses the OpenAI API to generate the edited code based on the user's selection and instruction.

The `VirtualAPI` interface defines the `editCode` function, which takes the code, operation, computer language, and human language as input, and returns an `EditedText` object containing the edited code and language. The `proxy` property returns an instance of this interface, configured with the OpenAI API settings and an example of how to use the `editCode` function.

Overall, this class provides a convenient way for users to edit code using natural language instructions, leveraging the power of the OpenAI API.

# code\ImplementStubAction.kt

Sure, here's the documentation for the `ImplementStubAction` class:

```kotlin
/**
 * An action that implements a code stub or declaration based on the selected code and context.
 *
 * This action is designed to work with code editors and uses the OpenAI API to generate the implementation
 * for a given code stub or declaration. It supports various programming languages, excluding plain text.
 *
 * The action works by extracting the selected code and the smallest intersecting method or function from
 * the context. It then sends this information, along with the programming language and the desired output
 * language, to the OpenAI API using the `VirtualAPI` interface.
 *
 * The `VirtualAPI` interface defines a single method `editCode` that takes the code, operation, computer
 * language, and human language as input, and returns a `ConvertedText` object containing the generated
 * code and the language it's written in.
 *
 * The `ImplementStubAction` class extends `SelectionAction<String>` and overrides the following methods:
 *
 * - `isLanguageSupported`: Checks if the given computer language is supported (excludes plain text).
 * - `defaultSelection`: Determines the default selection range based on the editor state and offset.
 * - `getConfig`: Returns an empty string as no additional configuration is required.
 * - `processSelection`: Processes the selected code, extracts the necessary context, and calls the
 *   `editCode` method of the `VirtualAPI` to generate the implementation.
 *
 * The action uses the `ChatProxy` class from the `com.simiacryptus.jopenai.proxy` package to create an
 * instance of the `VirtualAPI` interface. The `ChatProxy` is configured with the OpenAI API key, the
 * desired model, temperature, and other settings from the `AppSettingsState` class.
 *
 * @see SelectionAction
 * @see VirtualAPI
 * @see ChatProxy
 * @see AppSettingsState
 */
class ImplementStubAction : SelectionAction<String>() { ... }
```

This documentation explains the purpose of the `ImplementStubAction` class, its dependencies, and the responsibilities of each overridden method. It also provides information about the `VirtualAPI` interface and how the `ChatProxy` is used to interact with the OpenAI API.

# code\RecentCodeEditsAction.kt

Sure, here's the documentation for the `RecentCodeEditsAction` class:

```kotlin
/**
 * An ActionGroup that displays a list of recent code edits as child actions.
 * The child actions are dynamically generated based on the user's recent command history.
 * When a child action is selected, it applies the corresponding code edit to the current selection.
 */
class RecentCodeEditsAction : ActionGroup() {

    /**
     * Updates the visibility and enabled state of the action based on whether a valid code selection exists.
     */
    override fun update(e: AnActionEvent) {
        e.presentation.isEnabledAndVisible = isEnabled(e)
        super.update(e)
    }

    /**
     * Generates and returns an array of child actions representing recent code edits.
     * Each child action is an instance of CustomEditAction with a specific code edit instruction.
     */
    override fun getChildren(e: AnActionEvent?): Array<AnAction> {
        if (e == null) return emptyArray()
        val children = mutableListOf<AnAction>()
        for ((instruction, _) in AppSettingsState.instance.getRecentCommands("customEdits").mostUsedHistory) {
            val id = children.size + 1
            val text = if (id < 10) "_$id: $instruction" else "$id: $instruction"
            val element = object : CustomEditAction() {
                override fun getConfig(project: Project?): String {
                    return instruction
                }
            }
            element.templatePresentation.text = text
            element.templatePresentation.description = instruction
            element.templatePresentation.icon = null
            children.add(element)
        }
        return children.toTypedArray()
    }

    companion object {
        /**
         * Checks if the action should be enabled based on the current selection and language.
         * The action is enabled if a valid code selection exists (not plain text).
         */
        fun isEnabled(e: AnActionEvent): Boolean {
            if (!UITools.hasSelection(e)) return false
            val computerLanguage = ComputerLanguage.getComputerLanguage(e)
            return computerLanguage != ComputerLanguage.Text
        }
    }
}
```

This class represents an action group that displays a list of recent code edits as child actions. The child actions are dynamically generated based on the user's recent command history stored in the `AppSettingsState`.

The `update` function updates the visibility and enabled state of the action based on whether a valid code selection exists. The `isEnabled` function checks if the current selection is not plain text, ensuring that the action is only enabled for code selections.

The `getChildren` function generates and returns an array of child actions representing recent code edits. Each child action is an instance of `CustomEditAction` with a specific code edit instruction. The text and description of each child action are set based on the instruction and an index number.

When a child action is selected, it applies the corresponding code edit instruction to the current selection by overriding the `getConfig` function of the `CustomEditAction` class.

# code\PasteAction.kt

Sure, here's the documentation for the `PasteAction` class:

```kotlin
/**
 * An action that converts the text from the system clipboard into code in the specified language.
 *
 * This action uses the OpenAI API to convert the clipboard text into code. It supports converting
 * text from any language into code in a supported programming language.
 *
 * The action is enabled only if the system clipboard contains text data.
 *
 * @see SelectionAction
 * @see VirtualAPI
 */
open class PasteAction : SelectionAction<String>(false) {

    /**
     * A virtual API interface used to define the contract for the OpenAI API call.
     */
    interface VirtualAPI {
        /**
         * Converts the given text from one language to another.
         *
         * @param text The text to be converted.
         * @param from_language The source language of the text. Use "autodetect" to automatically detect the language.
         * @param to_language The target language for the converted text.
         * @return The converted text and its language.
         */
        fun convert(text: String, from_language: String, to_language: String): ConvertedText

        /**
         * A data class representing the converted text and its language.
         */
        class ConvertedText {
            var code: String? = null
            var language: String? = null
        }
    }

    /**
     * Returns an empty string as the configuration for this action.
     */
    override fun getConfig(project: Project?): String = ""

    /**
     * Processes the selected text by converting it to code using the OpenAI API.
     *
     * @param state The selection state containing the selected text and language.
     * @param config The configuration for the action (not used in this implementation).
     * @return The converted code.
     */
    override fun processSelection(state: SelectionState, config: String?): String {
        return ChatProxy(
            VirtualAPI::class.java,
            api,
            AppSettingsState.instance.defaultChatModel(),
            AppSettingsState.instance.temperature,
        ).create().convert(
            getClipboard().toString().trim(),
            "autodetect",
            state.language?.name ?: ""
        ).code ?: ""
    }

    /**
     * Checks if the given language is supported by this action.
     *
     * @param computerLanguage The language to check.
     * @return `true` if the language is supported (not plain text), `false` otherwise.
     */
    override fun isLanguageSupported(computerLanguage: ComputerLanguage?): Boolean =
        computerLanguage != null && computerLanguage != ComputerLanguage.Text

    /**
     * Checks if the action should be enabled based on the current event.
     *
     * @param event The action event.
     * @return `true` if the system clipboard contains text data, `false` otherwise.
     */
    override fun isEnabled(event: AnActionEvent): Boolean =
        hasClipboard() && super.isEnabled(event)

    /**
     * Checks if the system clipboard contains text data.
     *
     * @return `true` if the clipboard contains text data, `false` otherwise.
     */
    private fun hasClipboard() = Toolkit.getDefaultToolkit().systemClipboard.getContents(null)?.let { contents ->
        return@let when {
            contents.isDataFlavorSupported(DataFlavor.stringFlavor) -> true
            contents.isDataFlavorSupported(DataFlavor.getTextPlainUnicodeFlavor()) -> true
            else -> false
        }
    } ?: false

    /**
     * Retrieves the text data from the system clipboard.
     *
     * @return The text data from the clipboard, or `null` if no text data is available.
     */
    private fun getClipboard(): Any? = Toolkit.getDefaultToolkit().systemClipboard.getContents(null)?.let { contents ->
        return@let when {
            contents.isDataFlavorSupported(DataFlavor.stringFlavor) -> contents.getTransferData(DataFlavor.stringFlavor)
            contents.isDataFlavorSupported(DataFlavor.getTextPlainUnicodeFlavor()) -> contents.getTransferData(DataFlavor.getTextPlainUnicodeFlavor())
            else -> null
        }
    }
}
```

This class extends the `SelectionAction` class and provides an action that converts the text from the system clipboard into code in the specified language. The conversion is performed using the OpenAI API, and the action supports converting text from any language into code in a supported programming language.

The `VirtualAPI` interface defines the contract for the OpenAI API call, which includes a `convert` function that takes the text, source language, and target language as input and returns the converted text and its language.

The `getConfig` function returns an empty string as the configuration for this action.

The `processSelection` function processes the selected text by converting it to code using the OpenAI API. It creates a `ChatProxy` instance with the `VirtualAPI` interface, the OpenAI API key, the default chat model, and the temperature setting. It then calls the `convert` function of the `VirtualAPI` with the clipboard text, "autodetect" as the source language, and the target language specified in the selection state.

The `isLanguageSupported` function checks if the given language is supported by this action. It returns `true` if the language is not `null` and not plain text (`ComputerLanguage.Text`).

The `isEnabled` function checks if the action should be enabled based on the current event. It returns `true` if the system clipboard contains text data and the action is enabled by the parent class (`super.isEnabled(event)`).

The `hasClipboard` function checks if the system clipboard contains text data by checking if the clipboard contents support the `DataFlavor.stringFlavor` or `DataFlavor.getTextPlainUnicodeFlavor` data flavors.

The `getClipboard` function retrieves the text data from the system clipboard by checking if the clipboard contents support the `DataFlavor.stringFlavor` or `DataFlavor.getTextPlainUnicodeFlavor` data flavors and returning the corresponding data.

# code\InsertImplementationAction.kt

Sure, here's the documentation for the `InsertImplementationAction` class:

```kotlin
/**
 * An action that inserts an implementation based on a natural language specification.
 *
 * This action looks for a comment or selected text that describes the desired implementation.
 * It then uses an AI model to generate code that implements the specified functionality.
 * The generated code is inserted below the comment or selected text.
 *
 * The action supports various programming languages and can use the surrounding code context
 * to generate more relevant implementations.
 */
class InsertImplementationAction : SelectionAction<String>() {

    /**
     * A virtual API interface for generating code implementations.
     */
    interface VirtualAPI {
        /**
         * Generates code based on a natural language specification.
         *
         * @param specification The natural language specification for the desired implementation.
         * @param prefix The code context or prefix for the implementation.
         * @param computerLanguage The programming language for the implementation.
         * @param humanLanguage The natural language used for the specification.
         * @return A ConvertedText object containing the generated code and language.
         */
        fun implementCode(
            specification: String,
            prefix: String,
            computerLanguage: String,
            humanLanguage: String
        ): ConvertedText

        /**
         * A data class representing the generated code and language.
         */
        class ConvertedText {
            var code: String? = null
            var language: String? = null
        }
    }

    // ... (other methods and classes)
}
```

This class extends `SelectionAction<String>` and provides an implementation for generating code based on a natural language specification. Here's a breakdown of the key components:

1. `VirtualAPI` interface: This interface defines a contract for generating code implementations. The `implementCode` function takes a natural language specification, code context/prefix, programming language, and human language as input, and returns a `ConvertedText` object containing the generated code and language.

2. `getProxy()` function: This function creates an instance of the `VirtualAPI` interface using the `ChatProxy` class from the `com.simiacryptus.jopenai.proxy` package. It configures the proxy with the appropriate API, model, temperature, and deserialization retries.

3. `getConfig()` function: This function is required by the `SelectionAction` interface and returns an empty string, as no additional configuration is needed for this action.

4. `defaultSelection()` and `editSelection()` functions: These functions are responsible for determining the initial selection range and editing the selection range, respectively. They look for comments or other relevant code elements to determine the selection range.

5. `processSelection()` function: This is the main function that processes the selected text or comment and generates the code implementation. It extracts the natural language specification from the selected text or comment, retrieves the code context (if available), and calls the `implementCode` function from the `VirtualAPI` to generate the code implementation. The generated code is then inserted below the selected text or comment.

6. `getPsiClassContextActionParams()` function: This function retrieves the selection start and end offsets, as well as the largest intersecting comment, for use in the `processSelection()` function.

7. `isLanguageSupported()` function: This function checks if the current programming language is supported by the action. It returns `false` for plain text, Markdown, or if the language is `null`.

8. `PsiClassContextActionParams` class: This is a data class used to hold the selection start and end offsets, as well as the largest intersecting comment.

Overall, this action provides a convenient way to generate code implementations based on natural language specifications within an Integrated Development Environment (IDE) or code editor.

# code\RenameVariablesAction.kt

Sure, here's the documentation for the `RenameVariablesAction` class:

```kotlin
/**
 * An action that suggests and applies variable name renames in the selected code.
 *
 * This action uses an AI model to suggest better variable names for the selected code.
 * The user can then choose which variable names to rename, and the action will apply
 * the selected renames to the code.
 *
 * @property proxy The proxy object used to communicate with the AI model for suggesting renames.
 */
open class RenameVariablesAction : SelectionAction<String>() {

    /**
     * An interface defining the API for suggesting variable renames.
     */
    interface RenameAPI {
        /**
         * Suggests variable renames for the given code.
         *
         * @param code The code for which to suggest variable renames.
         * @param computerLanguage The programming language of the code.
         * @param humanLanguage The human language to use for the suggestions.
         * @return A response containing the suggested renames.
         */
        fun suggestRenames(
            code: String,
            computerLanguage: String,
            humanLanguage: String
        ): SuggestionResponse

        /**
         * A data class representing the response from the rename suggestion API.
         *
         * @property suggestions A list of suggested renames.
         */
        class SuggestionResponse {
            var suggestions: MutableList<Suggestion> = mutableListOf()

            /**
             * A data class representing a single suggested rename.
             *
             * @property originalName The original variable name.
             * @property suggestedName The suggested variable name.
             */
            class Suggestion {
                var originalName: String? = null
                var suggestedName: String? = null
            }
        }
    }

    /**
     * The proxy object used to communicate with the AI model for suggesting renames.
     */
    val proxy: RenameAPI
        get() = /* ... */

    /**
     * Returns an empty string as the configuration for this action.
     */
    override fun getConfig(project: Project?): String = ""

    /**
     * Processes the selected code and applies the chosen variable renames.
     *
     * @param event The action event.
     * @param state The selection state containing the selected code and language.
     * @param config The configuration for this action (unused).
     * @return The code with the chosen variable renames applied.
     */
    override fun processSelection(event: AnActionEvent?, state: SelectionState, config: String?): String {
        // Get rename suggestions from the AI model
        val renameSuggestions = /* ... */

        // Let the user choose which renames to apply
        val selectedSuggestions = choose(renameSuggestions)

        // Apply the chosen renames to the selected code
        return /* ... */
    }

    /**
     * Displays a dialog for the user to choose which variable renames to apply.
     *
     * @param renameSuggestions A map of original variable names to suggested names.
     * @return A set of original variable names for which to apply the suggested renames.
     */
    open fun choose(renameSuggestions: Map<String, String>): Set<String> {
        return /* ... */
    }

    /**
     * Checks if the action supports the given programming language.
     *
     * @param computerLanguage The programming language to check.
     * @return `true` if the language is supported, `false` otherwise.
     */
    override fun isLanguageSupported(computerLanguage: ComputerLanguage?): Boolean =
        computerLanguage != ComputerLanguage.Text
}
```

This class extends the `SelectionAction` class and provides functionality for suggesting and applying variable renames to the selected code. The `RenameAPI` interface defines the API for suggesting renames, and the `proxy` property provides an instance of this API.

The `processSelection` function is the main entry point for the action. It gets rename suggestions from the AI model, displays a dialog for the user to choose which renames to apply, and then applies the chosen renames to the selected code.

The `choose` function is responsible for displaying the dialog and allowing the user to select which variable renames to apply.

The `isLanguageSupported` function checks if the action supports the given programming language, returning `true` for all languages except plain text.

Overall, this class provides a convenient way for users to improve the variable naming in their code with the help of an AI model.

# dev\AppServer.kt

Sure, here's the documentation for the provided code:

```kotlin
package com.github.simiacryptus.aicoder.actions.dev

import com.github.simiacryptus.aicoder.config.AppSettingsState
import com.github.simiacryptus.aicoder.util.UITools
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.project.Project
import com.simiacryptus.skyenet.webui.chat.ChatServer
import org.eclipse.jetty.server.Server
import org.eclipse.jetty.server.handler.ContextHandlerCollection
import org.eclipse.jetty.webapp.WebAppContext
import org.eclipse.jetty.websocket.server.config.JettyWebSocketServletContainerInitializer
import org.slf4j.LoggerFactory
import java.net.InetSocketAddress

/**
 * This class represents an application server that can host multiple ChatServer instances.
 * It uses the Jetty server to run the ChatServer instances as web applications.
 *
 * @param localName The hostname or IP address to bind the server to.
 * @param port The port number to listen on.
 * @param project The IntelliJ project associated with this server (can be null).
 */
class AppServer(
  private val localName: String,
  private val port: Int,
  project: Project?
) {
    // ... (code omitted for brevity) ...

    /**
     * Adds a new ChatServer instance to the server.
     *
     * @param path The context path for the ChatServer instance.
     * @param socketServer The ChatServer instance to add.
     */
    @Synchronized
    fun addApp(path: String, socketServer: ChatServer) {
        // ... (code omitted for brevity) ...
    }

    /**
     * Creates a new WebAppContext for the given ChatServer instance.
     *
     * @param server The ChatServer instance.
     * @param path The context path for the ChatServer instance.
     * @return The created WebAppContext.
     */
    private fun newWebAppContext(server: ChatServer, path: String): WebAppContext {
        // ... (code omitted for brevity) ...
    }

    // ... (code omitted for brevity) ...

    companion object {
        /**
         * Gets the singleton instance of the AppServer.
         *
         * @param project The IntelliJ project associated with the server (can be null).
         * @return The AppServer instance.
         */
        fun getServer(project: Project?): AppServer {
            // ... (code omitted for brevity) ...
        }

        /**
         * Stops and cleans up the singleton instance of the AppServer.
         */
        fun stop() {
            // ... (code omitted for brevity) ...
        }
    }
}
```

This code defines an `AppServer` class that is responsible for hosting multiple instances of `ChatServer` as web applications using the Jetty server. The `AppServer` class has the following main components:

1. **Constructor**: The constructor takes three parameters: `localName` (the hostname or IP address to bind the server to), `port` (the port number to listen on), and `project` (the IntelliJ project associated with the server, which can be null).

2. **addApp()**: This method allows you to add a new `ChatServer` instance to the server. It takes two parameters: `path` (the context path for the `ChatServer` instance) and `socketServer` (the `ChatServer` instance to add).

3. **newWebAppContext()**: This private method creates a new `WebAppContext` for the given `ChatServer` instance. It configures the `WebAppContext` with the necessary settings, such as the base resource, class loader, context path, and welcome files.

4. **Companion Object**: The companion object provides two static methods:
   - `getServer()`: This method returns the singleton instance of the `AppServer`. If the instance doesn't exist or the server is not running, it creates a new instance and starts it.
   - `stop()`: This method stops and cleans up the singleton instance of the `AppServer`.

The `AppServer` class also includes some helper methods and fields for managing the server lifecycle, handling progress indicators, and logging.

Overall, this code provides a way to host multiple `ChatServer` instances as web applications within a single Jetty server instance. It allows you to dynamically add or remove `ChatServer` instances and manage the server lifecycle.

# dev\PrintTreeAction.kt

Sure, here's the documentation for the `PrintTreeAction` class:

```kotlin
/**
 * The PrintTreeAction class is an IntelliJ action that enables developers to print the tree structure of a PsiFile.
 * To use this action, follow these steps:
 *
 * 1. Make sure that the "devActions" setting is enabled in the plugin's settings.
 * 2. Open the file you want to print the tree structure of in the IntelliJ editor.
 * 3. Right-click in the editor and select the "PrintTreeAction" option from the context menu.
 *
 * This action will print the tree structure of the currently open file to the log.
 *
 * @see BaseAction The base class for this action.
 * @see AppSettingsState The class that manages the plugin's settings.
 * @see PsiUtil A utility class for working with the IntelliJ PSI (Program Structure Interface).
 */
class PrintTreeAction : BaseAction() {

    /**
     * Handles the action event by printing the tree structure of the largest contained entity
     * (e.g., file, class, method) to the log.
     *
     * @param e The action event.
     */
    override fun handle(e: AnActionEvent) {
        log.warn(PsiUtil.printTree(PsiUtil.getLargestContainedEntity(e)!!))
    }

    /**
     * Checks if the action is enabled based on the "devActions" setting in the plugin's settings.
     *
     * @param event The action event.
     * @return True if the "devActions" setting is enabled, false otherwise.
     */
    override fun isEnabled(event: AnActionEvent): Boolean {
        return AppSettingsState.instance.devActions
    }

    companion object {
        private val log = LoggerFactory.getLogger(PrintTreeAction::class.java)
    }
}
```

This documentation explains the purpose of the `PrintTreeAction` class, how to use the action, and provides details about the class members and their responsibilities. It also includes cross-references to related classes and utilities used by the action.

# dev\InternalCoderAction.kt

Sure, here's the documentation for the `InternalCoderAction` class:

```kotlin
/**
 * An action that launches an internal coding agent within the IntelliJ IDE.
 *
 * This action creates a new session for the coding agent and initializes it with the current
 * context information from the IDE, such as the current editor, file, project, and symbols.
 * It then starts a web server and opens a browser window to interact with the coding agent.
 *
 * The coding agent is an instance of [CodingAgent] and is configured with the specified API,
 * data storage, session, user, UI, interpreter, symbols, temperature, details, and model.
 *
 * When the user sends a message to the coding agent, the [userMessage] function of the
 * [ApplicationServer] is called, which starts the coding agent with the user's message.
 *
 * @property path The path for the internal coding agent's web server.
 */
class InternalCoderAction : BaseAction() {
    // ...

    /**
     * Handles the action event by launching the internal coding agent.
     *
     * @param e The action event.
     */
    override fun handle(e: AnActionEvent) {
        // ...
    }

    /**
     * Determines whether the action is enabled based on the development settings.
     *
     * @param event The action event.
     * @return `true` if the action is enabled, `false` otherwise.
     */
    override fun isEnabled(event: AnActionEvent): Boolean {
        // ...
    }

    companion object {
        // ...

        /**
         * Initializes the application server for the internal coding agent.
         *
         * @param server The app server instance.
         * @param path The path for the internal coding agent's web server.
         * @return The initialized [ApplicationServer] instance.
         */
        private fun initApp(server: AppServer, path: String): ApplicationServer {
            // ...
        }
    }
}
```

This class extends `BaseAction` and provides an implementation for the `handle` function, which is responsible for launching the internal coding agent. It creates a new session, initializes the coding agent with the current context information, starts a web server, and opens a browser window to interact with the agent.

The `isEnabled` function checks if the development actions are enabled in the app settings before allowing the action to be executed.

The `initApp` function is a companion object function that initializes the `ApplicationServer` for the internal coding agent. It creates a new instance of `ApplicationServer` if it doesn't already exist and registers it with the app server.

The `ApplicationServer` instance overrides the `userMessage` function to start the coding agent with the user's message when a message is received. It also provides the root directory for the coding agent's data storage.

# FileContextAction.kt

Sure, here's the documentation for the `FileContextAction` class:

The `FileContextAction` is an abstract class that extends `BaseAction` and provides a base implementation for actions that operate on files or folders in the current project. It defines two abstract methods that subclasses must implement:

1. `processSelection(state: SelectionState, config: T?): Array<File>`: This method is responsible for processing the selected file or folder and returning an array of generated files.

2. `getConfig(project: Project?, e: AnActionEvent): T?`: This method returns an optional configuration object of type `T` that can be used by the `processSelection` method.

The `FileContextAction` class also provides the following functionality:

- It handles the action execution by retrieving the selected file or folder, calling the `processSelection` method, and opening the generated files in the IDE.
- It supports both file and folder selections, controlled by the `supportsFiles` and `supportsFolders` constructor parameters.
- It provides a `SelectionState` data class that encapsulates the selected file or folder and the project root directory.
- It includes a utility method `open(project: Project, outputPath: Path)` that opens a generated file in the IDE. This method uses a scheduled thread pool to periodically check if the file exists and is ready to be opened.
- It has an `isDevAction` property that can be used to enable or disable the action based on the user's development mode settings.

The `FileContextAction` class also includes a companion object with a logger instance and the `open` method implementation.

To create a new action that extends `FileContextAction`, you need to implement the `processSelection` and `getConfig` methods. The `processSelection` method should perform the desired file or folder processing and return an array of generated files. The `getConfig` method can return an optional configuration object that will be passed to the `processSelection` method.

Here's an example of how you might implement a subclass of `FileContextAction`:

```kotlin
class MyFileAction : FileContextAction<MyConfig>() {

    override fun processSelection(state: SelectionState, config: MyConfig?): Array<File> {
        // Perform file or folder processing based on the selected file/folder and configuration
        val generatedFiles = mutableListOf<File>()
        // ... add generated files to the list
        return generatedFiles.toTypedArray()
    }

    override fun getConfig(project: Project?, e: AnActionEvent): MyConfig? {
        // Return a configuration object or null if no configuration is needed
        return MyConfig(/* ... */)
    }

}
```

In this example, `MyFileAction` is a subclass of `FileContextAction` that takes a custom configuration object `MyConfig`. The `processSelection` method performs the desired file or folder processing and returns an array of generated files. The `getConfig` method returns an instance of `MyConfig` or `null` if no configuration is needed.

# generic\AppendAction.kt

Sure, here's the documentation for the `AppendAction` class:

```kotlin
/**
 * An action that appends text to the end of the user's selected text using OpenAI's chat model.
 *
 * This action is a subclass of `SelectionAction<String>`, which means it operates on a selected text
 * and returns a `String` as the result.
 *
 * When the action is executed, it creates a `ChatRequest` with the following properties:
 * - The model specified in the app settings
 * - The temperature specified in the app settings
 * - Two messages:
 *   1. A system message instructing the model to append text to the user's prompt
 *   2. A user message containing the selected text
 *
 * The action then sends the `ChatRequest` to the OpenAI API and retrieves the response.
 * The response is then processed to append the generated text to the end of the selected text.
 * If the generated text starts with the selected text, the duplicated portion is removed.
 *
 * The `getConfig` method is overridden to return an empty string, as this action does not require
 * any additional configuration.
 */
class AppendAction : SelectionAction<String>() {
    /**
     * Returns an empty string, as this action does not require any additional configuration.
     */
    override fun getConfig(project: Project?): String {
        return ""
    }

    /**
     * Processes the selected text by appending text generated by the OpenAI chat model.
     *
     * @param state The selection state containing the selected text.
     * @param config Not used in this action.
     * @return The selected text with the generated text appended to the end.
     */
    override fun processSelection(state: SelectionState, config: String?): String {
        val settings = AppSettingsState.instance
        val request = ChatRequest(
            model = settings.defaultChatModel().modelName,
            temperature = settings.temperature
        ).copy(
            temperature = settings.temperature,
            messages = listOf(
                ChatMessage(Role.system, "Append text to the end of the user's prompt".toContentList(), null),
                ChatMessage(Role.user, state.selectedText.toString().toContentList(), null)
            ),
        )
        val chatResponse = api.chat(request, settings.defaultChatModel())
        val b4 = state.selectedText ?: ""
        val str = chatResponse.choices[0].message?.content ?: ""
        return b4 + if (str.startsWith(b4)) str.substring(b4.length) else str
    }
}
```

This documentation explains the purpose of the `AppendAction` class, its inheritance from `SelectionAction<String>`, and the details of how it creates and processes the `ChatRequest` to append text to the selected text using the OpenAI chat model. It also documents the `getConfig` and `processSelection` methods, explaining their roles and parameters.

# generic\AnalogueFileAction.kt

Sure, here's the documentation for the `AnalogueFileAction` class:

```kotlin
/**
 * An action that generates a new file based on a user-provided directive and an existing file's code.
 * The new file is created in the same directory as the existing file, with a unique filename if necessary.
 * After generating the file, it is opened in the IDE.
 */
class AnalogueFileAction : FileContextAction<AnalogueFileAction.Settings>() {

    /**
     * Checks if the action is enabled for the current event.
     * The action is disabled if the selected file is a directory.
     */
    override fun isEnabled(event: AnActionEvent): Boolean { ... }

    /**
     * Data class representing a project file with its path and code content.
     */
    data class ProjectFile(
        val path: String = "",
        val code: String = ""
    )

    /**
     * UI class for displaying the directive input field.
     */
    class SettingsUI { ... }

    /**
     * User settings data class for storing the directive.
     */
    class UserSettings(
        var directive: String = "",
    )

    /**
     * Settings data class for storing the user settings and the current project.
     */
    class Settings(
        val settings: UserSettings? = null,
        val project: Project? = null
    )

    /**
     * Gets the configuration for the action by showing a dialog to the user.
     */
    override fun getConfig(project: Project?, e: AnActionEvent): Settings { ... }

    /**
     * Processes the selected file by generating a new file based on the directive and the existing file's code.
     * Returns an array containing the generated file.
     */
    override fun processSelection(state: SelectionState, config: Settings?): Array<File> { ... }

    /**
     * Generates a new file based on the provided base file and directive.
     */
    private fun generateFile(baseFile: ProjectFile, directive: String): ProjectFile { ... }

    companion object {
        /**
         * Opens the generated file in the IDE.
         */
        fun open(project: Project, outputPath: Path) { ... }

        /**
         * Gets the module root directory for the given file by searching for the nearest `.git` directory.
         */
        fun getModuleRootForFile(file: File): File { ... }
    }
}
```

The `AnalogueFileAction` class extends `FileContextAction` and provides functionality to generate a new file based on a user-provided directive and an existing file's code. The user is prompted to enter a directive through a dialog, and the action then generates a new file using the OpenAI API. The generated file is created in the same directory as the existing file, with a unique filename if necessary. After generating the file, it is opened in the IDE.

The class has several nested classes:

- `ProjectFile`: A data class representing a project file with its path and code content.
- `SettingsUI`: A UI class for displaying the directive input field.
- `UserSettings`: A data class for storing the user-provided directive.
- `Settings`: A data class for storing the user settings and the current project.

The `generateFile` function is responsible for generating the new file using the OpenAI API. It sends a request to the API with the existing file's code and the user-provided directive, and the API generates the new file's content based on these inputs.

The `open` function in the companion object is responsible for opening the generated file in the IDE. It uses the `FileEditorManager` to open the file and refreshes the file system if necessary.

The `getModuleRootForFile` function in the companion object finds the module root directory for the given file by searching for the nearest `.git` directory.

Overall, this action provides a convenient way for developers to generate new files based on existing code and natural language instructions, leveraging the power of the OpenAI API.

# generic\AutoDevAction.kt

This code defines an IntelliJ IDEA plugin action called `AutoDevAction` that provides an interactive code development assistant powered by OpenAI's language models. Here's a breakdown of the code:

1. **AutoDevAction Class**
   - This class extends `BaseAction` and is responsible for handling the action event triggered by the user.
   - When the action is triggered, it creates a new session and initializes an `AutoDevApp` instance.
   - It also starts a server and opens a web browser window pointing to the server's URL.

2. **AutoDevApp Class**
   - This class extends `ApplicationServer` and handles the user's input messages.
   - It defines a `Settings` data class to store the user's preferences, such as the budget, tools, and model to be used.
   - The `userMessage` function is called when the user sends a message.
   - It creates an `AutoDevAgent` instance and starts the code development process based on the user's message.

3. **AutoDevAgent Class**
   - This class extends `ActorSystem` and is responsible for managing the code development process.
   - It defines two actors: `DesignActor` and `TaskCodingActor`.
   - The `DesignActor` is responsible for translating the user's directive into an action plan (a list of tasks).
   - The `TaskCodingActor` is responsible for implementing the changes to the codebase as described in the task list.
   - The `start` function is the entry point for the code development process.
   - It iterates over the tasks in the action plan and applies the necessary code changes using the `TaskCodingActor`.
   - The code changes are displayed as diffs, and the user can apply them to the codebase.

4. **Companion Object**
   - The companion object contains utility functions and data classes.
   - The `TaskList` data class represents the action plan generated by the `DesignActor`.
   - The `Task` data class represents an individual task in the action plan, including the paths of the files to be modified and a description of the changes.

Overall, this code provides an interactive code development assistant that can understand natural language instructions from the user, generate an action plan, and apply the necessary code changes to the codebase. It leverages OpenAI's language models and integrates with IntelliJ IDEA to provide a seamless development experience.

# generic\CodeChatAction.kt

Sure, here's the documentation for the `CodeChatAction` class:

```kotlin
/**
 * This class represents an action that opens a code chat session in a web browser.
 * It allows the user to interact with an AI model to get assistance with the code they have open in the editor.
 *
 * When the action is triggered, it creates a new session ID and initializes a [CodeChatSocketManager] instance
 * with the selected code, file name, and language. It then starts a web server and opens a browser window
 * pointing to the code chat application.
 *
 * The code chat application is a web-based interface that allows the user to chat with the AI model
 * and receive suggestions and explanations related to their code.
 */
class CodeChatAction : BaseAction() {

    /**
     * The path where the code chat application is served.
     */
    val path = "/codeChat"

    /**
     * Handles the action event by creating a new code chat session and opening a browser window.
     *
     * @param e The action event.
     */
    override fun handle(e: AnActionEvent) {
        // ... (implementation details)
    }

    /**
     * Checks if the action is enabled for the given event.
     *
     * @param event The action event.
     * @return `true` if the action is enabled, `false` otherwise.
     */
    override fun isEnabled(event: AnActionEvent) = true

    companion object {
        /**
         * The logger instance for this class.
         */
        private val log = LoggerFactory.getLogger(CodeChatAction::class.java)

        /**
         * A map of active socket managers for code chat sessions.
         */
        private val agents = mutableMapOf<Session, SocketManager>()

        /**
         * The root directory for the code chat application.
         */
        val root: File get() = File(ApplicationEvents.pluginHome, "code_chat")

        /**
         * Initializes the code chat application server and registers it with the provided [AppServer].
         *
         * @param server The [AppServer] instance.
         * @param path The path where the code chat application is served.
         * @return The initialized [ChatServer] instance.
         */
        private fun initApp(server: AppServer, path: String): ChatServer {
            // ... (implementation details)
        }
    }
}
```

This documentation provides an overview of the `CodeChatAction` class, its purpose, and the responsibilities of its methods and properties. It also includes documentation for the companion object and its members.

# generic\CreateFileAction.kt

This code defines a class `CreateFileAction` that extends `FileContextAction`. It is used to create a new file based on a natural language directive provided by the user. Here's a breakdown of the code:

1. `CreateFileAction` class:
   - Extends `FileContextAction` with `false` for `isReadOnly` and `true` for `isWritable`.
   - Defines an inner class `ProjectFile` to hold the file path and code.
   - Defines an inner class `Settings` to hold the user-provided directive.

2. `processSelection` method:
   - Takes a `SelectionState` and `Settings` object as input.
   - Determines the project root, module root, and relative file path based on the selected file.
   - Calls the `generateFile` method to generate the new file content based on the directive.
   - Constructs the output file path, ensuring it doesn't already exist.
   - Creates the necessary directories and writes the generated code to the output file.
   - Returns an array containing the newly created file.

3. `generateFile` method:
   - Takes the base file path and the user-provided directive as input.
   - Constructs a ChatRequest with instructions for generating a new file based on the directive.
   - Sends the ChatRequest to the OpenAI API using the configured model and temperature settings.
   - Parses the response to extract the file path and code.
   - Returns a `ProjectFile` object containing the file path and code.

The `CreateFileAction` class is designed to be used within the context of a larger application that interacts with files and directories. It leverages the OpenAI API to generate new file content based on natural language instructions provided by the user. The generated file is created at a location relative to the selected file or project root, ensuring that it doesn't overwrite existing files.

# generic\DictationAction.kt

Sure, here's the documentation for the `DictationAction` class:

```kotlin
/**
 * This class represents an action in the IntelliJ IDEA plugin that allows users to dictate text
 * using speech-to-text functionality. When the action is triggered, it starts recording audio,
 * processes the audio data, and transcribes the speech into text, which is then inserted into
 * the currently open editor at the cursor position or replacing the selected text.
 */
class DictationAction : BaseAction() {

    /**
     * This method is the entry point for the action. It sets up the necessary threads and components
     * for recording audio, processing the audio data, and transcribing the speech into text.
     *
     * @param e The action event that triggered the action.
     */
    override fun handle(e: AnActionEvent) {
        // Implementation details...
    }

    /**
     * This inner class is responsible for transcribing the audio data into text and inserting
     * the transcribed text into the editor.
     *
     * @property event The action event that triggered the action.
     * @property audioBuffer The buffer containing the audio data to be transcribed.
     * @property continueFn A function that determines whether the transcription should continue.
     * @property offsetStart The initial offset in the editor where the transcribed text should be inserted.
     * @property prompt The initial prompt for the speech-to-text transcription.
     */
    private inner class DictationPump(
        val event: AnActionEvent,
        private val audioBuffer: Deque<ByteArray>,
        val continueFn: () -> Boolean,
        offsetStart: Int,
        var prompt: String = ""
    ) {

        /**
         * This method runs the transcription process. It continuously checks the audio buffer
         * and transcribes the audio data into text, which is then inserted into the editor.
         */
        fun run() {
            // Implementation details...
        }
    }

    /**
     * This method creates and displays a dialog window that shows the status of the dictation process.
     *
     * @param e1 The action event that triggered the action.
     * @return The JFrame instance representing the dialog window.
     */
    private fun statusDialog(e1: AnActionEvent): JFrame {
        // Implementation details...
    }

    /**
     * This method checks if the action is enabled by attempting to get a TargetDataLine instance,
     * which is required for audio recording.
     *
     * @param event The action event that triggered the action.
     * @return True if the action is enabled, false otherwise.
     */
    override fun isEnabled(event: AnActionEvent): Boolean {
        // Implementation details...
    }

    companion object {
        // Companion object with logger and thread pool instances
    }
}
```

This documentation provides an overview of the `DictationAction` class, its purpose, and the responsibilities of its methods and inner classes. It also includes brief descriptions of the properties and parameters used in the class.

# generic\DiffChatAction.kt

Sure, here's the documentation for the `DiffChatAction` class:

```kotlin
/**
 * An action that opens a chat interface for generating code diffs based on the selected code or the entire file.
 *
 * When triggered, this action creates a new session and opens a web interface where the user can interact with
 * an AI model to generate code diffs. The generated diffs are rendered with links that allow the user to apply
 * the changes directly to the code editor.
 *
 * The action supports the following features:
 *
 * - If text is selected in the code editor, the selected text is used as the input code. Otherwise, the entire
 *   file content is used.
 * - The AI model is instructed to provide code patches in the diff format, using `+` for line additions and `-`
 *   for line deletions, with sufficient context before each change.
 * - The generated response is rendered as HTML with Markdown support, and the diff sections are enhanced with
 *   links that allow the user to apply the changes to the code editor with a single click.
 * - The web interface is opened in the default system browser, and the session is managed by a `CodeChatSocketManager`
 *   instance.
 * - The action is enabled for all events.
 *
 * @property path The path at which the chat server is mounted.
 * @property agents A map of active sessions and their corresponding `SocketManager` instances.
 * @property root The root directory for storing data related to the code chat feature.
 */
class DiffChatAction : BaseAction() {
    // ...
}
```

This class extends `BaseAction` and overrides the `handle` method to perform the action when triggered. The `handle` method performs the following steps:

1. Retrieves the current editor and the selected text or the entire file content.
2. Creates a new session and a `CodeChatSocketManager` instance with the selected code, language, and other settings.
3. Customizes the `systemPrompt` and `renderResponse` methods of the `CodeChatSocketManager` to provide instructions for generating diffs and rendering the response with diff links.
4. Initializes a chat server application and associates the session with the `CodeChatSocketManager` instance.
5. Opens the chat interface in the default system browser.

The `isEnabled` method always returns `true`, allowing the action to be enabled in all contexts.

The companion object contains utility methods for managing the chat server and sessions, as well as a logger instance.

# generic\LineFilterChatAction.kt

Sure, here's the documentation for the `LineFilterChatAction` class:

```kotlin
/**
 * This class represents an action that opens a chat interface for discussing and annotating code.
 * When the action is triggered, it creates a new chat session with a prompt that includes the
 * selected code or the entire file content if no selection is made. The chat interface allows
 * the user to interact with an AI assistant to ask questions and receive explanations about the code.
 *
 * The chat interface is rendered in a web browser, and the responses from the AI assistant are
 * formatted using Markdown. Additionally, the assistant can reference specific lines of code by
 * including the line number in the response.
 */
class LineFilterChatAction : BaseAction() {

    /**
     * The path where the chat application is hosted.
     */
    val path = "/codeChat"

    /**
     * Handles the action event and opens the chat interface.
     *
     * @param e The action event.
     */
    override fun handle(e: AnActionEvent) {
        // ... (implementation details)
    }

    /**
     * Checks if the action is enabled for the given event.
     *
     * @param event The action event.
     * @return `true` if the action is enabled, `false` otherwise.
     */
    override fun isEnabled(event: AnActionEvent) = true

    companion object {
        // ... (companion object implementation details)
    }
}
```

The `LineFilterChatAction` class extends the `BaseAction` class and overrides the `handle` method to handle the action event. When the action is triggered, it retrieves the selected code or the entire file content, creates a new chat session with a prompt that includes the code, and opens the chat interface in a web browser.

The chat interface is powered by a `ChatSocketManager` instance, which is responsible for managing the chat session and rendering the responses from the AI assistant. The responses are formatted using Markdown, and the assistant can reference specific lines of code by including the line number in the response.

The `isEnabled` method is overridden to always return `true`, indicating that the action is always enabled.

The companion object contains utility methods and properties related to the chat application, such as initializing the application server, managing the chat sessions, and handling the application root directory.

# generic\RedoLast.kt

Sure, here's the documentation for the `RedoLast` action:

```kotlin
/**
 * The RedoLast action is an IntelliJ action that allows users to redo the last AI Coder action they performed in the editor.
 *
 * This action is part of the AI Coder plugin for IntelliJ IDEA. It provides a convenient way for users to redo the last
 * action performed by the AI Coder plugin in the current editor.
 *
 * To use this action, open the editor and select the RedoLast action from the editor context menu or use the corresponding
 * keyboard shortcut (if configured). This will redo the last action that was performed in the editor by the AI Coder plugin.
 *
 * The action is enabled only if there is a previous AI Coder action that can be redone in the current editor.
 *
 * @see BaseAction The base class for IntelliJ actions in the AI Coder plugin.
 * @see retry A utility function that manages the redo/undo operations for AI Coder actions.
 */
class RedoLast : BaseAction() {

    /**
     * Handles the action event by redoing the last AI Coder action in the current editor.
     *
     * @param e The action event containing information about the context in which the action was invoked.
     */
    override fun handle(e: AnActionEvent) {
        retry[e.getRequiredData(CommonDataKeys.EDITOR).document]!!.run()
    }

    /**
     * Checks if the action is enabled in the current context.
     *
     * The action is enabled if there is a previous AI Coder action that can be redone in the current editor.
     *
     * @param event The action event containing information about the context in which the action was invoked.
     * @return `true` if the action is enabled, `false` otherwise.
     */
    override fun isEnabled(event: AnActionEvent): Boolean {
        return null != retry[event.getRequiredData(CommonDataKeys.EDITOR).document]
    }
}
```

This documentation explains the purpose of the `RedoLast` action, how to use it, and the conditions under which it is enabled. It also provides brief descriptions of the `handle` and `isEnabled` functions, explaining their roles and parameters.

# generic\MultiDiffChatAction.kt

Sure, here's the documentation for the `MultiDiffChatAction` class:

```kotlin
/**
 * The MultiDiffChatAction class is an action that allows users to interact with an AI assistant
 * to modify multiple code files simultaneously. When the action is triggered, it opens a web-based
 * chat interface where the user can ask questions and provide instructions to the AI assistant.
 * The assistant can then suggest code changes in the form of diffs, which can be applied to the
 * selected files with a single click.
 *
 * The action works as follows:
 *
 * 1. The user selects one or more files in the IDE.
 * 2. The action retrieves the contents of the selected files and their associated programming languages.
 * 3. A chat session is created with a custom prompt that includes the contents of the selected files.
 * 4. The chat interface is opened in the user's default web browser.
 * 5. The user can interact with the AI assistant through the chat interface.
 * 6. When the assistant suggests code changes in the form of diffs, the user can apply these changes
 *    to the corresponding files with a single click.
 *
 * The action uses the following components:
 *
 * - `ChatSocketManager`: Manages the chat session and handles user input and AI responses.
 * - `ApplicationServer`: Provides the web-based chat interface.
 * - `StorageInterface`: Manages the storage of chat sessions and associated data.
 *
 * The `handle` function is the entry point of the action. It retrieves the selected files, creates
 * the chat session, and opens the chat interface in the user's web browser.
 *
 * The `commonRoot` extension function is used to find the common root directory of the selected files.
 * This is used to display relative file paths in the chat interface.
 *
 * The `commonPrefixWith` extension function is a helper function used by `commonRoot`.
 */
class MultiDiffChatAction : BaseAction() {
    // Implementation details...
}
```

This documentation explains the purpose of the `MultiDiffChatAction` class, its workflow, the components it uses, and the roles of the `handle`, `commonRoot`, and `commonPrefixWith` functions. It should provide a clear understanding of how the action works and what it does.

# generic\DocumentationCompilerAction.kt

Sure, here's the documentation for the `DocumentationCompilerAction` class:

```kotlin
/**
 * An action that allows the user to select files and provide instructions to generate documentation
 * using an AI model. The generated documentation is saved to a file in the selected directory.
 */
class DocumentationCompilerAction : FileContextAction<DocumentationCompilerAction.Settings>() {

    /**
     * Checks if the action is enabled for the given event. The action is enabled only if a non-directory
     * file is selected.
     *
     * @param event The action event.
     * @return True if the action is enabled, false otherwise.
     */
    override fun isEnabled(event: AnActionEvent): Boolean {
        if (UITools.getSelectedFile(event)?.isDirectory == false) return false
        return super.isEnabled(event)
    }

    /**
     * A class to hold the settings for the action.
     *
     * @property settings The user-defined settings, or null if not set.
     * @property project The current project, or null if not available.
     */
    class Settings(
        val settings: UserSettings? = null,
        val project: Project? = null
    )

    /**
     * A class to hold the user-defined settings for the action.
     *
     * @property transformationMessage The instruction message for the AI model.
     * @property outputFilename The name of the output file for the generated documentation.
     * @property filesToProcess The list of files to process for documentation generation.
     */
    class UserSettings(
        var transformationMessage: String = "Create user documentation",
        var outputFilename: String = "compiled_documentation.md",
        var filesToProcess: List<Path> = listOf(),
    )

    /**
     * A class to hold the UI elements for the settings dialog.
     */
    class SettingsUI {
        @Name("Files to Process")
        val filesToProcess = CheckBoxList<Path>()

        @Name("AI Instruction")
        val transformationMessage = JBTextField()

        @Name("Output File")
        val outputFilename = JBTextField()
    }

    /**
     * Gets the configuration for the action by displaying a settings dialog to the user.
     *
     * @param project The current project, or null if not available.
     * @param e The action event.
     * @return The settings for the action.
     */
    override fun getConfig(project: Project?, e: AnActionEvent): Settings { ... }

    /**
     * Processes the selected files and generates documentation using the provided configuration.
     *
     * @param state The selection state containing the selected file.
     * @param config The configuration for the action.
     * @return An array of generated files.
     */
    override fun processSelection(state: SelectionState, config: Settings?): Array<File> { ... }

    /**
     * Transforms the given file content using the provided transformation message and an AI model.
     *
     * @param fileContent The content of the file to transform.
     * @param transformationMessage The instruction message for the AI model.
     * @return The transformed content.
     */
    private fun transformContent(fileContent: String, transformationMessage: String): String { ... }

    companion object {
        /**
         * Opens the generated documentation file in the IDE.
         *
         * @param project The current project.
         * @param outputPath The path of the generated documentation file.
         */
        fun open(project: Project, outputPath: Path) { ... }
    }

    /**
     * A dialog for configuring the settings for the documentation compiler action.
     *
     * @property project The current project, or null if not available.
     * @property settingsUI The UI elements for the settings dialog.
     */
    class DocumentationCompilerDialog(project: Project?, private val settingsUI: SettingsUI) : DialogWrapper(project) {
        val userSettings = UserSettings()

        init { ... }

        override fun createCenterPanel(): JComponent? { ... }

        override fun doOKAction() { ... }
    }
}

/**
 * An extension property to get the list of items in a CheckBoxList.
 */
private val <T> CheckBoxList<T>.items: List<T>
    get() { ... }
```

This class provides an action that allows the user to select files and provide instructions for generating documentation using an AI model. The generated documentation is saved to a file in the selected directory.

The `DocumentationCompilerAction` class extends `FileContextAction` and overrides the `isEnabled` method to check if the action is enabled for the given event. The action is enabled only if a non-directory file is selected.

The `Settings` class holds the user-defined settings and the current project. The `UserSettings` class holds the user-defined settings for the action, including the instruction message for the AI model, the name of the output file, and the list of files to process.

The `SettingsUI` class holds the UI elements for the settings dialog, including a `CheckBoxList` for selecting files, text fields for the instruction message and output file name.

The `getConfig` method displays a settings dialog to the user and returns the configuration for the action. The `processSelection` method processes the selected files and generates documentation using the provided configuration. The `transformContent` method transforms the given file content using the provided transformation message and an AI model.

The `open` function in the companion object opens the generated documentation file in the IDE.

The `DocumentationCompilerDialog` class is a dialog for configuring the settings for the documentation compiler action. It extends `DialogWrapper` and provides a custom UI for the settings.

Finally, the `items` extension property provides a convenient way to get the list of items in a `CheckBoxList`.

# generic\ReplaceOptionsAction.kt

Sure, here's the documentation for the `ReplaceOptionsAction` class:

```kotlin
/**
 * An action that suggests replacement options for the selected text based on the surrounding context.
 *
 * This action uses a virtual API to generate suggestions for replacing the selected text with a new
 * text that fits the context. The virtual API is implemented using a ChatProxy, which sends a
 * request to a language model with the selected text and its surrounding context as examples.
 *
 * The action presents the user with a dialog to choose from the suggested options. The chosen option
 * replaces the selected text in the editor.
 */
open class ReplaceOptionsAction : SelectionAction<String>() {

    /**
     * A virtual API interface for suggesting text based on a template and examples.
     */
    interface VirtualAPI {
        /**
         * Suggests text to fill in the blank in the given template based on the provided examples.
         *
         * @param template A string with a blank (`_____`) to be filled in.
         * @param examples A list of example strings to guide the suggestions.
         * @return A Suggestions object containing a list of suggested choices.
         */
        fun suggestText(template: String, examples: List<String>): Suggestions

        /**
         * A data class to hold the suggested choices.
         */
        class Suggestions {
            /**
             * A list of suggested choices for filling in the blank.
             */
            var choices: List<String>? = null
        }
    }

    /**
     * A proxy to the virtual API implemented using a ChatProxy.
     */
    val proxy: VirtualAPI
        get() = ChatProxy(
            clazz = VirtualAPI::class.java,
            api = api,
            model = AppSettingsState.instance.defaultChatModel(),
            temperature = AppSettingsState.instance.temperature,
            deserializerRetries = 5
        ).create()

    /**
     * Returns an empty string as the configuration for this action.
     */
    override fun getConfig(project: Project?): String = ""

    /**
     * Processes the selected text and generates replacement options using the virtual API.
     *
     * @param event The action event that triggered this action.
     * @param state The selection state containing the selected text and its context.
     * @param config The configuration for this action (not used).
     * @return The chosen replacement option for the selected text.
     */
    override fun processSelection(event: AnActionEvent?, state: SelectionState, config: String?): String {
        val choices = UITools.run(event?.project, templateText, true, true) {
            val selectedText = state.selectedText
            val idealLength = 2.0.pow(2 + ceil(ln(selectedText?.length?.toDouble() ?: 1.0))).toInt()
            val selectionStart = state.selectionOffset
            val allBefore = state.entireDocument?.substring(0, selectionStart) ?: ""
            val selectionEnd = state.selectionOffset + (state.selectionLength ?: 0)
            val allAfter = state.entireDocument?.substring(selectionEnd, state.entireDocument.length) ?: ""
            val before = StringUtil.getSuffixForContext(allBefore, idealLength).toString().replace('\n', ' ')
            val after = StringUtil.getPrefixForContext(allAfter, idealLength).toString().replace('\n', ' ')
            proxy.suggestText(
                "$before _____ $after",
                listOf(selectedText.toString())
            ).choices
        }
        return choose(choices ?: listOf())
    }

    /**
     * Presents a dialog to the user to choose from the given list of options.
     *
     * @param choices A list of options to choose from.
     * @return The chosen option, or an empty string if no option was chosen.
     */
    open fun choose(choices: List<String>): String =
        UITools.showRadioButtonDialog("Select an option to fill in the blank:", *choices.toTypedArray())?.toString() ?: ""
}
```

This class extends the `SelectionAction` class and overrides the `processSelection` method to generate replacement options for the selected text using a virtual API implemented with a `ChatProxy`. The `choose` method presents a dialog to the user to select one of the suggested options.

The `VirtualAPI` interface defines a `suggestText` method that takes a template string with a blank (`_____`) and a list of examples, and returns a `Suggestions` object containing a list of suggested choices for filling in the blank.

The `proxy` property creates an instance of the `VirtualAPI` using the `ChatProxy` class, which sends a request to a language model with the selected text and its surrounding context as examples.

In the `processSelection` method, the selected text and its surrounding context are extracted from the `SelectionState` object. The context is truncated to an ideal length based on the length of the selected text. The `suggestText` method of the `proxy` is then called with the context and the selected text as an example, and the resulting list of suggested choices is returned.

The `choose` method displays a dialog with the suggested choices as radio buttons, allowing the user to select one of the options. The chosen option is returned as the result of the `processSelection` method.

# generic\WebDevAction.kt

Sure, here's the documentation for the `WebDevAction` class:

```kotlin
/**
 * This class represents an action in the IntelliJ IDEA plugin that provides a web development assistant.
 * When triggered, it opens a new browser window with a web application that allows the user to describe
 * their desired web application, and the assistant will generate the necessary code files (HTML, CSS, JavaScript, etc.)
 * based on the user's input.
 *
 * The action is enabled only when a folder is selected in the project view.
 *
 * The main components of this class are:
 *
 * 1. [WebDevAction]: The main action class that handles the user's action and initializes the web application.
 * 2. [WebDevApp]: The web application server that handles user input and generates code files.
 * 3. [WebDevAgent]: The agent system that manages different actors responsible for generating code for different file types.
 *
 * The [WebDevApp] class uses an [ActorSystem] to manage different actors responsible for generating code for different file types.
 * The actors are defined in the [WebDevAgent] class, and they include:
 *
 * - [HtmlCodingActor]: Generates HTML code based on the user's input.
 * - [JavascriptCodingActor]: Generates JavaScript code based on the user's input.
 * - [CssCodingActor]: Generates CSS code based on the user's input.
 * - [ArchitectureDiscussionActor]: Generates a detailed architecture for the web application based on the user's input.
 * - [CodeReviewer]: Analyzes the generated code, looks for bugs, and provides fixes.
 * - [EtcCodingActor]: Generates other types of files (e.g., JSON, XML) based on the user's input.
 *
 * The [WebDevAgent] class orchestrates the interaction between these actors to generate the complete web application code.
 * It also provides a user interface for the user to provide feedback and revise the generated code.
 *
 * Overall, this action provides a convenient way for developers to quickly generate web application code based on natural language input,
 * leveraging the power of language models and the IntelliJ IDEA plugin ecosystem.
 */
class WebDevAction : BaseAction() {
    // ...
}
```

This documentation explains the purpose of the `WebDevAction` class, its main components, and the roles of the different actors involved in generating the web application code. It also provides an overview of how the different components interact with each other to achieve the desired functionality.

# generic\TaskRunnerAction.kt

The provided Kotlin code is an IntelliJ plugin action that integrates with a web UI and utilizes AI models to assist in task planning, documentation generation, new file creation, file patching, and inquiries. Here's a breakdown of the key components and their functionalities:

1. **TaskRunnerAction**
   - This class extends `BaseAction` and is responsible for initiating the task planning process.
   - When the action is triggered, it creates a new session, selects the current file or folder, and initializes the `TaskRunnerApp`.
   - It also opens a browser window with the application server URL.

2. **TaskRunnerApp**
   - This class extends `ApplicationServer` and serves as the main application server for the task planning process.
   - It handles user messages by creating a `TaskRunnerAgent` instance and starting the task processing.
   - It also manages sessions and settings for the application.

3. **TaskRunnerAgent**
   - This class extends `ActorSystem` and is responsible for processing user messages and delegating tasks to specific actors based on the task type.
   - It defines several actors for different tasks, such as `TaskBreakdownActor`, `DocumentationGeneratorActor`, `NewFileCreatorActor`, `FilePatcherActor`, and `InquiryActor`.
   - The `startProcess` method is the entry point for processing user messages. It breaks down the user request into smaller tasks using the `TaskBreakdownActor`, and then executes each task concurrently using a thread pool.
   - The `runTask` method handles the execution of individual tasks based on their type (e.g., creating new files, editing existing files, generating documentation, answering inquiries, or performing task planning).

4. **Actors**
   - The actors are responsible for performing specific tasks based on the user request and the provided context.
   - The `TaskBreakdownActor` identifies and lists smaller, actionable tasks from the user request.
   - The `DocumentationGeneratorActor` generates documentation for the provided code.
   - The `NewFileCreatorActor` creates new files based on the given requirements.
   - The `FilePatcherActor` generates patches for existing files to modify their functionality or fix issues.
   - The `InquiryActor` provides responses to user inquiries by compiling relevant information and insights.

5. **Task Breakdown and Execution**
   - The `TaskBreakdownResult` data class represents the output of the `TaskBreakdownActor`, containing a map of tasks and a final task ID.
   - The `Task` data class represents an individual task, including its description, type, dependencies, input files, and output files.
   - The `TaskType` enum defines the different types of tasks (TaskPlanning, Inquiry, NewFile, EditFile, Documentation).
   - The `GenState` data class keeps track of the subtasks, task IDs, reply text, completed tasks, and task futures during the execution process.
   - The `executionOrder` function determines the order in which tasks should be executed based on their dependencies.
   - The `buildMermaidGraph` function generates a Mermaid graph representation of the task dependencies.

The code provides a comprehensive system for task planning, code generation, documentation, and inquiries, leveraging AI models and a web UI for user interaction. It demonstrates a modular design with separate actors for different tasks, allowing for easy extensibility and maintenance.

# markdown\MarkdownImplementActionGroup.kt

Sure, here's the documentation for the provided code:

```kotlin
package com.github.simiacryptus.aicoder.actions.markdown

import com.github.simiacryptus.aicoder.actions.SelectionAction
import com.github.simiacryptus.aicoder.config.AppSettingsState
import com.github.simiacryptus.aicoder.util.ComputerLanguage
import com.github.simiacryptus.aicoder.util.UITools
import com.intellij.openapi.actionSystem.ActionGroup
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.project.Project
import com.simiacryptus.jopenai.proxy.ChatProxy

/**
 * An ActionGroup that provides actions to implement the selected text in various programming languages using Markdown.
 */
class MarkdownImplementActionGroup : ActionGroup() {
    private val markdownLanguages = listOf(
        "sql", "java", "asp", "c", "clojure", "coffee", "cpp", "csharp", "css", "bash", "go", "java", "javascript",
        "less", "make", "matlab", "objectivec", "pascal", "PHP", "Perl", "python", "rust", "scss", "sql", "svg",
        "swift", "ruby", "smalltalk", "vhdl"
    )

    override fun update(e: AnActionEvent) {
        e.presentation.isEnabledAndVisible = isEnabled(e)
        super.update(e)
    }

    companion object {
        /**
         * Checks if the action should be enabled for the given event.
         * @param e The action event.
         * @return True if the action should be enabled, false otherwise.
         */
        fun isEnabled(e: AnActionEvent): Boolean {
            val computerLanguage = ComputerLanguage.getComputerLanguage(e) ?: return false
            if (ComputerLanguage.Markdown != computerLanguage) return false
            return UITools.hasSelection(e)
        }
    }

    override fun getChildren(e: AnActionEvent?): Array<AnAction> {
        if (e == null) return emptyArray()
        val computerLanguage = ComputerLanguage.getComputerLanguage(e) ?: return emptyArray()
        val actions = markdownLanguages.map { language -> MarkdownImplementAction(language) }
        return actions.toTypedArray()
    }

    /**
     * An action that implements the selected text in a specific programming language using Markdown.
     */
    open class MarkdownImplementAction(private val language: String) : SelectionAction<String>(true) {
        init {
            templatePresentation.text = language
            templatePresentation.description = language
        }

        /**
         * An interface for the conversion API.
         */
        interface ConversionAPI {
            /**
             * Implements the given text in the specified computer language.
             * @param text The text to implement.
             * @param humanLanguage The human language of the input text.
             * @param computerLanguage The computer language to implement the text in.
             * @return The converted text.
             */
            fun implement(text: String, humanLanguage: String, computerLanguage: String): ConvertedText

            /**
             * A data class representing the converted text.
             */
            class ConvertedText {
                var code: String? = null
                var language: String? = null
            }
        }

        /**
         * Gets the proxy for the conversion API.
         * @return The conversion API proxy.
         */
        private fun getProxy(): ConversionAPI {
            return ChatProxy(
                clazz = ConversionAPI::class.java,
                api = api,
                model = AppSettingsState.instance.defaultChatModel(),
                temperature = AppSettingsState.instance.temperature,
                deserializerRetries = 5
            ).create()
        }

        override fun getConfig(project: Project?): String {
            return ""
        }

        /**
         * Processes the selected text and implements it in the specified programming language using Markdown.
         * @param state The selection state.
         * @param config The configuration (not used in this implementation).
         * @return The Markdown-formatted code block with the implemented code.
         */
        override fun processSelection(state: SelectionState, config: String?): String {
            val code = getProxy().implement(state.selectedText ?: "", "autodetect", language).code ?: ""
            return """
                |
                |
                |```$language
                |$code
                |```
                |
                |""".trimMargin()
        }
    }
}
```

This code defines an `ActionGroup` called `MarkdownImplementActionGroup` that provides actions to implement the selected text in various programming languages using Markdown. The group contains a list of supported programming languages (`markdownLanguages`).

The `isEnabled` function checks if the action should be enabled for the given event. It checks if the current file is a Markdown file and if there is a text selection.

The `getChildren` function creates an array of `MarkdownImplementAction` instances, one for each supported programming language.

The `MarkdownImplementAction` class is an implementation of the `SelectionAction` interface. It defines an interface called `ConversionAPI` that provides a method `implement` to convert the selected text into code in the specified programming language.

The `getProxy` function creates an instance of the `ConversionAPI` using the `ChatProxy` class from the `com.simiacryptus.jopenai.proxy` package. It configures the proxy with the API key, the default chat model, temperature, and the number of deserialization retries.

The `processSelection` function is the main logic of the action. It calls the `implement` method of the `ConversionAPI` with the selected text, the human language ("autodetect"), and the target programming language. It then formats the resulting code as a Markdown code block and returns it.

Overall, this code provides a convenient way to implement selected text in various programming languages within a Markdown file using an AI-powered conversion API.

# markdown\MarkdownListAction.kt

Sure, here's the documentation for the `MarkdownListAction` class:

```kotlin
/**
 * An action that generates new list items for a selected Markdown list in the editor.
 *
 * This action is enabled when the current file is a Markdown file and the caret or selection is inside a Markdown list.
 * When triggered, it extracts the existing list items, sends them to an AI model, and generates new list items based on
 * the existing ones. The new items are then appended to the end of the list in the editor.
 *
 * The AI model used for generating new list items is defined by the `ListAPI` interface, which is implemented using
 * the OpenAI API via the `ChatProxy` class. The `ListAPI` interface has a single method `newListItems` that takes
 * a list of existing items and a desired count, and returns a new list of items.
 *
 * The action also handles different bullet types (-, *, and numbered lists) and preserves the indentation of the
 * original list.
 */
class MarkdownListAction : BaseAction() {
    // ...
}
```

The `ListAPI` interface and its `Items` data class:

```kotlin
/**
 * An interface defining the API for generating new list items.
 */
interface ListAPI {
    /**
     * Generates new list items based on the provided existing items and desired count.
     *
     * @param items The existing list items.
     * @param count The desired total count of items (existing + new).
     * @return A data class containing the new list items.
     */
    fun newListItems(
        items: List<String?>?,
        count: Int,
    ): Items

    /**
     * A data class representing the result of the `newListItems` function.
     *
     * @property items The new list items.
     */
    data class Items(
        val items: List<String?>? = null,
    )
}
```

The `handle` function:

```kotlin
/**
 * The main function that handles the action when triggered.
 *
 * @param e The action event containing information about the current editor state.
 */
override fun handle(e: AnActionEvent) {
    // ...
}
```

The `isEnabled` function:

```kotlin
/**
 * Checks if the action should be enabled based on the current editor state.
 *
 * @param event The action event containing information about the current editor state.
 * @return `true` if the action should be enabled, `false` otherwise.
 */
override fun isEnabled(event: AnActionEvent): Boolean {
    // ...
}
```

This documentation explains the purpose of the `MarkdownListAction` class, the `ListAPI` interface used for generating new list items, and the main functions `handle` and `isEnabled`. It also provides brief descriptions of the parameters and return values for the relevant functions.

# SelectionAction.kt

Sure, here's the documentation for the `SelectionAction` class:

```kotlin
/**
 * An abstract class that provides a base implementation for actions that operate on selected text
 * or a default selection in an editor. This class handles the selection of text, retrieves the
 * necessary context information, and delegates the actual processing of the selection to subclasses.
 *
 * @param T The type of the configuration object used by the action.
 * @property requiresSelection Whether the action requires a non-empty selection to be enabled.
 *                             If set to false, the action will use a default selection if no text
 *                             is selected.
 */
abstract class SelectionAction<T : Any>(
    private val requiresSelection: Boolean = true
) : BaseAction() {

    /**
     * Returns the configuration object for the action, or null if no configuration is needed.
     *
     * @param project The current project, or null if no project is available.
     * @return The configuration object, or null if no configuration is needed.
     */
    open fun getConfig(project: Project?): T? = null

    // ... (implementation details omitted for brevity) ...

    /**
     * Determines whether the action is enabled for the given event.
     *
     * @param event The action event.
     * @return True if the action is enabled, false otherwise.
     */
    override fun isEnabled(event: AnActionEvent): Boolean { /* ... */ }

    /**
     * Data class representing the state of the selection and its context.
     *
     * @property selectedText The selected text, or null if no text is selected.
     * @property selectionOffset The offset of the selection start.
     * @property selectionLength The length of the selection, or null if no text is selected.
     * @property entireDocument The entire document text, or null if not available.
     * @property language The programming language of the document, or null if not available.
     * @property indent The indentation string to use, or null if not available.
     * @property contextRanges An array of context ranges surrounding the selection.
     * @property psiFile The PSI file representing the document, or null if not available.
     * @property project The current project, or null if no project is available.
     */
    data class SelectionState(
        val selectedText: String? = null,
        val selectionOffset: Int = 0,
        val selectionLength: Int? = null,
        val entireDocument: String? = null,
        val language: ComputerLanguage? = null,
        val indent: CharSequence? = null,
        val contextRanges: Array<ContextRange> = arrayOf(),
        val psiFile: PsiFile?,
        val project: Project?
    )

    /**
     * Determines whether the action supports the given programming language.
     *
     * @param computerLanguage The programming language.
     * @return True if the language is supported, false otherwise.
     */
    open fun isLanguageSupported(computerLanguage: ComputerLanguage?): Boolean { /* ... */ }

    /**
     * Returns the default selection range for the given offset if no text is selected.
     *
     * @param editorState The state of the editor.
     * @param offset The offset for which to get the default selection.
     * @return A pair representing the start and end offsets of the default selection.
     */
    open fun defaultSelection(editorState: EditorState, offset: Int): Pair<Int, Int> { /* ... */ }

    /**
     * Adjusts the given selection range if needed.
     *
     * @param state The state of the editor.
     * @param start The start offset of the selection.
     * @param end The end offset of the selection.
     * @return A pair representing the adjusted start and end offsets of the selection.
     */
    open fun editSelection(state: EditorState, start: Int, end: Int): Pair<Int, Int> { /* ... */ }

    /**
     * Processes the selected text or the default selection using the provided configuration.
     *
     * @param event The action event, or null if not available.
     * @param selectionState The state of the selection and its context.
     * @param config The configuration object for the action, or null if no configuration is needed.
     * @return The processed text.
     */
    open fun processSelection(
        event: AnActionEvent?,
        selectionState: SelectionState,
        config: T?
    ): String { /* ... */ }

    /**
     * Processes the selected text or the default selection using the provided configuration.
     * This method must be implemented by subclasses to perform the actual processing.
     *
     * @param state The state of the selection and its context.
     * @param config The configuration object for the action, or null if no configuration is needed.
     * @return The processed text.
     */
    open fun processSelection(state: SelectionState, config: T?): String {
        throw NotImplementedError()
    }
}
```

This class provides a base implementation for actions that operate on selected text or a default selection in an editor. It handles the selection of text, retrieves the necessary context information (such as the programming language, indentation, and surrounding code elements), and delegates the actual processing of the selection to subclasses.

Subclasses can override the `processSelection` method to implement their specific logic for processing the selected text or the default selection. They can also provide a configuration object by overriding the `getConfig` method.

The `SelectionState` data class encapsulates the state of the selection and its context, including the selected text, selection offsets, entire document text, programming language, indentation, surrounding code elements, and the current project.

The `isLanguageSupported` method can be overridden by subclasses to specify which programming languages are supported by the action.

The `defaultSelection` and `editSelection` methods can be overridden to customize the behavior for determining the default selection range and adjusting the selection range, respectively.

Overall, this class provides a reusable and extensible framework for implementing actions that operate on selected text or a default selection in an editor, while handling the necessary context information and delegating the actual processing logic to subclasses.

