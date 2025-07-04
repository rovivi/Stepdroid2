package com.kyagamy.step.engine

import android.content.Context
import android.graphics.Canvas
import android.graphics.PixelFormat
import android.graphics.Rect
import android.util.AttributeSet
import android.view.SurfaceHolder
import android.view.SurfaceView

class CanvasSpriteView : SurfaceView, SurfaceHolder.Callback, SpriteView {
    private var renderer: ISpriteRenderer? = null

    constructor(context: Context) : super(context) {
        holder.setFormat(PixelFormat.TRANSPARENT)
    }

    constructor(context: Context, attrs: AttributeSet?) : super(context, attrs) {
        holder.setFormat(PixelFormat.TRANSPARENT)
    }

    override fun surfaceCreated(holder: SurfaceHolder) {
        setWillNotDraw(false)
    }

    override fun surfaceChanged(holder: SurfaceHolder, format: Int, width: Int, height: Int) {
        // no-op
    }

    override fun surfaceDestroyed(holder: SurfaceHolder) {}

    override fun setRenderer(renderer: ISpriteRenderer) {
        this.renderer = renderer
    }

    override fun draw(canvas: Canvas) {
        super.draw(canvas)
        renderer?.let {
            if (it is SpriteReaderWrapper) {
                it.setCanvas(canvas)
            }
            it.draw(Rect(0, 0, width, height))
        }
    }

    override fun update() {
        renderer?.update()
    }
}
