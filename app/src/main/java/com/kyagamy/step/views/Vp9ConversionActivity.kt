package com.kyagamy.step.views

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.work.*
import com.kyagamy.step.workers.Vp9EncodeWorker
import com.kyagamy.step.ui.ui.theme.StepDroidTheme
import androidx.work.WorkInfo.State.*

class Vp9ConversionActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val basePath = intent.getStringExtra("basePath")

        setContent {
            StepDroidTheme {
                if (basePath != null) {
                    ConversionScreen(basePath = basePath)
                } else {
                    // Si no hay basePath, mostrar error
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = "Error",
                                tint = Color.Red,
                                modifier = Modifier.size(64.dp)
                            )
                            Text(
                                text = "Error: No se encontró la ruta base",
                                style = MaterialTheme.typography.headlineMedium
                            )
                            Button(onClick = { finish() }) {
                                Text("Volver")
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ConversionScreen(basePath: String) {
    val context = LocalContext.current
    var workInfo by remember { mutableStateOf<WorkInfo?>(null) }
    var isStarted by remember { mutableStateOf(false) }

    // Observar el estado del worker
    LaunchedEffect(Unit) {
        val workManager = WorkManager.getInstance(context)
        workManager.getWorkInfosForUniqueWorkLiveData("vp9_transcode").observeForever { workInfos ->
            workInfo = workInfos?.firstOrNull()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = "Conversión VP9",
                    style = MaterialTheme.typography.headlineMedium,
                    textAlign = TextAlign.Center
                )

                // Mostrar estado según el WorkInfo
                when (workInfo?.state) {
                    ENQUEUED -> {
                        Icon(
                            imageVector = Icons.Default.PlayArrow,
                            contentDescription = "En cola",
                            tint = Color.Blue,
                            modifier = Modifier.size(64.dp)
                        )
                        Text("En cola para procesamiento...")
                        CircularProgressIndicator()
                    }

                    RUNNING -> {
                        Icon(
                            imageVector = Icons.Default.PlayArrow,
                            contentDescription = "Procesando",
                            tint = Color.Blue,
                            modifier = Modifier.size(64.dp)
                        )
                        Text("Procesando videos...")
                        CircularProgressIndicator()

                        // Mostrar progreso si está disponible
                        workInfo?.progress?.let { progress ->
                            val current = progress.getInt("current", 0)
                            val total = progress.getInt("total", 0)
                            val fileName = progress.getString("fileName") ?: ""
                            val successful = progress.getInt("successful", 0)
                            val failed = progress.getInt("failed", 0)

                            if (total > 0) {
                                Text("Progreso: $current/$total")
                                LinearProgressIndicator(
                                    progress = current.toFloat() / total.toFloat(),
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }

                            if (fileName.isNotEmpty()) {
                                Text(
                                    text = "Archivo: $fileName",
                                    style = MaterialTheme.typography.bodySmall
                                )
                            }

                            if (successful > 0 || failed > 0) {
                                Text(
                                    text = "Éxitos: $successful | Fallos: $failed",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    }

                    SUCCEEDED -> {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = "Completado",
                            tint = Color.Green,
                            modifier = Modifier.size(64.dp)
                        )
                        Text("¡Conversión completada!")

                        // Mostrar estadísticas detalladas
                        workInfo?.outputData?.let { outputData ->
                            val totalProcessed = outputData.getInt("total_processed", 0)
                            val successfulCount = outputData.getInt("successful_count", 0)
                            val failedCount = outputData.getInt("failed_count", 0)
                            val processingTime = outputData.getString("processing_time") ?: ""
                            val successfulFiles = outputData.getString("successful_files") ?: ""
                            val failedFiles = outputData.getString("failed_files") ?: ""

                            Column(
                                modifier = Modifier.padding(top = 8.dp),
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Text(
                                    text = "Resumen de conversión:",
                                    style = MaterialTheme.typography.titleMedium
                                )
                                Text(
                                    text = "✅ Exitosos: $successfulCount",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = Color.Green
                                )
                                if (failedCount > 0) {
                                    Text(
                                        text = "❌ Fallidos: $failedCount",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = Color.Red
                                    )
                                }
                                if (processingTime.isNotEmpty()) {
                                    Text(
                                        text = "⏱️ Tiempo: $processingTime",
                                        style = MaterialTheme.typography.bodyMedium
                                    )
                                }

                                if (successfulFiles.isNotEmpty()) {
                                    Text(
                                        text = "Archivos convertidos:",
                                        style = MaterialTheme.typography.bodySmall,
                                        modifier = Modifier.padding(top = 8.dp)
                                    )
                                    Text(
                                        text = successfulFiles,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                }

                                if (failedFiles.isNotEmpty()) {
                                    Text(
                                        text = "Archivos que no se pudieron convertir:",
                                        style = MaterialTheme.typography.bodySmall,
                                        modifier = Modifier.padding(top = 8.dp)
                                    )
                                    Text(
                                        text = failedFiles,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = Color.Red
                                    )
                                }
                            }
                        }

                        Button(onClick = { (context as ComponentActivity).finish() }) {
                            Text("Volver")
                        }
                    }

                    FAILED -> {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = "Error",
                            tint = Color.Red,
                            modifier = Modifier.size(64.dp)
                        )
                        Text(
                            text = "¡Error en la conversión!",
                            style = MaterialTheme.typography.headlineMedium,
                            color = Color.Red
                        )

                        // Mostrar detalles del error
                        workInfo?.outputData?.let { outputData ->
                            val errorMessage = outputData.getString("error_message")
                            val errorDetails = outputData.getString("error_details")
                            val failedFile = outputData.getString("failed_file")
                            val errorCode = outputData.getInt("error_code", -1)

                            Column(
                                modifier = Modifier.padding(top = 8.dp),
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                if (errorMessage != null) {
                                    Text(
                                        text = "Error: $errorMessage",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = Color.Red
                                    )
                                }

                                if (failedFile != null) {
                                    Text(
                                        text = "Archivo que falló: $failedFile",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                }

                                if (errorCode != -1) {
                                    Text(
                                        text = "Código de error: $errorCode",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                }

                                if (errorDetails != null) {
                                    Text(
                                        text = "Detalles técnicos:",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        text = errorDetails,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurface,
                                        modifier = Modifier.padding(start = 8.dp)
                                    )
                                }

                                // Mensaje genérico si no hay detalles específicos
                                if (errorMessage == null && errorDetails == null && failedFile == null) {
                                    Text(
                                        text = "Error desconocido durante la conversión. Revisa los logs del sistema para más información.",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                }
                            }
                        }

                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.padding(top = 16.dp)
                        ) {
                            Button(onClick = { startConversion(context, basePath) }) {
                                Text("Reintentar")
                            }
                            Button(onClick = { (context as ComponentActivity).finish() }) {
                                Text("Volver")
                            }
                        }
                    }

                    CANCELLED -> {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = "Cancelado",
                            tint = Color.Gray,
                            modifier = Modifier.size(64.dp)
                        )
                        Text("Conversión cancelada")
                        Button(onClick = { (context as ComponentActivity).finish() }) {
                            Text("Volver")
                        }
                    }

                    else -> {
                        // Estado inicial - no hay trabajo ejecutándose
                        Icon(
                            imageVector = Icons.Default.PlayArrow,
                            contentDescription = "Iniciar",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(64.dp)
                        )
                        Text("Listo para convertir videos a VP9")
                        Button(
                            onClick = {
                                startConversion(context, basePath)
                                isStarted = true
                            },
                            enabled = !isStarted
                        ) {
                            Text("Iniciar Conversión")
                        }
                    }
                }

                // Botón para cancelar si está ejecutándose
                if (workInfo?.state == RUNNING || workInfo?.state == ENQUEUED) {
                    Button(
                        onClick = {
                            WorkManager.getInstance(context).cancelUniqueWork("vp9_transcode")
                        }
                    ) {
                        Text("Cancelar")
                    }
                }
            }
        }
    }
}

private fun startConversion(context: android.content.Context, basePath: String) {
    val data = workDataOf(
        Vp9EncodeWorker.KEY_BASE_PATH to basePath
    )
    val request = OneTimeWorkRequestBuilder<Vp9EncodeWorker>()
        .setInputData(data)
        .build()

    WorkManager.getInstance(context).enqueueUniqueWork(
        "vp9_transcode",
        ExistingWorkPolicy.REPLACE,
        request
    )
}