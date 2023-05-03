package com.github.simiacryptus.aicoder

import com.intellij.ide.FrameStateListener
import com.simiacryptus.skyenet.OutputInterceptor

internal class MyFrameStateListener : FrameStateListener {
    override fun onFrameActivated() {
        OutputInterceptor.setupInterceptor()
    }
}