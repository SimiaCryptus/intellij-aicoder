package com.github.simiacryptus.aicoder.config;

import com.github.simiacryptus.aicoder.ComputerLanguage;
import com.github.simiacryptus.aicoder.openai.*;
import com.github.simiacryptus.aicoder.text.IndentedText;
import com.intellij.openapi.diagnostic.Logger;

import javax.swing.*;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Random;

public class StyleUtil {
    private static final Logger log = Logger.getInstance(StyleUtil.class);

    /**
     * A list of style keywords used to describe the type of writing.
     */
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

    /**
     * A list of dialect keywords for use in writing.
     */
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
            "Yoda-Speak"
    );

    // This here code is gonna pick two random styles from a list of 'em and combine 'em with a random dialect
    // It'll then return a string with the combination of the three
    public static String randomStyle() {
        String dialect = dialectKeywords.get(new Random().nextInt(dialectKeywords.size()));
        String style1 = styleKeywords.get(new Random().nextInt(styleKeywords.size()));
        String style2 = style1;
        while (style2.equals(style1)) style2 = styleKeywords.get(new Random().nextInt(styleKeywords.size()));
        return String.format("%s - %s, %s", dialect, style1, style2);
    }

    public static void demoStyle(String style) {
        demoStyle(style,
                ComputerLanguage.Java,
                "List<String> items = new ArrayList<>();\n" +
                        "items.add(\"apple\");\n" +
                        "items.add(\"orange\");\n" +
                        "items.add(\"pear\");\n" +
                        "Random rand = new Random();\n" +
                        "int randomIndex = rand.nextInt(items.size());\n" +
                        "String randomItem = items.get(randomIndex);");
    }

    public static void demoStyle(String style, ComputerLanguage language, String code) {
        String codeDescription = describeTest(style, language, code);
        String message = String.format("This code:\n    %s\nwas described as:\n    %s", code.replace("\n", "\n    "), codeDescription.replace("\n", "\n    "));
        JOptionPane.showMessageDialog(null, message, "Style Demo", JOptionPane.INFORMATION_MESSAGE);
    }

    public static String describeTest(String style, ComputerLanguage language, String inputString) {
        AppSettingsState settings = AppSettingsState.getInstance();
        CompletionRequest request = settings.createTranslationRequestTemplate()
                .setInputTag(language.name())
                .setOutputTag(settings.humanLanguage)
                .setInstruction(String.format("Explain this %s in %s (%s)", language.name(), settings.humanLanguage, style))
                .setInputAttr("type", "code")
                .setOutputAttr("type", "description")
                .setOutputAttr("style", style)
                .setOriginalText(IndentedText.fromString(inputString).textBlock.trim())
                .buildRequest();
        try {
            CompletionResponse response = OpenAI.INSTANCE.complete(request);
            String completionText = request.getCompletionText(response, "");
            String trimmedText = completionText.trim();
            String lineWrappedText = StringTools.lineWrapping(trimmedText);
            return lineWrappedText;
        } catch (ModerationException e) {
            return e.getMessage();
        } catch (IOException e) {
            log.error(e);
            return e.getMessage();
        }
    }
}
