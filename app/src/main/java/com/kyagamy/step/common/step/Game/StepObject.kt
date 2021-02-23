package game

import android.util.Log
import com.kyagamy.step.common.step.CommonSteps.Companion.getFirstBPM
import com.kyagamy.step.common.step.Game.GameRow
import com.kyagamy.step.room.entities.Level
import java.util.*
import kotlin.collections.ArrayList

class StepObject {
    /**Game Data*/
    lateinit var steps: ArrayList<GameRow>
    var songMetadata: Map<String, String> = HashMap()
    var levelMetadata: Map<String, String> = HashMap()
    var name: String = ""
    var offset: Double = 0.0
    var stepType: String = ""


    /**Media Data*/
    var path: String = ""
    var songFileName: String = ""
    var bgImageFileName: String = ""

    /**ORM UTIL */
    var levelList: ArrayList<Level>? = null


    //Functions

    fun getMusicPath(): String {
        return path + "/" + songMetadata["MUSIC"]
    }

    ///TEST AREA
//ATTACKS= setMetadata(stepData.chartsInfo[nchar].get("ATTACKS"),stepData.songInfo.get("ATTACKS"));

    fun getSongOffset(): Float {

        if (levelMetadata["OFFSET"] != null) {
            val xof: String = levelMetadata["OFFSET"].toString()
            offset = xof.toFloat().toDouble()
        } else {
            val xof: String = songMetadata["OFFSET"].toString()
            offset += xof.toFloat()
        }
//    offset += (com.example.rodrigo.sgame.CommonGame.Common.OFFSET as kotlin.Float / 1000)
        return offset.toFloat()
    }

    fun getBgChanges(): String {

        return if (levelMetadata["BGCHANGES"] != null)
            levelMetadata["BGCHANGES"].toString()
        else
            songMetadata["BGCHANGES"].toString()
    }

    fun getInitialBPM(): Double {
        var x = 0;
        while (true) {
            if (steps[x].modifiers != null && steps[x].modifiers?.get("BPMS") != null)
                return steps[x].modifiers?.get("BPMS")?.get(1)!!
            x++
        }
    }


    fun getDisplayBPM(): Double {
        try {
            if (songMetadata["DISPLAYBPM"] != null && songMetadata["DISPLAYBPM"].toString()
                    .toDouble() > 0
            )
                return songMetadata["DISPLAYBPM"].toString().toDouble()
        } catch (ex: Exception) {
            Log.e("STEPDROID", ex.stackTraceToString())
        }
        try {
            if (levelMetadata["DISPLAYBPM"] != null
                && levelMetadata["DISPLAYBPM"].toString()
                    .toDouble() > 0
            )
                return levelMetadata["DISPLAYBPM"].toString().toDouble()
        } catch (ex: Exception) {

        }

        try {
            if (songMetadata["BPMS"] != null)
                return getFirstBPM(songMetadata["BPMS"].toString())
        } catch (ex: Exception) {
            Log.e("STEPDROID", ex.stackTraceToString())
        }
        try {
            if (levelMetadata["BPMS"] != null)
                return getFirstBPM(levelMetadata["BPMS"].toString())
        } catch (ex: Exception) {
            Log.e("STEPDROID", ex.stackTraceToString())
        }
        return -1500.0

    }

}


