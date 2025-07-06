package com.kyagamy.step

import com.kyagamy.step.common.step.Parsers.FileSSC
import game.NoteType
import org.junit.Assert.assertEquals
import org.junit.Test

class FileSSCParserTest {
    @Test
    fun parseSimpleHold_shouldLinkStartAndEnd() {
        val data = """
            #NOTEDATA:;
            #STEPSTYPE:pump-single;
            #NOTES:
            2000
            0000
            3000
            0000
            ;
        """.trimIndent()
        val parser = FileSSC(data, 0)
        val stepObj = parser.parseData(false)
        val rows = stepObj.steps
        val startNote = rows[0].notes!![0]
        val endNote = rows[2].notes!![0]
        assertEquals(NoteType.LONG_START, startNote.type)
        assertEquals(rows[2], startNote.rowEnd)
        assertEquals(rows[0], endNote.rowOrigin)
    }
}

    @Test
    fun parseSimpleHold_hasCorrectBeats() {
        val data = """
            #NOTEDATA:;
            #STEPSTYPE:pump-single;
            #NOTES:
            2000
            0000
            3000
            0000
            ;
        """.trimIndent()
        val parser = FileSSC(data, 0)
        val stepObj = parser.parseData(false)
        val rows = stepObj.steps
        assertEquals(0.0, rows[0].currentBeat, 0.0)
        assertEquals(1.0, rows[1].currentBeat, 0.0)
        assertEquals(2.0, rows[2].currentBeat, 0.0)
    }
}
