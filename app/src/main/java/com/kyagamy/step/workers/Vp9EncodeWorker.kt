package com.kyagamy.step.workers

import android.content.Context
import android.media.MediaCodecList
import android.media.MediaFormat
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

class Vp9EncodeWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        val basePath = inputData.getString(KEY_BASE_PATH) ?: return@withContext Result.failure()

        if (!isVp9Supported()) {
            return@withContext Result.failure()
        }

        val songsDir = File(basePath, "stepdroid${File.separator}songs")
        if (!songsDir.exists()) {
            return@withContext Result.failure()
        }

        val videoExtensions = setOf("mp4", "mkv", "webm", "avi", "mov")

        songsDir.walkTopDown().filter { it.isFile && it.extension.lowercase() in videoExtensions }
            .forEach { file ->
                try {
                    val output = File(file.parent, "${file.nameWithoutExtension}_tmp.${file.extension}")
                    transcodeToVp9(file, output)
                    if (output.exists()) {
                        file.delete()
                        output.renameTo(File(file.parent, file.name))
                    }
                } catch (_: Exception) {
                    return@withContext Result.failure()
                }
            }

        Result.success()
    }

    private fun isVp9Supported(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            MediaCodecList(MediaCodecList.ALL_CODECS).codecInfos.any { info ->
                info.isEncoder && info.supportedTypes.contains(MediaFormat.MIMETYPE_VIDEO_VP9)
            }
        } else false
    }

    @RequiresApi(Build.VERSION_CODES.LOLLIPOP)
    private fun transcodeToVp9(input: File, output: File) {
        // Stub implementation. Real transcoding using MediaCodec should be placed here.
        // This method intentionally left incomplete as full video transcoding is beyond scope.
        throw NotImplementedError("Video transcoding not implemented")
    }

    companion object {
        const val KEY_BASE_PATH = "base_path"
    }
}

