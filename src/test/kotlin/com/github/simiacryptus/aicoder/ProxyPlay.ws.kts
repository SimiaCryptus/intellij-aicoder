import com.github.simiacryptus.aicoder.proxy.ProxyTest
import com.github.simiacryptus.aicoder.openai.proxy.ChatProxy
import com.github.simiacryptus.aicoder.openai.proxy.CompletionProxy
import com.intellij.openapi.util.io.FileUtil
import java.io.File

val keyFile = File("C:\\Users\\andre\\code\\all-projects\\openai.key")
val chatProxy = ChatProxy(apiKey = FileUtil.loadFile(keyFile).trim(), apiLog = "api.log.json")
val completionProxy = CompletionProxy(
    apiKey = FileUtil.loadFile(keyFile).trim(),
    apiLog = FileUtil.loadFile(keyFile).trim()
)

println(completionProxy.api.getEngines().joinToString("\n"))
val statement = "The meaning of life is to live a life of meaning."
val proxyFactory = chatProxy
val proxy = proxyFactory.create(ProxyTest.EssayAPI::class.java)
val essayOutline = proxy.essayOutline(
    ProxyTest.EssayAPI.Thesis(statement),
    "5000 words"
)
println(essayOutline.introduction!!.thesis.statement)
