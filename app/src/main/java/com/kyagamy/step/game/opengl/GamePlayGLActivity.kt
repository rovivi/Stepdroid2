package com.kyagamy.step.game.opengl

import android.app.Activity
import android.graphics.Point
import android.opengl.GLSurfaceView
import android.os.Bundle
import android.widget.VideoView
import com.kyagamy.step.game.newplayer.Combo
import game.StepObject

/**
 * Example activity demonstrating the use of UIRenderer with GamePlayGLRenderer
 */
class GamePlayGLActivity : Activity() {

    private lateinit var glSurfaceView: GLSurfaceView
    private lateinit var renderer: GamePlayGLRenderer
    private var videoView: VideoView? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Initialize OpenGL surface view
        glSurfaceView = GLSurfaceView(this)
        glSurfaceView.setEGLContextClientVersion(2)

        // Get step data from intent or other source
        val stepData = getStepDataFromIntent() // Your implementation
        val screenSize = Point()
        windowManager.defaultDisplay.getSize(screenSize)

        // Create renderer with UIRenderer integration
        renderer = GamePlayGLRenderer(this, stepData, videoView, screenSize)
        glSurfaceView.setRenderer(renderer)

        // Set the content view
        setContentView(glSurfaceView)

        // Start the game
        renderer.start()

        // Example: Simulate some game events to test UI
        simulateGameEvents()
    }

    private fun getStepDataFromIntent(): StepObject {
        // Your implementation to get step data
        // This is just a placeholder
        return StepObject() // Replace with actual implementation
    }

    private fun simulateGameEvents() {
        // Example: Simulate hitting notes to test UI updates
        glSurfaceView.post {
            // Simulate a perfect hit
            renderer.updateUIFromGameState(Combo.VALUE_PERFECT, 1)

            // Simulate a miss after 2 seconds
            glSurfaceView.postDelayed({
                renderer.updateUIFromGameState(Combo.VALUE_MISS, 1)
            }, 2000)

            // Simulate a great hit after 4 seconds
            glSurfaceView.postDelayed({
                renderer.updateUIFromGameState(Combo.VALUE_GREAT, 1)
            }, 4000)
        }
    }

    override fun onPause() {
        super.onPause()
        glSurfaceView.onPause()
    }

    override fun onResume() {
        super.onResume()
        glSurfaceView.onResume()
    }

    override fun onDestroy() {
        super.onDestroy()
        renderer.stop()
    }

    // Method to get current UI state
    fun getCurrentUIState(): Pair<Float, Int> {
        return Pair(renderer.getUILife(), renderer.getUICombo())
    }
}