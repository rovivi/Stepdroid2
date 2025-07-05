package com.kyagamy.step.engine

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Rect
import android.opengl.GLSurfaceView
import com.kyagamy.step.R
import com.kyagamy.step.common.Common
import javax.microedition.khronos.egl.EGLConfig
import javax.microedition.khronos.opengles.GL10
import kotlin.random.Random

class ArrowSpriteRenderer(private val context: Context) : GLSurfaceView.Renderer {
    private val arrowSprites = mutableListOf<SpriteGLRenderer>()
    private val arrows = mutableListOf<ArrowData>()
    private var screenWidth = 0
    private var screenHeight = 0

    // Configuración de la prueba de estrés
    private val numberOfArrows = 1
    private val arrowSize = 80 // Tamaño más pequeño para las flechas

    // FPS Counter
    private var frameCount = 0
    private var lastFpsTime = System.currentTimeMillis()
    private var currentFps = 0f
    private val fpsUpdateInterval = 500L // Actualizar FPS cada 500ms

    // Callback para enviar FPS al Activity
    var fpsCallback: ((Float, Int) -> Unit)? = null

    data class ArrowData(
        val rect: Rect,
        val spriteIndex: Int,
        var velocityX: Float = 0f,
        var velocityY: Float = 0f
    )

    override fun onSurfaceCreated(gl: GL10?, config: EGLConfig?) {
        // Configurar transparencia
        gl?.glEnable(GL10.GL_BLEND)
        gl?.glBlendFunc(GL10.GL_SRC_ALPHA, GL10.GL_ONE_MINUS_SRC_ALPHA)
        gl?.glClearColor(0.0f, 0.0f, 0.0f, 0.0f) // Fondo transparente

        // Cargar sprites de todos los tipos de flechas (5 tipos)
        loadAllArrowSprites()
        arrowSprites.forEach { sprite ->
            sprite.onSurfaceCreated(gl, config)
        }
    }

    private fun loadAllArrowSprites() {
        arrowSprites.clear()

        // Cargar los 5 tipos de flechas del noteskin
        for (arrowType in 0 until 5) {
            try {
                val pathNS = "NoteSkins/pump/default/"
                val arrowName = Common.PIU_ARROW_NAMES[arrowType]
                val stream = context.assets.open(pathNS + arrowName + "tap.png")
                val bitmap = BitmapFactory.decodeStream(stream)

                if (bitmap != null) {
                    val frames = createFramesFromBitmap(bitmap, 3, 2)
                    val sprite = SpriteGLRenderer(context, frames)
                    arrowSprites.add(sprite)
                } else {
                    // Fallback si no se puede cargar
                    loadFallbackArrow(arrowType)
                }
            } catch (e: Exception) {
                e.printStackTrace()
                loadFallbackArrow(arrowType)
            }
        }
    }

    private fun loadFallbackArrow(arrowType: Int) {
        val opts = BitmapFactory.Options().apply { inPreferredConfig = Bitmap.Config.ARGB_8888 }
        val bitmap =
            BitmapFactory.decodeResource(context.resources, R.drawable.dance_pad_up_on, opts)
        if (bitmap != null) {
            val sprite = SpriteGLRenderer(context, arrayOf(bitmap))
            arrowSprites.add(sprite)
        }
    }

    private fun createFramesFromBitmap(sprite: Bitmap, sizeX: Int, sizeY: Int): Array<Bitmap> {
        val frames = Array<Bitmap>(sizeX * sizeY) {
            Bitmap.createBitmap(1, 1, Bitmap.Config.ARGB_8888)
        }

        val frameWidth = sprite.width / sizeX
        val frameHeight = sprite.height / sizeY
        var count = 0

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

        return frames
    }


    override fun onSurfaceChanged(gl: GL10?, width: Int, height: Int) {
        arrowSprites.forEach { sprite ->
            sprite.onSurfaceChanged(gl, width, height)
        }

        screenWidth = width
        screenHeight = height

        // Generar las 1000 flechas
        generateStressTestArrows()
    }

    private fun generateStressTestArrows() {
        arrows.clear()
        if (screenWidth > 0 && screenHeight > 0 && arrowSprites.isNotEmpty()) {
            repeat(numberOfArrows) {
                val left = Random.nextInt(0, screenWidth - arrowSize)
                val top = Random.nextInt(0, screenHeight - arrowSize)
                val rect = Rect(left, top, left + arrowSize, top + arrowSize)
                val spriteIndex = Random.nextInt(arrowSprites.size)

                // Velocidades aleatorias para movimiento
                val velocityX = Random.nextFloat() * 4f - 2f // -2 a 2
                val velocityY = Random.nextFloat() * 4f - 2f // -2 a 2

                arrows.add(ArrowData(rect, spriteIndex, velocityX, velocityY))
            }
        }
    }

    override fun onDrawFrame(gl: GL10?) {
        // Limpiar con transparencia
        gl?.glClear(GL10.GL_COLOR_BUFFER_BIT or GL10.GL_DEPTH_BUFFER_BIT)

        // Actualizar contador de FPS
        updateFpsCounter()

        // Actualizar posiciones de las flechas
        updateArrowPositions()

        // Renderizar todas las flechas
        arrows.forEach { arrow ->
            if (arrow.spriteIndex < arrowSprites.size) {
                val sprite = arrowSprites[arrow.spriteIndex]
                sprite.draw(arrow.rect)
                sprite.onDrawFrame(gl)
                sprite.update()
            }
        }
    }

    private fun updateFpsCounter() {
        frameCount++
        val currentTime = System.currentTimeMillis()

        if (currentTime - lastFpsTime >= fpsUpdateInterval) {
            val deltaTime = (currentTime - lastFpsTime) / 1000f
            currentFps = frameCount / deltaTime

            // Enviar FPS al Activity a través del callback
            fpsCallback?.invoke(currentFps, numberOfArrows)

            frameCount = 0
            lastFpsTime = currentTime
        }
    }

    private fun updateArrowPositions() {
        arrows.forEach { arrow ->
            // Mover la flecha
            arrow.rect.offset(arrow.velocityX.toInt(), arrow.velocityY.toInt())

            // Rebotar en los bordes
            if (arrow.rect.left <= 0 || arrow.rect.right >= screenWidth) {
                arrow.velocityX = -arrow.velocityX
                // Asegurar que esté dentro de los límites
                if (arrow.rect.left < 0) {
                    arrow.rect.offsetTo(0, arrow.rect.top)
                }
                if (arrow.rect.right > screenWidth) {
                    arrow.rect.offsetTo(screenWidth - arrowSize, arrow.rect.top)
                }
            }

            if (arrow.rect.top <= 0 || arrow.rect.bottom >= screenHeight) {
                arrow.velocityY = -arrow.velocityY
                // Asegurar que esté dentro de los límites
                if (arrow.rect.top < 0) {
                    arrow.rect.offsetTo(arrow.rect.left, 0)
                }
                if (arrow.rect.bottom > screenHeight) {
                    arrow.rect.offsetTo(arrow.rect.left, screenHeight - arrowSize)
                }
            }
        }
    }
}
