package com.example.inframonitor.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.inframonitor.data.models.AlertResponse
import com.example.inframonitor.data.models.ContainerResponse
import com.example.inframonitor.data.models.SystemStatsResponse
import com.example.inframonitor.data.network.RetrofitClient
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class MainViewModel : ViewModel() {

    private val _systemStats = MutableStateFlow<SystemStatsResponse?>(
        SystemStatsResponse(
            cpu_percent = 0.0,
            cpu_cores = 0,
            cpu_frequency = 0.0,
            ram_percent = 0.0,
            ram_used_gb = 0.0,
            ram_total_gb = 0.0
        )
    )
    val systemStats: StateFlow<SystemStatsResponse?> = _systemStats.asStateFlow()

    private val _containers = MutableStateFlow<List<ContainerResponse>>(emptyList())
    val containers: StateFlow<List<ContainerResponse>> = _containers.asStateFlow()

    private val _logs = MutableStateFlow<List<String>>(emptyList())
    val logs: StateFlow<List<String>> = _logs.asStateFlow()

    private val _alerts = MutableStateFlow<List<AlertResponse>>(emptyList())
    val alerts: StateFlow<List<AlertResponse>> = _alerts.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    init {
        fetchInitialData()
        
        viewModelScope.launch {
            while (true) {
                delay(5000)
                updateDynamicData()
            }
        }
    }

    private fun fetchInitialData() {
        viewModelScope.launch {
            try {
                fetchContainers()
                fetchAlerts()
                updateStats()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private suspend fun updateDynamicData() {
        try {
            updateStats()
            fetchAlerts()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private suspend fun fetchContainers() {
        val response = RetrofitClient.instance.getContainers()
        if (response.isSuccessful) {
            _containers.value = response.body() ?: _containers.value
        }
    }

    private suspend fun fetchAlerts() {
        try {
            val response = RetrofitClient.instance.getAlerts()
            if (response.isSuccessful && response.body() != null) {
                _alerts.value = response.body()!!
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun fetchLogs(containerId: String? = null) {
        if (containerId == null) return
        viewModelScope.launch {
            try {
                val response = RetrofitClient.instance.getLogs(containerId)
                if (response.isSuccessful) {
                    _logs.value = response.body()?.logs ?: _logs.value
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private suspend fun updateStats() {
        val statsResponse = RetrofitClient.instance.getSystemStats()
        if (statsResponse.isSuccessful) {
            _systemStats.value = statsResponse.body() ?: _systemStats.value
        }
    }

    fun toggleContainer(containerId: String, enable: Boolean) {
        viewModelScope.launch {
            try {
                val response = if (enable) {
                    RetrofitClient.instance.startContainer(containerId)
                } else {
                    RetrofitClient.instance.stopContainer(containerId)
                }
                
                if (response.isSuccessful) {
                    fetchContainers()
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
}
