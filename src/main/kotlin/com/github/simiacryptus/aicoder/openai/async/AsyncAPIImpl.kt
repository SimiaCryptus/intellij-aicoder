package com.github.simiacryptus.aicoder.openai.async

import com.github.simiacryptus.aicoder.config.AppSettingsState
import com.github.simiacryptus.aicoder.openai.ui.OpenAI_API
import com.github.simiacryptus.aicoder.openai.core.CoreAPI
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.progress.ProgressIndicator
import com.jetbrains.rd.util.AtomicReference
import java.io.IOException

class AsyncAPIImpl(core: CoreAPI, appSettingsState: AppSettingsState) : AsyncAPI(
    core,
    appSettingsState.suppressProgress
) {
    @Override
    override fun checkCanceled(indicator: ProgressIndicator, threadRef: AtomicReference<Thread?>) {
        if (indicator.isCanceled) {
            val thread = threadRef.get()
            if (null != thread) {
                thread.interrupt()
                try {
                    coreAPI.clients[thread]!!.close()
                } catch (e: IOException) {
                    log.warn("Error closing client: " + e.message)
                }
            }
        }
    }

    companion object {
        @JvmStatic
        private val log = Logger.getInstance(OpenAI_API::class.java)
    }
}