package com.example.inframonitor.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.inframonitor.data.models.ContainerResponse
import com.example.inframonitor.data.models.SystemStatsResponse
import com.example.inframonitor.ui.UiState
import com.example.inframonitor.viewmodel.DashboardData
import com.example.inframonitor.viewmodel.DashboardViewModel
import com.example.inframonitor.ui.theme.*
import java.util.Locale

@Composable
fun DashboardScreen(viewModel: DashboardViewModel = viewModel()) {
    val uiState by viewModel.uiState.collectAsState()

    Box(modifier = Modifier.fillMaxSize().background(Background)) {
        when (val state = uiState) {
            is UiState.Loading -> {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.Center), color = Primary)
            }
            is UiState.Success -> {
                DashboardContent(state.data, onRefresh = { viewModel.refresh() })
            }
            is UiState.Error -> {
                Column(
                    modifier = Modifier.align(Alignment.Center).padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(text = state.message, color = Error)
                    Button(
                        onClick = { viewModel.refresh() }, 
                        modifier = Modifier.padding(top = 8.dp),
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
fun DashboardContent(data: DashboardData, onRefresh: () -> Unit) {
    val stats = data.stats
    val containers = data.containers
    val activeContainers = containers.filter { it.status == "running" }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        item {
            Row(
                modifier = Modifier.padding(top = 24.dp, start = 24.dp, end = 24.dp).fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        "Panel de Control",
                        style = MaterialTheme.typography.headlineSmall.copy(
                            fontWeight = FontWeight.ExtraBold,
                            letterSpacing = (-0.5).sp,
                            color = TextPrimary
                        )
                    )
                    Text(
                        "Métricas críticas del sistema en tiempo real", 
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextSecondary
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                com.example.inframonitor.ui.components.RefreshButton(onClick = onRefresh)
            }
        }

        item {
            Column(modifier = Modifier.padding(horizontal = 24.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                SectionHeader("Recursos del host")
                MetricCard(
                    title = "CPU", 
                    progress = (stats.cpu_percent.toFloat()) / 100f, 
                    percentage = "${stats.cpu_percent.toInt()}%", 
                    subLeft = "${stats.cpu_cores ?: 0} núcleos", 
                    subRight = String.format(Locale.getDefault(), "%.1f GHz prom.", stats.cpu_frequency ?: 0.0), 
                    progressColor = Primary
                )
                MetricCard(
                    title = "Memoria", 
                    progress = (stats.ram_percent.toFloat()) / 100f, 
                    percentage = "${stats.ram_percent.toInt()}%", 
                    subLeft = "${stats.ram_used_gb} GB usados", 
                    subRight = "${stats.ram_total_gb} GB total",
                    progressColor = Success
                )
                MetricCard(
                    title = "Almacenamiento", 
                    progress = (stats.disk_percent?.toFloat() ?: 0f) / 100f, 
                    percentage = "${stats.disk_percent?.toInt() ?: 0}%", 
                    subLeft = "${stats.disk_used_gb ?: 0.0} GB usados", 
                    subRight = "${stats.disk_total_gb ?: 0.0} GB total",
                    progressColor = Warning
                )
            }
        }

        item {
            Column(modifier = Modifier.padding(horizontal = 24.dp)) {
                SectionHeader("Servicios Activos")
                Spacer(modifier = Modifier.height(12.dp))
                if (activeContainers.isEmpty()) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = Surface),
                        border = CardDefaults.outlinedCardBorder()
                    ) {
                        Box(modifier = Modifier.padding(32.dp).fillMaxWidth(), contentAlignment = Alignment.Center) {
                            Text("No hay servicios activos", color = TextMuted)
                        }
                    }
                } else {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        activeContainers.forEach { container ->
                            ActiveServiceItem(container)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun SectionHeader(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleMedium.copy(
            fontWeight = FontWeight.ExtraBold,
            color = TextPrimary,
            letterSpacing = (-0.5).sp
        ),
        modifier = Modifier.padding(bottom = 4.dp)
    )
}

@Composable
fun MetricCard(
    title: String,
    progress: Float,
    percentage: String,
    subLeft: String,
    subRight: String,
    progressColor: Color
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Surface),
        border = CardDefaults.outlinedCardBorder(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(title, fontWeight = FontWeight.Bold, fontSize = 16.sp, color = TextPrimary)
                Text(percentage, fontWeight = FontWeight.ExtraBold, fontSize = 16.sp, color = progressColor)
            }
            Spacer(modifier = Modifier.height(16.dp))
            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .clip(CircleShape),
                color = progressColor,
                trackColor = progressColor.copy(alpha = 0.1f)
            )
            Spacer(modifier = Modifier.height(12.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(subLeft, style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                Text(subRight, style = MaterialTheme.typography.bodySmall, color = TextSecondary)
            }
        }
    }
}

@Composable
fun ActiveServiceItem(container: ContainerResponse) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Surface),
        border = CardDefaults.outlinedCardBorder(),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(10.dp)
                    .background(Success, CircleShape)
            )
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(container.name, fontWeight = FontWeight.Bold, fontSize = 15.sp, color = TextPrimary)
                Text("Contenedor Docker · Activo", style = MaterialTheme.typography.bodySmall, color = TextSecondary)
            }
            Text(
                "ACTIVO",
                style = MaterialTheme.typography.labelSmall.copy(
                    fontWeight = FontWeight.ExtraBold, 
                    color = Success,
                    letterSpacing = 1.sp
                )
            )
        }
    }
}
