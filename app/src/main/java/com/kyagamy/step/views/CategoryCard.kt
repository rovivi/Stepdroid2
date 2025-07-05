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
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.kyagamy.step.room.entities.Category
import kotlin.math.*

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

    // Animación de zoom in/out para el elemento central (solo si tiene foco)
    val zoomAnimation = animateFloatAsState(
        targetValue = if (isCenter) 1.1f else 1.0f,
        animationSpec = if (isCenter) {
            infiniteRepeatable(
                animation = tween(2000, easing = LinearEasing),
                repeatMode = RepeatMode.Reverse
            )
        } else {
            tween(300) // Animación rápida para elementos no centrales
        },
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
                // Imagen de fondo de la categoría o placeholder
                if (category.banner.isNullOrEmpty()) {
                    // Placeholder con hexágono y degradado
                    CategoryPlaceholder(
                        categoryName = category.name ?: "Unknown",
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    AsyncImage(
                        model = ImageRequest.Builder(context)
                            .data(category.banner)
                            .crossfade(true)
                            .build(),
                        contentDescription = category.name,
                        contentScale = ContentScale.Fit,
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(RoundedCornerShape(16.dp))
                    )
                }

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

@Composable
fun CategoryPlaceholder(
    categoryName: String,
    modifier: Modifier = Modifier
) {
    // Obtener la inicial del canal
    val initial = categoryName.firstOrNull()?.uppercase() ?: "?"

    // Colores base del degradado basados en el nombre
    val baseColors = when (categoryName.firstOrNull()?.lowercaseChar()) {
        in 'a'..'f' -> listOf(Color(0xFF6B73FF), Color(0xFF000DFF))
        in 'g'..'l' -> listOf(Color(0xFF9B59B6), Color(0xFF8E44AD))
        in 'm'..'r' -> listOf(Color(0xFFE74C3C), Color(0xFFC0392B))
        in 's'..'z' -> listOf(Color(0xFF1ABC9C), Color(0xFF16A085))
        else -> listOf(Color(0xFF34495E), Color(0xFF2C3E50))
    }

    // Animación del degradado
    val animatedProgress by animateFloatAsState(
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(3000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "gradientAnimation"
    )

    // Interpolación de colores para la animación
    val animatedColors = listOf(
        androidx.compose.ui.graphics.lerp(baseColors[0], baseColors[1], animatedProgress),
        androidx.compose.ui.graphics.lerp(baseColors[1], baseColors[0], animatedProgress)
    )

    Box(
        modifier = modifier
            .drawWithContent {
                // Dibujar hexágono
                val hexagonPath = androidx.compose.ui.graphics.Path().apply {
                    val centerX = size.width / 2f
                    val centerY = size.height / 2f
                    val radius = kotlin.math.min(size.width, size.height) / 2f * 0.9f

                    // Crear hexágono
                    for (i in 0..5) {
                        val angle = (i * 60 - 90) * kotlin.math.PI / 180
                        val x = centerX + radius * kotlin.math.cos(angle).toFloat()
                        val y = centerY + radius * kotlin.math.sin(angle).toFloat()
                        if (i == 0) moveTo(x, y) else lineTo(x, y)
                    }
                    close()
                }

                // Dibujar con degradado animado
                drawPath(
                    path = hexagonPath,
                    brush = Brush.radialGradient(
                        colors = animatedColors,
                        radius = kotlin.math.min(size.width, size.height) / 2f
                    )
                )

                // Dibujar contenido encima
                drawContent()
            },
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // Icono de música
            Box(
                modifier = Modifier
                    .size(60.dp)
                    .background(
                        brush = Brush.linearGradient(
                            colors = listOf(
                                Color.White.copy(alpha = 0.3f),
                                Color.White.copy(alpha = 0.1f)
                            )
                        ),
                        shape = RoundedCornerShape(12.dp)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "♪",
                    fontSize = 32.sp,
                    color = Color.White,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Inicial del canal
            Text(
                text = initial,
                fontSize = 24.sp,
                color = Color.White,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Preview(showBackground = true, widthDp = 200, heightDp = 280)
@Composable
fun PreviewCategoryCardCenter() {
    val category = Category(
        name = "PHOENIX",
        path = "/sdcard/stepdroid/songs/PHOENIX",
        banner = null, // Sin imagen para mostrar placeholder
        music_category = null
    )
    CategoryCard(
        category = category,
        position = 0,
        dragOffset = 0f,
        isCenter = true,
        isAnimating = false,
        animationProgress = 0f,
        swipeDirection = 0,
        onClick = {}
    )
}

@Preview(showBackground = true, widthDp = 150, heightDp = 220)
@Composable
fun PreviewCategoryCardSide() {
    val category = Category(
        name = "VINTAGE",
        path = "/sdcard/stepdroid/songs/VINTAGE",
        banner = null,
        music_category = null
    )
    CategoryCard(
        category = category,
        position = 1,
        dragOffset = 0f,
        isCenter = false,
        isAnimating = false,
        animationProgress = 0f,
        swipeDirection = 0,
        onClick = {}
    )
}

@Preview(showBackground = true, widthDp = 400, heightDp = 300)
@Composable
fun PreviewCategoryCarouselSample() {
    Row(
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        modifier = Modifier.padding(16.dp)
    ) {
        // Elemento lateral izquierdo
        CategoryCard(
            category = Category("CLASSIC", "", null, null),
            position = -1,
            dragOffset = 0f,
            isCenter = false,
            isAnimating = false,
            animationProgress = 0f,
            swipeDirection = 0,
            onClick = {}
        )

        // Elemento central (con foco)
        CategoryCard(
            category = Category("PHOENIX", "", null, null),
            position = 0,
            dragOffset = 0f,
            isCenter = true,
            isAnimating = false,
            animationProgress = 0f,
            swipeDirection = 0,
            onClick = {}
        )

        // Elemento lateral derecho
        CategoryCard(
            category = Category("MODERN", "", null, null),
            position = 1,
            dragOffset = 0f,
            isCenter = false,
            isAnimating = false,
            animationProgress = 0f,
            swipeDirection = 0,
            onClick = {}
        )
    }
}