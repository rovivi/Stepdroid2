package com.kyagamy.step.engine

import android.content.Context
import android.graphics.BitmapFactory
import android.graphics.Rect
import androidx.annotation.DrawableRes

/**
 * Simple wrapper around [SpriteGLRenderer] that loads a single arrow texture
 * and exposes it as an [android.opengl.GLSurfaceView.Renderer].
 */
class ArrowSpriteRenderer(context: Context, @DrawableRes resId: Int) : ISpriteRenderer {
    private val renderer: SpriteGLRenderer

    init {
        val bitmap = BitmapFactory.decodeResource(context.resources, resId)
        renderer = SpriteGLRenderer(context, arrayOf(bitmap))
    }

    fun getGLRenderer(): android.opengl.GLSurfaceView.Renderer = renderer

    override fun draw(rect: Rect) {
        renderer.draw(rect)
    }

    override fun update() {
        renderer.update()
    }
}
