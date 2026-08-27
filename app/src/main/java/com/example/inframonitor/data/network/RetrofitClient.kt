package com.example.inframonitor.data.network

import com.example.inframonitor.BuildConfig
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

object RetrofitClient {
    private var currentBaseUrl: String = "https://api-infra.andr3sdev7.com/"
    private var currentApiKey: String? = null
    private var _instance: ServerApi? = null

    fun updateBaseUrl(newUrl: String) {
        val sanitizedUrl = if (newUrl.endsWith("/")) newUrl else "$newUrl/"
        if (currentBaseUrl != sanitizedUrl) {
            currentBaseUrl = sanitizedUrl
            _instance = null
        }
    }

    fun updateApiKey(apiKey: String?) {
        if (currentApiKey != apiKey) {
            currentApiKey = apiKey
            _instance = null
        }
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
        
        currentApiKey?.let {
            // Enviamos ambos formatos para asegurar compatibilidad
            requestBuilder.header("X-API-Key", it)
            requestBuilder.header("Authorization", "Bearer $it")
        }
        
        chain.proceed(requestBuilder.build())
    }

    private val client = OkHttpClient.Builder()
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
