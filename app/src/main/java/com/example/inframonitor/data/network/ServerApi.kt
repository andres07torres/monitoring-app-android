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
}
