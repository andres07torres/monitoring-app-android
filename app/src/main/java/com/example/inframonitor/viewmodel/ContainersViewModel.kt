package com.example.inframonitor.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.inframonitor.data.models.ContainerResponse
import com.example.inframonitor.data.network.RetrofitClient
import com.example.inframonitor.ui.UiState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class ContainersViewModel : ViewModel() {

    private val _uiState = MutableStateFlow<UiState<List<ContainerResponse>>>(UiState.Loading)
    val uiState: StateFlow<UiState<List<ContainerResponse>>> = _uiState.asStateFlow()

    init {
        fetchContainers()
    }

    fun fetchContainers() {
        viewModelScope.launch {
            _uiState.value = UiState.Loading
            try {
                val response = RetrofitClient.instance.getContainers()
                if (response.isSuccessful && response.body() != null) {
                    _uiState.value = UiState.Success(response.body()!!)
                } else {
                    _uiState.value = UiState.Error("Error: ${response.code()}")
                }
            } catch (e: Exception) {
                _uiState.value = UiState.Error(e.message ?: "Error desconocido")
            }
        }
    }

    fun toggleContainer(containerId: String, currentStatus: String) {
        viewModelScope.launch {
            try {
                val isRunning = currentStatus == "running"
                val response = if (isRunning) {
                    RetrofitClient.instance.stopContainer(containerId)
                } else {
                    RetrofitClient.instance.startContainer(containerId)
                }
                
                if (response.isSuccessful) {
                    fetchContainers()
                }
            } catch (e: Exception) {
                // Manejar error
            }
        }
    }

    fun restartContainer(containerId: String) {
        viewModelScope.launch {
            try {
                val response = RetrofitClient.instance.restartContainer(containerId)
                if (response.isSuccessful) {
                    fetchContainers()
                }
            } catch (e: Exception) {
                // Silently fail or log to a production crashlytics
            }
        }
    }

    fun pauseContainer(containerId: String, isPaused: Boolean) {
        viewModelScope.launch {
            try {
                val response = if (isPaused) {
                    RetrofitClient.instance.unpauseContainer(containerId)
                } else {
                    RetrofitClient.instance.pauseContainer(containerId)
                }
                if (response.isSuccessful) {
                    fetchContainers()
                }
            } catch (e: Exception) {
                // Silently fail or log to a production crashlytics
            }
        }
    }

    fun removeContainer(containerId: String) {
        viewModelScope.launch {
            try {
                val response = RetrofitClient.instance.removeContainer(containerId)
                if (response.isSuccessful) {
                    fetchContainers()
                }
            } catch (e: Exception) {
                // Silently fail or log to a production crashlytics
            }
        }
    }

    fun pruneContainers() {
        viewModelScope.launch {
            try {
                RetrofitClient.instance.pruneSystem()
                fetchContainers()
            } catch (e: Exception) {
                // Silently fail or log to a production crashlytics
            }
        }
    }
}
