package com.kyagamy.step.views

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.kyagamy.step.ui.SplashScreen
import com.kyagamy.step.ui.StartScreen
import com.kyagamy.step.ui.ui.theme.StepDroidTheme
import com.kyagamy.step.viewmodels.StartViewModel
import kotlinx.coroutines.delay

class StartActivity : ComponentActivity() {

    private val viewModel: StartViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            StepDroidTheme {
                val navController = rememberNavController()
                NavHost(navController, startDestination = "splash") {
                    composable("splash") {
                        SplashScreenWithDelay { navController.navigate("home") { popUpTo("splash") { inclusive = true } } }
                    }
                    composable("home") {
                        val ctx = LocalContext.current
                        StartScreen(viewModel) { dest -> ctx.startActivity(Intent(ctx, dest)) }
                    }
                }
            }
        }
    }
}

@Composable
private fun SplashScreenWithDelay(onFinish: () -> Unit) {
    SplashScreen()
    LaunchedEffect(Unit) {
        delay(500)
        onFinish()
    }
}
