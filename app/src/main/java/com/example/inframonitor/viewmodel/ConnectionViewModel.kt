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

    private val _serverUrl = MutableStateFlow<String?>(null)
    val serverUrl: StateFlow<String?> = _serverUrl.asStateFlow()

    private val _isConfigured = MutableStateFlow<Boolean?>(null)
    val isConfigured: StateFlow<Boolean?> = _isConfigured.asStateFlow()

    init {
        viewModelScope.launch {
            val url = settingsManager.serverUrl.first()
            _serverUrl.value = url
            _isConfigured.value = url != null
            if (url != null) {
                RetrofitClient.updateBaseUrl(url)
            }
        }
    }

    fun saveUrl(url: String) {
        viewModelScope.launch {
            settingsManager.saveServerUrl(url)
            RetrofitClient.updateBaseUrl(url)
            _serverUrl.value = url
            _isConfigured.value = true
        }
    }

    fun logout() {
        viewModelScope.launch {
            settingsManager.clearSettings()
            _serverUrl.value = null
            _isConfigured.value = false
        }
    }
}
