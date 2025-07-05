package com.kyagamy.step.views

import android.net.Uri
import android.os.Bundle
import android.widget.VideoView
import androidx.appcompat.app.AppCompatActivity
import com.kyagamy.step.R
import com.kyagamy.step.databinding.ActivityTestGlplayerBinding
import com.kyagamy.step.engine.TestSongRenderer

class TestGLPlayerActivity : AppCompatActivity() {
    private lateinit var binding: ActivityTestGlplayerBinding
    private var renderer: TestSongRenderer? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityTestGlplayerBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Configurar video de fondo BGA
        setupBgaVideo()

        // Usar TestSongRenderer en lugar de ArrowSpriteRenderer
        renderer = TestSongRenderer(this)

        // Configurar el renderer en el GLSurfaceView con cast explícito para resolver ambigüedad
        binding.openGLView.setRenderer(renderer!! as android.opengl.GLSurfaceView.Renderer)

        // Actualizar el texto del FPS counter para mostrar información de los test modes
        binding.fpsCounter.text = "StepsDrawer GL - All Test Modes Active"
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
