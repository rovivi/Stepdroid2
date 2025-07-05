package com.kyagamy.step.game.newplayer

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Rect
import com.kyagamy.step.R
import com.kyagamy.step.common.step.commonGame.customSprite.SpriteReader
import com.kyagamy.step.game.newplayer.Evaluator.Companion.BAD
import com.kyagamy.step.game.newplayer.Evaluator.Companion.GOOD
import com.kyagamy.step.game.newplayer.Evaluator.Companion.GREAT
import com.kyagamy.step.game.newplayer.Evaluator.Companion.MAX_COMBO
import com.kyagamy.step.game.newplayer.Evaluator.Companion.MISS
import com.kyagamy.step.game.newplayer.Evaluator.Companion.PERFECT
import kotlin.math.abs

class Combo(c: Context, stepsDrawer: StepsDrawer) {
    private var timeMark: Long?

    private val judgeSprite: SpriteReader
    private val numberCombo: SpriteReader
    private val comboImage: Bitmap
    private val badCombo: Bitmap
    private var currentBitMapCombo: Bitmap? = null

    private val x: Int
    private val y: Int

    private var combo = 0
    private var aumentTip = -220
    private val paint = Paint()
    private var lifeBar: LifeBar? = null


    var positionJudge: Short = 0

    init {
        timeMark = System.currentTimeMillis()
        this.x = stepsDrawer.sizeX + stepsDrawer.offsetX
        this.y = stepsDrawer.sizeY
        val myOpt2 = BitmapFactory.Options()
        myOpt2.inSampleSize = 0
        numberCombo = SpriteReader(
            BitmapFactory.decodeResource(
                c.getResources(),
                R.drawable.play_combo_number,
                myOpt2
            ), 10, 1, 1f
        )
        judgeSprite = SpriteReader(
            BitmapFactory.decodeResource(
                c.getResources(),
                R.drawable.play_combo_judge,
                myOpt2
            ), 1, 5, 1f
        )
        comboImage = BitmapFactory.decodeResource(c.getResources(), R.drawable.play_combo, myOpt2)
        badCombo = BitmapFactory.decodeResource(c.getResources(), R.drawable.play_combo_bad, myOpt2)
    }

    fun setLifeBar(lifeBar: LifeBar) {
        this.lifeBar = lifeBar
    }

    fun show() {
        aumentTip = 20
        paint.setAlpha(255)
        currentBitMapCombo = if (combo >= 0) comboImage else badCombo
    }

    fun setComboUpdate(typeTap: Short) {
        positionJudge = typeTap
        when (typeTap) {
            VALUE_PERFECT -> {
                Evaluator.PERFECT = Evaluator.PERFECT + 1
                combo = if (combo < 0) 1 else (combo + 1)
            }

            VALUE_GREAT -> {
                Evaluator.GREAT = Evaluator.GREAT + 1
                combo = if (combo < 0) 1 else (combo + 1)
            }

            VALUE_GOOD -> {
                Evaluator.GOOD = Evaluator.GOOD + 1
                if (combo < -4) combo = 0
            }

            VALUE_BAD -> {
                Evaluator.BAD = Evaluator.BAD + 1
                if (combo != 0) combo = 0
            }

            VALUE_MISS -> {
                Evaluator.MISS = Evaluator.MISS + 1
                combo = if (combo > 0) 0 else (combo - 1)
            }
        }
        lifeBar!!.updateLife(typeTap, 1)
        if (combo > Evaluator.MAX_COMBO) Evaluator.MAX_COMBO = combo
        show()
    }

    fun update() {
        //  if (System.nanoTime() - timeMark > 100) {
        aumentTip -= 1
        timeMark = System.nanoTime()
        // }
    }

    fun draw(canvas: Canvas) {
        //setSizes
        val numberSizeY = (y * COMBO_NUMBER_RATIO_X).toInt()
        val numberSizeX = (y * COMBO_NUMBER_RATIO_X).toInt()

        var comboSizeY = ((y * COMBO_TEXT_RATIO_Y)).toInt()
        var comboSizeX = ((y * COMBO_TEXT_RATIO_X)).toInt()

        var labelSizeY = ((y * COMBO_LABEL_RATIO_Y)).toInt()
        var labelSizeX = ((y * COMBO_LABEL_RATIO_X)).toInt()

        //initX For each type
        if (aumentTip > 14 && aumentTip < 21) {
            val relation: Float = 1 + (aumentTip - 15) * 0.22f * RATIO_BIGGER_LABEL
            labelSizeY = (labelSizeY * relation).toInt()
            labelSizeX = (labelSizeX * relation).toInt()
            comboSizeX = (comboSizeX * ((relation - 1) / 3 + 1)).toInt()
            comboSizeY = (comboSizeY * ((relation - 1) / 3 + 1)).toInt()
        }

        val posLabelIntX = ((x / 2f - labelSizeX / 2f) * 1.008).toInt()
        val posComboIntX = (x / 2f - comboSizeX / 2f).toInt()

        if (aumentTip < 6) paint.setAlpha(abs(-(255 / (5) * aumentTip)))


        var posIntYCombo =
            (y / 2 - (numberSizeY + labelSizeY + comboSizeY) / 2) // (int) (y / 2 - (y * 0.05) / 2);

        if (aumentTip > 0) {
            canvas.drawBitmap(
                judgeSprite.frames[positionJudge.toInt()],
                null,
                Rect(
                    posLabelIntX,
                    posIntYCombo,
                    posLabelIntX + labelSizeX,
                    posIntYCombo + labelSizeY
                ),
                paint
            )

            posIntYCombo = (posIntYCombo + labelSizeY * 1.08).toInt()
            if (combo > 3 || combo < -3) {
                //show combo text
                canvas.drawBitmap(
                    currentBitMapCombo!!,
                    null,
                    Rect(
                        posComboIntX,
                        posIntYCombo,
                        posComboIntX + comboSizeX,
                        posIntYCombo + comboSizeY
                    ),
                    paint
                )
                posIntYCombo += comboSizeY
                val stringComboAux = (100000000 + abs(combo)).toString() + ""
                val stringCombo = abs(combo).toString() + ""

                var drawTimes =
                    4 //number of types you need to draw number example combo 39 then 3 digits show 039
                if (stringCombo.length > 3) drawTimes = stringCombo.length + 1

                for (w in 1..<drawTimes) {
                    val totalComboLength = (drawTimes - 1) * numberSizeX
                    val positionCurrentNumber = ((totalComboLength / 2) + x / 2) - numberSizeX * w
                    val n = (stringComboAux.get(stringComboAux.length - w).toString() + "").toInt()
                    canvas.drawBitmap(
                        numberCombo.frames[n],
                        null,
                        Rect(
                            positionCurrentNumber,
                            posIntYCombo,
                            positionCurrentNumber + numberSizeX,
                            posIntYCombo + numberSizeY
                        ),
                        paint
                    )
                }
            }
        }
    }

    companion object {
        const val VALUE_PERFECT: Short = 0
        const val VALUE_GREAT: Short = 1
        const val VALUE_GOOD: Short = 2
        const val VALUE_BAD: Short = 3
        const val VALUE_MISS: Short = 4
        const val VALUE_MISSING: Short = 1


        //proportions Y
        private val COMBO_TEXT_RATIO_X = 0.14815f * 1.25f
        private val COMBO_TEXT_RATIO_Y = 0.0363637f * 1.25f

        private val COMBO_NUMBER_RATIO_X = 0.05555556f * 1.15f
        private const val COMBO_NUMBER_RATIO_Y = 0.06141616f

        private const val COMBO_LABEL_RATIO_X = 0.306f
        private const val COMBO_LABEL_RATIO_Y = 0.0555555556f

        private const val RATIO_BIGGER_LABEL = 0.6666666667f
    }
}
