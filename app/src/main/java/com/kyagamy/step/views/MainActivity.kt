package com.kyagamy.step.views

import android.content.DialogInterface
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.appcompat.app.AlertDialog
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.kyagamy.step.R
import com.kyagamy.step.ui.compose.SongDetailScreen
import com.kyagamy.step.ui.compose.SongsListScreen
import com.kyagamy.step.ui.ui.theme.StepDroidTheme
import com.kyagamy.step.viewmodels.CategoryViewModel
import com.kyagamy.step.viewmodels.SongViewModel
import com.kyagamy.step.utils.EdgeToEdgeHelper
import android.media.MediaPlayer
import android.view.KeyEvent
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import androidx.compose.ui.viewinterop.AndroidView
import android.widget.VideoView
import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionLayout
import androidx.compose.animation.core.rememberTransition
import androidx.compose.animation.core.Transition
import androidx.compose.runtime.livedata.observeAsState
import com.kyagamy.step.ui.compose.SongDetailScreen
import com.kyagamy.step.views.FullScreenActivity
import com.kyagamy.step.room.entities.Level
import com.kyagamy.step.room.entities.Song

class MainActivity : FullScreenActivity() {
    private var mediaPlayer: MediaPlayer? = null
    private var positionCategory = 2

    // Estado de navegación
    private val _navigationState = MutableStateFlow(NavigationState.Categories)
    private val navigationState: StateFlow<NavigationState> = _navigationState.asStateFlow()

    private val _selectedCategory = MutableStateFlow<String?>(null)
    private val selectedCategory: StateFlow<String?> = _selectedCategory.asStateFlow()

    private val _selectedSongId = MutableStateFlow<Int?>(null)
    private val selectedSongId: StateFlow<Int?> = _selectedSongId.asStateFlow()

    public override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Remove title bar completely
        supportActionBar?.hide()

        // EdgeToEdgeHelper setup
        EdgeToEdgeHelper.setupCustomEdgeToEdge(
            this,
            findViewById(android.R.id.content),
            applyTopInset = false,
            applyBottomInset = false
        )

        // Setup background video
        setupBackgroundVideo()

        setContent {
            StepDroidTheme {
                MainScreen(
                    navigationState = navigationState.collectAsStateWithLifecycle(),
                    selectedCategory = selectedCategory.collectAsStateWithLifecycle(),
                    selectedSongId = selectedSongId.collectAsStateWithLifecycle(),
                    onCategorySelected = { categoryName, position ->
                        changeCategory(categoryName, position)
                    },
                    onBackPressed = {
                        handleBackPress()
                    },
                    onSongSelected = { songId ->
                        showSongDetail(songId)
                    },
                    onSongDetailDismissed = {
                        _selectedSongId.value = null
                    },
                    onLevelSelected = { song, level ->
                        startGame(song, level)
                    }
                )
            }
        }
    }

    private fun setupBackgroundVideo() {
        val rawId = R.raw.ssmbg
        val path = "android.resource://$packageName/$rawId"

        try {
            mediaPlayer = MediaPlayer().apply {
                setDataSource(path)
                isLooping = true
                setVolume(0f, 0f)
                prepareAsync()
                setOnPreparedListener { start() }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    override fun onPause() {
        super.onPause()
        mediaPlayer?.pause()
    }

    override fun onStop() {
        super.onStop()
        mediaPlayer?.pause()
    }

    override fun onResume() {
        super.onResume()
        mediaPlayer?.start()
    }

    override fun onDestroy() {
        super.onDestroy()
        mediaPlayer?.release()
        mediaPlayer = null
    }

    private fun handleBackPress() {
        when (_navigationState.value) {
            NavigationState.Categories -> {
                // En categorías, mostrar diálogo de salida
                showExitDialog()
            }

            NavigationState.Songs -> {
                // En canciones, volver a categorías
                _navigationState.value = NavigationState.Categories
                _selectedCategory.value = null
            }
        }
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        if (keyCode == KeyEvent.KEYCODE_BACK) {
            handleBackPress()
            return true
        }
        return super.onKeyDown(keyCode, event)
    }

    private fun showExitDialog() {
        AlertDialog.Builder(this)
            .setIcon(android.R.drawable.ic_dialog_alert)
            .setTitle("Confirm")
            .setMessage("Are you sure you want to close StepDroid")
            .setPositiveButton("Yes") { _, _ -> finish() }
            .setNegativeButton("No", null)
            .show()
    }

    public fun changeCategory(category: String, categoryPosition: Int) {
        positionCategory = categoryPosition
        Toast.makeText(this, "Category: $category", Toast.LENGTH_SHORT).show()
        _selectedCategory.value = category
        _navigationState.value = NavigationState.Songs
    }

    public fun showFragmentCategory() {
        _navigationState.value = NavigationState.Categories
        _selectedCategory.value = null
    }

    private fun showSongDetail(songId: Int) {
        _selectedSongId.value = songId
    }

    private fun startGame(song: Song, level: Level) {
        try {
            val intent = Intent(this, TestGLPlayerActivity::class.java).apply {
                putExtra("ssc", song.PATH_File)
                putExtra("nchar", level.index)
                putExtra("path", song.PATH_SONG)
                putExtra("pathDisc", song.PATH_SONG + song.BANNER_SONG)
            }
            startActivity(intent)
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(this, "Error starting game: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }
}

enum class NavigationState {
    Categories,
    Songs
}

@Composable
fun MainScreen(
    navigationState: State<NavigationState>,
    selectedCategory: State<String?>,
    selectedSongId: State<Int?>,
    onCategorySelected: (String, Int) -> Unit,
    onBackPressed: () -> Unit,
    onSongSelected: (Int) -> Unit,
    onSongDetailDismissed: () -> Unit,
    onLevelSelected: (Song, Level) -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        // Background video placeholder
        BackgroundVideoView()

        // Main content with animations
        AnimatedContent(
            targetState = navigationState.value,
            transitionSpec = {
                slideInHorizontally(
                    initialOffsetX = { width -> if (targetState == NavigationState.Songs) width else -width },
                    animationSpec = tween(300)
                ) + fadeIn(animationSpec = tween(300)) togetherWith
                        slideOutHorizontally(
                            targetOffsetX = { width -> if (targetState == NavigationState.Songs) -width else width },
                            animationSpec = tween(300)
                        ) + fadeOut(animationSpec = tween(300))
            },
            label = "navigation_animation"
        ) { state ->
            when (state) {
                NavigationState.Categories -> {
                    CategoryScreen(
                        onCategorySelected = onCategorySelected
                    )
                }

                NavigationState.Songs -> {
                    selectedCategory.value?.let { category ->
                        SongsListScreen(
                            channel = category,
                            onBack = onBackPressed,
                            onSongClick = onSongSelected,
                            onLevelSelected = onLevelSelected
                        )
                    }
                }
            }
        }

        // Song detail screen
        selectedSongId.value?.let { songId ->
            SongDetailWrapper(
                songId = songId,
                onDismiss = onSongDetailDismissed,
                onLevelSelected = onLevelSelected
            )
        }
    }
}

@Composable
fun BackgroundVideoView() {
    val context = LocalContext.current
    AndroidView(
        factory = { ctx ->
            VideoView(ctx).apply {
                val rawId = R.raw.ssmbg
                val path = "android.resource://${ctx.packageName}/$rawId"
                setOnPreparedListener { mediaPlayer ->
                    mediaPlayer.isLooping = true
                    mediaPlayer.setVolume(0f, 0f)
                }
                setVideoURI(Uri.parse(path))
                start()
            }
        },
        modifier = Modifier.fillMaxSize()
    )
}

@Composable
fun CategoryScreen(
    onCategorySelected: (String, Int) -> Unit
) {
    val categoryViewModel: CategoryViewModel = viewModel()
    val songViewModel: SongViewModel = viewModel()

    CategoryCarousel(
        categoryViewModel = categoryViewModel,
        songViewModel = songViewModel,
        initialPosition = 2,
        onCategorySelected = { category, position ->
            onCategorySelected(category.name, position)
        },
        onCategoryChanged = { category ->
            // Aquí se puede manejar el cambio de categoría
            // Por ejemplo, reproducir sonido de la categoría
        }
    )
}

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun SongDetailWrapper(
    songId: Int,
    onDismiss: () -> Unit,
    onLevelSelected: (Song, Level) -> Unit
) {
    val songViewModel: SongViewModel = viewModel()
    val songs by songViewModel.songById(songId).observeAsState(emptyList())

    if (songs.isNotEmpty()) {
        val song = songs[0]

        // Create fullscreen overlay
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black)
        ) {
            SharedTransitionLayout {
                AnimatedContent(
                    targetState = true,
                    transitionSpec = {
                        fadeIn() togetherWith fadeOut()
                    },
                    label = "song_detail"
                ) { _ ->
                    SongDetailScreen(
                        song = song,
                        onBack = onDismiss,
                        onLevelSelect = { level ->
                            onLevelSelected(song, level)
                        },
                        animatedVisibilityScope = this@AnimatedContent
                    )
                }
            }
        }
    }
}