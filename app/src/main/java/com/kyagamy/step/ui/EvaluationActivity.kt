package com.kyagamy.step.ui

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.media.MediaPlayer
import android.media.SoundPool
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.blur
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kyagamy.step.R
import com.kyagamy.step.ui.ui.theme.StepDroidTheme
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class EvaluationActivity : ComponentActivity() {
    private var mediaPlayer: MediaPlayer? = null
    private var soundPool: SoundPool? = null
    private var soundPullTick: Int = 0
    private var isPoolLoaded = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Configurar sonidos con mejor manejo
        soundPool = SoundPool.Builder()
            .setMaxStreams(10)
            .build()

        soundPool?.setOnLoadCompleteListener { _, _, _ ->
            isPoolLoaded = true
        }

        soundPullTick = soundPool?.load(applicationContext, R.raw.grade_tick, 1) ?: 0

        setContent {
            StepDroidTheme {
                ResultScreen(
                    perfect = intent.getIntExtra("perfect", 1337),
                    great = intent.getIntExtra("great", 151),
                    good = intent.getIntExtra("good", 54),
                    bad = intent.getIntExtra("bad", 31),
                    miss = intent.getIntExtra("miss", 47),
                    maxCombo = intent.getIntExtra("maxCombo", 153),
                    totalScore = intent.getFloatExtra("totalScore", 92.387f),
                    rank = intent.getStringExtra("rank") ?: "A+",
                    songName = intent.getStringExtra("songName") ?: "PRIMA MATERIA",
                    imagePath = intent.getStringExtra("imagePath"),
                    soundPool = soundPool,
                    soundTick = soundPullTick,
                    isPoolLoaded = { isPoolLoaded }
                ) { finish() }
            }
        }

        // Música de fondo con volumen reducido
        mediaPlayer = MediaPlayer.create(this, R.raw.evaluation_loop)
        mediaPlayer?.isLooping = true
        mediaPlayer?.setVolume(0.7f, 0.7f)
        mediaPlayer?.start()
    }

    override fun onDestroy() {
        super.onDestroy()
        mediaPlayer?.release()
        soundPool?.release()
    }
}

@Composable
fun ResultScreen(
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
    soundPool: SoundPool?,
    soundTick: Int,
    isPoolLoaded: () -> Boolean,
    onContinueClicked: () -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    val bitmap = remember(imagePath) {
        if (imagePath != null) {
            BitmapFactory.decodeFile(imagePath)
        } else {
            // Usar imagen por defecto
            BitmapFactory.decodeResource(context.resources, R.drawable.no_banner)
        }
    }

    // Animaciones de entrada
    val titleAlpha = remember { Animatable(0f) }
    val statsAlpha = remember { Animatable(0f) }
    val rankAlpha = remember { Animatable(0f) }

    LaunchedEffect(Unit) {
        // Secuencia de animaciones
        launch {
            delay(200)
            titleAlpha.animateTo(1f, animationSpec = tween(800))
        }
        launch {
            delay(800)
            statsAlpha.animateTo(1f, animationSpec = tween(600))
        }
        launch {
            delay(2000)
            rankAlpha.animateTo(1f, animationSpec = tween(1000))
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        // Imagen de fondo con efecto blur y darkening
        if (bitmap != null) {
            Image(
                bitmap = bitmap.asImageBitmap(),
                contentDescription = "Background",
                modifier = Modifier
                    .fillMaxSize()
                    .blur(radius = 8.dp), // Reduce blur radius
                contentScale = ContentScale.Crop
            )
        }

        // Overlay con gradiente
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color.Black.copy(alpha = 0.8f),
                            Color.Black.copy(alpha = 0.9f),
                            Color.Black.copy(alpha = 0.8f)
                        )
                    )
                )
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Título de la canción
            Text(
                text = songName,
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .alpha(titleAlpha.value)
                    .padding(vertical = 16.dp)
            )

            Spacer(modifier = Modifier.height(20.dp))

            // Score principal
            Row(
                modifier = Modifier
                    .alpha(statsAlpha.value)
                    .fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "SCORE",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                Spacer(modifier = Modifier.width(20.dp))
                AnimatedScore(
                    score = (totalScore * 10000).toInt(),
                    soundPool = soundPool,
                    soundTick = soundTick,
                    delayMs = 1000,
                    isPoolLoaded = isPoolLoaded
                )
            }

            Spacer(modifier = Modifier.height(32.dp))

            // Estadísticas detalladas
            Column(
                modifier = Modifier.alpha(statsAlpha.value),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                StatRow(
                    label = "PERFECT",
                    value = perfect,
                    color = Color.Cyan,
                    soundPool = soundPool,
                    soundTick = soundTick,
                    delayMs = 1200,
                    isPoolLoaded = isPoolLoaded
                )
                StatRow(
                    label = "GREAT",
                    value = great,
                    color = Color.Green,
                    soundPool = soundPool,
                    soundTick = soundTick,
                    delayMs = 1400,
                    isPoolLoaded = isPoolLoaded
                )
                StatRow(
                    label = "GOOD",
                    value = good,
                    color = Color.Yellow,
                    soundPool = soundPool,
                    soundTick = soundTick,
                    delayMs = 1600,
                    isPoolLoaded = isPoolLoaded
                )
                StatRow(
                    label = "BAD",
                    value = bad,
                    color = Color.Magenta,
                    soundPool = soundPool,
                    soundTick = soundTick,
                    delayMs = 1800,
                    isPoolLoaded = isPoolLoaded
                )
                StatRow(
                    label = "MISS",
                    value = miss,
                    color = Color.Red,
                    soundPool = soundPool,
                    soundTick = soundTick,
                    delayMs = 2000,
                    isPoolLoaded = isPoolLoaded
                )
                StatRow(
                    label = "MAX COMBO",
                    value = maxCombo,
                    color = Color.White,
                    soundPool = soundPool,
                    soundTick = soundTick,
                    delayMs = 2200,
                    isPoolLoaded = isPoolLoaded
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Porcentaje total
                Row(
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "KCAL",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    AnimatedPercentage(
                        percentage = totalScore,
                        soundPool = soundPool,
                        soundTick = soundTick,
                        delayMs = 2400,
                        isPoolLoaded = isPoolLoaded
                    )
                }
            }

            Spacer(modifier = Modifier.weight(1f))

            // Grado/Rank
            Text(
                text = rank,
                fontSize = 120.sp,
                fontWeight = FontWeight.Bold,
                color = when (rank) {
                    "SSS" -> Color(0xFFFFD700) // Dorado
                    "SS" -> Color(0xFFC0C0C0)  // Plateado
                    "S" -> Color(0xFFFFB347)   // Naranja
                    "A", "A+" -> Color(0xFF90EE90) // Verde claro
                    "B" -> Color(0xFF87CEEB)    // Azul cielo
                    "C" -> Color.Yellow
                    "D" -> Color(0xFFDDA0DD)    // Ciruela
                    else -> Color.Red
                },
                modifier = Modifier.alpha(rankAlpha.value),
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(32.dp))

            // Botón de salida
            Button(
                onClick = onContinueClicked,
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color.Red.copy(alpha = 0.8f)
                ),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .alpha(rankAlpha.value)
                    .padding(16.dp)
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ExitToApp,
                    contentDescription = "Exit",
                    tint = Color.White,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "EXIT",
                    color = Color.White,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
fun StatRow(
    label: String,
    value: Int,
    color: Color,
    soundPool: SoundPool?,
    soundTick: Int,
    delayMs: Long,
    isPoolLoaded: () -> Boolean
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            color = color
        )
        AnimatedNumber(
            number = value,
            soundPool = soundPool,
            soundTick = soundTick,
            delayMs = delayMs,
            isPoolLoaded = isPoolLoaded
        )
    }
}

@Composable
fun AnimatedNumber(
    number: Int,
    soundPool: SoundPool?,
    soundTick: Int,
    delayMs: Long,
    isPoolLoaded: () -> Boolean
) {
    var displayText by remember { mutableStateOf("000") }
    var lastSoundTime by remember { mutableStateOf(0L) }
    val coroutineScope = rememberCoroutineScope()

    fun playTickSound() {
        val currentTime = System.currentTimeMillis()
        if (isPoolLoaded() && currentTime - lastSoundTime > 50) { // Throttle a 50ms
            soundPool?.play(soundTick, 0.6f, 0.6f, 1, 0, 1.0f)
            lastSoundTime = currentTime
        }
    }

    LaunchedEffect(number) {
        delay(delayMs)

        if (isPoolLoaded()) {
            var strNumber = number.toString()
            if (strNumber.length < 3) {
                strNumber = (strNumber.toInt() + 1000).toString().substring(1)
            }
            val reversedNumber = strNumber.reversed()

            coroutineScope.launch {
                var currentDisplay = ""
                val timePerDigit = 70L / strNumber.length

                for (digit in reversedNumber) {
                    // Animación de números aleatorios
                    repeat(6) {
                        displayText = (0..9).random().toString() + currentDisplay
                        delay(timePerDigit)
                    }

                    // Sonido tick
                    playTickSound()

                    currentDisplay = digit + currentDisplay
                    displayText = currentDisplay
                }

                // Resultado final
                displayText = strNumber
            }
        }
    }

    Text(
        text = displayText,
        fontSize = 20.sp,
        fontWeight = FontWeight.Bold,
        color = Color.White,
        textAlign = TextAlign.End
    )
}

@Composable
fun AnimatedScore(
    score: Int,
    soundPool: SoundPool?,
    soundTick: Int,
    delayMs: Long,
    isPoolLoaded: () -> Boolean
) {
    var displayText by remember { mutableStateOf("0000000") }
    var lastSoundTime by remember { mutableStateOf(0L) }
    val coroutineScope = rememberCoroutineScope()

    fun playTickSound() {
        val currentTime = System.currentTimeMillis()
        if (isPoolLoaded() && currentTime - lastSoundTime > 60) { // Throttle a 60ms para score
            soundPool?.play(soundTick, 0.8f, 0.8f, 1, 0, 1.0f)
            lastSoundTime = currentTime
        }
    }

    LaunchedEffect(score) {
        delay(delayMs)

        if (isPoolLoaded()) {
            val strScore = String.format("%07d", score)
            val reversedScore = strScore.reversed()

            coroutineScope.launch {
                var currentDisplay = ""
                val timePerDigit = 100L / strScore.length

                for (digit in reversedScore) {
                    repeat(8) {
                        displayText = (0..9).random().toString() + currentDisplay
                        delay(timePerDigit)
                    }

                    playTickSound()

                    currentDisplay = digit + currentDisplay
                    displayText = currentDisplay
                }

                displayText = strScore
            }
        }
    }

    Text(
        text = displayText,
        fontSize = 32.sp,
        fontWeight = FontWeight.Bold,
        color = Color.White
    )
}

@Composable
fun AnimatedPercentage(
    percentage: Float,
    soundPool: SoundPool?,
    soundTick: Int,
    delayMs: Long,
    isPoolLoaded: () -> Boolean
) {
    var displayText by remember { mutableStateOf("000.000") }
    var lastSoundTime by remember { mutableStateOf(0L) }
    val coroutineScope = rememberCoroutineScope()

    fun playTickSound() {
        val currentTime = System.currentTimeMillis()
        if (isPoolLoaded() && currentTime - lastSoundTime > 70) { // Throttle a 70ms para porcentaje
            soundPool?.play(soundTick, 0.5f, 0.5f, 1, 0, 1.0f)
            lastSoundTime = currentTime
        }
    }

    LaunchedEffect(percentage) {
        delay(delayMs)

        if (isPoolLoaded()) {
            val strPercentage = String.format("%.3f", percentage)
            val reversedPercentage = strPercentage.reversed()

            coroutineScope.launch {
                var currentDisplay = ""
                val timePerChar = 80L / strPercentage.length

                for (char in reversedPercentage) {
                    if (char != '.') {
                        repeat(6) {
                            displayText = (0..9).random().toString() + currentDisplay
                            delay(timePerChar)
                        }
                    }

                    playTickSound()

                    currentDisplay = char + currentDisplay
                    displayText = currentDisplay
                }

                displayText = strPercentage
            }
        }
    }

    Text(
        text = displayText,
        fontSize = 20.sp,
        fontWeight = FontWeight.Bold,
        color = Color.White
    )
}
