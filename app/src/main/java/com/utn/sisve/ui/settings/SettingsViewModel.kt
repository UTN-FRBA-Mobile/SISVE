package com.utn.sisve.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.utn.sisve.data.local.AmbulancePreferences
import com.utn.sisve.data.local.AmbulanceStatusStore
import com.utn.sisve.domain.model.AmbulanceStatus
import com.utn.sisve.domain.model.SessionState
import com.utn.sisve.session.SessionManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import java.util.concurrent.TimeUnit
import javax.inject.Inject

enum class SettingsError {
    EMPTY_SERVER_URL,
    INVALID_SERVER_URL,
    INVALID_GPS_INTERVAL
}

enum class ConnectionStatus {
    IDLE,
    TESTING,
    SUCCESS,
    FAILED
}

data class SettingsUiState(
    val ambulanceId: String = "",
    val licensePlate: String = "",
    val serverUrl: String = "",
    val gpsIntervalSeconds: String = "",
    val isSavedSuccess: Boolean = false,
    val connectionStatus: ConnectionStatus = ConnectionStatus.IDLE,
    val connectionMessage: String? = null,
    val error: SettingsError? = null
)

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val preferences: AmbulancePreferences,
    private val sessionManager: SessionManager,
    private val ambulanceStatusStore: AmbulanceStatusStore
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
                connectionStatus = ConnectionStatus.IDLE,
                error = null
            )
        }
    }

    fun onServerUrlChange(url: String) {
        _uiState.update {
            it.copy(
                serverUrl = url,
                isSavedSuccess = false,
                connectionStatus = ConnectionStatus.IDLE,
                error = null
            )
        }
    }

    fun onGpsIntervalChange(interval: String) {
        _uiState.update { it.copy(gpsIntervalSeconds = interval, isSavedSuccess = false, error = null) }
    }

    fun testConnection() {
        val rawUrl = _uiState.value.serverUrl.trim()
        val parsed = rawUrl.toHttpUrlOrNull()
        if (parsed == null) {
            _uiState.update {
                it.copy(
                    connectionStatus = ConnectionStatus.FAILED,
                    connectionMessage = "URL inválida"
                )
            }
            return
        }

        _uiState.update {
            it.copy(
                connectionStatus = ConnectionStatus.TESTING,
                connectionMessage = null
            )
        }

        viewModelScope.launch {
            val startTime = System.currentTimeMillis()
            val result = withContext(Dispatchers.IO) {
                try {
                    val client = OkHttpClient.Builder()
                        .connectTimeout(4, TimeUnit.SECONDS)
                        .readTimeout(4, TimeUnit.SECONDS)
                        .build()

                    val request = Request.Builder().url(parsed).build()
                    client.newCall(request).execute().use { response ->
                        val elapsed = System.currentTimeMillis() - startTime
                        // Cualquier respuesta HTTP recibida (incluso 404) confirma que el host y puerto están vivos
                        Pair(true, "Conectado ($elapsed ms)")
                    }
                } catch (e: Exception) {
                    Pair(false, "No se pudo conectar con el servidor")
                }
            }

            _uiState.update {
                it.copy(
                    connectionStatus = if (result.first) ConnectionStatus.SUCCESS else ConnectionStatus.FAILED,
                    connectionMessage = result.second
                )
            }
        }
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

        preferences.saveSettings(cleanUrl, interval)
        _uiState.update { it.copy(isSavedSuccess = true, error = null) }
        return true
    }

    fun resetAmbulance(onResetComplete: () -> Unit) {
        if (sessionManager.state.value !is SessionState.Active) {
            return
        }

        preferences.clearAmbulance()
        ambulanceStatusStore.setStatus(AmbulanceStatus.FUERA_DE_SERVICIO)
        sessionManager.logout()

        onResetComplete()
    }
}