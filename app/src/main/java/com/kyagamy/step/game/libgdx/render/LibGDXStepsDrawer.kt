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
    
    // Up Scroll: Receptors at top
    private val RECEPTOR_OFFSET = 150f 

    init {
        noteSkin = LibGDXNoteSkin(skinName, type)
        calculateDimensions()
    }

    fun calculateDimensions() {
        val width = Gdx.graphics.width.toFloat()
        val height = Gdx.graphics.height.toFloat()
        
        // Calculate note size (width / 5 for single, etc)
        // Similar to StepsDrawer logic but simplified
        val numCols = if (type.contains("double") || type.contains("routine")) 10 else 5
        sizeNote = width / (numCols + 2) // Add some padding
        
        startX = (width - (sizeNote * numCols)) / 2f
        
        // Up Scroll: Receptor at Top
        receptorY = height - RECEPTOR_OFFSET
    }

    fun update(delta: Float) {
        // Update animations
        noteSkin.arrows.forEach { it?.update(delta) }
        noteSkin.receptors.forEach { it?.update(delta) }
        noteSkin.longs.forEach { it?.update(delta) }
        noteSkin.tails.forEach { it?.update(delta) }
        noteSkin.explosions.forEach { it?.update(delta) }
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
