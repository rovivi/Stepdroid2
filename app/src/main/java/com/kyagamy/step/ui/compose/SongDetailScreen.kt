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
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.zIndex
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.LifecycleOwner
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.kyagamy.step.room.entities.Song
import com.kyagamy.step.room.entities.Level
import com.kyagamy.step.viewmodels.LevelViewModel
import kotlinx.coroutines.delay
import java.io.File
import android.media.MediaPlayer
import android.widget.VideoView
import androidx.compose.foundation.Canvas
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.core.graphics.drawable.toBitmap
import coil.ImageLoader
import coil.request.SuccessResult

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

    // Media player states
    var mediaPlayer by remember { mutableStateOf<MediaPlayer?>(null) }
    var videoView by remember { mutableStateOf<VideoView?>(null) }

    // Extract dominant color from image for background
    var dominantColor by remember { mutableStateOf(Color.Black) }
    var backgroundImage by remember { mutableStateOf<String?>(null) }

    // Setup background image
    LaunchedEffect(song) {
        val bgFile = File(song.PATH_SONG + "/" + song.BACKGROUND)
        if (bgFile.exists()) {
            backgroundImage = bgFile.path
            dominantColor = Color.Blue.copy(alpha = 0.3f) // Placeholder color
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

    val contentAlpha by animateFloatAsState(
        targetValue = if (shouldShowContent) 1f else 0f,
        animationSpec = tween(600, easing = EaseInOutCubic),
        label = "content_alpha"
    )

    // Dynamic background based on image
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                brush = Brush.radialGradient(
                    colors = listOf(
                        dominantColor.copy(alpha = 0.8f),
                        dominantColor.copy(alpha = 0.4f),
                        Color.Black
                    ),
                    radius = 800f
                )
            )
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

        // Dark overlay
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.6f))
        )

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
                if (isMusicPlaying) {
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
                            text = "♪ Playing",
                            fontSize = 12.sp,
                            color = Color.White,
                            fontWeight = FontWeight.Bold
                        )
                    }
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

            // Content section
            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .graphicsLayer { alpha = contentAlpha }
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                item {
                    // Song info card
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = Color.Black.copy(alpha = 0.7f)
                        )
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            horizontalArrangement = Arrangement.SpaceEvenly
                        ) {
                            SongDetailItem("BPM", song.DISPLAYBPM, Color.Cyan)
                            SongDetailItem("Genre", song.GENRE, Color.Magenta)
                            SongDetailItem("Levels", "${levels.size}", Color.Yellow)
                        }
                    }
                }

                item {
                    // Levels section
                    Text(
                        text = "Select Difficulty",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        modifier = Modifier.padding(vertical = 8.dp)
                    )

                    if (levels.isNotEmpty()) {
                        LevelGrid(
                            levels = levels,
                            columns = 4,
                            bigMode = false,
                            onItemClick = { level ->
                                isVideoPlaying = false
                                isMusicPlaying = false
                                releaseMediaPlayer()
                                onLevelSelect(level)
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(300.dp)
                        )
                    } else {
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(100.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = Color.Gray.copy(alpha = 0.3f)
                            ),
                            shape = RoundedCornerShape(16.dp)
                        ) {
                            Box(
                                modifier = Modifier.fillMaxSize(),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "No levels available",
                                    color = Color.White.copy(alpha = 0.7f),
                                    fontSize = 16.sp
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun SongDetailItem(label: String, value: String, color: Color) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = value,
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            color = color
        )
        Text(
            text = label,
            fontSize = 12.sp,
            color = Color.White.copy(alpha = 0.7f)
        )
    }
}

@Composable
fun SongDetailLevelRange(type: String, range: String, color: Color) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = range,
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            color = color
        )
        Text(
            text = type,
            fontSize = 12.sp,
            color = Color.White.copy(alpha = 0.7f)
        )
    }
}

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun SplitImageWithVideo(
    song: Song,
    modifier: Modifier = Modifier
) {
    val ctx = LocalContext.current
    val imgPath = "${song.PATH_SONG}/${song.BANNER_SONG}"
    val vidPath = "${song.PATH_SONG}/${song.PREVIEWVID}"
    val showVideo = remember(vidPath) { File(vidPath).exists() }

    var trigger by remember { mutableFloatStateOf(0f) }
    LaunchedEffect(song) {
        delay(155) // Delay para que sea más visible al inicio
        trigger = 1f
    }

    // carga bitmap sin recompos excesivas
    val bitmap by produceState<ImageBitmap?>(null, imgPath) {
        val loader = ImageLoader(ctx)
        val req = ImageRequest.Builder(ctx).data(imgPath).build()
        val res = loader.execute(req)
        if (res is SuccessResult) value = (res.drawable.toBitmap()).asImageBitmap()
    }
    if (bitmap == null) return

    BoxWithConstraints(modifier = modifier.clip(RoundedCornerShape(20.dp))) {
        val w = maxWidth
        val h = maxHeight
        val halfW = w / 2
        val anim = animateDpAsState(
            targetValue = (halfW * trigger).coerceAtMost(halfW),
            animationSpec = tween(
                1500,
                easing = FastOutSlowInEasing
            ) // Duración aumentada de 1800 a 2800ms
        )

        if (showVideo) {
            AndroidView({
                VideoView(it).apply {
                    setVideoPath(vidPath)
                    setOnPreparedListener { mp -> mp.isLooping = true; mp.start() }
                }
            }, Modifier.fillMaxSize())
        }

        Canvas(Modifier.fillMaxSize()) {
            val imgW = bitmap!!.width
            val imgH = bitmap!!.height
            val srcW = imgW / 2

            // izquierda
            drawImage(
                image = bitmap!!,
                srcOffset = IntOffset(0, 0),
                srcSize = IntSize(srcW, imgH),
                dstOffset = IntOffset(-anim.value.roundToPx(), 0),
                dstSize = IntSize(halfW.roundToPx(), h.roundToPx())
            )
            // derecha
            drawImage(
                image = bitmap!!,
                srcOffset = IntOffset(srcW, 0),
                srcSize = IntSize(srcW, imgH),
                dstOffset = IntOffset((halfW + anim.value).roundToPx(), 0),
                dstSize = IntSize(halfW.roundToPx(), h.roundToPx())
            )
        }
    }

}