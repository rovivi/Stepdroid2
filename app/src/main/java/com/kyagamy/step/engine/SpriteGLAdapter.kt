package com.kyagamy.step.engine

import android.graphics.Bitmap
import android.graphics.Rect
import android.opengl.GLES20
import android.opengl.GLUtils
import android.opengl.Matrix
import com.kyagamy.step.common.step.commonGame.customSprite.SpriteReader

class SpriteGLAdapter(private val spriteReader: SpriteReader) : ISpriteRenderer {

    private var textureId = 0
    private var isTextureLoaded = false
    private var currentFrameIndex = 0
    private var lastUpdateTime = 0L
    private val frameTime = 100L // 100ms per frame

    // Batching system
    private val drawCommands = mutableListOf<DrawCommand>()
    private var batchActive = false

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
        return try {
            // Use reflection to access the frames and frameIndex from SpriteReader
            val framesField = spriteReader.javaClass.getDeclaredField("frames")
            framesField.isAccessible = true
            val frames = framesField.get(spriteReader) as? Array<Bitmap>

            val frameIndexField = spriteReader.javaClass.getDeclaredField("frameIndex")
            frameIndexField.isAccessible = true
            val frameIndex = frameIndexField.getInt(spriteReader)

            if (frames != null && frameIndex < frames.size && frames[frameIndex] != null) {
                frames[frameIndex]
            } else {
                null
            }
        } catch (e: Exception) {
            android.util.Log.e("SpriteGLAdapter", "Error getting current frame bitmap", e)
            null
        }
    }

    fun bindTexture() {
        if (isTextureLoaded) {
            GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, textureId)
        }
    }

    fun unbindTexture() {
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, 0)
    }

    // New batching interface implementation
    override fun begin() {
        if (batchActive) {
            android.util.Log.w("SpriteGLAdapter", "begin() called while batch is already active")
            return
        }
        batchActive = true
        drawCommands.clear()
    }

    override fun drawCommand(textureId: Int, model: FloatArray, uvCoords: UVCoords) {
        if (!batchActive) {
            android.util.Log.w("SpriteGLAdapter", "drawCommand() called outside of begin()/end()")
            return
        }
        drawCommands.add(DrawCommand(textureId, model.clone(), uvCoords))
    }

    override fun end() {
        if (!batchActive) {
            android.util.Log.w("SpriteGLAdapter", "end() called without begin()")
            return
        }
        batchActive = false
        // Commands are stored for external processing
    }

    override fun update(deltaMs: Long) {
        // Update frame animation
        val currentTime = System.currentTimeMillis()
        if (currentTime - lastUpdateTime > frameTime) {
            spriteReader.update()
            lastUpdateTime = currentTime

            // Update texture if needed
            updateTextureFrame()
        }
    }

    override fun flushBatch() {
        // This adapter doesn't handle rendering directly
        // The actual batching would be handled by a master renderer
        // For now, just clear the commands
        drawCommands.clear()
    }

    override fun clearCommands() {
        drawCommands.clear()
    }

    @Deprecated("Use drawCommand instead")
    override fun draw(rect: Rect) {
        // Convert rect to model matrix for compatibility
        val model = FloatArray(16)
        Matrix.setIdentityM(model, 0)
        Matrix.translateM(model, 0, rect.left.toFloat(), rect.top.toFloat(), 0f)
        Matrix.scaleM(model, 0, rect.width().toFloat(), rect.height().toFloat(), 1f)

        // Use current texture ID
        val currentTextureId = if (isTextureLoaded) textureId else 0
        val uvCoords = UVCoords(0f, 0f, 1f, 1f)
        drawCommand(currentTextureId, model, uvCoords)
    }

    @Deprecated("Use update(deltaMs) instead")
    override fun update() {
        update(16L) // ~60 FPS
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

    fun getDrawCommands(): List<DrawCommand> {
        return drawCommands.toList()
    }
}