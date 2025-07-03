package com.kyagamy.step.ui

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.RenderEffect
import android.graphics.Shader
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
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
                    modifier = Modifier.padding(innerPadding),
                    perfect = intent.getIntExtra("perfect", 0),
                    great = intent.getIntExtra("great", 0),
                    good = intent.getIntExtra("good", 0),
                    bad = intent.getIntExtra("bad", 0),
                    miss = intent.getIntExtra("miss", 0),
                    maxCombo = intent.getIntExtra("maxCombo", 0),
                    totalScore = intent.getFloatExtra("totalScore", 0f),
                    rank = intent.getStringExtra("rank") ?: "F",
                    songName = intent.getStringExtra("songName") ?: "Unknown Song",
                    imagePath = intent.getStringExtra("imagePath")
                ) { finish() }
            }
        }
    }
}

@Composable
fun ResultScreen(
    modifier: Modifier = Modifier,
    perfect: Int,
    great: Int,
    good: Int,
    bad: Int,
    miss: Int,
    maxCombo: Int,
    totalScore: Float,
    rank: String,
    songName: String,
    imagePath: String?,
    onContinueClicked: () -> Unit
) {
    val baseDelay = 100

    val bitmap = remember(imagePath) {
        if (imagePath != null) {
            BitmapFactory.decodeFile(imagePath)
        } else {
            null
        }
    }

    Box(
        modifier = modifier.fillMaxSize()
    ) {
        // Imagen de fondo blureada
        if (bitmap != null) {
            Image(
                bitmap = bitmap.asImageBitmap(),
                contentDescription = "Background Image",
                modifier = Modifier
                    .fillMaxSize()
                    .blur(radius = 15.dp)
                    .alpha(0.3f),
                contentScale = ContentScale.Crop
            )
        }

        // Overlay negro semi-transparente
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.7f))
        )

        // Contenido principal
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            AnimateText(text = "SCORE", fontSize = 28, delayMillis = baseDelay * 0)
            AnimateText(
                text = totalScore.toInt().toString(),
                fontSize = 40,
                delayMillis = baseDelay * 1
            )
            AnimateText(text = rank, fontSize = 100, delayMillis = baseDelay * 12)

            StatRow(
                label = "PERFECT",
                value = perfect.toString(),
                color = Color.Cyan,
                delayMillis = baseDelay * 3
            )
            StatRow(
                label = "GREAT",
                value = great.toString(),
                color = Color.Green,
                delayMillis = baseDelay * 4
            )
            StatRow(
                label = "GOOD",
                value = good.toString(),
                color = Color.Yellow,
                delayMillis = baseDelay * 5
            )
            StatRow(
                label = "BAD",
                value = bad.toString(),
                color = Color.Magenta,
                delayMillis = baseDelay * 6
            )
            StatRow(
                label = "MISS",
                value = miss.toString(),
                color = Color.Red,
                delayMillis = baseDelay * 7
            )
            StatRow(
                label = "MAX COMBO",
                value = maxCombo.toString(),
                color = Color.White,
                delayMillis = baseDelay * 8
            )
            Text(
                text = songName,
                fontSize = 24.sp,
                color = Color.White,
                modifier = Modifier.padding(vertical = 8.dp)
            )

            Spacer(modifier = Modifier.height(16.dp))

            IconButton(
                onClick = { onContinueClicked() },
                modifier = Modifier
                    .size(56.dp)
                    .clip(CircleShape)
                    .background(Color.Gray)
                    .padding(16.dp)
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                    contentDescription = "Continue",
                    tint = Color.White
                )
            }
        }
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
        AnimateText(text = value, fontSize = 24, delayMillis = delayMillis, Color.White, true)
    }
}
