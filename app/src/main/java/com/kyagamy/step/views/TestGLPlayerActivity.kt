package com.kyagamy.step.views

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.graphics.BitmapFactory
import android.graphics.Point
import android.graphics.Rect
import android.graphics.drawable.Drawable
import android.media.AudioManager
import android.media.MediaPlayer
import android.net.Uri
import android.os.Bundle
import android.util.DisplayMetrics
import android.util.TypedValue
import android.view.KeyEvent
import android.view.MotionEvent
import android.widget.Button
import android.widget.RelativeLayout
import android.widget.Toast
import android.opengl.GLSurfaceView
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
import com.kyagamy.step.ui.EvaluationActivity
import com.kyagamy.step.game.newplayer.Evaluator
import com.kyagamy.step.databinding.ActivityTestGlplayerBinding
import com.kyagamy.step.game.opengl.GamePlayGLRenderer
import com.kyagamy.step.engine.ISpriteRenderer
import com.kyagamy.step.game.newplayer.Combo
import com.kyagamy.step.utils.EdgeToEdgeHelper
import com.squareup.picasso.Picasso
import game.StepObject
import java.io.File
import java.util.*

class TestGLPlayerActivity : FullScreenActivity() {
    private lateinit var binding: ActivityTestGlplayerBinding
    private var renderer: GamePlayGLRenderer? = null
    private val handler = android.os.Handler(android.os.Looper.getMainLooper())
    private var updateUIRunnable: Runnable? = null

    // GamePlay variables
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

    @SuppressLint("ClickableViewAccessibility")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        android.util.Log.d("TestGLPlayerActivity", "=== onCreate ===")
        // Remove title bar completely
        supportActionBar?.hide()

        binding = ActivityTestGlplayerBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Use EdgeToEdgeHelper for game-optimized edge-to-edge
        EdgeToEdgeHelper.setupGameEdgeToEdge(this)

        // Initialize GamePlay components
        audio = getSystemService(Context.AUDIO_SERVICE) as AudioManager
        nchar = Objects.requireNonNull(intent.extras)!!.getInt("nchar")
        i = Intent(this, EvaluationActivity::class.java)

        // Load arrow positions from SharedPreferences
        val sharedPref = this.getSharedPreferences(
            getString(R.string.singleArrowsPos), Context.MODE_PRIVATE
        )
        val pathImg = intent.extras!!.getString("pathDisc", null)
        if (pathImg != null) {
            // You might need to add background pad to the layout if needed
            // Picasso.get().load(File(pathImg)).into(binding.bgPad)
        }

        val gson = Gson()
        val saveGson = sharedPref.getString(getString(R.string.singleArrowsPos), "")
        if (saveGson != "") {
            val obj: ArrowsPositionPlace = gson.fromJson(saveGson, ArrowsPositionPlace::class.java)
            drawArrows(obj)
        } else {
            // Si no hay configuración guardada, crear una configuración por defecto
            drawDefaultArrows()
        }

        // Configurar video de fondo BGA
        setupBgaVideo()

        // Preparar datos de la canción igual que en GamePlayActivity
        val rawSSC = intent.extras?.getString("ssc")
        val path = intent.extras?.getString("path")
        val nchar = intent.extras?.getInt("nchar") ?: 0
        android.util.Log.d(
            "TestGLPlayerActivity",
            "Song data: ssc=$rawSSC, path=$path, nchar=$nchar"
        )

        val step: StepObject? = try {
            val s = convertStreamToString(java.io.FileInputStream(rawSSC))
            FileSSC(s.toString(), nchar).parseData(false).apply {
                this.path = path ?: ""

                // Setup evaluator data like in GamePlayActivity
                Evaluator.songName = this.songMetadata["TITLE"].toString()
                val bgPad =
                    BitmapFactory.decodeFile(this.path + File.separator + this.songMetadata["BACKGROUND"])
                if (bgPad != null) {
                    Evaluator.imagePath =
                        this.path + File.separator + this.songMetadata["BACKGROUND"]
                    Evaluator.bitmap = TransformBitmap.doBrightness(bgPad, -60)
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
            android.util.Log.e("TestGLPlayerActivity", "Error parsing step data", e)
            gamePlayError = true
            Toast.makeText(this, e.message, Toast.LENGTH_LONG).show()
            null
        }

        val metrics = DisplayMetrics()
        windowManager.defaultDisplay.getRealMetrics(metrics)
        android.util.Log.d(
            "TestGLPlayerActivity",
            "Screen size: ${metrics.widthPixels}x${metrics.heightPixels}"
        )

        if (step != null && !gamePlayError) {
            android.util.Log.d("TestGLPlayerActivity", "Creating GamePlayGLRenderer...")
            renderer = GamePlayGLRenderer(
                this,
                step,
                binding.bgaVideoView,
                Point(metrics.widthPixels, metrics.heightPixels),
                inputs
            )

            // Set up game completion callback
            renderer?.setGameCompletionCallback(object : GamePlayGLRenderer.GameCompletionCallback {
                override fun onGameCompleted() {
                    android.util.Log.d(
                        "TestGLPlayerActivity",
                        "Game completed, starting evaluation..."
                    )
                    runOnUiThread {
                        startEvaluation()
                        finish()
                    }
                }
            })

            binding.openGLView.setRenderer(renderer as ISpriteRenderer)
            binding.openGLView.renderMode = GLSurfaceView.RENDERMODE_CONTINUOUSLY
            android.util.Log.d("TestGLPlayerActivity", "Renderer set up complete")
        } else {
            android.util.Log.e(
                "TestGLPlayerActivity",
                "Step data is null or error occurred, cannot create renderer"
            )
            if (gamePlayError) {
                finish()
            }
        }

        setupUIUpdater()
    }

    override fun onResume() {
        android.util.Log.d("TestGLPlayerActivity", "=== onResume called ===")
        super.onResume()
        binding.openGLView.onResume()
        // Start UI updater
        updateUIRunnable?.let { handler.post(it) }
        // Delay start to ensure surface is ready
        android.util.Log.d("TestGLPlayerActivity", "Posting delayed renderer start...")
        android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
            android.util.Log.d(
                "TestGLPlayerActivity",
                "Delayed start executing, calling renderer.start()"
            )
            renderer?.start()
        }, 100)
    }

    override fun onPause() {
        super.onPause()
        binding.openGLView.onPause()
        renderer?.stop()
        // Stop UI updater
        updateUIRunnable?.let { handler.removeCallbacks(it) }
    }

    private fun setupUIUpdater() {
        updateUIRunnable = object : Runnable {
            override fun run() {
                renderer?.let { r ->
                    val fps = r.getFPS()
                    val arrowCount = r.getVisibleArrowCount()
                    binding.fpsCounter.text = "FPS: ${"%.1f".format(fps)} | Arrows: $arrowCount"
                }
                handler.postDelayed(this, 100) // Update every 100ms
            }
        }
    }

    private fun setupBgaVideo() {
        val videoUri = Uri.parse("android.resource://$packageName/${R.raw.bgaoff}")
        binding.bgaVideoView.setVideoURI(videoUri)

        binding.bgaVideoView.setOnPreparedListener { mediaPlayer ->
            mediaPlayer.isLooping = true
            binding.bgaVideoView.start()
        }

        binding.bgaVideoView.setOnErrorListener { _, _, _ ->
            // En caso de error, reintentar
            binding.bgaVideoView.setVideoURI(videoUri)
            binding.bgaVideoView.start()
            true
        }

        // Prepare the video immediately
        binding.bgaVideoView.requestFocus()
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        // Handle back button
        if (keyCode == KeyEvent.KEYCODE_BACK) {
            renderer?.stop()
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

            KeyEvent.KEYCODE_C, KeyEvent.KEYCODE_DPAD_DOWN -> inputs[4] = 1
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
        }
        return super.onKeyDown(keyCode, event)
    }

    override fun onKeyUp(keyCode: Int, event: KeyEvent?): Boolean {
        when (keyCode) {
            KeyEvent.KEYCODE_BUTTON_1 -> inputs[7] = 0
            // Add other key up events as needed
        }
        return true
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

            // Only log important touch events
            if (maskedAction == MotionEvent.ACTION_DOWN || maskedAction == MotionEvent.ACTION_POINTER_DOWN) {
                android.util.Log.d(
                    "TestGLPlayerActivity",
                    "Touch DOWN at (${inputsTouch[0][0]}, ${inputsTouch[0][1]})"
                )
            }

            when (maskedAction) {
                MotionEvent.ACTION_DOWN -> {
                    checkInputs(inputsTouch, true)
                }
                MotionEvent.ACTION_MOVE -> {
                    checkInputs(inputsTouch, false)
                }
                MotionEvent.ACTION_POINTER_DOWN -> {
                    checkInputs(inputsTouch, true)
                }
                MotionEvent.ACTION_POINTER_UP -> {
                    val actionIndex = event.actionIndex
                    val pointerId = event.getPointerId(actionIndex)
                    unPress(event.getX(actionIndex), event.getY(actionIndex))
                }
                MotionEvent.ACTION_UP -> {
                    if (fingers == 1) {
                        clearPad()
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return super.dispatchTouchEvent(event)
    }

    private fun drawArrows(data: ArrowsPositionPlace) {
        android.util.Log.d("TestGLPlayerActivity", "drawArrows called with size=${data.size}")

        // Clear existing arrows and touch areas
        arrows.clear()
        arrowsPosition2.clear()
        binding.arrowsContainer.removeAllViews()

        val pixel = TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_DIP,
            data.size.toFloat(), resources.displayMetrics
        ).toInt()

        android.util.Log.d("TestGLPlayerActivity", "Arrow size in pixels: $pixel")

        // Usar el contenedor de flechas del layout
        val arrowsContainer = binding.arrowsContainer
        arrowsContainer.post {
            android.util.Log.d(
                "TestGLPlayerActivity",
                "Container dimensions: ${arrowsContainer.width}x${arrowsContainer.height}"
            )

            stepInfo.forEachIndexed { index, x ->
                val iv = Button(this)
                iv.background = Drawable.createFromXml(resources, resources.getXml(x))

                // Hacer el botón más visible para debug
                iv.alpha = 0.8f

                arrows.add(iv)

                // Usar las posiciones guardadas
                if (index < data.positions.size) {
                    val position = data.positions[index]
                    iv.x = position.x.toFloat()
                    iv.y = position.y.toFloat()

                    android.util.Log.d(
                        "TestGLPlayerActivity",
                        "Arrow $index positioned at (${position.x}, ${position.y})"
                    )

                    arrowsContainer.addView(iv)
                    val lp = RelativeLayout.LayoutParams(pixel, pixel)
                    iv.layoutParams = lp

                    // Crear área de toque con margen generoso
                    val touchMargin = 100  // Aumentar margen para facilitar toque
                    val touchArea = Rect(
                        (position.x - touchMargin),
                        (position.y - touchMargin),
                        (position.x + pixel + touchMargin),
                        (position.y + pixel + touchMargin)
                    )
                    arrowsPosition2.add(touchArea)

                    android.util.Log.d(
                        "TestGLPlayerActivity",
                        "Arrow $index touch area: $touchArea"
                    )
                } else {
                    android.util.Log.e("TestGLPlayerActivity", "No position data for arrow $index")
                }
            }

            android.util.Log.d(
                "TestGLPlayerActivity",
                "Created ${arrows.size} arrows with ${arrowsPosition2.size} touch areas"
            )
        }
    }

    private fun drawDefaultArrows() {
        // Obtener las dimensiones de la pantalla
        val metrics = DisplayMetrics()
        windowManager.defaultDisplay.getRealMetrics(metrics)
        val screenWidth = metrics.widthPixels
        val screenHeight = metrics.heightPixels

        // Crear configuración por defecto estilo PUMP
        val defaultArrows = ArrowsPositionPlace()
        defaultArrows.size = 100
        defaultArrows.heightOffset = 0.8f  // Cerca del fondo
        defaultArrows.horizontalOffset = 0.5f  // Centrado horizontalmente
        defaultArrows.spacingMultiplier = 1.0f
        defaultArrows.currentPreset = "PUMP"

        // Calcular posiciones para estilo PUMP
        val padSize = 100f
        val centerX = (screenWidth - padSize) / 2f
        val centerY = screenHeight * 0.8f  // 80% hacia abajo

        val offset = (padSize + (padSize * 1.0f)) / 2f
        val sqrt2over2 = 0.70710677f
        val dx = offset * sqrt2over2
        val dy = offset * sqrt2over2

        val positions = arrayOf(
            Point((centerX - dx).toInt(), (centerY + dy).toInt()), // DownLeft
            Point((centerX - dx).toInt(), (centerY - dy).toInt()), // UpLeft
            Point(centerX.toInt(), centerY.toInt()),                // Center
            Point((centerX + dx).toInt(), (centerY - dy).toInt()), // UpRight
            Point((centerX + dx).toInt(), (centerY + dy).toInt())  // DownRight
        )

        defaultArrows.positions = positions
        // Guardar configuración por defecto
        val gson = Gson()
        val json = gson.toJson(defaultArrows)
        val sharedPref = this.getSharedPreferences(
            getString(R.string.singleArrowsPos), Context.MODE_PRIVATE
        )
        sharedPref.edit().putString(getString(R.string.singleArrowsPos), json).apply()

        // Dibujar las flechas
        drawArrows(defaultArrows)
    }

    private fun clearPad() {
        for (j in inputs.indices) {
            inputs[j] = ARROW_UNPRESSED
        }
    }

    private fun checkInputs(positions: Array<IntArray>, isDownMove: Boolean) {
        arrows.forEachIndexed { index, arrow ->
            var wasPressed = false
            for (k in positions) {
                val x = k[0]
                val y = k[1]

                if (index < arrowsPosition2.size) {
                    val touchArea = arrowsPosition2[index]

                    if (touchArea.contains(x, y)) {
                        android.util.Log.d("TestGLPlayerActivity", " Arrow $index receptor touched")
                        if (inputs[index] == ARROW_UNPRESSED || isDownMove && inputs[index] == ARROW_HOLD_PRESSED) {
                            inputs[index] = ARROW_PRESSED
                            // Removed manual combo testing - combo should only increase when hitting falling notes
                        }
                        wasPressed = true
                        break
                    }
                }
            }
            if (!wasPressed && index < inputs.size) {
                if (inputs[index] != ARROW_UNPRESSED) {
                    android.util.Log.d("TestGLPlayerActivity", " Arrow $index released")
                }
                inputs[index] = ARROW_UNPRESSED
            }
        }
        updateArrowsVisualState()
    }

    private fun updateArrowsVisualState() {
        arrows.forEachIndexed { index, arrow ->
            if (index < inputs.size) {
                val isPressed = inputs[index] == ARROW_PRESSED
                arrow.isPressed = isPressed
                arrow.isSelected = isPressed

                // Cambiar el alpha para dar feedback visual más claro
                arrow.alpha = if (isPressed) 1.0f else 0.8f

                android.util.Log.d(
                    "TestGLPlayerActivity",
                    "Arrow $index visual state: pressed=$isPressed"
                )
                arrow.refreshDrawableState()
            }
        }
    }

    private fun unPress(x: Float, y: Float) {
        android.util.Log.d("TestGLPlayerActivity", "unPress called at ($x, $y)")
        for (j in arrows.indices) {
            if (j < arrowsPosition2.size && arrowsPosition2[j].contains(x.toInt(), y.toInt())) {
                android.util.Log.d("TestGLPlayerActivity", "Arrow $j unpressed")
                inputs[j] = ARROW_UNPRESSED
            }
        }
        updateArrowsVisualState()
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
