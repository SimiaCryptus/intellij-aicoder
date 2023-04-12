package com.github.simiacryptus.aicoder.util

import com.github.simiacryptus.aicoder.config.AppSettingsState
import com.github.simiacryptus.aicoder.openai.async.AsyncAPI
import com.github.simiacryptus.aicoder.openai.async.AsyncAPI.Companion.map
import com.github.simiacryptus.aicoder.openai.ui.OpenAI_API.getCompletion
import com.simiacryptus.util.StringTools.lineWrapping
import com.google.common.util.concurrent.ListenableFuture
import com.intellij.openapi.diagnostic.Logger
import java.util.*
import javax.swing.JOptionPane

object StyleUtil {
    @Suppress("unused")
    private val log = Logger.getInstance(
        StyleUtil::class.java
    )

    /**
     * A list of style keywords used to describe the type of writing.
     */
    private val styleKeywords: List<CharSequence> = mutableListOf<CharSequence>(
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
    )

    /**
     * A list of dialect keywords for use in writing.
     */
    private val dialectKeywords: List<CharSequence> = mutableListOf<CharSequence>(
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
    )

    /**
     *
     * This method will generate a random combination of a dialect and style
     *
     * @return A string in the format of "Dialect - Casual, Inspirational"
     */
    fun randomStyle(): String {
        val dialect = dialectKeywords[Random().nextInt(dialectKeywords.size)]
        val style1 = styleKeywords[Random().nextInt(styleKeywords.size)]
        var style2 = style1
        while (style2 == style1) style2 =
            styleKeywords[Random().nextInt(styleKeywords.size)]
        return String.format("%s - %s, %s", dialect, style1, style2)
    }

    /**
     * Demonstrates the description of a given code snippet using a given style.
     *
     * @param style    The style to describe with.
     * @param language The language of the code snippet.
     * @param code     The code snippet to be described.
     */
    @JvmOverloads
    fun demoStyle(
        style: CharSequence?, language: ComputerLanguage =
            ComputerLanguage.Java, code: String =
            "List<String> items = new ArrayList<>();\n" +
                    "items.add(\"apple\");\n" +
                    "items.add(\"orange\");\n" +
                    "items.add(\"pear\");\n" +
                    "Random rand = new Random();\n" +
                    "int randomIndex = rand.nextInt(items.size());\n" +
                    "String randomItem = items.get(randomIndex);"
    ) {
        AsyncAPI.onSuccess(
            describeTest(style, language, code)
        ) { description: CharSequence ->
            val message: CharSequence = String.format(
                "This code:\n    %s\nwas described as:\n    %s",
                code.replace("\n", "\n    "),
                description.toString().replace("\n", "\n    ")
            )
            JOptionPane.showMessageDialog(
                null,
                message,
                "Style Demo",
                JOptionPane.INFORMATION_MESSAGE
            )
        }
    }

    /**
     * Describes some test code in the specified style and language.
     *
     * @param style    The style of the description.
     * @param language The language of the test.
     * @param code     The code.
     * @return A description of the test in the specified style and language.
     */
    fun describeTest(style: CharSequence?, language: ComputerLanguage, code: String?): ListenableFuture<CharSequence> {
        val settings = AppSettingsState.instance
        val completionRequest = settings.createTranslationRequest()
            .setInstruction(String.format("Explain this %s in %s (%s)", language.name, settings.humanLanguage, style))
            .setInputText(IndentedText.fromString((code)!!).textBlock.toString().trim())
            .setInputType(language.name)
            .setInputAttribute("type", "code")
            .setOutputType(settings.humanLanguage)
            .setOutputAttrute("type", "description")
            .setOutputAttrute("style", style)
            .buildCompletionRequest()
        val future = getCompletion(null, completionRequest, "")
        return map(future) { x: CharSequence? ->
            lineWrapping(
                x.toString().trim { it <= ' ' },
                120
            )
        }
    }
}