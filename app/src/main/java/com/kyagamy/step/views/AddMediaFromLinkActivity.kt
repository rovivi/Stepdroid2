package com.kyagamy.step.views

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Button
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.kyagamy.step.ui.ui.theme.StepDroidTheme

class AddMediaFromLinkActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            StepDroidTheme {
                AddMediaFromLinkScreen()
            }
        }
    }
}

@Composable
fun AddMediaFromLinkScreen(viewModel: AddMediaFromLinkViewModel = viewModel()) {
    val uiState by viewModel.uiState.collectAsState()
    var url by remember { mutableStateOf(TextFieldValue(uiState.url)) }

    LaunchedEffect(url.text) {
        viewModel.setUrl(url.text)
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .align(Alignment.Center)
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            OutlinedTextField(
                value = url,
                onValueChange = { url = it },
                label = { Text("URL (.zip)") },
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(16.dp))
            Button(onClick = { viewModel.startDownload(this@AddMediaFromLinkActivity) }, enabled = !uiState.isWorking) {
                Text("Add media from external link")
            }
            if (uiState.isWorking) {
                Spacer(modifier = Modifier.height(16.dp))
                LinearProgressIndicator(progress = uiState.progress, modifier = Modifier.fillMaxWidth())
                Spacer(modifier = Modifier.height(8.dp))
                Text(uiState.phaseLabel)
            }
            uiState.error?.let { error ->
                Spacer(modifier = Modifier.height(8.dp))
                Text(error)
            }
        }
    }
}
