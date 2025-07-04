package com.kyagamy.step.engine

import android.graphics.Canvas
import android.graphics.Rect
import com.kyagamy.step.common.step.CommonGame.CustomSprite.SpriteReader

class SpriteReaderWrapper(private val sprite: SpriteReader) : ISpriteRenderer {
    private var canvas: Canvas? = null

    fun setCanvas(canvas: Canvas?) {
        this.canvas = canvas
    }

    override fun draw(rect: Rect) {
        canvas?.let { sprite.draw(it, rect) }
    }

    override fun update() {
        sprite.update()
    }
}
