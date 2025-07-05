package com.kyagamy.step.views

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.kyagamy.step.room.entities.Category

@Composable
fun CategoryCard(
    category: Category,
    position: Int, // -2, -1, 0, 1, 2
    dragOffset: Float,
    isCenter: Boolean,
    isAnimating: Boolean,
    animationProgress: Float,
    swipeDirection: Int, // -1 = left, 1 = right, 0 = none
    onClick: () -> Unit
) {
    val context = LocalContext.current

    // Calcular transformaciones basadas en la posición
    val baseOffset = position * 180f // Espaciado horizontal base

    // Compensar el cambio de orden durante la animación
    val offsetCompensation = if (isAnimating && swipeDirection != 0) {
        when (swipeDirection) {
            1 -> (1f - animationProgress) * 180f // Swipe derecha: compensar hacia la derecha
            -1 -> -(1f - animationProgress) * 180f // Swipe izquierda: compensar hacia la izquierda
            else -> 0f
        }
    } else {
        0f
    }

    val totalOffset =
        baseOffset + dragOffset * 0.3f + offsetCompensation // Aplicar drag y compensación

    val scale = if (isCenter) 1.0f else 0.7f
    val alpha = if (isCenter) 1f else 0.6f
    val elevation = if (isCenter) 12.dp else 6.dp

    // Animación de zoom in/out para el elemento central
    val zoomAnimation = animateFloatAsState(
        targetValue = if (isCenter) 1.1f else 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "zoom"
    )

    Box(
        modifier = Modifier
            .offset(x = totalOffset.dp)
            .scale(scale)
    ) {
        // Borde rotado detrás de la card (solo para el elemento central)
        if (isCenter) {
            Box(
                modifier = Modifier
                    .size(width = 170.dp, height = 210.dp)
                    .graphicsLayer {
                        rotationZ = 45f
                        this.alpha = 0.3f
                    }
                    .background(
                        brush = Brush.linearGradient(
                            colors = listOf(
                                Color.Cyan,
                                Color.Blue,
                                Color.Magenta
                            )
                        ),
                        shape = RoundedCornerShape(16.dp)
                    )
            )
        }

        Card(
            modifier = Modifier
                .size(width = 160.dp, height = 200.dp)
                .scale(if (isCenter) zoomAnimation.value else 1f)
                .graphicsLayer {
                    this.alpha = alpha
                }
                .clickable { onClick() },
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = Color.Transparent // Hacer las cards transparentes
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = elevation)
        ) {
            Box(
                modifier = Modifier.fillMaxSize()
            ) {
                // Imagen de fondo de la categoría
                AsyncImage(
                    model = ImageRequest.Builder(context)
                        .data(category.banner ?: "")
                        .crossfade(true)
                        .build(),
                    contentDescription = category.name,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(RoundedCornerShape(16.dp))
                )

                // Overlay con gradiente solo en la parte inferior para el texto
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            brush = Brush.verticalGradient(
                                colors = listOf(
                                    Color.Transparent,
                                    Color.Transparent,
                                    Color.Transparent,
                                    Color.Black.copy(alpha = 0.5f)
                                ),
                                startY = 0f,
                                endY = Float.POSITIVE_INFINITY
                            )
                        )
                )

                // Texto del nombre de la categoría
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.Bottom,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = category.name ?: "Unknown",
                        color = Color.White,
                        fontSize = if (isCenter) 18.sp else 14.sp,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
    }
}