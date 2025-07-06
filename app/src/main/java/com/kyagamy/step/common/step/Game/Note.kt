package game

import com.kyagamy.step.common.step.Game.GameRow
import game.NoteType


class Note {
    var type: NoteType = NoteType.EMPTY
    var player: Byte = 0
    var skin: Byte = 0
    var sudden: Boolean = false
    var fake: Boolean = false
    var hidden: Boolean = false
    var vanish: Boolean = false
    // var effects :SDEffect[] //Will be implemented in the future
    var rowOrigin: GameRow?=null
    var rowEnd: GameRow?=null


    companion object {
        fun CloneNote(baseNote: Note): Note {
            val newNote = Note()
            newNote.rowOrigin=baseNote.rowOrigin
            newNote.rowEnd=baseNote.rowEnd

            newNote.fake = baseNote.fake
            newNote.vanish = baseNote.vanish
            newNote.hidden = baseNote.hidden
            newNote.skin = baseNote.skin
            newNote.player = baseNote.player
            newNote.sudden = baseNote.sudden
            newNote.type = baseNote.type
            return newNote
        }
    }

}