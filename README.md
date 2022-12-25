# intellij-aicoder

![Build](https://github.com/SimiaCryptus/intellij-aicoder/workflows/Build/badge.svg)
[![Version](https://img.shields.io/jetbrains/plugin/v/20724-ai-coding-assistant.svg)](https://plugins.jetbrains.com/plugin/20724-ai-coding-assistant)
[![Downloads](https://img.shields.io/jetbrains/plugin/d/20724-ai-coding-assistant.svg)](https://plugins.jetbrains.com/plugin/20724-ai-coding-assistant)

<!-- Plugin description -->
Ahoy, mateys! This plugin be a powerful tool fer yer coding needs. It be adding some editor context menu options that be providing text processing functions powered by OpenAI's GPT models.

It be enabling automated coding in Java, Scala, Python, Bash, SQL, and more!

Features:
- Automatic code commenting, JavaDoc (et al) creation
- Code generation based on plain-text requirements, with context awareness
- Directive-based code editing
- Custom styles (e.g. write like a pirate!)

To use, simply access the context menu within any editor window and view the options under "AI Coder".

Ye need yer own [OpenAPI access token](https://beta.openai.com/) fer this plugin. This be intended as a development tool fer developers who already have an OpenAI access token; it be not itself to be considered an end-product. This plugin be provided free of charge and without any guarantee. So what be ye waitin' fer? Get yer hands on this plugin now!<!-- Plugin description end -->
<!-- Plugin description end -->

## Installation

- Using IDE built-in plugin system:
  
  <kbd>Settings/Preferences</kbd> > <kbd>Plugins</kbd> > <kbd>Marketplace</kbd> > <kbd>Search for "intellij-aicoder"</kbd> >
  <kbd>Install Plugin</kbd>
  
- Manually:

  Download the [latest release](https://github.com/SimiaCryptus/intellij-aicoder/releases/latest) and install it manually using
  <kbd>Settings/Preferences</kbd> > <kbd>Plugins</kbd> > <kbd>⚙️</kbd> > <kbd>Install plugin from disk...</kbd>

## Usage

After installation, edit the settings and add your API key. If desired, customize your style!
![aicoder_settings.png](docs/aicoder_settings.png)

Use the context menu to access features. In this example, we use the "Insert Implementation" tool. _Note: After inserting, the code needs reformatting and some manual cleanup (e.g. correcting additional ending braces)_
![aicoder_implement.png](docs/aicoder_implement.png)

After reviewing the code, we can easily add documentation.
![aicoder_adddocs.png](docs/aicoder_adddocs.png)

![aicoder_final.png](docs/aicoder_final.png)

We can also iterate and use the AI to refactor the code using custom edits:
![aicoder_edit.png](docs/aicoder_edit.png)

![aicoder_editresult.png](docs/aicoder_editresult.png)

---

## Template ToDo list
- [x] Create a new [IntelliJ Platform Plugin Template][template] project.
- [x] Get familiar with the [template documentation][template].
- [x] Verify the [pluginGroup](./gradle.properties), [plugin ID](./src/main/resources/META-INF/plugin.xml) and [sources package](./src/main/kotlin).
- [x] Review the [Legal Agreements](https://plugins.jetbrains.com/docs/marketplace/legal-agreements.html?from=IJPluginTemplate).
- [x] [Publish a plugin manually](https://plugins.jetbrains.com/docs/intellij/publishing-plugin.html?from=IJPluginTemplate) for the first time.
- [x] Set the Plugin ID in the above README badges.
- [ ] Set the [Plugin Signing](https://plugins.jetbrains.com/docs/intellij/plugin-signing.html?from=IJPluginTemplate) related [secrets](https://github.com/JetBrains/intellij-platform-plugin-template#environment-variables).
- [ ] Set the [Deployment Token](https://plugins.jetbrains.com/docs/marketplace/plugin-upload.html?from=IJPluginTemplate).
- [ ] Click the <kbd>Watch</kbd> button on the top of the [IntelliJ Platform Plugin Template][template] to be notified about releases containing new features and fixes.

---
Plugin based on the [IntelliJ Platform Plugin Template][template].

[template]: https://github.com/JetBrains/intellij-platform-plugin-template
