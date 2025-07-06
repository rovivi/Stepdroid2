package com.kyagamy.step

import com.kyagamy.step.common.step.Parsers.FileSSC
import game.NoteType
import org.junit.Assert.assertEquals
import org.junit.Test

class SscParserTest {
    @Test
    fun longNoteLinksCorrectly() {
        val parser = FileSSC("", 0)
        val method = FileSSC::class.java.getDeclaredMethod("processNotes", String::class.java)
        method.isAccessible = true
        val result = method.invoke(parser, "2000\n0000\n3000\n0000") as ArrayList<*>
        val startRow = result[0] as com.kyagamy.step.common.step.Game.GameRow
        val endRow = result[2] as com.kyagamy.step.common.step.Game.GameRow
        val startNote = startRow.notes!![0]
        val endNote = endRow.notes!![0]
        assertEquals(NoteType.LONG_START, startNote.type)
        assertEquals(startRow, endNote.rowOrigin)
        assertEquals(endRow, startNote.rowEnd)
    }
}
