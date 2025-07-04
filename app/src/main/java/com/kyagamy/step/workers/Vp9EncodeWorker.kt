package com.kyagamy.step.workers

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.media.MediaCodecList
import android.media.MediaFormat
import android.net.Uri
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.core.app.NotificationCompat
import androidx.core.net.toUri
import androidx.media3.common.MediaItem
import androidx.media3.common.MimeTypes
import androidx.media3.transformer.TransformationException
import androidx.media3.transformer.TransformationRequest
import androidx.media3.transformer.TransformationResult
import androidx.media3.transformer.Transformer
import androidx.work.CoroutineWorker
import androidx.work.ForegroundInfo
import androidx.work.WorkerParameters
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

class Vp9EncodeWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    private fun initNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val manager = applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            val channel = NotificationChannel(
                NOTIFICATION_CHANNEL_ID,
                "VP9 Encoding",
                NotificationManager.IMPORTANCE_LOW
            )
            manager.createNotificationChannel(channel)
        }
    }

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
        val videoFiles = songsDir.walkTopDown()
            .filter { it.isFile && it.extension.lowercase() in videoExtensions }
            .toList()

        if (videoFiles.isEmpty()) return@withContext Result.success()

        initNotificationChannel()
        val notificationManager = applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val builder = NotificationCompat.Builder(applicationContext, NOTIFICATION_CHANNEL_ID)
            .setContentTitle("Converting videos to VP9")
            .setSmallIcon(android.R.drawable.stat_sys_download)
            .setOnlyAlertOnce(true)
            .setOngoing(true)
            .setProgress(videoFiles.size, 0, false)
        setForeground(ForegroundInfo(NOTIFICATION_ID, builder.build()))

        var processed = 0
        for (file in videoFiles) {
            try {
                val output = File(file.parent, "${file.nameWithoutExtension}_tmp.${file.extension}")
                transcodeToVp9(file, output)
                if (output.exists()) {
                    file.delete()
                    output.renameTo(File(file.parent, file.name))
                }
            } catch (_: Exception) {
                notificationManager.notify(NOTIFICATION_ID, builder.setOngoing(false).setProgress(0,0,false).setContentText("Error").build())
                return@withContext Result.failure()
            }
            processed++
            notificationManager.notify(NOTIFICATION_ID, builder.setProgress(videoFiles.size, processed, false).build())
        }

        notificationManager.notify(NOTIFICATION_ID, builder.setContentText("Completed").setOngoing(false).setProgress(0,0,false).build())
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
    private suspend fun transcodeToVp9(input: File, output: File) {
        val completion = CompletableDeferred<Unit>()
        val request = TransformationRequest.Builder()
            .setVideoMimeType(MimeTypes.VIDEO_VP9)
            .build()
        val transformer = Transformer.Builder(applicationContext)
            .setTransformationRequest(request)
            .addListener(object : Transformer.Listener {
                override fun onTransformationCompleted(mediaItem: MediaItem, result: TransformationResult) {
                    completion.complete(Unit)
                }

                override fun onTransformationError(mediaItem: MediaItem, exception: TransformationException) {
                    completion.completeExceptionally(exception)
                }
            })
            .build()

        val mediaItem = MediaItem.fromUri(input.toUri())
        transformer.start(mediaItem, output.absolutePath)
        completion.await()
    }

    companion object {
        const val KEY_BASE_PATH = "base_path"
        private const val NOTIFICATION_CHANNEL_ID = "vp9_encode_channel"
        private const val NOTIFICATION_ID = 1001
    }
}

