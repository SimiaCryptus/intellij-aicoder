package com.github.simiacryptus.aicoder.test.util

import com.simiacryptus.diff.DiffMatchPatch
import com.simiacryptus.diff.DiffMatchPatch.Diff
import com.simiacryptus.diff.DiffMatchPatch.Operation.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions
import java.util.*


class DiffMatchPatchTest {

    @Test
    fun testDiffMain() {
        // Test equality
        Assertions.assertEquals(mutableListOf(Diff(EQUAL, "test")), DiffMatchPatch.diff_main("test", "test", false))

        // Test differences
        val diffs = LinkedList<Diff>()
        diffs.add(Diff(DELETE, "Hello"))
        diffs.add(Diff(INSERT, "Goodbye"))
        diffs.add(Diff(EQUAL, " world."))
        Assertions.assertEquals(
            mutableListOf(
                Diff(DELETE, "Hell"), Diff(INSERT, "G"), Diff(EQUAL, "o"), Diff(INSERT, "odbye"), Diff(EQUAL, " world.")
            ), DiffMatchPatch.diff_main("Hello world.", "Goodbye world.", false)
        )
    }

    @Test
    fun testDiffCommonPrefix() {
        Assertions.assertEquals(0, DiffMatchPatch.diff_commonPrefix("abc", "xyz"))
        Assertions.assertEquals(4, DiffMatchPatch.diff_commonPrefix("1234abcdef", "1234xyz"))
        Assertions.assertEquals(4, DiffMatchPatch.diff_commonPrefix("1234", "1234xyz"))
    }

    @Test
    fun testDiffCommonSuffix() {
        Assertions.assertEquals(0, DiffMatchPatch.diff_commonSuffix("abc", "xyz"))
        Assertions.assertEquals(4, DiffMatchPatch.diff_commonSuffix("abcdef1234", "xyz1234"))
        Assertions.assertEquals(4, DiffMatchPatch.diff_commonSuffix("1234", "xyz1234"))
    }

//    @Test
    fun testPatchMakeAndApply() {
        val text1 = "The quick brown fox jumps over the lazy dog."
        val text2 = "The quick red fox jumps over the tired dog."
        val patches: LinkedList<DiffMatchPatch.Patch> = DiffMatchPatch.patch_make(text1, text2)
        val results: Array<Any> = DiffMatchPatch.patch_apply(patches, text1)

        Assertions.assertEquals(text2, results[0])
        val applied = results[1] as BooleanArray
        for (b in applied) {
            Assertions.assertTrue(b)
        }
    }

//    @Test
    fun testPatchMakeWithDiffs() {
        val text1 = "The quick brown fox jumps over the lazy dog."
        val text2 = "That quick brown fox jumped over a lazy dog."
        val diffs: LinkedList<Diff> = DiffMatchPatch.diff_main(text1, text2, false)
        val patches: LinkedList<DiffMatchPatch.Patch> = DiffMatchPatch.patch_make(diffs)

        Assertions.assertFalse(patches.isEmpty())
        Assertions.assertEquals(diffs, patches.first().diffs)
    }

    @Test
    fun testPatchToText() {
        val text1 = "The quick brown\n fox jumps over\n the lazy dog.\n"
        val text2 = "The quick red\n fox jumps over\n the tired dog.\n"
        if (text1 == null || text2 == null) {
            throw IllegalArgumentException("Null inputs. (patch_make)")
        }
        // No diffs provided, compute our own.
        // Set a deadline by which time the diff must be complete.
        val deadline: Long
        if (DiffMatchPatch.Diff_Timeout <= 0) {
            deadline = Long.MAX_VALUE
        } else {
            deadline = System.currentTimeMillis() + (DiffMatchPatch.Diff_Timeout * 1000).toLong()
        }
        val diffs = DiffMatchPatch.diff_main(text1, text2, false /*true*/, deadline)
        if (diffs.size > 2) {
            DiffMatchPatch.diff_cleanupSemantic(diffs)
            DiffMatchPatch.diff_cleanupEfficiency(diffs)
        }
        val patches: LinkedList<DiffMatchPatch.Patch> = DiffMatchPatch.patch_make(text1, diffs)
        val patchText: String = DiffMatchPatch.patch_toText(patches)
        println(patchText)

        Assertions.assertFalse(patchText.isEmpty())
        Assertions.assertTrue(patchText.startsWith("@@ -1,"), patchText)
    }

    @Test
    fun testPatchFromText() {
        val patchText = """@@ -1,8 +1,5 @@
-The quick
+That
  brown
@@ -22,17 +19,17 @@
 jumps
-over the lazy
+over a lazy
  dog.
"""
        val patches = DiffMatchPatch.patch_fromText(patchText)

        Assertions.assertFalse(patches.isEmpty())
        Assertions.assertEquals("The quick", patches.first().diffs.first().text)
    }

    @Test
    // expect IllegalArgumentException::class
    fun testPatchFromTextWithInvalidInput() {
        val invalidPatchText = """@@ -1,8 +1,5 @@
-The quick
+That
  brown
"""
        // This should throw an IllegalArgumentException due to the incomplete patch text
        DiffMatchPatch.patch_fromText(invalidPatchText)
    }

}
