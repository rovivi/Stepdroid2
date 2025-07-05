package com.kyagamy.step.views

import android.graphics.Point
import android.media.MediaPlayer
import android.net.Uri
import android.os.Bundle
import android.util.DisplayMetrics
import androidx.appcompat.app.AppCompatActivity
import com.kyagamy.step.R
import com.kyagamy.step.common.Common.Companion.convertStreamToString
import com.kyagamy.step.common.step.Parsers.FileSSC
import com.kyagamy.step.databinding.ActivityTestGlplayerBinding
import com.kyagamy.step.game.opengl.GamePlayGLRenderer
import game.StepObject

class TestGLPlayerActivity : AppCompatActivity() {
    private lateinit var binding: ActivityTestGlplayerBinding
    private var renderer: GamePlayGLRenderer? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityTestGlplayerBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Configurar video de fondo BGA
        setupBgaVideo()

        // Preparar datos de la canción igual que en GamePlayActivity
        val rawSSC = intent.extras?.getString("ssc")
        val path = intent.extras?.getString("path")
        val nchar = intent.extras?.getInt("nchar") ?: 0

        val step: StepObject? = try {
            val s = convertStreamToString(java.io.FileInputStream(rawSSC))
            FileSSC(s.toString(), nchar).parseData(false).apply { this.path = path ?: "" }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }

        val metrics = DisplayMetrics()
        windowManager.defaultDisplay.getRealMetrics(metrics)

        if (step != null) {
            renderer = GamePlayGLRenderer(
                this,
                step,
                binding.bgaVideoView,
                Point(metrics.widthPixels, metrics.heightPixels)
            )
            binding.openGLView.setRenderer(renderer!!)
        }

        binding.fpsCounter.text = "OpenGL Renderer"
    }

    override fun onResume() {
        super.onResume()
        binding.openGLView.onResume()
        renderer?.start()
    }

    override fun onPause() {
        super.onPause()
        binding.openGLView.onPause()
        renderer?.stop()
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
    }
}
