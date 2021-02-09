package com.kyagamy.step.common.step.Game

import game.Note
import kotlin.collections.ArrayList

const val NOT_DRAWABLE = -99999

class GameRow {

    var notes: ArrayList<Note>? = null
    var currentBeat: Double = 0.0
    var modifiers: HashMap<String, ArrayList<Double>>? = null
    var hasPressed: Boolean = false
    private var firstPressedTime: Double? = null
    private var posY: Int = NOT_DRAWABLE

    override fun toString(): String {
        var noteStr = ""
        var modStr = ""
        notes?.forEach { x -> noteStr += x.type }
        modifiers?.forEach { mod ->
            modStr = "type: " + mod.key + " val: " + mod.value.toString()
        }
        return "GameRow(---notes=$noteStr,---- currentBeat=$currentBeat, modifiers=$modStr)"
    }

    fun getPosY(): Int {
        var aux = posY + 0
        // posY=-9
        return aux
    }

    fun setPosY(value: Int) {
        posY = value
    }

    fun setPressedTime(value: Double) {
        if (firstPressedTime == null) firstPressedTime=value
    }
    fun getPressedTime():Double?{return firstPressedTime}

}