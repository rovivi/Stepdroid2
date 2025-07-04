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
import androidx.work.*
import com.kyagamy.step.workers.Vp9EncodeWorker
import com.kyagamy.step.ui.ui.theme.StepDroidTheme
import androidx.work.WorkInfo.State.*
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.graphics.Color

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
                        Text("Error en la conversión")
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
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
    val data = Data.Builder()
        .putString(Vp9EncodeWorker.KEY_BASE_PATH, basePath)
        .build()
    val request = OneTimeWorkRequestBuilder<Vp9EncodeWorker>()
        .setInputData(data)
        .build()

    WorkManager.getInstance(context).enqueueUniqueWork(
        "vp9_transcode",
        ExistingWorkPolicy.REPLACE,
        request
    )
}