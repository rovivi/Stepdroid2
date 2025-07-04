package com.kyagamy.step.game.newplayer

import android.content.Context
import android.graphics.*
import com.kyagamy.step.R
import com.kyagamy.step.common.step.CommonGame.TransformBitmap.Companion.cutBitmap
import com.kyagamy.step.common.step.CommonGame.TransformBitmap.Companion.makeTransparent
import kotlin.math.abs

class LifeBar(
    context: Context,
    stepsDrawer: StepsDrawer

) {

    private val sizeX: Int
    private val sizeY: Int
    private val startX: Int
    private val startY: Int
    var life = 50f
    private val lifeblue = 0f
    var aumento = 0f
    var aumentLife = 0f
    var auxLife = 1f
    private val bg: Bitmap
    private val bgDanger: Bitmap

    private val tipBlue: Bitmap
    private val tipRed: Bitmap
    private val glowBlue: Bitmap
    private val glueRed: Bitmap
    private val skin: Bitmap
    private val lifeMeter: Bitmap
    private val lightFull: Bitmap
    private val paint: Paint
    var timeMark: Long
    fun draw(canvas: Canvas /*int x,int y*/) {
        //se calcula la pociocion del tip
        aumento++
        val percent = life / 100
        val positionTip =
            startX + when {
                life < 6 -> (sizeX * (0.005)).toInt()
                life > 98 ->(sizeX * (0.94)).toInt()
                else ->  (sizeX * (percent-0.05 )).toInt()
            }
        val positionBar = startX + if (life >= 98) (sizeX) else (sizeX * (percent - 0.1)).toInt()
        val posBarBlue = sizeX * (percent - 0.06 + aumentLife / 100)
        val currentHotBar = cutBitmap(lifeMeter, life)

        if (life < DANGER_VALUE) {
            canvas.drawBitmap(
                glueRed,
                null,
                Rect(startX, startY, startX + sizeX, sizeY),
                paint
            )
        }
        //bg
        if (life < 100)
            canvas.drawBitmap(
                if (life <= DANGER_VALUE) bgDanger else bg,
                null,
                Rect(startX, startY, startX + sizeX, sizeY),
                paint
            )

        canvas.drawBitmap(
            glowBlue,
            null,
            Rect(startX, startY, (startX + posBarBlue).toInt(), sizeY),
            paint
        )
        canvas.drawBitmap(
            currentHotBar,
            null,
            Rect(startX, startY, positionBar, sizeY),
            paint
        )
        if (life > AMAZING_VALUE) {
            canvas.drawBitmap(
                makeTransparent(
                    lightFull,
                    (0 + aumentLife * 20).toInt()
                ),
                null,
                Rect(startX, startY, startX + sizeX, sizeY),
                paint
            )
        }
        //Skin
        canvas.drawBitmap(
            skin,
            null,
            Rect(startX, startY, startX + sizeX, sizeY),
            paint
        )
        canvas.drawBitmap(
            if (life > DANGER_VALUE) tipBlue else tipRed,
            null,
            Rect(
                +positionTip,
                startY,
                (positionTip + (sizeX * 0.08f)).toInt(),
                sizeY
            ),
            paint
        )
    }

    fun update() {
        if (System.nanoTime() - timeMark > 150) {
            if (aumentLife > 6 || aumentLife < 0)auxLife *= -1f
            aumentLife += auxLife
            timeMark = System.nanoTime()
        }
    }

    fun updateLife(typeTap: Short, combo: Int) {
        when (typeTap) {
            Combo.VALUE_PERFECT, Combo.VALUE_GREAT -> life+=1*abs(combo)
            Combo.VALUE_BAD -> life-=0.3f*abs(combo)
            Combo.VALUE_MISS -> life-=3* abs(combo)
        }
        if (life>100)
            life=100f
        if (life <0)
            life =0f
    }

    init {
        val myOpt2 = BitmapFactory.Options()
        myOpt2.inSampleSize = 0
        bg = BitmapFactory.decodeResource(context.resources, R.drawable.lifebar_bg, myOpt2)
        bgDanger =
            BitmapFactory.decodeResource(context.resources, R.drawable.lifebar_bg_danger, myOpt2)
        tipBlue =
            BitmapFactory.decodeResource(context.resources, R.drawable.lifebar_blue_tip, myOpt2)
        tipRed = BitmapFactory.decodeResource(context.resources, R.drawable.lifebar_red_tip, myOpt2)
        glowBlue =
            BitmapFactory.decodeResource(context.resources, R.drawable.lifebar_back_tip, myOpt2)
        lifeMeter =
            BitmapFactory.decodeResource(context.resources, R.drawable.lifebar_life, myOpt2)
        skin = BitmapFactory.decodeResource(context.resources, R.drawable.lifebar_skin, myOpt2)
        glueRed =
            BitmapFactory.decodeResource(context.resources, R.drawable.lifebar_light_danger, myOpt2)
        lightFull =
            BitmapFactory.decodeResource(context.resources, R.drawable.lifebar_light_full, myOpt2)

        timeMark = System.nanoTime()
        paint = Paint()

        this.sizeX = stepsDrawer.sizeNote * stepsDrawer.stepsByGameMode
        this.sizeY = ((stepsDrawer.sizeNote / 3) * 1.9f).toInt()
        this.startX = stepsDrawer.posInitialX
        this.startY = stepsDrawer.sizeNote / 8
    }

    companion object {
        const val DANGER_VALUE = 15
        const val AMAZING_VALUE = 95
    }
}