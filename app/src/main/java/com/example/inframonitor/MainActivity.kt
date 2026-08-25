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
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.inframonitor.ui.screens.ConnectionScreen
import com.example.inframonitor.ui.screens.MainScreen
import com.example.inframonitor.ui.theme.InfraMonitorTheme
import com.example.inframonitor.viewmodel.ConnectionViewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            InfraMonitorTheme {
                val connectionViewModel: ConnectionViewModel = viewModel()
                val isConfigured by connectionViewModel.isConfigured.collectAsState()

                when (isConfigured) {
                    null -> {
                        // Cargando estado inicial de DataStore
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator()
                        }
                    }
                    false -> {
                        ConnectionScreen(onUrlSaved = { url ->
                            connectionViewModel.saveUrl(url)
                        })
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
}
