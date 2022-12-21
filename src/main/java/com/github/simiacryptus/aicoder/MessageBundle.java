package com.github.simiacryptus.aicoder;

import com.intellij.DynamicBundle;
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.PropertyKey;

import java.util.function.Supplier;

public class MessageBundle extends DynamicBundle {

  @NonNls
  private static final String BUNDLE = "messages.AICoder";


  public static MessageBundle INSTANCE = new MessageBundle();

  private MessageBundle() {
    super(BUNDLE);
  }

  @SuppressWarnings("SpreadOperator")
  public String message(@PropertyKey(resourceBundle = BUNDLE) String key, Object... params) {
    return getMessage(key, params);
  }

  @SuppressWarnings({"SpreadOperator", "unused"})
  public @NotNull Supplier<@Nls String> messagePointer(@PropertyKey(resourceBundle = BUNDLE) String key, Object... params) {
    return getLazyMessage(key, params);
  }
}