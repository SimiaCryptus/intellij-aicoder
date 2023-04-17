package com.github.simiacryptus.aicoder.openai.ui

import com.github.simiacryptus.aicoder.config.Name
import com.github.simiacryptus.aicoder.openai.async.AsyncAPI
import com.github.simiacryptus.aicoder.util.UITools
import com.simiacryptus.openai.EditRequest
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

class InteractiveEditRequest {
    @Suppress("unused")
    @Name("Input")
    val input: JBScrollPane

    @Suppress("unused")
    @Name("Instruction")
    val instruction: JBTextArea

    @Suppress("unused")
    @Name("Model")
    val model = OpenAI_API.modelSelector

    @Suppress("unused")
    @Name("Temperature")
    val temperature = JBTextField(8)
    val testRequest: JButton = JButton(object : AbstractAction("Test Request") {
        override fun actionPerformed(e: ActionEvent) {
            val request = EditRequest()
            UITools.readKotlinUI(this@InteractiveEditRequest, request)
            val future = OpenAI_API.edit(null, request, "")
            isEnabled = false
            Futures.addCallback(future, object : FutureCallback<CharSequence?> {
                override fun onSuccess(result: CharSequence?) {
                    isEnabled = true
                    val text = result.toString()
                    val rows = 50.coerceAtMost(text.split("\n".toRegex()).dropLastWhile { it.isEmpty() }
                        .toTypedArray().size)
                    val columns =
                        200.coerceAtMost(Arrays.stream(text.split("\n".toRegex()).dropLastWhile { it.isEmpty() }
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

    init {
        input = UITools.wrapScrollPane(JBTextArea(10, 120))
        instruction = UITools.configureTextArea(JBTextArea(1, 120))
    }
}