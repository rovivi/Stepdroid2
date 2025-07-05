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
    private val arrowTextures = mutableListOf<Int>()
    private val arrows = mutableListOf<ArrowData>()
    private var screenWidth = 0
    private var screenHeight = 0

    // Configuración de la prueba de estrés
    private val numberOfArrows = 5000
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

    // Matrices pre-calculadas para evitar allocations
    private val tempMatrix = FloatArray(16)

    data class ArrowData(
        var x: Float,
        var y: Float,
        val arrowType: Int, // Tipo de flecha (0-4)
        val baseTextureIds: List<Int>, // Lista de IDs de textura para animación
        var currentFrameIndex: Int = 0, // Frame actual de animación
        var velocityX: Float = 0f,
        var velocityY: Float = 0f,
        var animationTime: Long = 0L,
        var rotation: Float = 0f
    ) {
        fun getCurrentTextureId(batchRenderer: SpriteGLRenderer): Int {
            return if (baseTextureIds.isNotEmpty()) {
                val frameIndex = baseTextureIds[currentFrameIndex % baseTextureIds.size]
                batchRenderer.getTextureId(frameIndex)
            } else {
                batchRenderer.getCurrentTextureId()
            }
        }
    }

    override fun onSurfaceCreated(gl: GL10?, config: EGLConfig?) {
        // Configurar transparencia
        GLES20.glEnable(GLES20.GL_BLEND)
        GLES20.glBlendFunc(GLES20.GL_SRC_ALPHA, GLES20.GL_ONE_MINUS_SRC_ALPHA)
        GLES20.glClearColor(0.0f, 0.0f, 0.0f, 0.0f) // Fondo transparente

        // Cargar sprites de todos los tipos de flechas (5 tipos)
        loadAllArrowSprites()

        // Inicializar el batch renderer
        if (::batchRenderer.isInitialized) {
            batchRenderer.onSurfaceCreated(gl, config)
        }
    }

    private fun loadAllArrowSprites() {
        // Crear un conjunto de bitmaps para todas las flechas
        val allFrames = mutableListOf<Bitmap>()
        val arrowTypeMapping =
            mutableListOf<Int>() // Mapear qué frames pertenecen a qué tipo de flecha

        // Cargar los 5 tipos de flechas del noteskin
        for (arrowType in 0 until 5) {
            try {
                val pathNS = "NoteSkins/pump/default/"
                val arrowName = Common.PIU_ARROW_NAMES[arrowType]
                val stream = context.assets.open(pathNS + arrowName + "tap.png")
                val bitmap = BitmapFactory.decodeStream(stream)

                if (bitmap != null) {
                    val frames = createFramesFromBitmap(bitmap, 3, 2)
                    val startIndex = allFrames.size
                    allFrames.addAll(frames)
                    // Recordar que estos frames pertenecen a este tipo de flecha
                    repeat(frames.size) { arrowTypeMapping.add(arrowType) }
                } else {
                    // Fallback si no se puede cargar
                    loadFallbackArrow(allFrames, arrowTypeMapping, arrowType)
                }
            } catch (e: Exception) {
                e.printStackTrace()
                loadFallbackArrow(allFrames, arrowTypeMapping, arrowType)
            }
        }

        // Crear un solo renderer con todas las texturas
        if (allFrames.isNotEmpty()) {
            batchRenderer = SpriteGLRenderer(context, allFrames.toTypedArray())
            // Guardar el mapeo para usar más tarde
            (batchRenderer as? SpriteGLRenderer)?.let { renderer ->
                // Almacenar información sobre qué texturas corresponden a qué tipos
                storeArrowTypeInfo(arrowTypeMapping)
            }
        } else {
            // Fallback completo
            val opts = BitmapFactory.Options().apply { inPreferredConfig = Bitmap.Config.ARGB_8888 }
            val bitmap =
                BitmapFactory.decodeResource(context.resources, R.drawable.dance_pad_up_on, opts)
            if (bitmap != null) {
                batchRenderer = SpriteGLRenderer(context, arrayOf(bitmap))
            }
        }
    }

    private fun loadFallbackArrow(
        allFrames: MutableList<Bitmap>,
        arrowTypeMapping: MutableList<Int>,
        arrowType: Int
    ) {
        val opts = BitmapFactory.Options().apply { inPreferredConfig = Bitmap.Config.ARGB_8888 }
        val bitmap =
            BitmapFactory.decodeResource(context.resources, R.drawable.dance_pad_up_on, opts)
        if (bitmap != null) {
            allFrames.add(bitmap)
            arrowTypeMapping.add(arrowType)
        }
    }

    // Información sobre los tipos de flechas cargadas
    private val arrowTypeToTextureIds = mutableMapOf<Int, List<Int>>()

    private fun storeArrowTypeInfo(arrowTypeMapping: List<Int>) {
        // Agrupar los IDs de textura por tipo de flecha
        arrowTypeMapping.forEachIndexed { textureIndex, arrowType ->
            val currentList = arrowTypeToTextureIds[arrowType] ?: emptyList()
            arrowTypeToTextureIds[arrowType] = currentList + textureIndex
        }

        // Debug: imprimir información sobre las texturas cargadas
        android.util.Log.d("ArrowSpriteRenderer", "Loaded arrow types:")
        arrowTypeToTextureIds.forEach { (arrowType, textureIndices) ->
            android.util.Log.d(
                "ArrowSpriteRenderer",
                "Arrow type $arrowType: frames ${textureIndices}"
            )
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
        if (::batchRenderer.isInitialized) {
            batchRenderer.onSurfaceChanged(gl, width, height)
        }

        screenWidth = width
        screenHeight = height

        // Generar las flechas para el stress test
        generateStressTestArrows()
    }

    private fun generateStressTestArrows() {
        arrows.clear()
        if (screenWidth > 0 && screenHeight > 0 && ::batchRenderer.isInitialized) {
            android.util.Log.d("ArrowSpriteRenderer", "Generating $numberOfArrows arrows")
            android.util.Log.d(
                "ArrowSpriteRenderer",
                "Total textures available: ${batchRenderer.getTextureCount()}"
            )

            val textureUsageCount = mutableMapOf<Int, Int>()

            repeat(numberOfArrows) {
                // Posiciones completamente aleatorias en toda la pantalla
                val x = Random.nextFloat() * (screenWidth - arrowSize)
                val y = Random.nextFloat() * (screenHeight - arrowSize)

                // Elegir un tipo de flecha aleatorio (0-4)
                val arrowType = Random.nextInt(5)
                val textureIds = arrowTypeToTextureIds[arrowType]

                // Crear ArrowData con la lista de IDs de textura para animación
                val arrowData = ArrowData(
                    x,
                    y,
                    arrowType,
                    textureIds ?: emptyList(),
                    0,
                    Random.nextFloat() * 6f - 3f, // -3 a 3 (más rápido)
                    Random.nextFloat() * 6f - 3f, // -3 a 3 (más rápido)
                    0L,
                    Random.nextFloat() * 360f // Rotación inicial aleatoria
                )

                // Contar uso de texturas para debug
                val firstTextureId = arrowData.baseTextureIds.firstOrNull() ?: -1
                textureUsageCount[firstTextureId] = (textureUsageCount[firstTextureId] ?: 0) + 1

                arrows.add(arrowData)

                // Debug cada 1000 flechas para verificar posiciones
                if ((it + 1) % 1000 == 0) {
                    android.util.Log.d(
                        "ArrowSpriteRenderer",
                        "Arrow ${it + 1}: x=$x, y=$y, textureId=${arrowData.baseTextureIds.firstOrNull()}"
                    )
                }
            }

            // Debug: mostrar distribución de texturas
            android.util.Log.d("ArrowSpriteRenderer", "Texture usage distribution:")
            textureUsageCount.forEach { (textureId, count) ->
                android.util.Log.d("ArrowSpriteRenderer", "Texture ID $textureId: $count arrows")
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
        renderArrowsOptimized()
    }

    private fun updateAnimations(deltaTime: Long) {
        // Actualizar animaciones del batch renderer
        if (::batchRenderer.isInitialized) {
            batchRenderer.update(deltaTime)
        }

        // Actualizar tiempo de animación de cada flecha
        arrows.forEach { arrow ->
            arrow.animationTime += deltaTime

            // Cambiar frame de animación cada 200ms
            if (arrow.animationTime > 200 && arrow.baseTextureIds.isNotEmpty()) {
                arrow.currentFrameIndex = (arrow.currentFrameIndex + 1) % arrow.baseTextureIds.size
                arrow.animationTime = 0L
            }

            // Rotación más sutil y lenta
            arrow.rotation += deltaTime * 0.02f // Mucho más lento
            if (arrow.rotation > 360f) arrow.rotation -= 360f
        }
    }

    private fun renderArrowsOptimized() {
        if (!::batchRenderer.isInitialized) return

        // Iniciar el lote de dibujo
        batchRenderer.begin()

        // Encolar todos los comandos de dibujo
        arrows.forEach { arrow ->
            // Usar la función optimizada para crear la matriz de transformación
            val model = batchRenderer.createTransformMatrix(
                arrow.x + arrowSize / 2f,
                arrow.y + arrowSize / 2f,
                arrowSize.toFloat() / 2f,
                arrowSize.toFloat() / 2f,
                arrow.rotation
            )

            // UV coordinates por defecto (toda la textura)
            val uvCoords = UVCoords()

            // Obtener la textura actual de la flecha
            val textureId = arrow.getCurrentTextureId(batchRenderer)

            // Encolar comando de dibujo
            batchRenderer.drawCommand(textureId, model, uvCoords)
        }

        // Ejecutar el lote de dibujo
        batchRenderer.end()
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
