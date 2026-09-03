package com.example.inframonitor.ui.screens

import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.inframonitor.data.models.ContainerResponse
import com.example.inframonitor.ui.UiState
import com.example.inframonitor.viewmodel.ContainersViewModel
import com.example.inframonitor.ui.theme.*
import kotlinx.coroutines.launch
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ContainersScreen(viewModel: ContainersViewModel = viewModel()) {
    val uiState by viewModel.uiState.collectAsState()
    
    Box(modifier = Modifier.fillMaxSize().background(Background)) {
        when (val state = uiState) {
            is UiState.Loading -> {
                CircularProgressIndicator(
                    modifier = Modifier.align(Alignment.Center),
                    color = Primary,
                    strokeWidth = 3.dp
                )
            }
            is UiState.Success -> {
                ContainersContent(state.data, viewModel)
            }
            is UiState.Error -> {
                Column(
                    modifier = Modifier.align(Alignment.Center).padding(32.dp), 
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(Icons.Default.Stop, contentDescription = null, tint = Error, modifier = Modifier.size(48.dp))
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = state.message, 
                        color = TextPrimary,
                        fontWeight = FontWeight.Medium,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                    Button(
                        onClick = { viewModel.fetchContainers() }, 
                        modifier = Modifier.padding(top = 16.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Primary),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text("Reintentar Conexión")
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ContainersContent(containers: List<ContainerResponse>, viewModel: ContainersViewModel) {
    var selectedFilter by remember { mutableStateOf("Todos") }
    val filters = listOf("Todos", "Activos", "Detenidos")

    val filteredContainers = when (selectedFilter) {
        "Activos" -> containers.filter { it.status == "running" }
        "Detenidos" -> containers.filter { it.status != "running" }
        else -> containers
    }

    val sheetState = rememberModalBottomSheetState()
    val scope = rememberCoroutineScope()
    var showBottomSheet by remember { mutableStateOf(false) }
    var selectedContainer by remember { mutableStateOf<ContainerResponse?>(null) }

    Column(modifier = Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier.padding(24.dp).fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    "Servicios Docker",
                    style = MaterialTheme.typography.headlineSmall.copy(
                        fontWeight = FontWeight.ExtraBold,
                        letterSpacing = (-0.5).sp,
                        color = TextPrimary
                    )
                )
                Text(
                    "${filteredContainers.size} instancias detectadas", 
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextSecondary
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            com.example.inframonitor.ui.components.RefreshButton(onClick = { viewModel.fetchContainers() })
        }

        Row(
            modifier = Modifier.padding(horizontal = 24.dp).fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            filters.forEach { filter ->
                val isSelected = selectedFilter == filter
                Surface(
                    onClick = { selectedFilter = filter },
                    shape = RoundedCornerShape(8.dp),
                    color = if (isSelected) Primary else Surface,
                    border = BorderStroke(1.dp, if (isSelected) Primary else Outline),
                    modifier = Modifier.height(36.dp)
                ) {
                    Box(modifier = Modifier.padding(horizontal = 16.dp), contentAlignment = Alignment.Center) {
                        Text(
                            filter,
                            style = MaterialTheme.typography.labelLarge,
                            color = if (isSelected) Surface else TextPrimary,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                        )
                    }
                }
            }
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            items(filteredContainers, key = { it.id }) { container ->
                ContainerItem(
                    container = container,
                    onToggle = { 
                        viewModel.toggleContainer(container.id, container.status)
                    },
                    onClick = {
                        selectedContainer = container
                        showBottomSheet = true
                    }
                )
            }
        }
    }

    if (showBottomSheet && selectedContainer != null) {
        ModalBottomSheet(
            onDismissRequest = { showBottomSheet = false },
            sheetState = sheetState,
            containerColor = Surface,
            shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
            dragHandle = { BottomSheetDefaults.DragHandle(color = Outline) }
        ) {
            ContainerActionsContent(
                container = selectedContainer!!,
                onAction = { action ->
                    when(action) {
                        "restart" -> viewModel.restartContainer(selectedContainer!!.id)
                        "pause" -> viewModel.pauseContainer(selectedContainer!!.id, selectedContainer!!.status == "paused")
                        "remove" -> viewModel.removeContainer(selectedContainer!!.id)
                        else -> viewModel.toggleContainer(selectedContainer!!.id, selectedContainer!!.status)
                    }
                    scope.launch { sheetState.hide() }.invokeOnCompletion {
                        if (!sheetState.isVisible) {
                            showBottomSheet = false
                        }
                    }
                }
            )
        }
    }
}

@Composable
fun ContainerItem(
    container: ContainerResponse, 
    onToggle: (Boolean) -> Unit,
    onClick: () -> Unit
) {
    val isRunning = container.status == "running"
    val isPaused = container.status == "paused"
    
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val alpha by infiniteTransition.animateFloat(
        initialValue = 0.4f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "alpha"
    )

    Card(
        modifier = Modifier.fillMaxWidth(),
        onClick = onClick,
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Surface),
        border = BorderStroke(1.dp, if (isRunning) Success.copy(alpha = 0.3f) else Outline),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(contentAlignment = Alignment.Center) {
                    if (isRunning) {
                        Box(
                            modifier = Modifier
                                .size(12.dp)
                                .background(Success.copy(alpha = alpha), CircleShape)
                        )
                    }
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .background(if (isRunning) Success else (if (isPaused) Accent else Error), CircleShape)
                    )
                }
                
                Spacer(modifier = Modifier.width(16.dp))
                
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        container.name, 
                        fontWeight = FontWeight.Bold, 
                        fontSize = 16.sp,
                        color = TextPrimary
                    )
                    
                    Text(
                        "ID: ${container.id.take(12)}",
                        style = MaterialTheme.typography.bodySmall.copy(
                            fontFamily = FontFamily.Monospace,
                            color = TextSecondary,
                            fontSize = 11.sp
                        )
                    )
                }

                Switch(
                    checked = isRunning,
                    onCheckedChange = onToggle,
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = Surface,
                        checkedTrackColor = Success,
                        uncheckedThumbColor = Surface,
                        uncheckedTrackColor = TextMuted.copy(alpha = 0.5f),
                        uncheckedBorderColor = Color.Transparent
                    )
                )
            }

            if (isRunning) {
                Spacer(modifier = Modifier.height(16.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    MetricBadge("CPU", "${String.format(Locale.getDefault(), "%.1f", container.cpu_usage ?: 0.0)}%", Primary)
                    MetricBadge("RAM", "${String.format(Locale.getDefault(), "%.1f", container.ram_usage ?: 0.0)}%", Accent)
                }
            }
        }
    }
}

@Composable
fun MetricBadge(label: String, value: String, color: Color) {
    Surface(
        color = color.copy(alpha = 0.1f),
        shape = RoundedCornerShape(8.dp),
        border = BorderStroke(1.dp, color.copy(alpha = 0.2f))
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                label,
                style = MaterialTheme.typography.labelSmall,
                color = color,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                value,
                style = MaterialTheme.typography.bodySmall,
                color = TextPrimary,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

@Composable
fun ContainerActionsContent(container: ContainerResponse, onAction: (String) -> Unit) {
    Column(
        modifier = Modifier.fillMaxWidth().padding(bottom = 40.dp, start = 24.dp, end = 24.dp)
    ) {
        Text(
            text = container.name,
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.ExtraBold,
            color = TextPrimary,
            modifier = Modifier.padding(top = 8.dp)
        )
        Text(
            text = "Estado: ${container.status.uppercase()}",
            style = MaterialTheme.typography.bodySmall,
            color = TextSecondary,
            modifier = Modifier.padding(bottom = 24.dp)
        )
        
        HorizontalDivider(color = Outline, modifier = Modifier.padding(bottom = 16.dp))

        if (container.status != "running") {
            ActionRow(Icons.Default.PlayArrow, "Iniciar instancia", Success) { onAction("start") }
        } else {
            ActionRow(Icons.Default.Stop, "Detener instancia", Error) { onAction("stop") }
        }
        
        ActionRow(Icons.Default.Refresh, "Reiniciar servicio", Accent) { onAction("restart") }
        
        val isPaused = container.status == "paused"
        ActionRow(
            if (isPaused) Icons.Default.PlayCircle else Icons.Default.Pause,
            if (isPaused) "Reanudar (Unpause)" else "Pausar instancia",
            Primary
        ) { onAction("pause") }

        ActionRow(Icons.Default.Delete, "Eliminar contenedor", Error) { onAction("remove") }
    }
}

@androidx.compose.ui.tooling.preview.Preview(showBackground = true)
@Composable
fun ContainersHeaderPreview() {
    InfraMonitorTheme {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(Background)
                .padding(horizontal = 24.dp, vertical = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    "Servicios Docker",
                    style = MaterialTheme.typography.headlineSmall.copy(
                        fontWeight = FontWeight.ExtraBold,
                        letterSpacing = (-0.5).sp,
                        color = TextPrimary
                    )
                )
                Text(
                    "4 instancias detectadas", 
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextSecondary
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            com.example.inframonitor.ui.components.RefreshButton(onClick = {})
        }
    }
}

@Composable
fun ActionRow(icon: ImageVector, label: String, color: Color, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth().height(56.dp),
        color = Color.Transparent,
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .background(color.copy(alpha = 0.1f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(20.dp))
            }
            Spacer(modifier = Modifier.width(16.dp))
            Text(label, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium, color = TextPrimary)
        }
    }
}
