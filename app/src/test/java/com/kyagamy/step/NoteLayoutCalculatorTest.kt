package com.kyagamy.step

import android.graphics.Point
import com.kyagamy.step.engine.NoteLayoutCalculator
import com.kyagamy.step.engine.StepsDrawerGL
import org.junit.Assert.*
import org.junit.Test

class NoteLayoutCalculatorTest {
    @Test
    fun layout_basic_values() {
        val screen = Point(1080, 1920)
        val layout = NoteLayoutCalculator.calculate(
            StepsDrawerGL.GameMode.PUMP_SINGLE,
            "16:9",
            true,
            screen
        )
        assertTrue(layout.sizeX > 0)
        assertTrue(layout.sizeY > 0)
        assertTrue(layout.scaledNoteSize > layout.sizeNote)
    }
}
