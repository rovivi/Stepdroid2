package com.kyagamy.step.views

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.kyagamy.step.utils.EdgeToEdgeHelper

open class FullScreenActivity: AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Use EdgeToEdgeHelper for consistent edge-to-edge implementation
        EdgeToEdgeHelper.setupEdgeToEdge(this, applySystemBarPadding = false)
    }

    override fun onRestart() {
        super.onRestart()
        hideSystemUI()
    }

    override fun onStart() {
        super.onStart()
        hideSystemUI()
    }

    override fun onPause() {
        super.onPause()
        hideSystemUI()
    }

    override fun onResume() {
        super.onResume()
        hideSystemUI()
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus) {
            hideSystemUI()
        }
    }

    private fun hideSystemUI() {
        EdgeToEdgeHelper.hideSystemBars(this)
    }

    // Public method to show system bars temporarily (useful for settings, etc.)
    protected fun showSystemUI() {
        EdgeToEdgeHelper.showSystemBars(this)
    }

    // Method to toggle system bars visibility
    protected fun toggleSystemUI() {
        if (EdgeToEdgeHelper.areSystemBarsVisible(this)) {
            EdgeToEdgeHelper.hideSystemBars(this)
        } else {
            EdgeToEdgeHelper.showSystemBars(this)
        }
    }
}