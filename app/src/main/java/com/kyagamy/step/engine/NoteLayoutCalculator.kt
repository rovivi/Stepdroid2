package com.kyagamy.step.engine

import android.graphics.Point
import kotlin.math.abs

class NoteLayoutCalculator {
    data class Layout(
        val sizeX: Int,
        val sizeY: Int,
        val sizeNote: Int,
        val scaledNoteSize: Int,
        val offsetX: Int,
        val offsetY: Int,
        val posInitialX: Int,
        val startValueY: Int
    )

    companion object {
        private const val STEPS_Y_COUNT = 9.3913f
        private const val RECEPTOR_Y_FACTOR = 0.7f
        private const val NOTE_SCALE_FACTOR = 1.245f
        private const val ASPECT_RATIO_16_9_CALC = 1.77777778f

        fun calculate(
            gameMode: StepsDrawerGL.GameMode,
            aspectRatio: String,
            landScape: Boolean,
            screenSize: Point
        ): Layout {
            var sizeX: Int
            var sizeY: Int
            var offsetX = 0
            var offsetY = 0

            if (landScape) {
                sizeY = screenSize.y
                sizeX = (screenSize.y * ASPECT_RATIO_16_9_CALC).toInt()
                offsetX = ((screenSize.x - sizeX) / 2f).toInt()

                if (sizeX > screenSize.x) {
                    sizeY = (screenSize.x / ASPECT_RATIO_16_9_CALC).toInt()
                    sizeX = (sizeY * ASPECT_RATIO_16_9_CALC).toInt()
                    offsetX = abs(((screenSize.x - sizeX) / 2f).toInt())
                    offsetY = ((screenSize.y - sizeY) / 2f).toInt()
                }

                sizeX += offsetX / 2
                sizeY += offsetY
            } else {
                sizeY = screenSize.y / 2
                sizeX = screenSize.x

                if ((sizeY / STEPS_Y_COUNT).toInt() * gameMode.steps > sizeX) {
                    sizeY = (sizeX / (gameMode.steps + 0.2) * STEPS_Y_COUNT).toInt()
                    offsetY = screenSize.y - sizeY
                }
            }

            val sizeNote = (sizeY / STEPS_Y_COUNT).toInt()
            val scaledNoteSize = (sizeNote * NOTE_SCALE_FACTOR).toInt()
            val posInitialX = (((sizeX) - (sizeNote * gameMode.steps))) / 2 + offsetX / 2
            val startValueY = (sizeNote * RECEPTOR_Y_FACTOR).toInt()

            return Layout(
                sizeX,
                sizeY,
                sizeNote,
                scaledNoteSize,
                offsetX,
                offsetY,
                posInitialX,
                startValueY
            )
        }
    }
}
