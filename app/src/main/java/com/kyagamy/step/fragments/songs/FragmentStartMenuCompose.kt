package com.kyagamy.step.fragments.songs

import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.graphics.drawable.BitmapDrawable
import android.media.MediaPlayer
import android.media.SoundPool
import android.widget.VideoView
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.compose.runtime.livedata.observeAsState
import com.kyagamy.step.R
import com.kyagamy.step.adapters.LevelAdapter
import com.kyagamy.step.common.step.CommonGame.TransformBitmap
import com.kyagamy.step.room.entities.Song
import com.kyagamy.step.viewmodels.LevelViewModel
import com.kyagamy.step.viewmodels.SongViewModel
import com.kyagamy.step.views.gameplayactivity.GamePlayActivity
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileNotFoundException

@Composable
fun FragmentStartMenuCompose(
    idSong: Int,
    onDismiss: () -> Unit,
    loadingScreen: Boolean = false
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val scope = rememberCoroutineScope()
    val density = LocalDensity.current

    // ViewModels
    val levelViewModel: LevelViewModel = viewModel()
    val songViewModel: SongViewModel = viewModel()

    // States
    var currentSong by remember { mutableStateOf<Song?>(null) }
    var warningStartSong by remember { mutableStateOf(false) }
    var mediaPlayer by remember { mutableStateOf<MediaPlayer?>(null) }
    var changeMusic by remember { mutableStateOf<SoundPool?>(null) }
    var spCode by remember { mutableIntStateOf(0) }
    var errorAuxImage by remember { mutableStateOf<BitmapDrawable?>(null) }

    // Animation states
    val backgroundAlpha by animateFloatAsState(
        targetValue = if (warningStartSong) 1f else 0.39f,
        animationSpec = tween(250),
        finishedListener = {
            if (warningStartSong) {
                onDismiss()
            }
        }
    )

    val rotationAnimation by rememberInfiniteTransition().animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(3000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        )
    )

    val fadeAnimation by rememberInfiniteTransition().animateFloat(
        initialValue = 0.5f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        )
    )

    // Observe song data
    val songData by songViewModel.songById(idSong).observeAsState()

    // Observe levels
    val levels by levelViewModel.get(idSong).observeAsState(initial = emptyList())

    // Update currentSong when data changes
    LaunchedEffect(songData) {
        songData?.let { songs ->
            if (songs.isNotEmpty()) {
                currentSong = songs[0]
            }
        }
    }

    // Handle song change
    LaunchedEffect(currentSong) {
        currentSong?.let { song ->
            changeSong(
                song,
                mediaPlayer,
                changeMusic,
                spCode,
                errorAuxImage
            ) { newMediaPlayer, newErrorImage ->
                mediaPlayer = newMediaPlayer
                errorAuxImage = newErrorImage
            }
        }
    }

    // Release media player on dispose
    DisposableEffect(Unit) {
        onDispose {
            releaseMediaPlayer(mediaPlayer) { mediaPlayer = null }
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(androidx.compose.ui.graphics.Color.Black.copy(alpha = backgroundAlpha))
        ) {
            Card(
                modifier = Modifier
                    .fillMaxWidth(0.9f)
                    .fillMaxHeight(0.95f)
                    .align(Alignment.Center),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Box(modifier = Modifier.fillMaxSize()) {
                    // Background video/image
                    AndroidView(
                        factory = { context ->
                            VideoView(context).apply {
                                setOnPreparedListener { mediaPlayer ->
                                    mediaPlayer.isLooping = true
                                    mediaPlayer.setVolume(0f, 0f)
                                }
                            }
                        },
                        modifier = Modifier.fillMaxSize()
                    ) { videoView ->
                        currentSong?.let { song ->
                            val video = File(song.PATH_SONG + "/" + song.PREVIEWVID)
                            if (video.exists() && (video.path.endsWith(".mpg") ||
                                        video.path.endsWith(".mp4") || video.path.endsWith(".avi"))
                            ) {
                                videoView.background = null
                                videoView.setVideoPath(video.path)
                                videoView.start()
                            } else {
                                videoView.background = errorAuxImage
                            }
                        }
                    }

                    // Rotating hexagons
                    Icon(
                        painter = painterResource(R.drawable.ic_launcher_foreground), // Replace with actual hexagon
                        contentDescription = null,
                        modifier = Modifier
                            .size(100.dp)
                            .align(Alignment.TopStart)
                            .padding(16.dp)
                            .graphicsLayer { rotationZ = rotationAnimation },
                        tint = androidx.compose.ui.graphics.Color.White
                    )

                    Icon(
                        painter = painterResource(R.drawable.ic_launcher_foreground), // Replace with actual hexagon
                        contentDescription = null,
                        modifier = Modifier
                            .size(80.dp)
                            .align(Alignment.TopEnd)
                            .padding(16.dp)
                            .graphicsLayer { rotationZ = -rotationAnimation },
                        tint = androidx.compose.ui.graphics.Color.White
                    )

                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp)
                    ) {
                        // Header with song title and exit button
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = currentSong?.TITLE ?: "",
                                fontSize = 24.sp,
                                fontWeight = FontWeight.Bold,
                                color = androidx.compose.ui.graphics.Color.White
                            )

                            if (!loadingScreen) {
                                TextButton(
                                    onClick = onDismiss,
                                    colors = ButtonDefaults.textButtonColors(
                                        contentColor = androidx.compose.ui.graphics.Color.White
                                    )
                                ) {
                                    Text("Salir")
                                }
                            }
                        }

                        Spacer(modifier = Modifier.weight(1f))

                        // Menu options placeholder
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(200.dp)
                        ) {
                            // This would be your MenuOptionFragment equivalent
                            MenuOptionsCompose()
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        // Levels RecyclerView replacement
                        LazyRow(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            itemsIndexed(levels) { index, level ->
                                LevelCard(
                                    level = level,
                                    onClick = {
                                        scope.launch {
                                            try {
                                                val intent =
                                                    Intent(context, GamePlayActivity::class.java)
                                                releaseMediaPlayer(mediaPlayer) {
                                                    mediaPlayer = null
                                                }
                                                intent.putExtra("ssc", currentSong?.PATH_File)
                                                intent.putExtra("nchar", level.index)
                                                intent.putExtra("path", currentSong?.PATH_SONG)
                                                intent.putExtra(
                                                    "pathDisc",
                                                    currentSong?.PATH_SONG + currentSong?.BANNER_SONG
                                                )
                                                context.startActivity(intent)
                                            } catch (ex: Exception) {
                                                ex.printStackTrace()
                                            }
                                        }
                                    }
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        // Start button
                        if (!loadingScreen) {
                            StartButton(
                                onClick = { warningStartSong = true },
                                fadeAnimation = fadeAnimation
                            )
                        } else {
                            // Loading state
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(60.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                CircularProgressIndicator(
                                    color = MaterialTheme.colorScheme.primary
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
private fun MenuOptionsCompose() {
    // Placeholder for menu options - implement based on your MenuOptionFragment
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(androidx.compose.ui.graphics.Color.Black.copy(alpha = 0.3f)),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = "Menu Options",
            color = androidx.compose.ui.graphics.Color.White,
            fontSize = 16.sp
        )
    }
}

@Composable
private fun LevelCard(
    level: com.kyagamy.step.room.entities.Level,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .size(80.dp)
            .clickable { onClick() },
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primary)
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = level.index.toString(),
                color = androidx.compose.ui.graphics.Color.White,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
private fun StartButton(
    onClick: () -> Unit,
    fadeAnimation: Float
) {
    Button(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .height(60.dp)
            .alpha(fadeAnimation),
        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
    ) {
        Text(
            text = "INICIAR",
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

private suspend fun changeSong(
    song: Song,
    mediaPlayer: MediaPlayer?,
    changeMusic: SoundPool?,
    spCode: Int,
    errorAuxImage: BitmapDrawable?,
    onUpdate: (MediaPlayer?, BitmapDrawable?) -> Unit
) {
    changeMusic?.play(spCode, 1f, 1f, 1, 0, 1.0f)
    releaseMediaPlayer(mediaPlayer) { }

    try {
        val video = File(song.PATH_SONG + "/" + song.PREVIEWVID)
        val bg = File(song.PATH_SONG + "/" + song.BACKGROUND)
        val transparent: Bitmap

        playMusicPreview(song, onUpdate)

        if (video.exists() && (video.path.endsWith(".mpg") ||
                    video.path.endsWith(".mp4") || video.path.endsWith(".avi"))
        ) {
            transparent = TransformBitmap.makeTransparent(BitmapFactory.decodeFile(bg.path), 180)
            onUpdate(null, BitmapDrawable(transparent))
        } else {
            if (bg.exists() && bg.isFile) {
                transparent =
                    TransformBitmap.makeTransparent(BitmapFactory.decodeFile(bg.path), 180)
                onUpdate(null, BitmapDrawable(transparent))
            } else {
                // Handle no banner case
                onUpdate(null, null)
            }
        }
    } catch (e: FileNotFoundException) {
        e.printStackTrace()
    } catch (e: Exception) {
        e.printStackTrace()
    }
}

private fun releaseMediaPlayer(mediaPlayer: MediaPlayer?, onUpdate: () -> Unit) {
    try {
        mediaPlayer?.let {
            if (it.isPlaying) it.stop()
            it.release()
            onUpdate()
        }
    } catch (e: Exception) {
        e.printStackTrace()
    }
}

private suspend fun playMusicPreview(
    song: Song,
    onUpdate: (MediaPlayer?, BitmapDrawable?) -> Unit
) {
    val startTime = System.currentTimeMillis()
    val audio = File(song.PATH_SONG + "/" + song.MUSIC)
    val duration = song.SAMPLELENGTH * 1000 + 3000

    try {
        val newMediaPlayer = MediaPlayer().apply {
            setVolume(1f, 1f)
            setDataSource(audio.path)
            prepare()
            seekTo(song.SAMPLESTART.toInt() * 1000)
            start()
        }
        onUpdate(newMediaPlayer, null)

        while (newMediaPlayer.isPlaying) {
            try {
                delay(100)
                val timeLapsed = System.currentTimeMillis() - startTime
                if (timeLapsed >= duration) {
                    releaseMediaPlayer(newMediaPlayer) { onUpdate(null, null) }
                    break
                } else if (timeLapsed >= (duration - 3000)) {
                    val lapset = (100 - ((timeLapsed - (duration - 3000)) / 3000 * 100)) / 100
                    newMediaPlayer.setVolume(lapset.toFloat(), lapset.toFloat())
                }
            } catch (_: NullPointerException) {
                break
            }
        }
    } catch (_: Exception) {
        // Handle audio playback errors
    }
}