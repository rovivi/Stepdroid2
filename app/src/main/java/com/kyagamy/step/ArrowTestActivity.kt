package com.kyagamy.step

import android.app.Activity
import android.opengl.GLSurfaceView
import android.os.Bundle
import android.widget.TextView
import com.kyagamy.step.engine.ArrowSpriteRenderer

class ArrowTestActivity : Activity() {
    private lateinit var glSurfaceView: GLSurfaceView
    private lateinit var fpsText: TextView
    private lateinit var arrowRenderer: ArrowSpriteRenderer

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_arrow_test)

        // Configurar GLSurfaceView
        glSurfaceView = findViewById(R.id.gl_surface_view)
        fpsText = findViewById(R.id.fps_text)

        // Configurar OpenGL ES 2.0
        glSurfaceView.setEGLContextClientVersion(2)

        // Crear y configurar el renderer
        arrowRenderer = ArrowSpriteRenderer(this)
        arrowRenderer.fpsCallback = { fps, arrowCount ->
            runOnUiThread {
                fpsText.text = "FPS: ${String.format("%.1f", fps)} | Arrows: $arrowCount"
            }
        }

        glSurfaceView.setRenderer(arrowRenderer)
        glSurfaceView.renderMode = GLSurfaceView.RENDERMODE_CONTINUOUSLY
    }

    override fun onResume() {
        super.onResume()
        glSurfaceView.onResume()
    }

    override fun onPause() {
        super.onPause()
        glSurfaceView.onPause()
    }
}