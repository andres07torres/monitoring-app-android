package com.example.inframonitor.data.models

data class ContainerResponse(
    val id: String,
    val name: String,
    val status: String,
    val cpu_usage: Double? = null,
    val ram_usage: Double? = null
)

data class HistoryPoint(
    val timestamp: Long,
    val cpu: Double,
    val ram: Double
)

data class SystemStatsResponse(
    val cpu_percent: Double,
    val ram_percent: Double,
    val ram_used_gb: Double,
    val ram_total_gb: Double,
    val disk_percent: Double? = null,
    val disk_used_gb: Double? = null,
    val disk_total_gb: Double? = null
)

// Tarea 2: Logs como lista de Strings
data class ContainerLogsResponse(
    val logs: List<String>
)

// Tarea 1: Modelo exacto de Alerta según JSON recibido
data class AlertResponse(
    val id: String,
    val severity: String,
    val message: String,
    val timestamp: String? = null
)
