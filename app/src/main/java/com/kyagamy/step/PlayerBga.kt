package com.kyagamy.step

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
import android.os.Build
import android.os.Bundle
import android.util.DisplayMetrics
import android.util.TypedValue
import android.view.KeyEvent
import android.view.MotionEvent
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.Toast
import androidx.annotation.RequiresApi
import com.google.gson.Gson
import com.kyagamy.step.common.Common.Companion.convertStreamToString
import com.kyagamy.step.common.step.CommonGame.ArrowsPositionPlace
import com.kyagamy.step.common.step.CommonGame.ParamsSong
import com.kyagamy.step.common.step.CommonGame.TransformBitmap
import com.kyagamy.step.common.step.CommonGame.TransformBitmap.Companion.doBrightness
import com.kyagamy.step.common.step.CommonSteps.Companion.ARROW_HOLD_PRESSED
import com.kyagamy.step.common.step.CommonSteps.Companion.ARROW_PRESSED
import com.kyagamy.step.common.step.CommonSteps.Companion.ARROW_UNPRESSED
import com.kyagamy.step.common.step.Parsers.FileSSC
import com.kyagamy.step.game.newplayer.EvaluationActivity
import com.kyagamy.step.game.newplayer.Evaluator
import com.kyagamy.step.game.newplayer.MainThreadNew
import com.kyagamy.step.game.newplayer.StepsDrawer
import com.squareup.picasso.Picasso
import kotlinx.android.synthetic.main.activity_playerbga.*
import java.io.File
import java.io.FileInputStream
import java.util.*
import kotlin.collections.ArrayList


class PlayerBga : Activity() {
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
    @RequiresApi(api = Build.VERSION_CODES.KITKAT)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_playerbga)
        audio = getSystemService(Context.AUDIO_SERVICE) as AudioManager
        nchar = Objects.requireNonNull(intent.extras)!!.getInt("nchar")
        hilo = this.gamePlay?.mainTread
        i = Intent(this, EvaluationActivity::class.java)
        val sharedPref = this.getSharedPreferences(
            getString(R.string.singleArrowsPos), Context.MODE_PRIVATE
        )
        val pathImg = intent.extras!!.getString("pathDisc", null)
        if (bg_pad != null)
            if (pathImg != null) Picasso.get().load(File(pathImg)).into(bg_pad)
        videoViewBGA.setOnPreparedListener { mp: MediaPlayer ->
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

    @RequiresApi(api = Build.VERSION_CODES.KITKAT)
    override fun onStart() {
        super.onStart()
        gamePlay!!.systemUiVisibility = (View.SYSTEM_UI_FLAG_LOW_PROFILE
                or View.SYSTEM_UI_FLAG_FULLSCREEN
                or View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                or View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION)
        //set height  to bga
        startGamePlay()
    }

    @RequiresApi(api = Build.VERSION_CODES.KITKAT)
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
                gamePlay!!.build1Object(
                    videoViewBGA,
                    step,
                    baseContext,
                    Point(displayMetrics.widthPixels, displayMetrics.heightPixels),
                    this,
                    inputs
                )

                Evaluator.songName = step.songMetada["TITLE"].toString()
                val bgPad =
                    BitmapFactory.decodeFile(step.path + File.separator + step.songMetada["BACKGROUND"])
                if (bg_pad != null && bgPad != null) {
                    Evaluator.imagePath = step.path + File.separator + step.songMetada["BACKGROUND"]
                    Evaluator.bitmap = TransformBitmap.doBrightness(bgPad, -60)
                    bg_pad.setImageBitmap(TransformBitmap.myblur(bgPad, this)?.let {
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
        videoViewBGA!!.setOnErrorListener { _: MediaPlayer?, _: Int, _: Int ->
            val path2 = "android.resource://" + packageName + "/" + R.raw.bgaoff
            videoViewBGA!!.setVideoPath(path2)
            videoViewBGA!!.start()
            true
        }
        if (!gamePlayError && gamePlay != null) gamePlay!!.startGame() else finish()
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        if (keyCode == KeyEvent.KEYCODE_BACK) {
            if (!gamePlayError && gamePlay != null) {
                gamePlay.stop()
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
            KeyEvent.KEYCODE_F8 -> ParamsSong.autoplay = !ParamsSong.autoplay
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
        stepInfo.forEachIndexed { index, x ->
            val iv = Button(this)

            iv.background = Drawable.createFromXml(resources, resources.getXml(x))
            // iv.setImageDrawable(x)
            arrows.add(iv)

            iv.x = data.positions[index].x.toFloat()
            iv.y = data.positions[index].y.toFloat()

            rootPad.addView(iv)
            var lp = iv.layoutParams
            lp.height = pixel
            lp.width = pixel
            iv.layoutParams = lp
            arrowsPosition2.add(
                Rect(
                    iv.x.toInt(),
                    iv.y.toInt(),
                    iv.x.toInt() + pixel,
                    iv.y.toInt() + pixel
                )
            )
            //            iv.setOnTouchListener { _, event ->
//                when (event.action) {
//                    MotionEvent.ACTION_DOWN, MotionEvent.ACTION_MOVE -> {
//                        if (inputs[index] != 2.toByte()) {
//                            inputs[index] = 1
//                            StepsDrawer.noteSkins[0].tapsEffect[index].play()
//                        }
//                    }
//                    MotionEvent.ACTION_UP -> {
//                        inputs[index] = 0
//                    }
//                }
//                false
//            }
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
            //this.fingersOnScreen = (byte) fingers;
            val inputsTouch = Array(fingers) {
                IntArray(2)
            }
            for (i in 0 until fingers) {
                inputsTouch[i][0] = event.getX(i).toInt()
                inputsTouch[i][1] = event.getY(i).toInt()
                //                this.event += " " + i + ":(" + (int) event.getX(i) + "," + (int) event.getY(i) + ")";
            }
            when (maskedAction) {
                MotionEvent.ACTION_POINTER_UP -> {
                    val actionIndex = event.getPointerId(event.actionIndex)
                    unPress(event.getX(actionIndex), event.getY(actionIndex))
                }
                MotionEvent.ACTION_DOWN -> {
                    checkInputs(inputsTouch, true)
                    //   this.event = "numero" + maskedAction;
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

    override fun onTouchEvent(event: MotionEvent): Boolean {

        return true
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
                            // arrow.setImageState(intArrayOf(android.R.attr.state_checked),false)
//                            arrows[j].setImageDrawable(
//                                Drawable.createFromXml(
//                                    resources,
//                                    resources.getXml(stepInfo[0])
//                                )
//                            )
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
                //arrows[j].state(intArrayOf(android.R.attr.state_focused))}
                //arr

            }
        }
    }

    fun startEvaluation() {
        startActivity(i)
    }

    
}

