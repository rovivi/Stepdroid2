package com.kyagamy.step.ui.compose

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.kyagamy.step.room.entities.Song
import com.kyagamy.step.viewmodels.LevelViewModel
import com.kyagamy.step.ui.compose.SongImage
import com.kyagamy.step.viewmodels.SongViewModel

@Composable
fun StartMenuDialog(
    songId: Int,
    onDismiss: () -> Unit
) {
    val songViewModel: SongViewModel = viewModel()
    val songs by songViewModel.songById(songId).observeAsState(emptyList())

    if (songs.isNotEmpty()) {
        StartMenuDialog(
            song = songs[0],
            onDismiss = onDismiss,
            onSelect = {
                // Aquí se puede manejar la selección
                onDismiss()
            }
        )
    }
}

@Composable
fun StartMenuDialog(
    song: Song,
    onDismiss: () -> Unit,
    onSelect: () -> Unit
) {
    val levelViewModel: LevelViewModel = viewModel()
    val context = LocalContext.current

    val levels by levelViewModel.get(song.song_id).observeAsState(emptyList())

    var showVideo by remember { mutableStateOf(false) }
    var isVideoPlaying by remember { mutableStateOf(false) }

    // Calculate level ranges
    val singleLevels = levels.filter { it.STEPSTYPE.contains("single", ignoreCase = true) }
    val doubleLevels = levels.filter { it.STEPSTYPE.contains("double", ignoreCase = true) }

    val singleRange = if (singleLevels.isNotEmpty()) {
        "${singleLevels.minOf { it.METER }} - ${singleLevels.maxOf { it.METER }}"
    } else "N/A"

    val doubleRange = if (doubleLevels.isNotEmpty()) {
        "${doubleLevels.minOf { it.METER }} - ${doubleLevels.maxOf { it.METER }}"
    } else "N/A"

    // Switch to video after 2 seconds
    LaunchedEffect(Unit) {
        kotlinx.coroutines.delay(2000)
        showVideo = true
        isVideoPlaying = true
    }

    // Stop video when selecting
    LaunchedEffect(onSelect) {
        if (isVideoPlaying) {
            isVideoPlaying = false
        }
    }

    Dialog(
        onDismissRequest = {
            isVideoPlaying = false
            onDismiss()
        },
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.5f)) // 50% transparency
        ) {
            Column(
                modifier = Modifier.fillMaxSize()
            ) {
                // Top image section with 16:9 aspect ratio
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(16f / 9f)
                        .clip(RoundedCornerShape(bottomStart = 20.dp, bottomEnd = 20.dp))
                ) {
                    // Background image
                    SongImage(
                        song = song,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )

                    // Video preview overlay (if available)
                    if (showVideo && song.PREVIEWVID.isNotEmpty() && isVideoPlaying) {
                        val videoAlpha by animateFloatAsState(
                            targetValue = 0.8f,
                            animationSpec = tween(1000, easing = LinearEasing),
                            label = "video_alpha"
                        )

                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(Color.Black.copy(alpha = 0.3f))
                                .graphicsLayer {
                                    alpha = videoAlpha
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(
                                    text = "🎬",
                                    fontSize = 48.sp,
                                    color = Color.White
                                )
                                Text(
                                    text = "Video Preview Playing",
                                    fontSize = 16.sp,
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }

                    // Dark gradient overlay for text readability
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                brush = Brush.verticalGradient(
                                    colors = listOf(
                                        Color.Transparent,
                                        Color.Black.copy(alpha = 0.7f)
                                    )
                                )
                            )
                    )

                    // Close button
                    IconButton(
                        onClick = {
                            isVideoPlaying = false
                            onDismiss()
                        },
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(16.dp)
                            .size(48.dp)
                            .background(
                                Color.Black.copy(alpha = 0.6f),
                                shape = RoundedCornerShape(24.dp)
                            )
                    ) {
                        Icon(
                            Icons.Default.Close,
                            contentDescription = "Close",
                            tint = Color.White,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }

                // Song information section
                LazyColumn(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .background(Color.Black.copy(alpha = 0.9f))
                        .padding(24.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    item {
                        // Title and Artist
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = song.TITLE,
                                fontSize = 28.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                                textAlign = TextAlign.Center
                            )

                            if (song.SUBTITLE.isNotEmpty()) {
                                Text(
                                    text = song.SUBTITLE,
                                    fontSize = 18.sp,
                                    color = Color.White.copy(alpha = 0.8f),
                                    textAlign = TextAlign.Center
                                )
                            }

                            Text(
                                text = song.ARTIST,
                                fontSize = 20.sp,
                                color = Color.Cyan,
                                textAlign = TextAlign.Center,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }

                    item {
                        // Main song details
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = Color.Black.copy(alpha = 0.7f)
                            )
                        ) {
                            Column(
                                modifier = Modifier.padding(16.dp),
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceEvenly
                                ) {
                                    InfoItem("BPM", song.DISPLAYBPM, Color.Cyan)
                                    InfoItem("Genre", song.GENRE, Color.Magenta)
                                    InfoItem("Type", song.SONGTYPE, Color.Yellow)
                                }

                                if (song.VERSION.isNotEmpty()) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceEvenly
                                    ) {
                                        InfoItem("Version", song.VERSION, Color.Green)
                                        InfoItem("Category", song.SONGCATEGORY, Color.Red)
                                        if (song.CDTITLE.isNotEmpty()) {
                                            InfoItem("Album", song.CDTITLE, Color.White)
                                        }
                                    }
                                }
                            }
                        }
                    }

                    item {
                        // Level ranges
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = Color.Blue.copy(alpha = 0.2f)
                            )
                        ) {
                            Column(
                                modifier = Modifier.padding(16.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text(
                                    text = "Available Difficulties",
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White,
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier.fillMaxWidth()
                                )

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceEvenly
                                ) {
                                    LevelRangeItem("Single", singleRange, Color.Cyan)
                                    LevelRangeItem("Double", doubleRange, Color.Magenta)
                                }

                                Text(
                                    text = "Total Levels: ${levels.size}",
                                    fontSize = 14.sp,
                                    color = Color.White.copy(alpha = 0.7f),
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }
                        }
                    }

                    item {
                        // Sample time info
                        if (song.SAMPLESTART > 0 || song.SAMPLELENGTH > 0) {
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(16.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = Color.Green.copy(alpha = 0.2f)
                                )
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(16.dp),
                                    horizontalArrangement = Arrangement.SpaceEvenly
                                ) {
                                    InfoItem(
                                        "Sample Start",
                                        "${song.SAMPLESTART.toInt()}s",
                                        Color.Green
                                    )
                                    InfoItem(
                                        "Sample Length",
                                        "${song.SAMPLELENGTH.toInt()}s",
                                        Color.Green
                                    )
                                }
                            }
                        }
                    }
                }

                // Select button at bottom
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color.Black.copy(alpha = 0.9f))
                        .padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Button(
                        onClick = {
                            isVideoPlaying = false
                            onSelect()
                        },
                        modifier = Modifier
                            .fillMaxWidth(0.8f)
                            .height(60.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color.Cyan.copy(alpha = 0.8f)
                        ),
                        shape = RoundedCornerShape(30.dp)
                    ) {
                        Text(
                            text = "SELECT",
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun InfoItem(
    label: String,
    value: String,
    color: Color
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = label,
            fontSize = 12.sp,
            color = Color.White.copy(alpha = 0.7f)
        )
        Text(
            text = value,
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            color = color
        )
    }
}

@Composable
fun LevelRangeItem(
    type: String,
    range: String,
    color: Color
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = type,
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            color = color
        )
        Text(
            text = range,
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            color = Color.White
        )
    }
}

