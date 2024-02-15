package com.simiacryptus.skyenet.core.actors

import com.simiacryptus.jopenai.API
import com.simiacryptus.jopenai.ApiModel
import com.simiacryptus.jopenai.ApiModel.ChatMessage
import com.simiacryptus.jopenai.ClientUtil.toChatMessage
import com.simiacryptus.jopenai.ClientUtil.toContentList
import com.simiacryptus.jopenai.GPT4Tokenizer
import com.simiacryptus.jopenai.OpenAIClient
import com.simiacryptus.jopenai.models.ChatModels
import java.io.InputStream
import javax.sound.sampled.AudioSystem
import javax.sound.sampled.Clip

open class TextToSpeechActor(
    prompt: String = "Transform the user request into a text-to-speech prompt that the user will like",
    name: String? = null,
    textModel: ChatModels = ChatModels.GPT35Turbo,
    temperature: Double = 0.3,
) : BaseActor<List<String>, SpeechResponse>(
    prompt = prompt,
    name = name,
    model = textModel,
    temperature = temperature,
) {
    override fun chatMessages(questions: List<String>) = arrayOf(
        ChatMessage(
            role = ApiModel.Role.system,
            content = prompt.toContentList()
        ),
    ) + questions.map {
        ChatMessage(
            role = ApiModel.Role.user,
            content = it.toContentList()
        )
    }

    inner class SpeechResponseImpl(
        override val text: String,
        private val audioStream: InputStream
    ) : SpeechResponse {
        private val _clip: Clip by lazy { play(audioStream) }
        override val clip: Clip get() = _clip
    }

    open fun play(audioStream: InputStream): Clip {
        val clip = AudioSystem.getClip()
        clip.open(AudioSystem.getAudioInputStream(audioStream))
        return clip
    }

    private val codex = GPT4Tokenizer(false)

    override fun answer(vararg messages: ChatMessage, input: List<String>, api: API): SpeechResponse {
        var text = response(*messages, api = api).choices.first().message?.content
            ?: throw RuntimeException("No response")
        // Here you would implement the logic to convert the text to an audio stream
        // For example, using an API call to a text-to-speech service
        val audioStream = convertTextToSpeech(text, api)
        return SpeechResponseImpl(text, audioStream = audioStream)
    }

    // Placeholder for text-to-speech conversion method
    private fun convertTextToSpeech(text: String, api: API): InputStream {
        // Implement the conversion logic here
        throw NotImplementedError("Text-to-speech conversion not implemented")
    }

    override fun withModel(model: ChatModels): TextToSpeechActor = TextToSpeechActor(
        prompt = prompt,
        name = name,
        textModel = model,
        temperature = temperature,
    )
}

interface SpeechResponse {
    val text: String
    val clip: Clip
}