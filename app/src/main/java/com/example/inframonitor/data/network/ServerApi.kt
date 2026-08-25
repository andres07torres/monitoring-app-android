package com.example.inframonitor.data.network

import com.example.inframonitor.data.models.SystemStatsResponse
import com.example.inframonitor.data.models.ContainerResponse
import retrofit2.Response
import retrofit2.http.GET

interface ServerApi {
    // Los endpoints apuntarán al dominio seguro gestionado por Cloudflare
    
    @GET("/api/v1/system/stats")
    suspend fun getSystemStats(): Response<SystemStatsResponse>

    @GET("/api/v1/containers")
    suspend fun getContainers(): Response<List<ContainerResponse>>

    @retrofit2.http.POST("/api/v1/containers/{id}/start")
    suspend fun startContainer(@retrofit2.http.Path("id") id: String): Response<Unit>

    @retrofit2.http.POST("/api/v1/containers/{id}/stop")
    suspend fun stopContainer(@retrofit2.http.Path("id") id: String): Response<Unit>

    @GET("/api/v1/logs")
    suspend fun getLogs(@retrofit2.http.Query("container_id") containerId: String? = null): Response<List<com.example.inframonitor.data.models.LogResponse>>

    @GET("/api/v1/alerts")
    suspend fun getAlerts(): Response<List<com.example.inframonitor.data.models.AlertResponse>>
}
