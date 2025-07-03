package com.kyagamy.step.viewmodels

import android.app.Application
import android.content.Context
import android.os.Build
import androidx.core.content.ContextCompat
import androidx.lifecycle.AndroidViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import com.kyagamy.step.R
import android.Manifest
import android.content.pm.PackageManager
import android.os.Environment

data class StartUiState(
    val isReady: Boolean = false,
    val permissionsGranted: Boolean = false,
    val showRationale: Boolean = false,
    val basePath: String? = null
)

class StartViewModel(application: Application) : AndroidViewModel(application) {
    private val _uiState = MutableStateFlow(StartUiState())
    val uiState: StateFlow<StartUiState> = _uiState.asStateFlow()

    init {
        validateRoute()
        checkInitialPermissions()
    }

    private fun checkInitialPermissions() {
        val context = getApplication<Application>()
        val hasPermissions = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            Environment.isExternalStorageManager()
        } else if (Build.VERSION.SDK_INT >= 33) {
            arrayOf(
                Manifest.permission.READ_MEDIA_AUDIO,
                Manifest.permission.READ_MEDIA_IMAGES,
                Manifest.permission.READ_MEDIA_VIDEO
            ).all {
                ContextCompat.checkSelfPermission(
                    context,
                    it
                ) == PackageManager.PERMISSION_GRANTED
            }
        } else {
            arrayOf(
                Manifest.permission.READ_EXTERNAL_STORAGE,
                Manifest.permission.WRITE_EXTERNAL_STORAGE
            ).all {
                ContextCompat.checkSelfPermission(
                    context,
                    it
                ) == PackageManager.PERMISSION_GRANTED
            }
        }

        setPermissionsGranted(hasPermissions)
    }

    fun setPermissionsGranted(granted: Boolean) {
        _uiState.update { it.copy(permissionsGranted = granted) }
        if (granted) _uiState.update { it.copy(isReady = true) }
    }

    fun showRationale(show: Boolean) {
        _uiState.update { it.copy(showRationale = show) }
    }

    private fun validateRoute() {
        val context = getApplication<Application>()
        val prefs = context.getSharedPreferences("pref", Context.MODE_PRIVATE)
        val path = prefs.getString(context.getString(R.string.base_path), null)
        _uiState.update { it.copy(basePath = path) }
    }

    fun saveBasePath(path: String) {
        val context = getApplication<Application>()
        val prefs = context.getSharedPreferences("pref", Context.MODE_PRIVATE)
        prefs.edit().putString(context.getString(R.string.base_path), path).apply()
        _uiState.update { it.copy(basePath = path) }
    }
}
