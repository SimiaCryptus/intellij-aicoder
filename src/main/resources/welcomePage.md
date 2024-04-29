# AI Coding Assistant 1.5 - Your AI-Powered Coding Companion for IntelliJ IDEA

![Build](https://github.com/SimiaCryptus/intellij-aicoder/workflows/Build/badge.svg)
[![Version](https://img.shields.io/jetbrains/plugin/v/20724-ai-coding-assistant.svg)](https://plugins.jetbrains.com/plugin/20724-ai-coding-assistant)
[![Downloads](https://img.shields.io/jetbrains/plugin/d/20724-ai-coding-assistant.svg)](https://plugins.jetbrains.com/plugin/20724-ai-coding-assistant)

---

## Getting Started 🚀

Congratulations on installing the AI Coding Assistant! Let's get you set up and coding smarter in no time.

---

### Step 1: Obtain API Keys

To unlock the full potential of AI Coding Assistant, you'll need API keys from one or more supported Language Model APIs.
Here are links to popular providers where you can register and obtain your keys:

- [OpenAI](https://platform.openai.com/)
- [Anthropic](https://www.anthropic.com/api)
- [AWS Bedrock](https://aws.amazon.com/bedrock/) - AWS Credentials are different from API keys. Instead, it is json that
  allows you to specify the profile and region. If no overrides are needed, just use `{}` to enable default AWS
  credentials.
- [Google](https://ai.google.dev/tutorials/setup) - **PSST!** _Free For Now!_
- [Groq](https://console.groq.com/) - **PSST!** _Free For Now!_
- [Perplexity AI](https://www.perplexity.ai/)
- [ModelsLab](https://modelslab.com/dashboard/)

---

### Step 2: Configure Your Plugin 🛠️

Once you have your API keys, open IntelliJ IDEA and navigate to **Settings > Tools > AI Coder**. Enter your
keys in the configuration panel under the appropriate API provider fields.

---

### Step 3: Explore AI-Powered Actions 🧠

Enhance your development workflow with AI Coding Assistant's diverse range of actions. Each action is designed to
seamlessly integrate into your IntelliJ IDEA environment, boosting your productivity and coding efficiency.

Here are some powerful actions you can start using immediately:

- **Patch Chat**: Open a chat session with file-patching features to discuss and apply changes directly.
- **Code Chat**: Engage in a basic chat session about the selected code to gain insights and suggestions.
- **Chat with AI**: Start a general chat session with AI to discuss code or get assistance on various topics.
- **Task Runner**: Execute complex sequences of development tasks based on your directives.
- **Multi-Step Patch**: Apply a series of patches across multiple files, enhancing your project incrementally.
- **Generate Documentation**: Automatically create comprehensive documentation for your project files.
- **Generate Related File**: Create new files related to your current work, suggested by AI based on context.
- **Create File from Description**: Generate a new file entirely from a natural language description you provide.
- **Web Dev**: Quickly scaffold files necessary for a new web application, all suggested by AI.

These actions are accessible via the context menu within your editor or project view, making them easy to use as you
develop.