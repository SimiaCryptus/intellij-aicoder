Sure, here's a draft README.md file explaining the configuration options in the `AppSettingsState` class:

# AI Coder Configuration

The `AppSettingsState` class contains configuration settings for the AI Coder plugin. Here's an explanation of each
setting:

## `temperature`

- **Type:** `Double`
- **Default:** `0.1`
- This controls the randomness of the AI model's output. Higher values (up to 1.0) make the output more random and
  creative, while lower values make it more focused and deterministic.

## `modelName`

- **Type:** `String`
- **Default:** `"gpt-3.5-turbo"`
- The name of the AI model to use. The default is the GPT-3.5 Turbo model.

## `listeningPort`

- **Type:** `Int`
- **Default:** `8081`
- The local port to listen on for API requests.

## `listeningEndpoint`

- **Type:** `String`
- **Default:** `"localhost"`
- The local hostname or IP address to listen on for API requests.

## `humanLanguage`

- **Type:** `String`
- **Default:** `"English"`
- The language to use for user prompts and AI responses.

## `apiThreads`

- **Type:** `Int`
- **Default:** `4`
- The number of threads to use for processing API requests.

## `apiBase`

- **Type:** `Map<String, String>?`
- **Default:** `mapOf("OpenAI" to "https://api.openai.com/v1")`
- A map of provider names to base API URLs.

## `apiKey`

- **Type:** `Map<String, String>?`
- **Default:** `mapOf("OpenAI" to "")`
- A map of provider names to API keys.

## `modalTasks`

- **Type:** `Boolean`
- **Default:** `false`
- Whether to use modal tasks (blocking UI) for AI requests.

## `suppressErrors`

- **Type:** `Boolean`
- **Default:** `false`
- Whether to suppress error messages from the AI API.

## `apiLog`

- **Type:** `Boolean`
- **Default:** `false`
- Whether to log API requests and responses.

## `devActions`

- **Type:** `Boolean`
- **Default:** `false`
- Whether to enable developer actions (e.g., editing requests).

## `editRequests`

- **Type:** `Boolean`
- **Default:** `false`
- Whether to allow editing of AI requests before sending them.

The `AppSettingsState` class also includes an `editorActions` and `fileActions` registry for configuring custom actions,
as well as a `recentCommands` map for storing recent AI commands.