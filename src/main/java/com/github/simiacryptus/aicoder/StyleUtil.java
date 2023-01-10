package com.github.simiacryptus.aicoder;

import com.github.simiacryptus.aicoder.config.AppSettingsState;
import com.github.simiacryptus.aicoder.openai.ModerationException;
import com.github.simiacryptus.aicoder.text.IndentedText;
import com.github.simiacryptus.aicoder.text.StringTools;
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

    /**
     *
     * This method will generate a random combination of a dialect and style
     *
     * @return A string in the format of "Dialect - Casual, Inspirational"
     */
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

    /**
     * Demonstrates the description of a given code snippet using a given style.
     *
     * @param style    The style to describe with.
     * @param language The language of the code snippet.
     * @param code     The code snippet to be described.
     */
    public static void demoStyle(String style, ComputerLanguage language, String code) {
        String codeDescription = describeTest(style, language, code);
        String message = String.format("This code:\n    %s\nwas described as:\n    %s", code.replace("\n", "\n    "), codeDescription.replace("\n", "\n    "));
        JOptionPane.showMessageDialog(null, message, "Style Demo", JOptionPane.INFORMATION_MESSAGE);
    }

    /**
     * Describes some test code in the specified style and language.
     *
     * @param style       The style of the description.
     * @param language    The language of the test.
     * @param code The code.
     * @return A description of the test in the specified style and language.
     */
    public static String describeTest(String style, ComputerLanguage language, String code) {
        AppSettingsState settings = AppSettingsState.getInstance();
        try {
            return StringTools.lineWrapping(settings.createTranslationRequest()
                    .setInstruction(String.format("Explain this %s in %s (%s)", language.name(), settings.humanLanguage, style))
                    .setInputText(IndentedText.fromString(code).getTextBlock().trim())
                    .setInputType(language.name())
                    .setInputAttribute("type", "code")
                    .setOutputType(settings.humanLanguage)
                    .setOutputAttrute("type", "description")
                    .setOutputAttrute("style", style)
                    .buildCompletionRequest()
                    .complete("")
                    .trim(), 120);
        } catch (ModerationException e) {
            return e.getMessage();
        } catch (IOException e) {
            log.error(e);
            return e.getMessage();
        }
    }
}
