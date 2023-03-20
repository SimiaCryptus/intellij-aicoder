package com.github.simiacryptus.openai

import java.io.IOException

class ModelMaxException(
    val modelMax: Int,
    val request: Int,
    val messages: Int,
    val completion: Int
) : IOException("Model max exceeded: $modelMax, request: $request, messages: $messages, completion: $completion")