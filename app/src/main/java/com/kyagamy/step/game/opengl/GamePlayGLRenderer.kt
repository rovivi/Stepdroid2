package com.kyagamy.step.game.opengl

import android.content.Context
import android.graphics.Point
import android.media.MediaPlayer
import android.media.SoundPool
import android.opengl.GLES20
import android.opengl.GLSurfaceView
import android.widget.VideoView
import com.kyagamy.step.R
import com.kyagamy.step.common.Common.Companion.second2Beat
import com.kyagamy.step.common.step.CommonGame.ParamsSong
import com.kyagamy.step.common.step.Game.GameRow
import com.kyagamy.step.engine.ArrowSpriteRenderer
import com.kyagamy.step.engine.ISpriteRenderer
import com.kyagamy.step.engine.StepsDrawerGL
import com.kyagamy.step.engine.UIRenderer
import com.kyagamy.step.engine.UVCoords
import com.kyagamy.step.game.newplayer.*
import game.StepObject
import javax.microedition.khronos.egl.EGLConfig
import javax.microedition.khronos.opengles.GL10
import kotlin.math.abs

/**
 * Simplified OpenGL renderer replicating [GamePlayNew] but without touch pad.
 * It reuses [GameState] logic and draws using [StepsDrawerGL] with [ArrowSpriteRenderer].
 *
 * Note: When this renderer is used in an Activity, ensure edge-to-edge is properly configured
 * by extending FullScreenActivity or using EdgeToEdgeHelper.setupGameEdgeToEdge()
 */
class GamePlayGLRenderer(
    private val context: Context,
    private val stepData: StepObject,
    private val videoView: VideoView?,
    private val screenSize: Point,
    private val inputs: ByteArray? = null
) : GLSurfaceView.Renderer, ISpriteRenderer {

    private var gameState: GameState? = null
    private var stepsDrawer: StepsDrawerGL? = null
    private var arrowRenderer: ArrowSpriteRenderer? = null
    private var uiRenderer: UIRenderer? = null
    private var bar: LifeBar? = null
    private var combo: Combo? = null
    private var bgPlayer: BgPlayer? = null
    private var musicPlayer: MediaPlayer? = null
    private val handler = android.os.Handler(android.os.Looper.getMainLooper())
    private var audioVideoSyncValue = 100.0
    private var isGameStarted = false

    private val drawList = ArrayList<GameRow>()
    private var speed = 0
    private val autoVelocity = ParamsSong.av

    // FPS tracking
    private var frameCount = 0
    private var lastFPSTime = System.currentTimeMillis()
    private var currentFPS = 0.0

    // Audio effects
    private var soundPool: SoundPool? = null
    private var soundPullBeat: Int = 0
    private var soundPullMine: Int = 0

    // Batching state
    private var batchActive = false

    init {
        initializeSoundPool()
    }

    private fun initializeSoundPool() {
        soundPool = SoundPool.Builder()
            .setMaxStreams(GameConstants.SOUNDPOOL_MAX_STREAMS)
            .build()

        soundPullBeat = soundPool!!.load(context, R.raw.beat2, 1)
        soundPullMine = soundPool!!.load(context, R.raw.mine, 1)
    }

    fun getFPS(): Double = currentFPS

    fun getVisibleArrowCount(): Int = drawList.size

    fun start() {
        setupGame()
        android.util.Log.d("GamePlayGLRenderer", "Renderer start called")
        // If MediaPlayer is ready, startGameInternal will be called by onPrepared
        // If no MediaPlayer, start immediately
        if (musicPlayer == null) {
            startGameInternal()
        }
    }

    fun stop() {
        isGameStarted = false
        handler.removeCallbacksAndMessages(null)
        try {
            musicPlayer?.let {
                if (it.isPlaying) it.stop()
                it.release()
            }
        } catch (_: Exception) {
        }
        musicPlayer = null

        try {
            soundPool?.let {
                it.release()
                soundPool = null
            }
        } catch (_: Exception) {
        }
    }

    private fun setupGame() {
        if (gameState != null) {
            return
        }
        gameState = GameState(stepData, inputs ?: ByteArray(10))
        gameState?.reset()
        stepsDrawer = StepsDrawerGL(context, stepData.stepType, "16:9", false, screenSize)
        arrowRenderer = ArrowSpriteRenderer(context)
        stepsDrawer?.setArrowRenderer(arrowRenderer!!)

        // Regular StepsDrawer is required for lifebar/combo compatibility
        val regularStepsDrawer = StepsDrawer(context, stepData.stepType, "16:9", false, screenSize)

        // Initialize UIRenderer instead of individual components
        uiRenderer = UIRenderer(context, regularStepsDrawer)

        // Keep these for backward compatibility (some parts of GameState might need them)
        bar = LifeBar(context, regularStepsDrawer)
        combo = Combo(context, regularStepsDrawer)

        bgPlayer = BgPlayer(stepData.path, stepData.getBgChanges(), videoView, context, gameState!!.BPM)

        // Set up audio exactly like GamePlayNew
        setupAudio()

        audioVideoSyncValue = stepData.getDisplayBPM()
        combo?.setLifeBar(bar!!)
        gameState?.combo = combo
        gameState?.stepsDrawer = StepsDrawer(context, stepData.stepType, "16:9", false, screenSize)

        // Set up sound effects in the game state
        setupSoundEffects()
    }

    private fun setupSoundEffects() {
        // Configure sound effects to play on note hits
        // This would integrate with GameState's evaluation system
        gameState?.let { state ->
            // The sound effects are triggered through the GameState evaluation logic
            // when notes are processed in the evaluate() method
        }
    }

    private fun setupAudio() {
        try {
            musicPlayer = MediaPlayer().apply {
                setDataSource(stepData.getMusicPath())
                setOnCompletionListener { stop() }
                setOnPreparedListener {
                android.util.Log.d("GamePlayGLRenderer", "MediaPlayer prepared, starting game")
                    // Set volume to maximum to ensure we can hear it
                    setVolume(1.0f, 1.0f)
                    startGameInternal()
                }
                setOnErrorListener { mp, what, extra ->
                    android.util.Log.e(
                        "GamePlayGLRenderer",
                        "MediaPlayer error: what=$what, extra=$extra"
                    )
                    false
                }
                prepareAsync()
            }
            android.util.Log.d(
                "GamePlayGLRenderer",
                "MediaPlayer setup with path: ${stepData.getMusicPath()}"
            )
        } catch (e: Exception) {
            android.util.Log.e("GamePlayGLRenderer", "Error setting up MediaPlayer", e)
            e.printStackTrace()
            musicPlayer = null
        }
    }

    private fun startGameInternal() {
        android.util.Log.d("GamePlayGLRenderer", "Starting game internally")
        gameState?.start()

        try {
            val offset = gameState!!.offset.toDouble()
            android.util.Log.d("GamePlayGLRenderer", "Offset: $offset")

            if (offset > 0) {
                bgPlayer?.start(gameState!!.currentBeat)
                handler.postDelayed({
                    musicPlayer?.let { mp ->
                        if (!mp.isPlaying) {
                            mp.start()
                            android.util.Log.d(
                                "GamePlayGLRenderer",
                                "Music started after offset delay - isPlaying: ${mp.isPlaying}"
                            )
                        }
                    }
                    gameState?.isRunning = true
                    isGameStarted = true
                }, (offset * 1000).toLong())
            } else {
                musicPlayer?.apply {
                    seekTo(abs(offset * 1000).toInt())
                    if (!isPlaying) {
                        start()
                        android.util.Log.d(
                            "GamePlayGLRenderer",
                            "Music started immediately - isPlaying: $isPlaying"
                        )
                    }
                }
                bgPlayer?.start(gameState!!.currentBeat)
                gameState?.isRunning = true
                isGameStarted = true
            }
        } catch (e: Exception) {
            android.util.Log.e("GamePlayGLRenderer", "Error starting game", e)
            e.printStackTrace()
        }
    }

    fun playBeatSound() {
        soundPool?.play(soundPullBeat, 1.0f, 1.0f, 0, 0, 1.0f)
    }

    fun playMineSound() {
        soundPool?.play(soundPullMine, 1.0f, 1.0f, 0, 0, 1.0f)
    }

    override fun onSurfaceCreated(gl: GL10?, config: EGLConfig?) {
        GLES20.glClearColor(0f, 0f, 0f, 0f)
        GLES20.glEnable(GLES20.GL_BLEND)
        GLES20.glBlendFunc(GLES20.GL_SRC_ALPHA, GLES20.GL_ONE_MINUS_SRC_ALPHA)
        setupGame()
        // Initialize renderers
        arrowRenderer?.onSurfaceCreated(gl, config)
        uiRenderer?.onSurfaceCreated(gl, config)
    }

    override fun onSurfaceChanged(gl: GL10?, width: Int, height: Int) {
        GLES20.glViewport(0, 0, width, height)
        stepsDrawer?.setViewport(width, height)
        // Initialize renderers viewport
        arrowRenderer?.onSurfaceChanged(gl, width, height)
        uiRenderer?.onSurfaceChanged(gl, width, height)
    }

    override fun onDrawFrame(gl: GL10?) {
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT)
        if (!isGameStarted) return

        updateFPS()
        updateGame()
        drawList.clear()
        calculateVisibleNotes()

        // Draw game notes using StepsDrawerGL
        stepsDrawer?.drawGame(drawList)
        stepsDrawer?.update()

        // Draw arrow sprites using ArrowSpriteRenderer
        arrowRenderer?.onDrawFrame(gl)

        // Draw UI elements using UIRenderer
        uiRenderer?.onDrawFrame(gl)

        if (gameState != null && gameState!!.currentElement + 1 >= gameState!!.steps.size) {
            stop()
        }
    }

    private fun updateFPS() {
        frameCount++
        val currentTime = System.currentTimeMillis()
        if (currentTime - lastFPSTime >= 1000) {
            currentFPS = frameCount * 1000.0 / (currentTime - lastFPSTime)
            frameCount = 0
            lastFPSTime = currentTime
        }
    }

    private fun updateGame() {
        gameState?.update()

        // Log current game state for debugging
        if (frameCount % 300 == 0) { // Log every 5 seconds at 60fps
            android.util.Log.d(
                "GamePlayGLRenderer",
                "Game State - Beat: ${gameState?.currentBeat}, Element: ${gameState?.currentElement}, Running: ${gameState?.isRunning}"
            )
        }

        // Update UI renderer with game state
        uiRenderer?.let { ui ->
            // Sync life state
            bar?.let { b ->
                val currentUILife = ui.getLife()
                val gameLife = b.life

                // If there's a difference, we need to update the UI
                if (kotlin.math.abs(currentUILife - gameLife) > 0.1f) {
                    android.util.Log.d(
                        "GamePlayGLRenderer",
                        "Life updated: UI=$currentUILife → Game=$gameLife"
                    )
                    // Calculate the difference and apply it
                    val lifeDiff = gameLife - currentUILife
                    if (lifeDiff > 0) {
                        ui.updateLife(Combo.VALUE_PERFECT, 1)
                    } else if (lifeDiff < 0) {
                        ui.updateLife(Combo.VALUE_MISS, 1)
                    }
                }
            }

            // Sync combo state - this is the key part for combo display
            combo?.let { c ->
                // If there's a combo update from the game (when a note is actually hit)
                if (c.positionJudge != 0.toShort()) {
                    val judgeText = when (c.positionJudge) {
                        Combo.VALUE_PERFECT -> "PERFECT"
                        Combo.VALUE_GREAT -> "GREAT"
                        Combo.VALUE_GOOD -> "GOOD"
                        Combo.VALUE_BAD -> "BAD"
                        Combo.VALUE_MISS -> "MISS"
                        else -> "UNKNOWN(${c.positionJudge})"
                    }
                    android.util.Log.d("GamePlayGLRenderer", "🎵 NOTE EVALUATED! Judge: $judgeText")
                    android.util.Log.d(
                        "GamePlayGLRenderer",
                        "📊 Evaluator stats - P:${Evaluator.PERFECT} G:${Evaluator.GREAT} O:${Evaluator.GOOD} B:${Evaluator.BAD} M:${Evaluator.MISS}"
                    )

                    ui.setComboUpdate(c.positionJudge)
                    // Reset the judge position to avoid repeated updates
                    c.positionJudge = 0
                } else {
                    // Log inputs periodically to see if they're being detected
                    if (frameCount % 60 == 0 && gameState?.inputs?.any { it.toInt() != 0 } == true) {
                        val inputsStr =
                            gameState?.inputs?.mapIndexed { i, v -> if (v.toInt() != 0) "[$i]=$v" else "" }
                                ?.filter { it.isNotEmpty() }?.joinToString(" ")
                        if (!inputsStr.isNullOrEmpty()) {
                            android.util.Log.d("GamePlayGLRenderer", "🎮 Active inputs: $inputsStr")
                        }
                    }
                }
            }
        }

        // Keep original components for compatibility
        combo?.update()
        bgPlayer?.update(gameState!!.currentBeat)
        bar?.update()
        syncAudioVideo()
    }

    // Method to manually trigger UI updates from external game logic
    fun updateUIFromGameState(typeTap: Short, comboValue: Int) {
        uiRenderer?.let { ui ->
            ui.updateLife(typeTap, comboValue)
            ui.setComboUpdate(typeTap)
        }
    }

    // Getters for UI state
    fun getUILife(): Float = uiRenderer?.getLife() ?: 50f
    fun getUICombo(): Int = uiRenderer?.getCombo() ?: 0

    private fun syncAudioVideo() {
        val diff = (gameState!!.currentSecond / 100.0) - gameState!!.offset -
                (musicPlayer?.currentPosition ?: 0) / 1000.0
        if (abs(diff) >= GameConstants.AUDIO_SYNC_DIFF_THRESHOLD && musicPlayer?.isPlaying == true) {
            gameState!!.currentBeat -= second2Beat(diff, gameState!!.BPM)
            gameState!!.currentSecond -= diff * 100
        }
    }

    private fun calculateSpeed() {
        speed = ((stepsDrawer!!.sizeNote / audioVideoSyncValue * autoVelocity) *
                GameConstants.SPEED_MULTIPLIER).toInt()
    }

    private fun calculateVisibleNotes() {
        calculateSpeed()
        val lastScrollAux = gameState!!.lastScroll ?: 0.0
        val lastBeat = gameState!!.currentBeat
        val lastPosition = stepsDrawer!!.sizeNote * GameConstants.NOTE_POSITION_FACTOR
        val initialIndex = findInitialVisibleIndex(lastScrollAux, lastBeat, lastPosition)
        populateVisibleNotes(initialIndex)
    }

    private fun findInitialVisibleIndex(lastScrollAux: Double, lastBeat: Double, lastPosition: Double): Int {
        var initialIndex = 0
        var currentPosition = lastPosition
        var currentBeat = lastBeat
        var x = 0
        while ((gameState!!.currentElement + x) >= 0 && lastScrollAux != 0.0) {
            val currentElement = gameState!!.steps[gameState!!.currentElement + x]
            val diffBeats = currentElement.currentBeat - currentBeat
            currentPosition += diffBeats * speed * gameState!!.currentSpeedMod * lastScrollAux
            if (currentPosition < -stepsDrawer!!.sizeNote * GameConstants.NOTE_SIZE_MULTIPLIER) break
            currentBeat = currentElement.currentBeat
            initialIndex = x
            x--
        }
        return initialIndex
    }

    private fun populateVisibleNotes(initialIndex: Int) {
        var lastScrollAux = gameState!!.lastScroll ?: 0.0
        var lastBeat = gameState!!.currentBeat
        var lastPosition = stepsDrawer!!.sizeNote * GameConstants.NOTE_POSITION_FACTOR
        var x = initialIndex
        while ((gameState!!.currentElement + x) < gameState!!.steps.size &&
            (gameState!!.currentElement + x) >= 0) {
            val currentElement = gameState!!.steps[gameState!!.currentElement + x]
            val diffBeats = currentElement.currentBeat - lastBeat
            lastPosition += diffBeats * speed * gameState!!.currentSpeedMod * lastScrollAux
            currentElement.notes?.let {
                currentElement.setPosY(lastPosition.toInt())
                drawList.add(currentElement)
            }
            if (lastPosition >= stepsDrawer!!.sizeY + stepsDrawer!!.sizeNote) break
            currentElement.modifiers?.get("SCROLLS")?.let { scrolls ->
                if (x >= 0) {
                    lastScrollAux = scrolls[1]
                }
            }
            lastBeat = currentElement.currentBeat
            x++
        }
    }

    override fun begin() {
        if (batchActive) {
            android.util.Log.w("GamePlayGLRenderer", "begin() called while batch is already active")
            return
        }
        batchActive = true
        stepsDrawer?.begin()
    }

    override fun drawCommand(
        textureId: Int,
        model: FloatArray,
        uvCoords: UVCoords
    ) {
        if (!batchActive) {
            android.util.Log.w(
                "GamePlayGLRenderer",
                "drawCommand() called outside of begin()/end()"
            )
            return
        }
        stepsDrawer?.drawCommand(textureId, model, uvCoords)
    }

    override fun end() {
        if (!batchActive) {
            android.util.Log.w("GamePlayGLRenderer", "end() called without begin()")
            return
        }
        batchActive = false
        stepsDrawer?.end()
    }

    override fun update(deltaMs: Long) {
        if (isGameStarted) {
            updateGame()
        }
    }

    // Backward compatibility methods
    @Deprecated("Use begin()/end() pattern instead")
    override fun flushBatch() {
        stepsDrawer?.flushBatch()
    }

    @Deprecated("Use begin()/end() pattern instead")
    override fun clearCommands() {
        stepsDrawer?.clearCommands()
    }

    // ISpriteRenderer implementation (no-op wrappers)
    @Deprecated("Use drawCommand instead")
    override fun draw(rect: android.graphics.Rect) {
        // Rendering is handled in onDrawFrame
    }

    @Deprecated("Use update(deltaMs) instead")
    override fun update() {
        // No operation needed; game and UI update is handled in updateGame().
    }
}
