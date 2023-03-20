package com.github.simiacryptus.openai

import com.github.simiacryptus.aicoder.config.AppSettingsState
import com.github.simiacryptus.aicoder.openai.ui.InteractiveEditRequest
import com.github.simiacryptus.aicoder.util.UITools
import com.intellij.util.ui.FormBuilder

class EditRequest {
    var model: String = ""
    var input: String? = null
    var instruction: String = ""

    @Suppress("unused")
    var temperature: Double? = null

    @Suppress("unused")
    var n: Int? = null
    var top_p: Double? = null

    @Suppress("unused")
    constructor()
    constructor(settingsState: AppSettingsState) {
        setInstruction("")
        setModel(settingsState.model_edit)
        setTemperature(settingsState.temperature)
    }

    constructor(instruction: String) {
        setInstruction(instruction)
        setModel(AppSettingsState.instance.model_edit)
        setTemperature(AppSettingsState.instance.temperature)
    }

    constructor(instruction: String, input: String?) {
        setInput(input)
        setInstruction(instruction)
        setModel(AppSettingsState.instance.model_edit)
        setTemperature(AppSettingsState.instance.temperature)
    }

    constructor(model: String, input: String?, instruction: String, temperature: Double?) {
        setModel(model)
        setInput(input)
        setInstruction(instruction)
        setTemperature(temperature)
    }

    constructor(obj: EditRequest) {
        model = obj.model
        top_p = obj.top_p
        input = obj.input
        instruction = obj.instruction
        temperature = obj.temperature
        n = obj.n
    }

    fun setModel(model: String): EditRequest {
        this.model = model
        return this
    }

    fun setInput(input: String?): EditRequest {
        this.input = input
        return this
    }

    fun setInstruction(instruction: String): EditRequest {
        this.instruction = instruction
        return this
    }

    fun setTemperature(temperature: Double?): EditRequest {
        top_p = null
        this.temperature = temperature
        return this
    }

    fun setN(n: Int?): EditRequest {
        this.n = n
        return this
    }

    fun setTop_p(top_p: Double?): EditRequest {
        temperature = null
        this.top_p = top_p
        return this
    }

    override fun toString(): String {
        return "EditRequest{" + "model='" + model + '\'' +
                ", input='" + input + '\'' +
                ", instruction='" + instruction + '\'' +
                ", temperature=" + temperature +
                ", n=" + n +
                ", top_p=" + top_p +
                '}'
    }

    fun showModelEditDialog(): EditRequest {
        val formBuilder = FormBuilder.createFormBuilder()
        val withModel = EditRequest(this)
        val ui = InteractiveEditRequest(withModel)
        UITools.addKotlinFields<Any>(ui, formBuilder)
        UITools.writeKotlinUI(ui, withModel)
        val mainPanel = formBuilder.panel
        return if (UITools.showOptionDialog(mainPanel, arrayOf<Any>("OK"), title = "Completion Request") == 0) {
            UITools.readKotlinUI(ui, withModel)
            withModel
        } else {
            withModel
        }
    }

    fun uiIntercept(): EditRequest {
        return if (AppSettingsState.instance.devActions) {
            showModelEditDialog()
        } else {
            this
        }
    }
}