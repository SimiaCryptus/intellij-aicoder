package com.github.simiacryptus.aicoder.config;

import com.github.simiacryptus.aicoder.openai.CompletionRequest;
import com.github.simiacryptus.aicoder.openai.EditRequest;
import com.github.simiacryptus.aicoder.openai.translate.TranslationRequest;
import com.github.simiacryptus.aicoder.openai.translate.TranslationRequestTemplate;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.components.PersistentStateComponent;
import com.intellij.openapi.components.State;
import com.intellij.openapi.components.Storage;
import com.intellij.util.xmlb.XmlSerializerUtil;
import com.jetbrains.rd.util.LogLevel;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Supports storing the application settings in a persistent way.
 * The {@link State} and {@link Storage} annotations define the name of the data and the file name where
 * these persistent application settings are stored.
 */
@State(
        name = "org.intellij.sdk.settings.AppSettingsState",
        storages = @Storage("SdkSettingsPlugin.xml")
)
public class AppSettingsState implements PersistentStateComponent<AppSettingsState> {

    public @NotNull String apiBase = "https://api.openai.com/v1";
    public @NotNull String apiKey = "";
    public @NotNull String model_completion = "text-davinci-003";
    public @NotNull String model_edit = "text-davinci-edit-001";
    public int maxTokens = 1000;
    public double temperature = 0.1;
    public @NotNull String style = "";
    @SuppressWarnings("unused")
    public int tokenCounter = 0;
    private final @NotNull Map<String, Integer> mostUsedHistory = new HashMap<>();
    private final @NotNull List<String> mostRecentHistory = new ArrayList<>();
    public int historyLimit = 10;
    public @NotNull String humanLanguage = "English";
    public int maxPrompt = 5000;
    public @NotNull TranslationRequestTemplate translationRequestTemplate = TranslationRequestTemplate.XML;
    public @NotNull LogLevel apiLogLevel = LogLevel.Debug;
    public boolean devActions = false;

    public AppSettingsState() {
    }

    public static AppSettingsState getInstance() {
        return ApplicationManager.getApplication().getService(AppSettingsState.class);
    }

    public TranslationRequest createTranslationRequest() {
        return translationRequestTemplate.get(this);
    }

    public @NotNull CompletionRequest createCompletionRequest() {
        return new CompletionRequest(this);
    }

    public @NotNull EditRequest createEditRequest() {
        return new EditRequest(this);
    }

    @Nullable
    @Override
    public AppSettingsState getState() {
        return this;
    }

    @Override
    public void loadState(@NotNull AppSettingsState state) {
        XmlSerializerUtil.copyBean(state, this);
    }

    @Override
    public boolean equals(@Nullable Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        @NotNull AppSettingsState that = (AppSettingsState) o;
        if (maxTokens != that.maxTokens) return false;
        if (maxPrompt != that.maxPrompt) return false;
        if (Double.compare(that.temperature, temperature) != 0) return false;
        if (!Objects.equals(humanLanguage, that.humanLanguage)) return false;
        if (!Objects.equals(apiBase, that.apiBase)) return false;
        if (!Objects.equals(apiKey, that.apiKey)) return false;
        if (!Objects.equals(model_completion, that.model_completion)) return false;
        if (!Objects.equals(model_edit, that.model_edit)) return false;
        if (!Objects.equals(translationRequestTemplate, that.translationRequestTemplate)) return false;
        if (!Objects.equals(apiLogLevel, that.apiLogLevel)) return false;
        if (!Objects.equals(devActions, that.devActions)) return false;
        return Objects.equals(style, that.style);
    }

    @Override
    public int hashCode() {
        return Objects.hash(apiBase, apiKey, model_completion, model_edit, maxTokens, temperature, translationRequestTemplate, apiLogLevel, devActions, style);
    }

    public void addInstructionToHistory(@NotNull CharSequence instruction) {
        synchronized (mostRecentHistory) {
            mostRecentHistory.add(instruction.toString());
            while(mostRecentHistory.size() > historyLimit) {
                mostRecentHistory.remove(0);
            }
        }
        synchronized (mostUsedHistory) {
            mostUsedHistory.put(instruction.toString(), mostUsedHistory.getOrDefault(instruction, 0) + 1);
        }

        // If the instruction history is bigger than the history limit,
        // We'll make a set of strings to retain,
        // We'll sort the instruction history by its value,
        // And limit it to the history limit,
        // Then we'll map the entry key and collect it in a set,
        // Then we'll make a new hash set of the instruction history keys,
        // And remove all the ones we want to retain,
        // Then we'll remove all the ones we don't want to keep,
        // And that's how we'll make sure the instruction history is neat!
        if (mostUsedHistory.size() > historyLimit) {
            @NotNull List<CharSequence> retain = mostUsedHistory.entrySet().stream()
                    .sorted(Map.Entry.<String, Integer>comparingByValue().reversed())
                    .limit(historyLimit).map(Map.Entry::getKey).collect(Collectors.toList());
            @NotNull HashSet<CharSequence> toRemove = new HashSet<>(mostUsedHistory.keySet());
            toRemove.removeAll(retain);
            toRemove.removeAll(mostRecentHistory);
            toRemove.forEach(mostUsedHistory::remove);
        }
    }

    public @NotNull Set<String> getEditHistory() {
        return mostUsedHistory.keySet();
    }
}