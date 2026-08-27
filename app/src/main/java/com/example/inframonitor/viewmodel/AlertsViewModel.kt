package com.example.inframonitor.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.inframonitor.data.models.AlertResponse
import com.example.inframonitor.data.network.RetrofitClient
import com.example.inframonitor.ui.UiState
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class AlertsViewModel : ViewModel() {

    private val _uiState = MutableStateFlow<UiState<List<AlertResponse>>>(UiState.Loading)
    val uiState: StateFlow<UiState<List<AlertResponse>>> = _uiState.asStateFlow()

    init {
        startPolling()
    }

    private fun startPolling() {
        viewModelScope.launch {
            while (true) {
                fetchAlerts()
                delay(10000)
            }
        }
    }

    fun fetchAlerts() {
        viewModelScope.launch {
            _uiState.value = UiState.Loading
            try {
                // Llamada protegida para evitar Crash de la app
                val response = RetrofitClient.instance.getAlerts()
                _uiState.value = UiState.Success(response)
            } catch (e: Exception) {
                _uiState.value = UiState.Error("Error al obtener alertas: ${e.localizedMessage}")
            }
        }
    }
}
