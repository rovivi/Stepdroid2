package com.kyagamy.step

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.graphics.Point
import android.media.AudioManager
import android.media.MediaPlayer
import android.media.MediaPlayer.OnPreparedListener
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.util.DisplayMetrics
import android.view.MotionEvent
import android.view.View
import android.view.animation.AnimationUtils
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import android.widget.VideoView
import androidx.annotation.RequiresApi
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.constraintlayout.widget.Guideline
import com.kyagamy.step.common.Common.Companion.AnimateFactor
import com.kyagamy.step.common.Common.Companion.convertStreamToString
import com.kyagamy.step.common.step.Parsers.FileSSC
import com.kyagamy.step.game.newplayer.GamePlayNew
import com.kyagamy.step.game.newplayer.MainThreadNew
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


    @RequiresApi(api = Build.VERSION_CODES.KITKAT)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_playerbga)
        audio = getSystemService(Context.AUDIO_SERVICE) as AudioManager
        nchar = Objects.requireNonNull(intent.extras)!!.getInt("nchar")
        hilo = this.gamePlay?.mainTread


        val pathImg = intent.extras!!.getString("pathDisc", null)
        if (bg_pad!=null)
            if (pathImg != null) Picasso.get().load (File(pathImg)).into(bg_pad)
        videoViewBGA.setOnPreparedListener { mp: MediaPlayer ->
            mp.isLooping = true
            mp.setVolume(0f, 0f)
        }
    }

    fun startEvaluation(params: IntArray?) {
        i!!.putExtra("evaluation", params)
        i!!.putExtra("pathbg", "")
        i!!.putExtra("name", "Noame")
        i!!.putExtra("nchar", nchar)
        startActivity(i)
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
            gamePlay!!.top = 0
            val rawSSC =
                Objects.requireNonNull(intent.extras)?.getString("ssc")
            val path = intent.extras!!.getString("path")
            val s = convertStreamToString(
                FileInputStream(
                    Objects.requireNonNull(rawSSC)
                )
            )
            try {
                val step =
                    FileSSC(Objects.requireNonNull(s).toString(), nchar).parseData(
                        false
                    )
                step.path = Objects.requireNonNull(path).toString()
                //                gpo.build1Object(getBaseContext(), new SSC(z, false), nchar, path, this, pad, Common.WIDTH, Common.HEIGHT);

                val displayMetrics= DisplayMetrics()
                windowManager.defaultDisplay.getRealMetrics(displayMetrics)
                gamePlay!!.build1Object(videoViewBGA, step,baseContext,Point(displayMetrics.widthPixels,displayMetrics.heightPixels),inputs)
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
}