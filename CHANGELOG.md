<!-- Keep a Changelog guide -> https://keepachangelog.com -->

# intellij-aicoder Changelog

## [Unreleased]

## [1.5.10]

### Added
- New Git-related actions: ChatWithCommitAction, ChatWithCommitDiffAction, and ChatWithWorkingCopyDiffAction
- SimpleCommandAction for executing custom commands
- VCS menu integration with AI Coder options in the VCS log context menu

### Changed
- Updated dependencies: skyenet_version to 1.0.80 and jo-penai to 1.1.0
- Removed kotlinx-coroutines-core dependency
- Added Git4Idea and GitHub plugins to the intellij block
- Refactored CommandAutofixAction for more modular and extensible code
- Updated plugin.xml to include new actions and dependencies

### Improved
- Enhanced Git integration capabilities
- Streamlined build system and dependency management

## [1.4.0]

### Added
- Support for Gemini models
- Support for Mistral Large
### Removed
- Dynamic action support (temporarily) due to performance issues and bugs
### Improved
- Patching logic
- Various fixes
 
## [1.3.0]

### Added
- `DiffChatAction`: A new action for engaging in a chat session to generate and apply code diffs directly within the
  IDE.
- `MultiDiffChatAction`: Allows for collaborative code review and diff generation across multiple files.
- `MultiStepPatchAction`: Automates development tasks by translating user directives into actionable development tasks and code
  modifications.
- Support for models from https://www.perplexity.ai/, https://console.groq.com/, and https://modelslab.com/dashboard/,
  enhancing the plugin's versatility and performance in code generation and analysis.

### Improved

- General performance improvements and bug fixes.
- Enhanced UI for better user experience.

## [1.2.24]

### Improved

- Code console
- Usage tracking per model with cost
- Various fixes

## [1.2.23]

### Improved

- Fixed Kotlin dynamic evaluation
- Replaced all Groovy with Kotlin
- Revised configuration UI
- Various fixes

## [1.2.22]

### Improved

- Code Chat UI
- Pruned experimental/deprecated actions
- Various fixes

## [1.2.21]

### Improved

- Code Chat UI

### Fixed

- Fixed settings for service port

### Removed

- Country sanctioning logic

## [1.2.19]

### Fixed

- Code Chat

## [1.2.18]

### Improved

- Code Chat
- Various fixes
- Added GPT4 Turbo support

## [1.2.14]

### Improved

- Logo and Icon graphics
- Readme
- Updated dependencies

## [1.2.11]

### Improved

- Tasks are now performed in the background, so the UI is no longer blocked
- Added feedback options and improved settings popup UI

## [1.2.10]

### Improved

- Fixed performance issue with token counter

## [1.2.9]

### Improved

- Improved error dialogs for API keys and cancallations

## [1.2.8]

### Improved

- Improved error dialog for groovy errors
- Expanded support for latest IntelliJ versions

## [1.2.7]

### Improved

- Improved error dialog
- Clarity of sanctioning logic via UI message

## [1.2.4]

### Fixed

- Fixed several major issues preventing various actions from working
- Ported additional actions to Groovy, including GenerateStory and GenerateProject
- Improved virtual API protocol / type describers

## [1.2.2]

### Improved

- Fixed bug preventing custom actions from being loaded
- Fixed paste action when no text is selected

## [1.2.2]

### Improved

- Fixed bug preventing user settings from being persisted

## [1.2.0]

### Added

- API edit dialog
- Toolbar widget for token count
- Toolbar widget for model selection
- Editable and dynamically-defined actions

### Improved

- Token count handling

## [1.1.7]

### Added

- "Create File" action

### Improved

- Exception, timeout, and cancellation handling for API calls

## [1.1.3]

### Improved

- GPT4 enabled by default
- Fixed bug to allow selected model (GPT4/3.5Turbo) for all actions

## [1.1.3]

### Improved

- Various bug fixes and performance improvements
- Simplified plugin configuration and model selection

## [1.1.1]

### Added

- "Analogue File" action

### Improved

- Various bug fixes

## [1.1.0]

### Added

- "Code Chat" action

### Removed

- Various legacy actions that are no longer supported - Sorry, too many experiments to maintain! However, if you have a
  favorite that's now missing, let me know and I'll look into it.

### Improved

- All remaining actions have been rewritten to use the Chat API, thus making newer models available

## [1.0.20]

### Added

- Prototype integration with the "Skyenet" AI tool

### Improved

- Various bug fixes

## [1.0.19]

### Improved

- Various bug fixes

## [1.0.18]

### Improved

- API stability and performance
- Various bug fixes
- Max tokens handling

## [1.0.17]

### Added

- Ability to develop entire software projects from scratch (not a joke)
- Support for self-aware artificial intelligence (joke)

### Removed

- Human value and significance (joke)
- Barriers to information warfare (not a joke)

### Fixed

- Human nature (joke)

## [1.0.16]

### Added

- Completed transition to Kotlin
- Improved "max tokens" logic
- Added new proxy api feature
- Added "Generate Project" action (beta)

## [1.0.15]

### Added

- Improved logic for dictation action, particularly silence detection and prefix handling-

## [1.0.14]

### Fixed

- Various issues with "Insert Implementation" action

## [1.0.13]

### Removed

- Access for Russian users due to the invasion of Ukraine

## [1.0.12]

### Added

- Improved dictation action with better stream handling and prefix logic
- Added "add wiki links" action
- Added "to statement list" action
- Added "to factcheck links" action

## [1.0.11]

### Added

- Added actions documentation
- Reviewed and updated action shortcuts and descriptions
- Updated main product description
- Added prototype "dictation" action

## [1.0.10]

### Added

- New functional tests
- Test-generated documentation
- Added "replace options" action
- Added "ask a question" action
- Various formatting fixes

## [1.0.9]

### Added

- Fixed major bug preventing plugin from working
- Converted a majority of the project to Kotlin
- Removed references to OpenAI in UI (this is not an official product)
- Added annotation action
- Added semi-automated testing and documentation generation

## [1.0.8]

### Added

- Added "rename variables" action

## [1.0.7]

### Added

- Added cancel button to API call progress dialog
- Added comment translation action

## [1.0.6]

### Added

- Re-enabled support for PHP after verifying that it works
- Added Rust support for file translation
- Improved retry logic for API calls
- Added action to implement stubs

## [1.0.5]

### Added

- Permanently removed support for PHP

## [1.0.3]

### Added

- Added file conversion for certain languages (Java, JavaScript, Scala, Kotlin, Go, Python)

## [1.0.2]

### Added

- New actions to implement code blocks in markdown
- Various bug fixes and UI polish
- Improved API interception dialogs

## [1.0.0]

### Added

- Fixed bug when changing prefix in UI intercept dialog
- Added generic "edit" feature
- Fixed issues with Javascript and other languages

## [0.1.9]

### Added

- Dev mode now adds a API request preview/testing UI for each request
- Various UI polish
- Refactored code so Actions are independent components

## [0.1.8]

### Added

- Asynchronous operations now include modal progress
- Added "retry last" and generic "append" and "insert" operations
- Added SCSS support

## [0.1.7]

### Added

- All API calls are handled asynchronously - no UI thread blocking!

## [0.1.6]

### Added

- Fix compatibility issues

## [0.1.5]

### Added

- Added dropdown model selection
- Improved comment parsing
- Added functions to create new list items and table rows and columns (markdown)

## [0.1.4]

### Added

- Improved configurability
- Improved actions logic
- Improved menu UI navigability
- Improved exception handling
- Added support for bash, json, and others

## [0.1.3]

### Added

- Expanded language support
- Improved handling of braces and indentation before making document edits
- Improved settings - "Random style" and "Test style" helpers

## [0.1.2]

### Added

- Improved UI polish
- Code cleanups

## [0.1.0]

### Added

- Added filtering using OpenAI moderation API
- Handle error message by telling the user
- Improved support for various languages, especially markdown
- Improved UI polish

## [0.0.4]

### Added

- Added support for various languages
- Added custom edit options with recently used shortcuts
- Added context-aware generation tools

## [0.0.1]

### Added

- Initial implementation
- Initial scaffold created
  from [IntelliJ Platform Plugin Template](https://github.com/JetBrains/intellij-platform-plugin-template)

[Unreleased]: https://github.com/SimiaCryptus/intellij-aicoder/compare/v1.5.10...HEAD
[1.5.10]: https://github.com/SimiaCryptus/intellij-aicoder/compare/v1.4.0...v1.5.10
[1.4.0]: https://github.com/SimiaCryptus/intellij-aicoder/compare/v1.3.0...v1.4.0
[1.3.0]: https://github.com/SimiaCryptus/intellij-aicoder/compare/v1.2.24...v1.3.0
[1.2.24]: https://github.com/SimiaCryptus/intellij-aicoder/compare/v1.2.23...v1.2.24
[1.2.23]: https://github.com/SimiaCryptus/intellij-aicoder/compare/v1.2.22...v1.2.23
[1.2.22]: https://github.com/SimiaCryptus/intellij-aicoder/compare/v1.2.21...v1.2.22
[1.2.21]: https://github.com/SimiaCryptus/intellij-aicoder/compare/v1.2.19...v1.2.21
[1.2.19]: https://github.com/SimiaCryptus/intellij-aicoder/compare/v1.2.18...v1.2.19

[1.2.18]: https://github.com/SimiaCryptus/intellij-aicoder/compare/v1.2.14...v1.2.18

[1.2.14]: https://github.com/SimiaCryptus/intellij-aicoder/compare/v1.2.11...v1.2.14

[1.2.11]: https://github.com/SimiaCryptus/intellij-aicoder/compare/v1.2.10...v1.2.11

[1.2.10]: https://github.com/SimiaCryptus/intellij-aicoder/compare/v1.2.9...v1.2.10

[1.2.9]: https://github.com/SimiaCryptus/intellij-aicoder/compare/v1.2.8...v1.2.9

[1.2.8]: https://github.com/SimiaCryptus/intellij-aicoder/compare/v1.2.7...v1.2.8

[1.2.7]: https://github.com/SimiaCryptus/intellij-aicoder/compare/v1.2.4...v1.2.7

[1.2.4]: https://github.com/SimiaCryptus/intellij-aicoder/compare/v1.2.2...v1.2.4

[1.2.2]: https://github.com/SimiaCryptus/intellij-aicoder/compare/v1.2.1...v1.2.2

[1.2.2]: https://github.com/SimiaCryptus/intellij-aicoder/compare/v1.2.0...v1.2.1

[1.2.0]: https://github.com/SimiaCryptus/intellij-aicoder/compare/v1.1.4...v1.2.0

[1.1.7]: https://github.com/SimiaCryptus/intellij-aicoder/compare/v1.1.3...v1.1.4

[1.1.3]: https://github.com/SimiaCryptus/intellij-aicoder/compare/v1.1.2...v1.1.3

[1.1.3]: https://github.com/SimiaCryptus/intellij-aicoder/compare/v1.1.1...v1.1.2

[1.1.1]: https://github.com/SimiaCryptus/intellij-aicoder/compare/v1.1.0...v1.1.1

[1.1.0]: https://github.com/SimiaCryptus/intellij-aicoder/compare/v1.0.20...v1.1.0

[1.0.20]: https://github.com/SimiaCryptus/intellij-aicoder/compare/v1.0.19...v1.0.20

[1.0.19]: https://github.com/SimiaCryptus/intellij-aicoder/compare/v1.0.18...v1.0.19

[1.0.18]: https://github.com/SimiaCryptus/intellij-aicoder/compare/v1.0.17...v1.0.18

[1.0.17]: https://github.com/SimiaCryptus/intellij-aicoder/compare/v1.0.16...v1.0.17

[1.0.16]: https://github.com/SimiaCryptus/intellij-aicoder/compare/v1.0.15...v1.0.16

[1.0.15]: https://github.com/SimiaCryptus/intellij-aicoder/compare/v1.0.14...v1.0.15

[1.0.14]: https://github.com/SimiaCryptus/intellij-aicoder/compare/v1.0.13...v1.0.14

[1.0.13]: https://github.com/SimiaCryptus/intellij-aicoder/compare/v1.0.12...v1.0.13

[1.0.12]: https://github.com/SimiaCryptus/intellij-aicoder/compare/v1.0.11...v1.0.12

[1.0.11]: https://github.com/SimiaCryptus/intellij-aicoder/compare/v1.0.10...v1.0.11

[1.0.10]: https://github.com/SimiaCryptus/intellij-aicoder/compare/v1.0.9...v1.0.10

[1.0.9]: https://github.com/SimiaCryptus/intellij-aicoder/compare/v1.0.8...v1.0.9

[1.0.8]: https://github.com/SimiaCryptus/intellij-aicoder/compare/v1.0.7...v1.0.8

[1.0.7]: https://github.com/SimiaCryptus/intellij-aicoder/compare/v1.0.6...v1.0.7

[1.0.6]: https://github.com/SimiaCryptus/intellij-aicoder/compare/v1.0.5...v1.0.6

[1.0.5]: https://github.com/SimiaCryptus/intellij-aicoder/compare/v1.0.3...v1.0.5

[1.0.3]: https://github.com/SimiaCryptus/intellij-aicoder/compare/v1.0.2...v1.0.3

[1.0.2]: https://github.com/SimiaCryptus/intellij-aicoder/compare/v1.0.0...v1.0.2

[1.0.0]: https://github.com/SimiaCryptus/intellij-aicoder/compare/v0.1.9...v1.0.0

[0.1.9]: https://github.com/SimiaCryptus/intellij-aicoder/compare/v0.1.8...v0.1.9

[0.1.8]: https://github.com/SimiaCryptus/intellij-aicoder/compare/v0.1.7...v0.1.8

[0.1.7]: https://github.com/SimiaCryptus/intellij-aicoder/compare/v0.1.6...v0.1.7

[0.1.6]: https://github.com/SimiaCryptus/intellij-aicoder/compare/v0.1.5...v0.1.6

[0.1.5]: https://github.com/SimiaCryptus/intellij-aicoder/compare/v0.1.4...v0.1.5

[0.1.4]: https://github.com/SimiaCryptus/intellij-aicoder/compare/v0.1.3...v0.1.4

[0.1.3]: https://github.com/SimiaCryptus/intellij-aicoder/compare/v0.1.2...v0.1.3

[0.1.2]: https://github.com/SimiaCryptus/intellij-aicoder/compare/v0.1.0...v0.1.2

[0.1.0]: https://github.com/SimiaCryptus/intellij-aicoder/compare/v0.0.4...v0.1.0

[0.0.4]: https://github.com/SimiaCryptus/intellij-aicoder/compare/v0.0.1...v0.0.4

[0.0.1]: https://github.com/SimiaCryptus/intellij-aicoder/commits/v0.0.1