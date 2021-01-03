package com.kyagamy.step

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.graphics.BitmapFactory
import android.graphics.Point
import android.media.AudioManager
import android.media.MediaPlayer
import android.os.Build
import android.os.Bundle
import android.util.DisplayMetrics
import android.view.KeyEvent
import android.view.MotionEvent
import android.view.View
import android.widget.Toast
import androidx.annotation.RequiresApi
import com.kyagamy.step.common.Common.Companion.convertStreamToString
import com.kyagamy.step.common.step.CommonGame.ParamsSong
import com.kyagamy.step.common.step.CommonGame.TransformBitmap
import com.kyagamy.step.common.step.CommonGame.TransformBitmap.Companion.doBrightness
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


class PlayerBga : Activity() {
    var hilo: MainThreadNew? = null
    var i: Intent? = null
    var audio: AudioManager? = null
    var gamePlayError = false

    var nchar = 0
    var indexMsj = 0
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


        val pathImg = intent.extras!!.getString("pathDisc", null)
        if (bg_pad != null)
            if (pathImg != null) Picasso.get().load(File(pathImg)).into(bg_pad)
        videoViewBGA.setOnPreparedListener { mp: MediaPlayer ->
            mp.isLooping = true
            mp.setVolume(0f, 0f)
        }

        try {
            val inputsButton = arrayListOf(
                image_0,
                image_1,
                image_2,
                image_3,
                image_4
            )
            val size_pad = displayMetrics.widthPixels / 6
            inputsButton.forEachIndexed { index, input ->
                run {
                    input.setOnTouchListener { _, event ->
                        when (event.action) {
                            MotionEvent.ACTION_DOWN, MotionEvent.ACTION_MOVE -> {
                                if (inputs[index] != 2.toByte()) {
                                    inputs[index] = 1
                                    StepsDrawer.noteSkins[0].tapsEffect[index].play()
                                }
                            }
                            MotionEvent.ACTION_UP -> {
                                inputs[index] = 0
                            }
                        }

                        false
                    }
                }
            }
            //guideline xd
            guidelinecenter.setGuidelinePercent(0.5f)
            guidelineVer1.setGuidelinePercent(0.3333333333334f)
            guidelineVer3.setGuidelinePercent(0.6666666666667f)
            guidelinehor2.setGuidelinePercent(0.6666666666667f)
            guidelinehor3.setGuidelinePercent(0.8333333333334f)

        } catch (ex: java.lang.Exception) {

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
    val resolution: Point
        get() {
            val displayMetrics = DisplayMetrics()
            windowManager.defaultDisplay.getMetrics(displayMetrics)
            val height = displayMetrics.heightPixels
            val width = displayMetrics.widthPixels
            return Point(width, height)
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

                Evaluator.songName=step.songMetada["TITLE"].toString()
                val bgPad =
                    BitmapFactory.decodeFile(step.path + File.separator + step.songMetada["BACKGROUND"])
                if (bg_pad != null && bgPad != null) {
                    Evaluator.imagePath=step.path + File.separator + step.songMetada["BACKGROUND"]
                    Evaluator.bitmap = TransformBitmap.doBrightness(bgPad,-60)
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
            KeyEvent.KEYCODE_BUTTON_1 -> inputs[7] =1
            KeyEvent.KEYCODE_BUTTON_2 ->inputs[9] = 1
            KeyEvent.KEYCODE_BUTTON_3 ->inputs[6] = 1
            KeyEvent.KEYCODE_BUTTON_4 ->inputs[8] = 1
            KeyEvent.KEYCODE_BUTTON_5 ->inputs[0] = 1
            KeyEvent.KEYCODE_BUTTON_6 ->inputs[2] = 1
            KeyEvent.KEYCODE_BUTTON_7 ->inputs[3] = 1
            KeyEvent.KEYCODE_BUTTON_8 ->inputs[1] = 1
            KeyEvent.KEYCODE_BUTTON_9 ->inputs[4] = 1
            KeyEvent.KEYCODE_BUTTON_10 ->inputs[5] = 1
            145, 288 ->inputs[5] = 1
            157, 293 ->inputs[6] = 1
            149, 295 ->inputs[7] = 1
            153 ->inputs[8] = 1
            147 ->inputs[9] = 1
            KeyEvent.KEYCODE_Z, 290 ->inputs[0] = 1
            KeyEvent.KEYCODE_Q, 296 ->inputs[1] = 1
            KeyEvent.KEYCODE_S, 292 ->inputs[2] = 1
            KeyEvent.KEYCODE_E, KeyEvent.KEYCODE_DPAD_DOWN_LEFT, KeyEvent.KEYCODE_SYSTEM_NAVIGATION_LEFT ->inputs[
                3
            ] = 1
            KeyEvent.KEYCODE_C, KeyEvent.KEYCODE_DPAD_DOWN -> {
               inputs[4] = 1

               // startEvaluation()
            }
            KeyEvent.KEYCODE_SYSTEM_NAVIGATION_DOWN ->startEvaluation()
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


    override fun onKeyUp(keyCode: Int, event: KeyEvent?): Boolean {

        // Toast.makeText(getApplicationContext(),""+keyCode,Toast.LENGTH_LONG).show();
        when (keyCode) {
            KeyEvent.KEYCODE_BUTTON_1 ->inputs[7] = 0
            KeyEvent.KEYCODE_BUTTON_2 ->inputs[9] = 0
            KeyEvent.KEYCODE_BUTTON_3 ->inputs[6] = 0
            KeyEvent.KEYCODE_BUTTON_4 ->inputs[8] = 0
            KeyEvent.KEYCODE_BUTTON_5 ->inputs[0] = 0
            KeyEvent.KEYCODE_BUTTON_6 ->inputs[2] = 0
            KeyEvent.KEYCODE_BUTTON_7 ->inputs[3] = 0
            KeyEvent.KEYCODE_BUTTON_8 ->inputs[1] = 0
            KeyEvent.KEYCODE_BUTTON_9 ->inputs[4] = 0
            KeyEvent.KEYCODE_BUTTON_10 ->inputs[5] = 0
            145 ->inputs[5] = 0
            157 ->inputs[6] = 0
            149 ->inputs[7] = 0
            153 ->inputs[8] = 0
            147 ->inputs[9] = 0
            KeyEvent.KEYCODE_Z ->inputs[0] = 0
            KeyEvent.KEYCODE_Q ->inputs[1] = 0
            KeyEvent.KEYCODE_S ->inputs[2] = 0
            KeyEvent.KEYCODE_E ->inputs[3] = 0
            KeyEvent.KEYCODE_C ->inputs[4] = 0
            else -> {
            }
        }
        return true
    }
    
    
    fun startEvaluation() {
        startActivity(i)
    }
}