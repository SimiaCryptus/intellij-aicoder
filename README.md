# intellij-aicoder

![Build](https://github.com/SimiaCryptus/intellij-aicoder/workflows/Build/badge.svg)
[![Version](https://img.shields.io/jetbrains/plugin/v/20724-ai-coding-assistant.svg)](https://plugins.jetbrains.com/plugin/20724-ai-coding-assistant)
[![Downloads](https://img.shields.io/jetbrains/plugin/d/20724-ai-coding-assistant.svg)](https://plugins.jetbrains.com/plugin/20724-ai-coding-assistant)

<!-- Plugin description -->

Behold, a revolutionary new plugin for code editors that useth the power of artificial intelligence to make coding swifter and simpler. 'Tis compatible with popular autocomplete-based AI plugins such as GitHub Copilot and AWS CodeWhisperer, and can be explicitly invoked through the editor's context menu.

To use AI Coding Assistant, thou must possess an OpenAI access token. With this token, thou canst access a variety of features, including generics and system-wide functions, Markdown support, and general code editing.

## Generics & System-wide

AI Coding Assistant provides a rad selection of generics and system-wide functions, like Append, Edit, and Insert. Plus, you can totally undo any operation. You can also tweak the "language" and "style" of your code.

## Markdown

And the Lord said, "Let there be Markdown, and it shall allow for the quick creation of list items, table rows and columns, and code snippets."

## General Code

```
Generics and Markdown
AI Coding Assistant too
A variety of features

Documentation comments
Comment as code, reword too
Paste with translation

Java, C++, C#, JS
Python, Ruby, Go
Perl, R, Swift, SQL

HTML, CSS, TypeScript
Kotlin, Dart, Rust, Scala
Assembly, Ada, Basic

COBOL, Clojure, Delphi
Erlang, Elixir, FORTRAN
F#, Groovy, Haskell, Julia

Lisp, Logo, MATLAB
OCaml, Pascal, Prolog
Racket, Smalltalk, Tcl
Visual Basic, and more!
```

## Dev Features

Finally, AI Coding Assistant also offers a variety of developer-focused features, including the ability to keep tabs on API usage, tweak and adjust API calls, and access some real nifty experimental and utility functions.

With AI Coding Assistant, you can get your code edited in a jiffy with the help of artificial intelligence. Give it a whirl today and see how it can revolutionize your coding experience!

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
