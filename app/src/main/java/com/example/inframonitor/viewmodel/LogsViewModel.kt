package com.example.inframonitor.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.inframonitor.data.network.RetrofitClient
import com.example.inframonitor.ui.UiState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class LogsViewModel : ViewModel() {

    private val _uiState = MutableStateFlow<UiState<List<String>>>(UiState.Success(emptyList()))
    val uiState: StateFlow<UiState<List<String>>> = _uiState.asStateFlow()

    fun fetchLogs(containerId: String) {
        viewModelScope.launch {
            _uiState.value = UiState.Loading
            try {
                val response = RetrofitClient.instance.getLogs(containerId)
                if (response.isSuccessful && response.body() != null) {
                    _uiState.value = UiState.Success(response.body()!!.logs)
                } else {
                    _uiState.value = UiState.Error("No se encontraron logs")
                }
            } catch (e: Exception) {
                _uiState.value = UiState.Error(e.message ?: "Error desconocido")
            }
        }
    }
}
