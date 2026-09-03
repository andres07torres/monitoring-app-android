package com.example.inframonitor.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.inframonitor.ui.theme.Primary
import com.example.inframonitor.ui.theme.SurfaceAlt

@Composable
fun RefreshButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        onClick = onClick,
        shape = CircleShape,
        color = SurfaceAlt,
        tonalElevation = 0.dp,
        modifier = modifier.requiredSize(40.dp)
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(
                imageVector = Icons.Default.Refresh,
                contentDescription = "Actualizar",
                tint = Primary,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

@androidx.compose.ui.tooling.preview.Preview(showBackground = true)
@Composable
fun RefreshButtonPreview() {
    com.example.inframonitor.ui.theme.InfraMonitorTheme {
        androidx.compose.foundation.layout.Box(modifier = Modifier.size(80.dp), contentAlignment = Alignment.Center) {
            RefreshButton(onClick = {})
        }
    }
}
