# intellij-aicoder

![Build](https://github.com/SimiaCryptus/intellij-aicoder/workflows/Build/badge.svg)
[![Version](https://img.shields.io/jetbrains/plugin/v/20724-ai-coding-assistant.svg)](https://plugins.jetbrains.com/plugin/20724-ai-coding-assistant)
[![Downloads](https://img.shields.io/jetbrains/plugin/d/20724-ai-coding-assistant.svg)](https://plugins.jetbrains.com/plugin/20724-ai-coding-assistant)

<!-- Plugin description -->

# **AI Coding Assistant Plugin for IntelliJ**

The only fully open source plugin for IntelliJ that integrates with OpenAI's GPT-4 API.

* This requires an OpenAI access token, which you can get by signing up for a developer account at https://platform.openai.com/
* No membership fees! API access is paid but is billed by usage, with no base fee. The plugin is free and open source.
* Fully customizable actions in Groovy, including the ability to create your own actions.
* Toolbar UI for quick configuration of temperature/model and display of current token count
* Ability to intercept, edit, and log API requests

**NOTE**: Due to the war of aggression, widespread war crimes including acts of genocide committed by the Russian
Federation against Ukraine and various other countries, this plugin will not function for users in Russia and allied countries.
_We are working on a solution to this problem_, but cannot yet promise a date for the victory.
**_Slava Ukraini!_**

## **Installation & Configuration**

To begin with AI Coding Assistant, you will need an [OpenAI access token](https://platform.openai.com/account/api-keys). 
After obtaining your OpenAI token, input it into the appropriate field in the plugin's settings panel.

## **Usage Overview**

AI Coding Assistant offers a variety of actions, which are tools specifically designed to simplify and speed up your
coding process. These are not passively triggered by your typing but are invoked on command, giving you full control
over when and how to use them. You can access these actions via the context menu within an editor or in the project view.

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
