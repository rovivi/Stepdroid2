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
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
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
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text(text = "StepDroid", style = MaterialTheme.typography.headlineMedium)
    }
}

@Composable
fun StartScreen(viewModel: StartViewModel) {
    val context = LocalContext.current
    val activity = context as Activity
    val state by viewModel.uiState.collectAsState()
    var showSettingsDialog by remember { mutableStateOf(false) }
    var showFileInfoDialog by remember { mutableStateOf(false) }

    // SongViewModel y LevelViewModel para acceso a Room
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
        // Solo verificar permisos al inicio, no constantemente
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
                        // Para Android 11+, ir directamente a la configuración de administrar almacenamiento
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

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Button(onClick = {
            if (state.basePath != null && state.basePath!!.isNotEmpty()) {
                val songsPath = "${state.basePath}${File.separator}stepdroid${File.separator}songs"
                val songsFile = File(songsPath)

                if (songsFile.exists() && songsFile.isDirectory) {
                    // Buscar específicamente archivos SSC
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
        }) {
            Text("Start")
        }
        Spacer(Modifier.height(8.dp))

        // Botón para abrir una canción random usando Room, filtrando por niveles > 19
        Button(
            onClick = {
                // Filtrar canciones que tengan al menos un nivel > 19
                val songIdsWithHighLevel = allLevels
                    .filter { level ->
                        level.METER > 19
                    }
                    .map { it.song_fkid }
                    .toSet()

                val filteredSongs = allSongs.filter { song ->
                    song.song_id in songIdsWithHighLevel
                }

                if (filteredSongs.isNotEmpty()) {
                    isLoadingRandom = true
                    coroutineScope.launch {
                        // Elegir una canción random del filtro
                        val randomSong = filteredSongs[Random.nextInt(filteredSongs.size)]
                        val sscPath = randomSong.PATH_File
                        val folderPath = randomSong.PATH_SONG
                        val intent = Intent(
                            context,
                            com.kyagamy.step.views.gameplayactivity.GamePlayActivity::class.java
                        ).apply {
                            putExtra("ssc", sscPath)
                            putExtra("path", folderPath)
                            putExtra("nchar", 0) // Default character/level
                        }
                        isLoadingRandom = false
                        context.startActivity(intent)
                    }
                } else {
                    showFileInfoDialog = true
                }
            },
            enabled = !isLoadingRandom
        ) {
            Text(if (isLoadingRandom) "Loading..." else "Random 500AV >19")
        }

        Spacer(Modifier.height(8.dp))
        Button(onClick = { context.startActivity(Intent(context, DragStepActivity::class.java)) }) {
            Text("Drag System")
        }
        Spacer(Modifier.height(8.dp))
        Button(onClick = { context.startActivity(Intent(context, LoadingSongActivity::class.java)) }) {
            Text("Reload Songs")
        }
        Spacer(Modifier.height(8.dp))
        Button(onClick = { context.startActivity(Intent(context, AddMediaFromLinkActivity::class.java)) }) {
            Text("DS")
        }
        Spacer(Modifier.height(8.dp))
        Button(onClick = { context.startActivity(Intent(context, TestGLPlayerActivity::class.java)) }) {
            Text("Test GL Player")
        }
        Spacer(Modifier.height(8.dp))
        Button(onClick = { context.startActivity(Intent(context, EvaluationActivity::class.java)) }) {
            Text("Evaluation")
        }
    }

    LaunchedEffect(state.basePath) {
        if (state.basePath == null) {
            showStorageChooser(activity) { viewModel.saveBasePath(it) }
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
