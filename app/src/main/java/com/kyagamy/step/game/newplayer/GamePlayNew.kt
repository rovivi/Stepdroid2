package com.kyagamy.step.game.newplayer

import android.annotation.SuppressLint
import android.content.Context
import android.content.res.Configuration
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.PixelFormat
import android.graphics.Point
import android.graphics.PorterDuff
import android.media.MediaPlayer
import android.media.SoundPool
import android.os.Build
import android.os.Handler
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.SurfaceHolder
import android.view.SurfaceView
import android.view.WindowManager
import android.widget.VideoView
import com.kyagamy.step.R
import com.kyagamy.step.common.Common.Companion.getSize
import com.kyagamy.step.common.Common.Companion.second2Beat
import com.kyagamy.step.common.step.CommonGame.ParamsSong
import com.kyagamy.step.common.step.Game.GameRow
import com.kyagamy.step.game.newplayer.Evaluator.Companion.resetScore
import com.kyagamy.step.views.gameplayactivity.GamePlayActivity
import game.StepObject
import java.io.IOException
import java.util.*
import kotlin.math.abs

class GamePlayNew(context: Context, attrs: AttributeSet?) : SurfaceView(context, attrs),
    SurfaceHolder.Callback {
    // Core game components
    var mainThread: MainThreadNew? = null
    private var gameState: GameState? = null
    private var musicPlayer: MediaPlayer? = null
    private var renderer: GameRenderer? = null

    // Game elements
    private var stepsDrawer: StepsDrawer? = null
    private var bar: LifeBar? = null
    private var combo: Combo? = null
    private var touchPad: GamePad? = null
    private var bgPlayer: BgPlayer? = null

    // Display and configuration
    private var playerSizeX = GameConstants.DEFAULT_PLAYER_SIZE_X
    private var playerSizeY = GameConstants.DEFAULT_PLAYER_SIZE_Y
    private var isLandScape = false
    private val refreshRate: Int

    // Performance optimizations
    private val drawList = ArrayList<GameRow>()
    private var debugPaint: Paint? = null
    private var musicPlayerUpdated = false
    private var audioVideoSyncValue = 100.0

    // Game state
    @JvmField
    var fps: Double? = null
    private val handler = Handler()
    private var gamePlayActivity: GamePlayActivity? = null

    private var speed = 0

    init {
        initializePaints()
        refreshRate = getDisplayRefreshRate(context)
    }

    private fun initializePaints() {
        debugPaint = Paint().apply {
            textSize = GameConstants.DEBUG_TEXT_SIZE.toFloat()
            color = Color.WHITE
            style = Paint.Style.FILL
        }
    }

    private val musicStartRunnable = Runnable {
        musicPlayer?.let {
            it.start()
            gameState?.isRunning = true
        }
    }

    fun startGamePLay(
        videoView: VideoView?,
        stepData: StepObject,
        context: Context,
        sizeScreen: Point,
        gamePlayActivity: GamePlayActivity?,
        inputs: ByteArray
    ) {
        try {
            this.gamePlayActivity = gamePlayActivity
            isLandScape =
                context.resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE

            setupSurface()
            initializeGameComponents(stepData, inputs, context, sizeScreen, videoView)
            setupAudio(stepData)
            setupVideoView(videoView)
            initializeSoundPool()

            audioVideoSyncValue = stepData.getDisplayBPM()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun setupSurface() {
        setZOrderOnTop(true)
        holder.setFormat(PixelFormat.TRANSPARENT)
        holder.addCallback(this)
    }

    private fun initializeGameComponents(
        stepData: StepObject,
        inputs: ByteArray,
        context: Context,
        sizeScreen: Point,
        videoView: VideoView?
    ) {
        gameState = GameState(stepData, inputs).apply {
            reset()
        }

        mainThread = MainThreadNew(holder, this, refreshRate).apply {
            setRunning(true)
        }

        bgPlayer = BgPlayer(
            stepData.path,
            stepData.getBgChanges(),
            videoView,
            context,
            gameState!!.BPM
        )
        fps = 0.0
        isFocusable = true

        calculatePlayerSize(context)

        stepsDrawer = StepsDrawer(context, stepData.stepType, "16:9", isLandScape, sizeScreen)
        bar = LifeBar(context, stepsDrawer!!)
        combo = Combo(context, stepsDrawer!!)
        touchPad = GamePad(
            context,
            stepData.stepType,
            gameState!!.inputs,
            sizeScreen.x,
            getSize(context).y
        ).apply {
            setGamePlayNew(this@GamePlayNew)
        }

        combo!!.setLifeBar(bar!!)
        gameState!!.combo = combo
        gameState!!.stepsDrawer = stepsDrawer

        renderer = GameRenderer(stepsDrawer, bar, combo, debugPaint, isLandScape)
    }

    private fun calculatePlayerSize(context: Context) {
        val size = getSize(context)
        playerSizeX = size.x
        playerSizeY = (size.x * GameConstants.ASPECT_RATIO_4_3).toInt()
    }

    private fun setupAudio(stepData: StepObject) {
        try {
            musicPlayer = MediaPlayer().apply {
                setDataSource(stepData.getMusicPath())
                prepare()
                setOnCompletionListener { startEvaluation() }
                setOnPreparedListener { startGame() }
            }
        } catch (e: IOException) {
            e.printStackTrace()
        }
    }

    private fun setupVideoView(videoView: VideoView?) {
        videoView?.layoutParams?.let { params ->
            params.height = stepsDrawer!!.sizeY + stepsDrawer!!.offsetY
            params.width = stepsDrawer!!.sizeX
        }
    }

    private fun initializeSoundPool() {
        soundPool = SoundPool.Builder()
            .setMaxStreams(GameConstants.SOUNDPOOL_MAX_STREAMS)
            .build()

        soundPullBeat = soundPool!!.load(context, R.raw.beat2, 1)
        soundPullMine = soundPool!!.load(context, R.raw.mine, 1)
    }

    override fun surfaceCreated(holder: SurfaceHolder) {
        gameState?.reset()
        mainThread?.setRunning(true)
        mainThread?.start()
    }

    override fun surfaceChanged(holder: SurfaceHolder, format: Int, width: Int, height: Int) {
    }

    override fun surfaceDestroyed(holder: SurfaceHolder) {
        stop()
    }

    fun startGame() {
        resetScore()
        musicPlayer?.setOnCompletionListener { startEvaluation() }

        gameState?.start()
        try {
            if (mainThread?.running == true) {
                val offset = gameState!!.offset
                if (offset > 0) {
                    bgPlayer?.start(gameState!!.currentBeat)
                    handler.postDelayed(musicStartRunnable, (offset * 1000).toLong())
                } else {
                    musicPlayer?.apply {
                        seekTo(abs(offset * 1000).toInt())
                        setOnPreparedListener {
                            start()
                            gameState?.isRunning = true
                        }
                    }
                    bgPlayer?.start(gameState!!.currentBeat)
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    override fun draw(canvas: Canvas) {
        super.draw(canvas)
        canvas.drawColor(Color.TRANSPARENT, PorterDuff.Mode.CLEAR)

        try {
            if (gameState?.isRunning == true) {
                calculateSpeed()
                drawList.clear()
                calculateVisibleNotes()
                renderer?.drawGame(canvas, drawList, gameState, speed)
                renderer?.drawDebugInfo(
                    canvas,
                    gameState,
                    musicPlayer,
                    fps,
                    speed,
                    musicPlayerUpdated,
                    playerSizeX,
                    playerSizeY
                )

                if (gameState!!.currentElement + 1 == gameState!!.steps.size) {
                    startEvaluation()
                }
            }

            renderer?.drawUI(canvas)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun calculateSpeed() {
        speed =
            ((stepsDrawer!!.sizeNote / audioVideoSyncValue * ParamsSong.av) * GameConstants.SPEED_MULTIPLIER).toInt()
    }

    private fun calculateVisibleNotes() {
        val lastScrollAux = gameState!!.lastScroll ?: 0.0
        val lastBeat = gameState!!.currentBeat
        val lastPosition = stepsDrawer!!.sizeNote * GameConstants.NOTE_POSITION_FACTOR

        val initialIndex = findInitialVisibleIndex(lastScrollAux, lastBeat, lastPosition)
        populateVisibleNotes(initialIndex)
    }

    private fun findInitialVisibleIndex(
        lastScrollAux: Double,
        lastBeat: Double,
        lastPosition: Double
    ): Int {
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
            (gameState!!.currentElement + x) >= 0
        ) {
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

    fun update() {
        gameState?.update()
        combo?.update()

        if (gameState?.isRunning == true) {
            stepsDrawer?.update()
            bgPlayer?.update(gameState!!.currentBeat)
            bar?.update()
            syncAudioVideo()
        }
    }

    private fun syncAudioVideo() {
        if (!musicPlayerUpdated) {
            val diff =
                (gameState!!.currentSecond / 100.0) - gameState!!.offset - (musicPlayer?.currentPosition
                    ?: 0) / 1000.0
            if (abs(diff) < GameConstants.AUDIO_SYNC_THRESHOLD) {
                musicPlayerUpdated = true
            }

            if (diff >= GameConstants.AUDIO_SYNC_DIFF_THRESHOLD && !musicPlayerUpdated &&
                gameState!!.isRunning && musicPlayer?.isPlaying == true
            ) {
                gameState!!.currentBeat -= second2Beat(diff, gameState!!.BPM)
                gameState!!.currentSecond -= diff * 100
            }
        }
    }

    private fun startEvaluation() {
        stop()
        gamePlayActivity?.let {
            it.startEvaluation()
            it.finish()
        }
    }

    fun stop() {
        var retry = true
        mainThread?.setRunning(false)

        while (retry) {
            try {
                mainThread?.setRunning(false)
                releaseResources()
                retry = false
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private fun releaseResources() {
        releaseMusicPlayer()
        releaseSoundPool()
        handler.removeCallbacksAndMessages(null)
    }

    private fun releaseMusicPlayer() {
        try {
            musicPlayer?.let {
                if (it.isPlaying) {
                    it.stop()
                }
                it.release()
                musicPlayer = null
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun releaseSoundPool() {
        try {
            soundPool?.let {
                it.release()
                soundPool = null
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onTouchEvent(event: MotionEvent): Boolean {
        try {
            val maskedAction = event.actionMasked
            val fingers = event.pointerCount
            val inputsTouch = Array(fingers) { IntArray(2) }

            for (i in 0 until fingers) {
                inputsTouch[i][0] = event.getX(i).toInt()
                inputsTouch[i][1] = event.getY(i).toInt()
            }

            when (maskedAction) {
                MotionEvent.ACTION_POINTER_UP -> {
                    val actionIndex = event.getPointerId(event.actionIndex)
                    touchPad?.unpress(event.getX(actionIndex), event.getY(actionIndex))
                }
                MotionEvent.ACTION_DOWN -> {
                    handleDebugTouches(event)
                    touchPad?.checkInputs(inputsTouch, true)
                }
                MotionEvent.ACTION_UP -> {
                    if (fingers == 1) {
                        touchPad?.clearPad()
                    }
                }
                else -> {
                    touchPad?.checkInputs(inputsTouch, false)
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return true
    }

    private fun handleDebugTouches(event: MotionEvent) {
        when {
            event.x > playerSizeX / 2 && event.y < playerSizeY / 2 -> {
                speed += GameConstants.SPEED_INCREMENT
            }
            event.x < playerSizeX / 2 && event.y < playerSizeY / 2 -> {
                if (speed > GameConstants.MIN_SPEED) {
                    speed -= GameConstants.SPEED_INCREMENT
                }
            }
        }
    }

    fun getTouchPad(): GamePad? = touchPad

    fun getStepsDrawer(): StepsDrawer? = stepsDrawer

    fun notifyPadStateChanged() {
        gamePlayActivity?.syncPadState()
    }

    companion object {
        // Audio
        var soundPool: SoundPool? = null
        var soundPullBeat: Int = 0
        var soundPullMine: Int = 0

        private fun getDisplayRefreshRate(context: Context): Int {
            return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                context.display.refreshRate.let { Math.round(it) }
            } else {
                val wm = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager?
                wm?.defaultDisplay?.refreshRate?.let { Math.round(it) }
                    ?: GameConstants.DEFAULT_REFRESH_RATE
            }
        }
    }
}
