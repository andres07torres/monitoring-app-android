package com.example.inframonitor.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.inframonitor.data.models.ContainerResponse
import com.example.inframonitor.ui.UiState
import com.example.inframonitor.ui.theme.*
import com.example.inframonitor.viewmodel.DiagnosticsViewModel

@Composable
fun DiagnosticsScreen(viewModel: DiagnosticsViewModel = viewModel()) {
    val isExecuting by viewModel.isExecuting.collectAsState()
    val healthStatus by viewModel.healthStatus.collectAsState()
    val containersState by viewModel.containersState.collectAsState()
    val systemDetails by viewModel.systemDetails.collectAsState()
    val actionMessage by viewModel.actionMessage.collectAsState()

    var showSystemDetails by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp)
            .imePadding()
            .verticalScroll(rememberScrollState())
    ) {
        Spacer(modifier = Modifier.height(24.dp))
        
        HeaderSection(onRefresh = { viewModel.fetchContainers() })

        Spacer(modifier = Modifier.height(24.dp))

        // Health Checks Section
        SectionTitle("Comprobación de Salud (HTTP)")
        
        HealthStatusList(containersState, healthStatus, viewModel)
        
        Spacer(modifier = Modifier.height(32.dp))

        // Quick Actions Section
        SectionTitle("Acciones Rápidas")
        QuickActionsGrid(
            isExecuting = isExecuting,
            onAction = { viewModel.executeAction(it) }
        )

        actionMessage?.let {
            val isError = it.contains("Error", ignoreCase = true)
            Card(
                modifier = Modifier.padding(top = 16.dp).fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = if (isError) Error.copy(alpha = 0.1f) else PrimarySoft
                ),
                shape = RoundedCornerShape(12.dp),
                border = androidx.compose.foundation.BorderStroke(
                    1.dp, 
                    if (isError) Error.copy(alpha = 0.5f) else Primary.copy(alpha = 0.5f)
                )
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = if (isError) Icons.Default.ErrorOutline else Icons.Default.Info,
                        contentDescription = null,
                        tint = if (isError) Error else Primary,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = it,
                        style = MaterialTheme.typography.bodyMedium,
                        color = if (isError) Error else TextPrimary,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(32.dp))

        // System Info Section
        SystemDetailsSection(
            details = systemDetails,
            isExpanded = showSystemDetails,
            onExpandClick = { showSystemDetails = !showSystemDetails }
        )

        Spacer(modifier = Modifier.height(100.dp))
    }
}

@Composable
fun HeaderSection(onRefresh: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                "Diagnóstico y Salud",
                style = MaterialTheme.typography.headlineSmall.copy(
                    fontWeight = FontWeight.ExtraBold,
                    letterSpacing = (-0.5).sp,
                    color = TextPrimary
                )
            )
            Text(
                "Mantenimiento y estado detallado del host",
                style = MaterialTheme.typography.bodyMedium,
                color = TextSecondary
            )
        }
        Spacer(modifier = Modifier.width(12.dp))
        com.example.inframonitor.ui.components.RefreshButton(onClick = onRefresh)
    }
}

@Composable
fun SectionTitle(title: String) {
    Text(
        title,
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.Bold,
        color = TextPrimary,
        modifier = Modifier.padding(bottom = 12.dp)
    )
}

@Composable
fun HealthStatusList(
    state: UiState<List<ContainerResponse>>,
    healthStatus: Map<String, String>,
    viewModel: DiagnosticsViewModel
) {
    when (state) {
        is UiState.Loading -> {
            Box(modifier = Modifier.fillMaxWidth().height(100.dp), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(modifier = Modifier.size(24.dp))
            }
        }
        is UiState.Success -> {
            if (state.data.isEmpty()) {
                Text("No hay contenedores activos.", color = TextMuted, modifier = Modifier.padding(vertical = 16.dp))
            } else {
                state.data.forEach { container ->
                    HealthItem(container, healthStatus, viewModel)
                }
            }
        }
        is UiState.Error -> {
            Text("Error: ${state.message}", color = Error, modifier = Modifier.padding(vertical = 16.dp))
        }
    }
}

@Composable
fun QuickActionsGrid(isExecuting: Boolean, onAction: (String) -> Unit) {
    val actions = listOf(
        QuickActionItem("Limpiar Caché", "docker_prune", Icons.Default.DeleteSweep, Color(0xFFEF5350)),
        QuickActionItem("Reiniciar", "restart_orchestrator", Icons.Default.RestartAlt, Color(0xFF42A5F5)),
        QuickActionItem("Verificar DNS", "dns_check", Icons.Default.Dns, Color(0xFF66BB6A)),
        QuickActionItem("Speedtest", "speedtest", Icons.Default.Speed, Color(0xFFFFA726))
    )

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            ActionCard(actions[0], isExecuting, onAction, Modifier.weight(1f))
            ActionCard(actions[1], isExecuting, onAction, Modifier.weight(1f))
        }
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            ActionCard(actions[2], isExecuting, onAction, Modifier.weight(1f))
            ActionCard(actions[3], isExecuting, onAction, Modifier.weight(1f))
        }
    }
}

data class QuickActionItem(val label: String, val id: String, val icon: ImageVector, val color: Color)

@Composable
fun ActionCard(action: QuickActionItem, isExecuting: Boolean, onAction: (String) -> Unit, modifier: Modifier = Modifier) {
    Card(
        onClick = { onAction(action.id) },
        enabled = !isExecuting,
        modifier = modifier.height(100.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Surface),
        border = CardDefaults.outlinedCardBorder()
    ) {
        Column(
            modifier = Modifier.fillMaxSize().padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(action.icon, contentDescription = null, tint = action.color, modifier = Modifier.size(32.dp))
            Spacer(modifier = Modifier.height(8.dp))
            Text(action.label, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold, color = TextPrimary)
        }
    }
}

@Composable
fun SystemDetailsSection(details: Map<String, String>, isExpanded: Boolean, onExpandClick: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Surface),
        border = CardDefaults.outlinedCardBorder()
    ) {
        Column {
            Row(
                modifier = Modifier
                    .clickable { onExpandClick() }
                    .padding(16.dp)
                    .fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Info, contentDescription = null, tint = Primary)
                    Spacer(modifier = Modifier.width(12.dp))
                    Text("Información Detallada del Sistema", fontWeight = FontWeight.Bold, color = TextPrimary)
                }
                Icon(
                    if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                    contentDescription = null,
                    tint = TextSecondary
                )
            }
            
            AnimatedVisibility(visible = isExpanded) {
                Column(modifier = Modifier.padding(start = 16.dp, end = 16.dp, bottom = 16.dp)) {
                    HorizontalDivider(modifier = Modifier.padding(bottom = 16.dp), color = Outline.copy(alpha = 0.5f))
                    details.forEach { (key, value) ->
                        SystemDetailRow(key, value)
                        Spacer(modifier = Modifier.height(8.dp))
                    }
                }
            }
        }
    }
}

@Composable
fun SystemDetailRow(label: String, value: String) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, style = MaterialTheme.typography.bodySmall, color = TextSecondary)
        Text(value, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold, color = TextPrimary)
    }
}

@Composable
fun HealthItem(container: ContainerResponse, statusMap: Map<String, String>, viewModel: DiagnosticsViewModel) {
    val status = statusMap[container.id] ?: "Pendiente"
    val isOk = status.contains("OK")

    Surface(
        modifier = Modifier.padding(vertical = 4.dp).fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        color = Surface,
        border = CardDefaults.outlinedCardBorder()
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
                Text(container.name, fontWeight = FontWeight.Bold, color = TextPrimary, fontSize = 14.sp)
                Text("ID: ${container.id.take(12)}", fontSize = 10.sp, color = TextSecondary)
            }
            Text(
                status,
                style = MaterialTheme.typography.labelSmall,
                color = if (isOk) Success else (if (status == "Pendiente") TextSecondary else Error),
                fontWeight = FontWeight.Bold
            )
            IconButton(onClick = { viewModel.checkHealth(container.id, container.name) }) {
                Icon(Icons.AutoMirrored.Filled.Send, contentDescription = "Check", modifier = Modifier.size(16.dp), tint = Primary)
            }
        }
    }
}
