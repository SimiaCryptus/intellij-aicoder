package icons

import com.intellij.openapi.util.IconLoader

object MyIcons {

    @JvmField
    val icon = IconLoader.getIcon("/META-INF/toolbarIcon.svg", javaClass)
    /*
    IconLoader.findIcon(
        url = classLoader.getResource("./META-INF/toolbarIcon.svg"),
        storeToCache = true
    )
    */
}