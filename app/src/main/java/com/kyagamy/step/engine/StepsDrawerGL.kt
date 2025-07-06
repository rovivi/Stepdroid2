package com.kyagamy.step.engine

import android.content.Context
import android.graphics.Point
import android.graphics.Rect
import com.kyagamy.step.common.step.CommonSteps
import com.kyagamy.step.common.step.Game.GameRow
import com.kyagamy.step.common.step.Game.NOT_DRAWABLE
import com.kyagamy.step.game.newplayer.NoteSkin
import game.Note
import game.NoteType
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

    // Game fields
    var sizeX: Int = 0
    var sizeY: Int = 0
    var sizeNote: Int = 0
    var scaledNoteSize: Int = 0
    var offsetX: Int = 0
    var offsetY: Int = 0
    var posInitialX: Int = 0
    private var startValueY: Int = 0
    private var viewWidth = 0
    private var viewHeight = 0

    private val gameMode: GameMode
    private val steps: Int
    private val noteSkins = arrayOfNulls<NoteSkin>(SkinType.values().size)
    private val lastPositionDraw: IntArray = IntArray(10) { NOT_USED }
    private val activeLongNotes: MutableMap<Int, Note> = mutableMapOf()

    // Reusable objects
    private val drawRect: Rect = Rect()

    // Renderer delegation
    private var arrowRenderer: ArrowSpriteRenderer? = null
    private val gameArrows = mutableListOf<ArrowSpriteRenderer.GameArrowData>()

    init {
        this.gameMode = GameMode.fromString(gameModeStr)
        this.steps = gameMode.steps
        calculateDimensions(aspectRatio, landScape, screenSize)
        initializeNoteSkins(context)
        initializeDrawingValues()
    }

    fun setArrowRenderer(renderer: ArrowSpriteRenderer) {
        this.arrowRenderer = renderer
    }

    fun setViewport(width: Int, height: Int) {
        viewWidth = width
        viewHeight = height
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

    fun drawGame(listRow: ArrayList<GameRow>, skinType: SkinType = SkinType.SELECTED) {
        resetLastPositionDraw()
        gameArrows.clear()
        drawReceptorsAndEffects(skinType)
        drawNotes(listRow, skinType)
        arrowRenderer?.populateArrows(gameArrows.toList())
    }

    private fun drawReceptorsAndEffects(skinType: SkinType = SkinType.SELECTED) {
        val selectedSkin = noteSkins[skinType.ordinal] ?: return

        for (j in 0 until steps) {
            val startNoteX = posInitialX + sizeNote * j
            val endNoteX = startNoteX + scaledNoteSize

            // Draw receptors
            drawRect.set(startNoteX, startValueY, endNoteX, startValueY + scaledNoteSize)
            drawSprite(
                drawRect,
                selectedSkin.receptors[j],
                j,
                ArrowSpriteRenderer.NoteType.RECEPTOR
            )

//            // Draw effects
//            drawSprite(
//                drawRect,
//                selectedSkin.explotions[j],
//                j,
//                ArrowSpriteRenderer.NoteType.EXPLOSION
//            )
//            drawSprite(
//                drawRect,
//                selectedSkin.explotionTails[j],
//                j,
//                ArrowSpriteRenderer.NoteType.EXPLOSION_TAIL
//            )
//            drawSprite(
//                drawRect,
//                selectedSkin.tapsEffect[j],
//                j,
//                ArrowSpriteRenderer.NoteType.TAP_EFFECT
//            )
        }
    }

    private fun drawNotes(listRow: ArrayList<GameRow>, skinType: SkinType = SkinType.SELECTED) {
        val selectedSkin = noteSkins[skinType.ordinal] ?: return

        // draw active long notes first
        activeLongNotes.forEach { (index, note) ->
            val startX = posInitialX + sizeNote * index
            val endX = startX + scaledNoteSize
            drawLongNote(note, note.rowOrigin ?: listRow.firstOrNull() ?: return@forEach, startX, endX, index, selectedSkin)
        }

        for (gameRow in listRow) {
            val notes = gameRow.notes
            if (notes != null) {
                for (count in notes.indices) {
                    val note = notes[count]
                    if (note.type != NoteType.EMPTY) {
                        drawSingleNote(note, gameRow, count, skinType)
                    }
                }
            }
        }
    }

    private fun drawSingleNote(
        note: Note,
        gameRow: GameRow,
        columnIndex: Int,
        skinType: SkinType = SkinType.SELECTED
    ) {
        val selectedSkin = noteSkins[skinType.ordinal] ?: return
        val startNoteX = posInitialX + sizeNote * columnIndex
        val endNoteX = startNoteX + scaledNoteSize

        when (note.type) {
            NoteType.TAP, NoteType.FAKE -> {
                drawRect.set(
                    startNoteX,
                    gameRow.getPosY(),
                    endNoteX,
                    gameRow.getPosY() + scaledNoteSize
                )
                drawSprite(
                    drawRect,
                    selectedSkin.arrows[columnIndex],
                    columnIndex,
                    ArrowSpriteRenderer.NoteType.NORMAL
                )
            }
            NoteType.LONG_START -> {
                activeLongNotes[columnIndex] = note
                drawLongNote(note, gameRow, startNoteX, endNoteX, columnIndex, selectedSkin)
            }

            NoteType.LONG_END -> {
                activeLongNotes.remove(columnIndex)
                drawLongNote(note, note.rowOrigin ?: gameRow, startNoteX, endNoteX, columnIndex, selectedSkin)
            }

            NoteType.MINE -> {
                drawRect.set(
                    startNoteX,
                    gameRow.getPosY(),
                    endNoteX,
                    gameRow.getPosY() + scaledNoteSize
                )
                drawSprite(
                    drawRect,
                    selectedSkin.mine,
                    columnIndex,
                    ArrowSpriteRenderer.NoteType.MINE
                )
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

        val layout = NoteLayoutCalculator.calculateLongNote(
            startY,
            endY,
            scaledNoteSize,
            LONG_NOTE_BODY_OFFSET,
            LONG_NOTE_TAIL_OFFSET_DIVISOR
        )

        // Draw body
        drawRect.set(startNoteX, layout.bodyTop, endNoteX, layout.bodyBottom)
        drawSprite(
            drawRect,
            skin.longs[columnIndex],
            columnIndex,
            ArrowSpriteRenderer.NoteType.LONG_BODY
        )

        // Draw tail (if end exists)
        if (endYRaw != NOT_DRAWABLE) {
            drawRect.set(startNoteX, endY, endNoteX, layout.tailBottom)
            drawSprite(
                drawRect,
                skin.tails[columnIndex],
                columnIndex,
                ArrowSpriteRenderer.NoteType.LONG_TAIL
            )
        }

        // Draw head
        drawRect.set(startNoteX, startY, endNoteX, layout.headBottom)
        drawSprite(
            drawRect,
            skin.arrows[columnIndex],
            columnIndex,
            ArrowSpriteRenderer.NoteType.LONG_HEAD
        )
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

        val layout = NoteLayoutCalculator.calculateLongNote(
            startY,
            endY,
            scaledNoteSize,
            LONG_NOTE_BODY_OFFSET,
            LONG_NOTE_TAIL_OFFSET_DIVISOR
        )

        // Draw body
        drawRect.set(startNoteX, layout.bodyTop, endNoteX, layout.bodyBottom)
        drawSprite(
            drawRect,
            skin.longs[columnIndex],
            columnIndex,
            ArrowSpriteRenderer.NoteType.LONG_BODY
        )

        // Draw tail (if end exists)
        if (endYRaw != NOT_DRAWABLE) {
            drawRect.set(startNoteX, endY, endNoteX, layout.tailBottom)
            drawSprite(
                drawRect,
                skin.tails[columnIndex],
                columnIndex,
                ArrowSpriteRenderer.NoteType.LONG_TAIL
            )
        }

        // Draw head
        drawRect.set(startNoteX, startY, endNoteX, layout.headBottom)
        drawSprite(
            drawRect,
            skin.arrows[columnIndex],
            columnIndex,
            ArrowSpriteRenderer.NoteType.LONG_HEAD
        )
    }

    private fun drawSprite(
        rect: Rect,
        sprite: Any?,
        arrowType: Int = 0,
        noteType: ArrowSpriteRenderer.NoteType = ArrowSpriteRenderer.NoteType.NORMAL
    ) {
        if (sprite != null && arrowRenderer != null) {
            if (rect.bottom < -scaledNoteSize || rect.top > viewHeight + scaledNoteSize) return
            // Convert sprite drawing call to arrow instruction using actual calculated sizes
            val gameArrow = ArrowSpriteRenderer.GameArrowData(
                x = rect.left.toFloat(),
                y = rect.top.toFloat(),
                width = rect.width().toFloat(),  // Use actual width from StepsDrawerGL calculations
                height = rect.height()
                    .toFloat(), // Use actual height from StepsDrawerGL calculations
                arrowType = arrowType % 5, // Ensure it's 0-4
                noteType = noteType,
                rotation = 0f
            )
            gameArrows.add(gameArrow)
        }
    }

    // ISpriteRenderer implementation
    override fun begin() {
    }

    override fun drawCommand(textureId: Int, model: FloatArray, uvCoords: UVCoords) {
    }

    override fun end() {
    }

    override fun update(deltaMs: Long) {
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

    // Backward compatibility methods
    @Deprecated("Use begin()/end() pattern instead")
    override fun flushBatch() {
        // No longer needed
    }

    @Deprecated("Use begin()/end() pattern instead")
    override fun clearCommands() {
        // No longer needed
    }

    @Deprecated("Use drawCommand instead")
    override fun draw(rect: Rect) {
        drawSprite(rect, null, 0, ArrowSpriteRenderer.NoteType.NORMAL)
    }

    @Deprecated("Use update(deltaMs) instead")
    override fun update() {
        update(16L)
    }

    val stepsByGameMode: Int
        get() = steps

    fun getNoteSkin(skinType: SkinType): NoteSkin? {
        return noteSkins[skinType.ordinal]
    }

    val selectedSkin: NoteSkin?
        get() = noteSkins[SkinType.SELECTED.ordinal]

    // Testing methods for skin variants
    fun prueba() {
        var currentY = 10
        val spriteSize = 30
        val spacing = 5
        val rowHeight = spriteSize + spacing

        for (skinType in SkinType.values()) {
            val skin = noteSkins[skinType.ordinal]
            if (skin != null) {
                currentY += rowHeight
                drawSkinVariants(skin, skinType.name, currentY, spriteSize, spacing)
            }
        }
    }

    private fun drawSkinVariants(
        skin: NoteSkin,
        skinName: String,
        startY: Int,
        spriteSize: Int,
        spacing: Int
    ) {
        var currentX = 10
        val columnWidth = spriteSize + spacing

        // Draw arrows for each step
        for (i in 0 until steps) {
            drawRect.set(currentX, startY, currentX + spriteSize, startY + spriteSize)
            drawSprite(drawRect, skin.arrows[i], i, ArrowSpriteRenderer.NoteType.NORMAL)
            currentX += columnWidth
        }

        // Draw other sprite types
        currentX += spacing
        for (i in 0 until steps) {
            drawRect.set(currentX, startY, currentX + spriteSize, startY + spriteSize)
            drawSprite(drawRect, skin.receptors[i], i, ArrowSpriteRenderer.NoteType.RECEPTOR)
            currentX += columnWidth
        }

        currentX += spacing
        for (i in 0 until steps) {
            drawRect.set(currentX, startY, currentX + spriteSize, startY + spriteSize)
            drawSprite(drawRect, skin.longs[i], i, ArrowSpriteRenderer.NoteType.LONG_BODY)
            currentX += columnWidth
        }

        currentX += spacing
        for (i in 0 until steps) {
            drawRect.set(currentX, startY, currentX + spriteSize, startY + spriteSize)
            drawSprite(drawRect, skin.tails[i], i, ArrowSpriteRenderer.NoteType.LONG_TAIL)
            currentX += columnWidth
        }

        currentX += spacing
        for (i in 0 until steps) {
            drawRect.set(currentX, startY, currentX + spriteSize, startY + spriteSize)
            drawSprite(drawRect, skin.explotions[i], i, ArrowSpriteRenderer.NoteType.EXPLOSION)
            currentX += columnWidth
        }

        currentX += spacing
        for (i in 0 until steps) {
            drawRect.set(currentX, startY, currentX + spriteSize, startY + spriteSize)
            drawSprite(
                drawRect,
                skin.explotionTails[i],
                i,
                ArrowSpriteRenderer.NoteType.EXPLOSION_TAIL
            )
            currentX += columnWidth
        }

        currentX += spacing
        for (i in 0 until steps) {
            drawRect.set(currentX, startY, currentX + spriteSize, startY + spriteSize)
            drawSprite(drawRect, skin.tapsEffect[i], i, ArrowSpriteRenderer.NoteType.TAP_EFFECT)
            currentX += columnWidth
        }

        currentX += spacing
        drawRect.set(currentX, startY, currentX + spriteSize, startY + spriteSize)
        drawSprite(drawRect, skin.mine, 0, ArrowSpriteRenderer.NoteType.MINE)
    }

    fun pruebaGrid() {
        val spriteSize = 25
        val spacing = 3
        val startX = 10
        val startY = 10
        val columnsPerRow = 12

        var currentX = startX
        var currentY = startY
        var itemCount = 0

        for (skinType in SkinType.values()) {
            val skin = noteSkins[skinType.ordinal]
            if (skin != null) {
                val allSprites = mutableListOf<Pair<Any, ArrowSpriteRenderer.NoteType>>()

                skin.arrows.forEach { allSprites.add(it to ArrowSpriteRenderer.NoteType.NORMAL) }
                skin.receptors.forEach { allSprites.add(it to ArrowSpriteRenderer.NoteType.RECEPTOR) }
                skin.longs.forEach { allSprites.add(it to ArrowSpriteRenderer.NoteType.LONG_BODY) }
                skin.tails.forEach { allSprites.add(it to ArrowSpriteRenderer.NoteType.LONG_TAIL) }
                skin.explotions.forEach { allSprites.add(it to ArrowSpriteRenderer.NoteType.EXPLOSION) }
                skin.explotionTails.forEach { allSprites.add(it to ArrowSpriteRenderer.NoteType.EXPLOSION_TAIL) }
                skin.tapsEffect.forEach { allSprites.add(it to ArrowSpriteRenderer.NoteType.TAP_EFFECT) }
                allSprites.add(skin.mine to ArrowSpriteRenderer.NoteType.MINE)

                for ((sprite, noteType) in allSprites) {
                    drawRect.set(currentX, currentY, currentX + spriteSize, currentY + spriteSize)
                    drawSprite(drawRect, sprite, 0, noteType)

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

    fun pruebaSpecificSkin(skinType: SkinType) {
        val skin = noteSkins[skinType.ordinal] ?: return
        val spriteSize = 40
        val spacing = 8
        val startX = 20
        val startY = 20

        var currentX = startX
        var currentY = startY

        // Draw arrows
        for (i in 0 until steps) {
            drawRect.set(currentX, currentY, currentX + spriteSize, currentY + spriteSize)
            drawSprite(drawRect, skin.arrows[i], i, ArrowSpriteRenderer.NoteType.NORMAL)
            currentX += spriteSize + spacing
        }

        // Move to next row
        currentY += spriteSize + spacing * 2
        currentX = startX

        // Draw receptors
        for (i in 0 until steps) {
            drawRect.set(currentX, currentY, currentX + spriteSize, currentY + spriteSize)
            drawSprite(drawRect, skin.receptors[i], i, ArrowSpriteRenderer.NoteType.RECEPTOR)
            currentX += spriteSize + spacing
        }

        // Move to next row
        currentY += spriteSize + spacing * 2
        currentX = startX

        // Draw effects
        for (i in 0 until steps) {
            drawRect.set(currentX, currentY, currentX + spriteSize, currentY + spriteSize)
            drawSprite(drawRect, skin.explotions[i], i, ArrowSpriteRenderer.NoteType.EXPLOSION)
            currentX += spriteSize + spacing
        }
    }

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
    }
}