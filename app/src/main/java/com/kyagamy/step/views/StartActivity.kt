package com.kyagamy.step.views

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.Settings
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.codekidlabs.storagechooser.StorageChooser
import com.kyagamy.step.BuildConfig
import com.kyagamy.step.ui.EvaluationActivity
import com.kyagamy.step.viewmodels.StartViewModel
import com.kyagamy.step.ui.ui.theme.StepDroidTheme

class StartActivity : ComponentActivity() {
    private val viewModel: StartViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            StepDroidTheme {
                val navController = rememberNavController()
                NavHost(navController, startDestination = "splash") {
                    composable("splash") {
                        SplashScreen { navController.navigate("home") { popUpTo("splash") { inclusive = true } } }
                    }
                    composable("home") {
                        StartScreen(viewModel)
                    }
                }
            }
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

    val permissionsLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { perms ->
        val allGranted = perms.values.all { it }
        if (allGranted) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R && !Environment.isExternalStorageManager()) {
                openManageStorage(activity)
            } else {
                viewModel.setPermissionsGranted(true)
            }
        } else {
            val rationale = perms.entries.any { !it.value && ActivityCompat.shouldShowRequestPermissionRationale(activity, it.key) }
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
        val notGranted = permissions.filter { ContextCompat.checkSelfPermission(context, it) != android.content.pm.PackageManager.PERMISSION_GRANTED }
        if (notGranted.isNotEmpty()) {
            permissionsLauncher.launch(notGranted.toTypedArray())
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R && !Environment.isExternalStorageManager()) {
            openManageStorage(activity)
        } else {
            viewModel.setPermissionsGranted(true)
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
                    openAppSettings(activity)
                }) { Text("Abrir ajustes") }
            },
            dismissButton = {
                TextButton(onClick = { showSettingsDialog = false }) { Text("Cancelar") }
            },
            title = { Text("Permiso requerido") },
            text = { Text("Otorga permisos desde ajustes para continuar") }
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Button(onClick = { context.startActivity(Intent(context, MainActivity::class.java)) }) {
            Text("Start")
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
        Button(onClick = { context.startActivity(Intent(context, InstallFilesActivity::class.java)) }) {
            Text("DS")
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
