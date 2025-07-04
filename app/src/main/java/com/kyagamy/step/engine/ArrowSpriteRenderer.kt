package com.kyagamy.step.engine

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Rect
import android.opengl.GLSurfaceView
import com.kyagamy.step.R
import com.kyagamy.step.common.Common
import java.io.InputStream
import javax.microedition.khronos.egl.EGLConfig
import javax.microedition.khronos.opengles.GL10

class ArrowSpriteRenderer(private val context: Context) : GLSurfaceView.Renderer {
    private var arrowSprite: SpriteGLRenderer? = null
    private var arrowRect: Rect = Rect()
    private var screenWidth = 0
    private var screenHeight = 0

    override fun onSurfaceCreated(gl: GL10?, config: EGLConfig?) {
        // Cargar sprite del noteskin
        loadNoteSkinArrow()
        arrowSprite?.onSurfaceCreated(gl, config)
    }

    private fun loadNoteSkinArrow() {
        try {
            val pathNS = "NoteSkins/pump/default/"
            val arrowName = Common.PIU_ARROW_NAMES[2] // Usar flecha "up" (centro)
            val stream = context.assets.open(pathNS + arrowName + "tap.png")
            val bitmap = BitmapFactory.decodeStream(stream)

            if (bitmap != null) {
                // Crear array de frames desde el bitmap (3x2 segun el NoteSkin)
                val frames = createFramesFromBitmap(bitmap, 3, 2)
                arrowSprite = SpriteGLRenderer(context, frames)
            }
        } catch (e: Exception) {
            e.printStackTrace()
            // Fallback a drawable si no se puede cargar el noteskin
            loadFallbackArrow()
        }
    }

    private fun createFramesFromBitmap(sprite: Bitmap, sizeX: Int, sizeY: Int): Array<Bitmap> {
        val frames = Array<Bitmap>(sizeX * sizeY) {
            Bitmap.createBitmap(1, 1, Bitmap.Config.ARGB_8888)
        }

        val frameWidth = sprite.width / sizeX
        val frameHeight = sprite.height / sizeY
        var count = 0

        for (y in 0 until sizeY) {
            for (x in 0 until sizeX) {
                frames[count] = Bitmap.createBitmap(
                    sprite,
                    x * frameWidth,
                    y * frameHeight,
                    frameWidth,
                    frameHeight
                )
                count++
            }
        }

        return frames
    }

    private fun loadFallbackArrow() {
        val opts = BitmapFactory.Options().apply { inPreferredConfig = Bitmap.Config.ARGB_8888 }
        val bitmap =
            BitmapFactory.decodeResource(context.resources, R.drawable.dance_pad_up_on, opts)
        if (bitmap != null) {
            arrowSprite = SpriteGLRenderer(context, arrayOf(bitmap))
        }
    }

    override fun onSurfaceChanged(gl: GL10?, width: Int, height: Int) {
        arrowSprite?.onSurfaceChanged(gl, width, height)
        screenWidth = width
        screenHeight = height

        // Posicionar la flecha en el centro arriba
        setupArrowPosition()
    }

    private fun setupArrowPosition() {
        if (screenWidth > 0 && screenHeight > 0) {
            val arrowSize = 200 // Tamaño de la flecha
            val centerX = screenWidth / 2
            val topY = 100 // Distancia desde arriba

            arrowRect = Rect(
                centerX - arrowSize / 2,
                topY,
                centerX + arrowSize / 2,
                topY + arrowSize
            )
        }
    }

    override fun onDrawFrame(gl: GL10?) {
        arrowSprite?.let { sprite ->
            sprite.draw(arrowRect)
            sprite.onDrawFrame(gl)
            sprite.update()
        }
    }
}
