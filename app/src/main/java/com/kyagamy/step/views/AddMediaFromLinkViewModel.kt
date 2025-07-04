package com.kyagamy.step.views

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.FileOutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipFile

enum class DownloadPhase { IDLE, DOWNLOADING, EXTRACTING, DONE }

data class AddMediaUiState(
    val url: String = "",
    val progress: Float = 0f,
    val phase: DownloadPhase = DownloadPhase.IDLE,
    val isWorking: Boolean = false,
    val error: String? = null
) {
    val phaseLabel: String
        get() = when (phase) {
            DownloadPhase.DOWNLOADING -> "Downloading"
            DownloadPhase.EXTRACTING -> "Extracting"
            DownloadPhase.DONE -> "Completed"
            else -> ""
        }
}

class AddMediaFromLinkViewModel(application: Application) : AndroidViewModel(application) {
    private val _uiState = MutableStateFlow(AddMediaUiState())
    val uiState: StateFlow<AddMediaUiState> = _uiState.asStateFlow()

    fun setUrl(url: String) {
        _uiState.update { it.copy(url = url) }
    }

    fun startDownload(context: Context) {
        val url = uiState.value.url
        if (!url.endsWith(".zip", true)) {
            _uiState.update { it.copy(error = "Only zip files are supported") }
            return
        }
        viewModelScope.launch(Dispatchers.IO) {
            try {
                _uiState.update { it.copy(isWorking = true, phase = DownloadPhase.DOWNLOADING, progress = 0f, error = null) }
                val client = OkHttpClient()
                val request = Request.Builder().url(url).build()
                val response = client.newCall(request).execute()
                val body = response.body ?: throw Exception("Empty body")
                val contentLength = body.contentLength()
                val tempFile = File(context.cacheDir, "download.zip")
                FileOutputStream(tempFile).use { out ->
                    var downloaded = 0L
                    val buffer = ByteArray(8 * 1024)
                    body.byteStream().use { input ->
                        while (true) {
                            val read = input.read(buffer)
                            if (read == -1) break
                            out.write(buffer, 0, read)
                            downloaded += read
                            if (contentLength > 0) {
                                val prog = 0.5f * downloaded.toFloat() / contentLength
                                _uiState.update { it.copy(progress = prog) }
                            }
                        }
                    }
                }
                _uiState.update { it.copy(phase = DownloadPhase.EXTRACTING, progress = 0.5f) }
                val prefs = context.getSharedPreferences("pref", Context.MODE_PRIVATE)
                val basePath = prefs.getString(context.getString(com.kyagamy.step.R.string.base_path), null)
                    ?: throw Exception("Base path not set")
                val destDir = File(basePath + File.separator + "stepdroid" + File.separator + "songs")
                if (!destDir.exists()) destDir.mkdirs()
                unzipWithProgress(tempFile, destDir) { percent ->
                    val prog = 0.5f + 0.5f * percent
                    _uiState.update { it.copy(progress = prog) }
                }
                tempFile.delete()
                _uiState.update { it.copy(isWorking = false, phase = DownloadPhase.DONE, progress = 1f) }
            } catch (e: Exception) {
                _uiState.update { it.copy(isWorking = false, error = e.message) }
            }
        }
    }

    private fun unzipWithProgress(zipFile: File, targetDir: File, onProgress: (Float) -> Unit) {
        val zip = ZipFile(zipFile)
        val entries = zip.entries().asSequence().toList()
        val total = entries.size
        var count = 0
        for (entry in entries) {
            val outFile = File(targetDir, entry.name)
            if (entry.isDirectory) {
                outFile.mkdirs()
            } else {
                outFile.parentFile?.mkdirs()
                zip.getInputStream(entry).use { input ->
                    FileOutputStream(outFile).use { output ->
                        input.copyTo(output)
                    }
                }
            }
            count++
            onProgress(count.toFloat() / total)
        }
        zip.close()
    }
}
