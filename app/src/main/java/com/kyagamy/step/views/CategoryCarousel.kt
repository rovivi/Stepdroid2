package com.kyagamy.step.views

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import com.kyagamy.step.views.CategoryCard
import com.kyagamy.step.views.CategoryInfo
import com.kyagamy.step.views.CategoryStats
import com.kyagamy.step.room.entities.Category
import com.kyagamy.step.viewmodels.CategoryViewModel
import com.kyagamy.step.viewmodels.SongViewModel
import kotlinx.coroutines.launch

@Composable
fun CategoryCarousel(
    categoryViewModel: CategoryViewModel,
    songViewModel: SongViewModel,
    initialPosition: Int,
    onCategorySelected: (Category, Int) -> Unit,
    onCategoryChanged: (Category) -> Unit
) {
    val categories by categoryViewModel.allCategory.observeAsState(emptyList())
    val allSongs by songViewModel.allSong.observeAsState(emptyList())
    var currentIndex by remember { mutableIntStateOf(initialPosition) }

    // Crear lista infinita repitiendo las categorías si hay menos de 5
    val infiniteCategories = remember(categories) {
        if (categories.isEmpty()) {
            emptyList()
        } else if (categories.size < 5) {
            // Repetir las categorías hasta tener al menos 10 elementos para el efecto infinito
            val repeated = mutableListOf<Category>()
            repeat(10) { index ->
                repeated.add(categories[index % categories.size])
            }
            repeated
        } else {
            categories
        }
    }

    // Obtener estadísticas de la categoría actual
    val currentCategory = if (categories.isNotEmpty()) {
        categories[currentIndex % categories.size]
    } else null

    val categoryStats = remember(currentCategory, allSongs) {
        if (currentCategory != null) {
            val songsInCategory = allSongs.filter { it.catecatecate == currentCategory.name }
            CategoryStats(
                songCount = songsInCategory.size,
                totalTime = songsInCategory.sumOf { it.SAMPLELENGTH },
                genres = songsInCategory.map { it.GENRE }.distinct().size
            )
        } else {
            CategoryStats(0, 0.0, 0)
        }
    }

    // Efectos de cambio de categoría
    LaunchedEffect(currentIndex) {
        if (infiniteCategories.isNotEmpty()) {
            val realIndex = currentIndex % categories.size
            onCategoryChanged(categories[realIndex])
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        // Fondo con efecto de video (simulado con gradiente por ahora)
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            Color.Blue.copy(alpha = 0.3f),
                            Color.Magenta.copy(alpha = 0.2f),
                            Color.Black.copy(alpha = 0.8f)
                        )
                    )
                )
        )

        Column(
            modifier = Modifier.fillMaxSize()
        ) {
            // Carousel de categorías
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            ) {
                var isAnimating by remember { mutableStateOf(false) }
                if (infiniteCategories.isNotEmpty()) {
                    InfiniteCarousel(
                        items = infiniteCategories,
                        currentIndex = currentIndex,
                        isAnimating = isAnimating,
                        onIndexChanged = { newIndex, animate ->
                            currentIndex = newIndex
                            if (animate) {
                                isAnimating = true
                            }
                        },
                        onItemSelected = { category ->
                            val realIndex = currentIndex % categories.size
                            onCategorySelected(category, realIndex)
                        },
                        onAnimationComplete = {
                            isAnimating = false
                        }
                    )
                }
            }

            // Información de la categoría en la parte inferior
            if (currentCategory != null) {
                CategoryInfo(
                    category = currentCategory,
                    stats = categoryStats,
                    modifier = Modifier.padding(24.dp)
                )
            }
        }
    }
}

@Composable
fun InfiniteCarousel(
    items: List<Category>,
    currentIndex: Int,
    isAnimating: Boolean,
    onIndexChanged: (Int, Boolean) -> Unit,
    onItemSelected: (Category) -> Unit,
    onAnimationComplete: () -> Unit
) {
    val scope = rememberCoroutineScope()
    var dragOffset by remember { mutableFloatStateOf(0f) }
    var isDragging by remember { mutableStateOf(false) }
    var swipeDirection by remember { mutableIntStateOf(0) } // -1 = left, 1 = right, 0 = none

    // Animación para el cambio suave de posiciones
    val animationProgress = animateFloatAsState(
        targetValue = if (isAnimating) 1f else 0f,
        animationSpec = tween(
            durationMillis = 800,
            easing = FastOutSlowInEasing
        ),
        finishedListener = {
            if (it == 1f) {
                swipeDirection = 0
                onAnimationComplete()
            }
        }
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .pointerInput(Unit) {
                detectDragGestures(
                    onDragStart = { isDragging = true },
                    onDragEnd = {
                        isDragging = false
                        scope.launch {
                            // Determinar dirección del swipe
                            val threshold = 100f
                            when {
                                dragOffset > threshold -> {
                                    // Swipe right - avanzar hacia la derecha (siguiente)
                                    swipeDirection = 1
                                    onIndexChanged((currentIndex + 1) % items.size, true)
                                }

                                dragOffset < -threshold -> {
                                    // Swipe left - avanzar hacia la izquierda (anterior)
                                    swipeDirection = -1
                                    onIndexChanged(
                                        (currentIndex - 1 + items.size) % items.size,
                                        true
                                    )
                                }
                            }
                            // Animar de vuelta a la posición original
                            dragOffset = 0f
                        }
                    }
                ) { change, dragAmount ->
                    change.consume()
                    dragOffset += dragAmount.x
                }
            },
        contentAlignment = Alignment.Center
    ) {
        // Mostrar 5 elementos con posiciones calculadas
        for (i in -2..2) {
            val itemIndex = (currentIndex + i + items.size) % items.size
            val item = items[itemIndex]

            CategoryCard(
                category = item,
                position = i,
                dragOffset = dragOffset,
                isCenter = i == 0,
                isAnimating = isAnimating,
                animationProgress = animationProgress.value,
                swipeDirection = swipeDirection,
                onClick = {
                    if (i == 0) {
                        onItemSelected(item)
                    } else {
                        // Determinar dirección hacia donde moverse
                        val direction = if (i > 0) 1 else -1
                        swipeDirection = direction
                        onIndexChanged(itemIndex, true)
                    }
                }
            )
        }
    }
}