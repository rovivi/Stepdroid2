package com.kyagamy.step.game.libgdx

import com.badlogic.gdx.ApplicationAdapter
import com.badlogic.gdx.Gdx
import com.badlogic.gdx.audio.Music
import com.badlogic.gdx.graphics.GL20
import com.badlogic.gdx.graphics.OrthographicCamera
import com.badlogic.gdx.graphics.g2d.BitmapFont
import com.badlogic.gdx.graphics.g2d.SpriteBatch
import com.badlogic.gdx.graphics.Texture
import com.badlogic.gdx.graphics.g2d.Sprite
import game.StepObject
import com.kyagamy.step.common.step.CommonSteps
import java.util.HashMap
import com.badlogic.gdx.graphics.glutils.ShapeRenderer
import com.kyagamy.step.game.newplayer.GameState
import com.kyagamy.step.game.newplayer.GameConstants
import kotlin.math.abs
import com.kyagamy.step.common.step.Game.GameRow

class StepDroidGame(private val stepData: StepObject?) : ApplicationAdapter() {
    private lateinit var batch: SpriteBatch
    private lateinit var font: BitmapFont
    private lateinit var camera: OrthographicCamera
    private lateinit var shapeRenderer: ShapeRenderer

    // Game State
    private var gameState: GameState? = null
    private var music: Music? = null
    private var isGameStarted = false
    private var autoVelocity = 650.0 // From ParamsSong.av usually

    // Skin resources
    private val skinTextures = HashMap<String, Texture>()
    private val arrowNames = arrayOf("down_left", "up_left", "center", "up_right", "down_right")
    private val skinPath = "NoteSkins/pump/prime/" // Default skin

    // Rendering state
    private val drawList = ArrayList<GameRow>() // Correct import used
    private var speed = 0.0
    private var noteSize = 100f // Will be calculated
    private val receptorY = 150f // Fixed receptor position for now

    override fun create() {
        batch = SpriteBatch()
        shapeRenderer = ShapeRenderer()
        font = BitmapFont()
        font.data.setScale(2f)

        camera = OrthographicCamera()

        if (stepData != null) {
            // Initialize GameState
            gameState = GameState(stepData, ByteArray(10))
            gameState?.reset()

            // Load Music
            try {
                val musicFile = Gdx.files.absolute(stepData.getMusicPath())
                if (musicFile.exists()) {
                    music = Gdx.audio.newMusic(musicFile)
                    music?.volume = 1.0f
                } else {
                    Gdx.app.error("StepDroid", "Music file not found: ${stepData.getMusicPath()}")
                }
            } catch (e: Exception) {
                Gdx.app.error("StepDroid", "Error loading music", e)
            }
        }

        loadSkin()

        startGameInternal()
    }

    private fun startGameInternal() {
        if (gameState == null) return

        gameState?.start()
        val offset = gameState!!.offset.toDouble()

        // Basic sync start similar to GamePlayGLRenderer
        // For simplicity in LibGDX, we can rely on updates
        if (offset > 0) {
            // Delay music start
            // For now, just start music and handle offset in update logic or a timer
            // LibGDX doesn't have a simple postDelayed like Handler, so we'd use a timer variable
            // But for immediate feedback, let's start music
            music?.play()
        } else {
            // Negative offset means music starts 'late' into the track
            music?.position = abs(offset).toFloat()
            music?.play()
        }
        gameState?.isRunning = true
        isGameStarted = true
    }

    override fun resize(width: Int, height: Int) {
        camera.setToOrtho(false, width.toFloat(), height.toFloat())
        camera.update()

        // Recalculate note size based on screen width (similar to StepsDrawerGL)
        // width / 10 for 5 notes + spacing?
        noteSize = (width / 10).toFloat()
    }

    private fun loadSkin() {
        try {
            Gdx.app.log("StepDroid", "Loading skin from $skinPath")
            arrowNames.forEach { name ->
                val tapPath = "$skinPath${name}_tap.png"
                val receptorPath = "$skinPath${name}_receptor.png"

                if (Gdx.files.internal(tapPath).exists()) {
                    skinTextures["${name}_tap"] = Texture(Gdx.files.internal(tapPath))
                }
                if (Gdx.files.internal(receptorPath).exists()) {
                    skinTextures["${name}_receptor"] = Texture(Gdx.files.internal(receptorPath))
                }

                // Load holds, mines, etc if available
            }
        } catch (e: Exception) {
            Gdx.app.error("StepDroid", "Error loading skin: ${e.message}")
            e.printStackTrace()
        }
    }

    override fun render() {
        Gdx.gl.glClearColor(0f, 0f, 0f, 0f) // Transparent background
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT)

        if (!isGameStarted || gameState == null) return

        // Update Game Logic
        gameState?.update()

        // Sync (Simple version)
        // LibGDX music position is in seconds
        music?.let { m ->
            if (m.isPlaying) {
                val diff = (gameState!!.currentSecond / 100.0) - gameState!!.offset - m.position
                if (abs(diff) > 0.05) {
                    // Adjust beat if drift is too large
                    // gameState!!.currentBeat -= ... (Simplification: rely on GameState update for now)
                }
            }
        }

        camera.update()
        batch.projectionMatrix = camera.combined

        calculateVisibleNotes()

        batch.begin()

        // Draw Receptors
        val totalWidth = noteSize * 5
        val startX = (Gdx.graphics.width - totalWidth) / 2f

        for (i in 0 until 5) {
            val texture = skinTextures["${arrowNames[i]}_receptor"]
            if (texture != null) {
                batch.draw(texture, startX + i * noteSize, receptorY, noteSize, noteSize)
            }
        }

        // Draw Notes
        drawList.forEach { row ->
            val yPos = row.getPosY().toFloat()

            row.notes?.forEachIndexed { index, note ->
                if (index < 5 && note.type != CommonSteps.NOTE_EMPTY) {
                    // Determine texture based on note type
                    val textureKey = when (note.type) {
                        CommonSteps.NOTE_TAP -> "${arrowNames[index]}_tap"
                        else -> "${arrowNames[index]}_tap" // Fallback
                    }

                    val texture = skinTextures[textureKey]
                    if (texture != null) {
                        batch.draw(texture, startX + index * noteSize, yPos, noteSize, noteSize)
                    }
                }
            }
        }

        // Debug Info
        if (stepData != null) {
            font.draw(
                batch,
                "Beat: %.2f".format(gameState?.currentBeat),
                20f,
                Gdx.graphics.height - 20f
            )
            font.draw(batch, "GDX FPS: ${Gdx.graphics.framesPerSecond}", 20f, Gdx.graphics.height - 50f)
        }

        batch.end()
    }

    private fun calculateSpeed() {
        // Replicating: speed = ((stepsDrawer!!.sizeNote / audioVideoSyncValue * autoVelocity) * GameConstants.SPEED_MULTIPLIER).toInt()
        // Simplified for LibGDX without StepsDrawer dependency for now
        val bpm = stepData?.getDisplayBPM() ?: 120.0
        speed = (noteSize / bpm * autoVelocity * 2.0) // 2.0 is roughly SPEED_MULTIPLIER default
    }

    private fun calculateVisibleNotes() {
        calculateSpeed()
        drawList.clear()

        if (gameState == null) return

        // Logic from GamePlayGLRenderer.calculateVisibleNotes adapted
        // We need to find notes that would be visible on screen based on currentBeat and speed
        // visible range: roughly from receptorY down to top of screen

        var lastBeat = gameState!!.currentBeat
        var lastScrollAux = gameState!!.lastScroll ?: 1.0
        var lastPosition = receptorY.toDouble() // Start at receptors

        // Start scanning from current element
        var currentIndex = gameState!!.currentElement

        // Look backwards slightly to catch notes just passed?
        // GamePlayGLRenderer scans forward. Let's stick to forward for simplicity first.

        // Simple forward scan
        val maxY = Gdx.graphics.height + noteSize

        // We iterate a bit to find visible notes
        // Ideally we'd use the same sophisticated logic as GamePlayGLRenderer
        // For now, let's iterate forward until notes are off top of screen

        var x = 0
        while ((currentIndex + x) < gameState!!.steps.size) {
            val currentElement = gameState!!.steps[currentIndex + x]

            // Calculate position relative to current beat
            val diffBeats = currentElement.currentBeat - lastBeat
            lastPosition += diffBeats * speed * gameState!!.currentSpeedMod * lastScrollAux

            // Set calculated position on the row object (if mutable) or store temporarily
            // The GameRow class in this project seems to have setPosY
            currentElement.setPosY(lastPosition.toInt())

            // If visible, add to draw list
            if (lastPosition > -noteSize && lastPosition < maxY) {
                drawList.add(currentElement)
            }

            if (lastPosition >= maxY) break // Optimization: stop if off screen top

            // Update scroll mod if changed
            currentElement.modifiers?.get("SCROLLS")?.let { scrolls ->
                lastScrollAux = scrolls[1]
            }

            lastBeat = currentElement.currentBeat
            x++
        }
    }

    override fun dispose() {
        batch.dispose()
        font.dispose()
        shapeRenderer.dispose()
        music?.dispose()
        skinTextures.values.forEach { it.dispose() }
    }

    fun updateInputs(inputs: ByteArray) {
        // Pass inputs to GameState if needed
        gameState?.inputs = inputs
    }
}
