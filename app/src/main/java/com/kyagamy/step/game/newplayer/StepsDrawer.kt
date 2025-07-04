package com.kyagamy.step.game.newplayer

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Point
import android.graphics.Rect
import com.kyagamy.step.common.step.CommonGame.CustomSprite.SpriteReader
import com.kyagamy.step.common.step.CommonSteps
import com.kyagamy.step.common.step.Game.GameRow
import com.kyagamy.step.common.step.Game.NOT_DRAWABLE
import game.Note
import java.util.*
import kotlin.math.abs

class StepsDrawer internal constructor(
    context: Context?,
    gameModeStr: String?,
    aspectRatio: String,
    landScape: Boolean,
    screenSize: Point
) {
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
                return GameMode.EMPTY
            }
        }
    }

    enum class SkinType {
        SELECTED, ROUTINE0, ROUTINE1, ROUTINE2, ROUTINE3
    }

    // Fields
    @JvmField
    var sizeX: Int = 0
    @JvmField
    var sizeY: Int = 0
    var sizeNote: Int = 0
    var scaledNoteSize: Int = 0
    @JvmField
    var offsetX: Int = 0
    var offsetY: Int = 0
    var posInitialX: Int = 0
    var startValueY: Int = 0

    private val gameMode: GameMode
    private val steps: Int
    private val noteSkins = arrayOfNulls<NoteSkin>(SkinType.values().size)
    private val lastPositionDraw: IntArray = IntArray(10) { NOT_USED }

    // Non-nullable reusable objects
    private val drawRect: Rect = Rect()
    private val debugPaint: Paint = Paint().apply {
        color = Color.WHITE
        style = Paint.Style.FILL
        textSize = DEBUG_TEXT_SIZE
    }

    /**
     * Created the step drawer
     */
    init {
        this.gameMode = GameMode.fromString(gameModeStr)
        this.steps = gameMode.steps
        calculateDimensions(aspectRatio, landScape, screenSize)
        initializeNoteSkins(context)
        initializeDrawingValues()
    }

    private fun calculateDimensions(aspectRatio: String, landScape: Boolean, screenSize: Point) {
        posInitialX = (screenSize.x * SCREEN_WIDTH_FACTOR).toInt()

        val relationAspectValue: Float =
            if (aspectRatio.contains("4:3")) ASPECT_RATIO_4_3 else ASPECT_RATIO_16_9

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

    fun draw(canvas: Canvas, listRow: ArrayList<GameRow>) {
        resetLastPositionDraw()
        drawReceptorsAndEffects(canvas)
        drawNotes(canvas, listRow)
    }

    private fun drawReceptorsAndEffects(canvas: Canvas) {
        val selectedSkin = noteSkins[SkinType.SELECTED.ordinal] ?: return
        val receptors = selectedSkin.receptors
        val explosions = selectedSkin.explotions
        val explosionTails = selectedSkin.explotionTails
        val tapsEffect = selectedSkin.tapsEffect

        for (j in 0 until steps) {
            val startNoteX = posInitialX + sizeNote * j
            val endNoteX = startNoteX + scaledNoteSize

            // Draw receptors
            drawRect.set(startNoteX, startValueY, endNoteX, startValueY + scaledNoteSize)
            receptors[j].draw(canvas, drawRect)

            // Draw effects
            explosions[j].staticDraw(canvas, drawRect)
            explosionTails[j].draw(canvas, drawRect)
            tapsEffect[j].staticDraw(canvas, drawRect)
        }
    }

    private fun drawNotes(canvas: Canvas, listRow: ArrayList<GameRow>) {
        for (gameRow in listRow) {
            val notes = gameRow.notes
            if (notes != null) {
                for (count in notes.indices) {
                    val note = notes[count]
                    if (note.type != CommonSteps.NOTE_EMPTY) {
                        drawSingleNote(canvas, note, gameRow, count)
                    }
                }
            }
        }
    }

    private  fun drawSingleNote(
        canvas: Canvas,
        note: Note,
        gameRow: GameRow,
        columnIndex: Int
    ) {
        val selectedSkin = noteSkins[SkinType.SELECTED.ordinal] ?: return
        val startNoteX = posInitialX + sizeNote * columnIndex
        val endNoteX = startNoteX + scaledNoteSize
        val arrows = selectedSkin.arrows
        var currentArrow: SpriteReader? = null

        when (note.type) {
            CommonSteps.NOTE_TAP, CommonSteps.NOTE_FAKE ->
                currentArrow = arrows[columnIndex]

            CommonSteps.NOTE_LONG_START -> drawLongNote(
                canvas, note, gameRow, startNoteX, endNoteX, columnIndex, selectedSkin
            )

            CommonSteps.NOTE_LONG_BODY -> drawLongNoteBody(
                canvas, note, gameRow, startNoteX, endNoteX, columnIndex, selectedSkin
            )

            CommonSteps.NOTE_MINE -> currentArrow = selectedSkin.mine
        }

        if (currentArrow != null) {
            drawRect.set(
                startNoteX,
                gameRow.getPosY(),
                endNoteX,
                gameRow.getPosY() + scaledNoteSize
            )
            currentArrow.draw(canvas, drawRect)
        }
    }

    private fun drawLongNote(
        canvas: Canvas, note: Note, gameRow: GameRow, startNoteX: Int, endNoteX: Int,
        columnIndex: Int, skin: NoteSkin
    ) {
        // Pre-calculate all values outside canvas operations
        val startY = gameRow.getPosY()
        val rowEnd = note.rowEnd
        val endYRaw = rowEnd?.getPosY() ?: NOT_DRAWABLE
        val endY = if (endYRaw == NOT_DRAWABLE) sizeY else endYRaw
        lastPositionDraw[columnIndex] = endY + scaledNoteSize

        // Pre-calculate offsets and positions
        val bodyOffsetPx = (scaledNoteSize * LONG_NOTE_BODY_OFFSET).toInt()
        val tailDiv = scaledNoteSize / LONG_NOTE_TAIL_OFFSET_DIVISOR
        val bodyTop = startY + bodyOffsetPx
        val bodyBottom = endY + tailDiv
        val headBottom = startY + scaledNoteSize
        val tailBottom = endY + scaledNoteSize

        // Cache sprite references
        val arrows = skin.arrows[columnIndex]
        val longs = skin.longs[columnIndex]
        val tails = skin.tails[columnIndex]

        // Draw order: body → tail → head (head always on top)

        // 1) Draw long note body
        drawRect.set(startNoteX, bodyTop, endNoteX, bodyBottom)
        longs.draw(canvas, drawRect)

        // 2) Draw tail (only if end exists)
        if (endYRaw != NOT_DRAWABLE) {
            drawRect.set(startNoteX, endY, endNoteX, tailBottom)
            tails.draw(canvas, drawRect)
        }

        // 3) Draw head arrow (always on top)
        drawRect.set(startNoteX, startY, endNoteX, headBottom)
        arrows.draw(canvas, drawRect)
    }

    private fun drawLongNoteBody(
        canvas: Canvas, note: Note, gameRow: GameRow, startNoteX: Int, endNoteX: Int,
        columnIndex: Int, skin: NoteSkin
    ) {
        // Pre-calculate all values
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

        // Pre-calculate offsets and positions
        val bodyOffsetPx = (scaledNoteSize * LONG_NOTE_BODY_OFFSET).toInt()
        val tailDiv = scaledNoteSize / LONG_NOTE_TAIL_OFFSET_DIVISOR
        val bodyTop = startY + bodyOffsetPx
        val bodyBottom = endY + tailDiv
        val headBottom = startY + scaledNoteSize
        val tailBottom = endY + scaledNoteSize

        // Cache sprite references
        val arrows = skin.arrows[columnIndex]
        val longs = skin.longs[columnIndex]
        val tails = skin.tails[columnIndex]

        // Draw order: body → tail → head (head always on top)

        // 1) Draw long note body
        drawRect.set(startNoteX, bodyTop, endNoteX, bodyBottom)
        longs.draw(canvas, drawRect)

        // 2) Draw tail (only if end exists)
        if (endYRaw != NOT_DRAWABLE) {
            drawRect.set(startNoteX, endY, endNoteX, tailBottom)
            tails.draw(canvas, drawRect)
        }

        // 3) Draw head arrow (always on top)
        drawRect.set(startNoteX, startY, endNoteX, headBottom)
        arrows.draw(canvas, drawRect)
    }

    fun update() {
        // Optimized update with flat loops
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

    fun getSizeX(): Int {
        return sizeX
    }

    fun getSizeY(): Int {
        return sizeY
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
        private const val DEBUG_TEXT_SIZE = 20f
    }
}
