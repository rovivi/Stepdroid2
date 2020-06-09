package com.kyagamy.step.common.step.CommonGame

/**
 * Created : Rodrigo Vidal
 * This class is used to manage the preferences in the game
 */
object ParamsSong {

    var speed = 2f
    var judgment = 3
    var av = -1
    var delayMS = 0
    var rush = 1.0f
    var autoplay = false
    var nameNoteSkin = "prime"
    var stepType2Evaluation = "single"
    var stepLevel = "15"
    var skinIndex = 0

    /**
     * Game mode
     * 0 50-50 mode
     * 1 70-30 mode
     * 2 DM mode
     * 3 Tile mode
     */
    @JvmField
    var gameMode = 0
    var padOption: Short = 1

    //List mode
    var listCuadricula = true
    var FD = false
}