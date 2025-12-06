package com.kyagamy.step.game.libgdx.render

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.graphics.g2d.SpriteBatch
import com.kyagamy.step.common.step.CommonSteps
import com.kyagamy.step.common.step.Game.GameRow
import com.kyagamy.step.game.interfaces.IStepsDrawer
import game.Note
import java.util.ArrayList

class LibGDXStepsDrawer(
    private val skinName: String,
    val type: String
) : IStepsDrawer {
    private val noteSkin: LibGDXNoteSkin
    var sizeNote = 0f
    var receptorY = 0f
    var startX = 0f
    
    // Game area dimensions with 4:3 aspect ratio (like GamePlayNew)
    var gameAreaWidth = 0f
    var gameAreaHeight = 0f
    var gameAreaOffsetX = 0f
    var gameAreaOffsetY = 0f

    // Up Scroll: Receptors at top
    private val RECEPTOR_OFFSET = 150f

    // Magic numbers from GamePlayNew - aspect ratio constants
    private val ASPECT_RATIO_4_3 = 0.75f  // Portrait mode: height = width * 0.75
    private val ASPECT_RATIO_16_9_CALC = 1.77777778f  // Landscape mode: width = height * 1.777...

    init {
        noteSkin = LibGDXNoteSkin(skinName, type)
        calculateDimensions()
    }

    fun calculateDimensions() {
        val screenWidth = Gdx.graphics.width.toFloat()
        val screenHeight = Gdx.graphics.height.toFloat()
        val isLandscape = screenWidth > screenHeight

        if (isLandscape) {
            // Landscape: Use 16:9 aspect ratio (similar to StepsDrawer landscape mode)
            // sizeX = (screenSize.y * ASPECT_RATIO_16_9_CALC).toInt()
            gameAreaHeight = screenHeight
            gameAreaWidth = screenHeight * ASPECT_RATIO_16_9_CALC

            // Center horizontally
            gameAreaOffsetX = (screenWidth - gameAreaWidth) / 2f
            gameAreaOffsetY = 0f

            // If game area exceeds screen width, recalculate
            if (gameAreaWidth > screenWidth) {
                gameAreaWidth = screenWidth
                gameAreaHeight = screenWidth / ASPECT_RATIO_16_9_CALC
                gameAreaOffsetX = 0f
                gameAreaOffsetY = (screenHeight - gameAreaHeight) / 2f
            }
        } else {
            // Portrait: Apply 4:3 aspect ratio like GamePlayNew does
            // playerSizeY = (size.x * GameConstants.ASPECT_RATIO_4_3).toInt()
            gameAreaWidth = screenWidth
            gameAreaHeight = screenWidth * ASPECT_RATIO_4_3

            // Center the game area vertically
            gameAreaOffsetX = 0f
            gameAreaOffsetY = (screenHeight - gameAreaHeight) / 2f
        }

        // Calculate note size (width / 5 for single, etc)
        // Similar to StepsDrawer logic but simplified
        val numCols = if (type.contains("double") || type.contains("routine")) 10 else 5
        sizeNote = gameAreaWidth / (numCols + 2) // Add some padding

        startX = gameAreaOffsetX + (gameAreaWidth - (sizeNote * numCols)) / 2f

        // Up Scroll: Receptor at Top (within game area)
        receptorY = gameAreaOffsetY + gameAreaHeight - RECEPTOR_OFFSET
    }

    fun update(delta: Float) {
        // Update animations with BPM-based speed
        noteSkin.arrows.forEach { it?.update(delta) }
        noteSkin.receptors.forEach { it?.update(delta) }
        noteSkin.longs.forEach { it?.update(delta) }
        noteSkin.tails.forEach { it?.update(delta) }
        noteSkin.explosions.forEach { it?.update(delta) }
    }

    /**
     * Updates animation speed based on current BPM.
     * 80 BPM = 1.0x speed (base)
     */
    fun updateAnimationSpeed(currentBPM: Float) {
        noteSkin.setAnimationSpeed(currentBPM)
        val speed = noteSkin.getAnimationSpeed()

        // Apply to all sprites
        noteSkin.arrows.forEach { it?.speedMultiplier = speed }
        noteSkin.receptors.forEach { it?.speedMultiplier = speed }
        noteSkin.longs.forEach { it?.speedMultiplier = speed }
        noteSkin.tails.forEach { it?.speedMultiplier = speed }
        noteSkin.explosions.forEach { it?.speedMultiplier = speed }
        noteSkin.explosionTails.forEach { it?.speedMultiplier = speed }
        noteSkin.tapsEffect.forEach { it?.speedMultiplier = speed }
        noteSkin.mine?.speedMultiplier = speed
    }

    fun draw(batch: SpriteBatch, drawList: ArrayList<GameRow>) {
        drawReceptors(batch)
        drawNotes(batch, drawList)
    }

    private fun drawReceptors(batch: SpriteBatch) {
        val numCols = if (type.contains("double") || type.contains("routine")) 10 else 5
        
        for (i in 0 until numCols) {
            val x = startX + (i * sizeNote)
            val receptor = noteSkin.receptors[i]
            receptor?.draw(batch, x, receptorY, sizeNote, sizeNote)
        }
    }

    private fun drawNotes(batch: SpriteBatch, drawList: ArrayList<GameRow>) {
        for (row in drawList) {
            val y = row.getPosY().toFloat()
            val notes = row.notes ?: continue
            
            for ((index, note) in notes.withIndex()) {
                if (index >= noteSkin.arrows.size) continue
                
                val x = startX + (index * sizeNote)
                
                when (note.type) {
                    CommonSteps.NOTE_TAP, CommonSteps.NOTE_FAKE -> {
                        noteSkin.arrows[index]?.draw(batch, x, y, sizeNote, sizeNote)
                    }
                    CommonSteps.NOTE_LONG_START -> {
                        drawLongNote(batch, note, row, x, index)
                    }
                    CommonSteps.NOTE_LONG_BODY -> {
                         drawLongNoteBody(batch, note, row, x, index)
                    }
                    CommonSteps.NOTE_MINE -> {
                        noteSkin.mine?.draw(batch, x, y, sizeNote, sizeNote)
                    }
                }
            }
        }
    }
    
    private fun drawLongNote(batch: SpriteBatch, note: Note, row: GameRow, x: Float, index: Int) {
        val endRow = note.rowEnd
        if (endRow != null) {
            val startY = row.getPosY().toFloat()
            val endY = endRow.getPosY().toFloat()
            
            val height = startY - endY
            if (height > 0) {
                 noteSkin.longs[index]?.draw(batch, x, endY, sizeNote, height)
            }
            
            noteSkin.tails[index]?.draw(batch, x, endY, sizeNote, sizeNote)
            noteSkin.arrows[index]?.draw(batch, x, startY, sizeNote, sizeNote)
        }
    }

    private fun drawLongNoteBody(batch: SpriteBatch, note: Note, row: GameRow, x: Float, index: Int) {
        // Simplified body drawing if head is missing
    }

    fun dispose() {
        noteSkin.dispose()
    }

    override fun playExplosion(index: Int) {
        // LibGDX implementation for explosions
        // noteSkin.explosions[index]?.play() 
    }

    override fun playExplosionTail(index: Int) {
        // LibGDX implementation for tails
    }

    override fun stopExplosionTail(index: Int) {
        // LibGDX implementation for tails
    }
}
