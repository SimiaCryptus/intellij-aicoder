package com.github.simiacryptus.aicoder.openai.translate

import com.github.simiacryptus.aicoder.config.AppSettingsState
import com.github.simiacryptus.openai.CompletionRequest
import java.util.*
import java.util.stream.Collectors

class TranslationRequest_XML(settings: AppSettingsState) :
    BaseTranslationRequest<TranslationRequest_XML>(settings) {

    override fun buildCompletionRequest(): CompletionRequest {
        val inputAttrStr: CharSequence = if (inputAttr.isEmpty()) "" else " " + inputAttr.entries.stream()
            .map { (key, value) -> """$key="$value"""" }.collect(Collectors.joining(" "))
        val outputAttrStr: CharSequence = if (outputAttr.isEmpty()) "" else " " + outputAttr.entries.stream()
            .map { (key, value) -> """$key="$value"""" }.collect(Collectors.joining(" "))
        val exampleStr: CharSequence =
            if (examples.isEmpty()) "" else ("<examples>" + examples.joinToString("\n\t") + "</examples>\n")
        val inputTagTxt = inputTag.toString().lowercase(Locale.getDefault())
        val outputTagTxt = outputTag.toString().lowercase(Locale.getDefault())
        val inputText = originalText.toString().trim { it <= ' ' }
        return CompletionRequest(
            """
            <!-- $instruction -->
            <$inputTagTxt$inputAttrStr>$inputText</$inputTagTxt>
            $exampleStr
            <$outputTagTxt$outputAttrStr>
            """.trimIndent().trim(),
            temperature,
            maxTokens,
            null,
            """</$outputTagTxt>"""
        )
    }

    private val examples: MutableList<CharSequence> = ArrayList()
    override fun addExample(
        exampleText: CharSequence,
        attributes: Map<CharSequence, CharSequence>
    ): TranslationRequest {
        val outputTagTxt = outputTag.toString().lowercase(Locale.getDefault())
        val outputAttrStr = if (attributes.isEmpty()) "" else " " + attributes.entries.stream()
            .map { (key, value) -> "$key=\"$value\"" }.collect(Collectors.joining(" "))
        examples.add("""<$outputTagTxt$outputAttrStr>$exampleText</$outputTagTxt>""")
        return this
    }
}