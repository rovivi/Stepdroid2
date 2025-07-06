package com.kyagamy.step.parser

import com.kyagamy.step.common.io.LocalFileSource
import com.kyagamy.step.common.step.Parsers.FileSSC
import org.junit.Assert.assertEquals
import org.junit.Test
import java.io.File

class FileSSCTest {
    @Test
    fun parse_simple_file() {
        val file = File("src/test/resources/sample.ssc")
        val source = LocalFileSource(file)
        val data = source.readText()
        source.close()
        val step = FileSSC(data, 0).parseData(false)
        assertEquals("TestSong", step.songMetadata["TITLE"])
    }
}
