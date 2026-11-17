package com.example.inframonitor.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.inframonitor.data.models.ContainerResponse
import com.example.inframonitor.data.network.RetrofitClient
import com.example.inframonitor.ui.UiState
import com.example.inframonitor.utils.NetworkErrorMapper
import com.example.inframonitor.utils.NotificationHelper
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class ContainersViewModel(application: Application) : AndroidViewModel(application) {

    private val notificationHelper = NotificationHelper(application)

    private val _uiState = MutableStateFlow<UiState<List<ContainerResponse>>>(UiState.Loading)
    val uiState: StateFlow<UiState<List<ContainerResponse>>> = _uiState.asStateFlow()

    private val _lastModifiedContainerId = MutableStateFlow<String?>(null)
    val lastModifiedContainerId: StateFlow<String?> = _lastModifiedContainerId.asStateFlow()

    private val _scrollTrigger = MutableStateFlow(0)
    val scrollTrigger: StateFlow<Int> = _scrollTrigger.asStateFlow()

    private val _actionMessage = MutableStateFlow<String?>(null)
    val actionMessage: StateFlow<String?> = _actionMessage.asStateFlow()

    init {
        fetchContainers()
    }

    fun clearActionMessage() {
        _actionMessage.value = null
    }

    private fun getContainerName(containerId: String): String {
        val list = (_uiState.value as? UiState.Success)?.data ?: emptyList()
        val c = list.find { it.id == containerId }
        return c?.name ?: containerId.take(6)
    }

    fun fetchContainers(showLoading: Boolean = true) {
        viewModelScope.launch {
            if (showLoading) {
                _uiState.value = UiState.Loading
            }
            try {
                val response = RetrofitClient.instance.getContainers()
                if (response.isSuccessful && response.body() != null) {
                    val rawList = response.body()!!
                    val targetId = _lastModifiedContainerId.value
                    val orderedList = if (!targetId.isNullOrBlank()) {
                        val targetContainer = rawList.find { it.id == targetId }
                        if (targetContainer != null) {
                            listOf(targetContainer) + rawList.filter { it.id != targetId }
                        } else {
                            rawList
                        }
                    } else {
                        rawList
                    }
                    _uiState.value = UiState.Success(orderedList)
                } else {
                    val err = NetworkErrorMapper.parseResponseError(response)
                    _uiState.value = UiState.Error(err)
                }
            } catch (e: Exception) {
                _uiState.value = UiState.Error(NetworkErrorMapper.parseThrowable(e))
            }
        }
    }

    fun toggleContainer(containerId: String, currentStatus: String) {
        viewModelScope.launch {
            try {
                val containerName = getContainerName(containerId)
                val s = currentStatus.lowercase().trim()
                val isPaused = s == "paused" || s.contains("paused")
                val isRunning = (s == "running" || s.startsWith("up")) && !isPaused

                val response = when {
                    isPaused -> RetrofitClient.instance.unpauseContainer(containerId)
                    isRunning -> RetrofitClient.instance.stopContainer(containerId)
                    else -> RetrofitClient.instance.startContainer(containerId)
                }
                
                if (response.isSuccessful) {
                    _lastModifiedContainerId.value = containerId
                    _scrollTrigger.value += 1
                    if (isPaused) {
                        notificationHelper.showNotification(
                            "Alerta de Contenedor",
                            "El contenedor '$containerName' se reanudó."
                        )
                    } else if (isRunning) {
                        notificationHelper.showNotification(
                            "Alerta de Contenedor",
                            "El contenedor '$containerName' se ha apagado."
                        )
                    } else {
                        notificationHelper.showNotification(
                            "Alerta de Contenedor",
                            "El contenedor '$containerName' se encendió."
                        )
                    }
                    fetchContainers(showLoading = false)
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
                val containerName = getContainerName(containerId)
                val response = RetrofitClient.instance.restartContainer(containerId)
                if (response.isSuccessful) {
                    _lastModifiedContainerId.value = containerId
                    _scrollTrigger.value += 1
                    notificationHelper.showNotification(
                        "Alerta de Contenedor",
                        "El contenedor '$containerName' se reinició."
                    )
                    fetchContainers(showLoading = false)
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
                val containerName = getContainerName(containerId)
                val response = if (isPaused) {
                    RetrofitClient.instance.unpauseContainer(containerId)
                } else {
                    RetrofitClient.instance.pauseContainer(containerId)
                }
                if (response.isSuccessful) {
                    _lastModifiedContainerId.value = containerId
                    _scrollTrigger.value += 1
                    if (isPaused) {
                        notificationHelper.showNotification(
                            "Alerta de Contenedor",
                            "El contenedor '$containerName' se reanudó."
                        )
                    } else {
                        notificationHelper.showNotification(
                            "Alerta de Contenedor",
                            "El contenedor '$containerName' se pausó."
                        )
                    }
                    fetchContainers(showLoading = false)
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
                val containerName = getContainerName(containerId)
                val response = RetrofitClient.instance.removeContainer(containerId)
                if (response.isSuccessful) {
                    _lastModifiedContainerId.value = containerId
                    _scrollTrigger.value += 1
                    notificationHelper.showNotification(
                        "Alerta de Contenedor",
                        "El contenedor '$containerName' se eliminó."
                    )
                    fetchContainers(showLoading = false)
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
                    fetchContainers(showLoading = false)
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
        fetchContainers(showLoading = false)
        delay(4000)
        _actionMessage.value = null
    }
}
