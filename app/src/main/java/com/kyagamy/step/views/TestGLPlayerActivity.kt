package com.kyagamy.step.views

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.kyagamy.step.databinding.ActivityTestGlplayerBinding
import com.kyagamy.step.engine.ArrowSpriteRenderer

class TestGLPlayerActivity : AppCompatActivity() {
    private lateinit var binding: ActivityTestGlplayerBinding
    private var renderer: ArrowSpriteRenderer? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityTestGlplayerBinding.inflate(layoutInflater)
        setContentView(binding.root)

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
}
