package com.example.inframonitor.data.network

import com.example.inframonitor.data.models.ContainerResponse
import com.example.inframonitor.data.models.SystemStatsResponse
import com.example.inframonitor.data.models.ContainerLogsResponse
import com.example.inframonitor.data.models.AlertResponse
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path

interface ServerApi {
    
    @GET("api/v1/system/stats")
    suspend fun getSystemStats(): Response<SystemStatsResponse>

    @GET("api/v1/containers")
    suspend fun getContainers(): Response<List<ContainerResponse>>

    @POST("api/v1/containers/{container_id}/start")
    suspend fun startContainer(@Path("container_id") id: String): Response<Unit>

    @POST("api/v1/containers/{container_id}/stop")
    suspend fun stopContainer(@Path("container_id") id: String): Response<Unit>

    @GET("api/v1/containers/{container_id}/logs")
    suspend fun getLogs(@Path("container_id") containerId: String): Response<ContainerLogsResponse>

    @GET("api/v1/alerts")
    suspend fun getAlerts(): List<AlertResponse> // Retorno directo como Array según contrato real
}
