package com.example.inframonitor

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.example.inframonitor.ui.screens.MainScreen
import com.example.inframonitor.ui.theme.InfraMonitorTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            InfraMonitorTheme {
                MainScreen()
            }
        }
    }
}
