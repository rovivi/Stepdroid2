package com.kyagamy.step

import org.junit.Test
import org.junit.Assert.*
import com.kyagamy.step.common.step.Parsers.FileSSC
import game.NoteType

/**
 * Example local unit test, which will execute on the development machine (host).
 *
 * See [testing documentation](http://d.android.com/tools/testing).
 */
class ExampleUnitTest {
    @Test
    fun parseLongNoteLinksRows() {
        val ssc = """
#NOTEDATA:;
#STEPSTYPE:pump-single;
#NOTES:
0000
2000
3000
0000,
0000
0000
0000
0000;
"""
        val parser = com.kyagamy.step.common.step.Parsers.FileSSC(ssc, 0)
        val step = parser.parseData(false)
        val steps = step.steps
        val startRow = steps[1]
        val endRow = steps[2]
        val note = startRow.notes!![0]
        assertEquals(game.NoteType.LONG_START, note.noteType)
        assertEquals(endRow, note.rowEnd)
        assertEquals(startRow, steps[1])
    }
}
