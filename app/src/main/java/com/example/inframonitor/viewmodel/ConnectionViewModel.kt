package com.example.inframonitor.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.inframonitor.data.SettingsManager
import com.example.inframonitor.data.network.RetrofitClient
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class ConnectionViewModel(application: Application) : AndroidViewModel(application) {
    private val settingsManager = SettingsManager(application)

    private val _isConfigured = MutableStateFlow<Boolean?>(null)
    val isConfigured: StateFlow<Boolean?> = _isConfigured.asStateFlow()

    init {
        viewModelScope.launch {
            val url = settingsManager.serverUrl.first() ?: SettingsManager.DEFAULT_SERVER_URL
            val apiKey = settingsManager.apiKey.first()
            
            RetrofitClient.updateBaseUrl(url)
            RetrofitClient.updateApiKey(apiKey)
            
            _isConfigured.value = url.isNotBlank()
        }
    }

    fun saveConfig(url: String, apiKey: String) {
        viewModelScope.launch {
            val cleanUrl = url.trim()
            val cleanApiKey = apiKey.trim()
            
            settingsManager.saveServerUrl(cleanUrl)
            settingsManager.saveApiKey(cleanApiKey)
            
            RetrofitClient.updateBaseUrl(cleanUrl)
            RetrofitClient.updateApiKey(cleanApiKey)
            
            _isConfigured.value = true
        }
    }

    fun logout() {
        viewModelScope.launch {
            settingsManager.clearSettings()
            RetrofitClient.updateApiKey(null)
            _isConfigured.value = false
        }
    }
}
