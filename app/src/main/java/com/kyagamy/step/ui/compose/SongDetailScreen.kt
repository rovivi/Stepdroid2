package com.kyagamy.step.ui.compose

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.animation.core.*
import androidx.compose.animation.core.EaseOutCubic
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.kyagamy.step.room.entities.Song
import com.kyagamy.step.room.entities.Level
import com.kyagamy.step.viewmodels.LevelViewModel
import com.kyagamy.step.common.SettingsGameGetter
import com.kyagamy.step.common.step.CommonGame.ParamsSong
import kotlinx.coroutines.delay
import java.io.File
import android.media.MediaPlayer
import android.widget.VideoView
import androidx.compose.foundation.clickable
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.ui.graphics.Path
import com.kyagamy.step.ui.compose.components.SplitImageWithVideo

@OptIn(ExperimentalSharedTransitionApi::class, ExperimentalMaterial3Api::class)
@Composable
fun SharedTransitionScope.SongDetailScreen(
    song: Song,
    onBack: () -> Unit,
    onLevelSelect: (Level) -> Unit,
    animatedVisibilityScope: AnimatedVisibilityScope
) {
    val levelViewModel: LevelViewModel = viewModel()
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    val levels by levelViewModel.get(song.song_id).observeAsState(emptyList())

    var showVideo by remember { mutableStateOf(false) }
    var isVideoPlaying by remember { mutableStateOf(false) }
    var isMusicPlaying by remember { mutableStateOf(false) }
    var shouldShowContent by remember { mutableStateOf(false) }
    var selectedLevelIndex by remember { mutableStateOf(-1) }

    // Media player states
    var mediaPlayer by remember { mutableStateOf<MediaPlayer?>(null) }
    //var videoView by remember { mutableStateOf<VideoView?>(null) }

    // Extract dominant color from image for background
    var dominantColor by remember { mutableStateOf(Color.Black) }
    var backgroundImage by remember { mutableStateOf<String?>(null) }

    // Setup background image
    LaunchedEffect(song) {
        val bgFile = File(song.PATH_SONG + "/" + song.BACKGROUND)
        if (bgFile.exists()) {
            backgroundImage = bgFile.path
            dominantColor = Color.Black // Cambiar a negro puro sin transparencia
        }
    }

    // Release media player function
    fun releaseMediaPlayer() {
        try {
            mediaPlayer?.let { player ->
                if (player.isPlaying) player.stop()
                player.release()
            }
            mediaPlayer = null
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    // Play music preview function
    suspend fun playMusicPreview(song: Song) {
        val startTime = System.currentTimeMillis()
        releaseMediaPlayer()
        val audio = File(song.PATH_SONG + "/" + song.MUSIC)
        val duration = song.SAMPLELENGTH * 1000 + 3000

        try {
            if (audio.exists()) {
                mediaPlayer = MediaPlayer().apply {
                    setVolume(1f, 1f)
                    setDataSource(audio.path)
                    prepare()
                    seekTo(song.SAMPLESTART.toInt() * 1000)
                    start()
                }

                while (mediaPlayer != null && isMusicPlaying) {
                    delay(100)
                    val timeLapsed = System.currentTimeMillis() - startTime
                    if (timeLapsed >= duration) {
                        releaseMediaPlayer()
                        break
                    } else if (timeLapsed >= (duration - 3000)) {
                        val lapset = (100 - ((timeLapsed - (duration - 3000)) / 3000 * 100)) / 100
                        mediaPlayer?.setVolume(lapset.toFloat(), lapset.toFloat())
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    // Lifecycle observer for media player cleanup
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_PAUSE -> {
                    isVideoPlaying = false
                    isMusicPlaying = false
                    releaseMediaPlayer()
                }

                Lifecycle.Event.ON_DESTROY -> {
                    isVideoPlaying = false
                    isMusicPlaying = false
                    releaseMediaPlayer()
                }

                else -> {}
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            isVideoPlaying = false
            isMusicPlaying = false
            releaseMediaPlayer()
        }
    }

    // Animation sequence
    LaunchedEffect(Unit) {
        shouldShowContent = true

        // Start video and music immediately
        if (song.PREVIEWVID.isNotEmpty()) {
            showVideo = true
            isVideoPlaying = true
        }
        if (song.MUSIC.isNotEmpty()) {
            isMusicPlaying = true
            playMusicPreview(song)
        }
    }

    // Stop music when isMusicPlaying changes
    LaunchedEffect(isMusicPlaying) {
        if (!isMusicPlaying) {
            releaseMediaPlayer()
        }
    }

    // Image split animation - FINAL VERSION
    var animationTrigger by remember { mutableStateOf(0f) }

    // Start animation after screen is fully loaded
    LaunchedEffect(shouldShowContent) {
        if (shouldShowContent) {
            delay(600) // Reduced delay as requested
            animationTrigger = 1f
        }
    }

    // Split animation - image parts slide to sides (slower and more elegant)
    val splitOffset by animateFloatAsState(
        targetValue = animationTrigger,
        animationSpec = tween(
            durationMillis = 1800, // Longer animation for more elegance
            easing = EaseOutCubic
        ),
        label = "image_split"
    )

    // Set default selection when levels are loaded
    LaunchedEffect(levels) {
        if (levels.isNotEmpty() && selectedLevelIndex == -1) {
            selectedLevelIndex = 0
        }
    }

    // Dynamic background based on image
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black) // Fondo negro simple sin gradientes
    ) {
        // Background image with transparency
        backgroundImage?.let { imagePath ->
            AsyncImage(
                model = ImageRequest.Builder(context)
                    .data(imagePath)
                    .crossfade(true)
                    .build(),
                contentDescription = null,
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer { alpha = 0.2f },
                contentScale = ContentScale.Crop
            )
        }

        // Remover el dark overlay que afecta el video
        Column(
            modifier = Modifier
                .fillMaxSize()
                .sharedBounds(
                    sharedContentState = rememberSharedContentState(key = "${song.song_id}-bounds"),
                    animatedVisibilityScope = animatedVisibilityScope,
                    enter = fadeIn(),
                    exit = fadeOut(),
                    resizeMode = SharedTransitionScope.ResizeMode.ScaleToBounds()
                )
        ) {
            // Top section with video/image
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(240.dp)
                    .clip(RoundedCornerShape(bottomStart = 20.dp, bottomEnd = 20.dp))
            ) {
                // Song/Video split image using merged component
                SplitImageWithVideo(
                    song = song,
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(16f / 9f)
                )

                // Music indicator - Z-index 2
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(16.dp)
                        .zIndex(2f)
                        .background(
                            Color.Black.copy(alpha = 0.7f),
                            shape = RoundedCornerShape(20.dp)
                        )
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Text(
                        text = song.GENRE,
                        fontSize = 12.sp,
                        color = Color.White,
                        fontWeight = FontWeight.Bold
                    )
                }

                // Back button - immediate response - Z-index 2
                IconButton(
                    onClick = {
                        isVideoPlaying = false
                        isMusicPlaying = false
                        releaseMediaPlayer()
                        onBack()
                    },
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(16.dp)
                        .size(48.dp)
                        .zIndex(2f)
                        .background(
                            Color.Black.copy(alpha = 0.7f),
                            shape = RoundedCornerShape(24.dp)
                        )
                ) {
                    Icon(
                        Icons.Default.ArrowBack,
                        contentDescription = "Back",
                        tint = Color.White,
                        modifier = Modifier.size(24.dp)
                    )
                }

                // Song title overlay - Z-index 2
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(16.dp)
                        .zIndex(2f)
                        .background(
                            Color.Black.copy(alpha = 0.8f),
                            shape = RoundedCornerShape(12.dp)
                        )
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "BPM: ${song.DISPLAYBPM}",
                            fontSize = 12.sp,
                            color = Color.White,
                            textAlign = TextAlign.Center
                        )
                        Text(
                            text = song.TITLE,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.sharedElement(
                                sharedContentState = rememberSharedContentState(key = "${song.song_id}-title"),
                                animatedVisibilityScope = animatedVisibilityScope
                            )
                        )
                        Text(
                            text = song.ARTIST,
                            fontSize = 14.sp,
                            color = Color.Cyan,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.sharedElement(
                                sharedContentState = rememberSharedContentState(key = "${song.song_id}-artist"),
                                animatedVisibilityScope = animatedVisibilityScope
                            )
                        )
                    }
                }
            }

            // Content section with new horizontal layout
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            ) {
                // Left: Menu de opciones (Level list)
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                ) {
                    Text(
                        "MENU DE OPCIONES",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )

                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                                RoundedCornerShape(16.dp)
                            )
                            .padding(8.dp)
                    ) {
                        itemsIndexed(levels) { index, level ->
                            LevelTrapezoidItem(
                                level = level,
                                isSelected = index == selectedLevelIndex,
                                onSelect = {
                                    selectedLevelIndex = index
                                }
                            )
                        }
                    }
                }

                // Right: Auto Velocity and Speed Controls (A/V)
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                ) {
                    val settingsGameGetter = remember { SettingsGameGetter(context) }

                    // Read initial values from settings
                    var autoVelocity by remember {
                        mutableIntStateOf(
                            settingsGameGetter.getValueInt(SettingsGameGetter.AV)
                        )
                    }
                    var speedValue by remember {
                        mutableFloatStateOf(
                            settingsGameGetter.getValueFloat(SettingsGameGetter.SPEED)
                        )
                    }

                    Text(
                        "CONTROLES A/V",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )

                    Card(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                                RoundedCornerShape(16.dp)
                            ),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = Color.Transparent
                        )
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            // Auto Velocity Section
                            Card(
                                colors = CardDefaults.cardColors(
                                    containerColor = Color(0xFF4CAF50).copy(alpha = 0.2f)
                                ),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(12.dp),
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = "Auto Velocity",
                                            color = Color.White,
                                            fontSize = 14.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Text(
                                            text = autoVelocity.toString(),
                                            color = Color(0xFF4CAF50),
                                            fontSize = 14.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }

                                    // Increment buttons
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceEvenly
                                    ) {
                                        listOf(1, 10, 100).forEach { increment ->
                                            Button(
                                                onClick = {
                                                    autoVelocity += increment
                                                    if (autoVelocity > 1200) autoVelocity =
                                                        1200
                                                    settingsGameGetter.saveSetting(
                                                        SettingsGameGetter.AV,
                                                        autoVelocity
                                                    )
                                                    ParamsSong.av = autoVelocity
                                                },
                                                modifier = Modifier
                                                    .width(60.dp)
                                                    .height(32.dp),
                                                colors = ButtonDefaults.buttonColors(
                                                    containerColor = Color(0xFF4CAF50)
                                                ),
                                                contentPadding = PaddingValues(0.dp)
                                            ) {
                                                Text(
                                                    text = "+$increment",
                                                    fontSize = 10.sp,
                                                    color = Color.White
                                                )
                                            }
                                        }
                                    }

                                    // Decrement buttons
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceEvenly
                                    ) {
                                        listOf(-1, -10, -100).forEach { decrement ->
                                            Button(
                                                onClick = {
                                                    autoVelocity += decrement
                                                    if (autoVelocity < 200) autoVelocity =
                                                        200
                                                    settingsGameGetter.saveSetting(
                                                        SettingsGameGetter.AV,
                                                        autoVelocity
                                                    )
                                                    ParamsSong.av = autoVelocity
                                                },
                                                modifier = Modifier
                                                    .width(60.dp)
                                                    .height(32.dp),
                                                colors = ButtonDefaults.buttonColors(
                                                    containerColor = Color(0xFF4CAF50).copy(
                                                        alpha = 0.7f
                                                    )
                                                ),
                                                contentPadding = PaddingValues(0.dp)
                                            ) {
                                                Text(
                                                    text = "$decrement",
                                                    fontSize = 10.sp,
                                                    color = Color.White
                                                )
                                            }
                                        }
                                    }
                                }
                            }

                            // Speed Section
                            Card(
                                colors = CardDefaults.cardColors(
                                    containerColor = Color(0xFF2196F3).copy(alpha = 0.2f)
                                ),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(12.dp),
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = "Speed",
                                            color = Color.White,
                                            fontSize = 14.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Text(
                                            text = String.format("%.2f", speedValue),
                                            color = Color(0xFF2196F3),
                                            fontSize = 14.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }

                                    // Increment buttons
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceEvenly
                                    ) {
                                        listOf(0.25f, 0.5f, 1f).forEach { increment ->
                                            Button(
                                                onClick = {
                                                    speedValue += increment
                                                    if (speedValue > 10f) speedValue = 10f
                                                    settingsGameGetter.saveSetting(
                                                        SettingsGameGetter.SPEED,
                                                        speedValue
                                                    )
                                                    ParamsSong.speed = speedValue
                                                },
                                                modifier = Modifier
                                                    .width(60.dp)
                                                    .height(32.dp),
                                                colors = ButtonDefaults.buttonColors(
                                                    containerColor = Color(0xFF2196F3)
                                                ),
                                                contentPadding = PaddingValues(0.dp)
                                            ) {
                                                Text(
                                                    text = "+$increment",
                                                    fontSize = 10.sp,
                                                    color = Color.White
                                                )
                                            }
                                        }
                                    }

                                    // Decrement buttons
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceEvenly
                                    ) {
                                        listOf(-0.25f, -0.5f, -1f).forEach { decrement ->
                                            Button(
                                                onClick = {
                                                    speedValue += decrement
                                                    if (speedValue < 0.25f) speedValue = 0.25f
                                                    settingsGameGetter.saveSetting(
                                                        SettingsGameGetter.SPEED,
                                                        speedValue
                                                    )
                                                    ParamsSong.speed = speedValue
                                                },
                                                modifier = Modifier
                                                    .width(60.dp)
                                                    .height(32.dp),
                                                colors = ButtonDefaults.buttonColors(
                                                    containerColor = Color(0xFF2196F3).copy(
                                                        alpha = 0.7f
                                                    )
                                                ),
                                                contentPadding = PaddingValues(0.dp)
                                            ) {
                                                Text(
                                                    text = "$decrement",
                                                    fontSize = 10.sp,
                                                    color = Color.White
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Start button at bottom center
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 16.dp),
                contentAlignment = Alignment.Center
            ) {
                Button(
                    onClick = {
                        if (selectedLevelIndex != -1) {
                            onLevelSelect(levels[selectedLevelIndex])
                        }
                    },
                    modifier = Modifier
                        .width(200.dp)
                        .height(60.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color.Blue.copy(alpha = 0.8f)
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(
                        "Start",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
            }
        }
    }
}

@Composable
fun LevelTrapezoidItem(
    level: Level,
    isSelected: Boolean,
    onSelect: () -> Unit,
    modifier: Modifier = Modifier
) {
    val particleColor = if (level.STEPSTYPE.contains("single", ignoreCase = true)) {
        Color(0xFFFF9800) // Orange for single
    } else {
        Color(0xFF4CAF50) // Green for double
    }

    val glowAlpha by animateFloatAsState(
        targetValue = if (isSelected) 1f else 0.6f,
        animationSpec = tween(300),
        label = "glow_alpha"
    )

    val scaleAnimation by animateFloatAsState(
        targetValue = if (isSelected) 1.05f else 1f,
        animationSpec = spring(dampingRatio = 0.8f, stiffness = 300f),
        label = "scale_animation"
    )

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(90.dp)
            .padding(vertical = 6.dp)
            .graphicsLayer {
                scaleX = scaleAnimation
                scaleY = scaleAnimation
            }
            .clickable { onSelect() }
    ) {
        // Glow effect background
        if (isSelected) {
            Canvas(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(4.dp)
            ) {
                val path = Path().apply {
                    val width = size.width
                    val height = size.height
                    val skew = 25f

                    moveTo(skew, 0f)
                    lineTo(width, 0f)
                    lineTo(width - skew, height)
                    lineTo(0f, height)
                    close()
                }

                // Outer glow
                drawPath(
                    path = path,
                    color = particleColor.copy(alpha = 0.4f)
                )
            }
        }

        // Main trapezoid background with Canvas
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .padding(if (isSelected) 6.dp else 2.dp)
                .graphicsLayer { alpha = glowAlpha }
        ) {
            val path = Path().apply {
                val width = size.width
                val height = size.height
                val skew = 22f

                // Create trapezoid shape
                moveTo(skew, 0f)
                lineTo(width, 0f)
                lineTo(width - skew, height)
                lineTo(0f, height)
                close()
            }

            // Background gradient
            val colors = if (isSelected) {
                listOf(
                    particleColor.copy(alpha = 0.9f),
                    particleColor.copy(alpha = 0.7f)
                )
            } else {
                listOf(
                    Color.Gray.copy(alpha = 0.5f),
                    Color.Gray.copy(alpha = 0.3f)
                )
            }

            drawPath(
                path = path,
                brush = Brush.linearGradient(
                    colors = colors,
                    start = androidx.compose.ui.geometry.Offset(0f, 0f),
                    end = androidx.compose.ui.geometry.Offset(size.width, size.height)
                )
            )

            // Border effect
            if (isSelected) {
                drawPath(
                    path = path,
                    color = particleColor,
                    style = androidx.compose.ui.graphics.drawscope.Stroke(width = 3f)
                )
            }

            // Enhanced particles
            if (isSelected) {
                repeat(12) { i ->
                    val width = size.width
                    val height = size.height
                    val x = (width * 0.15f) + (i * width * 0.07f)
                    val y = height * 0.15f + (i % 3) * height * 0.35f
                    val radius = if (i % 3 == 0) 4f else 2.5f

                    drawCircle(
                        color = particleColor.copy(alpha = 0.8f),
                        radius = radius,
                        center = androidx.compose.ui.geometry.Offset(x, y)
                    )

                    // Add sparkle effect
                    if (i % 4 == 0) {
                        drawCircle(
                            color = Color.White.copy(alpha = 0.6f),
                            radius = 1.5f,
                            center = androidx.compose.ui.geometry.Offset(x + 1f, y - 1f)
                        )
                    }
                }
            }
        }

        // Content overlay with enhanced styling
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Main level number with enhanced styling
            Box(
                modifier = Modifier.weight(1f),
                contentAlignment = Alignment.Center
            ) {
                // Small shadow box when selected
                if (isSelected) {
                    Box(
                        modifier = Modifier
                            .size(60.dp)
                            .offset(x = 3.dp, y = 3.dp)
                            .background(
                                Color.Black.copy(alpha = 0.4f),
                                RoundedCornerShape(8.dp)
                            )
                    )

                    // Main highlight box
                    Box(
                        modifier = Modifier
                            .size(60.dp)
                            .background(
                                brush = Brush.radialGradient(
                                    colors = listOf(
                                        particleColor.copy(alpha = 0.3f),
                                        Color.Transparent
                                    )
                                ),
                                RoundedCornerShape(8.dp)
                            )
                    )
                }

                // Shadow text
                if (isSelected) {
                    Text(
                        text = level.METER.toString(),
                        fontSize = 28.sp, // Reduced to prevent overflow
                        fontWeight = FontWeight.Black,
                        color = Color.Black.copy(alpha = 0.3f),
                        modifier = Modifier.offset(x = 2.dp, y = 2.dp),
                        maxLines = 1
                    )
                }
                // Main text
                Text(
                    text = level.METER.toString(),
                    fontSize = 28.sp, // Reduced to prevent overflow
                    fontWeight = FontWeight.Black,
                    color = Color.White,
                    style = MaterialTheme.typography.headlineLarge,
                    maxLines = 1
                )
            }

            // Enhanced metadata column
            Column(
                modifier = Modifier.weight(2.2f),
                verticalArrangement = Arrangement.Center
            ) {
                // Type badge
                Box(
                    modifier = Modifier
                        .background(
                            particleColor,
                            RoundedCornerShape(8.dp)
                        )
                        .padding(horizontal = 8.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = if (level.STEPSTYPE.contains(
                                "single",
                                ignoreCase = true
                            )
                        ) "Single" else "Double",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = level.DESCRIPTION,
                    fontSize = 11.sp,
                    color = Color.White.copy(alpha = 0.95f),
                    fontWeight = FontWeight.Medium,
                    maxLines = 1
                )

                if (level.CREDIT.isNotEmpty()) {
                    Text(
                        text = "By: ${level.CREDIT}",
                        fontSize = 8.sp,
                        color = Color.White.copy(alpha = 0.8f),
                        maxLines = 1
                    )
                }
            }

            // Enhanced step type indicator
            Box(
                modifier = Modifier
                    .size(24.dp)
                    .background(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                particleColor,
                                particleColor.copy(alpha = 0.7f)
                            )
                        ),
                        shape = RoundedCornerShape(12.dp)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = if (level.STEPSTYPE.contains("single", ignoreCase = true)) "S" else "D",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Black,
                    color = Color.White
                )
            }
        }
    }
}