package com.kyagamy.step.engine

import android.content.Context
import android.graphics.Point
import android.graphics.Rect
import android.opengl.GLES20
import android.opengl.Matrix
import com.kyagamy.step.common.step.CommonSteps
import com.kyagamy.step.common.step.Game.GameRow
import com.kyagamy.step.common.step.Game.NOT_DRAWABLE
import com.kyagamy.step.game.newplayer.NoteSkin
import game.Note
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.FloatBuffer
import kotlin.math.abs

class StepsDrawerGL(
    private val context: Context?,
    gameModeStr: String?,
    aspectRatio: String,
    landScape: Boolean,
    screenSize: Point
) : ISpriteRenderer {

    // Enums
    enum class GameMode(val value: String, val steps: Int) {
        PUMP_ROUTINE("pump-routine", 10),
        PUMP_DOUBLE("pump-double", 10),
        PUMP_HALFDOUBLE("pump-halfdouble", 10),
        PUMP_SINGLE("pump-single", 5),
        DANCE_SINGLE("dance-single", 4),
        EMPTY("", 0);

        companion object {
            fun fromString(value: String?): GameMode {
                for (mode in entries) {
                    if (mode.value == value) {
                        return mode
                    }
                }
                return EMPTY
            }
        }
    }

    enum class SkinType {
        SELECTED, ROUTINE0, ROUTINE1, ROUTINE2, ROUTINE3
    }

    // OpenGL rendering variables
    private var program = 0
    private var positionHandle = 0
    private var texHandle = 0
    private var mvpMatrixHandle = 0
    private var textureHandle = 0
    private lateinit var vertexBuffer: FloatBuffer
    private lateinit var texBuffer: FloatBuffer
    private val mvpMatrix = FloatArray(16)
    private val projectionMatrix = FloatArray(16)
    private val viewMatrix = FloatArray(16)
    private val modelMatrix = FloatArray(16)
    private val tempMatrix = FloatArray(16)

    // Game fields
    var sizeX: Int = 0
    var sizeY: Int = 0
    var sizeNote: Int = 0
    var scaledNoteSize: Int = 0
    var offsetX: Int = 0
    var offsetY: Int = 0
    private var posInitialX: Int = 0
    private var startValueY: Int = 0
    private var viewWidth = 0
    private var viewHeight = 0

    private val gameMode: GameMode
    private val steps: Int
    private val noteSkins = arrayOfNulls<NoteSkin>(SkinType.values().size)
    private val lastPositionDraw: IntArray = IntArray(10) { NOT_USED }

    // Reusable objects
    private val drawRect: Rect = Rect()

    init {
        this.gameMode = GameMode.fromString(gameModeStr)
        this.steps = gameMode.steps
        calculateDimensions(aspectRatio, landScape, screenSize)
        initializeNoteSkins(context)
        initializeDrawingValues()
        initializeGL()
    }

    private fun initializeGL() {
        // Initialize vertex buffer for quad
        val vertices = floatArrayOf(
            -1f, 1f,   // Top left
            -1f, -1f,  // Bottom left
            1f, 1f,    // Top right
            1f, -1f    // Bottom right
        )

        val texCoords = floatArrayOf(
            0f, 0f,    // Top left
            0f, 1f,    // Bottom left
            1f, 0f,    // Top right
            1f, 1f     // Bottom right
        )

        vertexBuffer = ByteBuffer.allocateDirect(vertices.size * 4)
            .order(ByteOrder.nativeOrder())
            .asFloatBuffer()
        vertexBuffer.put(vertices).position(0)

        texBuffer = ByteBuffer.allocateDirect(texCoords.size * 4)
            .order(ByteOrder.nativeOrder())
            .asFloatBuffer()
        texBuffer.put(texCoords).position(0)
    }

    fun initializeGLProgram() {
        // Initialize shader program
        program = createProgram(VERTEX_SHADER, FRAGMENT_SHADER)
        positionHandle = GLES20.glGetAttribLocation(program, "aPosition")
        texHandle = GLES20.glGetAttribLocation(program, "aTexCoord")
        mvpMatrixHandle = GLES20.glGetUniformLocation(program, "uMVPMatrix")
        textureHandle = GLES20.glGetUniformLocation(program, "uTexture")
    }

    fun setViewport(width: Int, height: Int) {
        viewWidth = width
        viewHeight = height

        // Set up projection matrix
        val ratio = width.toFloat() / height.toFloat()
        Matrix.orthoM(projectionMatrix, 0, 0f, width.toFloat(), height.toFloat(), 0f, -1f, 1f)
        Matrix.setIdentityM(viewMatrix, 0)
        Matrix.multiplyMM(mvpMatrix, 0, projectionMatrix, 0, viewMatrix, 0)
    }

    private fun calculateDimensions(aspectRatio: String, landScape: Boolean, screenSize: Point) {
        posInitialX = (screenSize.x * SCREEN_WIDTH_FACTOR).toInt()

        if (landScape) {
            calculateLandscapeDimensions(screenSize)
        } else {
            calculatePortraitDimensions(screenSize)
        }

        sizeNote = (sizeY / STEPS_Y_COUNT).toInt()
        scaledNoteSize = (sizeNote * NOTE_SCALE_FACTOR).toInt()
        posInitialX = (((sizeX) - (sizeNote * gameMode.steps))) / 2 + offsetX / 2
    }

    private fun calculateLandscapeDimensions(screenSize: Point) {
        sizeY = screenSize.y
        sizeX = (screenSize.y * ASPECT_RATIO_16_9_CALC).toInt()
        offsetX = ((screenSize.x - sizeX) / 2f).toInt()

        if (sizeX > screenSize.x) {
            sizeY = (screenSize.x / ASPECT_RATIO_16_9_CALC).toInt()
            sizeX = (sizeY * ASPECT_RATIO_16_9_CALC).toInt()
            offsetX = abs(((screenSize.x - sizeX) / 2f).toInt())
            offsetY = ((screenSize.y - sizeY) / 2f).toInt()
        }

        sizeX += offsetX / 2
        sizeY += offsetY
    }

    private fun calculatePortraitDimensions(screenSize: Point) {
        sizeY = screenSize.y / 2
        sizeX = screenSize.x

        if ((sizeY / STEPS_Y_COUNT).toInt() * gameMode.steps > sizeX) {
            sizeY = (sizeX / (gameMode.steps + 0.2) * STEPS_Y_COUNT).toInt()
            offsetY = screenSize.y - sizeY
        }
    }

    private fun initializeNoteSkins(context: Context?) {
        when (gameMode) {
            GameMode.PUMP_ROUTINE -> {
                noteSkins[SkinType.ROUTINE0.ordinal] = NoteSkin(context, gameMode.value, "routine1")
                noteSkins[SkinType.ROUTINE1.ordinal] = NoteSkin(context, gameMode.value, "routine2")
                noteSkins[SkinType.ROUTINE2.ordinal] = NoteSkin(context, gameMode.value, "routine3")
                noteSkins[SkinType.ROUTINE3.ordinal] = NoteSkin(context, gameMode.value, "soccer")
            }

            GameMode.PUMP_DOUBLE, GameMode.PUMP_HALFDOUBLE, GameMode.PUMP_SINGLE -> {
                noteSkins[SkinType.SELECTED.ordinal] = NoteSkin(context, gameMode.value, "prime")
            }

            GameMode.DANCE_SINGLE, GameMode.EMPTY -> {}
        }
    }

    private fun initializeDrawingValues() {
        startValueY = (sizeNote * RECEPTOR_Y_FACTOR).toInt()
        resetLastPositionDraw()
    }

    private fun resetLastPositionDraw() {
        for (i in lastPositionDraw.indices) {
            lastPositionDraw[i] = NOT_USED
        }
    }

    fun drawGame(listRow: ArrayList<GameRow>) {
        if (program == 0) return

        GLES20.glUseProgram(program)
        GLES20.glEnable(GLES20.GL_BLEND)
        GLES20.glBlendFunc(GLES20.GL_SRC_ALPHA, GLES20.GL_ONE_MINUS_SRC_ALPHA)

        resetLastPositionDraw()
        drawReceptorsAndEffects()
        drawNotes(listRow)
    }

    private fun drawReceptorsAndEffects() {
        val selectedSkin = noteSkins[SkinType.SELECTED.ordinal] ?: return

        for (j in 0 until steps) {
            val startNoteX = posInitialX + sizeNote * j
            val endNoteX = startNoteX + scaledNoteSize

            // Draw receptors
            drawRect.set(startNoteX, startValueY, endNoteX, startValueY + scaledNoteSize)
            drawSprite(drawRect, selectedSkin.receptors[j])

            // Draw effects
            drawSprite(drawRect, selectedSkin.explotions[j])
            drawSprite(drawRect, selectedSkin.explotionTails[j])
            drawSprite(drawRect, selectedSkin.tapsEffect[j])
        }
    }

    private fun drawNotes(listRow: ArrayList<GameRow>) {
        for (gameRow in listRow) {
            val notes = gameRow.notes
            if (notes != null) {
                for (count in notes.indices) {
                    val note = notes[count]
                    if (note.type != CommonSteps.NOTE_EMPTY) {
                        drawSingleNote(note, gameRow, count)
                    }
                }
            }
        }
    }

    private fun drawSingleNote(note: Note, gameRow: GameRow, columnIndex: Int) {
        val selectedSkin = noteSkins[SkinType.SELECTED.ordinal] ?: return
        val startNoteX = posInitialX + sizeNote * columnIndex
        val endNoteX = startNoteX + scaledNoteSize

        when (note.type) {
            CommonSteps.NOTE_TAP, CommonSteps.NOTE_FAKE -> {
                drawRect.set(
                    startNoteX,
                    gameRow.getPosY(),
                    endNoteX,
                    gameRow.getPosY() + scaledNoteSize
                )
                drawSprite(drawRect, selectedSkin.arrows[columnIndex])
            }

            CommonSteps.NOTE_LONG_START -> {
                drawLongNote(note, gameRow, startNoteX, endNoteX, columnIndex, selectedSkin)
            }

            CommonSteps.NOTE_LONG_BODY -> {
                drawLongNoteBody(note, gameRow, startNoteX, endNoteX, columnIndex, selectedSkin)
            }

            CommonSteps.NOTE_MINE -> {
                drawRect.set(
                    startNoteX,
                    gameRow.getPosY(),
                    endNoteX,
                    gameRow.getPosY() + scaledNoteSize
                )
                drawSprite(drawRect, selectedSkin.mine)
            }
        }
    }

    private fun drawLongNote(
        note: Note,
        gameRow: GameRow,
        startNoteX: Int,
        endNoteX: Int,
        columnIndex: Int,
        skin: NoteSkin
    ) {
        val startY = gameRow.getPosY()
        val rowEnd = note.rowEnd
        val endYRaw = rowEnd?.getPosY() ?: NOT_DRAWABLE
        val endY = if (endYRaw == NOT_DRAWABLE) sizeY else endYRaw
        lastPositionDraw[columnIndex] = endY + scaledNoteSize

        val bodyOffsetPx = (scaledNoteSize * LONG_NOTE_BODY_OFFSET).toInt()
        val tailDiv = scaledNoteSize / LONG_NOTE_TAIL_OFFSET_DIVISOR
        val bodyTop = startY + bodyOffsetPx
        val bodyBottom = endY + tailDiv
        val headBottom = startY + scaledNoteSize
        val tailBottom = endY + scaledNoteSize

        // Draw body
        drawRect.set(startNoteX, bodyTop, endNoteX, bodyBottom)
        drawSprite(drawRect, skin.longs[columnIndex])

        // Draw tail (if end exists)
        if (endYRaw != NOT_DRAWABLE) {
            drawRect.set(startNoteX, endY, endNoteX, tailBottom)
            drawSprite(drawRect, skin.tails[columnIndex])
        }

        // Draw head
        drawRect.set(startNoteX, startY, endNoteX, headBottom)
        drawSprite(drawRect, skin.arrows[columnIndex])
    }

    private fun drawLongNoteBody(
        note: Note,
        gameRow: GameRow,
        startNoteX: Int,
        endNoteX: Int,
        columnIndex: Int,
        skin: NoteSkin
    ) {
        val currentPosY = gameRow.getPosY()
        if (currentPosY <= lastPositionDraw[columnIndex]) return

        var startY = currentPosY
        if (currentPosY > startValueY && currentPosY < sizeY) {
            startY = startValueY
        }

        val rowEnd = note.rowEnd
        val endYRaw = rowEnd?.getPosY() ?: NOT_DRAWABLE
        val endY = if (endYRaw == NOT_DRAWABLE) sizeY else endYRaw
        lastPositionDraw[columnIndex] = endY

        val bodyOffsetPx = (scaledNoteSize * LONG_NOTE_BODY_OFFSET).toInt()
        val tailDiv = scaledNoteSize / LONG_NOTE_TAIL_OFFSET_DIVISOR
        val bodyTop = startY + bodyOffsetPx
        val bodyBottom = endY + tailDiv
        val headBottom = startY + scaledNoteSize
        val tailBottom = endY + scaledNoteSize

        // Draw body
        drawRect.set(startNoteX, bodyTop, endNoteX, bodyBottom)
        drawSprite(drawRect, skin.longs[columnIndex])

        // Draw tail (if end exists)
        if (endYRaw != NOT_DRAWABLE) {
            drawRect.set(startNoteX, endY, endNoteX, tailBottom)
            drawSprite(drawRect, skin.tails[columnIndex])
        }

        // Draw head
        drawRect.set(startNoteX, startY, endNoteX, headBottom)
        drawSprite(drawRect, skin.arrows[columnIndex])
    }

    private fun drawSprite(rect: Rect, sprite: Any?) {
        if (sprite == null) return

        // Convert screen coordinates to OpenGL coordinates
        val left = rect.left.toFloat() / viewWidth * 2f - 1f
        val right = rect.right.toFloat() / viewWidth * 2f - 1f
        val top = 1f - rect.top.toFloat() / viewHeight * 2f
        val bottom = 1f - rect.bottom.toFloat() / viewHeight * 2f

        val scaleX = (right - left) / 2f
        val scaleY = (top - bottom) / 2f

        Matrix.setIdentityM(modelMatrix, 0)
        Matrix.translateM(modelMatrix, 0, left + scaleX, bottom + scaleY, 0f)
        Matrix.scaleM(modelMatrix, 0, scaleX, scaleY, 1f)
        Matrix.multiplyMM(tempMatrix, 0, mvpMatrix, 0, modelMatrix, 0)

        // Set up vertex attributes
        vertexBuffer.position(0)
        GLES20.glVertexAttribPointer(positionHandle, 2, GLES20.GL_FLOAT, false, 0, vertexBuffer)
        GLES20.glEnableVertexAttribArray(positionHandle)

        texBuffer.position(0)
        GLES20.glVertexAttribPointer(texHandle, 2, GLES20.GL_FLOAT, false, 0, texBuffer)
        GLES20.glEnableVertexAttribArray(texHandle)

        // Set matrix
        GLES20.glUniformMatrix4fv(mvpMatrixHandle, 1, false, tempMatrix, 0)

        // Handle sprite texture binding
        when (sprite) {
            is SpriteGLAdapter -> {
                sprite.bindTexture()
                GLES20.glUniform1i(textureHandle, 0)
            }

            else -> {
                // For sprites that don't have GL texture support yet,
                // we can use a default white texture or skip drawing
                GLES20.glActiveTexture(GLES20.GL_TEXTURE0)
                GLES20.glUniform1i(textureHandle, 0)
            }
        }

        // Draw
        GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP, 0, 4)

        // Cleanup
        when (sprite) {
            is SpriteGLAdapter -> {
                sprite.unbindTexture()
            }
        }

        GLES20.glDisableVertexAttribArray(positionHandle)
        GLES20.glDisableVertexAttribArray(texHandle)
    }

    override fun draw(rect: Rect) {
        // Implementation for ISpriteRenderer interface
        drawSprite(rect, null)
    }

    override fun update() {
        // Update sprites
        for (skinIndex in noteSkins.indices) {
            val currentSkin = noteSkins[skinIndex] ?: continue
            val arrows = currentSkin.arrows
            val tails = currentSkin.tails
            val longs = currentSkin.longs
            val explosions = currentSkin.explotions
            val explosionTails = currentSkin.explotionTails
            val tapsEffect = currentSkin.tapsEffect
            val receptors = currentSkin.receptors

            for (x in arrows.indices) {
                arrows[x].update()
                tails[x].update()
                longs[x].update()
                explosions[x].update()
                explosionTails[x].update()
                tapsEffect[x].update()
                receptors[x].update()
            }
            currentSkin.mine.update()
        }
    }

    val stepsByGameMode: Int
        get() = steps

    fun getNoteSkin(skinType: SkinType): NoteSkin? {
        return noteSkins[skinType.ordinal]
    }

    val selectedSkin: NoteSkin?
        get() = noteSkins[SkinType.SELECTED.ordinal]

    private fun createProgram(vertexSource: String, fragmentSource: String): Int {
        val vertexShader = loadShader(GLES20.GL_VERTEX_SHADER, vertexSource)
        val fragmentShader = loadShader(GLES20.GL_FRAGMENT_SHADER, fragmentSource)

        val program = GLES20.glCreateProgram()
        GLES20.glAttachShader(program, vertexShader)
        GLES20.glAttachShader(program, fragmentShader)
        GLES20.glLinkProgram(program)

        return program
    }

    private fun loadShader(type: Int, shaderCode: String): Int {
        val shader = GLES20.glCreateShader(type)
        GLES20.glShaderSource(shader, shaderCode)
        GLES20.glCompileShader(shader)
        return shader
    }

    // Helper method to create a default white texture for sprites without GL support
    private fun createDefaultTexture(): Int {
        val textureIds = IntArray(1)
        GLES20.glGenTextures(1, textureIds, 0)
        val textureId = textureIds[0]

        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, textureId)

        // Create a simple white 1x1 texture
        val whitePixel = byteArrayOf(255.toByte(), 255.toByte(), 255.toByte(), 255.toByte())
        val buffer = java.nio.ByteBuffer.allocateDirect(whitePixel.size)
        buffer.put(whitePixel)
        buffer.position(0)

        GLES20.glTexImage2D(
            GLES20.GL_TEXTURE_2D,
            0,
            GLES20.GL_RGBA,
            1,
            1,
            0,
            GLES20.GL_RGBA,
            GLES20.GL_UNSIGNED_BYTE,
            buffer
        )

        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_LINEAR)
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_LINEAR)

        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, 0)

        return textureId
    }

    // Method to convert existing sprites to GL-compatible sprites
    fun convertSpritesToGL() {
        for (skinIndex in noteSkins.indices) {
            val currentSkin = noteSkins[skinIndex] ?: continue

            // Convert arrows to GL sprites
            for (i in currentSkin.arrows.indices) {
                val originalSprite = currentSkin.arrows[i]
                // In a complete implementation, you would wrap the original sprite
                // with a SpriteGLAdapter here
            }

            // Convert other sprite types similarly
            // tails, longs, explosions, etc.
        }
    }

    // --- BEGIN: SKIN TESTING METHODS ---

    // Paints all variants for all available skins for testing purposes
    fun prueba() {
        if (program == 0) return

        GLES20.glUseProgram(program)
        GLES20.glEnable(GLES20.GL_BLEND)
        GLES20.glBlendFunc(GLES20.GL_SRC_ALPHA, GLES20.GL_ONE_MINUS_SRC_ALPHA)

        // Scale for smaller viewport
        var currentY = 10 // Starting Y position
        val spriteSize = 30 // Smaller size for viewport section
        val spacing = 5 // Smaller spacing
        val rowHeight = spriteSize + spacing

        // Test all available skins
        for (skinType in SkinType.values()) {
            val skin = noteSkins[skinType.ordinal]
            if (skin != null) {
                currentY += rowHeight
                drawSkinVariants(skin, skinType.name, currentY, spriteSize, spacing)
            }
        }
    }

    // Helper method: draws all the variants of this skin in a single horizontal row
    private fun drawSkinVariants(
        skin: NoteSkin,
        skinName: String,
        startY: Int,
        spriteSize: Int,
        spacing: Int
    ) {
        var currentX = 10 // Starting X position, smaller margin
        val columnWidth = spriteSize + spacing

        // Draw arrows for each step
        for (i in 0 until steps) {
            drawRect.set(currentX, startY, currentX + spriteSize, startY + spriteSize)
            drawSprite(drawRect, skin.arrows[i])
            currentX += columnWidth
        }

        // Draw other sprite types in the same row
        currentX += spacing // Smaller extra spacing between sprite types

        // Draw receptors
        for (i in 0 until steps) {
            drawRect.set(currentX, startY, currentX + spriteSize, startY + spriteSize)
            drawSprite(drawRect, skin.receptors[i])
            currentX += columnWidth
        }

        currentX += spacing

        // Draw long note components
        for (i in 0 until steps) {
            drawRect.set(currentX, startY, currentX + spriteSize, startY + spriteSize)
            drawSprite(drawRect, skin.longs[i])
            currentX += columnWidth
        }

        currentX += spacing

        // Draw tails
        for (i in 0 until steps) {
            drawRect.set(currentX, startY, currentX + spriteSize, startY + spriteSize)
            drawSprite(drawRect, skin.tails[i])
            currentX += columnWidth
        }

        currentX += spacing

        // Draw explosions
        for (i in 0 until steps) {
            drawRect.set(currentX, startY, currentX + spriteSize, startY + spriteSize)
            drawSprite(drawRect, skin.explotions[i])
            currentX += columnWidth
        }

        currentX += spacing

        // Draw explosion tails
        for (i in 0 until steps) {
            drawRect.set(currentX, startY, currentX + spriteSize, startY + spriteSize)
            drawSprite(drawRect, skin.explotionTails[i])
            currentX += columnWidth
        }

        currentX += spacing

        // Draw tap effects
        for (i in 0 until steps) {
            drawRect.set(currentX, startY, currentX + spriteSize, startY + spriteSize)
            drawSprite(drawRect, skin.tapsEffect[i])
            currentX += columnWidth
        }

        currentX += spacing

        // Draw mine
        drawRect.set(currentX, startY, currentX + spriteSize, startY + spriteSize)
        drawSprite(drawRect, skin.mine)
    }

    // Method to draw all skin variants in a grid layout
    fun pruebaGrid() {
        if (program == 0) return

        GLES20.glUseProgram(program)
        GLES20.glEnable(GLES20.GL_BLEND)
        GLES20.glBlendFunc(GLES20.GL_SRC_ALPHA, GLES20.GL_ONE_MINUS_SRC_ALPHA)

        // Scale for smaller viewport
        val spriteSize = 25 // Smaller sprites
        val spacing = 3 // Smaller spacing
        val startX = 10
        val startY = 10
        val columnsPerRow = 12 // More columns for smaller sprites

        var currentX = startX
        var currentY = startY
        var itemCount = 0

        // Test all available skins
        for (skinType in SkinType.values()) {
            val skin = noteSkins[skinType.ordinal]
            if (skin != null) {
                // Draw all sprite types for this skin
                val allSprites = mutableListOf<Any>()

                // Add arrows
                skin.arrows.forEach { allSprites.add(it) }

                // Add receptors
                skin.receptors.forEach { allSprites.add(it) }

                // Add longs
                skin.longs.forEach { allSprites.add(it) }

                // Add tails
                skin.tails.forEach { allSprites.add(it) }

                // Add explosions
                skin.explotions.forEach { allSprites.add(it) }

                // Add explosion tails
                skin.explotionTails.forEach { allSprites.add(it) }

                // Add tap effects
                skin.tapsEffect.forEach { allSprites.add(it) }

                // Add mine
                allSprites.add(skin.mine)

                // Draw all sprites in grid
                for (sprite in allSprites) {
                    drawRect.set(currentX, currentY, currentX + spriteSize, currentY + spriteSize)
                    drawSprite(drawRect, sprite)

                    itemCount++
                    if (itemCount % columnsPerRow == 0) {
                        currentX = startX
                        currentY += spriteSize + spacing
                    } else {
                        currentX += spriteSize + spacing
                    }
                }
            }
        }
    }

    // Method to draw specific skin type components
    fun pruebaSpecificSkin(skinType: SkinType) {
        if (program == 0) return

        val skin = noteSkins[skinType.ordinal] ?: return

        GLES20.glUseProgram(program)
        GLES20.glEnable(GLES20.GL_BLEND)
        GLES20.glBlendFunc(GLES20.GL_SRC_ALPHA, GLES20.GL_ONE_MINUS_SRC_ALPHA)

        // Scale for smaller viewport
        val spriteSize = 40 // Medium size
        val spacing = 8
        val startX = 20
        val startY = 20

        var currentX = startX
        var currentY = startY

        // Draw arrows
        for (i in 0 until steps) {
            drawRect.set(currentX, currentY, currentX + spriteSize, currentY + spriteSize)
            drawSprite(drawRect, skin.arrows[i])
            currentX += spriteSize + spacing
        }

        // Move to next row
        currentY += spriteSize + spacing * 2
        currentX = startX

        // Draw receptors
        for (i in 0 until steps) {
            drawRect.set(currentX, currentY, currentX + spriteSize, currentY + spriteSize)
            drawSprite(drawRect, skin.receptors[i])
            currentX += spriteSize + spacing
        }

        // Move to next row
        currentY += spriteSize + spacing * 2
        currentX = startX

        // Draw effects
        for (i in 0 until steps) {
            drawRect.set(currentX, currentY, currentX + spriteSize, currentY + spriteSize)
            drawSprite(drawRect, skin.explotions[i])
            currentX += spriteSize + spacing
        }
    }

    // --- END: SKIN TESTING METHODS ---

    companion object {
        // Constants
        private const val NOT_USED = -999
        private const val STEPS_Y_COUNT = 9.3913f
        private const val RECEPTOR_Y_FACTOR = 0.7f
        private const val NOTE_SCALE_FACTOR = 1.245f
        private const val SCREEN_WIDTH_FACTOR = 0.1f
        private const val ASPECT_RATIO_4_3 = 0.75f
        private const val ASPECT_RATIO_16_9 = 0.5625f
        private const val ASPECT_RATIO_16_9_CALC = 1.77777778f
        private const val LONG_NOTE_BODY_OFFSET = 0.35f
        private const val LONG_NOTE_TAIL_OFFSET_DIVISOR = 3

        private const val VERTEX_SHADER = """
            attribute vec2 aPosition;
            attribute vec2 aTexCoord;
            varying vec2 vTexCoord;
            uniform mat4 uMVPMatrix;
            void main() {
                vTexCoord = aTexCoord;
                gl_Position = uMVPMatrix * vec4(aPosition, 0.0, 1.0);
            }
        """

        private const val FRAGMENT_SHADER = """
            precision mediump float;
            varying vec2 vTexCoord;
            uniform sampler2D uTexture;
            void main() {
                gl_FragColor = texture2D(uTexture, vTexCoord);
            }
        """
    }
}