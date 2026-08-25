package com.example.inframonitor.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LogsScreen(
    logsViewModel: LogsViewModel = viewModel(),
    containersViewModel: ContainersViewModel = viewModel()
) {
    val logsState by logsViewModel.uiState.collectAsState()
    val containersState by containersViewModel.uiState.collectAsState()
    
    var searchQuery by remember { mutableStateOf("") }
    var selectedContainerId by remember { mutableStateOf<String?>(null) }

    Column(modifier = Modifier.fillMaxSize()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("Registros en tiempo real", fontWeight = FontWeight.Bold, fontSize = 14.sp)
            Spacer(modifier = Modifier.height(12.dp))
            
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
                            label = { Text(container.name) }
                        )
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("Buscar en los registros...", fontSize = 14.sp) },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, modifier = Modifier.size(20.dp)) },
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)
                ),
                singleLine = true
            )
            Spacer(modifier = Modifier.height(16.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(modifier = Modifier.size(6.dp).background(Color(0xFFF04438), CircleShape))
                Spacer(modifier = Modifier.width(8.dp))
                Text("Transmisión en vivo", color = Color(0xFFF04438), fontSize = 12.sp, fontWeight = FontWeight.Medium)
            }
        }

        Box(modifier = Modifier.fillMaxSize()) {
            if (selectedContainerId == null) {
                Text(
                    "Selecciona un contenedor para ver sus registros", 
                    modifier = Modifier.align(Alignment.Center),
                    color = MaterialTheme.colorScheme.secondary
                )
            } else {
                when (val state = logsState) {
                    is UiState.Loading -> {
                        CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                    }
                    is UiState.Success -> {
                        val filteredLogs = state.data.filter {
                            it.contains(searchQuery, ignoreCase = true)
                        }
                        if (filteredLogs.isEmpty()) {
                            Text("No hay registros para mostrar", modifier = Modifier.align(Alignment.Center), color = MaterialTheme.colorScheme.secondary)
                        } else {
                            LogsList(filteredLogs)
                        }
                    }
                    is UiState.Error -> {
                        Column(
                            modifier = Modifier.align(Alignment.Center).padding(16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(text = state.message, color = MaterialTheme.colorScheme.error)
                            Spacer(modifier = Modifier.height(8.dp))
                            Button(onClick = { logsViewModel.fetchLogs(selectedContainerId!!) }) {
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
    Card(
        modifier = Modifier.padding(horizontal = 16.dp).padding(bottom = 16.dp).fillMaxSize(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = CardDefaults.outlinedCardBorder()
    ) {
        LazyColumn(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            items(logs) { log ->
                Text(
                    text = log,
                    style = MaterialTheme.typography.bodySmall.copy(
                        fontFamily = FontFamily.Monospace,
                        lineHeight = 16.sp
                    ),
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}
