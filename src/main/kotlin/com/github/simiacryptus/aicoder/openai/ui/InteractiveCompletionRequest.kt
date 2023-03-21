package com.github.simiacryptus.aicoder.openai.ui

import com.github.simiacryptus.aicoder.config.AppSettingsState
import com.github.simiacryptus.aicoder.config.Name
import com.github.simiacryptus.aicoder.openai.async.AsyncAPI
import com.github.simiacryptus.aicoder.util.UITools
import com.github.simiacryptus.openai.CompletionRequest
import com.google.common.util.concurrent.FutureCallback
import com.google.common.util.concurrent.Futures
import com.intellij.ui.components.JBScrollPane
import com.intellij.ui.components.JBTextArea
import com.intellij.ui.components.JBTextField
import java.awt.event.ActionEvent
import java.util.*
import javax.swing.AbstractAction
import javax.swing.JButton
import javax.swing.JOptionPane

class InteractiveCompletionRequest(parent: CompletionRequest) {
    @Suppress("unused")
    @Name("Prompt")
    val prompt: JBScrollPane

    @Suppress("unused")
    @Name("Suffix")
    val suffix: JBTextArea

    @Suppress("unused")
    @Name("Model")
    val model = OpenAI_API.modelSelector

    @Suppress("unused")
    @Name("Temperature")
    val temperature = JBTextField(8)

    @Suppress("unused")
    @Name("Max Tokens")
    val max_tokens = JBTextField(8)
    val testRequest: JButton

    init {
        testRequest = JButton(object : AbstractAction("Test Request") {
            override fun actionPerformed(e: ActionEvent) {
                val withModel = CompletionRequestWithModel(parent, AppSettingsState.instance.model_completion)
                UITools.readKotlinUI(this@InteractiveCompletionRequest, withModel)
                val future = OpenAI_API.getCompletion(null, withModel, "")
                isEnabled = false
                Futures.addCallback(future, object : FutureCallback<CharSequence> {
                    override fun onSuccess(result: CharSequence) {
                        isEnabled = true
                        val text = result.toString()
                        val rows = Math.min(50, text.split("\n".toRegex()).dropLastWhile { it.isEmpty() }
                            .toTypedArray().size)
                        val columns =
                            Math.min(200, Arrays.stream(text.split("\n".toRegex()).dropLastWhile { it.isEmpty() }
                                .toTypedArray()).mapToInt { obj: String -> obj.length }.max().asInt)
                        val area = JBTextArea(rows, columns)
                        area.text = text
                        area.isEditable = false
                        JOptionPane.showMessageDialog(null, area, "Test Output", JOptionPane.PLAIN_MESSAGE)
                    }

                    override fun onFailure(t: Throwable) {
                        isEnabled = true
                        UITools.handle(t)
                    }
                }, AsyncAPI.pool)
            }
        })
        suffix = UITools.configureTextArea(JBTextArea(1, 120))
        prompt = UITools.wrapScrollPane(JBTextArea(10, 120))
    }
}