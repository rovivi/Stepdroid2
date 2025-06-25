package com.kyagamy.step.viewmodels

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import com.kyagamy.step.R

class StartViewModel(application: Application) : AndroidViewModel(application) {

    private val _permissionsGranted = MutableStateFlow(false)
    val permissionsGranted: StateFlow<Boolean> = _permissionsGranted.asStateFlow()

    fun setPermissionsResult(granted: Boolean) {
        _permissionsGranted.value = granted
    }

    fun isBasePathSet(): Boolean {
        val ctx = getApplication<Application>()
        val prefs = ctx.getSharedPreferences("pref", Context.MODE_PRIVATE)
        val value = prefs.getString(ctx.getString(R.string.base_path), null)
        return value != null
    }

    fun saveBasePath(path: String) {
        val ctx = getApplication<Application>()
        val prefs = ctx.getSharedPreferences("pref", Context.MODE_PRIVATE)
        prefs.edit().putString(ctx.getString(R.string.base_path), path).apply()
    }
}
