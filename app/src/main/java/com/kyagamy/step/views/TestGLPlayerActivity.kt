package com.kyagamy.step.views

import android.app.Activity
import android.os.Bundle
import com.kyagamy.step.R
import com.kyagamy.step.engine.ArrowSpriteRenderer
import com.kyagamy.step.engine.OpenGLSpriteView

class TestGLPlayerActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_test_gl_player)

        val glView = findViewById<OpenGLSpriteView>(R.id.openGLSpriteView)
        val renderer = ArrowSpriteRenderer(this, R.drawable.blue_arrow_l)
        glView.setRenderer(renderer.getGLRenderer())
        glView.renderMode = android.opengl.GLSurfaceView.RENDERMODE_CONTINUOUSLY
    }
}
