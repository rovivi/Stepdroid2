package com.kyagamy.step.workers

import android.annotation.SuppressLint
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.pm.PackageManager
import android.content.pm.ServiceInfo
import android.media.MediaCodecList
import android.media.MediaFormat
import android.net.Uri
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.net.toUri
import androidx.media3.common.MediaItem
import androidx.media3.common.MimeTypes
import androidx.media3.common.util.UnstableApi
import androidx.media3.transformer.Composition
import androidx.media3.transformer.EditedMediaItem
import androidx.media3.transformer.Effects
import androidx.media3.transformer.ExportException
import androidx.media3.transformer.ExportResult
import androidx.media3.transformer.Transformer
import androidx.work.CoroutineWorker
import androidx.work.ForegroundInfo
import androidx.work.WorkerParameters
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import android.util.Log
import androidx.work.WorkManager
import android.widget.Toast
import androidx.work.workDataOf

@RequiresApi(Build.VERSION_CODES.UPSIDE_DOWN_CAKE)
@OptIn(UnstableApi::class)
@Suppress("OPT_IN_USAGE", "EXPERIMENTAL_API_USAGE")
class Vp9EncodeWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    private fun initNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val manager = applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            val channel = NotificationChannel(
                NOTIFICATION_CHANNEL_ID,
                "Conversión de Videos VP9",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Muestra el progreso durante la conversión de videos a formato VP9"
                enableLights(false)
                enableVibration(false)
                setShowBadge(true)
            }
            manager.createNotificationChannel(channel)
        }
    }

    private fun hasNotificationPermission(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ActivityCompat.checkSelfPermission(
                applicationContext,
                android.Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            true
        }
    }

    @SuppressLint("MissingPermission", "NotificationPermission")
    private fun notifyIfPermitted(
        notificationManager: NotificationManagerCompat,
        id: Int,
        notification: androidx.core.app.NotificationCompat.Builder
    ) {
        if (hasNotificationPermission()) {
            notificationManager.notify(id, notification.build())
        }
    }

    @Suppress("EXPERIMENTAL_API_USAGE")
    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        // Mostrar un mensaje al usuario independientemente de los permisos
        withContext(Dispatchers.Main) {
            Toast.makeText(
                applicationContext,
                "Iniciando conversión a VP9. Verifica las notificaciones para ver el progreso.",
                Toast.LENGTH_LONG
            ).show()
        }

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
        val notificationManager = NotificationManagerCompat.from(applicationContext)

        val cancelIntent = WorkManager.getInstance(applicationContext)
            .createCancelPendingIntent(id)

        val builder = NotificationCompat.Builder(applicationContext, NOTIFICATION_CHANNEL_ID)
            .setContentTitle("Convirtiendo videos a VP9")
            .setContentText("Iniciando conversión...")
            .setSmallIcon(android.R.drawable.stat_sys_download)
            .setOnlyAlertOnce(true)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setProgress(videoFiles.size, 0, false)
            .setCategory(NotificationCompat.CATEGORY_PROGRESS)
            .addAction(android.R.drawable.ic_menu_close_clear_cancel, "Cancelar", cancelIntent)

        try {
            // Intenta crear un ForegroundInfo con el tipo DATA_SYNC
            val foregroundInfo = ForegroundInfo(
                NOTIFICATION_ID,
                builder.build(),
                ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC
            )
            setForeground(foregroundInfo)
        } catch (e: SecurityException) {
            // Si falla debido a permisos, continuamos en background
            // Registro el error pero no interrumpo el procesamiento
            Log.w("Vp9EncodeWorker", "No se pudo iniciar como foreground service: ${e.message}")
        }

        var processed = 0
        val startTime = System.currentTimeMillis()

        for (file in videoFiles) {
            try {
                // Enviar progreso inicial
                val progressData = workDataOf(
                    "current" to processed,
                    "total" to videoFiles.size,
                    "fileName" to file.name
                )
                setProgress(progressData)

                // Calcular el tiempo transcurrido y el tiempo estimado restante
                val currentTime = System.currentTimeMillis()
                val elapsedTimeMs = currentTime - startTime
                val elapsedSeconds = elapsedTimeMs / 1000

                var estimatedTotalTime = 0L
                var estimatedRemainingTime = 0L

                if (processed > 0) {
                    // Calcular tiempo estimado total basado en el tiempo promedio por archivo
                    val averageTimePerFile = elapsedTimeMs / processed
                    estimatedTotalTime = averageTimePerFile * videoFiles.size
                    estimatedRemainingTime = estimatedTotalTime - elapsedTimeMs
                }

                // Crear texto de tiempo para la notificación
                val timeText = if (processed > 0) {
                    val remainingMinutes = estimatedRemainingTime / 60000
                    val remainingSeconds = (estimatedRemainingTime % 60000) / 1000
                    "Tiempo rest: ${remainingMinutes}m ${remainingSeconds}s"
                } else {
                    "Tiempo: ${elapsedSeconds}s"
                }

                // Actualizar notificación con el archivo actual y tiempo
                val fileName = file.name
                val shortName = if (fileName.length > 20) "${fileName.take(17)}..." else fileName
                val progressText = "${processed + 1}/${videoFiles.size}: $shortName\n$timeText"

                updateNotification(
                    notificationManager,
                    builder,
                    processed,
                    videoFiles.size,
                    progressText
                )

                val output = File(file.parent, "${file.nameWithoutExtension}_tmp.${file.extension}")
                transcodeToVp9(file, output)
                if (output.exists()) {
                    file.delete()
                    output.renameTo(File(file.parent, file.name))
                }
                processed++

                // Enviar progreso actualizado
                val updatedProgressData = workDataOf(
                    "current" to processed,
                    "total" to videoFiles.size,
                    "fileName" to ""
                )
                setProgress(updatedProgressData)

                // Actualizar progreso después de cada archivo
                val elapsedTime = (System.currentTimeMillis() - startTime) / 1000
                val minutes = elapsedTime / 60
                val seconds = elapsedTime % 60
                val progressText2 = if (processed < videoFiles.size) {
                    "Procesado $processed/${videoFiles.size} - Tiempo: ${minutes}m ${seconds}s"
                } else {
                    "Procesamiento completado en ${minutes}m ${seconds}s"
                }
                updateNotification(
                    notificationManager,
                    builder,
                    processed,
                    videoFiles.size,
                    progressText2
                )

            } catch (e: Exception) {
                notifyIfPermitted(
                    notificationManager,
                    NOTIFICATION_ID,
                    builder.setOngoing(false)
                        .setProgress(0, 0, false)
                        .setContentText("Error: ${e.message}")
                )
                return@withContext Result.failure()
            }
        }

        // Calcular tiempo total final
        val totalTime = (System.currentTimeMillis() - startTime) / 1000
        val minutes = totalTime / 60
        val seconds = totalTime % 60

        // Notificación final
        notifyIfPermitted(
            notificationManager,
            NOTIFICATION_ID,
            builder.setContentTitle("Conversión a VP9 completada")
                .setContentText("$processed videos convertidos en ${minutes}m ${seconds}s")
                .setOngoing(false)
                .setProgress(0, 0, false)
        )
        Result.success()
    }

    /**
     * Actualiza la notificación con el progreso actual
     */
    private fun updateNotification(
        notificationManager: NotificationManagerCompat,
        builder: NotificationCompat.Builder,
        current: Int,
        total: Int,
        contentText: String
    ) {
        notifyIfPermitted(
            notificationManager,
            NOTIFICATION_ID,
            builder.setProgress(total, current, false)
                .setContentText(contentText)
        )
    }

    private fun isVp9Supported(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            MediaCodecList(MediaCodecList.ALL_CODECS).codecInfos.any { info ->
                info.isEncoder && info.supportedTypes.contains(MediaFormat.MIMETYPE_VIDEO_VP9)
            }
        } else false
    }

    @RequiresApi(Build.VERSION_CODES.LOLLIPOP)
    @Suppress("EXPERIMENTAL_API_USAGE")
    private suspend fun transcodeToVp9(input: File, output: File) {
        val completion = CompletableDeferred<Unit>()

        val transformer = Transformer.Builder(applicationContext)
            .setVideoMimeType(MimeTypes.VIDEO_VP9)
            .addListener(object : Transformer.Listener {
                override fun onCompleted(composition: Composition, exportResult: ExportResult) {
                    completion.complete(Unit)
                }

                override fun onError(
                    composition: Composition,
                    exportResult: ExportResult,
                    exportException: ExportException
                ) {
                    completion.completeExceptionally(exportException)
                }
            })
            .build()

        val inputMediaItem = MediaItem.fromUri(input.toUri())
        val editedMediaItem = EditedMediaItem.Builder(inputMediaItem).build()

        transformer.start(editedMediaItem, output.absolutePath)
        completion.await()
    }

    companion object {
        const val KEY_BASE_PATH = "base_path"
        private const val NOTIFICATION_CHANNEL_ID = "vp9_encode_channel"
        private const val NOTIFICATION_ID = 1001
    }
}

