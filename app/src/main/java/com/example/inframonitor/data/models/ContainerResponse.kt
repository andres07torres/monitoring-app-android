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
