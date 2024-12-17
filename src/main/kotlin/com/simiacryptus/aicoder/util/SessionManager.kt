package com.simiacryptus.aicoder.util

import com.intellij.openapi.components.Service
import com.simiacryptus.skyenet.core.platform.Session

@Service
class SessionManager {
    private var currentSession: Session? = null
    
    fun createSession(): Session {
        currentSession = Session.newGlobalID() // Or however you create your sessions
        return currentSession!!
    }
    
    fun getCurrentSession(): Session? = currentSession
}