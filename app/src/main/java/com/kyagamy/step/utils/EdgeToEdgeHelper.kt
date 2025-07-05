package com.kyagamy.step.utils

import android.app.Activity
import android.os.Build
import android.view.View
import android.view.WindowInsetsController
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat

/**
 * Utility class to handle edge-to-edge display for Android 15+ with fallback for older versions
 */
object EdgeToEdgeHelper {

    /**
     * Sets up edge-to-edge display for an activity
     * @param activity The activity to configure
     * @param applySystemBarPadding Whether to apply system bar padding to prevent overlap
     */
    fun setupEdgeToEdge(activity: Activity, applySystemBarPadding: Boolean = false) {
        // Enable edge-to-edge display
        WindowCompat.setDecorFitsSystemWindows(activity.window, false)

        // Get the WindowInsetsController
        val windowInsetsController =
            WindowCompat.getInsetsController(activity.window, activity.window.decorView)
        windowInsetsController?.let { controller ->
            // Hide system bars
            controller.hide(WindowInsetsCompat.Type.systemBars())

            // Configure behavior for immersive experience
            controller.systemBarsBehavior =
                WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        }

        // Handle window insets
        if (applySystemBarPadding) {
            ViewCompat.setOnApplyWindowInsetsListener(activity.window.decorView) { view, windowInsets ->
                val insets = windowInsets.getInsets(WindowInsetsCompat.Type.systemBars())
                view.setPadding(insets.left, insets.top, insets.right, insets.bottom)
                WindowInsetsCompat.CONSUMED
            }
        } else {
            ViewCompat.setOnApplyWindowInsetsListener(activity.window.decorView) { view, windowInsets ->
                // Don't apply padding - true edge-to-edge
                view.setPadding(0, 0, 0, 0)
                WindowInsetsCompat.CONSUMED
            }
        }

        // Fallback for older Android versions
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R) {
            @Suppress("DEPRECATION")
            activity.window.decorView.systemUiVisibility = (
                    View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                            or View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                            or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                            or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                            or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                            or View.SYSTEM_UI_FLAG_FULLSCREEN
                    )
        }
    }

    /**
     * Sets up edge-to-edge for game activities with special handling
     * @param activity The game activity to configure
     */
    fun setupGameEdgeToEdge(activity: Activity) {
        setupEdgeToEdge(activity, applySystemBarPadding = false)

        // Additional game-specific configuration
        val windowInsetsController =
            WindowCompat.getInsetsController(activity.window, activity.window.decorView)
        windowInsetsController?.let { controller ->
            // Ensure system bars stay hidden during gameplay
            controller.systemBarsBehavior =
                WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        }
    }

    /**
     * Sets up edge-to-edge with custom inset handling for specific views
     * @param activity The activity to configure
     * @param rootView The root view to apply insets to
     * @param applyTopInset Whether to apply top inset (for status bar)
     * @param applyBottomInset Whether to apply bottom inset (for navigation bar)
     */
    fun setupCustomEdgeToEdge(
        activity: Activity,
        rootView: View,
        applyTopInset: Boolean = false,
        applyBottomInset: Boolean = false
    ) {
        // Enable edge-to-edge display
        WindowCompat.setDecorFitsSystemWindows(activity.window, false)

        // Hide system bars
        val windowInsetsController =
            WindowCompat.getInsetsController(activity.window, activity.window.decorView)
        windowInsetsController?.hide(WindowInsetsCompat.Type.systemBars())
        windowInsetsController?.systemBarsBehavior =
            WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE

        // Custom inset handling
        ViewCompat.setOnApplyWindowInsetsListener(rootView) { view, windowInsets ->
            val insets = windowInsets.getInsets(WindowInsetsCompat.Type.systemBars())

            val leftPadding = 0
            val topPadding = if (applyTopInset) insets.top else 0
            val rightPadding = 0
            val bottomPadding = if (applyBottomInset) insets.bottom else 0

            view.setPadding(leftPadding, topPadding, rightPadding, bottomPadding)
            WindowInsetsCompat.CONSUMED
        }
    }

    /**
     * Temporarily shows system bars (useful for settings or pause menus)
     * @param activity The activity to show system bars for
     */
    fun showSystemBars(activity: Activity) {
        val windowInsetsController =
            WindowCompat.getInsetsController(activity.window, activity.window.decorView)
        windowInsetsController?.show(WindowInsetsCompat.Type.systemBars())
    }

    /**
     * Hides system bars again after they were shown
     * @param activity The activity to hide system bars for
     */
    fun hideSystemBars(activity: Activity) {
        val windowInsetsController =
            WindowCompat.getInsetsController(activity.window, activity.window.decorView)
        windowInsetsController?.let { controller ->
            controller.hide(WindowInsetsCompat.Type.systemBars())
            controller.systemBarsBehavior =
                WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        }
    }

    /**
     * Checks if system bars are currently visible
     * @param activity The activity to check
     * @return True if system bars are visible
     */
    fun areSystemBarsVisible(activity: Activity): Boolean {
        return ViewCompat.getRootWindowInsets(activity.window.decorView)
            ?.isVisible(WindowInsetsCompat.Type.systemBars()) == true
    }
}