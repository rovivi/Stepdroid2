package com.kyagamy.step.game.opengl

import android.content.Context
import android.graphics.Point
import android.media.MediaPlayer
import android.opengl.GLES20
import android.opengl.GLSurfaceView
import android.view.View
import android.widget.VideoView
import com.kyagamy.step.common.Common.Companion.second2Beat
import com.kyagamy.step.common.step.CommonGame.ParamsSong
import com.kyagamy.step.common.step.Game.GameRow
import com.kyagamy.step.engine.StepsDrawerGL
import com.kyagamy.step.game.newplayer.*
import game.StepObject
import java.util.*
import javax.microedition.khronos.egl.EGLConfig
import javax.microedition.khronos.opengles.GL10
import kotlin.math.abs

/**
 * Simplified OpenGL renderer replicating [GamePlayNew] but without touch pad.
 * It reuses [GameState] logic and draws using [StepsDrawerGL].
 */
class GamePlayGLRenderer(
    private val context: Context,
    private val stepData: StepObject,
    private val videoView: VideoView?,
    private val screenSize: Point
) : GLSurfaceView.Renderer, com.kyagamy.step.engine.ISpriteRenderer {

    private var gameState: GameState? = null
    private var stepsDrawer: StepsDrawerGL? = null
    private var bar: LifeBar? = null
    private var combo: Combo? = null
    private var bgPlayer: BgPlayer? = null
    private var musicPlayer: MediaPlayer? = null
    private val handler = android.os.Handler(android.os.Looper.getMainLooper())
    private var audioVideoSyncValue = 100.0

    private val drawList = ArrayList<GameRow>()
    private var speed = 0

    fun start() {
        setupGame()
        val offset = gameState!!.offset
        musicPlayer?.setOnCompletionListener { stop() }

        if (offset > 0) {
            bgPlayer?.start(gameState!!.currentBeat)
            handler.postDelayed({
                musicPlayer?.start()
                gameState?.start()
            }, (offset * 1000).toLong())
        } else {
            musicPlayer?.seekTo(abs((offset * 1000).toInt()))
            musicPlayer?.start()
            bgPlayer?.start(gameState!!.currentBeat)
            gameState?.start()
        }
    }

    fun stop() {
        handler.removeCallbacksAndMessages(null)
        try {
            musicPlayer?.let {
                if (it.isPlaying) it.stop()
                it.release()
            }
        } catch (_: Exception) {
        }
        musicPlayer = null
        bgPlayer?.player?.stopPlayback()
    }

    private fun setupGame() {
        gameState = GameState(stepData, ByteArray(10))
        gameState!!.reset()

        stepsDrawer = StepsDrawerGL(context, stepData.stepType, "16:9", false, screenSize)
        bar = LifeBar(context, stepsDrawer!!)
        combo = Combo(context, stepsDrawer!!)

        bgPlayer = BgPlayer(stepData.path, stepData.getBgChanges(), videoView, context, gameState!!.BPM)

        musicPlayer = MediaPlayer().apply {
            setDataSource(stepData.getMusicPath())
            prepare()
            setOnCompletionListener { stop() }
        }

        audioVideoSyncValue = stepData.getDisplayBPM()

        videoView?.layoutParams?.let { params ->
            params.height = stepsDrawer!!.sizeY + stepsDrawer!!.offsetY
            params.width = stepsDrawer!!.sizeX
        }
    }

    override fun onSurfaceCreated(gl: GL10?, config: EGLConfig?) {
        GLES20.glClearColor(0f, 0f, 0f, 0f)
        GLES20.glEnable(GLES20.GL_BLEND)
        GLES20.glBlendFunc(GLES20.GL_SRC_ALPHA, GLES20.GL_ONE_MINUS_SRC_ALPHA)
        stepsDrawer?.initializeGLProgram()
    }

    override fun onSurfaceChanged(gl: GL10?, width: Int, height: Int) {
        GLES20.glViewport(0, 0, width, height)
        stepsDrawer?.setViewport(width, height)
    }

    override fun onDrawFrame(gl: GL10?) {
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT)
        update()
        drawList.clear()
        calculateVisibleNotes()
        stepsDrawer?.drawGame(drawList)
        stepsDrawer?.update()
    }

    private fun update() {
        gameState?.update()
        combo?.update()
        bgPlayer?.update(gameState!!.currentBeat)
        bar?.update()
        syncAudioVideo()
    }

    private fun syncAudioVideo() {
        val diff = (gameState!!.currentSecond / 100.0) - gameState!!.offset -
            (musicPlayer?.currentPosition ?: 0) / 1000.0
        if (abs(diff) >= GameConstants.AUDIO_SYNC_DIFF_THRESHOLD && musicPlayer?.isPlaying == true) {
            gameState!!.currentBeat -= second2Beat(diff, gameState!!.BPM)
            gameState!!.currentSecond -= diff * 100
        }
    }

    private fun calculateSpeed() {
        speed = ((stepsDrawer!!.sizeNote / audioVideoSyncValue * ParamsSong.av) *
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

    // ISpriteRenderer implementation (no-op wrappers)
    override fun draw(rect: android.graphics.Rect) {
        // Rendering is handled in onDrawFrame
    }

    override fun update() {
        // Update is triggered each frame in onDrawFrame
    }
}
