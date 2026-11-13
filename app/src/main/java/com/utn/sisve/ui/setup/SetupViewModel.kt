package com.utn.sisve.ui.setup

import androidx.lifecycle.ViewModel
import com.utn.sisve.data.local.AmbulancePreferences
import com.utn.sisve.domain.model.AmbulanceQrPayload
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import javax.inject.Inject

data class SetupUiState(
    val ambulanceId: String = "",
    val licensePlate: String = "",
    val isAlreadyConfigured: Boolean = false,
    val qrError: String? = null
)

@HiltViewModel
class SetupViewModel @Inject constructor(
    private val ambulancePreferences: AmbulancePreferences
) : ViewModel() {

    private val _uiState = MutableStateFlow(SetupUiState())
    val uiState: StateFlow<SetupUiState> = _uiState.asStateFlow()

    init {
        val id = ambulancePreferences.getAmbulanceId()
        val plate = ambulancePreferences.getLicensePlate()
        _uiState.update {
            it.copy(
                ambulanceId = id,
                licensePlate = plate,
                isAlreadyConfigured = ambulancePreferences.isConfigured()
            )
        }
    }

    fun onAmbulanceIdChange(value: String) = _uiState.update {
        it.copy(ambulanceId = value, qrError = null)
    }

    fun onLicensePlateChange(value: String) = _uiState.update {
        it.copy(licensePlate = value, qrError = null)
    }

    fun applyQrCode(rawJson: String): Boolean {
        val payload = AmbulanceQrPayload.fromJson(rawJson)
        if (payload == null) {
            _uiState.update { it.copy(qrError = "Código QR inválido o datos incompletos") }
            return false
        }

        if (!payload.serverUrl.isNull(rawJson) && payload.gpsIntervalSeconds != null) {
            val url = payload.serverUrl ?: ambulancePreferences.getServerUrl()
            val interval = payload.gpsIntervalSeconds ?: ambulancePreferences.getGpsIntervalSeconds()
            ambulancePreferences.saveSettings(url, interval)
        }

        _uiState.update {
            it.copy(
                ambulanceId = payload.ambulanceId,
                licensePlate = payload.licensePlate,
                qrError = null
            )
        }
        return true
    }

    private fun String?.isNull(unused: String) = this.isNullOrBlank()

    fun saveAndContinue(): Boolean {
        val state = _uiState.value
        if (state.ambulanceId.isBlank() || state.licensePlate.isBlank()) return false
        ambulancePreferences.saveAmbulanceData(state.ambulanceId, state.licensePlate)
        return true
    }
}