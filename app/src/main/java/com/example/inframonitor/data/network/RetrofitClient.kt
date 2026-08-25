package com.example.inframonitor.data.network

import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

object RetrofitClient {
    // URL base, que luego apuntará al dominio seguro gestionado por Cloudflare Tunnels
    private const val BASE_URL = "https://api.tu-dominio.com/"

    val instance: ServerApi by lazy {
        val retrofit = Retrofit.Builder()
            .baseUrl(BASE_URL)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
        
        retrofit.create(ServerApi::class.java)
    }
}
