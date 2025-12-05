package com.kyagamy.step.ui.compose

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.Crossfade
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.core.*
import androidx.compose.animation.core.EaseOutCubic
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Settings
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
import androidx.compose.material.icons.filled.Close
import androidx.compose.ui.graphics.Path
import com.kyagamy.step.ui.compose.components.SplitImageWithVideo
import com.kyagamy.step.views.gameplayactivity.GamePlayLibGDXActivity

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

                    // State for collapsing modifiers
                    var showModifiers by remember { mutableStateOf(false) }

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            "CONTROLES A/V",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        IconButton(
                            onClick = { showModifiers = !showModifiers },
                            modifier = Modifier.size(24.dp)
                        ) {
                            Icon(
                                imageVector = if (showModifiers) Icons.Default.Close else Icons.Default.Settings,
                                contentDescription = "Toggle Modifiers",
                                tint = Color.White
                            )
                        }
                    }

                    AnimatedVisibility(
                        visible = showModifiers,
                        enter = fadeIn() + expandVertically(),
                        exit = fadeOut() + shrinkVertically()
                    ) {
                        Card(
                            modifier = Modifier
                                .fillMaxWidth() // Changed from fillMaxSize to fit content when visible
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
                                    .fillMaxWidth()
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
                            val selectedLevel = levels[selectedLevelIndex]
                            // Stop media playback
                            isVideoPlaying = false
                            isMusicPlaying = false
                            releaseMediaPlayer()

                            // Use the same logic as FragmentStartMenu to start the game
                            val intent = android.content.Intent(
                                context,
                                GamePlayLibGDXActivity::class.java
                            )
                            intent.putExtra("ssc", song.PATH_File)
                            intent.putExtra("nchar", selectedLevel.index)
                            intent.putExtra("path", song.PATH_SONG)
                            intent.putExtra("pathDisc", song.PATH_SONG + song.BANNER_SONG)

                            context.startActivity(intent)
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
        targetValue = if (isSelected) 1f else 0.8f,
        animationSpec = tween(300),
        label = "glow_alpha"
    )

    val scaleAnimation by animateFloatAsState(
        targetValue = if (isSelected) 1.05f else 1f,
        animationSpec = spring(dampingRatio = 0.7f, stiffness = 400f),
        label = "scale_animation"
    )

    // Animated gradient colors for selected items
    val animatedTime by rememberInfiniteTransition(label = "gradient_animation").animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "gradient_time"
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
        // Enhanced glow effect background with contour
        if (isSelected) {
            Canvas(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(1.dp)
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

                // Outer contour glow
                drawPath(
                    path = path,
                    color = particleColor.copy(alpha = 0.8f),
                    style = androidx.compose.ui.graphics.drawscope.Stroke(width = 4f)
                )
            }
        }

        // Main trapezoid background with Canvas
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .padding(if (isSelected) 8.dp else 4.dp)
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

            // Simpler gradient for performance
            val colors = if (isSelected) {
                listOf(
                    particleColor.copy(alpha = 0.9f),
                    particleColor.copy(alpha = 0.6f)
                )
            } else {
                listOf(
                    particleColor.copy(alpha = 0.6f),
                    particleColor.copy(alpha = 0.4f)
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

            // Enhanced border effect
            if (isSelected) {
                drawPath(
                    path = path,
                    color = Color.White.copy(alpha = 0.8f),
                    style = androidx.compose.ui.graphics.drawscope.Stroke(width = 2f)
                )
            }
        }

        // Content overlay with enhanced styling
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Main level number - smaller but bold
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight(),
                contentAlignment = Alignment.Center
            ) {
                // Enhanced circular glow when selected (instead of black box)
                if (isSelected) {
                    // Outer bright glow ring
                    Box(
                        modifier = Modifier
                            .size(60.dp)
                            .background(
                                brush = Brush.radialGradient(
                                    colors = listOf(
                                        particleColor.copy(alpha = 0.6f + 0.4f * animatedTime),
                                        particleColor.copy(alpha = 0.3f + 0.3f * animatedTime),
                                        Color.Transparent
                                    ),
                                    radius = 80f
                                ),
                                shape = RoundedCornerShape(30.dp)
                            )
                    )

                    // Inner bright core
                    Box(
                        modifier = Modifier
                            .size(45.dp)
                            .background(
                                brush = Brush.radialGradient(
                                    colors = listOf(
                                        Color.White.copy(alpha = 0.4f * animatedTime),
                                        particleColor.copy(alpha = 0.5f),
                                        Color.Transparent
                                    ),
                                    radius = 60f
                                ),
                                shape = RoundedCornerShape(22.dp)
                            )
                    )
                }

                // Bright outline text effect when selected
                if (isSelected) {
                    // White outline for contrast
                    Text(
                        text = level.METER.toString(),
                        fontSize = 26.sp,
                        fontWeight = FontWeight.Black,
                        color = Color.White,
                        modifier = Modifier.offset(x = 1.dp, y = 1.dp),
                        maxLines = 1,
                        textAlign = TextAlign.Center,
                        style = MaterialTheme.typography.headlineLarge.copy(
                            shadow = androidx.compose.ui.graphics.Shadow(
                                color = particleColor,
                                offset = androidx.compose.ui.geometry.Offset(2f, 2f),
                                blurRadius = 8f
                            )
                        )
                    )
                }

                // Main number text with enhanced styling
                Text(
                    text = level.METER.toString(),
                    fontSize = 26.sp,
                    fontWeight = FontWeight.Black,
                    color = if (isSelected) Color.White else Color.White.copy(alpha = 0.9f),
                    maxLines = 1,
                    textAlign = TextAlign.Center,
                    style = if (isSelected) {
                        MaterialTheme.typography.headlineLarge.copy(
                            shadow = androidx.compose.ui.graphics.Shadow(
                                color = Color.Black,
                                offset = androidx.compose.ui.geometry.Offset(1f, 1f),
                                blurRadius = 4f
                            )
                        )
                    } else {
                        MaterialTheme.typography.headlineLarge
                    }
                )
            }

            // Enhanced metadata column with stepmaker info
            Column(
                modifier = Modifier.weight(2.2f),
                verticalArrangement = Arrangement.Center
            ) {
                // Type badge with semi-transparent gray background
                Box(
                    modifier = Modifier
                        .background(
                            Color.Gray.copy(alpha = 0.3f), // Semi-transparent gray
                            shape = RoundedCornerShape(8.dp)
                        )
                        .padding(horizontal = 8.dp, vertical = 3.dp)
                ) {
                    Text(
                        text = if (level.STEPSTYPE.contains(
                                "single",
                                ignoreCase = true
                            )
                        ) "Single" else "Double",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White, // Colored text on gray background
                    )
                }

                Spacer(modifier = Modifier.height(4.dp))

                // Stepmaker/UCS info
                if (level.CREDIT.isNotEmpty()) {
                    Text(
                        text = level.CREDIT,
                        fontSize = 10.sp,
                        color = Color.White.copy(alpha = 0.9f),
                        fontWeight = FontWeight.Medium,
                        maxLines = 1
                    )
                } else if (level.DESCRIPTION.isNotEmpty()) {
                    Text(
                        text = level.DESCRIPTION,
                        fontSize = 10.sp,
                        color = Color.White.copy(alpha = 0.9f),
                        fontWeight = FontWeight.Medium,
                        maxLines = 1
                    )
                }

                // Additional metadata
                Text(
                    text = "Lv.${level.METER} • ${
                        if (level.STEPSTYPE.contains(
                                "single",
                                ignoreCase = true
                            )
                        ) "SP" else "DP"
                    }",
                    fontSize = 8.sp,
                    color = Color.White.copy(alpha = 0.7f),
                    maxLines = 1
                )
            }
        }
    }
}