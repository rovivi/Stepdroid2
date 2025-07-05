package com.kyagamy.step.views

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.Settings
import java.io.File
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.codekidlabs.storagechooser.StorageChooser
import com.kyagamy.step.BuildConfig
import com.kyagamy.step.ui.EvaluationActivity
import com.kyagamy.step.viewmodels.StartViewModel
import com.kyagamy.step.viewmodels.SongViewModel
import com.kyagamy.step.viewmodels.LevelViewModel
import com.kyagamy.step.ui.ui.theme.StepDroidTheme
import com.kyagamy.step.views.TestGLPlayerActivity
import kotlinx.coroutines.launch
import kotlin.random.Random

class StartActivity : ComponentActivity() {
    private val viewModel: StartViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            StepDroidTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    val navController = rememberNavController()
                    NavHost(navController, startDestination = "splash") {
                        composable("splash") {
                            SplashScreen {
                                navController.navigate("home") {
                                    popUpTo("splash") {
                                        inclusive = true
                                    }
                                }
                            }
                        }
                        composable("home") {
                            StartScreen(viewModel)
                        }
                    }
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        // Verificar permisos cuando se regresa a la actividad
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            viewModel.setPermissionsGranted(Environment.isExternalStorageManager())
        } else {
            val hasPermissions = if (Build.VERSION.SDK_INT >= 33) {
                arrayOf(
                    Manifest.permission.READ_MEDIA_AUDIO,
                    Manifest.permission.READ_MEDIA_IMAGES,
                    Manifest.permission.READ_MEDIA_VIDEO
                ).all {
                    ContextCompat.checkSelfPermission(
                        this,
                        it
                    ) == android.content.pm.PackageManager.PERMISSION_GRANTED
                }
            } else {
                arrayOf(
                    Manifest.permission.READ_EXTERNAL_STORAGE,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE
                ).all {
                    ContextCompat.checkSelfPermission(
                        this,
                        it
                    ) == android.content.pm.PackageManager.PERMISSION_GRANTED
                }
            }
            viewModel.setPermissionsGranted(hasPermissions)
        }
    }
}

@Composable
fun SplashScreen(onTimeout: () -> Unit) {
    LaunchedEffect(Unit) {
        kotlinx.coroutines.delay(1000)
        onTimeout()
    }
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        MaterialTheme.colorScheme.primary,
                        MaterialTheme.colorScheme.secondary
                    )
                )
            ),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = "StepDroid",
            style = MaterialTheme.typography.headlineMedium,
            color = Color.White,
            fontWeight = FontWeight.Bold
        )
    }
}

data class MenuItem(
    val title: String,
    val subtitle: String,
    val icon: ImageVector,
    val color: Color,
    val onClick: () -> Unit
)

@Composable
fun StartScreen(viewModel: StartViewModel) {
    val context = LocalContext.current
    val activity = context as Activity
    val state by viewModel.uiState.collectAsState()
    var showSettingsDialog by remember { mutableStateOf(false) }
    var showFileInfoDialog by remember { mutableStateOf(false) }

    val songViewModel = ViewModelProvider(activity as ComponentActivity)[SongViewModel::class.java]
    val levelViewModel =
        ViewModelProvider(activity as ComponentActivity)[LevelViewModel::class.java]

    val allSongs by songViewModel.allSong.observeAsState(emptyList())
    val allLevels by levelViewModel.allLevel.observeAsState(emptyList())
    val coroutineScope = rememberCoroutineScope()
    var isLoadingRandom by remember { mutableStateOf(false) }

    val manageStorageLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R && Environment.isExternalStorageManager()) {
            viewModel.setPermissionsGranted(true)
        } else {
            showSettingsDialog = true
        }
    }

    val permissionsLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { perms ->
        val allGranted = perms.values.all { it }
        if (allGranted) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R && !Environment.isExternalStorageManager()) {
                val intent = Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION).apply {
                    data = Uri.parse("package:" + BuildConfig.APPLICATION_ID)
                }
                manageStorageLauncher.launch(intent)
            } else {
                viewModel.setPermissionsGranted(true)
            }
        } else {
            val rationale = perms.entries.any {
                !it.value && ActivityCompat.shouldShowRequestPermissionRationale(
                    activity,
                    it.key
                )
            }
            if (rationale) {
                viewModel.showRationale(true)
            } else {
                showSettingsDialog = true
            }
        }
    }

    val permissions = if (Build.VERSION.SDK_INT >= 33) {
        arrayOf(
            Manifest.permission.READ_MEDIA_AUDIO,
            Manifest.permission.READ_MEDIA_IMAGES,
            Manifest.permission.READ_MEDIA_VIDEO
        )
    } else {
        arrayOf(
            Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.WRITE_EXTERNAL_STORAGE
        )
    }

    LaunchedEffect(Unit) {
        if (!state.permissionsGranted) {
            val notGranted = permissions.filter {
                ContextCompat.checkSelfPermission(
                    context,
                    it
                ) != android.content.pm.PackageManager.PERMISSION_GRANTED
            }
            if (notGranted.isNotEmpty()) {
                permissionsLauncher.launch(notGranted.toTypedArray())
            } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R && !Environment.isExternalStorageManager()) {
                val intent = Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION).apply {
                    data = Uri.parse("package:" + BuildConfig.APPLICATION_ID)
                }
                manageStorageLauncher.launch(intent)
            } else {
                viewModel.setPermissionsGranted(true)
            }
        }
    }

    if (state.showRationale) {
        AlertDialog(
            onDismissRequest = { viewModel.showRationale(false) },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.showRationale(false)
                    permissionsLauncher.launch(permissions)
                }) { Text("Reintentar") }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.showRationale(false) }) { Text("Cancelar") }
            },
            title = { Text("Permiso requerido") },
            text = { Text("Se necesitan permisos de almacenamiento para continuar") }
        )
    }

    if (showSettingsDialog) {
        AlertDialog(
            onDismissRequest = { showSettingsDialog = false },
            confirmButton = {
                TextButton(onClick = {
                    showSettingsDialog = false
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                        val intent =
                            Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION).apply {
                                data = Uri.parse("package:" + BuildConfig.APPLICATION_ID)
                            }
                        activity.startActivity(intent)
                    } else {
                        openAppSettings(activity)
                    }
                }) { Text("Abrir ajustes") }
            },
            dismissButton = {
                TextButton(onClick = { showSettingsDialog = false }) { Text("Cancelar") }
            },
            title = { Text("Permiso requerido") },
            text = {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                    Text("Activa 'Administrar todo el almacenamiento' para continuar")
                } else {
                    Text("Otorga permisos desde ajustes para continuar")
                }
            }
        )
    }

    if (showFileInfoDialog) {
        AlertDialog(
            onDismissRequest = { showFileInfoDialog = false },
            confirmButton = {
                TextButton(onClick = { showFileInfoDialog = false }) { Text("OK") }
            },
            dismissButton = {
                TextButton(onClick = {
                    showFileInfoDialog = false
                    context.startActivity(Intent(context, AddMediaFromLinkActivity::class.java))
                }) { Text("Ir a DS") }
            },
            title = { Text("Archivos no encontrados") },
            text = {
                Text(
                    if (state.basePath != null) {
                        val expectedPath =
                            "${state.basePath}${File.separator}stepdroid${File.separator}songs"
                        "No se encontraron archivos SSC en:\n$expectedPath\n\n" +
                                "Estructura esperada:\n" +
                                "$expectedPath${File.separator}[Categoría]${File.separator}[Canción]${File.separator}archivo.ssc\n\n" +
                                "Ejemplo:\n" +
                                "$expectedPath${File.separator}Pop${File.separator}My Song${File.separator}mysong.ssc\n\n" +
                                "Usa 'DS' para instalar archivos automáticamente o 'Reload Songs' para cargar las canciones."
                    } else {
                        "No se ha seleccionado una ruta base. Por favor, selecciona una carpeta donde colocar los archivos de StepMania."
                    }
                )
            }
        )
    }

    // Crear lista de elementos del menú
    val menuItems = listOf(
        MenuItem(
            title = "Start Game",
            subtitle = "Begin your dance journey",
            icon = Icons.Default.PlayArrow,
            color = MaterialTheme.colorScheme.primary
        ) {
            if (state.basePath != null && state.basePath!!.isNotEmpty()) {
                val songsPath = "${state.basePath}${File.separator}stepdroid${File.separator}songs"
                val songsFile = File(songsPath)

                if (songsFile.exists() && songsFile.isDirectory) {
                    val sscFiles = songsFile.walkTopDown()
                        .filter { it.isFile && it.extension.equals("ssc", true) }
                        .toList()

                    if (sscFiles.isNotEmpty()) {
                        context.startActivity(Intent(context, MainActivity::class.java))
                    } else {
                        showFileInfoDialog = true
                    }
                } else {
                    showFileInfoDialog = true
                }
            } else {
                showFileInfoDialog = true
            }
        },
        MenuItem(
            title = "Random Challenge",
            subtitle = "Random 500AV >19",
            icon = Icons.Default.Star,
            color = MaterialTheme.colorScheme.secondary
        ) {
            val songIdsWithHighLevel = allLevels
                .filter { level -> level.METER > 19 }
                .map { it.song_fkid }
                .toSet()

            val filteredSongs = allSongs.filter { song ->
                song.song_id in songIdsWithHighLevel
            }

            if (filteredSongs.isNotEmpty()) {
                isLoadingRandom = true
                coroutineScope.launch {
                    val randomSong = filteredSongs[Random.nextInt(filteredSongs.size)]
                    val sscPath = randomSong.PATH_File
                    val folderPath = randomSong.PATH_SONG
                    val intent = Intent(
                        context,
                        com.kyagamy.step.views.gameplayactivity.GamePlayActivity::class.java
                    ).apply {
                        putExtra("ssc", sscPath)
                        putExtra("path", folderPath)
                        putExtra("nchar", 0)
                    }
                    isLoadingRandom = false
                    context.startActivity(intent)
                }
            } else {
                showFileInfoDialog = true
            }
        },
        MenuItem(
            title = "Drag System",
            subtitle = "Practice your moves",
            icon = Icons.Default.Add,
            color = MaterialTheme.colorScheme.tertiary
        ) {
            context.startActivity(Intent(context, DragStepActivity::class.java))
        },
        MenuItem(
            title = "Reload Songs",
            subtitle = "Refresh your music library",
            icon = Icons.Default.Refresh,
            color = MaterialTheme.colorScheme.primary
        ) {
            context.startActivity(Intent(context, LoadingSongActivity::class.java))
        },
        MenuItem(
            title = "Download Songs",
            subtitle = "Get new music packs",
            icon = Icons.Default.Add,
            color = MaterialTheme.colorScheme.secondary
        ) {
            context.startActivity(Intent(context, AddMediaFromLinkActivity::class.java))
        },
        MenuItem(
            title = "Evaluation",
            subtitle = "Check your performance",
            icon = Icons.Default.Check,
            color = MaterialTheme.colorScheme.tertiary
        ) {
            context.startActivity(Intent(context, EvaluationActivity::class.java))
        },
        MenuItem(
            title = "Test GL Player",
            subtitle = "Graphics testing mode",
            icon = Icons.Default.Settings,
            color = MaterialTheme.colorScheme.primary
        ) {
            val songIdsWithHighLevel = allLevels
                .filter { level -> level.METER > 19 }
                .map { it.song_fkid }
                .toSet()

            val filteredSongs = allSongs.filter { song ->
                song.song_id in songIdsWithHighLevel
            }

            if (filteredSongs.isNotEmpty()) {
                isLoadingRandom = true
                coroutineScope.launch {
                    val randomSong = filteredSongs[Random.nextInt(filteredSongs.size)]
                    val sscPath = randomSong.PATH_File
                    val folderPath = randomSong.PATH_SONG
                    val intent = Intent(context, TestGLPlayerActivity::class.java).apply {
                        putExtra("ssc", sscPath)
                        putExtra("path", folderPath)
                        putExtra("nchar", 0)
                    }
                    isLoadingRandom = false
                    context.startActivity(intent)
                }
            } else {
                showFileInfoDialog = true
            }
        }
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        MaterialTheme.colorScheme.surface,
                        MaterialTheme.colorScheme.background
                    )
                )
            )
    ) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Header
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "🎵",
                        fontSize = 48.sp,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                    Text(
                        text = "StepDroid",
                        style = MaterialTheme.typography.headlineLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                        textAlign = TextAlign.Center
                    )
                    Text(
                        text = "Dance Revolution Experience",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
            }

            // Menu items
            items(menuItems.size) { index ->
                val item = menuItems[index]
                MenuItemCard(
                    menuItem = item,
                    isLoading = if (item.title == "Random Challenge" || item.title == "Test GL Player") isLoadingRandom else false
                )
            }
        }
    }

    LaunchedEffect(state.basePath) {
        if (state.basePath == null) {
            showStorageChooser(activity) { viewModel.saveBasePath(it) }
        }
    }
}

@Composable
fun MenuItemCard(
    menuItem: MenuItem,
    isLoading: Boolean = false
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .alpha(if (isLoading) 0.7f else 1f),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Button(
            onClick = if (isLoading) {
                {}
            } else menuItem.onClick,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = Color.Transparent,
                contentColor = MaterialTheme.colorScheme.onSurfaceVariant
            ),
            elevation = ButtonDefaults.buttonElevation(defaultElevation = 0.dp),
            contentPadding = PaddingValues(20.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Start,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Card(
                    modifier = Modifier.size(48.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = menuItem.color.copy(alpha = 0.15f)
                    )
                ) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        if (isLoading) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(24.dp),
                                color = menuItem.color,
                                strokeWidth = 2.dp
                            )
                        } else {
                            Icon(
                                imageVector = menuItem.icon,
                                contentDescription = null,
                                tint = menuItem.color,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.width(16.dp))

                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = if (isLoading) "Loading..." else menuItem.title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = menuItem.subtitle,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                    )
                }
            }
        }
    }
}

private fun showStorageChooser(activity: Activity, onSelect: (String) -> Unit) {
    val chooser = StorageChooser.Builder()
        .withActivity(activity)
        .withFragmentManager(activity.fragmentManager)
        .setDialogTitle("Choose Destination Folder")
        .withMemoryBar(true)
        .build()
    chooser.show()
    chooser.setOnSelectListener { path -> onSelect(path) }
}

private fun openManageStorage(activity: Activity) {
    val uri = Uri.parse("package:" + BuildConfig.APPLICATION_ID)
    val intent = Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION, uri)
    activity.startActivity(intent)
}

private fun openAppSettings(activity: Activity) {
    val intent = Intent(
        Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
        Uri.fromParts("package", activity.packageName, null)
    )
    activity.startActivity(intent)
}
