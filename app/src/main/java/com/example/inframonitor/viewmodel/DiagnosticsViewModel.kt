package com.example.inframonitor.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.inframonitor.data.network.CommandRequest
import com.example.inframonitor.data.network.RetrofitClient
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class DiagnosticsViewModel : ViewModel() {

    private val _commandOutput = MutableStateFlow("")
    val commandOutput: StateFlow<String> = _commandOutput.asStateFlow()

    private val _isExecuting = MutableStateFlow(false)
    val isExecuting: StateFlow<Boolean> = _isExecuting.asStateFlow()

    private val _healthStatus = MutableStateFlow<Map<String, String>>(emptyMap())
    val healthStatus: StateFlow<Map<String, String>> = _healthStatus.asStateFlow()

    fun executeCommand(command: String) {
        viewModelScope.launch {
            _isExecuting.value = true
            try {
                val response = RetrofitClient.instance.executeCommand(CommandRequest(command))
                if (response.isSuccessful) {
                    _commandOutput.value = response.body()?.output ?: "Sin salida"
                } else {
                    _commandOutput.value = "Error: ${response.code()}"
                }
            } catch (e: Exception) {
                _commandOutput.value = "Error: ${e.localizedMessage}"
            } finally {
                _isExecuting.value = false
            }
        }
    }

    fun checkHealth(url: String) {
        viewModelScope.launch {
            try {
                val response = RetrofitClient.instance.checkHealth(url)
                val statusText = if (response.isSuccessful) {
                    "OK (${response.body()?.status})"
                } else {
                    "Error: ${response.code()}"
                }
                
                val current = _healthStatus.value.toMutableMap()
                current[url] = statusText
                _healthStatus.value = current
            } catch (e: Exception) {
                val current = _healthStatus.value.toMutableMap()
                current[url] = "Error: ${e.localizedMessage}"
                _healthStatus.value = current
            }
        }
    }
}
