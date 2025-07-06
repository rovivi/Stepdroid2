package com.kyagamy.step.game.newplayer

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import com.kyagamy.step.common.step.CommonSteps.Companion.NOTE_LONG_BODY
import com.kyagamy.step.common.step.CommonSteps.Companion.NOTE_LONG_END
import com.kyagamy.step.common.step.CommonSteps.Companion.NOTE_LONG_PRESSED
import com.kyagamy.step.common.step.CommonSteps.Companion.NOTE_LONG_START
import com.kyagamy.step.common.step.CommonSteps.Companion.NOTE_MINE
import com.kyagamy.step.common.step.CommonSteps.Companion.NOTE_PRESSED
import com.kyagamy.step.common.step.CommonSteps.Companion.NOTE_TAP
import game.NoteType
import com.kyagamy.step.common.step.Game.GameRow
import java.util.*

/***
 * This class provide tools to evalate to suppuort the game state
 */
class Evaluator {

    companion object {
        var PURRFECT = 0
        var PERFECT = 0
        var GREAT = 0
        var GOOD = 0
        var BAD = 0
        var MISS = 0
        var MAX_COMBO = 0
        var imagePath = ""
        var songName = ""
        var bitmap:Bitmap? = null


        fun resetScore() {
            PURRFECT = 0
            PERFECT = 0
            GREAT = 0
            GOOD = 0
            BAD = 0
            MISS = 0
            MAX_COMBO = 0
        }

        fun getTotalScore(): Float {
            var total = PERFECT + GREAT + GOOD + BAD + MISS;
            var sum = PERFECT + GREAT * 0.8 + GOOD * 0.5 + BAD * 0.3;

            if (total == 0) total = 1

            return (100.0 * sum / total).toFloat()
        }

        fun containNoteType(row: GameRow, typeNote: NoteType): Boolean {
            if (row.notes != null) {
                for (x in row.notes!!) {
                    if (x.noteType == typeNote && !x.fake)
                        return true
                }
            }
            return false
        }

        fun containNoteToEvaluate(row: GameRow): Boolean {
//
            if (row.notes != null) {
                for (x in row.notes!!) {
                    if ((x.noteType == NOTE_LONG_BODY ||
                                x.noteType == NOTE_LONG_START ||
                                x.noteType == NOTE_LONG_END ||
                                x.noteType == NOTE_TAP)
                        && !x.fake
                    )
                        return true
                }
            }
            return false
        }


        fun getRank(): String {
            val totalScore = getTotalScore()
            if (totalScore >= 100)
                return "SSS"
            if (GOOD == 0 && BAD == 0 && MISS == 0)
                return "SS"
            if (MISS == 0)
                return "S"
            if (totalScore >= 90)
                return "A"
            if (totalScore >= 80)
                return "B"
            if (totalScore >= 70)
                return "C"
            if (totalScore >= 60)
                return "D"
            return "F"
        }

        fun containsNoteTap(row: GameRow): Boolean {
            return containNoteType(row, NOTE_TAP)
        }


        fun containsNotePressed(row: GameRow): Boolean {
            return containNoteType(row, NOTE_PRESSED)
        }

        fun containsNoteLongPressed(row: GameRow): Boolean {
            return containNoteType(row, NOTE_LONG_PRESSED)
        }


        fun containsNoteMine(row: GameRow): Boolean {
            return containNoteType(row, NOTE_MINE)
        }

        fun containNoteLong(row: GameRow): Boolean {
//
            if (row.notes != null) {
                for (x in row.notes!!) {
                    if ((x.noteType == NOTE_LONG_END || x.noteType == NOTE_LONG_START || x.noteType == NOTE_LONG_BODY) && !x.fake)
                        return true
                }
            }
            return false
        }


    }
}

