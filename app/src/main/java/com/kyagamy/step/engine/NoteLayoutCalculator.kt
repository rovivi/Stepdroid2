package com.kyagamy.step.engine

import android.graphics.Point
import kotlin.math.abs

class NoteLayoutCalculator(private val steps: Int) {
    data class Layout(
        val sizeX: Int,
        val sizeY: Int,
        val offsetX: Int,
        val offsetY: Int,
        val sizeNote: Int,
        val scaledNote: Int,
        val posInitialX: Int
    )

    fun calculate(aspectRatio: String, landScape: Boolean, screenSize: Point): Layout {
        var sizeX: Int
        var sizeY: Int
        var offsetX = 0
        var offsetY = 0
        if (landScape) {
            sizeY = screenSize.y
            sizeX = (screenSize.y * StepsDrawerGL.ASPECT_RATIO_16_9_CALC).toInt()
            offsetX = ((screenSize.x - sizeX) / 2f).toInt()
            if (sizeX > screenSize.x) {
                sizeY = (screenSize.x / StepsDrawerGL.ASPECT_RATIO_16_9_CALC).toInt()
                sizeX = (sizeY * StepsDrawerGL.ASPECT_RATIO_16_9_CALC).toInt()
                offsetX = abs(((screenSize.x - sizeX) / 2f).toInt())
                offsetY = ((screenSize.y - sizeY) / 2f).toInt()
            }
            sizeX += offsetX / 2
            sizeY += offsetY
        } else {
            sizeY = screenSize.y / 2
            sizeX = screenSize.x
            if ((sizeY / StepsDrawerGL.STEPS_Y_COUNT).toInt() * steps > sizeX) {
                sizeY = (sizeX / (steps + 0.2) * StepsDrawerGL.STEPS_Y_COUNT).toInt()
                offsetY = screenSize.y - sizeY
            }
        }
        val sizeNote = (sizeY / StepsDrawerGL.STEPS_Y_COUNT).toInt()
        val scaledNote = (sizeNote * StepsDrawerGL.NOTE_SCALE_FACTOR).toInt()
        val posInitialX = (((sizeX) - (sizeNote * steps))) / 2 + offsetX / 2
        return Layout(sizeX, sizeY, offsetX, offsetY, sizeNote, scaledNote, posInitialX)
    }
}
