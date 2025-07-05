package com.kyagamy.step.views

import android.net.Uri
import android.os.Bundle
import android.widget.VideoView
import androidx.appcompat.app.AppCompatActivity
import com.kyagamy.step.R
import com.kyagamy.step.databinding.ActivityTestGlplayerBinding
import com.kyagamy.step.engine.ArrowSpriteRenderer

class TestGLPlayerActivity : AppCompatActivity() {
    private lateinit var binding: ActivityTestGlplayerBinding
    private var renderer: ArrowSpriteRenderer? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityTestGlplayerBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Configurar video de fondo BGA
        setupBgaVideo()

        renderer = ArrowSpriteRenderer(this)

        // Configurar callback para recibir datos de FPS
        renderer?.fpsCallback = { fps, arrowCount ->
            runOnUiThread {
                binding.fpsCounter.text = "FPS: ${String.format("%.1f", fps)} | Arrows: $arrowCount"
            }
        }

        binding.openGLView.setRenderer(renderer!!)
    }

    override fun onResume() {
        super.onResume()
        binding.openGLView.onResume()
    }

    override fun onPause() {
        super.onPause()
        binding.openGLView.onPause()
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
