package com.example.inframonitor.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.BugReport
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.inframonitor.ui.theme.*
import com.example.inframonitor.viewmodel.DiagnosticsViewModel

@Composable
fun DiagnosticsScreen(viewModel: DiagnosticsViewModel = viewModel()) {
    var command by remember { mutableStateOf("") }
    val output by viewModel.commandOutput.collectAsState()
    val isExecuting by viewModel.isExecuting.collectAsState()
    val healthStatus by viewModel.healthStatus.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp)
            .verticalScroll(rememberScrollState())
    ) {
        Text(
            "Diagnóstico y Salud",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.ExtraBold,
            color = TextPrimary
        )
        Text(
            "Monitoreo de endpoints y ejecución de comandos",
            style = MaterialTheme.typography.bodyMedium,
            color = TextSecondary,
            modifier = Modifier.padding(bottom = 24.dp)
        )

        // Health Checks Section
        Text(
            "Comprobación de Salud (HTTP)",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = TextPrimary,
            modifier = Modifier.padding(bottom = 8.dp)
        )
        
        HealthItem("API Gateway", "https://api.example.com/health", healthStatus, viewModel)
        HealthItem("Auth Service", "https://auth.example.com/check", healthStatus, viewModel)
        
        Spacer(modifier = Modifier.height(32.dp))

        // Terminal Section
        Text(
            "Consola de Diagnóstico",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = TextPrimary,
            modifier = Modifier.padding(bottom = 8.dp)
        )
        
        OutlinedTextField(
            value = command,
            onValueChange = { command = it },
            modifier = Modifier.fillMaxWidth(),
            placeholder = { Text("Ej: ping 8.8.8.8, curl -I localhost:8080") },
            trailingIcon = {
                IconButton(onClick = { viewModel.executeCommand(command) }, enabled = !isExecuting && command.isNotBlank()) {
                    if (isExecuting) CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                    else Icon(Icons.AutoMirrored.Filled.Send, contentDescription = "Ejecutar", tint = Primary)
                }
            },
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = Primary,
                unfocusedBorderColor = Outline
            ),
            shape = RoundedCornerShape(12.dp),
            singleLine = true
        )

        Spacer(modifier = Modifier.height(16.dp))

        Card(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 150.dp, max = 400.dp),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = Color.Black),
            border = androidx.compose.foundation.BorderStroke(1.dp, Outline)
        ) {
            Box(modifier = Modifier.padding(16.dp).verticalScroll(rememberScrollState())) {
                Text(
                    text = if (output.isEmpty()) "$ > _" else "$ $command\n\n$output",
                    color = Color.Green,
                    fontFamily = FontFamily.Monospace,
                    fontSize = 12.sp
                )
            }
        }
    }
}

@Composable
fun HealthItem(name: String, url: String, statusMap: Map<String, String>, viewModel: DiagnosticsViewModel) {
    val status = statusMap[url] ?: "Pendiente"
    val isOk = status.contains("OK")

    Surface(
        modifier = Modifier.padding(vertical = 4.dp).fillMaxWidth(),
        shape = RoundedCornerShape(8.dp),
        color = Surface,
        border = androidx.compose.foundation.BorderStroke(1.dp, Outline)
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                if (isOk) Icons.Default.CheckCircle else Icons.Default.BugReport,
                contentDescription = null,
                tint = if (isOk) Success else (if (status == "Pendiente") TextMuted else Error),
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(name, fontWeight = FontWeight.Bold, color = TextPrimary)
                Text(url, fontSize = 10.sp, color = TextSecondary)
            }
            Text(
                status,
                style = MaterialTheme.typography.labelSmall,
                color = if (isOk) Success else Error,
                fontWeight = FontWeight.Bold
            )
            IconButton(onClick = { viewModel.checkHealth(url) }) {
                Icon(Icons.AutoMirrored.Filled.Send, contentDescription = "Check", modifier = Modifier.size(16.dp), tint = Primary)
            }
        }
    }
}
