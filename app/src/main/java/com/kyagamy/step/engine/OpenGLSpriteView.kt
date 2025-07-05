package com.kyagamy.step.engine

import android.content.Context
import android.opengl.GLSurfaceView
import android.util.AttributeSet
import android.graphics.PixelFormat

class OpenGLSpriteView(context: Context, attrs: AttributeSet? = null) : GLSurfaceView(context, attrs), SpriteView {
    private var spriteRenderer: ISpriteRenderer? = null

    init {
        setEGLContextClientVersion(2)
        // Configurar para transparencia
        setEGLConfigChooser(8, 8, 8, 8, 16, 0)
        holder.setFormat(PixelFormat.TRANSLUCENT)
        preserveEGLContextOnPause = true
    }

    override fun setRenderer(renderer: ISpriteRenderer) {
        spriteRenderer = renderer
        if (renderer is GLSurfaceView.Renderer) {
            super.setRenderer(renderer)
        }
    }

    override fun update() {
        spriteRenderer?.update()
        requestRender()
    }
}
