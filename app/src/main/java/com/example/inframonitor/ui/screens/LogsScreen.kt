package com.example.inframonitor.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
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
import com.example.inframonitor.ui.UiState
import com.example.inframonitor.viewmodel.ContainersViewModel
import com.example.inframonitor.viewmodel.LogsViewModel
import com.example.inframonitor.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LogsScreen(
    logsViewModel: LogsViewModel = viewModel(),
    containersViewModel: ContainersViewModel = viewModel()
) {
    val logsState by logsViewModel.uiState.collectAsState()
    val containersState by containersViewModel.uiState.collectAsState()
    
    var selectedContainerId by remember { mutableStateOf<String?>(null) }

    Column(modifier = Modifier.fillMaxSize().background(Background)) {
        Row(
            modifier = Modifier.padding(24.dp).fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    "Registros de Docker",
                    style = MaterialTheme.typography.headlineSmall.copy(
                        fontWeight = FontWeight.ExtraBold,
                        letterSpacing = (-0.5).sp,
                        color = TextPrimary
                    )
                )
                Text(
                    "Monitorización de eventos en tiempo real",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextSecondary
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            com.example.inframonitor.ui.components.RefreshButton(onClick = { selectedContainerId?.let { logsViewModel.fetchLogs(it) } })
        }

        Column(modifier = Modifier.padding(horizontal = 24.dp)) {
            if (containersState is UiState.Success) {
                val containers = (containersState as UiState.Success).data
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(containers) { container ->
                        FilterChip(
                            selected = selectedContainerId == container.id,
                            onClick = { 
                                selectedContainerId = container.id
                                logsViewModel.fetchLogs(container.id)
                            },
                            label = { Text(container.name) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = Primary,
                                selectedLabelColor = Surface,
                                containerColor = Surface,
                                labelColor = TextPrimary
                            ),
                            border = FilterChipDefaults.filterChipBorder(
                                selectedBorderColor = Primary,
                                borderColor = Outline,
                                borderWidth = 1.dp,
                                enabled = true,
                                selected = selectedContainerId == container.id
                            )
                        )
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(24.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(modifier = Modifier.size(6.dp).background(Error, CircleShape))
                Spacer(modifier = Modifier.width(8.dp))
                Text("Transmisión en vivo", color = Error, fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Box(modifier = Modifier.fillMaxSize()) {
            if (selectedContainerId == null) {
                Column(
                    modifier = Modifier.align(Alignment.Center),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        "Selecciona un contenedor para ver sus registros", 
                        color = TextMuted,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            } else {
                when (val state = logsState) {
                    is UiState.Loading -> {
                        CircularProgressIndicator(modifier = Modifier.align(Alignment.Center), color = Primary)
                    }
                    is UiState.Success -> {
                        if (state.data.isEmpty()) {
                            Text("No hay registros disponibles", modifier = Modifier.align(Alignment.Center), color = TextMuted)
                        } else {
                            LogsList(state.data)
                        }
                    }
                    is UiState.Error -> {
                        Column(
                            modifier = Modifier.align(Alignment.Center).padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(text = state.message, color = Error)
                            Spacer(modifier = Modifier.height(16.dp))
                            Button(
                                onClick = { logsViewModel.fetchLogs(selectedContainerId!!) },
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
    }
}

@Composable
fun LogsList(logs: List<String>) {
    val listState = rememberLazyListState()

    LaunchedEffect(logs.size) {
        if (logs.isNotEmpty()) {
            listState.animateScrollToItem(logs.size - 1)
        }
    }

    Card(
        modifier = Modifier
            .padding(horizontal = 24.dp)
            .padding(bottom = 24.dp)
            .fillMaxSize(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Surface),
        border = androidx.compose.foundation.BorderStroke(1.dp, Outline),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        LazyColumn(
            state = listState,
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            items(logs) { log ->
                Text(
                    text = log,
                    style = MaterialTheme.typography.bodySmall.copy(
                        fontFamily = FontFamily.Monospace,
                        lineHeight = 16.sp,
                        color = TextPrimary
                    ),
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}
