package com.github.simiacryptus.openai

class ApiError {
    @Suppress("unused")
    var message: String? = null

    @Suppress("unused")
    var type: String? = null

    @Suppress("unused")
    var param: String? = null

    @Suppress("unused")
    var code: Double? = null

    @Suppress("unused")
    constructor()

    @Suppress("unused")
    constructor(message: String?, type: String?, param: String?, code: Double?) {
        this.message = message
        this.type = type
        this.param = param
        this.code = code
    }
}