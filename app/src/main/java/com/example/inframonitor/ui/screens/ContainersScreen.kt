package com.example.inframonitor.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.inframonitor.data.models.ContainerResponse
import com.example.inframonitor.viewmodel.MainViewModel
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ContainersScreen(viewModel: MainViewModel) {
    val containers by viewModel.containers.collectAsState()
    var selectedFilter by remember { mutableStateOf("Todos") }
    val filters = listOf("Todos", "Activos", "Detenidos")

    // Mock data if empty for visual validation
    val displayContainers = if (containers.isEmpty()) {
        listOf(
            ContainerResponse("1", "nginx-proxy", "running"),
            ContainerResponse("2", "postgres-db", "running"),
            ContainerResponse("3", "plex-media", "restarting"),
            ContainerResponse("4", "grafana", "exited"),
            ContainerResponse("5", "watchtower", "exited")
        )
    } else {
        containers
    }

    val filteredContainers = when (selectedFilter) {
        "Activos" -> displayContainers.filter { it.status == "running" }
        "Detenidos" -> displayContainers.filter { it.status != "running" }
        else -> displayContainers
    }

    // Bottom Sheet Logic
    val sheetState = rememberModalBottomSheetState()
    val scope = rememberCoroutineScope()
    var showBottomSheet by remember { mutableStateOf(false) }
    var selectedContainer by remember { mutableStateOf<ContainerResponse?>(null) }

    Column(modifier = Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Contenedores", fontWeight = FontWeight.Bold, fontSize = 14.sp)
            Spacer(modifier = Modifier.width(4.dp))
            Text("- ${filteredContainers.size}", color = MaterialTheme.colorScheme.secondary, fontSize = 14.sp)
        }

        Row(
            modifier = Modifier
                .padding(horizontal = 16.dp)
                .fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            filters.forEach { filter ->
                FilterChip(
                    selected = selectedFilter == filter,
                    onClick = { selectedFilter = filter },
                    label = { Text(filter) },
                    shape = RoundedCornerShape(20.dp),
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = MaterialTheme.colorScheme.primary,
                        selectedLabelColor = Color.White
                    )
                )
            }
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(filteredContainers) { container ->
                ContainerItem(
                    container = container,
                    onToggle = { isChecked ->
                        viewModel.toggleContainer(container.id, isChecked)
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
            containerColor = MaterialTheme.colorScheme.surface,
            shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp)
        ) {
            ContainerActionsContent(
                container = selectedContainer!!,
                onAction = { action ->
                    // Handle actions (start, stop, restart)
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
    val initials = container.name.take(2).uppercase()
    val isRunning = container.status == "running"
    
    val (statusColor, bgColor) = when {
        isRunning -> Color(0xFF12B76A) to Color(0xFFEAFBF3)
        container.status == "restarting" -> Color(0xFFF79009) to Color(0xFFFFF6E9)
        else -> Color(0xFFF04438) to Color(0xFFFEEEEC)
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        onClick = onClick,
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = CardDefaults.outlinedCardBorder()
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                modifier = Modifier.size(40.dp),
                shape = RoundedCornerShape(8.dp),
                color = bgColor
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(initials, fontWeight = FontWeight.Bold, color = statusColor, fontSize = 12.sp)
                }
            }
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(container.name, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                Text("Status: ${container.status}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.secondary)
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    "ID: ${container.id.take(12)}",
                    style = MaterialTheme.typography.bodySmall.copy(
                        fontSize = 11.sp,
                        letterSpacing = 0.5.sp
                    ),
                    color = MaterialTheme.colorScheme.secondary
                )
            }
            Switch(
                checked = isRunning,
                onCheckedChange = onToggle,
                colors = SwitchDefaults.colors(
                    checkedThumbColor = Color.White,
                    checkedTrackColor = MaterialTheme.colorScheme.primary,
                    uncheckedThumbColor = Color.White,
                    uncheckedTrackColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f),
                    uncheckedBorderColor = Color.Transparent
                )
            )
        }
    }
}

@Composable
fun ContainerActionsContent(container: ContainerResponse, onAction: (String) -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 32.dp, start = 16.dp, end = 16.dp)
    ) {
        Text(
            text = "Acciones para ${container.name}",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(vertical = 16.dp)
        )
        
        ActionRow(Icons.Default.PlayArrow, "Iniciar contenedor", Color(0xFF12B76A)) { onAction("start") }
        ActionRow(Icons.Default.Stop, "Detener contenedor", Color(0xFFF04438)) { onAction("stop") }
        ActionRow(Icons.Default.Refresh, "Reiniciar contenedor", Color(0xFF3757F5)) { onAction("restart") }
    }
}

@Composable
fun ActionRow(icon: ImageVector, label: String, color: Color, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        color = Color.Transparent
    ) {
        Row(
            modifier = Modifier.padding(vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(24.dp))
            Spacer(modifier = Modifier.width(16.dp))
            Text(label, style = MaterialTheme.typography.bodyLarge)
        }
    }
}
