package com.n7.vibeprivacy.features.main

import android.content.Intent
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.n7.vibeprivacy.services.MonitoringService
import com.n7.vibeprivacy.ui.theme.VibePrivacyTheme

@Composable
fun MainScreen() {
    val context = LocalContext.current
    var isServiceRunning by remember { mutableStateOf(false) } // This should ideally be observed from the service

    Scaffold(
        topBar = {
            TopAppBar(title = { Text("VibePrivacy") })
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = if (isServiceRunning) "Monitoring Service Active" else "Monitoring Service Inactive",
                style = MaterialTheme.typography.headlineMedium,
                modifier = Modifier.padding(bottom = 16.dp)
            )

            Button(
                onClick = {
                    if (isServiceRunning) {
                        context.stopService(Intent(context, MonitoringService::class.java))
                    } else {
                        context.startForegroundService(Intent(context, MonitoringService::class.java))
                    }
                    isServiceRunning = !isServiceRunning
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(text = if (isServiceRunning) "Stop Monitoring" else "Start Monitoring")
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun PreviewMainScreen() {
    VibePrivacyTheme {
        MainScreen()
    }
}
