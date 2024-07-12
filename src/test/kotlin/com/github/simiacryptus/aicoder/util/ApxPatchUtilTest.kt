package com.github.simiacryptus.aicoder.util

import com.simiacryptus.diff.PatchUtil
import org.junit.Assert.assertTrue
import org.junit.Test

class ApxPatchUtilTest {
    private val patch = """
    --- IntArrayAppendFile.kt
    +++ IntArrayAppendFileUpdated.kt
    @@ -8,7 +8,7 @@
         val length = file.length()
         //require(length > 0) { "Data file empty: {$}length" }
         require(length < Int.MAX_VALUE) { "Data file too large: {$}length" }
    -    XElements(length/4)
    +    XElements(length/4) // Initialize length directly in the constructor
       }
     
        fun append(values: IntArray) {
    -     if(isClosed) throw IllegalStateException("File is closed")
    -     val toBytes = value.toBytes()
    -     bufferedOutputStream.write(toBytes)
    -     length = length + 1
    +     try {
    +         if(isClosed) throw IllegalStateException("File is closed")
    +         for (value in values) {
    +             val toBytes = value.toBytes()
    +             bufferedOutputStream.write(toBytes)
    +         }
    +         length = length + values.size
    +     } catch (e: IOException) {
    +         // Handle file I/O exception
    +         e.printStackTrace()
    +     }
        }
  """.trimIndent()
    private val source = """
    package com.simiacryptus.util.files

    import java.io.File

    class IntArrayAppendFile(val file: File) {

      private var isClosed: Boolean = false
      var length : XElements = run {
        val length = file.length()
        //require(length > 0) { "Data file empty: {$}length" }
        require(length < Int.MAX_VALUE) { "Data file too large: {$}length" }
        XElements(length/4)
      }

      private val bufferedOutputStream by lazy { file.outputStream().buffered() }
      fun append(value: Int) {
        if(isClosed) throw IllegalStateException("File is closed")
        val toBytes = value.toBytes()
        bufferedOutputStream.write(toBytes)
        length = length + 1
      }


      fun close() {
        isClosed = true
        bufferedOutputStream.close()
      }

      companion object {
      }
    }
  """.trimIndent()

    @Test
    fun testPatch() {
        val result = PatchUtil.applyPatch(source, patch)
        println(result)
        assertTrue(result.contains("for (value in values"))
    }
}