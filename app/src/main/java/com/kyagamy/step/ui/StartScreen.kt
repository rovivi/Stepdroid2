package com.kyagamy.step.ui

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.codekidlabs.storagechooser.StorageChooser
import com.kyagamy.step.R
import com.kyagamy.step.viewmodels.StartViewModel
import com.kyagamy.step.views.MainActivity
import com.kyagamy.step.views.DragStepActivity
import com.kyagamy.step.views.LoadingSongActivity
import com.kyagamy.step.views.InstallFilesActivity
import com.kyagamy.step.ui.EvaluationActivity

@Composable
fun StartScreen(viewModel: StartViewModel, onNavigate: (Class<*>) -> Unit) {
    val context = LocalContext.current
    val activity = context as Activity
    val permissions = remember {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            arrayOf(Manifest.permission.MANAGE_EXTERNAL_STORAGE)
        } else {
            arrayOf(
                Manifest.permission.READ_EXTERNAL_STORAGE,
                Manifest.permission.WRITE_EXTERNAL_STORAGE
            )
        }
    }

    var showRationale by remember { mutableStateOf(false) }
    val launcher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { result ->
        val granted = result.all { it.value }
        viewModel.setPermissionsResult(granted)
        if (!granted) {
            val permanentlyDenied = permissions.all { !activity.shouldShowRequestPermissionRationale(it) }
            if (permanentlyDenied) {
                showRationale = true
            }
        }
    }

    LaunchedEffect(Unit) {
        launcher.launch(permissions)
    }

    if (showRationale) {
        AlertDialog(
            onDismissRequest = { showRationale = false },
            confirmButton = {
                TextButton(onClick = {
                    val intent = Intent(
                        Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                        Uri.fromParts("package", context.packageName, null)
                    )
                    context.startActivity(intent)
                    showRationale = false
                }) { Text(text = "Open Settings") }
            },
            dismissButton = {
                TextButton(onClick = { showRationale = false }) { Text("Cancel") }
            },
            title = { Text(text = "Permission required") },
            text = { Text("Storage permission is required for StepDroid") }
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterVertically),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Button(onClick = { onNavigate(MainActivity::class.java) }) {
            Text("Start")
        }
        Button(onClick = { onNavigate(DragStepActivity::class.java) }) {
            Text("Drag System")
        }
        Button(onClick = { onNavigate(LoadingSongActivity::class.java) }) {
            Text("Reload Songs")
        }
        Button(onClick = { onNavigate(InstallFilesActivity::class.java) }) {
            Text("DS")
        }
        Button(onClick = { onNavigate(EvaluationActivity::class.java) }) {
            Text("Evaluation")
        }
    }

    LaunchedEffect(Unit) {
        if (!viewModel.isBasePathSet()) {
            showStorageChooser(activity) { path ->
                viewModel.saveBasePath(path)
                onNavigate(LoadingSongActivity::class.java)
            }
        }
    }
}

private fun showStorageChooser(activity: Activity, onPath: (String) -> Unit) {
    val chooser = StorageChooser.Builder()
        .withActivity(activity)
        .withFragmentManager(activity.fragmentManager)
        .setDialogTitle("Choose Destination Folder")
        .withMemoryBar(true)
        .build()
    chooser.show()
    chooser.setOnSelectListener { path -> onPath(path) }
}
