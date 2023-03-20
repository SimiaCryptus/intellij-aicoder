package com.github.simiacryptus.aicoder.proxy

import java.awt.image.BufferedImage
import java.io.BufferedWriter
import java.io.File
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import javax.imageio.ImageIO
import kotlin.reflect.KClass

open class GenerationReportBase {
    val proxy = ProxyTest.chatProxy(
        "api.${
            LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm-ss"))
        }.log.json"
    )
    val outputDir = File("../intellij-aicoder-docs")
    fun <T : Any> runReport(prefix: String, kClass: KClass<T>, fn: (T, (Any?) -> Unit, (Any?) -> Unit) -> Unit) {
        if (!ProxyTest.keyFile.exists()) return
        val markdownOutputFile = File(
            outputDir,
            "${prefix}_${LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm-ss"))}.md"
        )
        BufferedWriter(markdownOutputFile.writer()).use { writer ->
            fun out(s: Any?) {
                if (null == s) return
                println(s.toString())
                writer.write(s.toString())
                writer.newLine()
                writer.flush()
            }

            fun logJson(obj: Any?) {
                if (null == obj) return
                out(
                    """
                            |```json
                            |${proxy.toJson(obj)}
                            |```
                            |""".trimMargin()
                )
            }

            val file = File("$prefix.examples.json")
            if (file.exists()) proxy.addExamples(file)
            fn(proxy.create(kClass.java), ::logJson, ::out)
        }
    }

    fun writeImage(image: BufferedImage): String {
        val imageDir = File(outputDir, "images")
        imageDir.mkdirs()
        val file = File.createTempFile("image", ".png", imageDir)
        ImageIO.write(image, "png", file)
        return file.toRelativeString(outputDir)
    }
}
