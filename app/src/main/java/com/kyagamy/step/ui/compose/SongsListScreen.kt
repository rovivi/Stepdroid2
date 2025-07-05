package com.kyagamy.step.ui.compose

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowDropDown
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
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.kyagamy.step.room.entities.Song
import com.kyagamy.step.viewmodels.SongViewModel
import com.kyagamy.step.viewmodels.LevelViewModel

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionLayout
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import com.kyagamy.step.views.InfoItem

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SongsListScreen(
    channel: String,
    onBack: () -> Unit,
    onSongClick: (Int) -> Unit
) {
    val songsModel: SongViewModel = viewModel()

    var selectedSortOption by remember { mutableStateOf(0) }
    var showDropdownMenu by remember { mutableStateOf(false) }
    var selectedSong by remember { mutableStateOf<Song?>(null) }
    var showSongDetail by remember { mutableStateOf(false) }

    val sortOptions = listOf("Name", "Artist", "BPM")

    val songs by when (selectedSortOption) {
        0 -> songsModel.songByCategory(channel).observeAsState(emptyList())
        1 -> songsModel.songByCategoryAuthor(channel).observeAsState(emptyList())
        2 -> songsModel.songByCategoryBPM(channel).observeAsState(emptyList())
        else -> songsModel.songByCategory(channel).observeAsState(emptyList())
    }

    // Simple transition without shared elements for now
    AnimatedContent(
        targetState = showSongDetail,
        label = "song_transition"
    ) { targetState ->
        if (!targetState) {
            // Songs List
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black)
            ) {
                TopAppBar(
                    title = { Text("Songs - $channel", color = Color.White) },
                    navigationIcon = {
                        IconButton(onClick = onBack) {
                            Icon(
                                Icons.Default.ArrowBack,
                                contentDescription = "Back",
                                tint = Color.White
                            )
                        }
                    },
                    actions = {
                        Box {
                            TextButton(
                                onClick = { showDropdownMenu = true }
                            ) {
                                Text(sortOptions[selectedSortOption], color = Color.White)
                                Icon(
                                    Icons.Default.ArrowDropDown,
                                    contentDescription = null,
                                    tint = Color.White
                                )
                            }
                            DropdownMenu(
                                expanded = showDropdownMenu,
                                onDismissRequest = { showDropdownMenu = false }
                            ) {
                                sortOptions.forEachIndexed { index, option ->
                                    DropdownMenuItem(
                                        text = { Text(option) },
                                        onClick = {
                                            selectedSortOption = index
                                            showDropdownMenu = false
                                        }
                                    )
                                }
                            }
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Black)
                )

                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(songs) { song ->
                        SimpleSongCard(
                            song = song,
                            onClick = {
                                selectedSong = song
                                showSongDetail = true
                            }
                        )
                    }
                }
            }
        } else {
            // Song Detail - simple version
            selectedSong?.let { song ->
                SimpleSongDetailScreen(
                    song = song,
                    onBack = {
                        showSongDetail = false
                        selectedSong = null
                    },
                    onSelect = {
                        showSongDetail = false
                        onSongClick(song.song_id)
                        selectedSong = null
                    }
                )
            }
        }
    }
}

@Composable
fun SimpleSongCard(
    song: Song,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(120.dp)
            .clickable { onClick() },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color.Transparent
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    brush = Brush.horizontalGradient(
                        colors = listOf(
                            Color.Blue.copy(alpha = 0.3f),
                            Color.Magenta.copy(alpha = 0.2f),
                            Color.Transparent
                        )
                    )
                )
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Song image
            Box(
                modifier = Modifier
                    .size(80.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color.Gray.copy(alpha = 0.3f))
            ) {
                SongImage(
                    song = song,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            }

            Spacer(modifier = Modifier.width(16.dp))

            // Song info
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = song.TITLE,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    maxLines = 1
                )

                Text(
                    text = song.ARTIST,
                    fontSize = 14.sp,
                    color = Color.White.copy(alpha = 0.8f),
                    maxLines = 1
                )

                Text(
                    text = "BPM: ${song.DISPLAYBPM}",
                    fontSize = 12.sp,
                    color = Color.White.copy(alpha = 0.6f)
                )
            }
        }
    }
}


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SimpleSongDetailScreen(
    song: Song,
    onBack: () -> Unit,
    onSelect: () -> Unit
) {
    val levelViewModel: LevelViewModel = viewModel()

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

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
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
                    onBack()
                },
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(16.dp)
                    .size(48.dp)
                    .background(
                        Color.Black.copy(alpha = 0.6f),
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

@Composable
fun LevelRangeItem(x0: String, x1: String, x2: Color) {
    TODO("Not yet implemented")
}

@Composable
fun SongImage(
    song: Song,
    modifier: Modifier = Modifier,
    contentScale: ContentScale = ContentScale.Crop
) {
    val context = LocalContext.current

    if (song.BANNER_SONG.isNotEmpty()) {
        AsyncImage(
            model = ImageRequest.Builder(context)
                .data(song.PATH_SONG + "/" + song.BANNER_SONG)
                .crossfade(true)
                .build(),
            contentDescription = song.TITLE,
            contentScale = contentScale,
            modifier = modifier
        )
    } else {
        Box(
            modifier = modifier.background(
                brush = Brush.radialGradient(
                    colors = listOf(
                        Color.Cyan.copy(alpha = 0.7f),
                        Color.Blue.copy(alpha = 0.5f)
                    )
                )
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
    }
}


