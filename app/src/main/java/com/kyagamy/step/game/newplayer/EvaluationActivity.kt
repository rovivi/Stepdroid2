package com.kyagamy.step.game.newplayer

import android.graphics.BitmapFactory
import android.graphics.Typeface
import android.media.AudioManager
import android.media.MediaPlayer
import android.media.SoundPool
import android.os.Build
import android.os.Bundle
import android.view.View
import android.view.Window
import android.view.WindowManager
import android.view.animation.AnimationUtils
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.kyagamy.step.R
import com.kyagamy.step.adapters.LevelAdapter
import com.kyagamy.step.common.step.CommonGame.TransformBitmap
import com.kyagamy.step.room.entities.Level
import com.squareup.picasso.Picasso
import kotlinx.android.synthetic.main.activity_evaluation.*
import kotlinx.android.synthetic.main.activity_evaluation2.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class EvaluationActivity : AppCompatActivity() {
    lateinit var mediaPlayer: MediaPlayer
    lateinit var soundPool: SoundPool
    private var soundPullTick: Int = 0
    private var soundPullExplotion: Int = 0

    private val timeToDisplayMs = 160L
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        this.requestWindowFeature(Window.FEATURE_NO_TITLE)
        window.setFlags(
            WindowManager.LayoutParams.FLAG_FULLSCREEN,
            WindowManager.LayoutParams.FLAG_FULLSCREEN
        )
        setContentView(R.layout.activity_evaluation2)
        //setSounds
        soundPool = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            SoundPool.Builder()
                .setMaxStreams(35)
                .build()
        } else {
            SoundPool(35, AudioManager.STREAM_MUSIC, 1)
        }

        soundPullTick = soundPool.load(applicationContext, R.raw.grade_tick, 1)


        //setFonts
        val customFont = Typeface.createFromAsset(applicationContext.assets, "fonts/karnivol.ttf")
        result_Perfect.typeface = customFont
        result_Great.typeface = customFont
        result_good.typeface = customFont
        result_bad.typeface = customFont
        result_miss.typeface = customFont
        result_max_combo.typeface = customFont
        result_total_score.typeface = customFont


        level_view_rank.layoutManager = LinearLayoutManager(
            this,
            LinearLayoutManager.HORIZONTAL,
            false
        )
//        levelRecycl
        //ADAPTERS
        val levelAdapter =LevelAdapter(this)
        var lvl =  Level(1,1, "22","me","pump-single","asdasd","name",1,null)

        var level = listOf(lvl)
        levelAdapter.setLevels(level )
        levelAdapter.bigMode=true
        level_view_rank.adapter= levelAdapter
        levelAdapter.notifyDataSetChanged()


        if (Evaluator.bitmap!=null){
            bg_title_grade.setImageBitmap(Evaluator.bitmap)

        }
        title_evaluation2.text = Evaluator.songName
        hideSystemUI()
    }

    override fun onStart() {
        super.onStart()
        hideSystemUI()
        //play loop
        mediaPlayer = MediaPlayer.create(this, R.raw.evaluation_loop)
        mediaPlayer.isLooping = true
        mediaPlayer.start()
        //animation

        img_perfect.visibility = View.INVISIBLE
        img_great.visibility = View.INVISIBLE
        img_good.visibility = View.INVISIBLE
        img_bad.visibility = View.INVISIBLE
        img_miss.visibility = View.INVISIBLE
        img_max_combo.visibility = View.INVISIBLE
        img_total_score.visibility = View.INVISIBLE


        val anim_down = AnimationUtils.loadAnimation(applicationContext, R.anim.slide_down)

        level_view_rank.startAnimation(AnimationUtils.loadAnimation(applicationContext,R.anim.translate_up))
        val anim_move = AnimationUtils.loadAnimation(this,R.anim.translate_down_static)
        title_evaluation2.startAnimation(AnimationUtils.loadAnimation(this,R.anim.zoom_in))
        val animDuration = 70L
        anim_down.duration = animDuration

        lifecycleScope.launch {
                 img_dance_grade.startAnimation(anim_move)

            img_perfect.startAnimation(
                AnimationUtils.loadAnimation(
                    applicationContext,
                    R.anim.slide_down
                )
            )
            delay(animDuration)
            img_great.startAnimation(
                AnimationUtils.loadAnimation(
                    applicationContext,
                    R.anim.slide_down
                )
            )
            delay(animDuration)
            img_good.startAnimation(
                AnimationUtils.loadAnimation(
                    applicationContext,
                    R.anim.slide_down
                )
            )
            delay(animDuration)
            img_bad.startAnimation(
                AnimationUtils.loadAnimation(
                    applicationContext,
                    R.anim.slide_down
                )
            )
            delay(animDuration)
            img_miss.startAnimation(
                AnimationUtils.loadAnimation(
                    applicationContext,
                    R.anim.slide_down
                )
            )
            delay(animDuration)
            img_max_combo.startAnimation(
                AnimationUtils.loadAnimation(
                    applicationContext,
                    R.anim.slide_down
                )
            )
            delay(animDuration)
            img_total_score.startAnimation(
                AnimationUtils.loadAnimation(
                    applicationContext,
                    R.anim.slide_down
                )
            )
            delay(animDuration)

            animateTextView(result_Perfect, Evaluator.PERFECT.toString())
            delay(timeToDisplayMs)
            animateTextView(result_Great, Evaluator.GREAT.toString())
            delay(timeToDisplayMs)
            animateTextView(result_good, Evaluator.GOOD.toString())
            delay(timeToDisplayMs)
            animateTextView(result_bad, Evaluator.BAD.toString())
            delay(timeToDisplayMs)
            animateTextView(result_miss, Evaluator.MISS.toString())
            delay(timeToDisplayMs)
            animateTextView(result_max_combo, Evaluator.MAX_COMBO.toString())
            delay(timeToDisplayMs)
            animateTextView(
                result_total_score,
                String.format("%.3f", Evaluator.getTotalScore()) + "%"
            )
            delay(timeToDisplayMs * 2)
            showGrade()
        }
    }

    suspend fun animateTextView(tv: TextView, combo: String) {
        //primero calculamos el numero de digitos
        var strCombo = combo
        if (strCombo.length < 4 && !strCombo.contains(".")) {
            strCombo = (strCombo.toInt() + 1000).toString().substring(1)
        }
        strCombo = strCombo.reversed()
        lifecycleScope.launch {
            var str = ""
            val timeMs = 70 / strCombo.length
            for (element in strCombo) {
                if (element != '.') {
                    for (x in 0..6) {
                        tv.text = ((0..9).random().toString() + str)
                        delay(timeMs.toLong())
                    }
                }
                soundPool.play(soundPullTick, 1f, 1f, 1, 0, 1.002f)
                str = element + str
            }
            tv.text = strCombo.reversed()
        }
    }

    override fun onStop() {
        super.onStop()
        try {
            mediaPlayer.release()
            mediaPlayer.stop()
        } catch (ex: Exception) {
        }
    }

    override fun onResume() {
        super.onResume()
        hideSystemUI()
    }
    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        hideSystemUI()
    }

    fun showGrade() {
        val mediaPlayer: MediaPlayer
        val mediaPlayerbg: MediaPlayer
        var res = -1
        var res2 = -1
        //
        val letterGradeBitmap = BitmapFactory.decodeResource(resources, R.drawable.letters)
        val lettersArray =
            TransformBitmap.customSpriteArray(letterGradeBitmap, 1, 8, 0, 1, 2, 3, 4, 5, 6, 7)
        when (Evaluator.getRank()) {
            "SSS" -> {
                res = R.raw.rank_0
                res2 = R.raw.rank_0_b
                img_rank_grade.setImageBitmap(lettersArray[0])
            }
            "SS" -> {
                res = R.raw.rank_1
                res2 = R.raw.rank_1_b
                img_rank_grade.setImageBitmap(lettersArray[1])
            }
            "S" -> {
                res = R.raw.rank_2
                res2 = R.raw.rank_2_b
                img_rank_grade.setImageBitmap(lettersArray[2])
            }
            "A" -> {
                res = R.raw.rank_3
                res2 = R.raw.rank_3_b
                img_rank_grade.setImageBitmap(lettersArray[3])
            }
            "B" -> {
                res = R.raw.rank_4
                res2 = R.raw.rank_4_b
                img_rank_grade.setImageBitmap(lettersArray[4])
            }
            "C" -> {
                res = R.raw.rank_5
                res2 = R.raw.rank_5_b
                img_rank_grade.setImageBitmap(lettersArray[5])
            }
            "D" -> {
                res = R.raw.rank_6
                res2 = R.raw.rank_6_b
                img_rank_grade.setImageBitmap(lettersArray[6])
            }
            "F" -> {
                res = R.raw.rank_7
                res2 = R.raw.rank_7_b
                img_rank_grade.setImageBitmap(lettersArray[7])
            }
        }
        img_rank_grade.startAnimation(
            AnimationUtils.loadAnimation(
                baseContext,
                R.anim.zoom_letter
            )
        )
        val mediaPlayerExplotion = MediaPlayer.create(this, R.raw.rank_explotion)
        mediaPlayer = MediaPlayer.create(this, res)
        mediaPlayerbg = MediaPlayer.create(this, res2)
        mediaPlayerExplotion.start()
        mediaPlayer.start()
        mediaPlayerbg.start()
    }

    private fun hideSystemUI() {
        // Enables regular immersive mode.
        // For "lean back" mode, remove SYSTEM_UI_FLAG_IMMERSIVE.
        // Or for "sticky immersive," replace it with SYSTEM_UI_FLAG_IMMERSIVE_STICKY
        window.decorView.systemUiVisibility = (View.SYSTEM_UI_FLAG_IMMERSIVE
                // Set the content to appear under the system bars so that the
                // content doesn't resize when the system bars hide and show.
                or View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                // Hide the nav bar and status bar
                or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                or View.SYSTEM_UI_FLAG_FULLSCREEN)
    }
}