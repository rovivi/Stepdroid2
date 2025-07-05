package com.kyagamy.step.engine

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Rect
import android.opengl.GLES20
import android.opengl.GLSurfaceView
import android.opengl.Matrix
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
    private val numberOfArrows = 3000
    private val arrowSize = 48 // Tamaño más pequeño y realista para las flechas

    // FPS Counter
    private var frameCount = 0
    private var lastFpsTime = System.currentTimeMillis()
    private var currentFps = 0f
    private val fpsUpdateInterval = 500L // Actualizar FPS cada 500ms

    // Callback para enviar FPS al Activity
    var fpsCallback: ((Float, Int) -> Unit)? = null

    // Optimización: usar un solo renderer para todo el batching
    private lateinit var batchRenderer: SpriteGLRenderer
    private var lastUpdateTime = System.currentTimeMillis()

    data class ArrowData(
        var x: Float,
        var y: Float,
        val spriteIndex: Int,
        var velocityX: Float = 0f,
        var velocityY: Float = 0f,
        var animationTime: Long = 0L
    )

    override fun onSurfaceCreated(gl: GL10?, config: EGLConfig?) {
        // Configurar transparencia
        GLES20.glEnable(GLES20.GL_BLEND)
        GLES20.glBlendFunc(GLES20.GL_SRC_ALPHA, GLES20.GL_ONE_MINUS_SRC_ALPHA)
        GLES20.glClearColor(0.0f, 0.0f, 0.0f, 0.0f) // Fondo transparente

        // Cargar sprites de todos los tipos de flechas (5 tipos)
        loadAllArrowSprites()

        // Inicializar cada sprite renderer
        arrowSprites.forEach { sprite ->
            sprite.onSurfaceCreated(gl, config)
        }

        // Usar el primer sprite como batch renderer principal
        if (arrowSprites.isNotEmpty()) {
            batchRenderer = arrowSprites[0]
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

        // Generar las flechas para el stress test
        generateStressTestArrows()
    }

    private fun generateStressTestArrows() {
        arrows.clear()
        if (screenWidth > 0 && screenHeight > 0 && arrowSprites.isNotEmpty()) {
            repeat(numberOfArrows) {
                val x = Random.nextFloat() * (screenWidth - arrowSize)
                val y = Random.nextFloat() * (screenHeight - arrowSize)
                val spriteIndex = Random.nextInt(arrowSprites.size)

                // Velocidades aleatorias para movimiento
                val velocityX = Random.nextFloat() * 4f - 2f // -2 a 2
                val velocityY = Random.nextFloat() * 4f - 2f // -2 a 2

                arrows.add(ArrowData(x, y, spriteIndex, velocityX, velocityY))
            }
        }
    }

    override fun onDrawFrame(gl: GL10?) {
        // Limpiar con transparencia
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT or GLES20.GL_DEPTH_BUFFER_BIT)

        // Actualizar contador de FPS
        updateFpsCounter()

        // Actualizar posiciones y animaciones
        val currentTime = System.currentTimeMillis()
        val deltaTime = currentTime - lastUpdateTime
        lastUpdateTime = currentTime

        updateArrowPositions()
        updateAnimations(deltaTime)

        // Usar el sistema de batching optimizado
        renderArrowsBatched()
    }

    private fun updateAnimations(deltaTime: Long) {
        // Actualizar animaciones de todos los sprites
        arrowSprites.forEach { sprite ->
            sprite.update(deltaTime)
        }

        // Actualizar tiempo de animación de cada flecha
        arrows.forEach { arrow ->
            arrow.animationTime += deltaTime
        }
    }

    private fun renderArrowsBatched() {
        // Limpiar comandos previos
        arrowSprites.forEach { it.clearCommands() }

        // Preparar comandos de dibujo para cada flecha
        arrows.forEach { arrow ->
            if (arrow.spriteIndex < arrowSprites.size) {
                val sprite = arrowSprites[arrow.spriteIndex]
                val textureId = sprite.getCurrentTextureId()

                // Crear matriz modelo
                val model = FloatArray(16)
                Matrix.setIdentityM(model, 0)
                Matrix.translateM(model, 0, arrow.x + arrowSize / 2f, arrow.y + arrowSize / 2f, 0f)
                Matrix.scaleM(model, 0, arrowSize.toFloat() / 2f, arrowSize.toFloat() / 2f, 1f)

                // UV offset por defecto (toda la textura)
                val uvOff = floatArrayOf(0f, 0f, 1f, 1f)

                // Encolar comando de dibujo
                sprite.drawCommand(textureId, model, uvOff)
            }
        }

        // Ejecutar todos los lotes de dibujo
        arrowSprites.forEach { sprite ->
            sprite.flushBatch()
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
            arrow.x += arrow.velocityX
            arrow.y += arrow.velocityY

            // Rebotar en los bordes con mejor detección
            if (arrow.x < 0) {
                arrow.x = 0f
                arrow.velocityX = -arrow.velocityX
            } else if (arrow.x + arrowSize > screenWidth) {
                arrow.x = (screenWidth - arrowSize).toFloat()
                arrow.velocityX = -arrow.velocityX
            }

            if (arrow.y < 0) {
                arrow.y = 0f
                arrow.velocityY = -arrow.velocityY
            } else if (arrow.y + arrowSize > screenHeight) {
                arrow.y = (screenHeight - arrowSize).toFloat()
                arrow.velocityY = -arrow.velocityY
            }
        }
    }
}
