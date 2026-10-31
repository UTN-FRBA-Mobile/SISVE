package com.utn.sisve.ui.settings

import androidx.lifecycle.ViewModel
import com.utn.sisve.data.local.AmbulancePreferences
import com.utn.sisve.domain.model.SessionState
import com.utn.sisve.session.SessionManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import javax.inject.Inject

enum class SettingsError {
    EMPTY_SERVER_URL,
    INVALID_SERVER_URL,
    INVALID_GPS_INTERVAL
}

data class SettingsUiState(
    val ambulanceId: String = "",
    val licensePlate: String = "",
    val serverUrl: String = "",
    val gpsIntervalSeconds: String = "",
    val isSavedSuccess: Boolean = false,
    val error: SettingsError? = null
)

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val preferences: AmbulancePreferences,
    private val sessionManager: SessionManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    init {
        loadSettings()
    }

    private fun loadSettings() {
        if (sessionManager.state.value !is SessionState.Active) {
            return
        }

        _uiState.update {
            it.copy(
                ambulanceId = preferences.getAmbulanceId(),
                licensePlate = preferences.getLicensePlate(),
                serverUrl = preferences.getServerUrl(),
                gpsIntervalSeconds = preferences.getGpsIntervalSeconds().toString(),
                isSavedSuccess = false,
                error = null
            )
        }
    }

    fun onServerUrlChange(url: String) {
        _uiState.update { it.copy(serverUrl = url, isSavedSuccess = false, error = null) }
    }

    fun onGpsIntervalChange(interval: String) {
        _uiState.update { it.copy(gpsIntervalSeconds = interval, isSavedSuccess = false, error = null) }
    }

    fun saveSettings(): Boolean {
        if (sessionManager.state.value !is SessionState.Active) {
            return false
        }

        val current = _uiState.value
        val cleanUrl = current.serverUrl.trim()
        val interval = current.gpsIntervalSeconds.trim().toLongOrNull()

        if (cleanUrl.isBlank()) {
            _uiState.update { it.copy(error = SettingsError.EMPTY_SERVER_URL) }
            return false
        }

        val parsedUrl = cleanUrl.toHttpUrlOrNull()

        if (
            parsedUrl == null || parsedUrl.query != null || parsedUrl.fragment != null ||
            parsedUrl.username.isNotEmpty() || parsedUrl.password.isNotEmpty()
        ) {
            _uiState.update { it.copy(error = SettingsError.INVALID_SERVER_URL) }
            return false
        }

        if (interval == null || interval <= 0) {
            _uiState.update { it.copy(error = SettingsError.INVALID_GPS_INTERVAL) }
            return false
        }

        // TODO No funciona, Retrofit es singleton y conserva la URL con la que se creó.
        preferences.saveSettings(cleanUrl, interval)
        _uiState.update { it.copy(isSavedSuccess = true, error = null) }
        return true
    }

    fun resetAmbulance(onResetComplete: () -> Unit) {
        if (sessionManager.state.value !is SessionState.Active) {
            return
        }

        preferences.clearAmbulance()
        sessionManager.logout()

        onResetComplete()
    }
}
