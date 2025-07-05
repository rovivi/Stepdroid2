package com.kyagamy.step.common.step.CommonGame

/**
 * Created : Rodrigo Vidal
 * This class is used to manage the preferences in the game
 */
 object ParamsSong {
    @JvmField
    var speed = 2f
    @JvmField
    var judgment = 3
    var av = 600
    @JvmField
    var delayMS = 0
    @JvmField
    var rush = 1.0f
    @JvmField
    var autoPlay = false
    @JvmField
    var nameNoteSkin = "prime"
    @JvmField
    var stepType2Evaluation = "single"
    var stepLevel = "15"
    @JvmField
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
    @JvmField
    var FD = false


}