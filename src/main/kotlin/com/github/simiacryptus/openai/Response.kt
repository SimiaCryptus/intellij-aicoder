package com.github.simiacryptus.openai

class Response {
    @Suppress("unused")
    var `object`: String? = null

    @Suppress("unused")
    private var data: Array<Engine> = arrayOf()

    @Suppress("unused")
    constructor()

    @Suppress("unused")
    constructor(`object`: String?, data: Array<Engine>) {
        this.`object` = `object`
        this.data = data
    }
}