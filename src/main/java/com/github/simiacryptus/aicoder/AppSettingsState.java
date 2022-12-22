package com.github.simiacryptus.aicoder;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.components.PersistentStateComponent;
import com.intellij.openapi.components.State;
import com.intellij.openapi.components.Storage;
import com.intellij.util.xmlb.XmlSerializerUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;

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

  public String apiBase = "https://api.openai.com/v1";
  public String apiKey = "";
  public String model = "text-davinci-003";
  public int maxTokens = 250;
  public double temperature = 0.0;
  public String style = "";

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
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    AppSettingsState that = (AppSettingsState) o;
    return maxTokens == that.maxTokens && Double.compare(that.temperature, temperature) == 0 && Objects.equals(apiBase, that.apiBase) && Objects.equals(apiKey, that.apiKey) && Objects.equals(model, that.model) && Objects.equals(style, that.style);
  }

  @Override
  public int hashCode() {
    return Objects.hash(apiBase, apiKey, model, maxTokens, temperature);
  }
}