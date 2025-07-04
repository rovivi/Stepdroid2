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
    private var noteSkins: EnumMap<SkinType, NoteSkin>? = null
    private var lastPositionDraw: IntArray = IntArray(10) { -999 }

    // Reusable objects to avoid garbage collection
    private var debugPaint: Paint? = null
    private var drawRect: Rect? = null

    /**
     * Created the step drawer
     */
    init {
        this.gameMode = GameMode.Companion.fromString(gameModeStr)
        initializeReusableObjects()
        calculateDimensions(aspectRatio, landScape, screenSize)
        initializeNoteSkins(context)
        initializeDrawingValues()
    }

    private fun initializeReusableObjects() {
        debugPaint = Paint()
        debugPaint!!.setColor(Color.WHITE)
        debugPaint!!.setStyle(Paint.Style.FILL)
        debugPaint!!.setTextSize(DEBUG_TEXT_SIZE)

        drawRect = Rect()
        noteSkins = EnumMap<SkinType, NoteSkin>(SkinType::class.java)
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
                noteSkins!!.put(SkinType.ROUTINE0, NoteSkin(context, gameMode.value, "routine1"))
                noteSkins!!.put(SkinType.ROUTINE1, NoteSkin(context, gameMode.value, "routine2"))
                noteSkins!!.put(SkinType.ROUTINE2, NoteSkin(context, gameMode.value, "routine3"))
                noteSkins!!.put(SkinType.ROUTINE3, NoteSkin(context, gameMode.value, "soccer"))
            }

            GameMode.PUMP_DOUBLE, GameMode.PUMP_HALFDOUBLE, GameMode.PUMP_SINGLE -> noteSkins!!.put(
                SkinType.SELECTED, NoteSkin(
                    context,
                    gameMode.value, "prime"
                )
            )

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

    fun draw(canvas: Canvas?, listRow: ArrayList<GameRow>) {
        resetLastPositionDraw()

        drawReceptors(canvas)
        drawNotes(canvas, listRow)
        drawEffects(canvas)
    }

    private fun drawReceptors(canvas: Canvas?) {
        val selectedSkin = noteSkins!!.get(SkinType.SELECTED)
        if (selectedSkin == null) return

        for (j in selectedSkin.receptors.indices) {
            val startNoteX = posInitialX + sizeNote * j
            setDrawRect(
                startNoteX,
                startValueY,
                startNoteX + scaledNoteSize,
                startValueY + scaledNoteSize
            )
            selectedSkin.receptors[j].draw(canvas, drawRect)
        }
    }

    private fun drawNotes(canvas: Canvas?, listRow: ArrayList<GameRow>) {
        for (gameRow in listRow) {
            val notes = gameRow.notes
            var count: Short = 0

            if (notes != null) {
                for (note in notes) {
                    if (note != null && note.type != CommonSteps.NOTE_EMPTY) {
                        drawSingleNote(canvas, note, gameRow, count.toInt())
                    }
                    count++
                }
            }
        }
    }

    private fun drawSingleNote(canvas: Canvas?, note: Note, gameRow: GameRow, columnIndex: Int) {
        val selectedSkin = noteSkins!!.get(SkinType.SELECTED)
        if (selectedSkin == null) return

        val startNoteX = posInitialX + sizeNote * columnIndex
        val endNoteX = startNoteX + scaledNoteSize
        var currentArrow: SpriteReader? = null

        when (note.type) {
            CommonSteps.NOTE_TAP, CommonSteps.NOTE_FAKE -> currentArrow =
                selectedSkin.arrows[columnIndex]

            CommonSteps.NOTE_LONG_START -> drawLongNote(
                canvas,
                note,
                gameRow,
                startNoteX,
                endNoteX,
                columnIndex,
                selectedSkin
            )

            CommonSteps.NOTE_LONG_BODY -> drawLongNoteBody(
                canvas,
                note,
                gameRow,
                startNoteX,
                endNoteX,
                columnIndex,
                selectedSkin
            )

            CommonSteps.NOTE_MINE -> currentArrow = selectedSkin.mine
        }

        if (currentArrow != null) {
            setDrawRect(startNoteX, gameRow.getPosY(), endNoteX, gameRow.getPosY() + scaledNoteSize)
            currentArrow.draw(canvas, drawRect)
        }
    }

    private fun drawLongNote(
        canvas: Canvas?,
        note: Note,
        gameRow: GameRow,
        startNoteX: Int,
        endNoteX: Int,
        columnIndex: Int,
        skin: NoteSkin
    ) {
        val endY = if (Objects.requireNonNull<GameRow?>(note.rowEnd)
                .getPosY() == NOT_DRAWABLE
        ) sizeY else note.rowEnd!!.getPosY()
        lastPositionDraw[columnIndex] = endY + scaledNoteSize

        // Draw long note body
        setDrawRect(
            startNoteX, gameRow.getPosY() + ((scaledNoteSize * LONG_NOTE_BODY_OFFSET).toInt()),
            endNoteX, endY + scaledNoteSize / LONG_NOTE_TAIL_OFFSET_DIVISOR
        )
        skin.longs[columnIndex].draw(canvas, drawRect)

        // Draw start arrow
        setDrawRect(startNoteX, gameRow.getPosY(), endNoteX, gameRow.getPosY() + scaledNoteSize)
        skin.arrows[columnIndex].draw(canvas, drawRect)

        // Draw tail if end exists
        if (Objects.requireNonNull<GameRow?>(note.rowEnd).getPosY() != NOT_DRAWABLE) {
            setDrawRect(startNoteX, endY, endNoteX, endY + scaledNoteSize)
            skin.tails[columnIndex].draw(canvas, drawRect)
        }
    }

    private fun drawLongNoteBody(
        canvas: Canvas?,
        note: Note,
        gameRow: GameRow,
        startNoteX: Int,
        endNoteX: Int,
        columnIndex: Int,
        skin: NoteSkin
    ) {
        if (gameRow.getPosY() > lastPositionDraw[columnIndex]) {
            var startY = gameRow.getPosY()
            if (gameRow.getPosY() > startValueY && gameRow.getPosY() < sizeY) {
                startY = startValueY
            }

            val endY = if (Objects.requireNonNull<GameRow?>(note.rowEnd)
                    .getPosY() == NOT_DRAWABLE
            ) sizeY else note.rowEnd!!.getPosY()
            lastPositionDraw[columnIndex] = endY

            // Draw long note body
            setDrawRect(
                startNoteX, startY + ((scaledNoteSize * LONG_NOTE_BODY_OFFSET).toInt()),
                endNoteX, endY + scaledNoteSize / LONG_NOTE_TAIL_OFFSET_DIVISOR
            )
            skin.longs[columnIndex].draw(canvas, drawRect)

            // Draw arrow
            setDrawRect(startNoteX, startY, endNoteX, startY + scaledNoteSize)
            skin.arrows[columnIndex].draw(canvas, drawRect)

            // Draw tail if end exists
            if (Objects.requireNonNull<GameRow?>(note.rowEnd).getPosY() != NOT_DRAWABLE) {
                setDrawRect(startNoteX, endY, endNoteX, endY + scaledNoteSize)
                skin.tails[columnIndex].draw(canvas, drawRect)
            }
        }
    }

    private fun drawEffects(canvas: Canvas?) {
        val selectedSkin = noteSkins!!.get(SkinType.SELECTED)
        if (selectedSkin == null) return

        for (j in selectedSkin.arrows.indices) {
            val startNoteX = posInitialX + sizeNote * j
            val endNoteX = startNoteX + scaledNoteSize

            setDrawRect(startNoteX, startValueY, endNoteX, startValueY + scaledNoteSize)
            selectedSkin.explotions[j].staticDraw(canvas, drawRect)
            selectedSkin.explotionTails[j].draw(canvas, drawRect)
            selectedSkin.tapsEffect[j].staticDraw(canvas, drawRect)
        }
    }

    private fun setDrawRect(left: Int, top: Int, right: Int, bottom: Int) {
        drawRect!!.set(left, top, right, bottom)
    }

    fun update() {
        for (currentNote in noteSkins!!.values) {
            updateNoteSkin(currentNote)
        }
    }

    private fun updateNoteSkin(noteSkin: NoteSkin) {
        for (x in noteSkin.arrows.indices) {
            noteSkin.arrows[x].update()
            noteSkin.tails[x].update()
            noteSkin.longs[x].update()
            noteSkin.explotions[x].update()
            noteSkin.explotionTails[x].update()
            noteSkin.tapsEffect[x].update()
            noteSkin.receptors[x].update()
        }
        noteSkin.mine.update()
    }

    val stepsByGameMode: Int
        get() = gameMode.steps

    fun getNoteSkin(skinType: SkinType): NoteSkin? {
        return noteSkins!!.get(skinType)
    }

    val selectedSkin: NoteSkin?
        get() = noteSkins!![SkinType.SELECTED]

    fun getSizeX(): Int {
        return sizeX
    }

    fun getSizeY(): Int {
        return sizeY
    }

    companion object {
        // Constants
        private val NOT_USED = -999
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
