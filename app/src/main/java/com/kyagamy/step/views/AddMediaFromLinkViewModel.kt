package com.kyagamy.step.views

import android.app.Application
import android.content.Context
import android.content.Intent
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
    val error: String? = null,
    val isPhoenixDownloaded: Boolean = false,
    val isPhoenix2Downloaded: Boolean = false,
    val isMobileDownloaded: Boolean = false,
    val isXXDownloaded: Boolean = false
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

    private val downloadPrefs =
        application.getSharedPreferences("download_prefs", Context.MODE_PRIVATE)

    fun checkForExistingDownloads() {
        val phoenix1Done = downloadPrefs.getBoolean("phoenix1_downloaded", false)
        val phoenix2Done = downloadPrefs.getBoolean("phoenix2_downloaded", false)
        val mobileDone = downloadPrefs.getBoolean("mobile_downloaded", false)
        val xxDone = downloadPrefs.getBoolean("xx_downloaded", false)
        _uiState.update {
            it.copy(
                isPhoenixDownloaded = phoenix1Done,
                isPhoenix2Downloaded = phoenix2Done,
                isMobileDownloaded = mobileDone,
                isXXDownloaded = xxDone
            )
        }
    }

    fun setUrl(url: String) {
        _uiState.update { it.copy(url = url) }
    }

    fun startDownload(context: Context) {
        val url = uiState.value.url
        if (!url.endsWith(".zip", true)) {
            _uiState.update { it.copy(error = "Only zip files are supported") }
            return
        }
        downloadAndExtract(context, url, null)
    }

    fun startPackDownload(context: Context, packUrl: String, packId: String) {
        downloadAndExtract(context, packUrl, packId)
    }

    private fun downloadAndExtract(context: Context, url: String, packId: String?) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                _uiState.update {
                    it.copy(
                        isWorking = true,
                        phase = DownloadPhase.DOWNLOADING,
                        progress = 0f,
                        error = null
                    )
                }
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
                val basePath =
                    prefs.getString(context.getString(com.kyagamy.step.R.string.base_path), null)
                        ?: throw Exception("Base path not set")
                val songsDir = File(basePath, "stepdroid/songs")
                var destDir = songsDir

                // Custom logic to handle different packs
                when (packId) {
                    "phoenix2" -> destDir = File(songsDir, "PHOENIX")
                    "mobile" -> destDir = songsDir
                    "xx" -> destDir = songsDir
                }

                if (!destDir.exists()) destDir.mkdirs()
                unzipWithProgress(tempFile, destDir) { percent ->
                    val prog = 0.5f + 0.5f * percent
                    _uiState.update { it.copy(progress = prog) }
                }
                tempFile.delete()
                packId?.let {
                    when (packId) {
                        "phoenix1" -> {
                            downloadPrefs.edit().putBoolean("phoenix1_downloaded", true).apply()
                            _uiState.update { it.copy(isPhoenixDownloaded = true) }
                        }

                        "phoenix2" -> {
                            downloadPrefs.edit().putBoolean("phoenix2_downloaded", true).apply()
                            _uiState.update { it.copy(isPhoenix2Downloaded = true) }
                        }

                        "mobile" -> {
                            downloadPrefs.edit().putBoolean("mobile_downloaded", true).apply()
                            _uiState.update { it.copy(isMobileDownloaded = true) }
                        }

                        "xx" -> {
                            downloadPrefs.edit().putBoolean("xx_downloaded", true).apply()
                            _uiState.update { it.copy(isXXDownloaded = true) }
                        }
                    }
                }
                _uiState.update {
                    it.copy(
                        isWorking = false,
                        phase = DownloadPhase.DONE,
                        progress = 1f
                    )
                }

                // Launch LoadingSongActivity after extraction
                val intent = Intent(context, LoadingSongActivity::class.java)
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                context.startActivity(intent)

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