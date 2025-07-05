package com.kyagamy.step.fragments

import android.media.MediaPlayer
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.RepeatMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import com.kyagamy.step.viewmodels.SongViewModel
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.kyagamy.step.views.MainActivity
import com.kyagamy.step.room.entities.Category
import com.kyagamy.step.viewmodels.CategoryViewModel
import kotlinx.coroutines.launch
import java.io.File

private const val POSITION = "position"

class CategoryFragament : Fragment() {
    private var position = 0
    private lateinit var categoryModel: CategoryViewModel
    private lateinit var songViewModel: SongViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            position = it.getInt(POSITION)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        categoryModel = ViewModelProvider(this)[CategoryViewModel::class.java]
        songViewModel = ViewModelProvider(this)[SongViewModel::class.java]

        return ComposeView(requireContext()).apply {
            setContent {
                CategoryCarousel(
                    categoryViewModel = categoryModel,
                    songViewModel = songViewModel,
                    initialPosition = position,
                    onCategorySelected = { category, pos ->
                        position = pos
                        val mainActivity = activity as MainActivity
                        mainActivity.changeCategory(category.name, pos)
                    },
                    onCategoryChanged = { category ->
                        playSound(category)
                    }
                )
            }
        }
    }

    private fun playSound(category: Category) {
        if (category.music_category != null) {
            try {
                if (File(category.music_category.toString()).exists()) {
                    val mp = MediaPlayer()
                    mp.setDataSource(category.music_category)
                    mp.prepare()
                    mp.start()
                }
            } catch (_: Exception) {
            }
        }
    }
}

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
    var isAnimating by remember { mutableStateOf(false) }

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

data class CategoryStats(
    val songCount: Int,
    val totalTime: Double,
    val genres: Int
)

@Composable
fun CategoryInfo(
    category: Category,
    stats: CategoryStats,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Text(
                text = "📺 Channel: ",
                color = Color.Yellow,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = category.name ?: "Unknown",
                color = Color.Yellow,
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        Row(
            horizontalArrangement = Arrangement.spacedBy(32.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            InfoItem(
                label = "Songs",
                value = stats.songCount.toString(),
                icon = "🎵"
            )

            InfoItem(
                label = "Genres",
                value = stats.genres.toString(),
                icon = "🎭"
            )

            InfoItem(
                label = "Duration",
                value = "${String.format("%.1f", stats.totalTime / 60)}m",
                icon = "⏱️"
            )
        }
    }
}

@Composable
fun InfoItem(
    label: String,
    value: String,
    icon: String
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = icon,
            fontSize = 20.sp
        )
        Text(
            text = value,
            color = Color.Yellow,
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = label,
            color = Color.Yellow.copy(alpha = 0.7f),
            fontSize = 12.sp
        )
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

    // Animación para el cambio suave de posiciones
    val animationProgress = animateFloatAsState(
        targetValue = if (isAnimating) 1f else 0f,
        animationSpec = tween(
            durationMillis = 800,
            easing = FastOutSlowInEasing
        ),
        finishedListener = {
            if (it == 1f) {
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
                                    // Swipe right - ir al anterior
                                    onIndexChanged(
                                        (currentIndex - 1 + items.size) % items.size,
                                        true
                                    )
                                }

                                dragOffset < -threshold -> {
                                    // Swipe left - ir al siguiente
                                    onIndexChanged((currentIndex + 1) % items.size, true)
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
                onClick = {
                    if (i == 0) {
                        onItemSelected(item)
                    } else {
                        onIndexChanged(itemIndex, true)
                    }
                }
            )
        }
    }
}

@Composable
fun CategoryCard(
    category: Category,
    position: Int, // -2, -1, 0, 1, 2
    dragOffset: Float,
    isCenter: Boolean,
    isAnimating: Boolean,
    animationProgress: Float,
    onClick: () -> Unit
) {
    val context = LocalContext.current

    // Calcular transformaciones basadas en la posición
    val baseOffset = position * 180f // Espaciado horizontal base
    val totalOffset = baseOffset + dragOffset * 0.3f // Aplicar drag con factor de amortiguación

    val scale = if (isCenter) 1.0f else 0.7f
    val cardRotationY = if (isCenter) 0f else position * 25f // Rotación en Y
    val cardRotationZ = if (isCenter) 0f else -position * 15f // Rotación en Z opuesta
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

    // Animación de rotación durante el cambio
    val rotationAnimation = if (isAnimating && !isCenter) {
        // Elementos laterales dan 2 vueltas completas (720 grados)
        animationProgress * 720f * if (position > 0) 1f else -1f
    } else {
        0f
    }

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
                    rotationY = cardRotationY + rotationAnimation
                    rotationZ = cardRotationZ
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
