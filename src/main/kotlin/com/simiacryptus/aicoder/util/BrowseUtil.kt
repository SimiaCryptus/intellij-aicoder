package com.simiacryptus.aicoder.util

import com.simiacryptus.aicoder.config.AppSettingsState
import com.simiacryptus.aicoder.ui.SettingsWidgetFactory.SettingsWidget
import org.slf4j.LoggerFactory
import java.awt.Desktop
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress
import java.net.URI

object BrowseUtil {

  fun browse(uri: URI) {
    SettingsWidget().updateSessionsList()
    sendUdpMessage(uri.toString())
    if (!AppSettingsState.instance.disableAutoOpenUrls && Desktop.isDesktopSupported()) {
      val desktop = Desktop.getDesktop()
      if (desktop.isSupported(Desktop.Action.BROWSE)) {
        desktop.browse(uri)
      }
    }
  }

  var NOTIFICATION_PORT: Int? = 41390
  private fun sendUdpMessage(message: String) {
    try {
      log.info("Sending UDP message: $message")
      val address = InetAddress.getByName("localhost")
      val buf = message.toByteArray()
      val packet = DatagramPacket(buf, buf.size, address, NOTIFICATION_PORT ?: return)
      val socket = DatagramSocket()
      socket.send(packet)
      socket.close()
    } catch (e: Exception) {
      log.warn("Error sending UDP message", e)
    }
  }

  val log = LoggerFactory.getLogger(BrowseUtil::class.java)


}