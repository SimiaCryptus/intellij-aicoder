@file:Suppress("NAME_SHADOWING")

package com.github.simiacryptus.aicoder.test

import com.github.simiacryptus.aicoder.test.UITestUtil.Companion.canRunTests
import com.github.simiacryptus.aicoder.test.UITestUtil.Companion.documentJavaImplementation
import com.github.simiacryptus.aicoder.test.UITestUtil.Companion.documentMarkdownListAppend
import com.github.simiacryptus.aicoder.test.UITestUtil.Companion.documentTextAppend
import com.github.simiacryptus.aicoder.test.UITestUtil.Companion.outputDir
import org.junit.jupiter.api.Test
import java.io.File
import java.io.FileOutputStream
import java.io.PrintWriter

/**
 * See Also:
 *  https://github.com/JetBrains/intellij-ui-test-robot
 *  https://joel-costigliola.github.io/assertj/swing/api/org/assertj/swing/core/Robot.html
 */
class UITest {

    @Test
    fun javaTests() {
        if (!canRunTests()) return
        val testOutputFile = File(outputDir, "java.md")
        val out = PrintWriter(FileOutputStream(testOutputFile))
        out.use { out ->
            documentJavaImplementation("Text_to_Morse", "Convert text to Morse code", out, outputDir)
            //documentJavaImplementation("Prime_Numbers", "Print all prime numbers from 1 to 100", out, outputDir)
            //            documentJavaImplementation("Calculate_Pi", "Calculate Pi using the convergence of x = 1+sin x starting at x=3", out, buildDir)
            //            documentJavaImplementation("Java_8", "Demonstrate language features of Java 8", out, buildDir)
            //            documentJavaImplementation("Draw_A_Smile", "Draw a smiley face", out, buildDir)
            //            documentJavaImplementation("Fibonacci_Sequence", "Print the Fibonacci sequence up to 100", out, buildDir)
            //            documentJavaImplementation("Bubble_Sort", "Sort an array of integers using the bubble sort algorithm", out, buildDir)
            //            documentJavaImplementation("Factorial", "Calculate the factorial of a number", out, buildDir)
            //            documentJavaImplementation("Binary_Search", "Search an array of integers using the binary search algorithm", out, buildDir)
            //            documentJavaImplementation("Quick_Sort", "Sort an array of integers using the quick sort algorithm", out, buildDir)
            //            documentJavaImplementation("Linear_Search", "Search an array of integers using the linear search algorithm", out, buildDir)
            //            documentJavaImplementation("Insertion_Sort", "Sort an array of integers using the insertion sort algorithm", out, buildDir)
            //            documentJavaImplementation("Selection_Sort", "Sort an array of integers using the selection sort algorithm", out, buildDir)
            //            documentJavaImplementation("Merge_Sort", "Sort an array of integers using the merge sort algorithm", out, buildDir)
            //            documentJavaImplementation("Heap_Sort", "Sort an array of integers using the heap sort algorithm", out, buildDir)
            //            documentJavaImplementation("Shell_Sort", "Sort an array of integers using the shell sort algorithm", out, buildDir)
            //            documentJavaImplementation("Counting_Sort", "Sort an array of integers using the counting sort algorithm", out, buildDir)
            //            documentJavaImplementation("Radix_Sort", "Sort an array of integers using the radix sort algorithm", out, buildDir)
            //            documentJavaImplementation("Bucket_Sort", "Sort an array of integers using the bucket sort algorithm", out, buildDir)
            //            documentJavaImplementation("Bogo_Sort", "Sort an array of integers using the bogo sort algorithm", out, buildDir)
            //            documentJavaImplementation("Stooge_Sort", "Sort an array of integers using the stooge sort algorithm", out, buildDir)
            //            documentJavaImplementation("Cocktail_Sort", "Sort an array of integers using the cocktail sort algorithm", out, buildDir)
            //            documentJavaImplementation("Comb_Sort", "Sort an array of integers using the comb sort algorithm", out, buildDir)
            //            documentJavaImplementation("Gnome_Sort", "Sort an array of integers using the gnome sort algorithm", out, buildDir)
            //            documentJavaImplementation("Pancake_Sort", "Sort an array of integers using the pancake sort algorithm", out, buildDir)
            //            documentJavaImplementation("Cycle_Sort", "Sort an array of integers using the cycle sort algorithm", out, buildDir)
            //            documentJavaImplementation("Odd_Even_Sort", "Sort an array of integers using the odd-even sort algorithm", out, buildDir)
            //            documentJavaImplementation("Sleep_Sort", "Sort an array of integers using the sleep sort algorithm", out, buildDir)
            //            documentJavaImplementation("Binary_Tree_Sort", "Sort an array of integers using the binary tree sort algorithm", out, buildDir)
            //            documentJavaImplementation("Tim_Sort", "Sort an array of integers using the tim sort algorithm", out, buildDir)
            //            documentJavaImplementation("Pigeonhole_Sort", "Sort an array of integers using the pigeonhole sort algorithm", out, buildDir)
            //            documentJavaImplementation("Strand_Sort", "Sort an array of integers using the strand sort algorithm", out, buildDir)
            //            documentJavaImplementation("Bead_Sort", "Sort an array of integers using the bead sort algorithm", out, buildDir)
            //            documentJavaImplementation("Bitonic_Sort", "Sort an array of integers using the bitonic sort algorithm", out, buildDir)
            //            documentJavaImplementation("Tournament_Sort", "Sort an array of integers using the tournament sort algorithm", out, buildDir)
            //            documentJavaImplementation("Spread_Sort", "Sort an array of integers using the spread sort algorithm", out, buildDir)
            //            documentJavaImplementation("Library_Sort", "Sort an array of integers using the library sort algorithm", out, buildDir)
            //            documentJavaImplementation("Patience_Sort", "Sort an array of integers using the patience sort algorithm", out, buildDir)
            //            documentJavaImplementation("Smooth_Sort", "Sort an array of integers using the smooth sort algorithm", out, buildDir)
            //            documentJavaImplementation("American_Flag_Sort", "Sort an array of integers using the american flag sort algorithm", out, buildDir)
            //            documentJavaImplementation("Binary_Insertion_Sort", "Sort an array of integers using the binary insertion sort algorithm", out, buildDir)
            //            documentJavaImplementation("Block_Sort", "Sort an array of integers using the block sort algorithm", out, buildDir)
            //            documentJavaImplementation("Bozosort", "Sort an array of integers using the bozosort algorithm", out, buildDir)
            //            documentJavaImplementation("Brick_Sort", "Sort an array of integers using the brick sort algorithm", out, buildDir)
            //            documentJavaImplementation("Cocktail_Shaker_Sort", "Sort an array of integers using the cocktail shaker sort algorithm", out, buildDir)
            //            documentJavaImplementation("Gravity_Sort", "Sort an array of integers using the gravity sort algorithm", out, buildDir)
            //            documentJavaImplementation("Library_Sort", "Sort an array of integers using the library sort algorithm", out, buildDir)
            //            documentJavaImplementation("Pancake_Sorting", "Sort an array of integers using the pancake sorting algorithm", out, buildDir)
            //            documentJavaImplementation("Permutation_Sort", "Sort an array of integers using the permutation sort algorithm", out, buildDir)
            //            documentJavaImplementation("Postman_Sort", "Sort an array of integers using the postman sort algorithm", out, buildDir)
            //            documentJavaImplementation("Sleep_Sort", "Sort an array of integers using the sleep sort algorithm", out, buildDir)
            //            documentJavaImplementation("Spaghetti_Sort", "Sort an array of integers using the spaghetti sort algorithm", out, buildDir)
            //            documentJavaImplementation("Staircase_Sort", "Sort an array of integers using the staircase sort algorithm", out, buildDir)
            //            documentJavaImplementation("Strand_Sort", "Sort an array of integers using the strand sort algorithm", out, buildDir)
            //            documentJavaImplementation("Tree_Sort", "Sort an array of integers using the tree sort algorithm", out, buildDir)
            //            documentJavaImplementation("UnShuffle_Sort", "Sort an array of integers using the unshuffle sort algorithm", out, buildDir)
            //            documentJavaImplementation("Binary_Indexed_Tree", "Implement a binary indexed tree", out, buildDir)
            //            documentJavaImplementation("Binary_Search_Tree", "Implement a binary search tree", out, buildDir)
            //            documentJavaImplementation("AVL_Tree", "Implement an AVL tree", out, buildDir)
            //            documentJavaImplementation("Red_Black_Tree", "Implement a red-black tree", out, buildDir)
            //            documentJavaImplementation("Splay_Tree", "Implement a splay tree", out, buildDir)
            //            documentJavaImplementation("Trie", "Implement a trie", out, buildDir)
            //            documentJavaImplementation("KD_Tree", "Implement a KD tree", out, buildDir)
            //            documentJavaImplementation("B_Tree", "Implement a B-tree", out, buildDir)
            //            documentJavaImplementation("Binary_Heap", "Implement a binary heap", out, buildDir)
            //            documentJavaImplementation("Fibonacci_Heap", "Implement a Fibonacci heap", out, buildDir)
            //            documentJavaImplementation("Hash_Table", "Implement a hash table", out, buildDir)
            //            documentJavaImplementation("Graph", "Implement a graph", out, buildDir)
            //            documentJavaImplementation("Disjoint_Set", "Implement a disjoint set", out, buildDir)
            //            documentJavaImplementation("Priority_Queue", "Implement a priority queue", out, buildDir)
            //            documentJavaImplementation("Stack", "Implement a stack", out, buildDir)
            //            documentJavaImplementation("Queue", "Implement a queue", out, buildDir)
            //            documentJavaImplementation("Linked_List", "Implement a linked list", out, buildDir)
            //            documentJavaImplementation("Circular_Linked_List", "Implement a circular linked list", out, buildDir)
            //            documentJavaImplementation("Doubly_Linked_List", "Implement a doubly linked list", out, buildDir)
            //            documentJavaImplementation("Array_List", "Implement an array list", out, buildDir)
            //            documentJavaImplementation("Binary_Search_Tree", "Implement a binary search tree", out, buildDir)
            //            documentJavaImplementation("AVL_Tree", "Implement an AVL tree", out, buildDir)
            //            documentJavaImplementation("Red_Black_Tree", "Implement a red-black tree", out, buildDir)
            //            documentJavaImplementation("Splay_Tree", "Implement a splay tree", out, buildDir)
            //            documentJavaImplementation("Trie", "Implement a trie", out, buildDir)
            //            documentJavaImplementation("KD_Tree", "Implement a KD tree", out, buildDir)
            //            documentJavaImplementation("B_Tree", "Implement a B-tree", out, buildDir)
            //            documentJavaImplementation("Binary_Heap", "Implement a binary heap", out, buildDir)
            //            documentJavaImplementation("Fibonacci_Heap", "Implement a Fibonacci heap", out, buildDir)
            //            documentJavaImplementation("Hash_Table", "Implement a hash table", out, buildDir)
            //            documentJavaImplementation("Graph", "Implement a graph", out, buildDir)
            //            documentJavaImplementation("Disjoint_Set", "Implement a disjoint set", out, buildDir)
            //            documentJavaImplementation("Priority_Queue", "Implement a priority queue", out, buildDir)
            //            documentJavaImplementation("Stack", "Implement a stack", out, buildDir)
            //            documentJavaImplementation("Queue", "Implement a queue", out, buildDir)
            //            documentJavaImplementation("Linked_List", "Implement a linked list", out, buildDir)
            //            documentJavaImplementation("Circular_Linked_List", "Implement a circular linked list", out, buildDir)
            //            documentJavaImplementation("Doubly_Linked_List", "Implement a doubly linked list", out, buildDir)
            //            documentJavaImplementation("Array_List", "Implement an array list", out, buildDir)
            //            documentJavaImplementation("Binary_Search_Tree", "Implement a binary search tree", out, buildDir)
            //            documentJavaImplementation("AVL_Tree", "Implement an AVL tree", out, buildDir)
            //            documentJavaImplementation("Red_Black_Tree", "Implement a red-black tree", out, buildDir)
            //            documentJavaImplementation("Splay_Tree", "Implement a splay tree", out, buildDir)
        }
    }

    @Test
    fun markdownTests() {
        if (!canRunTests()) return
        val testOutputFile = File(outputDir, "markdown.md")
        val out = PrintWriter(FileOutputStream(testOutputFile))
        //documentMarkdownTableOps("State_Details", "Data Table of State Details", out, outputDir)
        documentMarkdownListAppend("Puppy_Playtime", "Top 10 Best Ways to Play with Puppies", arrayOf(), out, outputDir)
        out.close() // Close file
    }

    @Test
    fun plaintextTests() {
        if (!canRunTests()) return
        val testOutputFile = File(outputDir, "plaintext.md")
        val out = PrintWriter(FileOutputStream(testOutputFile))
        documentTextAppend("Once_Upon_A_Time", "Once upon a time", out, outputDir)
        out.close() // Close file
    }

}
