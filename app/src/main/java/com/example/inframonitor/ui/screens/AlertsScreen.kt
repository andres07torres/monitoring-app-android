package com.example.inframonitor.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.inframonitor.data.models.AlertResponse
import com.example.inframonitor.ui.UiState
import com.example.inframonitor.viewmodel.AlertsViewModel
import com.example.inframonitor.ui.theme.*

@Composable
fun AlertsScreen(viewModel: AlertsViewModel = viewModel()) {
    val uiState by viewModel.uiState.collectAsState()

    Box(modifier = Modifier.fillMaxSize().background(Background)) {
        when (val state = uiState) {
            is UiState.Loading -> {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.Center), color = Primary)
            }
            is UiState.Success -> {
                AlertsContent(state.data, onRefresh = { viewModel.fetchAlerts() })
            }
            is UiState.Error -> {
                Column(
                    modifier = Modifier.align(Alignment.Center).padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = state.message,
                        color = Error,
                        style = MaterialTheme.typography.bodyLarge
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(
                        onClick = { viewModel.fetchAlerts() },
                        colors = ButtonDefaults.buttonColors(containerColor = Primary),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Reintentar")
                    }
                }
            }
        }
    }
}

@Composable
fun AlertsContent(alerts: List<AlertResponse>, onRefresh: () -> Unit) {
    Column(modifier = Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier.padding(24.dp).fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    "Alertas del Sistema",
                    style = MaterialTheme.typography.headlineSmall.copy(
                        fontWeight = FontWeight.ExtraBold,
                        letterSpacing = (-0.5).sp,
                        color = TextPrimary
                    )
                )
                Text(
                    "Historial de incidentes y umbrales críticos",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextSecondary
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            com.example.inframonitor.ui.components.RefreshButton(onClick = onRefresh)
        }

        if (alerts.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("No hay alertas activas", color = TextMuted)
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(horizontal = 24.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                items(alerts) { alert ->
                    AlertItem(alert)
                }
            }
        }
    }
}

@Composable
fun AlertItem(alert: AlertResponse) {
    val color = when (alert.severity.uppercase()) {
        "HIGH", "CRITICAL" -> Error
        "MEDIUM", "WARNING" -> Warning
        "LOW", "SUCCESS" -> Success
        else -> Primary
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Surface),
        border = BorderStroke(1.dp, Outline.copy(alpha = 0.6f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(modifier = Modifier.height(IntrinsicSize.Min)) {
            Box(
                modifier = Modifier
                    .width(5.dp)
                    .fillMaxHeight()
                    .background(color)
            )
            Column(
                modifier = Modifier
                    .padding(16.dp)
                    .fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "ID: ${alert.id.uppercase()}",
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        color = TextPrimary
                    )
                    Surface(
                        color = color.copy(alpha = 0.12f),
                        shape = RoundedCornerShape(50),
                        border = BorderStroke(1.dp, color.copy(alpha = 0.25f))
                    ) {
                        val severityText = when (alert.severity.uppercase()) {
                            "HIGH", "CRITICAL" -> "CRÍTICA"
                            "MEDIUM", "WARNING" -> "ADVERTENCIA"
                            "LOW", "SUCCESS" -> "BAJA"
                            else -> alert.severity
                        }
                        Text(
                            severityText,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 11.sp
                            ),
                            color = color
                        )
                    }
                }
                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    alert.message,
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextPrimary.copy(alpha = 0.9f),
                    lineHeight = 20.sp
                )
                if (!alert.timestamp.isNullOrBlank()) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        alert.timestamp,
                        style = MaterialTheme.typography.labelSmall,
                        color = TextMuted,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }
    }
}

@androidx.compose.ui.tooling.preview.Preview(showBackground = true)
@Composable
fun AlertItemPreview() {
    InfraMonitorTheme {
        Box(modifier = Modifier.padding(16.dp)) {
            AlertItem(
                AlertResponse(
                    id = "A0",
                    severity = "LOW",
                    message = "Todos los sistemas operan dentro de los parámetros normales. Sin alertas.",
                    timestamp = null
                )
            )
        }
    }
}
