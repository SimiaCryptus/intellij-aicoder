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
import java.util.Map;

public class AICoderContextGroup extends ActionGroup {

  @Override
  public AnAction @NotNull [] getChildren(AnActionEvent e) {
    VirtualFile file = e.getRequiredData(CommonDataKeys.VIRTUAL_FILE);
    ArrayList<AnAction> children = new ArrayList<>();
    String extension = file.getExtension().toLowerCase();
    String humanLanguage = "English";
    switch (extension) {
      case "java":
        children.add(TextReplacementAction.create("Add JavaDoc Comments", "Add JavaDoc Comments", Icons.default_icon, (event, string) -> {
          String instruction = "Rewrite to include detailed JavaDocs";
          Map<String, String> inputAttr = Map.of("type", "uncommented");
          Map<String, String> outputAttr = Map.of("type", "commented");
          return OpenAIAPI.INSTANCE.xmlFN(extension, extension, instruction, inputAttr, outputAttr).apply(string);
        }));
      case "sh":
      case "py":
        children.add(TextReplacementAction.create("Add Code Comments", "Add Code Comments", Icons.default_icon, (event, string) -> {
          String instruction = "Rewrite to include detailed code comments at the end of every line";
          Map<String, String> inputAttr = Map.of("type", "uncommented");
          Map<String, String> outputAttr = Map.of("type", "commented");
          return OpenAIAPI.INSTANCE.xmlFN(extension, extension, instruction, inputAttr, outputAttr).apply(string);
        }));
        children.add(TextReplacementAction.create("From " + humanLanguage, String.format("Implement %s -> %s", humanLanguage, extension), Icons.default_icon, (event, string) -> {
          String instruction = "Implement this specification";
          Map<String, String> inputAttr = Map.of("type", "input");
          Map<String, String> outputAttr = Map.of("type", "output");
          return OpenAIAPI.INSTANCE.xmlFN(humanLanguage.toLowerCase(), extension, instruction, inputAttr, outputAttr).apply(string);
        }));
        children.add(TextReplacementAction.create("To " + humanLanguage, String.format("Describe %s -> %s", humanLanguage, extension), Icons.default_icon, (event, string) -> {
          String instruction = "Describe this code";
          Map<String, String> inputAttr = Map.of("type", "input");
          Map<String, String> outputAttr = Map.of("type", "output");
          return OpenAIAPI.INSTANCE.xmlFN(extension, humanLanguage.toLowerCase(), instruction, inputAttr, outputAttr).apply(string);
        }));
        break;
      default:
        break;
    }

    if(CopyPasteManager.getInstance().areDataFlavorsAvailable(DataFlavor.stringFlavor)) {
      children.add(TextReplacementAction.create("Paste", "Paste", Icons.default_icon, (event, string) -> {
        String instruction = "Translate this input into " + extension;
        Map<String, String> inputAttr = Map.of("language", "autodetect");
        Map<String, String> outputAttr = Map.of("language", extension);
        String pasteContents = CopyPasteManager.getInstance().getContents(DataFlavor.stringFlavor).toString();
        return OpenAIAPI.INSTANCE.xmlFN("source", "translated", instruction, inputAttr, outputAttr).apply(pasteContents);
      }));
    }

    return children.toArray(AnAction[]::new);
  }

  @Override
  public void update(@NotNull AnActionEvent e) {
    e.getPresentation().setEnabledAndVisible(true);
    super.update(e);
  }
}