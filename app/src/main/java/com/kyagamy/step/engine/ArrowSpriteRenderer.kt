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

    // Configuración del noteskin
    private var selectedNoteSkin = "default"

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
        var rotation: Float = 0f,
        var width: Float = 0f,
        var height: Float = 0f,
        var noteType: NoteType = NoteType.NORMAL,
        var zOrder: Int = 0 // Orden de profundidad para renderizado
    ) {
        fun getCurrentTextureId(renderer: ArrowSpriteRenderer): Int {
            // Seleccionar las texturas correctas según el tipo de nota
            val textureIds = when (noteType) {
                NoteType.NORMAL -> renderer.tapTextureIds[arrowType]
                NoteType.RECEPTOR -> renderer.receptorTextureIds[arrowType]
                NoteType.LONG_HEAD -> renderer.tapTextureIds[arrowType] // Cabeza usa tap
                NoteType.LONG_BODY -> renderer.holdTextureIds[arrowType]
                NoteType.LONG_TAIL -> renderer.holdEndTextureIds[arrowType]
                NoteType.MINE -> renderer.mineTextureIds[arrowType]
                NoteType.EXPLOSION -> renderer.explosionTextureIds[arrowType]
                NoteType.EXPLOSION_TAIL -> renderer.explosionTextureIds[arrowType]
                NoteType.TAP_EFFECT -> renderer.explosionTextureIds[arrowType]
            }

            return if (!textureIds.isNullOrEmpty()) {
                val frameIndex = textureIds[currentFrameIndex % textureIds.size]
                renderer.batchRenderer.getTextureId(frameIndex)
            } else {
                // Fallback a las texturas tap si no se encuentra el tipo específico
                val fallbackIds = renderer.tapTextureIds[arrowType] ?: emptyList()
                if (fallbackIds.isNotEmpty()) {
                    val frameIndex = fallbackIds[currentFrameIndex % fallbackIds.size]
                    renderer.batchRenderer.getTextureId(frameIndex)
                } else {
                    renderer.batchRenderer.getCurrentTextureId()
                }
            }
        }

        companion object {
            // Constantes de Z-order para definir la jerarquía de renderizado
            const val Z_ORDER_LONG_TAIL = 2      // Más atrás
            const val Z_ORDER_LONG_BODY = 1      // Medio
            const val Z_ORDER_RECEPTOR = 0       // Receptores
            const val Z_ORDER_EXPLOSION = 2      // Efectos
            const val Z_ORDER_MINE = 3           // Minas
            const val Z_ORDER_NORMAL = 3         // Notas normales
            const val Z_ORDER_LONG_HEAD = 2      // Cabeza de long notes
            const val Z_ORDER_TAP_EFFECT = 3     // Efectos de tap (más adelante)
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
        // Crear diferentes listas para cada tipo de nota
        val tapFrames = mutableListOf<Bitmap>()
        val holdFrames = mutableListOf<Bitmap>()
        val holdEndFrames = mutableListOf<Bitmap>()
        val receptorFrames = mutableListOf<Bitmap>()
        val explosionFrames = mutableListOf<Bitmap>()
        val mineFrames = mutableListOf<Bitmap>()

        // Mapear qué frames pertenecen a qué tipo de flecha y tipo de nota
        val tapArrowTypeMapping = mutableListOf<Int>()
        val holdArrowTypeMapping = mutableListOf<Int>()
        val holdEndArrowTypeMapping = mutableListOf<Int>()
        val receptorArrowTypeMapping = mutableListOf<Int>()
        val explosionArrowTypeMapping = mutableListOf<Int>()
        val mineArrowTypeMapping = mutableListOf<Int>()

        // Cargar los 5 tipos de flechas del noteskin
        for (arrowType in 0 until 5) {
            try {
                val pathNS = "NoteSkins/pump/$selectedNoteSkin/"
                val arrowName = Common.PIU_ARROW_NAMES[arrowType]

                // Cargar tap (notas normales)
                try {
                    val tapStream = context.assets.open(pathNS + arrowName + "tap.png")
                    val tapBitmap = BitmapFactory.decodeStream(tapStream)
                    if (tapBitmap != null) {
                        val frames = createFramesFromBitmap(tapBitmap, 3, 2)
                        tapFrames.addAll(frames)
                        repeat(frames.size) { tapArrowTypeMapping.add(arrowType) }
                    }
                } catch (e: Exception) {
                    loadFallbackArrow(tapFrames, tapArrowTypeMapping, arrowType)
                }

                // Cargar hold (cuerpo de notas largas)
                try {
                    val holdStream = context.assets.open(pathNS + arrowName + "hold.png")
                    val holdBitmap = BitmapFactory.decodeStream(holdStream)
                    if (holdBitmap != null) {
                        val frames = createFramesFromBitmap(holdBitmap, 6, 1)
                        holdFrames.addAll(frames)
                        repeat(frames.size) { holdArrowTypeMapping.add(arrowType) }
                    }
                } catch (e: Exception) {
                    loadFallbackArrow(holdFrames, holdArrowTypeMapping, arrowType)
                }

                // Cargar hold_end (cola de notas largas)
                try {
                    val holdEndStream = context.assets.open(pathNS + arrowName + "hold_end.png")
                    val holdEndBitmap = BitmapFactory.decodeStream(holdEndStream)
                    if (holdEndBitmap != null) {
                        val frames = createFramesFromBitmap(holdEndBitmap, 6, 1)
                        holdEndFrames.addAll(frames)
                        repeat(frames.size) { holdEndArrowTypeMapping.add(arrowType) }
                    }
                } catch (e: Exception) {
                    loadFallbackArrow(holdEndFrames, holdEndArrowTypeMapping, arrowType)
                }

                // Cargar receptor
                try {
                    val receptorStream = context.assets.open(pathNS + arrowName + "receptor.png")
                    val receptorBitmap = BitmapFactory.decodeStream(receptorStream)
                    if (receptorBitmap != null) {
                        val frames = createFramesFromBitmap(receptorBitmap, 1, 3)
                        receptorFrames.addAll(frames)
                        repeat(frames.size) { receptorArrowTypeMapping.add(arrowType) }
                    }
                } catch (e: Exception) {
                    loadFallbackArrow(receptorFrames, receptorArrowTypeMapping, arrowType)
                }

            } catch (e: Exception) {
                e.printStackTrace()
                // Fallback para todos los tipos
                loadFallbackArrow(tapFrames, tapArrowTypeMapping, arrowType)
                loadFallbackArrow(holdFrames, holdArrowTypeMapping, arrowType)
                loadFallbackArrow(holdEndFrames, holdEndArrowTypeMapping, arrowType)
                loadFallbackArrow(receptorFrames, receptorArrowTypeMapping, arrowType)
                loadFallbackArrow(mineFrames, mineArrowTypeMapping, arrowType)
            }
        }

        // Cargar explosiones globales
        try {
            val explosionStream =
                context.assets.open("NoteSkins/pump/$selectedNoteSkin/_explosion 6x1.png")
            val explosionBitmap = BitmapFactory.decodeStream(explosionStream)
            if (explosionBitmap != null) {
                val frames = createFramesFromBitmap(explosionBitmap, 6, 1)
                explosionFrames.addAll(frames)
                repeat(frames.size) { explosionArrowTypeMapping.add(0) } // Usar tipo 0 para explosiones
            }
        } catch (e: Exception) {
            e.printStackTrace()
            // Fallback para explosiones
            loadFallbackArrow(explosionFrames, explosionArrowTypeMapping, 0)
        }

        // Cargar mina si existe
        try {
            val mineStream = context.assets.open("NoteSkins/pump/$selectedNoteSkin/mine.png")
            val mineBitmap = BitmapFactory.decodeStream(mineStream)
            if (mineBitmap != null) {
                val frames = createFramesFromBitmap(mineBitmap, 3, 2)
                mineFrames.addAll(frames)
                repeat(frames.size) { mineArrowTypeMapping.add(0) }
            }
        } catch (e: Exception) {
            e.printStackTrace()
            // Fallback: usar el drawable por defecto
            val opts = BitmapFactory.Options().apply { inPreferredConfig = Bitmap.Config.ARGB_8888 }
            val mineBitmap = BitmapFactory.decodeResource(context.resources, R.drawable.mine, opts)
            if (mineBitmap != null) {
                val frames = createFramesFromBitmap(mineBitmap, 3, 2)
                mineFrames.addAll(frames)
                repeat(frames.size) { mineArrowTypeMapping.add(0) }
            }
        }

        // Combinar todos los frames en orden específico
        val allFrames = mutableListOf<Bitmap>()
        allFrames.addAll(tapFrames)
        allFrames.addAll(holdFrames)
        allFrames.addAll(holdEndFrames)
        allFrames.addAll(receptorFrames)
        allFrames.addAll(explosionFrames)
        allFrames.addAll(mineFrames)

        // Crear un solo renderer con todas las texturas
        if (allFrames.isNotEmpty()) {
            batchRenderer = SpriteGLRenderer(context, allFrames.toTypedArray())
            // Almacenar información sobre qué texturas corresponden a qué tipos
            storeArrowTypeInfoByNoteType(
                tapArrowTypeMapping, holdArrowTypeMapping, holdEndArrowTypeMapping,
                receptorArrowTypeMapping,
                explosionArrowTypeMapping,
                mineArrowTypeMapping,
                tapFrames.size,
                holdFrames.size,
                holdEndFrames.size,
                receptorFrames.size,
                mineFrames.size
            )
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

    // Mapeo específico por tipo de nota y tipo de flecha
    private val tapTextureIds = mutableMapOf<Int, List<Int>>()
    private val holdTextureIds = mutableMapOf<Int, List<Int>>()
    private val holdEndTextureIds = mutableMapOf<Int, List<Int>>()
    private val receptorTextureIds = mutableMapOf<Int, List<Int>>()
    private val explosionTextureIds = mutableMapOf<Int, List<Int>>()
    private val mineTextureIds = mutableMapOf<Int, List<Int>>()

    // Índices base para cada tipo de nota
    private var tapBaseIndex = 0
    private var holdBaseIndex = 0
    private var holdEndBaseIndex = 0
    private var receptorBaseIndex = 0
    private var explosionBaseIndex = 0
    private var mineBaseIndex = 0

    private fun storeArrowTypeInfoByNoteType(
        tapArrowTypeMapping: List<Int>,
        holdArrowTypeMapping: List<Int>,
        holdEndArrowTypeMapping: List<Int>,
        receptorArrowTypeMapping: List<Int>,
        explosionArrowTypeMapping: List<Int>,
        mineArrowTypeMapping: List<Int>,
        tapFramesSize: Int,
        holdFramesSize: Int,
        holdEndFramesSize: Int,
        receptorFramesSize: Int,
        mineFramesSize: Int
    ) {
        // Guardar índices base
        tapBaseIndex = 0
        holdBaseIndex = tapFramesSize
        holdEndBaseIndex = holdBaseIndex + holdFramesSize
        receptorBaseIndex = holdEndBaseIndex + holdEndFramesSize
        explosionBaseIndex = receptorBaseIndex + receptorFramesSize
        mineBaseIndex = explosionBaseIndex + mineFramesSize

        // Mapear texturas TAP
        var index = tapBaseIndex
        tapArrowTypeMapping.forEach { arrowType ->
            val currentList = tapTextureIds[arrowType] ?: emptyList()
            tapTextureIds[arrowType] = currentList + index
            index++
        }

        // Mapear texturas HOLD
        index = holdBaseIndex
        holdArrowTypeMapping.forEach { arrowType ->
            val currentList = holdTextureIds[arrowType] ?: emptyList()
            holdTextureIds[arrowType] = currentList + index
            index++
        }

        // Mapear texturas HOLD_END
        index = holdEndBaseIndex
        holdEndArrowTypeMapping.forEach { arrowType ->
            val currentList = holdEndTextureIds[arrowType] ?: emptyList()
            holdEndTextureIds[arrowType] = currentList + index
            index++
        }

        // Mapear texturas RECEPTOR
        index = receptorBaseIndex
        receptorArrowTypeMapping.forEach { arrowType ->
            val currentList = receptorTextureIds[arrowType] ?: emptyList()
            receptorTextureIds[arrowType] = currentList + index
            index++
        }

        // Mapear texturas EXPLOSION
        index = explosionBaseIndex
        explosionArrowTypeMapping.forEach { arrowType ->
            val currentList = explosionTextureIds[arrowType] ?: emptyList()
            explosionTextureIds[arrowType] = currentList + index
            index++
        }

        // Mapear texturas MINE
        index = mineBaseIndex
        mineArrowTypeMapping.forEach { arrowType ->
            val currentList = mineTextureIds[arrowType] ?: emptyList()
            mineTextureIds[arrowType] = currentList + index
            index++
        }

        // Mantener compatibilidad con el mapeo original
        // Agrupar los IDs de textura por tipo de flecha para tap (por defecto)
        tapArrowTypeMapping.forEachIndexed { textureIndex, arrowType ->
            val currentList = arrowTypeToTextureIds[arrowType] ?: emptyList()
            arrowTypeToTextureIds[arrowType] = currentList + (tapBaseIndex + textureIndex)
        }

        // Debug: imprimir información sobre las texturas cargadas
        android.util.Log.d("ArrowSpriteRenderer", "Loaded arrow types:")
        android.util.Log.d("ArrowSpriteRenderer", "TAP textures: $tapTextureIds")
        android.util.Log.d("ArrowSpriteRenderer", "HOLD textures: $holdTextureIds")
        android.util.Log.d("ArrowSpriteRenderer", "HOLD_END textures: $holdEndTextureIds")
        android.util.Log.d("ArrowSpriteRenderer", "RECEPTOR textures: $receptorTextureIds")
        android.util.Log.d("ArrowSpriteRenderer", "EXPLOSION textures: $explosionTextureIds")
        android.util.Log.d("ArrowSpriteRenderer", "MINE textures: $mineTextureIds")
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

        // Clear any existing arrows - we'll populate them via populateArrows()
        arrows.clear()
    }

    // Method to add arrows based on game instructions from StepsDrawerGL
    fun populateArrows(gameArrows: List<GameArrowData>) {
        arrows.clear()
        if (screenWidth > 0 && screenHeight > 0 && ::batchRenderer.isInitialized) {
            android.util.Log.d("ArrowSpriteRenderer", "Populating ${gameArrows.size} game arrows")

            for (gameArrow in gameArrows) {
                // Seleccionar las texturas correctas según el tipo de nota
                val textureIds = when (gameArrow.noteType) {
                    NoteType.NORMAL -> tapTextureIds[gameArrow.arrowType]
                    NoteType.RECEPTOR -> receptorTextureIds[gameArrow.arrowType]
                    NoteType.LONG_HEAD -> tapTextureIds[gameArrow.arrowType] // Cabeza usa tap
                    NoteType.LONG_BODY -> holdTextureIds[gameArrow.arrowType]
                    NoteType.LONG_TAIL -> holdEndTextureIds[gameArrow.arrowType]
                    NoteType.MINE -> mineTextureIds[gameArrow.arrowType]
                    NoteType.EXPLOSION -> explosionTextureIds[gameArrow.arrowType]
                    NoteType.EXPLOSION_TAIL -> explosionTextureIds[gameArrow.arrowType]
                    NoteType.TAP_EFFECT -> explosionTextureIds[gameArrow.arrowType]
                }

                val zOrder = when (gameArrow.noteType) {
                    NoteType.NORMAL -> ArrowData.Z_ORDER_NORMAL
                    NoteType.RECEPTOR -> ArrowData.Z_ORDER_RECEPTOR
                    NoteType.LONG_HEAD -> ArrowData.Z_ORDER_LONG_HEAD
                    NoteType.LONG_BODY -> ArrowData.Z_ORDER_LONG_BODY
                    NoteType.LONG_TAIL -> ArrowData.Z_ORDER_LONG_TAIL
                    NoteType.MINE -> ArrowData.Z_ORDER_MINE
                    NoteType.EXPLOSION -> ArrowData.Z_ORDER_EXPLOSION
                    NoteType.EXPLOSION_TAIL -> ArrowData.Z_ORDER_EXPLOSION
                    NoteType.TAP_EFFECT -> ArrowData.Z_ORDER_TAP_EFFECT
                }

                val arrowData = ArrowData(
                    gameArrow.x,
                    gameArrow.y,
                    gameArrow.arrowType,
                    textureIds ?: emptyList(),
                    0, // currentFrameIndex
                    0f, // velocityX - no movement for game arrows
                    0f, // velocityY - no movement for game arrows
                    0L, // animationTime
                    gameArrow.rotation,
                    gameArrow.width,
                    gameArrow.height,
                    gameArrow.noteType,
                    zOrder
                )

                arrows.add(arrowData)
            }

            android.util.Log.d(
                "ArrowSpriteRenderer",
                "Populated ${arrows.size} arrows for rendering"
            )
        }
    }

    // Data class for game arrow instructions
    data class GameArrowData(
        val x: Float,
        val y: Float,
        val width: Float,  // Use actual size from StepsDrawerGL
        val height: Float, // Use actual size from StepsDrawerGL
        val arrowType: Int, // 0-4 for different arrow types
        val noteType: NoteType = NoteType.NORMAL, // Type of note for different sprites
        val rotation: Float = 0f
    )

    // Enum for different note types
    enum class NoteType {
        NORMAL,      // Regular arrows
        RECEPTOR,    // Receptors at bottom
        LONG_HEAD,   // Head of long note
        LONG_BODY,   // Body of long note
        LONG_TAIL,   // Tail of long note
        MINE,        // Mine note
        EXPLOSION,   // Explosion effect
        EXPLOSION_TAIL, // Explosion tail effect
        TAP_EFFECT   // Tap effect
    }

    private fun generateStressTestArrows() {
        // This method is now unused - kept for reference
        // Real arrows are populated via populateArrows()
        // If needed, stress test arrows would also need zOrder assignment:
        // val zOrder = ArrowData.Z_ORDER_NORMAL // or appropriate value
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

            // Only rotate if arrow has velocity (stress test arrows)
            // Game arrows should stay in place
            if (arrow.velocityX != 0f || arrow.velocityY != 0f) {
                arrow.rotation += deltaTime * 0.02f
                if (arrow.rotation > 360f) arrow.rotation -= 360f
            }
        }
    }

    private fun renderArrowsOptimized() {
        if (!::batchRenderer.isInitialized) return

        // Iniciar el lote de dibujo
        batchRenderer.begin()

        // Renderizar las flechas ordenadas por Z-order (profundidad)
        // Z-order más bajo = más atrás (se dibuja primero)
        // Z-order más alto = más adelante (se dibuja encima)
        // Jerarquía: LONG_TAIL (1) -> LONG_BODY (2) -> RECEPTOR (3) -> EXPLOSION (4) -> MINE (5) -> NORMAL (6) -> LONG_HEAD (7) -> TAP_EFFECT (8)
        arrows.sortedBy { it.zOrder }.forEach { arrow ->
            // Usar la función optimizada para crear la matriz de transformación
            val model = batchRenderer.createTransformMatrix(
                arrow.x + arrow.width / 2f,
                arrow.y + arrow.height / 2f,
                arrow.width / 2f,
                arrow.height / 2f,
                arrow.rotation
            )

            // UV coordinates por defecto (toda la textura)
            val uvCoords = UVCoords()

            // Obtener la textura actual de la flecha
            val textureId = arrow.getCurrentTextureId(this)

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
            // Only move arrows that have velocity (stress test arrows)
            // Game arrows should stay in place
            if (arrow.velocityX != 0f || arrow.velocityY != 0f) {
                // Mover la flecha
                arrow.x += arrow.velocityX
                arrow.y += arrow.velocityY

                // Rebotar en los bordes con mejor detección
                if (arrow.x < 0) {
                    arrow.x = 0f
                    arrow.velocityX = -arrow.velocityX
                } else if (arrow.x + arrow.width > screenWidth) {
                    arrow.x = (screenWidth - arrow.width).toFloat()
                    arrow.velocityX = -arrow.velocityX
                }

                if (arrow.y < 0) {
                    arrow.y = 0f
                    arrow.velocityY = -arrow.velocityY
                } else if (arrow.y + arrow.height > screenHeight) {
                    arrow.y = (screenHeight - arrow.height).toFloat()
                    arrow.velocityY = -arrow.velocityY
                }
            }
        }
    }

    // Función para cambiar el noteskin
    fun setNoteSkin(noteSkinName: String) {
        if (selectedNoteSkin != noteSkinName) {
            selectedNoteSkin = noteSkinName
            // Recargar texturas si el contexto GL ya está disponible
            if (::batchRenderer.isInitialized) {
                loadAllArrowSprites()
            }
        }
    }

    fun getNoteSkin(): String = selectedNoteSkin
}
