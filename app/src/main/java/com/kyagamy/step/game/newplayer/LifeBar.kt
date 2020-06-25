package com.kyagamy.step.game.newplayer

import android.content.Context
import android.graphics.*
import com.kyagamy.step.R
import com.kyagamy.step.common.step.CommonGame.TransformBitmap.Companion.cutBitmap
import com.kyagamy.step.common.step.CommonGame.TransformBitmap.Companion.makeTransparent

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
    private val tipBlue: Bitmap
    private val glowBlue: Bitmap
    private val glueRed: Bitmap
    private val skin: Bitmap
    private val barHot: Bitmap
    private val resplandor: Bitmap
    private val paint:Paint
    var timeMark: Long
    fun draw(canvas: Canvas /*int x,int y*/) {
        //se calcula la pociocion del tip
        aumento++
        val percent = life / 100
        val positionTip =startX+ (sizeX * percent).toInt()
        val posBarBlue = sizeX * (percent + aumentLife / 100)
        val currentHotBar = cutBitmap(barHot, life)


        if (life < AMAZING_VALUE) {
            canvas.drawBitmap(
                glowBlue,
                null,
                Rect(startX, startY, (startX + posBarBlue).toInt(), sizeY),
                paint
            )

        }
        if (life < DANGER_VALUE)  {
            canvas.drawBitmap(
                glueRed,
                null,
                Rect(startX, startY, startX + sizeX, sizeY),
                paint
            )
        }


        canvas.drawBitmap(
            currentHotBar,
            null,
            Rect(startX, startY,  positionTip, sizeY),
            paint
        )

        if (life > AMAZING_VALUE ||life < DANGER_VALUE ) {
            canvas.drawBitmap(
                makeTransparent(
                    resplandor ,
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
            tipBlue,
            null,
            Rect(
                + positionTip,
                startY,
                ( positionTip + (sizeX*0.08f)).toInt(),
                sizeY
            ),
            paint
        )
    }

    fun updateLife(life: Float) {

        if (System.nanoTime() - timeMark > 250) {
            if (aumentLife > 5 || aumentLife < 0) {
                auxLife *= -1f
            }
            aumentLife += auxLife
            timeMark = System.nanoTime()
        }


//        this.life +=0.089f
////        this.life =0f
//        if (this.life>=120){
//            this.life=-1f;
//        }

    }

    init {
        val myOpt2 = BitmapFactory.Options()
        myOpt2.inSampleSize = 2 * 1
        tipBlue = BitmapFactory.decodeResource(context.resources, R.drawable.blue_tip, myOpt2)
        glowBlue = BitmapFactory.decodeResource(context.resources, R.drawable.solid, myOpt2)
        barHot =
            BitmapFactory.decodeResource(context.resources, R.drawable.barhot_double, myOpt2)
        skin = BitmapFactory.decodeResource(context.resources, R.drawable.lifeframe, myOpt2)
        glueRed = BitmapFactory.decodeResource(context.resources, R.drawable.caution, myOpt2)
        resplandor =
            BitmapFactory.decodeResource(context.resources, R.drawable.resplandor, myOpt2)

        timeMark = System.nanoTime()
        paint= Paint()

        this.sizeX = stepsDrawer.sizeNote * stepsDrawer.stepsByGameMode
        this.sizeY = (stepsDrawer.sizeNote / 3)*2
        this.startX = stepsDrawer.posInitialX+stepsDrawer.offsetX
        this.startY = stepsDrawer.sizeNote/3
    }

    companion object {
        const val DANGER_VALUE = 15
        const val AMAZING_VALUE = 95
    }
}