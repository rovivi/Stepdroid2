package com.kyagamy.step.ui

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay

class EvaluationActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                ResultScreen(
                    modifier = Modifier.padding(innerPadding)
                )
            }
        }
    }
}

@Composable
fun ResultScreen(modifier: Modifier = Modifier) {
    val baseDelay = 100

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black)
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        AnimateText(text = "SCORE", fontSize = 28, delayMillis = baseDelay * 0)
        AnimateText(text = "0801414", fontSize = 40, delayMillis = baseDelay * 1)
        AnimateText(text = "A", fontSize = 100, delayMillis = baseDelay * 12)

        StatRow(label = "PERFECT", value = "1195", color = Color.Cyan, delayMillis = baseDelay * 3)
        StatRow(label = "GREAT", value = "235", color = Color.Green, delayMillis = baseDelay * 4)
        StatRow(label = "GOOD", value = "124", color = Color.Yellow, delayMillis = baseDelay * 5)
        StatRow(label = "BAD", value = "073", color = Color.Magenta, delayMillis = baseDelay * 6)
        StatRow(label = "MISS", value = "073", color = Color.Red, delayMillis = baseDelay * 7)
        StatRow(label = "MAX COMBO", value = "229", color = Color.White, delayMillis = baseDelay * 8)

        Spacer(modifier = Modifier.height(16.dp))
    }
}


@Composable
fun AnimateText(
    text: String,
    fontSize: Int,
    delayMillis: Int,
    color: Color = Color.White,
    animateNumbers: Boolean = false
) {
    var animatedText by remember { mutableStateOf("") }
    val animAlpha = remember { Animatable(0f) }

    LaunchedEffect(Unit) {
        delay(delayMillis.toLong())
        animAlpha.animateTo(1f, animationSpec = tween(durationMillis = 500))

        if (animateNumbers) {
            val timeMs = 78L / text.length
            var tempText = ""

            for (element in text) {
                if (element != '.') {
                    repeat(6) {
                        animatedText = ((0..9).random().toString() + tempText)
                        delay(timeMs)
                    }
                }
                tempText = element + tempText
                animatedText = tempText
            }
            animatedText = tempText.reversed()
        } else {
            animatedText = text
        }
    }

    Text(
        text = animatedText,
        fontSize = fontSize.sp,
        color = color,
        fontWeight = FontWeight.Bold,
        modifier = Modifier.padding(vertical = 8.dp)
    )
}

@Composable
fun StatRow(label: String, value: String, color: Color, delayMillis: Int) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        AnimateText(text = label, fontSize = 24, delayMillis = delayMillis, color)
        AnimateText(text = value, fontSize = 24, delayMillis = delayMillis, color,true)
    }
}
