package com.kyagamy.step.views.gameplayactivity

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.graphics.BitmapFactory
import android.graphics.PixelFormat
import android.graphics.Point
import android.graphics.Rect
import android.graphics.drawable.Drawable
import android.media.AudioManager
import android.media.MediaPlayer
import android.opengl.GLSurfaceView
import android.os.Bundle
import android.util.DisplayMetrics
import android.util.TypedValue
import android.view.KeyEvent
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.Button
import android.widget.RelativeLayout
import android.widget.Toast
import androidx.core.content.ContextCompat.getSystemService
import com.badlogic.gdx.backends.android.AndroidApplication
import com.badlogic.gdx.backends.android.AndroidApplicationConfiguration
import com.google.gson.Gson
import com.kyagamy.step.R
import com.kyagamy.step.common.Common.Companion.convertStreamToString
import com.kyagamy.step.common.step.CommonGame.ArrowsPositionPlace
import com.kyagamy.step.common.step.CommonGame.ParamsSong
import com.kyagamy.step.common.step.CommonGame.TransformBitmap
import com.kyagamy.step.common.step.CommonGame.TransformBitmap.Companion.doBrightness
import com.kyagamy.step.common.step.CommonSteps.Companion.ARROW_HOLD_PRESSED
import com.kyagamy.step.common.step.CommonSteps.Companion.ARROW_PRESSED
import com.kyagamy.step.common.step.CommonSteps.Companion.ARROW_UNPRESSED
import com.kyagamy.step.common.step.Parsers.FileSSC
import com.kyagamy.step.databinding.ActivityPlayerbgaBinding
import com.kyagamy.step.game.libgdx.StepDroidGame
import com.kyagamy.step.game.newplayer.Evaluator
import com.kyagamy.step.utils.EdgeToEdgeHelper
import com.kyagamy.step.ui.EvaluationActivity
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import com.kyagamy.step.room.SDDatabase
import com.squareup.picasso.Picasso
import java.io.File
import java.io.FileInputStream
import java.util.*
import kotlin.jvm.java

class GamePlayLibGDXActivity : AndroidApplication() {

    private val binding: ActivityPlayerbgaBinding by lazy {
        ActivityPlayerbgaBinding.inflate(LayoutInflater.from(this))
    }

    var i: Intent? = null
    var audio: AudioManager? = null

    var gamePlayError = false
    private val arrowsPosition2: ArrayList<Rect> = ArrayList()

    private var stepInfo: List<Int> = listOf(
        R.drawable.selector_down_left,
        R.drawable.selector_up_left,
        R.drawable.selector_center,
        R.drawable.selector_up_right,
        R.drawable.selector_down_right
    )
    private var arrows: ArrayList<Button> = ArrayList()

    var nchar = 0
    var inputs = byteArrayOf(0, 0, 0, 0, 0, 0, 0, 0, 0, 0)
    private val displayMetrics = DisplayMetrics()

    private var game: StepDroidGame? = null

    @SuppressLint("ClickableViewAccessibility")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Remove title bar
        // requestWindowFeature(Window.FEATURE_NO_TITLE) // handled by theme or AndroidApplication

        // Initialize LibGDX Game
        if (intent.extras?.containsKey("ssc") == true) {
            val rawSSC = intent.extras?.getString("ssc")
            val path = intent.extras?.getString("path")
            val idx = intent.extras?.getInt("nchar") ?: 0
            nchar = idx
            if (rawSSC != null && path != null) {
                initializeGame(rawSSC, path, idx)
            }
        } else {
            // Random Game Mode
            runBlocking {
                val db = SDDatabase.getDatabase(applicationContext, GlobalScope)
                val songs =
                    db.songsDao().getRandomSongs(null, null, null, null, null, null, null, 1)
                val song = songs.firstOrNull()

                if (song != null) {
                    val levels = db.levelDao().getSingleLevelsBySongIdSync(song.song_id)
                    val level = if (levels.isNotEmpty()) levels.random() else null

                    if (level != null) {
                        nchar = level.index
                        val bannerPath = song.PATH_SONG + File.separator + song.BANNER_SONG

                        // Inject into intent for other components that might read it
                        intent.putExtra("pathDisc", bannerPath)

                        initializeGame(song.PATH_File, song.PATH_SONG, nchar)

                        Toast.makeText(
                            applicationContext,
                            "Random Song: ${song.TITLE}",
                            Toast.LENGTH_LONG
                        ).show()
                    } else {
                        Toast.makeText(
                            applicationContext,
                            "No single levels for random song",
                            Toast.LENGTH_SHORT
                        ).show()
                        finish()
                    }
                } else {
                    Toast.makeText(applicationContext, "No songs found", Toast.LENGTH_SHORT).show()
                    finish()
                }
            }
        }

        setContentView(binding.root)

        // Replace GamePlayNew with LibGDX View
        val config = AndroidApplicationConfiguration()
        config.useAccelerometer = false
        config.useCompass = false
        config.r = 8
        config.g = 8
        config.b = 8
        config.a = 8

        if (game != null) {
            val gameView = initializeForView(game, config)

            if (gameView is GLSurfaceView) {
                gameView.holder.setFormat(PixelFormat.TRANSLUCENT)
                gameView.setZOrderMediaOverlay(true)
            }

            gameView.id = R.id.gamePlay

            // Add gameView to the layout, replacing binding.gamePlay
            val layoutParams = binding.gamePlay.layoutParams
            val parent = binding.gamePlay.parent as ViewGroup
            val index = parent.indexOfChild(binding.gamePlay)
            parent.removeView(binding.gamePlay)
            parent.addView(gameView, index, layoutParams)

            // Setup videoView dimensions based on game area aspect ratio
            setupVideoViewDimensions()
        }

        // EdgeToEdge
        EdgeToEdgeHelper.setupGameEdgeToEdge(this)

        window.setFlags(
            WindowManager.LayoutParams.FLAG_HARDWARE_ACCELERATED,
            WindowManager.LayoutParams.FLAG_HARDWARE_ACCELERATED
        )

        audio = getSystemService(Context.AUDIO_SERVICE) as AudioManager
        i = Intent(this, EvaluationActivity::class.java)
        val sharedPref = this.getSharedPreferences(
            getString(R.string.singleArrowsPos), Context.MODE_PRIVATE
        )
        val pathImg = intent.extras!!.getString("pathDisc", null)
        if (binding.bgPad != null)
            if (pathImg != null) Picasso.get().load(File(pathImg)).into(binding.bgPad)

        binding.videoViewBGA.setOnPreparedListener { mp: MediaPlayer ->
            mp.isLooping = true
            mp.setVolume(0f, 0f)
        }

        val gson = Gson()
        val saveGson = sharedPref.getString(getString(R.string.singleArrowsPos), "")
        if (saveGson != "") {
            val obj: ArrowsPositionPlace = gson.fromJson(saveGson, ArrowsPositionPlace::class.java)
            drawArrows(obj)
        }

        startGamePlay()
    }

    private fun initializeGame(rawSSC: String, path: String, levelIndex: Int) {
        try {
            val s = convertStreamToString(FileInputStream(rawSSC))
            val step = FileSSC(Objects.requireNonNull(s).toString(), levelIndex).parseData(false)
            step.path = path

            game = StepDroidGame(step)

            // Set up metadata for UI if needed
            Evaluator.songName = step.songMetadata["TITLE"].toString()
            val bgPad =
                BitmapFactory.decodeFile(step.path + File.separator + step.songMetadata["BACKGROUND"])
            if (bgPad != null) {
                Evaluator.imagePath = step.path + File.separator + step.songMetadata["BACKGROUND"]
                Evaluator.bitmap = TransformBitmap.doBrightness(bgPad, -60)
                binding.bgPad?.setImageBitmap(TransformBitmap.myblur(bgPad, this)?.let {
                    doBrightness(it, -125)
                })
            }

        } catch (e: Exception) {
            e.printStackTrace()
            gamePlayError = true
            Toast.makeText(this, "Error loading game: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }

    override fun onStart() {
        super.onStart()
        binding.openGLSpriteView?.visibility = View.GONE
        binding.bgPad?.visibility = View.VISIBLE
        binding.videoViewBGA?.visibility = View.VISIBLE
    }

    private fun setupVideoViewDimensions() {
        // Apply aspect ratio to videoView similar to GamePlayNew.setupVideoView()
        // This ensures the video matches the game area proportions
        binding.videoViewBGA?.post {
            binding.videoViewBGA?.layoutParams?.let { params ->
                val screenWidth = displayMetrics.widthPixels
                val screenHeight = displayMetrics.heightPixels
                val isLandscape = screenWidth > screenHeight

                if (isLandscape) {
                    // Landscape: 16:9 aspect ratio
                    val gameAreaHeight = screenHeight
                    val gameAreaWidth = (screenHeight * 1.77777778f).toInt()

                    if (gameAreaWidth <= screenWidth) {
                        params.width = gameAreaWidth
                        params.height = gameAreaHeight
                    } else {
                        params.width = screenWidth
                        params.height = (screenWidth / 1.77777778f).toInt()
                    }
                } else {
                    // Portrait: 4:3 aspect ratio (from GameConstants.ASPECT_RATIO_4_3)
                    params.width = screenWidth
                    params.height = (screenWidth * 0.75).toInt()
                }

                binding.videoViewBGA?.layoutParams = params
            }
        }
    }

    private fun startGamePlay() {
        binding.videoViewBGA!!.setOnErrorListener { _: MediaPlayer?, _: Int, _: Int ->
            val path2 = "android.resource://" + packageName + "/" + R.raw.bgaoff
            binding.videoViewBGA!!.setVideoPath(path2)
            binding.videoViewBGA!!.start()
            true
        }

        if (gamePlayError) {
            finish()
        }

        // Start video if available? 
        // Logic for starting video was inside GamePlayNew.startGamePlay but mostly it just prepares media player.
        // Here we might need to manually start video if BGA is set? 
        // The original code seemed to rely on binding.videoViewBGA being controlled via GamePlayNew or external.
        // binding.videoViewBGA setup is here.
    }

    // ... Copy inputs handling ...

    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        if (keyCode == KeyEvent.KEYCODE_BACK) {
            // game.stop() // if needed
            super.onBackPressed()
        }

        when (keyCode) {
            KeyEvent.KEYCODE_BUTTON_1 -> inputs[7] = 1
            KeyEvent.KEYCODE_BUTTON_2 -> inputs[9] = 1
            KeyEvent.KEYCODE_BUTTON_3 -> inputs[6] = 1
            KeyEvent.KEYCODE_BUTTON_4 -> inputs[8] = 1
            KeyEvent.KEYCODE_BUTTON_5 -> inputs[0] = 1
            KeyEvent.KEYCODE_BUTTON_6 -> inputs[2] = 1
            KeyEvent.KEYCODE_BUTTON_7 -> inputs[3] = 1
            KeyEvent.KEYCODE_BUTTON_8 -> inputs[1] = 1
            KeyEvent.KEYCODE_BUTTON_9 -> inputs[4] = 1
            KeyEvent.KEYCODE_BUTTON_10 -> inputs[5] = 1
            145, 288 -> inputs[5] = 1
            157, 293 -> inputs[6] = 1
            149, 295 -> inputs[7] = 1
            153 -> inputs[8] = 1
            147 -> inputs[9] = 1
            KeyEvent.KEYCODE_Z, 290 -> inputs[0] = 1
            KeyEvent.KEYCODE_Q, 296 -> inputs[1] = 1
            KeyEvent.KEYCODE_S, 292 -> inputs[2] = 1
            KeyEvent.KEYCODE_E, KeyEvent.KEYCODE_DPAD_DOWN_LEFT, KeyEvent.KEYCODE_SYSTEM_NAVIGATION_LEFT -> inputs[3] =
                1

            KeyEvent.KEYCODE_C, KeyEvent.KEYCODE_DPAD_DOWN -> {
                inputs[4] = 1
            }

            KeyEvent.KEYCODE_SYSTEM_NAVIGATION_DOWN -> startEvaluation()
            KeyEvent.KEYCODE_F8 -> ParamsSong.autoPlay = !ParamsSong.autoPlay
            KeyEvent.KEYCODE_VOLUME_UP -> {
                audio!!.adjustStreamVolume(
                    AudioManager.STREAM_MUSIC,
                    AudioManager.ADJUST_RAISE, AudioManager.FLAG_SHOW_UI
                )
                return true
            }

            KeyEvent.KEYCODE_VOLUME_DOWN -> {
                audio!!.adjustStreamVolume(
                    AudioManager.STREAM_MUSIC,
                    AudioManager.ADJUST_LOWER, AudioManager.FLAG_SHOW_UI
                )
                return true
            }

            else -> {
            }
        }

        game?.updateInputs(inputs)
        return super.onKeyDown(keyCode, event)
    }

    override fun onKeyUp(keyCode: Int, event: KeyEvent?): Boolean {
        when (keyCode) {
            KeyEvent.KEYCODE_BUTTON_1 -> inputs[7] = 0
            else -> {
            }
        }
        game?.updateInputs(inputs)
        return true
    }

    private fun drawArrows(data: ArrowsPositionPlace) {
        val pixel = TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_DIP,
            data.size.toFloat(), resources.displayMetrics
        ).toInt()

        val rootPad = binding.rootPad
        rootPad.post {
            stepInfo.forEachIndexed { index, x ->
                val iv = Button(this)
                iv.background = Drawable.createFromXml(resources, resources.getXml(x))
                arrows.add(iv)

                val adjustedX = data.positions[index].x.toFloat()
                val adjustedY = data.positions[index].y.toFloat()

                iv.x = adjustedX
                iv.y = adjustedY
                rootPad.addView(iv)
                val lp = RelativeLayout.LayoutParams(pixel, pixel)
                iv.layoutParams = lp

                val touchMargin = 75
                arrowsPosition2.add(
                    Rect(
                        (adjustedX - touchMargin).toInt(),
                        (adjustedY - touchMargin).toInt(),
                        (adjustedX + pixel + touchMargin).toInt(),
                        (adjustedY + pixel + touchMargin).toInt()
                    )
                )
            }
        }
    }

    override fun dispatchTouchEvent(event: MotionEvent): Boolean {
        try {
            val maskedAction = event.actionMasked
            val fingers = event.pointerCount
            val inputsTouch = Array(fingers) {
                IntArray(2)
            }
            for (i in 0 until fingers) {
                inputsTouch[i][0] = event.getX(i).toInt()
                inputsTouch[i][1] = event.getY(i).toInt()
            }
            when (maskedAction) {
                MotionEvent.ACTION_POINTER_UP -> {
                    val actionIndex = event.getPointerId(event.actionIndex)
                    unPress(event.getX(actionIndex), event.getY(actionIndex))
                }

                MotionEvent.ACTION_DOWN -> {
                    checkInputs(inputsTouch, true)
                    checkInputs(inputsTouch, false)
                }

                MotionEvent.ACTION_UP -> if (fingers == 1) clearPad()
                else -> checkInputs(inputsTouch, false)
            }
        } catch (e: java.lang.Exception) {
            e.printStackTrace()
        }

        game?.updateInputs(inputs)

        return super.dispatchTouchEvent(event)
    }

    private fun clearPad() {
        for (j in inputs.indices) {
            inputs[j] = ARROW_UNPRESSED
        }
    }

    private fun checkInputs(positions: Array<IntArray>, isDownMove: Boolean) {
        arrows.forEachIndexed { index, arrow ->
            run {
                var wasPressed = false
                for (k in positions) {
                    val x = k[0]
                    val y = k[1]
                    if (arrowsPosition2[index].contains(x, y)) {
                        if (inputs[index] == ARROW_UNPRESSED || isDownMove && inputs[index] == ARROW_HOLD_PRESSED) {
                            inputs[index] = ARROW_PRESSED
                        }
                        wasPressed = true
                        break
                    }
                }
                if (!wasPressed) {
                    inputs[index] = ARROW_UNPRESSED
                }
            }
        }
        updateArrowsVisualState()
    }

    private fun updateArrowsVisualState() {
        arrows.forEachIndexed { index, arrow ->
            if (index < inputs.size) {
                arrow.isPressed = inputs[index] == ARROW_PRESSED
                arrow.isSelected = inputs[index] == ARROW_PRESSED
                arrow.refreshDrawableState()
            }
        }
    }

    private fun unPress(x: Float, y: Float) {
        for (j in arrows.indices) {
            if (arrowsPosition2[j].contains(x.toInt(), y.toInt())) {
                inputs[j] = 0
            }
        }
    }

    fun startEvaluation() {
        i!!.putExtra("perfect", Evaluator.PERFECT)
        i!!.putExtra("great", Evaluator.GREAT)
        i!!.putExtra("good", Evaluator.GOOD)
        i!!.putExtra("bad", Evaluator.BAD)
        i!!.putExtra("miss", Evaluator.MISS)
        i!!.putExtra("maxCombo", Evaluator.MAX_COMBO)
        i!!.putExtra("totalScore", Evaluator.getTotalScore())
        i!!.putExtra("rank", Evaluator.getRank())
        i!!.putExtra("songName", Evaluator.songName)
        i!!.putExtra("imagePath", Evaluator.imagePath)
        startActivity(i)
    }
}
