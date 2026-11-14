package com.example.inframonitor.data.network

import com.example.inframonitor.data.models.ContainerResponse
import com.example.inframonitor.data.models.SystemStatsResponse
import com.example.inframonitor.data.models.ContainerLogsResponse
import com.example.inframonitor.data.models.AlertResponse
import retrofit2.Response
import retrofit2.http.*

interface ServerApi {
    
    @GET("api/v1/system/stats")
    suspend fun getSystemStats(): Response<SystemStatsResponse>

    @GET("api/v1/containers")
    suspend fun getContainers(): Response<List<ContainerResponse>>

    @POST("api/v1/containers/{container_id}/start")
    suspend fun startContainer(@Path("container_id") id: String): Response<Unit>

    @POST("api/v1/containers/{container_id}/stop")
    suspend fun stopContainer(@Path("container_id") id: String): Response<Unit>

    @POST("api/v1/containers/{container_id}/restart")
    suspend fun restartContainer(@Path("container_id") id: String): Response<Unit>

    @POST("api/v1/containers/{container_id}/pause")
    suspend fun pauseContainer(@Path("container_id") id: String): Response<Unit>

    @POST("api/v1/containers/{container_id}/unpause")
    suspend fun unpauseContainer(@Path("container_id") id: String): Response<Unit>

    @DELETE("api/v1/containers/{container_id}")
    suspend fun removeContainer(@Path("container_id") id: String): Response<Unit>

    @POST("api/v1/system/prune")
    suspend fun pruneSystem(): Response<Unit>

    @GET("api/v1/containers/{container_id}/logs")
    suspend fun getLogs(@Path("container_id") containerId: String, @Query("tail") tail: Int = 100): Response<ContainerLogsResponse>

    @GET("api/v1/alerts")
    suspend fun getAlerts(): Response<List<AlertResponse>>

    @GET("api/v1/logs")
    suspend fun getGlobalLogs(): Response<List<LogResponse>>

    // Healthchecks
    @GET("api/v1/healthcheck")
    suspend fun checkHealth(@Query("url") url: String): Response<HealthCheckResponse>

    // Terminal / Diagnostics
    @POST("api/v1/diagnostics/exec")
    suspend fun executeCommand(@Body commandRequest: CommandRequest): Response<CommandResponse>
}

data class HealthCheckResponse(val status: Int, val message: String)
data class CommandRequest(val command: String)
data class CommandResponse(val output: String, val exitCode: Int)
data class LogResponse(val id: String, val timestamp: String, val level: String, val message: String)
