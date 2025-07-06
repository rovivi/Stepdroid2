package com.kyagamy.step

import com.kyagamy.step.common.step.Parsers.FileSSC
import game.NoteType
import org.junit.Assert.*
import org.junit.Test

class ParserLongNoteTest {
    @Test
    fun parse_long_note_links() {
        val data = """#NOTEDATA:;\n#STEPSTYPE:pump-single;\n#NOTES:\n0000\n2000\n0000\n3000\n;"""
        val parser = FileSSC(data, 0)
        val stepObject = parser.parseData(false)
        val steps = stepObject.steps
        val startRow = steps.first { row -> row.notes?.any { it.noteType == NoteType.LONG_START } == true }
        val endRow = steps.first { row -> row.notes?.any { it.noteType == NoteType.LONG_END } == true }
        val startNote = startRow.notes!!.first { it.noteType == NoteType.LONG_START }
        val endNote = endRow.notes!!.first { it.noteType == NoteType.LONG_END }
        assertSame(startRow, endNote.rowOrigin)
        assertSame(endRow, startNote.rowEnd)
        assertEquals(1.0, startRow.currentBeat, 0.0001)
        assertEquals(3.0, endRow.currentBeat, 0.0001)
    }
}
