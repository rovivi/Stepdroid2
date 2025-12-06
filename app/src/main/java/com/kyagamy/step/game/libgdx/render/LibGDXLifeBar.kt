package com.kyagamy.step.game.libgdx.render

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.graphics.Texture
import com.badlogic.gdx.graphics.g2d.SpriteBatch
import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.kyagamy.step.game.interfaces.ILifeBar
import kotlin.math.abs

class LibGDXLifeBar(
    private val stepsDrawer: LibGDXStepsDrawer
) : ILifeBar {

    private var life = 50f
    private val DANGER_VALUE = 15
    private val AMAZING_VALUE = 95
    
    // Textures
    private var bg: Texture? = null
    private var bgDanger: Texture? = null
    private var tipBlue: Texture? = null
    private var tipRed: Texture? = null
    private var glowBlue: Texture? = null
    private var glueRed: Texture? = null
    private var skin: Texture? = null
    private var lifeMeter: Texture? = null
    private var lightFull: Texture? = null

    // Dimensions
    private var sizeX = 0f
    private var sizeY = 0f
    private var startX = 0f
    private var startY = 0f

    init {
        loadTextures()
        calculateDimensions()
    }

    private fun loadTextures() {
        // Assuming textures are in assets/ or we load them from Android resources?
        // Since we are in LibGDX, we should use assets. 
        // If they are only in res/drawable, we can't easily access them without Android context or copying.
        // For migration, let's assume we have copied them or we use placeholders.
        // Or we can try to load from internal path if they exist.
        // Let's assume standard names in assets/ui/lifebar/
        
        // For this task, I'll use placeholders or try to load if paths were known.
        // Since I don't have the assets in the file list, I will create a helper to load or use error texture.
        // Ideally, user should provide assets. I will use simple colored rectangles if textures missing?
        // No, I should try to load.
        
        // Let's try to load from "ui/lifebar/"
        bg = loadTexture("ui/lifebar/lifebar_bg.png")
        bgDanger = loadTexture("ui/lifebar/lifebar_bg_danger.png")
        tipBlue = loadTexture("ui/lifebar/lifebar_blue_tip.png")
        tipRed = loadTexture("ui/lifebar/lifebar_red_tip.png")
        glowBlue = loadTexture("ui/lifebar/lifebar_back_tip.png")
        lifeMeter = loadTexture("ui/lifebar/lifebar_life.png")
        skin = loadTexture("ui/lifebar/lifebar_skin.png")
        glueRed = loadTexture("ui/lifebar/lifebar_light_danger.png")
        lightFull = loadTexture("ui/lifebar/lifebar_light_full.png")
    }
    
    private fun loadTexture(path: String): Texture? {
        return if (Gdx.files.internal(path).exists()) {
            Texture(Gdx.files.internal(path))
        } else {
            // Gdx.app.error("LibGDXLifeBar", "Texture missing: $path")
            null
        }
    }

    private fun calculateDimensions() {
        // Based on StepsDrawer
        // sizeX = stepsDrawer.sizeNote * stepsDrawer.stepsByGameMode (5 or 10)
        // sizeY = ((stepsDrawer.sizeNote / 3) * 1.9f).toInt()
        // startX = stepsDrawer.posInitialX
        // startY = stepsDrawer.sizeNote / 8
        
        // LibGDXStepsDrawer has sizeNote and startX.
        // We need number of steps.
        val numSteps = if (stepsDrawer.type.contains("double") || stepsDrawer.type.contains("routine")) 10 else 5
        
        sizeX = stepsDrawer.sizeNote * numSteps
        sizeY = (stepsDrawer.sizeNote / 3) * 1.9f
        startX = stepsDrawer.startX
        // In Up Scroll, LifeBar usually at Top or Bottom? 
        // Original code: startY = sizeNote / 8. (Top)
        // Up Scroll: Receptors at Top. LifeBar might overlap?
        // Let's put it at Top for now.
        startY = stepsDrawer.sizeNote / 8
    }

    override fun updateLife(typeTap: Short, combo: Int) {
        // Logic from LifeBar.kt
        // Combo.VALUE_PERFECT = 0, GREAT = 1, GOOD = 2, BAD = 3, MISS = 4
        when (typeTap.toInt()) {
             0, 1 -> life += 1 * abs(combo) // Perfect/Great
             3 -> life -= 0.3f * abs(combo) // Bad
             4 -> life -= 3 * abs(combo) // Miss
        }
        if (life > 100) life = 100f
        if (life < 0) life = 0f
    }

    fun draw(batch: SpriteBatch) {
        val percent = life / 100f
        
        // Draw BG
        if (life < 100) {
            val bgTex = if (life <= DANGER_VALUE) bgDanger else bg
            bgTex?.let { batch.draw(it, startX, startY, sizeX, sizeY) }
        }
        
        // Draw Meter
        // Cut bitmap logic: draw only a portion width
        val barWidth = sizeX * percent
        lifeMeter?.let { 
            // Draw partial texture
            // batch.draw(texture, x, y, width, height, u, v, u2, v2)
            // u2 = percent
            batch.draw(it, startX, startY, barWidth, sizeY, 0f, 0f, percent, 1f)
        }
        
        // Draw Skin
        skin?.let { batch.draw(it, startX, startY, sizeX, sizeY) }
        
        // Draw Tip
        // positionTip calculation
        // val positionTip = startX + ...
        // Simplified:
        val tipX = startX + (sizeX * percent) - (sizeX * 0.05f)
        val tipTex = if (life > DANGER_VALUE) tipBlue else tipRed
        tipTex?.let { batch.draw(it, tipX, startY, sizeX * 0.08f, sizeY) }
    }
    
    fun dispose() {
        bg?.dispose()
        bgDanger?.dispose()
        tipBlue?.dispose()
        tipRed?.dispose()
        glowBlue?.dispose()
        lifeMeter?.dispose()
        skin?.dispose()
        glueRed?.dispose()
        lightFull?.dispose()
    }
}
