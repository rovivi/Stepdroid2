package com.kyagamy.step.engine

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.opengl.GLES20
import android.opengl.GLSurfaceView
import com.kyagamy.step.R
import com.kyagamy.step.game.newplayer.Combo
import com.kyagamy.step.game.newplayer.LifeBar
import com.kyagamy.step.game.newplayer.StepsDrawer
import javax.microedition.khronos.egl.EGLConfig
import javax.microedition.khronos.opengles.GL10
import kotlin.math.abs

class UIRenderer(
    private val context: Context,
    private val stepsDrawer: StepsDrawer
) : GLSurfaceView.Renderer {

    // UI components state
    private var life = 50f
    private var combo = 0
    private var positionJudge: Short = 0
    private var timeMark: Long = System.currentTimeMillis()
    private var aumentTip = -220
    private var aumentLife = 0f
    private var auxLife = 1f
    private var currentBitMapCombo: Bitmap? = null

    // Texture management
    private lateinit var batchRenderer: SpriteGLRenderer
    private val uiTextures = mutableListOf<Bitmap>()
    private val textureIds = mutableMapOf<String, Int>()

    // Position and size calculations
    private val x: Int = stepsDrawer.sizeX + stepsDrawer.offsetX
    private val y: Int = stepsDrawer.sizeY
    private val sizeX: Int = stepsDrawer.sizeNote * stepsDrawer.stepsByGameMode
    private val sizeY: Int = ((stepsDrawer.sizeNote / 3) * 1.9f).toInt()
    private val startX: Int = stepsDrawer.posInitialX
    private val startY: Int = stepsDrawer.sizeNote / 8

    // Animation state
    private var lastUpdateTime = System.currentTimeMillis()
    private var alpha = 255

    // FPS callback
    var fpsCallback: ((Float, Int) -> Unit)? = null

    // UI Data classes
    data class UIElement(
        var x: Float,
        var y: Float,
        var width: Float,
        var height: Float,
        var textureId: Int,
        var alpha: Float = 1.0f,
        var rotation: Float = 0f,
        var visible: Boolean = true
    )

    private val uiElements = mutableListOf<UIElement>()

    override fun onSurfaceCreated(gl: GL10?, config: EGLConfig?) {
        // Configure transparency
        GLES20.glEnable(GLES20.GL_BLEND)
        GLES20.glBlendFunc(GLES20.GL_SRC_ALPHA, GLES20.GL_ONE_MINUS_SRC_ALPHA)
        GLES20.glClearColor(0.0f, 0.0f, 0.0f, 0.0f)

        // Load UI textures
        loadUITextures()

        // Initialize batch renderer
        if (::batchRenderer.isInitialized) {
            batchRenderer.onSurfaceCreated(gl, config)
        }
    }

    override fun onSurfaceChanged(gl: GL10?, width: Int, height: Int) {
        if (::batchRenderer.isInitialized) {
            batchRenderer.onSurfaceChanged(gl, width, height)
        }
    }

    override fun onDrawFrame(gl: GL10?) {
        // Update animations
        val currentTime = System.currentTimeMillis()
        val deltaTime = currentTime - lastUpdateTime
        lastUpdateTime = currentTime

        updateAnimations(deltaTime)
        updateUIElements()
        renderUI()
    }

    private fun loadUITextures() {
        val myOpt2 = BitmapFactory.Options().apply {
            inSampleSize = 0
            inPreferredConfig = Bitmap.Config.ARGB_8888
        }

        // Load LifeBar textures
        val lifeBarTextures = mapOf(
            "lifebar_bg" to R.drawable.lifebar_bg,
            "lifebar_bg_danger" to R.drawable.lifebar_bg_danger,
            "lifebar_blue_tip" to R.drawable.lifebar_blue_tip,
            "lifebar_red_tip" to R.drawable.lifebar_red_tip,
            "lifebar_back_tip" to R.drawable.lifebar_back_tip,
            "lifebar_life" to R.drawable.lifebar_life,
            "lifebar_skin" to R.drawable.lifebar_skin,
            "lifebar_light_danger" to R.drawable.lifebar_light_danger,
            "lifebar_light_full" to R.drawable.lifebar_light_full
        )

        // Load Combo textures
        val comboTextures = mapOf(
            "combo_number" to R.drawable.play_combo_number,
            "combo_judge" to R.drawable.play_combo_judge,
            "combo_text" to R.drawable.play_combo,
            "combo_bad" to R.drawable.play_combo_bad
        )

        // Load all textures
        val allTextures = lifeBarTextures + comboTextures
        var textureIndex = 0

        allTextures.forEach { (name, resourceId) ->
            try {
                val bitmap = BitmapFactory.decodeResource(context.resources, resourceId, myOpt2)
                if (bitmap != null) {
                    uiTextures.add(bitmap)
                    textureIds[name] = textureIndex
                    textureIndex++
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        // Create batch renderer with all UI textures
        if (uiTextures.isNotEmpty()) {
            batchRenderer = SpriteGLRenderer(context, uiTextures.toTypedArray())
        }

        // Initialize combo bitmap
        currentBitMapCombo = uiTextures.getOrNull(textureIds["combo_text"] ?: 0)
    }

    private fun updateAnimations(deltaTime: Long) {
        // Update LifeBar animations
        if (System.nanoTime() - timeMark > 150) {
            if (aumentLife > 6 || aumentLife < 0) auxLife *= -1f
            aumentLife += auxLife
            timeMark = System.nanoTime()
        }

        // Update Combo animations
        if (aumentTip > 0) {
            aumentTip -= 1
            alpha = if (aumentTip < 6) abs(-(255 / 5 * aumentTip)) else 255
        }

        // Update batch renderer
        if (::batchRenderer.isInitialized) {
            batchRenderer.update(deltaTime)
        }
    }

    private fun updateUIElements() {
        uiElements.clear()

        // Add LifeBar elements
        addLifeBarElements()

        // Add Combo elements
        addComboElements()
    }

    private fun addLifeBarElements() {
        val percent = life / 100f
        val positionTip = startX + when {
            life < 6 -> (sizeX * 0.005).toInt()
            life > 98 -> (sizeX * 0.94).toInt()
            else -> (sizeX * (percent - 0.05)).toInt()
        }

        val positionBar = startX + if (life >= 98) sizeX else (sizeX * (percent - 0.1)).toInt()

        // Background
        val bgTextureId = if (life <= LifeBar.DANGER_VALUE) {
            textureIds["lifebar_bg_danger"] ?: 0
        } else {
            textureIds["lifebar_bg"] ?: 0
        }

        if (life < 100) {
            uiElements.add(
                UIElement(
                    startX.toFloat(),
                    startY.toFloat(),
                    sizeX.toFloat(),
                    sizeY.toFloat(),
                    batchRenderer.getTextureId(bgTextureId),
                    1.0f
                )
            )
        }

        // Glow effects
        if (life < LifeBar.DANGER_VALUE) {
            val glowRedId = textureIds["lifebar_light_danger"] ?: 0
            uiElements.add(
                UIElement(
                    startX.toFloat(),
                    startY.toFloat(),
                    sizeX.toFloat(),
                    sizeY.toFloat(),
                    batchRenderer.getTextureId(glowRedId),
                    1.0f
                )
            )
        }

        // Blue glow
        val glowBlueId = textureIds["lifebar_back_tip"] ?: 0
        val posBarBlue = sizeX * (percent - 0.06 + aumentLife / 100)
        uiElements.add(
            UIElement(
                startX.toFloat(),
                startY.toFloat(),
                posBarBlue.toFloat(),
                sizeY.toFloat(),
                batchRenderer.getTextureId(glowBlueId),
                1.0f
            )
        )

        // Life bar
        val lifeBarId = textureIds["lifebar_life"] ?: 0
        uiElements.add(
            UIElement(
                startX.toFloat(),
                startY.toFloat(),
                (positionBar - startX).toFloat(),
                sizeY.toFloat(),
                batchRenderer.getTextureId(lifeBarId),
                1.0f
            )
        )

        // Light full effect
        if (life > LifeBar.AMAZING_VALUE) {
            val lightFullId = textureIds["lifebar_light_full"] ?: 0
            uiElements.add(
                UIElement(
                    startX.toFloat(),
                    startY.toFloat(),
                    sizeX.toFloat(),
                    sizeY.toFloat(),
                    batchRenderer.getTextureId(lightFullId),
                    (aumentLife * 20 / 255f).coerceIn(0f, 1f)
                )
            )
        }

        // Skin
        val skinId = textureIds["lifebar_skin"] ?: 0
        uiElements.add(
            UIElement(
                startX.toFloat(),
                startY.toFloat(),
                sizeX.toFloat(),
                sizeY.toFloat(),
                batchRenderer.getTextureId(skinId),
                1.0f
            )
        )

        // Tip
        val tipId = if (life > LifeBar.DANGER_VALUE) {
            textureIds["lifebar_blue_tip"] ?: 0
        } else {
            textureIds["lifebar_red_tip"] ?: 0
        }
        uiElements.add(
            UIElement(
                positionTip.toFloat(),
                startY.toFloat(),
                (sizeX * 0.08f),
                sizeY.toFloat(),
                batchRenderer.getTextureId(tipId),
                1.0f
            )
        )
    }

    private fun addComboElements() {
        if (aumentTip <= 0) return

        // Calculate sizes
        val numberSizeY = (y * 0.05555556f * 1.15f).toInt()
        val numberSizeX = (y * 0.05555556f * 1.15f).toInt()
        var comboSizeY = (y * 0.0363637f * 1.25f).toInt()
        var comboSizeX = (y * 0.14815f * 1.25f).toInt()
        var labelSizeY = (y * 0.0555555556f).toInt()
        var labelSizeX = (y * 0.306f).toInt()

        // Scale animation
        if (aumentTip > 14 && aumentTip < 21) {
            val relation = 1 + (aumentTip - 15) * 0.22f * 0.6666666667f
            labelSizeY = (labelSizeY * relation).toInt()
            labelSizeX = (labelSizeX * relation).toInt()
            comboSizeX = (comboSizeX * ((relation - 1) / 3 + 1)).toInt()
            comboSizeY = (comboSizeY * ((relation - 1) / 3 + 1)).toInt()
        }

        val posLabelIntX = ((x / 2f - labelSizeX / 2f) * 1.008).toInt()
        val posComboIntX = (x / 2f - comboSizeX / 2f).toInt()
        val currentAlpha = (alpha / 255f).coerceIn(0f, 1f)

        var posIntYCombo = (y / 2 - (numberSizeY + labelSizeY + comboSizeY) / 2)

        // Judge label
        val judgeId = textureIds["combo_judge"] ?: 0
        uiElements.add(
            UIElement(
                posLabelIntX.toFloat(),
                posIntYCombo.toFloat(),
                labelSizeX.toFloat(),
                labelSizeY.toFloat(),
                batchRenderer.getTextureId(judgeId),
                currentAlpha
            )
        )

        posIntYCombo = (posIntYCombo + labelSizeY * 1.08).toInt()

        // Show combo text and numbers if combo > 3 or < -3
        if (combo > 3 || combo < -3) {
            val comboTextId = if (combo >= 0) {
                textureIds["combo_text"] ?: 0
            } else {
                textureIds["combo_bad"] ?: 0
            }

            uiElements.add(
                UIElement(
                    posComboIntX.toFloat(),
                    posIntYCombo.toFloat(),
                    comboSizeX.toFloat(),
                    comboSizeY.toFloat(),
                    batchRenderer.getTextureId(comboTextId),
                    currentAlpha
                )
            )

            posIntYCombo += comboSizeY

            // Draw combo numbers
            val stringCombo = abs(combo).toString()
            var drawTimes = 4
            if (stringCombo.length > 3) drawTimes = stringCombo.length + 1

            for (w in 1 until drawTimes) {
                val totalComboLength = (drawTimes - 1) * numberSizeX
                val positionCurrentNumber = ((totalComboLength / 2) + x / 2) - numberSizeX * w
                val digitIndex = if (w <= stringCombo.length) {
                    stringCombo[stringCombo.length - w].toString().toInt()
                } else {
                    0
                }

                val numberTextureId = textureIds["combo_number"] ?: 0
                uiElements.add(
                    UIElement(
                        positionCurrentNumber.toFloat(),
                        posIntYCombo.toFloat(),
                        numberSizeX.toFloat(),
                        numberSizeY.toFloat(),
                        batchRenderer.getTextureId(numberTextureId),
                        currentAlpha
                    )
                )
            }
        }
    }

    private fun renderUI() {
        if (!::batchRenderer.isInitialized) return

        batchRenderer.begin()

        uiElements.forEach { element ->
            if (element.visible) {
                val model = batchRenderer.createTransformMatrix(
                    element.x + element.width / 2f,
                    element.y + element.height / 2f,
                    element.width / 2f,
                    element.height / 2f,
                    element.rotation
                )

                val uvCoords = UVCoords()
                batchRenderer.drawCommand(element.textureId, model, uvCoords)
            }
        }

        batchRenderer.end()
    }

    // Public methods to update UI state (similar to LifeBar and Combo)
    fun updateLife(typeTap: Short, comboValue: Int) {
        when (typeTap) {
            Combo.VALUE_PERFECT, Combo.VALUE_GREAT -> life += 1 * abs(comboValue)
            Combo.VALUE_BAD -> life -= 0.3f * abs(comboValue)
            Combo.VALUE_MISS -> life -= 3 * abs(comboValue)
        }
        life = life.coerceIn(0f, 100f)
    }

    fun setComboUpdate(typeTap: Short) {
        positionJudge = typeTap
        when (typeTap) {
            Combo.VALUE_PERFECT -> combo = if (combo < 0) 1 else (combo + 1)
            Combo.VALUE_GREAT -> combo = if (combo < 0) 1 else (combo + 1)
            Combo.VALUE_GOOD -> if (combo < -4) combo = 0
            Combo.VALUE_BAD -> if (combo != 0) combo = 0
            Combo.VALUE_MISS -> combo = if (combo > 0) 0 else (combo - 1)
        }
        show()
    }

    fun show() {
        aumentTip = 20
        alpha = 255
        currentBitMapCombo = if (combo >= 0) {
            uiTextures.getOrNull(textureIds["combo_text"] ?: 0)
        } else {
            uiTextures.getOrNull(textureIds["combo_bad"] ?: 0)
        }
    }

    // Getters
    fun getLife(): Float = life
    fun getCombo(): Int = combo

    companion object {
        // Constants from Combo class
        const val COMBO_NUMBER_RATIO_X = 0.05555556f * 1.15f
        const val COMBO_TEXT_RATIO_Y = 0.0363637f * 1.25f
        const val COMBO_TEXT_RATIO_X = 0.14815f * 1.25f
        const val COMBO_LABEL_RATIO_Y = 0.0555555556f
        const val COMBO_LABEL_RATIO_X = 0.306f
        const val RATIO_BIGGER_LABEL = 0.6666666667f
    }
}