package com.example.inframonitor.data.network

import com.example.inframonitor.BuildConfig
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

object RetrofitClient {
    private const val DEFAULT_HTTPS_URL = "https://api-infra.andr3sdev7.com/"
    
    private var currentBaseUrl: String = DEFAULT_HTTPS_URL
    private var currentApiKey: String? = null
    private var _instance: ServerApi? = null

    fun updateBaseUrl(rawUrl: String) {
        val sanitized = sanitizeUrl(rawUrl)
        if (currentBaseUrl != sanitized) {
            currentBaseUrl = sanitized
            _instance = null
        }
    }

    fun updateApiKey(apiKey: String?) {
        val cleanKey = apiKey?.trim()
        if (currentApiKey != cleanKey) {
            currentApiKey = cleanKey
            _instance = null
        }
    }

    fun getCurrentApiKey(): String? = currentApiKey

    fun getBaseUrl(): String = currentBaseUrl

    fun getWsBaseUrl(): String {
        return currentBaseUrl
            .replace("https://", "wss://")
            .replace("http://", "ws://")
    }

    private fun sanitizeUrl(rawUrl: String): String {
        var trimmed = rawUrl.trim()
        if (trimmed.isBlank()) return DEFAULT_HTTPS_URL

        // Enforzar HTTPS si viene HTTP o sin protocolo
        if (trimmed.startsWith("http://")) {
            trimmed = "https://" + trimmed.substring(7)
        } else if (!trimmed.startsWith("https://")) {
            trimmed = "https://$trimmed"
        }

        return if (trimmed.endsWith("/")) trimmed else "$trimmed/"
    }

    private val logging = HttpLoggingInterceptor().apply {
        level = if (BuildConfig.DEBUG) {
            HttpLoggingInterceptor.Level.BODY
        } else {
            HttpLoggingInterceptor.Level.NONE
        }
    }

    private val authInterceptor = Interceptor { chain ->
        val original = chain.request()
        val requestBuilder = original.newBuilder()
        
        currentApiKey?.takeIf { it.isNotBlank() }?.let { token ->
            // Inyección Global de Token en header X-API-Key y Authorization
            requestBuilder.header("X-API-Key", token)
            requestBuilder.header("Authorization", "Bearer $token")
        }
        
        chain.proceed(requestBuilder.build())
    }

    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .pingInterval(25, TimeUnit.SECONDS) // Keep-alive / Ping-pong para Cloudflare Zero Trust
        .addInterceptor(logging)
        .addInterceptor(authInterceptor)
        .build()

    val instance: ServerApi
        get() {
            if (_instance == null) {
                _instance = Retrofit.Builder()
                    .baseUrl(currentBaseUrl)
                    .client(client)
                    .addConverterFactory(GsonConverterFactory.create())
                    .build()
                    .create(ServerApi::class.java)
            }
            return _instance!!
        }
}
