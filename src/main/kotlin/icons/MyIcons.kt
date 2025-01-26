package icons

import com.intellij.openapi.util.IconLoader

object MyIcons {

  @JvmField
  val micActive = IconLoader.getIcon("/META-INF/Microphone_2.svg", MyIcons::class.java)

  @JvmField
  val micInactive = IconLoader.getIcon("/META-INF/Microphone_1.svg", MyIcons::class.java)

  @JvmField
  val icon = IconLoader.getIcon("/META-INF/toolbarIcon.svg", javaClass)
  /*
  IconLoader.findIcon(
      url = classLoader.getResource("./META-INF/toolbarIcon.svg"),
      storeToCache = true
  )
  */
}