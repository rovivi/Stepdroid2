package com.kyagamy.step.game.libgdx.render

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.graphics.Texture
import com.badlogic.gdx.graphics.g2d.SpriteBatch
import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.kyagamy.step.game.interfaces.ICombo
import com.kyagamy.step.game.interfaces.ILifeBar
import kotlin.math.abs

class LibGDXCombo(
    private val stepsDrawer: LibGDXStepsDrawer
) : ICombo {

    private var lifeBar: ILifeBar? = null
    private var combo = 0
    private var currentJudgment: Short = -1
    
    // Textures
    private var judgeTexture: Texture? = null
    private var judgeRegions: Array<TextureRegion>? = null
    
    private var comboTexture: Texture? = null
    private var comboNumberTexture: Texture? = null
    private var comboNumberRegions: Array<TextureRegion>? = null
    
    // Animation
    private var scale = 1.0f
    private var alpha = 1.0f
    private var timer = 0f

    init {
        loadTextures()
    }

    private fun loadTextures() {
        // Load Judgment Sprite Sheet (1x5 usually)
        val judgeTex = loadTexture("ui/combo/play_combo_judge.png")
        if (judgeTex != null) {
            judgeTexture = judgeTex
            val tmp = TextureRegion.split(judgeTex, judgeTex.width, judgeTex.height / 5)
            // Assuming 1 column, 5 rows
            judgeRegions = Array(5) { i -> tmp[i][0] }
        }
        
        comboTexture = loadTexture("ui/combo/play_combo.png")
        
        val numTex = loadTexture("ui/combo/play_combo_number.png")
        if (numTex != null) {
            comboNumberTexture = numTex
            val tmp = TextureRegion.split(numTex, numTex.width / 10, numTex.height)
            // Assuming 10 columns, 1 row
            comboNumberRegions = Array(10) { i -> tmp[0][i] }
        }
    }
    
    private fun loadTexture(path: String): Texture? {
        return if (Gdx.files.internal(path).exists()) {
            Texture(Gdx.files.internal(path))
        } else {
            null
        }
    }

    override fun setComboUpdate(typeTap: Short) {
        currentJudgment = typeTap
        
        // Update Combo Counter
        when (typeTap.toInt()) {
            0, 1 -> combo = if (combo < 0) 1 else combo + 1 // Perfect/Great
            2 -> if (combo < -4) combo = 0 // Good
            3 -> if (combo != 0) combo = 0 // Bad
            4 -> combo = if (combo > 0) 0 else combo - 1 // Miss
        }
        
        // Update LifeBar
        lifeBar?.updateLife(typeTap, 1)
        
        // Reset Animation
        scale = 1.5f
        alpha = 1.0f
        timer = 0f
    }

    override fun setLifeBar(lifeBar: ILifeBar) {
        this.lifeBar = lifeBar
    }
    
    fun update(delta: Float) {
        if (scale > 1.0f) {
            scale -= delta * 2f
            if (scale < 1.0f) scale = 1.0f
        }
        
        timer += delta
        if (timer > 1.0f) {
            alpha -= delta
            if (alpha < 0f) alpha = 0f
        }
    }

    fun draw(batch: SpriteBatch) {
        if (alpha <= 0f) return
        
        val centerX = Gdx.graphics.width / 2f
        val centerY = Gdx.graphics.height / 2f
        
        // Draw Judgment
        if (currentJudgment >= 0 && judgeRegions != null && currentJudgment < judgeRegions!!.size) {
            val region = judgeRegions!![currentJudgment.toInt()]
            val width = region.regionWidth.toFloat() * scale
            val height = region.regionHeight.toFloat() * scale
            
            batch.setColor(1f, 1f, 1f, alpha)
            batch.draw(region, centerX - width / 2, centerY, width, height)
        }
        
        // Draw Combo
        if (abs(combo) > 3) {
            // Draw "Combo" label
            comboTexture?.let {
                batch.draw(it, centerX - it.width / 2, centerY - 50, it.width.toFloat(), it.height.toFloat())
            }
            
            // Draw Number
            if (comboNumberRegions != null) {
                val str = abs(combo).toString()
                var startX = centerX + 50 // Offset
                
                for (char in str) {
                    val digit = char.toString().toInt()
                    val region = comboNumberRegions!![digit]
                    batch.draw(region, startX, centerY - 50, region.regionWidth.toFloat(), region.regionHeight.toFloat())
                    startX += region.regionWidth
                }
            }
        }
        
        batch.setColor(1f, 1f, 1f, 1f)
    }
    
    fun dispose() {
        judgeTexture?.dispose()
        comboTexture?.dispose()
        comboNumberTexture?.dispose()
    }
}
