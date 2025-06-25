package com.kyagamy.step.views

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import com.kyagamy.step.ui.SplashScreen
import com.kyagamy.step.ui.ui.theme.StepDroidTheme
import kotlinx.coroutines.delay

class SplashActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            StepDroidTheme {
                SplashContent { navigateToStart() }
            }
        }
    }

    private fun navigateToStart() {
        startActivity(Intent(this, StartActivity::class.java))
        finish()
    }
}

@Composable
private fun SplashContent(onTimeout: () -> Unit) {
    SplashScreen()
    LaunchedEffect(Unit) {
        delay(800)
        onTimeout()
    }
}
