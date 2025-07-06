package com.kyagamy.step.common.step.Parsers


import com.kyagamy.step.common.step.CommonSteps
import com.kyagamy.step.common.step.CommonSteps.Companion.getModifiersSM
import com.kyagamy.step.room.entities.Level
import com.kyagamy.step.common.step.Game.GameRow
import game.Note
import game.NoteType
import game.StepObject
import parsers.StepFile
import java.lang.Exception
import java.util.regex.Pattern
import kotlin.collections.ArrayList
import kotlin.collections.HashMap

class FileSSC(override var pathFile: String, override var indexStep: Int) : StepFile {

    override fun writeFile(path: String) {
    }

    override fun parseData(fastMode: Boolean): StepObject {
        //var stringData = StepFile.UtilsSteps.pathToString(pathFile)
        var stringData = pathFile
        val songMetaData: HashMap<String, String> = HashMap()
        val levelMetaData: HashMap<String, String> = HashMap()
        val modifiers: HashMap<String, ArrayList<ArrayList<Double>>> = HashMap()
        val stepObject = StepObject()
        stepObject.levelMetadata = HashMap()
        var steps: ArrayList<GameRow> = arrayListOf()

        //new variables tos save room orm
        var listLevels = ArrayList<Level>()
        var auxIndexToSaveORM = 0;


        //Se limpian los comen
        // tarios
        stringData =
                stringData.replace(Regex("(\\s+//-([^;]+)\\s)|(//[\\s+]measure\\s[0-9]+\\s)"), "")

        //Se crea el matcher Para Seccionar el Regex
        val matcher = Pattern.compile("#([^;]+);").matcher(stringData)
        var indexLevel = -1//

        //Parse for the win
        while (matcher.find()) {
            val currentTag = matcher.group().split(":")
            val key = currentTag[0].replace("#", "")
            val value = currentTag[1].replace(";", "")
            if (key == "NOTEDATA") {
                indexLevel++
            }//next step
            if (!fastMode) {
                when (indexLevel) {
                    indexStep -> when (key) {
                        "NOTES" -> steps = processNotes(value)
                        "STEPSTYPE" -> stepObject.stepType = value
                        "BPMS", "STOPS", "DELAYS", "WARPS", "TIMESIGNATURES", "TICKCOUNTS", "COMBOS", "SPEEDS", "SCROLLS" -> {
                            if (value != "") modifiers[key] = getModifiersSM(value)
                        }
                        else -> {
                            levelMetaData[key] = value
                        }
                    }
                    -1 -> {
                        when (key) {//Si se tienen effectos globales
                            "BPMS", "STOPS", "DELAYS", "WARPS", "TIMESIGNATURES", "TICKCOUNTS", "COMBOS", "SPEEDS", "SCROLLS" ->
                                if (value != "") modifiers[key] = getModifiersSM(value)
                            else -> songMetaData[key] = value
                        }
                    }
                }
            } else {
                if (indexLevel != -1) {//means not LEVEL TAG
                    if (auxIndexToSaveORM != indexLevel) {//when change level
                        val meter = levelMetaData["METER"]?.toIntOrNull() ?: 0
                        listLevels.add(
                            Level(
                                0,
                                auxIndexToSaveORM,
                                meter,
                                levelMetaData["CREDIT"] ?: "",
                                levelMetaData["STEPSTYPE"] ?: "",
                                levelMetaData["DESCRIPTION"] ?: "",
                                levelMetaData["CHARTNAME"] ?: "",
                                -1,
                                null
                            )
                        )
                        auxIndexToSaveORM++
                    } else {
                        levelMetaData[key] = value

                    }

                } else
                    songMetaData[key] = value

            }
        }
        try {//try to add last level
            val meter = levelMetaData["METER"]?.toIntOrNull() ?: 0
            listLevels.add(
                Level(
                    0,
                    auxIndexToSaveORM,
                    meter,
                    levelMetaData["CREDIT"] ?: "",
                    levelMetaData["STEPSTYPE"] ?: "",
                    levelMetaData["DESCRIPTION"] ?: "",
                    levelMetaData["CHARTNAME"] ?: "",
                    -1,
                    null
                )
            )
        } catch (ex: Exception) {
        }

        /**End Parsing */
        /**Start Setting effects*/
        modifiers.forEach { modifier ->
            when (modifier.key) {
                "BPMS", "WARPS", "TICKCOUNTS", "SPEEDS", "SCROLLS", "STOPS", "DELAYS", "COMBOS" -> {
                    modifier.value.forEach { values ->
                        //effect List
                        println("processing " + modifier.key)
                        val beat = values[0]
                        val element =
                            steps.firstOrNull { row ->
                                CommonSteps.almostEqual(
                                    row.currentBeat,
                                    beat
                                )
                            }
                        val index = (steps.indexOf(element))
                        if (index != -1) {
                            if (steps[index].modifiers == null) {
                                steps[index].modifiers = HashMap()
                            }
                            steps[index].modifiers?.put(modifier.key, values)
                        } else {
                            val newRow = GameRow()
                            newRow.currentBeat = values[0]
                            newRow.modifiers = HashMap()
                            newRow.modifiers?.put(modifier.key, values)
                            steps.add(newRow)
                        }
                    }
                }
            }
            CommonSteps.orderByBeat(steps)
        }

        //se ordernan
        CommonSteps.orderByBeat(steps)

        CommonSteps.stopsToScroll(steps)//Se aplican los stops
        CommonSteps.orderByBeat(steps)
        // steps.filter { x -> x.notes != null }.forEach { x -> println(x.toString()) }
        stepObject.steps = steps
        /**End Apply effects*/
        stepObject.songMetadata = songMetaData
        stepObject.levelMetadata = levelMetaData
        stepObject.levelList = listLevels
        //stepObject.steps.forEach { x -> println(x) }
        return stepObject
    }


    private fun processNotes(notes: String): ArrayList<GameRow> {
        val data = notes.replace(" ", "").replace("\n\n", "\n")
        val listGameRow = ArrayList<GameRow>()
        val blocks = data.split(",")
        var currentBeat = 0.0
        val openLongNotes: MutableMap<Int, Note> = mutableMapOf()
        blocks.forEach { block ->
            val rowsStep = block.split("\n").filter { x -> x != "" }
            val blockSize = rowsStep.size
            rowsStep.forEach { row ->
                if (!checkEmptyRow(row)) {
                    val gameRow = stringToGameRow(row)
                    gameRow.currentBeat = currentBeat

                    //scan form game row
                    gameRow.notes?.forEachIndexed { index, note ->
                        when (note.type) {
                            NoteType.LONG_START -> {
                                note.rowOrigin = gameRow
                                openLongNotes[index] = note
                            }
                            NoteType.LONG_END -> {
                                val head = openLongNotes[index]
                                head?.rowEnd = gameRow
                                note.rowOrigin = head?.rowOrigin
                                openLongNotes.remove(index)
                            }
                            else -> {}
                        }
                    }
                    listGameRow.add(gameRow)
                }
                currentBeat += 4.0 / blockSize
            }
        }
        //se añadie un ultimo row para que finalize
        var lastRow = GameRow()
        lastRow.currentBeat = currentBeat + 120
        listGameRow.add(lastRow)
        return listGameRow
    }

    private fun stringToGameRow(data: String): GameRow {
        val gameRow = GameRow()
        var row = data.replace("{x}", "f")
        val re = Regex("\\{([^\\}]+)\\}")
        val matcher = Pattern.compile(re.toString()).matcher(row)
        val arrayNotes = ArrayList<Note>()
        val arrayEspecialNote = ArrayList<String>()
        while (matcher.find()) {
            arrayEspecialNote.add(matcher.group())
        }
        row = row.replace(re, "E")
        var indexEspecialNote = 0
        row.forEach { l ->
            if (l == 'E') {
                arrayNotes.add(specialToNote(arrayEspecialNote[indexEspecialNote]))
                indexEspecialNote++
            } else {
                arrayNotes.add(charToNote(l))
            }
        }
        gameRow.notes = arrayNotes
        return gameRow
    }

    private fun checkEmptyRow(row: String): Boolean {
        return Regex("(0+)").matches(row)
    }

    private fun charToNote(caracter: Char): Note {
        val note = Note()
        var charCode = NoteType.EMPTY
        when (caracter) {
            '1' -> charCode = NoteType.TAP
            '2', '6' -> charCode = NoteType.LONG_START
            '3' -> charCode = NoteType.LONG_END
            'M' -> charCode = NoteType.MINE
            'F', 'f' -> charCode = NoteType.FAKE
            'V' -> {
                charCode = NoteType.TAP
                note.vanish = true
            }
            'h' -> {
                charCode = NoteType.TAP
                note.hidden = true
            }
            'x' -> {
                charCode = NoteType.LONG_START
                note.player = CommonSteps.PLAYER_1
            }
            'X' -> {
                charCode = NoteType.TAP
                note.player = CommonSteps.PLAYER_1
            }
            'y' -> {
                charCode = NoteType.LONG_START
                note.player = CommonSteps.PLAYER_2
            }
            'Y' -> {
                charCode = NoteType.TAP
                note.player = CommonSteps.PLAYER_2
            }
            'z' -> {
                charCode = NoteType.LONG_START
                note.player = CommonSteps.PLAYER_3
            }
            'Z' -> {
                charCode = NoteType.TAP
                note.player = CommonSteps.PLAYER_3
            }
        }
        note.type = charCode
        return note
    }

    private fun specialToNote(re: String): Note {
        val noteString = re.replace("{", "").replace("}", "").replace("|", "")
        return try {
            val note = charToNote(noteString[0])
            when (noteString[1]) {
                'v', 'V' -> note.vanish = true
                'h', 'H' -> note.hidden = true
                's', 'S' -> note.sudden = true
            }
            if (noteString[2] == '1') note.fake = true
            note
        } catch (ex: Exception) {
            ex.printStackTrace()
            Note()
        }
    }


}