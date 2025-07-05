package com.kyagamy.step.engine

import android.graphics.Canvas
import android.graphics.Rect
import com.kyagamy.step.common.step.commonGame.customSprite.SpriteReader

class SpriteReaderWrapper(private val sprite: SpriteReader) : ISpriteRenderer {
    private var canvas: Canvas? = null

    fun setCanvas(canvas: Canvas?) {
        this.canvas = canvas
    }

    override fun drawCommand(
        textureId: Int,
        model: FloatArray,
        uvOff: FloatArray
    ) {
        TODO("Not yet implemented")
    }

    override fun update(deltaMs: Long) {
        TODO("Not yet implemented")
    }

    override fun flushBatch() {
        TODO("Not yet implemented")
    }

    override fun clearCommands() {
        TODO("Not yet implemented")
    }

    override fun draw(rect: Rect) {
        canvas?.let { sprite.draw(it, rect) }
    }

    override fun update() {
        sprite.update()
    }
}
