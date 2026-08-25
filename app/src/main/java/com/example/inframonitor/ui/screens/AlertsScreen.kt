package com.example.inframonitor.ui.screens

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
import com.example.inframonitor.data.models.AlertResponse
import com.example.inframonitor.viewmodel.MainViewModel

@Composable
fun AlertsScreen(viewModel: MainViewModel) {
    val alerts by viewModel.alerts.collectAsState()

    Column(modifier = Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Alertas", fontWeight = FontWeight.Bold, fontSize = 14.sp)
            Spacer(modifier = Modifier.width(4.dp))
            Text("- ${alerts.size}", color = MaterialTheme.colorScheme.secondary, fontSize = 14.sp)
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(alerts) { alert ->
                AlertItem(alert)
            }
        }
    }
}

@Composable
fun AlertItem(alert: AlertResponse) {
    val color = when (alert.severity.lowercase()) {
        "critical" -> Color(0xFFF04438)
        "warning" -> Color(0xFFF79009)
        "success" -> Color(0xFF12B76A)
        else -> MaterialTheme.colorScheme.primary
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = CardDefaults.outlinedCardBorder()
    ) {
        Row(modifier = Modifier.height(IntrinsicSize.Min)) {
            Box(
                modifier = Modifier
                    .width(4.dp)
                    .fillMaxHeight()
                    .background(color)
            )
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(alert.title, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                    Text(alert.time, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.secondary)
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    alert.description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.secondary,
                    lineHeight = 18.sp
                )
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    alert.metadata,
                    style = MaterialTheme.typography.labelSmall.copy(letterSpacing = 0.5.sp),
                    color = MaterialTheme.colorScheme.secondary.copy(alpha = 0.7f)
                )
            }
        }
    }
}
