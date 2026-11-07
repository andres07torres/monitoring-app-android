package com.example.inframonitor

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.work.Constraints
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.example.inframonitor.ui.screens.ConnectionScreen
import com.example.inframonitor.ui.screens.MainScreen
import com.example.inframonitor.ui.theme.InfraMonitorTheme
import com.example.inframonitor.viewmodel.ConnectionViewModel
import com.example.inframonitor.worker.AlertWorker
import java.util.concurrent.TimeUnit

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        
        scheduleAlertWorker()

        setContent {
            InfraMonitorTheme {
                val connectionViewModel: ConnectionViewModel = viewModel()
                val isConfigured by connectionViewModel.isConfigured.collectAsStateWithLifecycle()

                when (isConfigured) {
                    null -> {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator()
                        }
                    }
                    false -> {
                        val serverUrl by connectionViewModel.serverUrl.collectAsStateWithLifecycle()
                        val apiKey by connectionViewModel.apiKey.collectAsStateWithLifecycle()
                        ConnectionScreen(
                            initialUrl = serverUrl,
                            initialApiKey = apiKey,
                            onConfigSaved = { url, key ->
                                connectionViewModel.saveConfig(url, key)
                            }
                        )
                    }
                    true -> {
                        MainScreen(onLogout = {
                            connectionViewModel.logout()
                        })
                    }
                }
            }
        }
    }

    private fun scheduleAlertWorker() {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()

        val alertRequest = PeriodicWorkRequestBuilder<AlertWorker>(15, TimeUnit.MINUTES)
            .setConstraints(constraints)
            .build()

        WorkManager.getInstance(applicationContext).enqueueUniquePeriodicWork(
            "AlertWorker",
            androidx.work.ExistingPeriodicWorkPolicy.KEEP,
            alertRequest
        )
    }
}
