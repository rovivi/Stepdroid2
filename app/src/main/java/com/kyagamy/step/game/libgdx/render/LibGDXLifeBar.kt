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

    var life = 50f
    private var aumento = 0f
    private var aumentLife = 0f
    private var auxLife = 1f
    private var timeMark = System.nanoTime()

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

    // Dimensions - calculadas igual que LifeBar.kt
    private var sizeX = 0f
    private var sizeY = 0f
    private var startX = 0f
    private var startY = 0f

    init {
        loadTextures()
        calculateDimensions()
    }

    private fun loadTextures() {
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
            null
        }
    }

    fun calculateDimensions() {
        // Basado en LifeBar.kt
        // sizeX = stepsDrawer.sizeNote * stepsDrawer.stepsByGameMode
        // sizeY = ((stepsDrawer.sizeNote / 3) * 1.9f).toInt()
        // startX = stepsDrawer.posInitialX
        // startY = stepsDrawer.sizeNote / 8

        val numSteps =
            if (stepsDrawer.type.contains("double") || stepsDrawer.type.contains("routine")) 10 else 5

        sizeX = stepsDrawer.sizeNote * numSteps
        sizeY = (stepsDrawer.sizeNote / 3) * 1.9f
        startX = stepsDrawer.startX

        // startY en la parte superior del área de juego
        // En Up Scroll, queremos que esté arriba
        startY = stepsDrawer.gameAreaOffsetY + stepsDrawer.sizeNote / 8
    }

    override fun updateLife(typeTap: Short, combo: Int) {
        // Exactamente como LifeBar.kt
        when (typeTap.toInt()) {
            0, 1 -> life += 1 * abs(combo) // Perfect/Great
            3 -> life -= 0.3f * abs(combo) // Bad
            4 -> life -= 3 * abs(combo) // Miss
        }
        if (life > 100) life = 100f
        if (life < 0) life = 0f
    }

    fun update() {
        // Animación del glow - basado en LifeBar.kt
        if (System.nanoTime() - timeMark > 150) {
            if (aumentLife > 6 || aumentLife < 0) auxLife *= -1f
            aumentLife += auxLife
            timeMark = System.nanoTime()
        }
    }

    fun draw(batch: SpriteBatch) {
        // Recalcular dimensiones por si cambió
        calculateDimensions()

        aumento++
        val percent = life / 100f

        // Calcular posiciones - exactamente como LifeBar.kt
        val positionTip = startX + when {
            life < 6 -> (sizeX * 0.005f)
            life > 98 -> (sizeX * 0.94f)
            else -> (sizeX * (percent - 0.05f))
        }

        val positionBar = startX + if (life >= 98) sizeX else (sizeX * (percent - 0.1f))
        val posBarBlue = sizeX * (percent - 0.06f + aumentLife / 100f)

        // Draw danger glow if life is low
        if (life < DANGER_VALUE) {
            glueRed?.let {
                batch.draw(it, startX, startY, sizeX, sizeY)
            }
        }

        // Draw BG
        if (life < 100) {
            val bgTex = if (life <= DANGER_VALUE) bgDanger else bg
            bgTex?.let {
                batch.draw(it, startX, startY, sizeX, sizeY)
            }
        }

        // Draw blue glow
        glowBlue?.let {
            val glowWidth = startX + posBarBlue
            // Draw with UV mapping to cut the texture
            batch.draw(it, startX, startY, posBarBlue, sizeY, 0f, 0f, posBarBlue / sizeX, 1f)
        }

        // Draw life meter (cut bitmap)
        lifeMeter?.let {
            // Draw only the portion corresponding to current life
            val barWidth = positionBar - startX
            batch.draw(it, startX, startY, barWidth, sizeY, 0f, 0f, percent, 1f)
        }

        // Draw amazing light when life > 95
        if (life > AMAZING_VALUE) {
            lightFull?.let {
                val lightAlpha = (0 + aumentLife * 20).toInt() / 255f
                batch.setColor(1f, 1f, 1f, lightAlpha)
                batch.draw(it, startX, startY, sizeX, sizeY)
                batch.setColor(1f, 1f, 1f, 1f)
            }
        }

        // Draw Skin overlay
        skin?.let {
            batch.draw(it, startX, startY, sizeX, sizeY)
        }

        // Draw Tip
        val tipTex = if (life > DANGER_VALUE) tipBlue else tipRed
        tipTex?.let {
            val tipWidth = sizeX * 0.08f
            batch.draw(it, positionTip, startY, tipWidth, sizeY)
        }
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

    companion object {
        const val DANGER_VALUE = 15
        const val AMAZING_VALUE = 95
    }
}
