package com.kyagamy.step.game.libgdx.render

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.graphics.Texture
import com.badlogic.gdx.graphics.g2d.BitmapFont
import com.badlogic.gdx.graphics.g2d.SpriteBatch
import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.kyagamy.step.game.interfaces.ICombo
import com.kyagamy.step.game.interfaces.ILifeBar
import com.kyagamy.step.game.newplayer.Evaluator
import kotlin.math.abs

class LibGDXCombo(
    private val stepsDrawer: LibGDXStepsDrawer
) : ICombo {

    private var lifeBar: ILifeBar? = null
    private var combo = 0
    private var currentJudgment: Short = VALUE_NONE

    // Textures
    private var judgeRegions: Array<TextureRegion>? = null
    private var comboTexture: Texture? = null
    private var badComboTexture: Texture? = null
    private var comboNumberRegions: Array<TextureRegion>? = null
    private var currentBitmapCombo: Texture? = null

    // Fallback font for debug
    private val font = BitmapFont()

    // Animation - basado en Combo.kt
    private var aumentTip = -220
    private var paintAlpha = 255

    // Screen dimensions
    private var x = 0f
    private var y = 0f

    // Proportions from Combo.kt - estas son las proporciones mágicas!
    private val COMBO_TEXT_RATIO_X = 0.14815f * 1.25f
    private val COMBO_TEXT_RATIO_Y = 0.0363637f * 1.25f
    private val COMBO_NUMBER_RATIO_X = 0.05555556f * 1.15f
    private val COMBO_LABEL_RATIO_X = 0.306f
    private val COMBO_LABEL_RATIO_Y = 0.0555555556f
    private val RATIO_BIGGER_LABEL = 0.6666666667f

    init {
        font.data.setScale(2f)
        loadTextures()
        calculateDimensions()
    }

    fun calculateDimensions() {
        // Basado en Combo.kt: x = stepsDrawer.sizeX + stepsDrawer.offsetX, y = stepsDrawer.sizeY
        x = stepsDrawer.gameAreaWidth + stepsDrawer.gameAreaOffsetX
        y = stepsDrawer.gameAreaHeight
    }

    private fun loadTextures() {
        // Load Judgment Sprite Sheet (1x5: Perfect, Great, Good, Bad, Miss)
        val judgeTex = loadTexture("ui/combo/play_combo_judge.png")
        if (judgeTex != null) {
            val tmp = TextureRegion.split(judgeTex, judgeTex.width, judgeTex.height / 5)
            judgeRegions = Array(5) { i -> tmp[i][0] }
            Gdx.app.log("LibGDXCombo", "Loaded judge sprites: ${judgeRegions!!.size} frames")
        } else {
            Gdx.app.log("LibGDXCombo", "Judge texture not found - will use fallback")
        }

        comboTexture = loadTexture("ui/combo/play_combo.png")
        badComboTexture = loadTexture("ui/combo/play_combo_bad.png")

        val numTex = loadTexture("ui/combo/play_combo_number.png")
        if (numTex != null) {
            val tmp = TextureRegion.split(numTex, numTex.width / 10, numTex.height)
            comboNumberRegions = Array(10) { i -> tmp[0][i] }
            Gdx.app.log("LibGDXCombo", "Loaded number sprites: ${comboNumberRegions!!.size} digits")
        } else {
            Gdx.app.log("LibGDXCombo", "Number texture not found - will use fallback")
        }
    }

    private fun loadTexture(path: String): Texture? {
        return if (Gdx.files.internal(path).exists()) {
            val tex = Texture(Gdx.files.internal(path))
            Gdx.app.log("LibGDXCombo", "Loaded texture: $path (${tex.width}x${tex.height})")
            tex
        } else {
            Gdx.app.log("LibGDXCombo", "Texture not found: $path")
            null
        }
    }

    override fun setComboUpdate(typeTap: Short) {
        currentJudgment = typeTap

        // Update Evaluator stats
        when (typeTap.toInt()) {
            0 -> Evaluator.PERFECT++
            1 -> Evaluator.GREAT++
            2 -> Evaluator.GOOD++
            3 -> Evaluator.BAD++
            4 -> Evaluator.MISS++
        }

        // Update Combo Counter - exactamente como Combo.kt
        when (typeTap.toInt()) {
            0, 1 -> combo = if (combo < 0) 1 else combo + 1 // Perfect/Great
            2 -> if (combo < -4) combo = 0 // Good
            3 -> if (combo != 0) combo = 0 // Bad
            4 -> combo = if (combo > 0) 0 else combo - 1 // Miss
        }

        // Update Max Combo
        if (combo > Evaluator.MAX_COMBO) {
            Evaluator.MAX_COMBO = combo
        }

        // Update LifeBar
        lifeBar?.updateLife(typeTap, 1)

        // Show animation - resetear aumentTip
        show()
    }

    private fun show() {
        aumentTip = 20
        paintAlpha = 255
        currentBitmapCombo = if (combo >= 0) comboTexture else badComboTexture
    }

    override fun setLifeBar(lifeBar: ILifeBar) {
        this.lifeBar = lifeBar
    }

    fun update(delta: Float) {
        // Basado en Combo.kt - el aumentTip decrece
        aumentTip -= 1
    }

    fun draw(batch: SpriteBatch) {
        // Recalcular dimensiones por si cambió el área
        calculateDimensions()

        // setSizes - basado en las proporciones de Combo.kt
        val numberSizeY = (y * COMBO_NUMBER_RATIO_X).toInt()
        val numberSizeX = (y * COMBO_NUMBER_RATIO_X).toInt()

        var comboSizeY = (y * COMBO_TEXT_RATIO_Y).toInt()
        var comboSizeX = (y * COMBO_TEXT_RATIO_X).toInt()

        var labelSizeY = (y * COMBO_LABEL_RATIO_Y).toInt()
        var labelSizeX = (y * COMBO_LABEL_RATIO_X).toInt()

        // Animación de escala cuando aumentTip está entre 14 y 21
        if (aumentTip in 15..20) {
            val relation = 1 + (aumentTip - 15) * 0.22f * RATIO_BIGGER_LABEL
            labelSizeY = (labelSizeY * relation).toInt()
            labelSizeX = (labelSizeX * relation).toInt()
            comboSizeX = (comboSizeX * ((relation - 1) / 3 + 1)).toInt()
            comboSizeY = (comboSizeY * ((relation - 1) / 3 + 1)).toInt()
        }

        // Posiciones
        val posLabelIntX = ((x / 2f - labelSizeX / 2f) * 1.008).toInt()
        val posComboIntX = (x / 2f - comboSizeX / 2f).toInt()

        // Fade out cuando aumentTip < 6
        if (aumentTip < 6) {
            paintAlpha = abs(-(255 / 5 * aumentTip))
        }

        val alphaFloat = paintAlpha / 255f

        // Posición Y inicial
        var posIntYCombo = (y / 2 - (numberSizeY + labelSizeY + comboSizeY) / 2).toInt()

        // Solo dibujar si aumentTip > 0
        if (aumentTip > 0) {
            // Draw Judgment Label
            if (currentJudgment >= 0) {
                if (judgeRegions != null && currentJudgment < judgeRegions!!.size) {
                    val region = judgeRegions!![currentJudgment.toInt()]
                    batch.setColor(1f, 1f, 1f, alphaFloat)
                    batch.draw(
                        region,
                        posLabelIntX.toFloat(),
                        posIntYCombo.toFloat(),
                        labelSizeX.toFloat(),
                        labelSizeY.toFloat()
                    )
                    batch.setColor(1f, 1f, 1f, 1f)
                } else {
                    // Fallback: draw text
                    val judgmentText = when (currentJudgment.toInt()) {
                        0 -> "PERFECT"
                        1 -> "GREAT"
                        2 -> "GOOD"
                        3 -> "BAD"
                        4 -> "MISS"
                        else -> "???"
                    }
                    font.color.a = alphaFloat
                    val textX = x / 2f - 50
                    val textY = y / 2f + 50
                    font.draw(batch, judgmentText, textX, textY)
                    font.color.a = 1f
                }
            }

            posIntYCombo = (posIntYCombo + labelSizeY * 1.08).toInt()

            // Draw Combo si es > 3 o < -3
            if (combo > 3 || combo < -3) {
                // Show combo text/image
                currentBitmapCombo?.let {
                    batch.setColor(1f, 1f, 1f, alphaFloat)
                    batch.draw(
                        it,
                        posComboIntX.toFloat(),
                        posIntYCombo.toFloat(),
                        comboSizeX.toFloat(),
                        comboSizeY.toFloat()
                    )
                    batch.setColor(1f, 1f, 1f, 1f)
                }

                posIntYCombo += comboSizeY

                // Draw Combo Number
                if (comboNumberRegions != null) {
                    val stringComboAux = (100000000 + abs(combo)).toString()
                    val stringCombo = abs(combo).toString()

                    var drawTimes = 4 // Mostrar 4 dígitos por defecto (ej: 0039)
                    if (stringCombo.length > 3) {
                        drawTimes = stringCombo.length + 1
                    }

                    batch.setColor(1f, 1f, 1f, alphaFloat)
                    for (w in 1 until drawTimes) {
                        val totalComboLength = (drawTimes - 1) * numberSizeX
                        val positionCurrentNumber =
                            ((totalComboLength / 2) + x / 2).toInt() - numberSizeX * w
                        val n = stringComboAux[stringComboAux.length - w].toString().toInt()

                        if (n < comboNumberRegions!!.size) {
                            val region = comboNumberRegions!![n]
                            batch.draw(
                                region,
                                positionCurrentNumber.toFloat(),
                                posIntYCombo.toFloat(),
                                numberSizeX.toFloat(),
                                numberSizeY.toFloat()
                            )
                        }
                    }
                    batch.setColor(1f, 1f, 1f, 1f)
                }
            }
        }
    }

    fun dispose() {
        judgeRegions?.let {
            // Las regiones no necesitan dispose individual, solo la textura padre
        }
        comboTexture?.dispose()
        badComboTexture?.dispose()
        comboNumberRegions?.let {
            // Las regiones no necesitan dispose individual
        }
        font.dispose()
    }

    companion object {
        const val VALUE_NONE: Short = -1
        const val VALUE_PERFECT: Short = 0
        const val VALUE_GREAT: Short = 1
        const val VALUE_GOOD: Short = 2
        const val VALUE_BAD: Short = 3
        const val VALUE_MISS: Short = 4
    }
}
