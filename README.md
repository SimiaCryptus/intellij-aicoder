# intellij-aicoder

![Build](https://github.com/SimiaCryptus/intellij-aicoder/workflows/Build/badge.svg)
[![Version](https://img.shields.io/jetbrains/plugin/v/20724-ai-coding-assistant.svg)](https://plugins.jetbrains.com/plugin/20724-ai-coding-assistant)
[![Downloads](https://img.shields.io/jetbrains/plugin/d/20724-ai-coding-assistant.svg)](https://plugins.jetbrains.com/plugin/20724-ai-coding-assistant)

<!-- Plugin description -->

Behold, a revolutionary new plugin for code editors that useth the power of artificial intelligence to make coding
swifter and simpler. 'Tis compatible with popular autocomplete-based AI plugins such as GitHub Copilot and AWS
CodeWhisperer, and is explicitly invoked through the context menu.

To use AI Coding Assistant, thou must possess an OpenAI access token. With this token, thou canst access a variety of
features, including generics and system-wide functions, Markdown support, and general code editing.

# Plaintext Actions

Plaintext actions provide text processing features for any language. The following actions are available for plaintext
in the AI Coder plugin:

| Text             | Description                                                                                                                                                                                                                                                                                                                                                                                                                                                                              |
|------------------|------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| Chat Append Text | The `Chat Append Text` action allows you to quickly append text to the end of the current selection.                                                                                                                                                                                                                                                                                                                                                                                     |
| Dictation        | The `Dictation` action allows you to speak into the microphone and have the text automatically inserted into the document. This uses OpenAI's Whisper API.                                                                                                                                                                                                                                                                                                                               |
| Redo Last        | The `Redo Last` action allows you to quickly redo the last action you performed. This is useful for getting variations from the generative AI. For example, if you generate something and want to see an alternate, you can use the `Redo Last` action to quickly retry without having to start from scratch.                                                                                                                                                                            |
| Replace Options  | The `Replace Options` action allows you to quickly replace selected text with one of a set of suggested options. This also captures some preceding and following context, but uses a different prompting method to suggest the options. For example, if you type ï¿½I had a dayï¿½ and place the cursor after 'a' and then use the `Replace Options` action, the plugin will **_provide an interactive selection UI_** to suggest words like ï¿½greatï¿½, ï¿½badï¿½, or ï¿½deliciousï¿½. |

# Code Actions

The following actions are available for coding in the AI Coder plugin:

| Text                              | Description                                                                                                                                                                                                                                                                                                  |
|-----------------------------------|--------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| Add Code Comments                 | The `Add Code Comments` action allows you to quickly add comments to selected code. It will generally add a comment for each line with a description. This is useful for quickly documenting your code and making it easier to understand.                                                                   |
| Convert To...                     | The `Convert To...` action allows you to quickly convert a file to a specific language. This is useful for quickly converting larger amounts of code between languages. For example, if you have a Java file and want to convert it to Kotlin, you can use the `Convert To...` action to quickly convert it. |
| Edit Code...                      | The `Edit Code...` action allows you to quickly edit your code based on a user-supplied directive. This is useful for quickly performing ad-hoc code transformations. For example, you can use this action to add logging statements or to quickly refactor your code.                                       |
| Describe Code and Prepend Comment | The `Describe Code and Prepend Comment` action allows you to quickly add a comment to your code that describes the current selection. This is useful for quickly documenting your code and making it easier to understand.                                                                                   |
| Add Doc Comments                  | The `Add Doc Comments` action allows you to quickly add comments to your code that are formatted for documentation. In contrast to existing code tools that can generate doc comments, this action is based on the full code and can generate more accurate comments.                                        |
| Implement Stub                    | The `Implement Stub` action allows you to quickly implement a stub of a method. This is useful for quickly implementing a new method.                                                                                                                                                                        |
| Insert Implementation             | The `Insert Implementation` action allows you to quickly insert the implementation of a comment. It attempts to process the code context and provide details about this generation context within the API call.                                                                                              |
| Paste                             | The `Paste` action allows you to quickly paste code into your project. This text will be translated into the current language. This is useful for quickly pasting code into your project.                                                                                                                    |
| Ask a question about the code     | The `Ask a question about the code` action allows you to quickly ask a question about the code. This is useful for quickly getting help with understanding the code. The question and response are prepended to the selected code.                                                                           |
| Recent Code Edits                 | The `Recent Code Edits` group allows you to quickly access your most recent code edits. This is useful for quickly accessing your most recent edits.                                                                                                                                                         |
| Rename Variables                  | The `Rename Variables` action allows you to quickly rename variables in your code. An interactive dialog is provided to select any/all/none of the rename suggestions. This is useful for quickly refactoring your code and making it easier to understand.                                                  |

# Markdown Actions

Markdown Actions allow you to quickly and easily add list items, table columns, and more to your Markdown documents.

| Text            | Description                                                                                                                                                                                                                                                                                                                          |
|-----------------|--------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| Implement As... | The `Implement As...` action allows you to quickly implement a markdown prompt in a specific language. This is useful for quickly implementing code in a specific language. For example, if select "find the largest file in the current directory" you can use the `Implement As...` action to quickly implement this idea in Bash. |
| Add List Items  | The `Add List Items` action allows you to quickly add list items to your Markdown document. It supports both ordered and unordered lists, and nested lists.                                                                                                                                                                          |

# Developer-Mode Actions

Some actions are only available when the plugin is running in developer mode. These may be useful for debugging or
development, but also contain experimental features that may not be fully functional.

| Text             | Description                                                                                                                                                                                                                                         |
|------------------|-----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| Open Code Chat   | The `Open Code Chat` action opens a chat window in a browser, allowing the user to discuss the code with ChatGPT.                                                                                                                                   |
| Generate Project | The `Generate Project` action allows you to quickly generate a new software project from a description.                                                                                                                                             |
| Write a Story    | The `Generate Story` action allows you to quickly generate a new story!                                                                                                                                                                             |
| Launch Skyenet   | The `Launch Skyenet` action opens the Skyenet interactive auto-scritping interface.                                                                                                                                                                 |
| Print PSI Tree   | The `Print PSI Tree` action allows you to print the PSI tree for the current file. This is useful for debugging and understanding the PSI tree for a file. This is a developer tool and is only visible if the `Developer Mode` setting is enabled. |

<!-- Plugin description end -->

## Installation

- Using IDE built-in plugin system:

  <kbd>Settings/Preferences</kbd> > <kbd>Plugins</kbd> > <kbd>Marketplace</kbd> > <kbd>Search for "
  intellij-aicoder"</kbd> >
  <kbd>Install Plugin</kbd>

- Manually:

  Download the [latest release](https://github.com/SimiaCryptus/intellij-aicoder/releases/latest) and install it
  manually using
  <kbd>Settings/Preferences</kbd> > <kbd>Plugins</kbd> > <kbd>âš™ï¸</kbd> > <kbd>Install plugin from disk...</kbd>

## Usage

After installation, edit the settings and add your API key. If desired, customize your style!
![aicoder_settings.png](docs/aicoder_settings.png)

Use the context menu to access features. In this example, we use the "Insert Implementation" tool.
_Note: After inserting, the code needs reformatting and some manual cleanup (e.g. correcting additional ending braces)_
![aicoder_implement.png](docs/aicoder_implement.png)

After reviewing the code, we can easily add documentation.
![aicoder_adddocs.png](docs/aicoder_adddocs.png)

![aicoder_final.png](docs/aicoder_final.png)

We can also iterate and use the AI to refactor the code using custom edits:
![aicoder_edit.png](docs/aicoder_edit.png)

![aicoder_editresult.png](docs/aicoder_editresult.png)

---
Plugin based on the [IntelliJ Platform Plugin Template][template].

[template]: https://github.com/JetBrains/intellij-platform-plugin-template

