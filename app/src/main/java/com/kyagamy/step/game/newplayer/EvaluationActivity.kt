package com.kyagamy.step.game.newplayer

import android.graphics.BitmapFactory
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
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.kyagamy.step.views.FullScreenActivity
import com.kyagamy.step.R
import com.kyagamy.step.adapters.LevelAdapter
import com.kyagamy.step.common.step.CommonGame.TransformBitmap
import com.kyagamy.step.databinding.ActivityEvaluation2Binding
import com.kyagamy.step.room.entities.Level

import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class EvaluationActivity : FullScreenActivity() {
    private lateinit var binding: ActivityEvaluation2Binding
    
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


        //
        binding.exitButton.setOnClickListener { finish() }

        binding.levelViewRank.layoutManager = LinearLayoutManager(
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
        binding.levelViewRank.adapter= levelAdapter
        levelAdapter.notifyDataSetChanged()

    
        if (Evaluator.bitmap!=null){
            binding.bgTitleGrade.setImageBitmap(Evaluator.bitmap)

        }
        binding.titleEvaluation2.text = Evaluator.songName
    }

    override fun onStart() {
        super.onStart()
        //play loop
        mediaPlayer = MediaPlayer.create(this, R.raw.evaluation_loop)
        mediaPlayer.isLooping = true
        mediaPlayer.start()
        //animation

        binding.imgPerfect.visibility = View.INVISIBLE
        binding.imgGreat.visibility = View.INVISIBLE
        binding.imgGood.visibility = View.INVISIBLE
        binding.imgBad.visibility = View.INVISIBLE
        binding.imgMiss.visibility = View.INVISIBLE
        binding.imgMaxCombo.visibility = View.INVISIBLE
        binding.imgTotalScore.visibility = View.INVISIBLE


        val anim_down = AnimationUtils.loadAnimation(applicationContext, R.anim.slide_down)

        binding.levelViewRank.startAnimation(AnimationUtils.loadAnimation(applicationContext,R.anim.translate_up))
        val anim_move = AnimationUtils.loadAnimation(this,R.anim.translate_down_static)
        binding.titleEvaluation2.startAnimation(AnimationUtils.loadAnimation(this,R.anim.zoom_in))
        val animDuration = 70L
        anim_down.duration = animDuration

        lifecycleScope.launch {
            binding.imgDanceGrade.startAnimation(anim_move)

            binding.imgPerfect.startAnimation(
                AnimationUtils.loadAnimation(
                    applicationContext,
                    R.anim.slide_down
                )
            )
            delay(animDuration)
            binding.imgGreat.startAnimation(
                AnimationUtils.loadAnimation(
                    applicationContext,
                    R.anim.slide_down
                )
            )
            delay(animDuration)
            binding.imgGood.startAnimation(
                AnimationUtils.loadAnimation(
                    applicationContext,
                    R.anim.slide_down
                )
            )
            delay(animDuration)
            binding.imgBad.startAnimation(
                AnimationUtils.loadAnimation(
                    applicationContext,
                    R.anim.slide_down
                )
            )
            delay(animDuration)
            binding.imgMiss.startAnimation(
                AnimationUtils.loadAnimation(
                    applicationContext,
                    R.anim.slide_down
                )
            )
            delay(animDuration)
            binding.imgMaxCombo.startAnimation(
                AnimationUtils.loadAnimation(
                    applicationContext,
                    R.anim.slide_down
                )
            )
            delay(animDuration)
            binding.imgTotalScore.startAnimation(
                AnimationUtils.loadAnimation(
                    applicationContext,
                    R.anim.slide_down
                )
            )
            delay(animDuration)

            animateTextView(binding.resultPerfect, Evaluator.PERFECT.toString())
            delay(timeToDisplayMs)
            animateTextView(binding.resultGreat, Evaluator.GREAT.toString())
            delay(timeToDisplayMs)
            animateTextView(binding.resultGood, Evaluator.GOOD.toString())
            delay(timeToDisplayMs)
            animateTextView(binding.resultBad, Evaluator.BAD.toString())
            delay(timeToDisplayMs)
            animateTextView(binding.resultMiss, Evaluator.MISS.toString())
            delay(timeToDisplayMs)
            animateTextView(binding.resultMaxCombo, Evaluator.MAX_COMBO.toString())
            delay(timeToDisplayMs)
            animateTextView(
                binding.resultTotalScore,
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
                binding.imgRankGrade.setImageBitmap(lettersArray[0])
            }
            "SS" -> {
                res = R.raw.rank_1
                res2 = R.raw.rank_1_b
                binding.imgRankGrade.setImageBitmap(lettersArray[1])
            }
            "S" -> {
                res = R.raw.rank_2
                res2 = R.raw.rank_2_b
                binding.imgRankGrade.setImageBitmap(lettersArray[2])
            }
            "A" -> {
                res = R.raw.rank_3
                res2 = R.raw.rank_3_b
                binding.imgRankGrade.setImageBitmap(lettersArray[3])
            }
            "B" -> {
                res = R.raw.rank_4
                res2 = R.raw.rank_4_b
                binding.imgRankGrade.setImageBitmap(lettersArray[4])
            }
            "C" -> {
                res = R.raw.rank_5
                res2 = R.raw.rank_5_b
                binding.imgRankGrade.setImageBitmap(lettersArray[5])
            }
            "D" -> {
                res = R.raw.rank_6
                res2 = R.raw.rank_6_b
                binding.imgRankGrade.setImageBitmap(lettersArray[6])
            }
            "F" -> {
                res = R.raw.rank_7
                res2 = R.raw.rank_7_b
                binding.imgRankGrade.setImageBitmap(lettersArray[7])
            }
        }
        binding.imgRankGrade.startAnimation(
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

}