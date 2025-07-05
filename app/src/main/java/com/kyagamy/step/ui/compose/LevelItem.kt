package com.kyagamy.step.ui.compose

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import com.kyagamy.step.R
import com.kyagamy.step.room.entities.Level
import java.util.Locale

@Composable
fun LevelItem(
    level: Level,
    bigMode: Boolean = false,
    onClick: () -> Unit = {}
) {
    val context = LocalContext.current
    val karnivol = FontFamily(Font(R.font.karma_light))

    val infiniteTransition = rememberInfiniteTransition(label = "rotation")
    val rotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "rotation"
    )

    val itemSize = if (bigMode) 200.dp else 60.dp
    val textSize = if (bigMode) 60.sp else 32.sp

    Box(
        modifier = Modifier
            .size(itemSize)
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        // Static background
        Image(
            painter = painterResource(id = R.drawable.level_levelblack),
            contentDescription = null,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Fit
        )

        // Type level background
        val typeDrawable = getTypeDrawable(level)
        Image(
            painter = painterResource(id = typeDrawable),
            contentDescription = null,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Fit
        )

        // Rotating glow
        Image(
            painter = painterResource(id = R.drawable.level_glow),
            contentDescription = null,
            modifier = Modifier
                .fillMaxSize()
                .rotate(rotation)
                .alpha(0.5f),
            contentScale = ContentScale.Fit
        )

        // Level text
        Text(
            text = if (level.METER >= 10) level.METER.toString() else ("0${level.METER}"),
            fontFamily = karnivol,
            fontSize = textSize,
            fontWeight = FontWeight.Bold,
            color = Color.White,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .fillMaxSize()
                .wrapContentSize(Alignment.Center)
                .zIndex(1f)
        )
    }
}

private fun getTypeDrawable(level: Level): Int {
    return when {
        level.STEPSTYPE.lowercase(Locale.ROOT).contains("pump-single") -> {
            if (level.DESCRIPTION.lowercase(Locale.ROOT).contains("dp")) {
                R.drawable.level_single_perf
            } else {
                R.drawable.level_single
            }
        }

        level.STEPSTYPE.lowercase(Locale.ROOT).contains("pump-double") ||
                level.STEPSTYPE.lowercase(Locale.ROOT).contains("half-double") -> {
            if (level.CHARTNAME.lowercase(Locale.ROOT).contains("coop")) {
                R.drawable.level_coop
            } else {
                if (level.DESCRIPTION.lowercase(Locale.ROOT).contains("dp")) {
                    R.drawable.level_double_perf
                } else {
                    R.drawable.level_double
                }
            }
        }

        else -> R.drawable.level_single
    }
}