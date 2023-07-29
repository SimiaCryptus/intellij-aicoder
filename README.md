# intellij-aicoder

![Build](https://github.com/SimiaCryptus/intellij-aicoder/workflows/Build/badge.svg)
[![Version](https://img.shields.io/jetbrains/plugin/v/20724-ai-coding-assistant.svg)](https://plugins.jetbrains.com/plugin/20724-ai-coding-assistant)
[![Downloads](https://img.shields.io/jetbrains/plugin/d/20724-ai-coding-assistant.svg)](https://plugins.jetbrains.com/plugin/20724-ai-coding-assistant)

<!-- Plugin description -->

**IMPORTANT**: Due to the war of aggression, widespread war crimes, and acts of genocide committed by the Russian
Federation against Ukraine and various other countries, this plugin will not function in Russian locales, or for the
Russian language outside a locale whitelist. (This is this reason for many of the 1-star reviews.)

_We are working on a solution to this problem_, but cannot yet promise a date for the Ukrainian victory. 
**_Slava Ukraini!_**

# **AI Coding Assistant Plugin for IntelliJ**

Welcome to the AI Coding Assistant, a robust IntelliJ plugin that empowers developers to streamline their coding
workflow using ChatGPT/GPT4. Our plugin compliments others such as GitHub Copilot or AWS CodeWhisperer -
Where those concentrate on passively enhancing the IDE with autocomplete, our plugin offers actively-invoked actions
that complement and enhance your coding experience. An OpenAI access token is required to use this tool.

In addition to our predefined actions, our latest version introduces:

1. Customizable actions, giving you the power to create your own tools through editable Groovy source files
2. Toolbar UI for quick configuration of temperature/model and display of current token count
3. Ability to intercept and edit individual API requests
4. Ability to log api requests and responses to a file

## **Installation & Configuration**

To begin with AI Coding Assistant, you will need an OpenAI access token. This key grants you access to a diverse range
of features, including language-agnostic text processing, extensive code actions, Markdown support, and much more.

After obtaining your OpenAI token, input it into the appropriate field in the plugin's settings panel. There, you'll
also find options for customizing the actions to suit your coding preferences.

## **Usage Overview**

AI Coding Assistant offers a variety of actions, which are tools specifically designed to simplify and speed up your
coding process. These are not passively triggered by your typing but are invoked on command, giving you full control
over when and how to use them. You can access these actions via the context menu.

## **Action Customization**

In our latest version, we provide the capability to tailor actions to your coding habits and project requirements.
Within the settings UI, you can view, edit, clone, or delete actions, enabling you to fine-tune existing tools or create
new ones from scratch.

These custom actions leverage Groovy scripting, and can be as complex or simple as required, even having access to the
entire IntelliJ API. This powerful feature allows for numerous possibilities, from refining prompts to adding intricate
logic to better support your preferred coding language.

## **Actions Catalogue**

Our plugin includes a broad catalogue of actions, categorized into Plaintext, Code, Markdown, and Developer-Mode
Actions.

### **Plaintext Actions**

These actions offer text processing features for any language, and include tools such
as `Chat Append Text`, `Dictation`, `Redo Last`, `Replace Options`, and `Generate Story`.

### **Code Actions**

Our Code Actions simplify and expedite your coding workflow with actions
like `Add Code Comments`, `Convert To...`, `Edit Code...`, `Describe Code and Prepend Comment`, `Add Doc Comments`,
`Implement Stub`, `Insert Implementation`, `Paste`, `Ask a question about the code`, `Recent Code Edits`,
`Rename Variables`, and `Generate Project`.

### **Markdown Actions**

Our Markdown Actions allow quick and easy additions to your Markdown documents. You can swiftly implement a Markdown
prompt in a specific language with the `Implement As...` action or rapidly add list items to your document using
the `Add List Items` action.

### **Developer-Mode Actions**

The Developer-Mode Actions are available when the plugin is in developer mode. These actions offer debugging and
development tools and experimental features that may not be fully functional. They
include `Open Code Chat`, `Launch Skyenet`, and `Print PSI Tree`.

## **Support**

Should you encounter issues or require further information, please file an issue on our github project.

Understand that this plugin is in active development, and we are constantly working to improve and expand its abilities.

With AI Coding Assistant, coding becomes more efficient, versatile, and customizable, enhancing your productivity and
creative potential. We look forward to seeing what you will create!

<!-- Plugin description end -->
