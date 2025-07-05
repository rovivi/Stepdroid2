package com.kyagamy.step.engine

import android.graphics.Bitmap
import android.graphics.Rect
import android.opengl.GLES20
import android.opengl.GLUtils
import com.kyagamy.step.common.step.CommonGame.CustomSprite.SpriteReader

class SpriteGLAdapter(private val spriteReader: SpriteReader) : ISpriteRenderer {

    private var textureId = 0
    private var isTextureLoaded = false
    private var currentFrameIndex = 0
    private var lastUpdateTime = 0L
    private val frameTime = 100L // 100ms per frame

    fun loadTexture() {
        if (isTextureLoaded) return

        // Generate and bind texture
        val textureIds = IntArray(1)
        GLES20.glGenTextures(1, textureIds, 0)
        textureId = textureIds[0]

        // Get current frame bitmap from sprite reader
        val bitmap = getCurrentFrameBitmap()
        if (bitmap != null) {
            GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, textureId)

            // Set texture parameters
            GLES20.glTexParameteri(
                GLES20.GL_TEXTURE_2D,
                GLES20.GL_TEXTURE_MIN_FILTER,
                GLES20.GL_LINEAR
            )
            GLES20.glTexParameteri(
                GLES20.GL_TEXTURE_2D,
                GLES20.GL_TEXTURE_MAG_FILTER,
                GLES20.GL_LINEAR
            )
            GLES20.glTexParameteri(
                GLES20.GL_TEXTURE_2D,
                GLES20.GL_TEXTURE_WRAP_S,
                GLES20.GL_CLAMP_TO_EDGE
            )
            GLES20.glTexParameteri(
                GLES20.GL_TEXTURE_2D,
                GLES20.GL_TEXTURE_WRAP_T,
                GLES20.GL_CLAMP_TO_EDGE
            )

            // Load bitmap into texture
            GLUtils.texImage2D(GLES20.GL_TEXTURE_2D, 0, bitmap, 0)

            GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, 0)
            isTextureLoaded = true
        }
    }

    private fun getCurrentFrameBitmap(): Bitmap? {
        // This is a simplified approach - in practice you'd need to extract 
        // the bitmap data from the SpriteReader
        // For now, we'll return null and handle this case
        return null
    }

    fun bindTexture() {
        if (isTextureLoaded) {
            GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, textureId)
        }
    }

    fun unbindTexture() {
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, 0)
    }

    override fun draw(rect: Rect) {
        // The actual drawing would be handled by the StepsDrawerGL
        // This adapter just manages the texture binding
        bindTexture()
    }

    override fun update() {
        // Update frame animation
        val currentTime = System.currentTimeMillis()
        if (currentTime - lastUpdateTime > frameTime) {
            spriteReader.update()
            lastUpdateTime = currentTime

            // Update texture if needed (this would require reloading the texture
            // with the new frame data in a real implementation)
            updateTextureFrame()
        }
    }

    private fun updateTextureFrame() {
        // In a complete implementation, this would update the texture
        // with the new frame data from the sprite reader
        if (isTextureLoaded) {
            val bitmap = getCurrentFrameBitmap()
            if (bitmap != null) {
                GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, textureId)
                GLUtils.texSubImage2D(GLES20.GL_TEXTURE_2D, 0, 0, 0, bitmap)
                GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, 0)
            }
        }
    }

    fun cleanup() {
        if (isTextureLoaded) {
            GLES20.glDeleteTextures(1, intArrayOf(textureId), 0)
            isTextureLoaded = false
        }
    }

    fun getTextureId(): Int {
        return if (isTextureLoaded) textureId else 0
    }
}