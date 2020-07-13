package com.kyagamy.step.game.newplayer

import com.kyagamy.step.common.step.CommonSteps.Companion.NOTE_LONG_BODY
import com.kyagamy.step.common.step.CommonSteps.Companion.NOTE_LONG_END
import com.kyagamy.step.common.step.CommonSteps.Companion.NOTE_LONG_START
import com.kyagamy.step.common.step.CommonSteps.Companion.NOTE_MINE
import com.kyagamy.step.common.step.CommonSteps.Companion.NOTE_PRESSED
import com.kyagamy.step.common.step.CommonSteps.Companion.NOTE_TAP
import com.kyagamy.step.common.step.Game.GameRow

/***
 * This class provide tools to evalate to suppuort the game state
 */
class Evaluator {

    companion object {
        fun containNoteType(row: GameRow, typeNote: Short): Boolean {
//
            if (row.notes != null) {
                for (x in row.notes!!) {
                    if (x.type == typeNote && !x.fake)
                        return true
                }
            }
            return false
        }


        fun containNoteToEvaluate(row: GameRow): Boolean {
//
            if (row.notes != null) {
                for (x in row.notes!!) {
                    if ((x.type == NOTE_LONG_BODY ||
                        x.type == NOTE_LONG_START ||
                        x.type == NOTE_LONG_END ||
                        x.type == NOTE_TAP)
                        && !x.fake
                    )
                        return true
                }
            }
            return false
        }


        fun containsNoteTap(row: GameRow): Boolean {
            return containNoteType(row, NOTE_TAP)
        }


        fun containsNotePressed(row: GameRow): Boolean {
            return containNoteType(row, NOTE_PRESSED)
        }


        fun containsNoteMine(row: GameRow): Boolean {
            return containNoteType(row, NOTE_MINE)
        }

        fun containNoteLong(row: GameRow): Boolean {
//
            if (row.notes != null) {
                for (x in row.notes!!) {
                    if ((x.type == NOTE_LONG_END || x.type == NOTE_LONG_START || x.type == NOTE_LONG_BODY) && !x.fake)
                        return true
                }
            }
            return false
        }


    }
}

