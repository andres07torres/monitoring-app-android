package com.example.inframonitor.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.inframonitor.data.models.ContainerResponse
import com.example.inframonitor.data.network.RetrofitClient
import com.example.inframonitor.ui.UiState
import com.example.inframonitor.utils.NetworkErrorMapper
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class ContainersViewModel : ViewModel() {

    private val _uiState = MutableStateFlow<UiState<List<ContainerResponse>>>(UiState.Loading)
    val uiState: StateFlow<UiState<List<ContainerResponse>>> = _uiState.asStateFlow()

    private val _actionMessage = MutableStateFlow<String?>(null)
    val actionMessage: StateFlow<String?> = _actionMessage.asStateFlow()

    init {
        fetchContainers()
    }

    fun clearActionMessage() {
        _actionMessage.value = null
    }

    fun fetchContainers() {
        viewModelScope.launch {
            _uiState.value = UiState.Loading
            try {
                val response = RetrofitClient.instance.getContainers()
                if (response.isSuccessful && response.body() != null) {
                    _uiState.value = UiState.Success(response.body()!!)
                } else {
                    _uiState.value = UiState.Error(NetworkErrorMapper.parseResponseError(response))
                }
            } catch (e: Exception) {
                _uiState.value = UiState.Error(NetworkErrorMapper.parseThrowable(e))
            }
        }
    }

    fun toggleContainer(containerId: String, currentStatus: String) {
        viewModelScope.launch {
            try {
                val s = currentStatus.lowercase().trim()
                val isRunning = s == "running" || s.startsWith("up")
                val response = if (isRunning) {
                    RetrofitClient.instance.stopContainer(containerId)
                } else {
                    RetrofitClient.instance.startContainer(containerId)
                }
                
                if (response.isSuccessful) {
                    fetchContainers()
                } else {
                    handleActionError(NetworkErrorMapper.parseResponseError(response))
                }
            } catch (e: Exception) {
                handleActionError(NetworkErrorMapper.parseThrowable(e))
            }
        }
    }

    fun restartContainer(containerId: String) {
        viewModelScope.launch {
            try {
                val response = RetrofitClient.instance.restartContainer(containerId)
                if (response.isSuccessful) {
                    fetchContainers()
                } else {
                    handleActionError(NetworkErrorMapper.parseResponseError(response))
                }
            } catch (e: Exception) {
                handleActionError(NetworkErrorMapper.parseThrowable(e))
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
                } else {
                    handleActionError(NetworkErrorMapper.parseResponseError(response))
                }
            } catch (e: Exception) {
                handleActionError(NetworkErrorMapper.parseThrowable(e))
            }
        }
    }

    fun removeContainer(containerId: String) {
        viewModelScope.launch {
            try {
                val response = RetrofitClient.instance.removeContainer(containerId)
                if (response.isSuccessful) {
                    fetchContainers()
                } else {
                    handleActionError(NetworkErrorMapper.parseResponseError(response))
                }
            } catch (e: Exception) {
                handleActionError(NetworkErrorMapper.parseThrowable(e))
            }
        }
    }

    fun pruneContainers() {
        viewModelScope.launch {
            try {
                val response = RetrofitClient.instance.pruneSystem()
                if (response.isSuccessful) {
                    fetchContainers()
                } else {
                    handleActionError(NetworkErrorMapper.parseResponseError(response))
                }
            } catch (e: Exception) {
                handleActionError(NetworkErrorMapper.parseThrowable(e))
            }
        }
    }

    private suspend fun handleActionError(errorMessage: String) {
        _actionMessage.value = errorMessage
        // Reintentamos actualizar la lista por si la operación continuó en segundo plano
        fetchContainers()
        delay(4000)
        _actionMessage.value = null
    }
}
