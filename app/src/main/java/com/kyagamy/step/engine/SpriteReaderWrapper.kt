package com.kyagamy.step.engine

import android.graphics.Canvas
import android.graphics.Rect
import com.kyagamy.step.common.step.commonGame.customSprite.SpriteReader

class SpriteReaderWrapper(private val sprite: SpriteReader) : ISpriteRenderer {
    private var canvas: Canvas? = null
    private var batchActive = false

    fun setCanvas(canvas: Canvas?) {
        this.canvas = canvas
    }

    override fun begin() {
        if (batchActive) {
            android.util.Log.w(
                "SpriteReaderWrapper",
                "begin() called while batch is already active"
            )
            return
        }
        batchActive = true
    }

    override fun drawCommand(
        textureId: Int,
        model: FloatArray,
        uvCoords: UVCoords
    ) {
        if (!batchActive) {
            android.util.Log.w(
                "SpriteReaderWrapper",
                "drawCommand() called outside of begin()/end()"
            )
            return
        }
        // This wrapper doesn't handle GL commands directly
        // It's for compatibility with Canvas-based rendering
    }

    override fun end() {
        if (!batchActive) {
            android.util.Log.w("SpriteReaderWrapper", "end() called without begin()")
            return
        }
        batchActive = false
    }

    override fun update(deltaMs: Long) {
        // Sprite reader doesn't use delta time
        sprite.update()
    }

    // Backward compatibility methods
    @Deprecated("Use begin()/end() pattern instead")
    override fun flushBatch() {
        // Nothing to flush for canvas-based rendering
    }

    @Deprecated("Use begin()/end() pattern instead")
    override fun clearCommands() {
        // Nothing to clear for canvas-based rendering
    }

    @Deprecated("Use drawCommand instead")
    override fun draw(rect: Rect) {
        canvas?.let { sprite.draw(it, rect) }
    }

    @Deprecated("Use update(deltaMs) instead")
    override fun update() {
        sprite.update()
    }
}
