package com.kyagamy.step.game.newplayer

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Point
import android.graphics.Rect
import android.os.Build
import androidx.annotation.RequiresApi
import com.kyagamy.step.R
import com.kyagamy.step.common.step.CommonGame.TransformBitmap

@RequiresApi(Build.VERSION_CODES.JELLY_BEAN_MR1)
class GamePad(
    context: Context,
    private val type: String,
    val pad: ByteArray,
    private val screenWidth: Int,
    private val screenHeight: Int
) {
    private var arrows: Array<Bitmap> = emptyArray()
    private var arrowOFF: Array<Bitmap> = emptyArray()
    private var panel: Bitmap? = null
    private var panel2: Bitmap? = null
    private var transformBitmap: TransformBitmap? = null
    private var wasPressed: Array<Boolean> = emptyArray()
    private var arrowsPosition2: Array<Rect> = emptyArray()
    private val posPanel = Point(0, 0)
    private var bg: Bitmap? = null
    private var gamePlayNew: GamePlayNew? = null

    init {
        initializeGamePad(context)
    }

    fun setGamePlayNew(gamePlayNew: GamePlayNew) {
        this.gamePlayNew = gamePlayNew
    }

    private fun initializeGamePad(context: Context) {
        val options = BitmapFactory.Options().apply {
            inSampleSize = 25
        }

        panel = BitmapFactory.decodeResource(context.resources, R.drawable.touch_controls)

        when (type) {
            "pump-single" -> setupPumpSingle(context, options)
            "pump-double", "pump-halfdouble", "pump-routine" -> setupPumpDouble(context, options)
            "dance-single" -> setupDanceSingle(context, options)
        }
    }

    private fun setupPumpSingle(context: Context, options: BitmapFactory.Options) {
        panel = BitmapFactory.decodeResource(context.resources, R.drawable.touch_controls, options)
        val step7On = BitmapFactory.decodeResource(context.resources, R.drawable.stomp7_on, options)
        val step1On = BitmapFactory.decodeResource(context.resources, R.drawable.stomp1_on, options)
        val step7Off =
            BitmapFactory.decodeResource(context.resources, R.drawable.stomp7_off, options)
        val step1Off =
            BitmapFactory.decodeResource(context.resources, R.drawable.stomp1_off, options)

        transformBitmap = TransformBitmap()

        arrowOFF = arrayOf(
            BitmapFactory.decodeResource(context.resources, R.drawable.stomp1_off, options),
            BitmapFactory.decodeResource(context.resources, R.drawable.stomp7_off, options),
            BitmapFactory.decodeResource(context.resources, R.drawable.stop5_off, options),
            TransformBitmap.FlipBitmap(step7Off, true),
            TransformBitmap.FlipBitmap(step1Off, true)
        )

        arrows = arrayOf(
            BitmapFactory.decodeResource(context.resources, R.drawable.stomp1_on, options),
            BitmapFactory.decodeResource(context.resources, R.drawable.stomp7_on, options),
            BitmapFactory.decodeResource(context.resources, R.drawable.stop5_on, options),
            TransformBitmap.FlipBitmap(step7On, true),
            TransformBitmap.FlipBitmap(step1On, true)
        )

        wasPressed = Array(5) { false }

        setupPumpSinglePositions()
    }

    private fun setupPumpSinglePositions() {
        val startY1 = 0.76f
        val startY2 = 0.51f
        val startY3 = 0.62f

        val startX1 = 0.0f
        val startX2 = 0.635f
        val startX3 = 0.255f

        val widthStep7 = 0.365f
        val widthStep5 = widthStep7 + 0.10f
        val heightStep7 = 0.174f
        val heightStep5 = 0.202f

        arrowsPosition2 = arrayOf(
            Rect(
                (screenWidth * startX1).toInt(),
                (screenHeight * startY1).toInt(),
                (screenWidth * (startX1 + widthStep7)).toInt(),
                (screenHeight * (startY1 + heightStep7)).toInt()
            ),
            Rect(
                (screenWidth * startX1).toInt(),
                (screenHeight * startY2).toInt(),
                (screenWidth * (startX1 + widthStep7)).toInt(),
                (screenHeight * (startY2 + heightStep7)).toInt()
            ),
            Rect(
                (screenWidth * startX3).toInt(),
                (screenHeight * startY3).toInt(),
                (screenWidth * (startX3 + widthStep5)).toInt(),
                (screenHeight * (startY3 + heightStep5)).toInt()
            ),
            Rect(
                (screenWidth * startX2).toInt(),
                (screenHeight * startY2).toInt(),
                (screenWidth * (startX2 + widthStep7)).toInt(),
                (screenHeight * (startY2 + heightStep7)).toInt()
            ),
            Rect(
                (screenWidth * startX2).toInt(),
                (screenHeight * startY1).toInt(),
                (screenWidth * (startX2 + widthStep7)).toInt(),
                (screenHeight * (startY1 + heightStep7)).toInt()
            )
        )

        createPanel2()
    }

    private fun setupPumpDouble(context: Context, options: BitmapFactory.Options) {
        panel = BitmapFactory.decodeResource(context.resources, R.drawable.touch_controls, options)
        val step7On = BitmapFactory.decodeResource(context.resources, R.drawable.stomp7_on, options)
        val step1On = BitmapFactory.decodeResource(context.resources, R.drawable.stomp1_on, options)
        val step7Off =
            BitmapFactory.decodeResource(context.resources, R.drawable.stomp7_off, options)
        val step1Off =
            BitmapFactory.decodeResource(context.resources, R.drawable.stomp1_off, options)

        transformBitmap = TransformBitmap()

        val baseArrowsOff = arrayOf(
            BitmapFactory.decodeResource(context.resources, R.drawable.stomp1_off, options),
            BitmapFactory.decodeResource(context.resources, R.drawable.stomp7_off, options),
            BitmapFactory.decodeResource(context.resources, R.drawable.stop5_off, options),
            TransformBitmap.FlipBitmap(step7Off, true),
            TransformBitmap.FlipBitmap(step1Off, true)
        )

        val baseArrowsOn = arrayOf(
            BitmapFactory.decodeResource(context.resources, R.drawable.stomp1_on, options),
            BitmapFactory.decodeResource(context.resources, R.drawable.stomp7_on, options),
            BitmapFactory.decodeResource(context.resources, R.drawable.stop5_on, options),
            TransformBitmap.FlipBitmap(step7On, true),
            TransformBitmap.FlipBitmap(step1On, true)
        )

        arrowOFF = Array(10) { index -> baseArrowsOff[index % 5] }
        arrows = Array(10) { index -> baseArrowsOn[index % 5] }

        wasPressed = Array(5) { false }

        setupPumpDoublePositions()
    }

    private fun setupPumpDoublePositions() {
        val startY1 = 0.76f
        val startY2 = 0.51f
        val startY3 = 0.62f

        val startX1 = -0.005f
        val startX2 = 0.329f
        val startX3 = 0.148f
        val startXP2 = 0.48f

        val widthStep7 = 0.365f / 2 + 0.033f
        val heightStep7 = 0.174f
        val widthStep5 = widthStep7 + 0.03f
        val heightStep5 = 0.202f

        arrowsPosition2 = arrayOf(
            Rect(
                (screenWidth * startX1).toInt(),
                (screenHeight * startY1).toInt(),
                (screenWidth * (startX1 + widthStep7)).toInt(),
                (screenHeight * (startY1 + heightStep7)).toInt()
            ),
            Rect(
                (screenWidth * startX1).toInt(),
                (screenHeight * startY2).toInt(),
                (screenWidth * (startX1 + widthStep7)).toInt(),
                (screenHeight * (startY2 + heightStep7)).toInt()
            ),
            Rect(
                (screenWidth * startX3).toInt(),
                (screenHeight * startY3).toInt(),
                (screenWidth * (startX3 + widthStep5)).toInt(),
                (screenHeight * (startY3 + heightStep5)).toInt()
            ),
            Rect(
                (screenWidth * startX2).toInt(),
                (screenHeight * startY2).toInt(),
                (screenWidth * (startX2 + widthStep7)).toInt(),
                (screenHeight * (startY2 + heightStep7)).toInt()
            ),
            Rect(
                (screenWidth * startX2).toInt(),
                (screenHeight * startY1).toInt(),
                (screenWidth * (startX2 + widthStep7)).toInt(),
                (screenHeight * (startY1 + heightStep7)).toInt()
            ),
            Rect(
                (screenWidth * (startX1 + startXP2)).toInt(),
                (screenHeight * startY1).toInt(),
                (screenWidth * (startX1 + widthStep7 + startXP2)).toInt(),
                (screenHeight * (startY1 + heightStep7)).toInt()
            ),
            Rect(
                (screenWidth * (startX1 + startXP2)).toInt(),
                (screenHeight * startY2).toInt(),
                (screenWidth * (startX1 + widthStep7 + startXP2)).toInt(),
                (screenHeight * (startY2 + heightStep7)).toInt()
            ),
            Rect(
                (screenWidth * (startX3 + startXP2)).toInt(),
                (screenHeight * startY3).toInt(),
                (screenWidth * (startX3 + widthStep5 + startXP2)).toInt(),
                (screenHeight * (startY3 + heightStep5)).toInt()
            ),
            Rect(
                (screenWidth * (startX2 + startXP2)).toInt(),
                (screenHeight * startY2).toInt(),
                (screenWidth * (startX2 + widthStep7 + startXP2)).toInt(),
                (screenHeight * (startY2 + heightStep7)).toInt()
            ),
            Rect(
                (screenWidth * (startX2 + startXP2)).toInt(),
                (screenHeight * startY1).toInt(),
                (screenWidth * (startX2 + widthStep7 + startXP2)).toInt(),
                (screenHeight * (startY1 + heightStep7)).toInt()
            )
        )

        createPanel2()
    }

    private fun setupDanceSingle(context: Context, options: BitmapFactory.Options) {
        val upOn = BitmapFactory.decodeResource(context.resources, R.drawable.dance_pad_up_on)
        val upOff = BitmapFactory.decodeResource(context.resources, R.drawable.dance_pad_up_on)

        arrowOFF = Array(10) { Bitmap.createBitmap(1, 1, Bitmap.Config.ARGB_8888) }
        arrows = arrayOf(
            TransformBitmap.RotateBitmap(upOn, 270f),
            TransformBitmap.RotateBitmap(upOn, 180f),
            upOn,
            TransformBitmap.RotateBitmap(upOn, 90f)
        )

        arrowOFF[0] = TransformBitmap.RotateBitmap(upOff, 270f)
        arrowOFF[1] = TransformBitmap.RotateBitmap(upOff, 180f)
        arrowOFF[2] = upOff
        arrowOFF[3] = TransformBitmap.RotateBitmap(upOff, 90f)

        wasPressed = Array(4) { false }

        setupDanceSinglePositions()
    }

    private fun setupDanceSinglePositions() {
        val startX1 = 0.0f
        val startX2 = 0.33f
        val startX3 = 0.635f
        val startY1 = 0.66f
        val startY2 = 0.51f
        val startY3 = 0.82f

        val widthStepUp = 0.365f
        val heightStepUp = 0.184f
        val heightStepLeft = 0.1875f
        val widthStepLeft = 0.345f

        arrowsPosition2 = arrayOf(
            Rect(
                (screenWidth * startX1).toInt(),
                (screenHeight * startY1).toInt(),
                (screenWidth * (startX1 + widthStepUp)).toInt(),
                (screenHeight * (startY1 + heightStepUp)).toInt()
            ),
            Rect(
                (screenWidth * startX2).toInt(),
                (screenHeight * startY3).toInt(),
                (screenWidth * (startX2 + widthStepLeft)).toInt(),
                (screenHeight * (startY3 + heightStepLeft)).toInt()
            ),
            Rect(
                (screenWidth * startX2).toInt(),
                (screenHeight * startY2).toInt(),
                (screenWidth * (startX2 + widthStepLeft)).toInt(),
                (screenHeight * (startY2 + heightStepLeft)).toInt()
            ),
            Rect(
                (screenWidth * startX3).toInt(),
                (screenHeight * startY1).toInt(),
                (screenWidth * (startX3 + widthStepUp)).toInt(),
                (screenHeight * (startY1 + heightStepUp)).toInt()
            )
        )

        createPanel2()
    }

    private fun createPanel2() {
        panel2 = Bitmap.createBitmap(screenWidth, screenHeight, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(panel2!!)
        val paint = Paint()

        arrowsPosition2.forEachIndexed { index, rect ->
            if (index < arrowOFF.size) {
                canvas.drawBitmap(arrowOFF[index], null, rect, paint)
            }
        }
    }

    fun draw(canvas: Canvas) {
        val paint = Paint()

        bg?.let { background ->
            // Uncomment if needed: canvas.drawBitmap(background, null, Rect(0, screenHeight/2, screenWidth, screenHeight), paint)
        }

        for (i in arrows.indices) {
            if (i < pad.size) {
                val bitmap = if (pad[i] != 0.toByte()) arrows[i] else arrowOFF[i]
                canvas.drawBitmap(bitmap, null, arrowsPosition2[i], paint)
            }
        }
    }

    fun clearPad() {
        val hasChanged = pad.any { it != 0.toByte() }
        pad.fill(0)
        if (hasChanged) {
            notifyPadStateChanged()
        }
    }

    fun checkInputs(positions: Array<IntArray>, isDownMove: Boolean) {
        for (j in arrows.indices) {
            var wasPressed = false
            val oldValue = pad[j]

            for (position in positions) {
                val x = position[0]
                val y = position[1]

                if (arrowsPosition2[j].contains(x, y)) {
                    if (pad[j] == 0.toByte() || (isDownMove && pad[j] == 2.toByte())) {
                        pad[j] = 1
                        playTapEffect(j)
                    }
                    wasPressed = true
                    break
                }
            }

            if (!wasPressed) {
                pad[j] = 0
            }

            if (pad[j] != oldValue) {
                notifyPadStateChanged()
            }
        }
    }

    private fun playTapEffect(index: Int) {
        // For now, we'll skip the tap effect until the StepsDrawer API is fully compatible
        // This can be re-enabled once the StepsDrawer.getSelectedSkin() method is working properly
    }

    fun unpress(x: Float, y: Float) {
        for (j in arrows.indices) {
            if (arrowsPosition2[j].contains(x.toInt(), y.toInt())) {
                val oldValue = pad[j]
                pad[j] = 0
                if (oldValue != pad[j]) {
                    notifyPadStateChanged()
                }
            }
        }
    }

    private fun notifyPadStateChanged() {
        gamePlayNew?.notifyPadStateChanged()
    }
}