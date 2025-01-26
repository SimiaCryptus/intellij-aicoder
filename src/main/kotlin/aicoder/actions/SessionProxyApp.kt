package aicoder.actions

import com.simiacryptus.skyenet.core.platform.ApplicationServices
import com.simiacryptus.skyenet.core.platform.Session
import com.simiacryptus.skyenet.core.platform.model.ApplicationServicesConfig.dataStorageRoot
import com.simiacryptus.skyenet.core.platform.model.User
import com.simiacryptus.skyenet.webui.application.AppInfoData
import com.simiacryptus.skyenet.webui.application.ApplicationServer
import com.simiacryptus.skyenet.webui.chat.ChatServer
import com.simiacryptus.skyenet.webui.session.SocketManager

class SessionProxyServer : ApplicationServer(
  applicationName = "AI Coding Assistant",
  path = "/",
  showMenubar = false,
) {
  override val singleInput = true
  override val stickyInput = false
  override fun appInfo(session: Session) = appInfoMap.getOrPut(session) {
    AppInfoData(
      applicationName = applicationName,
      singleInput = singleInput,
      stickyInput = stickyInput,
      loadImages = false,
      showMenubar = showMenubar
    )
  }.toMap()

  override fun newSession(user: User?, session: Session) =
    chats[session]?.newSession(user, session) ?: agents[session]
    ?: throw IllegalStateException("No agent found for session $session")

  companion object {
    private val log = org.slf4j.LoggerFactory.getLogger(SessionProxyServer::class.java)
    val metadataStorage by lazy { ApplicationServices.metadataStorageFactory(dataStorageRoot) }
    val agents = mutableMapOf<Session, SocketManager>()
    val chats = mutableMapOf<Session, ChatServer>()
  }
}
