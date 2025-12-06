package com.kyagamy.step.game.libgdx.render

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.graphics.Texture
import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.kyagamy.step.common.Common
import java.util.HashMap

class LibGDXNoteSkin(private val skinName: String, private val type: String) {

    val arrows = arrayOfNulls<LibGDXSpriteReader>(10)
    val receptors = arrayOfNulls<LibGDXSpriteReader>(10)
    val longs = arrayOfNulls<LibGDXSpriteReader>(10)
    val tails = arrayOfNulls<LibGDXSpriteReader>(10)
    val explosions = arrayOfNulls<LibGDXSpriteReader>(10)
    val explosionTails = arrayOfNulls<LibGDXSpriteReader>(10)
    val tapsEffect = arrayOfNulls<LibGDXSpriteReader>(10)
    
    var mine: LibGDXSpriteReader? = null
    
    private val textures = ArrayList<Texture>()

    // Animation speed based on BPM (80 BPM = 1.0x speed)
    private var animationSpeedMultiplier = 1.0f
    private val BASE_BPM = 80f

    init {
        loadSkin()
    }

    /**
     * Sets the animation speed based on current BPM.
     * Formula: speed = currentBPM / 80 (80 BPM = 1.0x speed)
     */
    fun setAnimationSpeed(currentBPM: Float) {
        animationSpeedMultiplier = currentBPM / BASE_BPM
    }

    fun getAnimationSpeed(): Float = animationSpeedMultiplier

    private fun loadSkin() {
        val pathNS = "NoteSkins/pump/$skinName/"
        val arrowNames = Common.PIU_ARROW_NAMES // ["down_left", "up_left", "center", "up_right", "down_right"]

        // Determine number of steps based on type
        val numberSteps = when (type) {
            "pump-single" -> 5
            "pump-double", "pump-routine" -> 10
            else -> 5 // Default
        }

        try {
            for (j in 0 until 5) {
                val name = arrowNames[j]
                
                // Load Textures
                val tapTex = loadTexture(pathNS + name + "tap.png")
                val holdTex = loadTexture(pathNS + name + "hold.png")
                val holdEndTex = loadTexture(pathNS + name + "hold_end.png")
                val receptorTex = loadTexture(pathNS + name + "receptor.png")

                // Create SpriteReaders
                // Assuming standard PIU skin format:
                // Tap: 3x2 (cols x rows) usually, or 1x1 depending on skin. NoteSkin.java says 3x2.
                if (tapTex != null) arrows[j] = LibGDXSpriteReader(tapTex, 3, 2, 0.1f)
                
                // Hold Body: 6x1? NoteSkin.java says 6x1
                if (holdTex != null) longs[j] = LibGDXSpriteReader(holdTex, 6, 1, 0.1f)
                
                // Hold Tail: 6x1? NoteSkin.java says 6x1
                if (holdEndTex != null) tails[j] = LibGDXSpriteReader(holdEndTex, 6, 1, 0.1f)
                
                // Receptor: NoteSkin.java does some custom slicing. 
                // Let's assume receptor.png is 1x3 (Flash, Static, Pressed?) or similar.
                // NoteSkin.java: customSpriteArray(..., 1, 3, 0, 1, 2)
                // It seems to use frame 0 for static receptor.
                if (receptorTex != null) {
                    val regions = TextureRegion.split(receptorTex, receptorTex.width, receptorTex.height / 3)
                    // Frame 0 is usually the static receptor
                    receptors[j] = LibGDXSpriteReader(arrayOf(regions[0][0]), 0f)
                    
                    // Taps Effect (Pressed state) - usually frame 2
                    tapsEffect[j] = LibGDXSpriteReader(arrayOf(regions[2][0]), 0f)
                }
            }

            // Copy for Double/Routine
            if (numberSteps == 10) {
                for (w in 0 until 5) {
                    arrows[w + 5] = arrows[w]
                    tails[w + 5] = tails[w]
                    longs[w + 5] = longs[w]
                    receptors[w + 5] = receptors[w]
                    tapsEffect[w + 5] = tapsEffect[w]
                }
            }

            // Load Explosion
            val explosionTex = loadTexture(pathNS + "_explosion 6x1.png")
            if (explosionTex != null) {
                // 6x1 sprite sheet
                val explReader = LibGDXSpriteReader(explosionTex, 6, 1, 0.05f)
                for (cd in 0 until numberSteps) {
                    explosions[cd] = explReader
                    explosionTails[cd] = explReader
                }
            }

            // Load Mine
            // Mine is usually in resources, not assets? NoteSkin.java loads from R.drawable.mine
            // For LibGDX we need it in assets or load via Android resources (harder).
            // Let's assume we can find a mine.png in assets or use a placeholder.
            // If not found, we might need to copy it or skip.
            // For now, let's try to load from skin path if exists, or skip.
             val mineTex = loadTexture(pathNS + "mine.png")
             if (mineTex != null) {
                 mine = LibGDXSpriteReader(mineTex, 3, 2, 0.1f)
             }

        } catch (e: Exception) {
            Gdx.app.error("LibGDXNoteSkin", "Error loading skin", e)
        }
    }

    private fun loadTexture(path: String): Texture? {
        return if (Gdx.files.internal(path).exists()) {
            val tex = Texture(Gdx.files.internal(path))
            textures.add(tex)
            tex
        } else {
            Gdx.app.error("LibGDXNoteSkin", "Texture not found: $path")
            null
        }
    }

    fun dispose() {
        textures.forEach { it.dispose() }
    }
}
