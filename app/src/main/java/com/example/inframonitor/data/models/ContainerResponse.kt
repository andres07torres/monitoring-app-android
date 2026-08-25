package com.example.inframonitor.data.models

data class ContainerResponse(
    val id: String,
    val name: String,
    val status: String
)

data class SystemStatsResponse(
    val cpu_percent: Double,
    val ram_percent: Double,
    val ram_used_gb: Double,
    val ram_total_gb: Double
)

data class LogResponse(
    val time: String,
    val level: String,
    val tag: String,
    val message: String
)

data class AlertResponse(
    val id: String,
    val title: String,
    val description: String,
    val time: String,
    val metadata: String,
    val severity: String // "critical", "warning", "info"
)
