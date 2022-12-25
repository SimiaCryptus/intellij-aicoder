package com.github.simiacryptus.aicoder.config;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.components.PersistentStateComponent;
import com.intellij.openapi.components.State;
import com.intellij.openapi.components.Storage;
import com.intellij.util.xmlb.XmlSerializerUtil;
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
    public @NotNull String model = "text-davinci-003";
    public int maxTokens = 1000;
    public double temperature = 0.1;
    public @NotNull String style = "";
    public @NotNull Map<String, Integer> instructionHistory = new HashMap<>();
    public int historyLimit = 10;
    public @NotNull String humanLanguage = "English";
    public int maxPrompt = 5000;

    public AppSettingsState() {
    }

    public static AppSettingsState getInstance() {
        return ApplicationManager.getApplication().getService(AppSettingsState.class);
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
        AppSettingsState that = (AppSettingsState) o;
        if (maxTokens != that.maxTokens) return false;
        if (maxPrompt != that.maxPrompt) return false;
        if (Double.compare(that.temperature, temperature) != 0) return false;
        if (!Objects.equals(humanLanguage, that.humanLanguage)) return false;
        if (!Objects.equals(apiBase, that.apiBase)) return false;
        if (!Objects.equals(apiKey, that.apiKey)) return false;
        if (!Objects.equals(model, that.model)) return false;
        return Objects.equals(style, that.style);
    }

    @Override
    public int hashCode() {
        return Objects.hash(apiBase, apiKey, model, maxTokens, temperature);
    }

    public void addInstructionToHistory(String instruction) {
        synchronized (instructionHistory) {
            instructionHistory.put(instruction, instructionHistory.getOrDefault(instruction, 0) + 1);
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
        if (instructionHistory.size() > historyLimit) {
            List<String> retain = instructionHistory.entrySet().stream()
                    .sorted(Map.Entry.<String, Integer>comparingByValue().reversed())
                    .limit(historyLimit).map(Map.Entry::getKey).collect(Collectors.toList());
            HashSet<String> toRemove = new HashSet<>(instructionHistory.keySet());
            toRemove.removeAll(retain);
            toRemove.forEach(instructionHistory::remove);
        }
    }

    public void removeInstructionFromHistory(String instruction) {
        instructionHistory.remove(instruction);
    }

    public @NotNull Set<String> getInstructionHistory() {
        return instructionHistory.keySet();
    }
}