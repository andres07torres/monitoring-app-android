package com.example.inframonitor.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
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
import com.example.inframonitor.data.models.LogResponse
import com.example.inframonitor.viewmodel.MainViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LogsScreen(viewModel: MainViewModel) {
    val logs by viewModel.logs.collectAsState()
    val containers by viewModel.containers.collectAsState()
    
    var searchQuery by remember { mutableStateOf("") }
    var selectedContainerId by remember { mutableStateOf<String?>(null) }

    val filteredLogs = logs.filter {
        (searchQuery.isEmpty() || it.message.contains(searchQuery, ignoreCase = true) || it.tag.contains(searchQuery, ignoreCase = true))
    }

    Column(modifier = Modifier.fillMaxSize()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("Registros en tiempo real", fontWeight = FontWeight.Bold, fontSize = 14.sp)
            Spacer(modifier = Modifier.height(12.dp))
            
            // Selector de contenedores para logs
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Opción "Todos"
                FilterChip(
                    selected = selectedContainerId == null,
                    onClick = { 
                        selectedContainerId = null
                        viewModel.fetchLogs(null)
                    },
                    label = { Text("Todos") }
                )
                
                containers.take(3).forEach { container ->
                    FilterChip(
                        selected = selectedContainerId == container.id,
                        onClick = { 
                            selectedContainerId = container.id
                            viewModel.fetchLogs(container.id)
                        },
                        label = { Text(container.name) }
                    )
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

        Card(
            modifier = Modifier
                .padding(horizontal = 16.dp)
                .padding(bottom = 16.dp)
                .fillMaxSize(),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = CardDefaults.outlinedCardBorder()
        ) {
            LazyColumn(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(filteredLogs) { log ->
                    LogItem(log)
                }
            }
        }
    }
}

@Composable
fun LogItem(log: LogResponse) {
    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(
            log.time,
            style = MaterialTheme.typography.bodySmall.copy(
                fontFamily = FontFamily.Monospace,
                color = MaterialTheme.colorScheme.secondary
            )
        )
        LevelTag(log.level)
        Column {
            Text(
                log.tag,
                style = MaterialTheme.typography.bodySmall.copy(
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.secondary
                )
            )
            Text(
                log.message,
                style = MaterialTheme.typography.bodySmall.copy(
                    fontFamily = FontFamily.Monospace,
                    lineHeight = 16.sp
                )
            )
        }
    }
}

@Composable
fun LevelTag(level: String) {
    val (bgColor, textColor) = when (level.uppercase()) {
        "INFO" -> Color(0xFFEEF1FF) to Color(0xFF3757F5)
        "WARN" -> Color(0xFFFFF6E9) to Color(0xFFF79009)
        "ERROR" -> Color(0xFFFEEEEC) to Color(0xFFF04438)
        else -> Color.LightGray to Color.DarkGray
    }
    Surface(
        color = bgColor,
        shape = RoundedCornerShape(4.dp)
    ) {
        Text(
            level,
            modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp),
            style = MaterialTheme.typography.labelSmall.copy(
                fontSize = 9.sp,
                fontWeight = FontWeight.Bold,
                color = textColor
            )
        )
    }
}
