package com.kyagamy.step.game.newplayer

import game.GameRow

/***
 * This class provide tools to evalate to suppuort the game state
 */
class Evaluator {

    companion object {
         fun containNoteType(row: GameRow, typeNote: Short): Boolean {
//
             if (row.notes!=null)
             {
                 return true
                 for (x in row.notes!!) if (x.type == typeNote) return true
             }
            return false
        }




    }
}

