package com.kyagamy.step.engine

import android.content.Context
import android.graphics.BitmapFactory
import android.graphics.Rect
import android.opengl.GLSurfaceView
import com.kyagamy.step.R
import javax.microedition.khronos.egl.EGLConfig
import javax.microedition.khronos.opengles.GL10
import kotlin.math.min

class ArrowSpriteRenderer(private val context: Context) : GLSurfaceView.Renderer {
    private lateinit var sprite: SpriteGLRenderer
    private val rect = Rect()

    override fun onSurfaceCreated(gl: GL10?, config: EGLConfig?) {
        val bitmap = BitmapFactory.decodeResource(context.resources, R.drawable.blue_arrow_r)
        sprite = SpriteGLRenderer(context, arrayOf(bitmap))
        sprite.onSurfaceCreated(gl, config)
    }

    override fun onSurfaceChanged(gl: GL10?, width: Int, height: Int) {
        sprite.onSurfaceChanged(gl, width, height)
        val size = min(width, height) / 2
        val left = (width - size) / 2
        val top = (height - size) / 2
        rect.set(left, top, left + size, top + size)
    }

    override fun onDrawFrame(gl: GL10?) {
        sprite.draw(rect)
        sprite.onDrawFrame(gl)
        sprite.update()
    }
}
