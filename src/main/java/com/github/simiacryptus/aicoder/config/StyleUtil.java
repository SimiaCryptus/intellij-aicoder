package com.github.simiacryptus.aicoder.config;

import com.github.simiacryptus.aicoder.ComputerLanguage;
import com.github.simiacryptus.aicoder.openai.CompletionRequest;
import com.github.simiacryptus.aicoder.openai.OpenAI;
import com.github.simiacryptus.aicoder.openai.StringTools;
import com.github.simiacryptus.aicoder.text.IndentedText;
import com.intellij.openapi.diagnostic.Logger;

import javax.swing.*;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Random;

public class StyleUtil {
    private static final Logger log = Logger.getInstance(StyleUtil.class);

    private static final List<String> styleKeywords = Arrays.asList(
            "Analytical",
            "Casual",
            "Comic-book",
            "Conversational",
            "Educational",
            "Factual",
            "Humorous",
            "Inspirational",
            "Instructional",
            "Literary",
            "Narrative",
            "Parody",
            "Philosophical",
            "Poetic",
            "Referential",
            "Scientific",
            "Storytelling",
            "Technical"
    );
    private static final List<String> dialectKeywords = Arrays.asList(
            "Academic Writing",
            "Business Writing",
            "Character Monologues",
            "Cockney Rhyming Slang",
            "Comedy Writing",
            "Comic Book Writing",
            "Comic Strip Writing",
            "Courtroom Language",
            "Cowboy Slang",
            "Detective Writing",
            "Dramatic Writing",
            "Elf-Speak",
            "Emojis",
            "Fantasy Writing",
            "Gangsta Rap",
            "Haiku",
            "Horror Writing",
            "Hyperbole",
            "Journalism Speak",
            "Legal Language",
            "Limericks",
            "Medical Terminology",
            "Military Language",
            "Mockery",
            "Olde English",
            "Pirate Talk",
            "Political Jargon",
            "Rap",
            "Religious Script",
            "Rhyming Poetry",
            "Romance Writing",
            "Sarcasm",
            "Satire Writing",
            "Sci-Fi Techno-Babble",
            "Science Fiction Writing",
            "Shakespearean English",
            "Slang",
            "Southern Charm",
            "Surfer Dude Speak",
            "Technical Writing",
            "Techno-Jargon",
            "Urban Slang",
            "Valley Girl Speak",
            "Verse",
            "Western Writing",
            "Yoda-Speak");

    public static String randomStyle() {
        String dialect = dialectKeywords.get(new Random().nextInt(dialectKeywords.size()));
        String style1 = styleKeywords.get(new Random().nextInt(styleKeywords.size()));
        String style2 = style1;
        while (style2.equals(style1)) style2 = styleKeywords.get(new Random().nextInt(styleKeywords.size()));
        return String.format("%s - %s, %s", dialect, style1, style2);
    }

    public static void demoStyle(String style) {
        ComputerLanguage language = ComputerLanguage.Java;
        String code = "List<String> items = new ArrayList<>();\n" +
                "items.add(\"apple\");\n" +
                "items.add(\"orange\");\n" +
                "items.add(\"pear\");\n" +
                "Random rand = new Random();\n" +
                "int randomIndex = rand.nextInt(items.size());\n" +
                "String randomItem = items.get(randomIndex);";
        String codeDescription = describeTest(style, AppSettingsState.getInstance().humanLanguage, language, code);
        String message = String.format("This code:\n    %s\nwas described as:\n    %s", code.replace("\n", "\n    "), codeDescription.replace("\n", "\n    "));
        JOptionPane.showMessageDialog(null, message, "Style Demo", JOptionPane.INFORMATION_MESSAGE);
    }

    public static String describeTest(String style, String outputHumanLanguage, ComputerLanguage language, String inputString) {
        AppSettingsState settings = AppSettingsState.getInstance();
        String instruction = "Explain this " + language.name() + " in " + outputHumanLanguage;
        if (!style.isEmpty()) instruction = String.format("%s (%s)", instruction, style);
        CompletionRequest request = settings.createTranslationRequestTemplate()
                .setInputTag(language.name())
                .setOutputTag(outputHumanLanguage)
                .setInstruction(instruction)
                .setInputAttr("type", "code")
                .setOutputAttr("type", "description")
                .setOutputAttr("style", style)
                .setOriginalText(IndentedText.fromString(inputString).textBlock.trim())
                .buildRequest();
        //String indent = indentedInput.indent;
        String codeDescription = "";
        try {
            codeDescription = StringTools.lineWrapping(request.getCompletionText(OpenAI.INSTANCE.request(request), "").trim());
        } catch (IOException e) {
            log.error(e);
        }
        return codeDescription;
    }
}
