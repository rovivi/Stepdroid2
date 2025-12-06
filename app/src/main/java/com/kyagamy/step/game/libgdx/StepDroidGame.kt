package com.kyagamy.step.game.libgdx

import com.badlogic.gdx.ApplicationAdapter
import com.badlogic.gdx.Gdx
import com.badlogic.gdx.audio.Music
import com.badlogic.gdx.graphics.GL20
import com.badlogic.gdx.graphics.OrthographicCamera
import com.badlogic.gdx.graphics.g2d.BitmapFont
import com.badlogic.gdx.graphics.g2d.SpriteBatch
import com.kyagamy.step.game.interfaces.ICombo
import com.kyagamy.step.game.interfaces.ILifeBar
import com.kyagamy.step.game.interfaces.IStepsDrawer
import com.kyagamy.step.game.libgdx.render.LibGDXStepsDrawer
import com.kyagamy.step.game.libgdx.render.LibGDXLifeBar
import com.kyagamy.step.game.libgdx.render.LibGDXCombo
import com.kyagamy.step.game.newplayer.GameState
import com.kyagamy.step.common.step.Game.GameRow
import game.StepObject
import kotlin.math.abs

class StepDroidGame(private val stepData: StepObject?) : ApplicationAdapter() {
    private lateinit var batch: SpriteBatch
    private lateinit var font: BitmapFont
    private lateinit var camera: OrthographicCamera
    
    // New Drawer
    private var stepsDrawer: IStepsDrawer? = null
    private var lifeBar: ILifeBar? = null
    private var combo: ICombo? = null

    // Game State
    private var gameState: GameState? = null
    private var music: Music? = null
    private var isGameStarted = false
    private var autoVelocity = 650.0 

    // Rendering state
    private val drawList = ArrayList<GameRow>()
    private var speed = 0.0

    override fun create() {
        batch = SpriteBatch()
        font = BitmapFont()
        font.data.setScale(2f)

        camera = OrthographicCamera()
        
        // Initialize Drawer
        val type = stepData?.stepType ?: "pump-single"
        val libGDXDrawer = LibGDXStepsDrawer("prime", type)
        stepsDrawer = libGDXDrawer
        
        // Initialize UI
        val libGDXLifeBar = LibGDXLifeBar(libGDXDrawer)
        lifeBar = libGDXLifeBar
        
        val libGDXCombo = LibGDXCombo(libGDXDrawer)
        libGDXCombo.setLifeBar(libGDXLifeBar)
        combo = libGDXCombo

        if (stepData != null) {
            gameState = GameState(stepData, ByteArray(10))
            gameState?.reset()
            
            // Link UI to GameState
            gameState?.stepsDrawer = stepsDrawer
            gameState?.combo = combo

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

        startGameInternal()
    }

    private fun startGameInternal() {
        if (gameState == null) return

        gameState?.start()
        val offset = gameState!!.offset.toDouble()

        if (offset > 0) {
            music?.play()
        } else {
            music?.position = abs(offset).toFloat()
            music?.play()
        }
        gameState?.isRunning = true
        isGameStarted = true
    }

    override fun resize(width: Int, height: Int) {
        camera.setToOrtho(false, width.toFloat(), height.toFloat())
        camera.update()
        (stepsDrawer as? LibGDXStepsDrawer)?.calculateDimensions()
    }

    override fun render() {
        Gdx.gl.glClearColor(0f, 0f, 0f, 1f)
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT)

        if (!isGameStarted || gameState == null) return

        // Update Logic
        gameState?.update()
        (stepsDrawer as? LibGDXStepsDrawer)?.update(Gdx.graphics.deltaTime)
        (combo as? LibGDXCombo)?.update(Gdx.graphics.deltaTime)

        // Sync Logic
        music?.let { m ->
            if (m.isPlaying) {
                // Simple sync check
            }
        }

        camera.update()
        batch.projectionMatrix = camera.combined

        calculateVisibleNotes()

        batch.begin()
        (stepsDrawer as? LibGDXStepsDrawer)?.draw(batch, drawList)
        (lifeBar as? LibGDXLifeBar)?.draw(batch)
        (combo as? LibGDXCombo)?.draw(batch)
        
        // Debug Info
        if (stepData != null) {
            font.draw(batch, "Beat: %.2f".format(gameState?.currentBeat), 20f, Gdx.graphics.height - 20f)
            font.draw(batch, "FPS: ${Gdx.graphics.framesPerSecond}", 20f, Gdx.graphics.height - 50f)
        }
        
        batch.end()
    }

    private fun calculateSpeed() {
        val bpm = stepData?.getDisplayBPM() ?: 120.0
        val noteSize = (stepsDrawer as? LibGDXStepsDrawer)?.sizeNote ?: 100f
        speed = (noteSize / bpm * autoVelocity * 2.0) 
    }

    private fun calculateVisibleNotes() {
        calculateSpeed()
        drawList.clear()

        if (gameState == null || stepsDrawer == null) return

        var lastBeat = gameState!!.currentBeat
        var lastScrollAux = gameState!!.lastScroll ?: 1.0
        
        // Start from Receptor Y (Top)
        val receptorY = (stepsDrawer as? LibGDXStepsDrawer)?.receptorY?.toDouble() ?: 0.0
        var lastPosition = receptorY

        var currentIndex = gameState!!.currentElement
        
        // Scan forward
        var x = 0
        while ((currentIndex + x) < gameState!!.steps.size) {
            val currentElement = gameState!!.steps[currentIndex + x]

            val diffBeats = currentElement.currentBeat - lastBeat
            
            // Up Scroll Logic:
            // Future notes (positive diffBeats) should be LOWER than receptor.
            // So we SUBTRACT from Y.
            lastPosition -= diffBeats * speed * gameState!!.currentSpeedMod * lastScrollAux

            currentElement.setPosY(lastPosition.toInt())

            // Visibility Check
            val sizeNote = (stepsDrawer as? LibGDXStepsDrawer)?.sizeNote ?: 100f
            if (lastPosition > -sizeNote && lastPosition < Gdx.graphics.height + sizeNote) {
                drawList.add(currentElement)
            }

            // Optimization: If we are way below screen, stop
            if (lastPosition < -sizeNote * 5) break 

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
        music?.dispose()
        (stepsDrawer as? LibGDXStepsDrawer)?.dispose()
        (lifeBar as? LibGDXLifeBar)?.dispose()
        (combo as? LibGDXCombo)?.dispose()
    }

    fun updateInputs(inputs: ByteArray) {
        gameState?.inputs = inputs
    }
}
