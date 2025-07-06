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
import com.kyagamy.step.room.entities.Level
import com.kyagamy.step.viewmodels.SongViewModel
import com.kyagamy.step.viewmodels.LevelViewModel
import com.kyagamy.step.ui.compose.components.SongImage
import com.kyagamy.step.ui.compose.components.InfoItem
import com.kyagamy.step.ui.compose.components.LevelRangeItem

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
    onSongClick: (Int) -> Unit,
    onLevelSelected: (Song, Level) -> Unit = { _, _ -> }
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
                    with(this@SharedTransitionLayout) {
                        SongDetailScreen(
                            song = song,
                            onBack = {
                                showSongDetail = false
                                selectedSong = null
                            },
                            onLevelSelect = { level ->
                                showSongDetail = false
                                onLevelSelected(song, level)
                                selectedSong = null
                            },
                            animatedVisibilityScope = this@AnimatedContent
                        )
                    }
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
