package com.github.simiacryptus.aicoder.proxy

import org.junit.Test

import java.io.File

class ImageTest: GenerationReportBase() {
    @Test
    fun imageGenerationTest() {
        val image = proxy.api.render("Hello World")[0]
        // Write the image to a file
        val file = File.createTempFile("image", ".png")
        javax.imageio.ImageIO.write(image, "png", file)
        // Open the file
        java.awt.Desktop.getDesktop().open(file)
    }
}