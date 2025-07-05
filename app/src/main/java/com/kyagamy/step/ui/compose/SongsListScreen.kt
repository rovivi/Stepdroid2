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
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.togetherWith
import androidx.compose.animation.core.EaseInOutCubic
import androidx.compose.animation.core.EaseOutBounce
import androidx.compose.animation.core.EaseInBack
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring

@OptIn(ExperimentalMaterial3Api::class, ExperimentalSharedTransitionApi::class)
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

    // SharedTransitionLayout to enable shared element transitions
    SharedTransitionLayout {
        AnimatedContent(
            targetState = showSongDetail,
            transitionSpec = {
                if (!targetState) {
                    // Transition back to songs list (reverse animation)
                    slideInHorizontally(
                        initialOffsetX = { -it },
                        animationSpec = spring(
                            dampingRatio = Spring.DampingRatioMediumBouncy,
                            stiffness = Spring.StiffnessLow
                        )
                    ) + scaleIn(
                        initialScale = 0.85f,
                        animationSpec = spring(
                            dampingRatio = Spring.DampingRatioMediumBouncy,
                            stiffness = Spring.StiffnessLow
                        )
                    ) + fadeIn(
                        animationSpec = tween(500, easing = EaseInOutCubic)
                    ) togetherWith slideOutHorizontally(
                        targetOffsetX = { it },
                        animationSpec = spring(
                            dampingRatio = Spring.DampingRatioMediumBouncy,
                            stiffness = Spring.StiffnessLow
                        )
                    ) + scaleOut(
                        targetScale = 1.1f,
                        animationSpec = spring(
                            dampingRatio = Spring.DampingRatioMediumBouncy,
                            stiffness = Spring.StiffnessLow
                        )
                    ) + fadeOut(
                        animationSpec = tween(500, easing = EaseInOutCubic)
                    )
                } else {
                    // Transition to song detail (forward animation)
                    slideInHorizontally(
                        initialOffsetX = { it },
                        animationSpec = spring(
                            dampingRatio = Spring.DampingRatioMediumBouncy,
                            stiffness = Spring.StiffnessLow
                        )
                    ) + scaleIn(
                        initialScale = 1.1f,
                        animationSpec = spring(
                            dampingRatio = Spring.DampingRatioMediumBouncy,
                            stiffness = Spring.StiffnessLow
                        )
                    ) + fadeIn(
                        animationSpec = tween(500, easing = EaseInOutCubic)
                    ) togetherWith slideOutHorizontally(
                        targetOffsetX = { -it },
                        animationSpec = spring(
                            dampingRatio = Spring.DampingRatioMediumBouncy,
                            stiffness = Spring.StiffnessLow
                        )
                    ) + scaleOut(
                        targetScale = 0.85f,
                        animationSpec = spring(
                            dampingRatio = Spring.DampingRatioMediumBouncy,
                            stiffness = Spring.StiffnessLow
                        )
                    ) + fadeOut(
                        animationSpec = tween(500, easing = EaseInOutCubic)
                    )
                }
            },
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
                            SharedSongCard(
                                song = song,
                                sharedTransitionScope = this@SharedTransitionLayout,
                                animatedVisibilityScope = this@AnimatedContent,
                                onClick = {
                                    selectedSong = song
                                    showSongDetail = true
                                }
                            )
                        }
                    }
                }
            } else {
                // Song Detail - with shared elements
                selectedSong?.let { song ->
                    SharedSongDetailScreen(
                        song = song,
                        sharedTransitionScope = this@SharedTransitionLayout,
                        animatedVisibilityScope = this@AnimatedContent,
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
}

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun SharedSongCard(
    song: Song,
    sharedTransitionScope: SharedTransitionScope,
    animatedVisibilityScope: AnimatedVisibilityScope,
    onClick: () -> Unit
) {
    // Animation state for hover effect
    var isHovered by remember { mutableStateOf(false) }

    val hoverScale by animateFloatAsState(
        targetValue = if (isHovered) 1.02f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "hover_scale"
    )

    with(sharedTransitionScope) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .height(120.dp)
                .graphicsLayer {
                    scaleX = hoverScale
                    scaleY = hoverScale
                    alpha = 1f
                }
                .clickable {
                    isHovered = true
                    onClick()
                },
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
                // Song image with shared element and bounce animation
                Box(
                    modifier = Modifier
                        .size(80.dp)
                        .sharedElement(
                            sharedContentState = rememberSharedContentState(key = "${song.song_id}-image"),
                            animatedVisibilityScope = animatedVisibilityScope,
                            boundsTransform = { _, _ ->
                                spring(
                                    dampingRatio = Spring.DampingRatioMediumBouncy,
                                    stiffness = Spring.StiffnessMedium
                                )
                            },
                            placeHolderSize = SharedTransitionScope.PlaceHolderSize.animatedSize
                        )
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
                        maxLines = 1,
                        modifier = Modifier.sharedElement(
                            sharedContentState = rememberSharedContentState(key = "${song.song_id}-title"),
                            animatedVisibilityScope = animatedVisibilityScope,
                            boundsTransform = { _, _ ->
                                spring(
                                    dampingRatio = Spring.DampingRatioMediumBouncy,
                                    stiffness = Spring.StiffnessMedium
                                )
                            },
                            placeHolderSize = SharedTransitionScope.PlaceHolderSize.animatedSize
                        )
                    )

                    Text(
                        text = song.ARTIST,
                        fontSize = 14.sp,
                        color = Color.White.copy(alpha = 0.8f),
                        maxLines = 1,
                        modifier = Modifier.sharedElement(
                            sharedContentState = rememberSharedContentState(key = "${song.song_id}-artist"),
                            animatedVisibilityScope = animatedVisibilityScope,
                            boundsTransform = { _, _ ->
                                spring(
                                    dampingRatio = Spring.DampingRatioMediumBouncy,
                                    stiffness = Spring.StiffnessMedium
                                )
                            },
                            placeHolderSize = SharedTransitionScope.PlaceHolderSize.animatedSize
                        )
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
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalSharedTransitionApi::class)
@Composable
fun SharedSongDetailScreen(
    song: Song,
    sharedTransitionScope: SharedTransitionScope,
    animatedVisibilityScope: AnimatedVisibilityScope,
    onBack: () -> Unit,
    onSelect: () -> Unit
) {
    val levelViewModel: LevelViewModel = viewModel()
    val levels by levelViewModel.get(song.song_id).observeAsState(emptyList())

    var showVideo by remember { mutableStateOf(false) }
    var isVideoPlaying by remember { mutableStateOf(false) }
    
    // Animation states for content entrance
    var showContent by remember { mutableStateOf(false) }
    val contentAlpha by animateFloatAsState(
        targetValue = if (showContent) 1f else 0f,
        animationSpec = tween(800, delayMillis = 200, easing = EaseInOutCubic),
        label = "content_alpha"
    )
    
    val contentScale by animateFloatAsState(
        targetValue = if (showContent) 1f else 0.95f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "content_scale"
    )

    // Calculate level ranges
    val singleLevels = levels.filter { it.STEPSTYPE.contains("single", ignoreCase = true) }
    val doubleLevels = levels.filter { it.STEPSTYPE.contains("double", ignoreCase = true) }

    val singleRange = if (singleLevels.isNotEmpty()) {
        "${singleLevels.minOf { it.METER }} - ${singleLevels.maxOf { it.METER }}"
    } else "N/A"

    val doubleRange = if (doubleLevels.isNotEmpty()) {
        "${doubleLevels.minOf { it.METER }} - ${doubleLevels.maxOf { it.METER }}"
    } else "N/A"

    // Animation triggers
    LaunchedEffect(Unit) {
        showContent = true
        kotlinx.coroutines.delay(2000)
        showVideo = true
        isVideoPlaying = true
    }

    with(sharedTransitionScope) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black)
                .sharedBounds(
                    sharedContentState = rememberSharedContentState(key = "${song.song_id}-bounds"),
                    animatedVisibilityScope = animatedVisibilityScope,
                    enter = fadeIn(animationSpec = tween(600, easing = EaseInOutCubic)),
                    exit = fadeOut(animationSpec = tween(600, easing = EaseInOutCubic)),
                    boundsTransform = { _, _ ->
                        spring(
                            dampingRatio = Spring.DampingRatioMediumBouncy,
                            stiffness = Spring.StiffnessLow
                        )
                    },
                    resizeMode = SharedTransitionScope.ResizeMode.ScaleToBounds()
                )
        ) {
            // Top image section with 16:9 aspect ratio
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(16f / 9f)
                    .clip(RoundedCornerShape(bottomStart = 20.dp, bottomEnd = 20.dp))
            ) {
                // Background image with shared element
                SongImage(
                    song = song,
                    modifier = Modifier
                        .fillMaxSize()
                        .sharedElement(
                            sharedContentState = rememberSharedContentState(key = "${song.song_id}-image"),
                            animatedVisibilityScope = animatedVisibilityScope,
                            boundsTransform = { _, _ ->
                                spring(
                                    dampingRatio = Spring.DampingRatioMediumBouncy,
                                    stiffness = Spring.StiffnessMedium
                                )
                            },
                            placeHolderSize = SharedTransitionScope.PlaceHolderSize.animatedSize
                        ),
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

                // Close button with animated entrance
                IconButton(
                    onClick = {
                        isVideoPlaying = false
                        showContent = false
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
                        .graphicsLayer {
                            alpha = contentAlpha
                            scaleX = contentScale
                            scaleY = contentScale
                        }
                ) {
                    Icon(
                        Icons.Default.ArrowBack,
                        contentDescription = "Back",
                        tint = Color.White,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }

            // Song information section with animated entrance
            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .background(Color.Black.copy(alpha = 0.9f))
                    .padding(24.dp)
                    .graphicsLayer {
                        alpha = contentAlpha
                        scaleX = contentScale
                        scaleY = contentScale
                    },
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                item {
                    // Title and Artist with shared elements
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = song.TITLE,
                            fontSize = 28.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.sharedElement(
                                sharedContentState = rememberSharedContentState(key = "${song.song_id}-title"),
                                animatedVisibilityScope = animatedVisibilityScope,
                                boundsTransform = { _, _ ->
                                    spring(
                                        dampingRatio = Spring.DampingRatioMediumBouncy,
                                        stiffness = Spring.StiffnessMedium
                                    )
                                },
                                placeHolderSize = SharedTransitionScope.PlaceHolderSize.animatedSize
                            )
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
                            fontWeight = FontWeight.Medium,
                            modifier = Modifier.sharedElement(
                                sharedContentState = rememberSharedContentState(key = "${song.song_id}-artist"),
                                animatedVisibilityScope = animatedVisibilityScope,
                                boundsTransform = { _, _ ->
                                    spring(
                                        dampingRatio = Spring.DampingRatioMediumBouncy,
                                        stiffness = Spring.StiffnessMedium
                                    )
                                },
                                placeHolderSize = SharedTransitionScope.PlaceHolderSize.animatedSize
                            )
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

            // Select button at bottom with animated entrance
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color.Black.copy(alpha = 0.9f))
                    .padding(24.dp)
                    .graphicsLayer {
                        alpha = contentAlpha
                        scaleX = contentScale
                        scaleY = contentScale
                    },
                contentAlignment = Alignment.Center
            ) {
                Button(
                    onClick = {
                        isVideoPlaying = false
                        showContent = false
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

