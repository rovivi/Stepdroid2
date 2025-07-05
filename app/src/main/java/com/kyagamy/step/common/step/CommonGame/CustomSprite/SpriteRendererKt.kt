package com.kyagamy.step.common.step.CommonGame.CustomSprite

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
import android.graphics.Rect
import android.graphics.Shader
import com.kyagamy.step.engine.ISpriteRenderer
import com.kyagamy.step.engine.SpriteGLRenderer
import com.kyagamy.step.common.step.CommonGame.TransformBitmap

/**
 * Kotlin version of SpriteReader with OpenGL support
 * Created by Rodrigo Vidal February 2018
 * Updated with OpenGL integration
 */
class SpriteRendererKt : ISpriteRenderer {
    var frames: Array<Bitmap>
    private var frameIndex = 0
    private var frameTime: Float = 0f
    private var lastFrame: Long = 0
    private var isPlaying = false
    private var lapsedTime: Float = 0f
    private var interpolateIndex = 0
    private var rotate = false

    private var seconds: Double = 0.0
    private val paint = Paint().apply {
        isAntiAlias = true
        isDither = true
    }
    private val paintShader = Paint().apply {
        isAntiAlias = true
        isDither = true
    }
    private val attacksList = ArrayList<Array<String>>()

    // Rendering mode
    private var useCanvas = true
    private var currentCanvas: Canvas? = null
    private var glRenderer: SpriteGLRenderer? = null

    /**
     * Constructor with bitmap array for each frame
     */
    constructor(frames: Array<Bitmap>, timeFrame: Float) {
        this.frames = frames
        this.frameTime = timeFrame / frames.size
        this.frameIndex = 0
    }

    /**
     * Constructor that creates sprite from a single bitmap resource
     */
    constructor(sprite: Bitmap, sizeX: Int, sizeY: Int, timeFrame: Float) {
        val frameCount = sizeX * sizeY
        frames = Array(frameCount) {
            Bitmap.createBitmap(1, 1, Bitmap.Config.ARGB_8888)
        }

        val frameWidth = sprite.width / sizeX
        val frameHeight = sprite.height / sizeY
        var count = 0

        try {
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
        } catch (e: Exception) {
            e.printStackTrace()
        }

        this.frameIndex = 0
        this.frameTime = timeFrame / frames.size
    }

    /**
     * Constructor with OpenGL context for GL rendering
     */
    constructor(context: Context, frames: Array<Bitmap>, timeFrame: Float) {
        this.frames = frames
        this.frameTime = timeFrame / frames.size
        this.frameIndex = 0
        this.useCanvas = false

        // Initialize OpenGL renderer
        this.glRenderer = SpriteGLRenderer(context, frames)
    }

    fun play() {
        isPlaying = true
        frameIndex = 0
        lastFrame = System.currentTimeMillis()

        // If using OpenGL, the renderer handles its own frame timing
        if (!useCanvas) {
            glRenderer?.let { renderer ->
                // The SpriteGLRenderer handles frame updates internally
            }
        }
    }

    fun stop() {
        isPlaying = false
    }

    /**
     * Draw the sprite on canvas
     */
    fun draw(canvas: Canvas, destiny: Rect) {
        if (!isPlaying) return

        when {
            rotate -> {
                canvas.drawBitmap(
                    TransformBitmap.RotateBitmap(frames[frameIndex], 45f),
                    null,
                    destiny,
                    paint
                )
            }

            attacksList.isEmpty() -> {
                canvas.drawBitmap(frames[frameIndex], null, destiny, paint)
            }
        }
    }

    fun opacityDraw(canvas: Canvas, destiny: Rect, transparency: Int) {
        if (transparency == 0) {
            draw(canvas, destiny)
        } else {
            paint.alpha = (transparency * 2.55).toInt()
            draw(canvas, destiny)
            paint.alpha = 255
        }
    }

    fun drawWithShader(canvas: Canvas, destiny: Rect, percent: Int) {
        val backing = Bitmap.createBitmap(
            frames[frameIndex].width,
            frames[frameIndex].height,
            Bitmap.Config.ARGB_8888
        )
        val offscreen = Canvas(backing)
        offscreen.drawBitmap(frames[frameIndex], 0f, 0f, null)

        val paint2 = Paint().apply {
            xfermode = PorterDuffXfermode(PorterDuff.Mode.DST_IN)
            shader = LinearGradient(
                0f, 0f, 0f, frames[0].height.toFloat(),
                0x15000000, 0x00000000, Shader.TileMode.CLAMP
            )
        }

        offscreen.drawRect(
            0f, 0f,
            frames[0].width.toFloat(),
            frames[0].height.toFloat(),
            paint2
        )
        canvas.drawBitmap(backing, null, destiny, paint2)
    }

    /**
     * Draw sprite only once
     */
    fun staticDraw(canvas: Canvas, destiny: Rect) {
        if (!isPlaying) return

        if (frameIndex + 1 == frames.size) {
            isPlaying = false
        } else {
            canvas.drawBitmap(frames[frameIndex], null, destiny, paint)
        }
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

    // ISpriteRenderer implementation
    override fun draw(rect: Rect) {
        if (useCanvas) {
            currentCanvas?.let { canvas ->
                draw(canvas, rect)
            }
        } else {
            glRenderer?.draw(rect)
        }
    }

    override fun update() {
        updateFrame()
        if (!useCanvas) {
            glRenderer?.update()
        }
    }

    private fun updateFrame() {
        if (!isPlaying) return

        lapsedTime = (System.currentTimeMillis() - lastFrame).toFloat()
        seconds += lapsedTime

        if (lapsedTime > frameTime * 1000) {
            frameIndex++
            if (frameIndex >= frames.size) {
                frameIndex = 0
            }
            lastFrame = System.currentTimeMillis()
        }
    }

    // Configuration methods
    fun setCanvas(canvas: Canvas) {
        this.currentCanvas = canvas
    }

    fun setUseCanvas(value: Boolean) {
        this.useCanvas = value
    }

    fun setGlRenderer(renderer: SpriteGLRenderer) {
        this.glRenderer = renderer
    }

    // Additional properties for compatibility
    fun setRotate(rotate: Boolean) {
        this.rotate = rotate
    }

    fun getCurrentFrame(): Bitmap {
        return frames[frameIndex]
    }

    fun getFrameCount(): Int {
        return frames.size
    }

    fun getCurrentFrameIndex(): Int {
        return frameIndex
    }

    fun setFrameIndex(index: Int) {
        if (index in 0 until frames.size) {
            frameIndex = index
        }
    }

    // Methods specific to OpenGL rendering
    fun switchToOpenGL(context: Context) {
        if (useCanvas) {
            useCanvas = false
            glRenderer = SpriteGLRenderer(context, frames)
        }
    }

    fun switchToCanvas() {
        useCanvas = true
        glRenderer = null
    }

    fun isUsingOpenGL(): Boolean {
        return !useCanvas && glRenderer != null
    }
}