package com.utn.sisve.ui.settings

import androidx.lifecycle.ViewModel
import com.utn.sisve.data.local.AmbulancePreferences
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import javax.inject.Inject

data class SettingsUiState(
    val ambulanceId: String = "",
    val licensePlate: String = "",
    val serverUrl: String = "",
    val gpsIntervalSeconds: String = "",
    val isSavedSuccess: Boolean = false,
    val errorMessage: String? = null
)

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val preferences: AmbulancePreferences
) : ViewModel() {

    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    init {
        loadSettings()
    }

    private fun loadSettings() {
        _uiState.update {
            it.copy(
                ambulanceId = preferences.getAmbulanceId(),
                licensePlate = preferences.getLicensePlate(),
                serverUrl = preferences.getServerUrl(),
                gpsIntervalSeconds = preferences.getGpsIntervalSeconds().toString(),
                isSavedSuccess = false,
                errorMessage = null
            )
        }
    }

    fun onServerUrlChange(url: String) {
        _uiState.update { it.copy(serverUrl = url, isSavedSuccess = false, errorMessage = null) }
    }

    fun onGpsIntervalChange(interval: String) {
        _uiState.update { it.copy(gpsIntervalSeconds = interval, isSavedSuccess = false, errorMessage = null) }
    }

    fun saveSettings(): Boolean {
        val current = _uiState.value
        val cleanUrl = current.serverUrl.trim()
        val interval = current.gpsIntervalSeconds.trim().toLongOrNull()

        if (cleanUrl.isBlank()) {
            _uiState.update { it.copy(errorMessage = "La URL del servidor no puede estar vacía") }
            return false
        }

        if (interval == null || interval <= 0) {
            _uiState.update { it.copy(errorMessage = "El intervalo de GPS debe ser mayor a 0 segundos") }
            return false
        }

        preferences.saveSettings(cleanUrl, interval)
        _uiState.update { it.copy(isSavedSuccess = true, errorMessage = null) }
        return true
    }

    fun resetAmbulance(onResetComplete: () -> Unit) {
        preferences.clearAmbulance()
        onResetComplete()
    }
}

