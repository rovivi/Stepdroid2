package com.kyagamy.step.io

import com.kyagamy.step.common.io.LocalFileSource
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test
import java.io.File
import java.io.FileNotFoundException

class LocalFileSourceTest {
    @Test
    fun read_existing_file() {
        val temp = File.createTempFile("test", ".txt")
        temp.writeText("hello")
        val source = LocalFileSource(temp)
        val text = source.readText()
        source.close()
        assertEquals("hello", text)
        temp.delete()
    }

    @Test
    fun read_nonexistent_file_throws() {
        val file = File("/tmp/does_not_exist.txt")
        val source = LocalFileSource(file)
        assertThrows(FileNotFoundException::class.java) {
            source.readText()
        }
    }
}
