package com.kyagamy.step.engine

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Rect
import android.opengl.GLSurfaceView
import com.kyagamy.step.R
import javax.microedition.khronos.egl.EGLConfig
import javax.microedition.khronos.opengles.GL10
import kotlin.math.min
import kotlin.random.Random

class ArrowSpriteRenderer(private val context: Context) : GLSurfaceView.Renderer {
    private val sprites = mutableListOf<SpriteGLRenderer>()
    private val arrows = mutableListOf<ArrowData>()
    private var screenWidth = 0
    private var screenHeight = 0
    private var lastUpdateTime = 0L
    private val updateInterval = 200L // 1 segundo

    var Drawables = listOf(
        R.drawable.stomp7_on,
        R.drawable.stomp1_on,
        R.drawable.stop5_on,
        R.drawable.black_up,
        R.drawable.dance_pad_up_on,
        R.drawable.stomp3_on,
        R.drawable.stomp9_on
    )

    data class ArrowData(
        val rect: Rect,
        val spriteIndex: Int
    )

    override fun onSurfaceCreated(gl: GL10?, config: EGLConfig?) {
        // Crear un SpriteGLRenderer para cada drawable
        sprites.clear()
        Drawables.forEach { drawableRes ->
            val opts = BitmapFactory.Options().apply { inPreferredConfig = Bitmap.Config.ARGB_8888 }
            val bitmap = BitmapFactory.decodeResource(context.resources, drawableRes, opts)
            if (bitmap != null) {
                val sprite = SpriteGLRenderer(context, arrayOf(bitmap))
                sprite.onSurfaceCreated(gl, config)
                sprites.add(sprite)
            }


        }
        lastUpdateTime = System.currentTimeMillis()
    }

    override fun onSurfaceChanged(gl: GL10?, width: Int, height: Int) {
        sprites.forEach { sprite ->
            sprite.onSurfaceChanged(gl, width, height)
        }
        screenWidth = width
        screenHeight = height

        // Generar las 22 flechas iniciales
        generateArrows()
    }

    private fun generateArrows() {
        arrows.clear()
        if (screenWidth > 0 && screenHeight > 0 && sprites.isNotEmpty()) {
            repeat(5000) {
                val size = Random.nextInt(50, 150)
                val left = Random.nextInt(0, screenWidth - size)
                val top = Random.nextInt(0, screenHeight - size)
                val rect = Rect(left, top, left + size, top + size)
                val spriteIndex = Random.nextInt(sprites.size)
                arrows.add(ArrowData(rect, spriteIndex))
            }
        }
    }

    override fun onDrawFrame(gl: GL10?) {
        // Verificar si ha pasado 1 segundo para actualizar posiciones
        val currentTime = System.currentTimeMillis()
        if (currentTime - lastUpdateTime >= updateInterval) {
            generateArrows()
            lastUpdateTime = currentTime
        }

        // Dibujar todas las flechas
        arrows.forEach { arrow ->
            val sprite = sprites[arrow.spriteIndex]
            sprite.draw(arrow.rect)
            sprite.onDrawFrame(gl)
            sprite.update()
        }
    }

    fun addMoreArrows() {
        generateArrows()
    }
}
