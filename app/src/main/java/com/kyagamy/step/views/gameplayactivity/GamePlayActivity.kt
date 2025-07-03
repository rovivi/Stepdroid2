package com.kyagamy.step.views.gameplayactivity

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.graphics.BitmapFactory
import android.graphics.Point
import android.graphics.Rect
import android.graphics.drawable.Drawable
import android.media.AudioManager
import android.media.MediaPlayer
import android.os.Bundle
import android.util.DisplayMetrics
import android.util.TypedValue
import android.view.KeyEvent
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.widget.Button
import android.widget.RelativeLayout
import android.widget.Toast
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
import com.kyagamy.step.game.newplayer.MainThreadNew
import com.kyagamy.step.game.newplayer.StepsDrawer
import com.squareup.picasso.Picasso
import java.io.File
import java.io.FileInputStream
import java.util.*
import kotlin.collections.ArrayList
import com.kyagamy.step.databinding.ActivityPlayerbgaBinding


class GamePlayActivity : Activity() {
    //private lateinit var binding :ActivityPlayerbgaBinding

    private val binding: ActivityPlayerbgaBinding by lazy {
        ActivityPlayerbgaBinding.inflate(LayoutInflater.from(this))
    }

    var hilo: MainThreadNew? = null
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
        setContentView(binding.root)
        audio = getSystemService(Context.AUDIO_SERVICE) as AudioManager
        nchar = Objects.requireNonNull(intent.extras)!!.getInt("nchar")
        hilo = this.binding.gamePlay?.mainTread
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
    }

    override fun onStart() {
        super.onStart()
        // Configurar inmersión completa considerando las barras de navegación
        window.decorView.systemUiVisibility = (View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                or View.SYSTEM_UI_FLAG_FULLSCREEN
                or View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY)

        binding.gamePlay!!.systemUiVisibility = (View.SYSTEM_UI_FLAG_LOW_PROFILE
                or View.SYSTEM_UI_FLAG_FULLSCREEN
                or View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                or View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION)
        //set height  to bga
        startGamePlay()
    }

    private fun startGamePlay() {
        try {
            // gamePlay!!.top = 0
            val rawSSC =
                Objects.requireNonNull(intent.extras)?.getString("ssc")
            val path = intent.extras!!.getString("path")
            val s = convertStreamToString(
                FileInputStream(
                    Objects.requireNonNull(rawSSC)
                )
            )
            try {
                val step = FileSSC(Objects.requireNonNull(s).toString(), nchar).parseData(
                    false
                )
                step.path = Objects.requireNonNull(path).toString()
                //                gpo.build1Object(getBaseContext(), new SSC(z, false), nchar, path, this, pad, Common.WIDTH, Common.HEIGHT);
                windowManager.defaultDisplay.getRealMetrics(displayMetrics)
                binding.gamePlay!!.build1Object(
                    binding.videoViewBGA,
                    step,
                    baseContext,
                    Point(displayMetrics.widthPixels, displayMetrics.heightPixels),
                    this,
                    inputs
                )

                Evaluator.songName = step.songMetadata["TITLE"].toString()
                val bgPad =
                    BitmapFactory.decodeFile(step.path + File.separator + step.songMetadata["BACKGROUND"])
                if (bgPad != null && bgPad != null) {
                    Evaluator.imagePath = step.path + File.separator + step.songMetadata["BACKGROUND"]
                    Evaluator.bitmap = TransformBitmap.doBrightness(bgPad, -60)
                    binding.bgPad?.setImageBitmap(TransformBitmap.myblur(bgPad, this)?.let {
                        doBrightness(
                            it, -125
                        )
                    })
                }
            } catch (e: Exception) {
                e.printStackTrace()
                gamePlayError = true
                Toast.makeText(this, e.message, Toast.LENGTH_LONG).show()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        binding.videoViewBGA!!.setOnErrorListener { _: MediaPlayer?, _: Int, _: Int ->
            val path2 = "android.resource://" + packageName + "/" + R.raw.bgaoff
            binding.videoViewBGA!!.setVideoPath(path2)
            binding.videoViewBGA!!.start()
            true
        }
        if (!gamePlayError && binding.gamePlay != null) binding.gamePlay!!.startGame() else finish()
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        if (keyCode == KeyEvent.KEYCODE_BACK) {
            if (!gamePlayError && binding.gamePlay != null) {
                binding.gamePlay.stop()
            }
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
            KeyEvent.KEYCODE_E, KeyEvent.KEYCODE_DPAD_DOWN_LEFT, KeyEvent.KEYCODE_SYSTEM_NAVIGATION_LEFT -> inputs[
                    3
            ] = 1
            KeyEvent.KEYCODE_C, KeyEvent.KEYCODE_DPAD_DOWN -> {
                inputs[4] = 1
                // startEvaluation()
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
        return super.onKeyDown(keyCode, event)
    }

    private fun drawArrows(data: ArrowsPositionPlace) {
        val pixel = TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_DIP,
            data.size.toFloat(), resources.displayMetrics
        ).toInt()

        // Obtener las dimensiones reales de la pantalla
        val displayMetrics = DisplayMetrics()
        windowManager.defaultDisplay.getRealMetrics(displayMetrics)
        val screenWidth = displayMetrics.widthPixels
        val screenHeight = displayMetrics.heightPixels

        // Calcular el factor de escala basado en las dimensiones de DragStepActivity
        // DragStepActivity usa el tamaño del RelativeLayout, que puede ser diferente
        val rootPad = binding.rootPad
        rootPad.post {
            val layoutWidth = rootPad.width
            val layoutHeight = rootPad.height

            stepInfo.forEachIndexed { index, x ->
                val iv = Button(this)
                iv.background = Drawable.createFromXml(resources, resources.getXml(x))
                arrows.add(iv)

                // Ajustar las posiciones considerando las diferencias de tamaño
                val adjustedX = data.positions[index].x.toFloat()
                val adjustedY = data.positions[index].y.toFloat()

                iv.x = adjustedX
                iv.y = adjustedY
                rootPad.addView(iv)
                val lp = RelativeLayout.LayoutParams(pixel, pixel)
                iv.layoutParams = lp

                // Crear el área de toque con un margen más generoso
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

    override fun onKeyUp(keyCode: Int, event: KeyEvent?): Boolean {

        when (keyCode) {
            KeyEvent.KEYCODE_BUTTON_1 -> inputs[7] = 0
            else -> {
            }
        }
        return true
    }
    //Evaluation methods
//Controles


    override fun dispatchTouchEvent(event: MotionEvent): Boolean {
        val v = currentFocus

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
                        if (inputs[index] == ARROW_UNPRESSED || isDownMove && inputs[index] == ARROW_HOLD_PRESSED) { //by this way confirm if the curret pad is off
                            inputs[index] = ARROW_PRESSED
                            StepsDrawer.noteSkins[0].tapsEffect[index].play()
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
    }

    private fun unPress(x: Float, y: Float) {
        for (j in arrows.indices) { //checa cada felcha
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