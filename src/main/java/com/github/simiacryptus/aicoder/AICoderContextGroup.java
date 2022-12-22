package com.github.simiacryptus.aicoder;

import com.intellij.openapi.actionSystem.ActionGroup;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.ide.CopyPasteManager;
import com.intellij.openapi.vfs.VirtualFile;
import org.jetbrains.annotations.NotNull;

import java.awt.datatransfer.DataFlavor;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class AICoderContextGroup extends ActionGroup {

  @Override
  public AnAction @NotNull [] getChildren(AnActionEvent e) {
    // Get the VirtualFile associated with the current action event
    VirtualFile file = e.getRequiredData(CommonDataKeys.VIRTUAL_FILE);
    // Create an ArrayList to store the generated AnAction objects
    ArrayList<AnAction> children = new ArrayList<>();
    // Get the file extension of the VirtualFile
    String extension = file.getExtension().toLowerCase();
    // Set the human language to English
    String humanLanguage = "English";
    // Switch on the file extension
    switch (extension) {
      // If the file extension is Java
      case "java":
        javaActions(children, extension);
        // If the file extension is sh or py
      case "scala":
      case "groovy":
      case "sh":
      case "py":
        standardCodeActions(children, extension, humanLanguage);
        // Break out of the switch statement
        break;
      case "gradle":
        standardCodeActions(children, "groovy", humanLanguage);
        break;
      case "md":
        standardCodeActions(children, "markdown", humanLanguage);
        break;
      // Default case
      default:
        break;
    }

    // If the CopyPasteManager has DataFlavors available
    if(CopyPasteManager.getInstance().areDataFlavorsAvailable(DataFlavor.stringFlavor)) {
      // Add a TextReplacementAction to the ArrayList
      children.add(TextReplacementAction.create("Paste", "Paste", Icons.default_icon, (event, string) -> {
        // Set the instruction to "Translate this input into " + extension
        String instruction = "Translate this input into " + extension;
        // Set the input attributes to "language: autodetect"
        Map<String, String> inputAttr = Map.of("language", "autodetect");
        // Set the output attributes to "language: " + extension
        Map<String, String> outputAttr = Map.of("language", extension);
        // Get the contents of the clipboard
        String pasteContents = CopyPasteManager.getInstance().getContents(DataFlavor.stringFlavor).toString();
        // Return the result of the OpenAIAPI xmlFN function
        return OpenAIAPI.INSTANCE.xmlFN("source", "translated", instruction, inputAttr, outputAttr).apply(pasteContents);
      }));
    }

    // Return the ArrayList as an array of AnAction objects
    return children.toArray(AnAction[]::new);
  }

  private void javaActions(ArrayList<AnAction> children, String extension) {
    // Add a TextReplacementAction to the ArrayList
    children.add(TextReplacementAction.create("Add JavaDoc Comments", "Add JavaDoc Comments", Icons.default_icon, (event, string) -> {
      // Set the instruction to "Rewrite to include detailed JavaDocs"
      String instruction = "Rewrite to include detailed JavaDocs";
      if(!AppSettingsState.getInstance().style.isEmpty()) instruction = String.format("%s (%s)", instruction, AppSettingsState.getInstance().style);
      // Set the input attributes to "type: uncommented"
      Map<String, String> inputAttr = new HashMap<>(Map.of("type", "uncommented"));
      // Set the output attributes to "type: commented"
      Map<String, String> outputAttr = new HashMap<>(Map.of("type", "commented"));
      if(!AppSettingsState.getInstance().style.isEmpty()) outputAttr.put("style", AppSettingsState.getInstance().style);
      // Return the result of the OpenAIAPI xmlFN function
      return OpenAIAPI.INSTANCE.xmlFN(extension, extension, instruction, inputAttr, outputAttr).apply(string);
    }));
  }

  private void standardCodeActions(ArrayList<AnAction> children, String extension, String humanLanguage) {
    // Add a TextReplacementAction to the ArrayList
    children.add(TextReplacementAction.create("Add Code Comments", "Add Code Comments", Icons.default_icon, (event, string) -> {
      // Set the instruction to "Rewrite to include detailed code comments at the end of every line"
      String instruction = "Rewrite to include detailed code comments at the end of every line";
      if(!AppSettingsState.getInstance().style.isEmpty()) instruction = String.format("%s (%s)", instruction, AppSettingsState.getInstance().style);
      // Set the input attributes to "type: uncommented"
      Map<String, String> inputAttr = Map.of("type", "uncommented");
      // Set the output attributes to "type: commented"
      Map<String, String> outputAttr = new HashMap<>(Map.of("type", "commented"));
      if(!AppSettingsState.getInstance().style.isEmpty()) outputAttr.put("style", AppSettingsState.getInstance().style);
      // Return the result of the OpenAIAPI xmlFN function
      return OpenAIAPI.INSTANCE.xmlFN(extension, extension, instruction, inputAttr, outputAttr).apply(string);
    }));
    // Add a TextReplacementAction to the ArrayList
    children.add(TextReplacementAction.create("From " + humanLanguage, String.format("Implement %s -> %s", humanLanguage, extension), Icons.default_icon, (event, string) -> {
      // Set the instruction to "Implement this specification"
      String instruction = "Implement this specification";
      // Set the input attributes to "type: input"
      Map<String, String> inputAttr = Map.of("type", "input");
      // Set the output attributes to "type: output"
      Map<String, String> outputAttr = Map.of("type", "output");
      // Return the result of the OpenAIAPI xmlFN function
      return OpenAIAPI.INSTANCE.xmlFN(humanLanguage.toLowerCase(), extension, instruction, inputAttr, outputAttr).apply(string);
    }));
    // Add a TextReplacementAction to the ArrayList
    children.add(TextReplacementAction.create("To " + humanLanguage, String.format("Describe %s -> %s", humanLanguage, extension), Icons.default_icon, (event, string) -> {
      // Set the instruction to "Describe this code"
      String instruction = "Describe this code";
      if(!AppSettingsState.getInstance().style.isEmpty()) instruction = String.format("%s (%s)", instruction, AppSettingsState.getInstance().style);
      // Set the input attributes to "type: input"
      Map<String, String> inputAttr = Map.of("type", "input");
      // Set the output attributes to "type: output"
      Map<String, String> outputAttr = new HashMap<>(Map.of("type", "output"));
      if(!AppSettingsState.getInstance().style.isEmpty()) outputAttr.put("style", AppSettingsState.getInstance().style);
      // Return the result of the OpenAIAPI xmlFN function
      return OpenAIAPI.INSTANCE.xmlFN(extension, humanLanguage.toLowerCase(), instruction, inputAttr, outputAttr).apply(string);
    }));
  }

  @Override
  public void update(@NotNull AnActionEvent e) {
    e.getPresentation().setEnabledAndVisible(true);
    super.update(e);
  }
}