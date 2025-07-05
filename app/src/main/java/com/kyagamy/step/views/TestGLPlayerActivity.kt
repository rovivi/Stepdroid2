package com.kyagamy.step.views

import android.graphics.Point
import android.media.MediaPlayer
import android.net.Uri
import android.os.Bundle
import android.util.DisplayMetrics
import androidx.appcompat.app.AppCompatActivity
import android.opengl.GLSurfaceView
import com.kyagamy.step.R
import com.kyagamy.step.common.Common.Companion.convertStreamToString
import com.kyagamy.step.common.step.Parsers.FileSSC
import com.kyagamy.step.databinding.ActivityTestGlplayerBinding
import com.kyagamy.step.game.opengl.GamePlayGLRenderer
import com.kyagamy.step.engine.ISpriteRenderer
import game.StepObject

class TestGLPlayerActivity : AppCompatActivity() {
    private lateinit var binding: ActivityTestGlplayerBinding
    private var renderer: GamePlayGLRenderer? = null
    private val handler = android.os.Handler(android.os.Looper.getMainLooper())
    private var updateUIRunnable: Runnable? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        android.util.Log.d("TestGLPlayerActivity", "=== onCreate ===")
        binding = ActivityTestGlplayerBinding.inflate(layoutInflater)
        setContentView(binding.root)

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
            FileSSC(s.toString(), nchar).parseData(false).apply { this.path = path ?: "" }
        } catch (e: Exception) {
            e.printStackTrace()
            android.util.Log.e("TestGLPlayerActivity", "Error parsing step data", e)
            null
        }

        val metrics = DisplayMetrics()
        windowManager.defaultDisplay.getRealMetrics(metrics)
        android.util.Log.d(
            "TestGLPlayerActivity",
            "Screen size: ${metrics.widthPixels}x${metrics.heightPixels}"
        )

        if (step != null) {
            android.util.Log.d("TestGLPlayerActivity", "Creating GamePlayGLRenderer...")
            renderer = GamePlayGLRenderer(
                this,
                step,
                binding.bgaVideoView,
                Point(metrics.widthPixels, metrics.heightPixels)
            )
            binding.openGLView.setRenderer(renderer as ISpriteRenderer)
            binding.openGLView.renderMode = GLSurfaceView.RENDERMODE_CONTINUOUSLY
            android.util.Log.d("TestGLPlayerActivity", "Renderer set up complete")
        } else {
            android.util.Log.e("TestGLPlayerActivity", "Step data is null, cannot create renderer")
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
}
