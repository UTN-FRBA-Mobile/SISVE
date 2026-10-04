package com.utn.sisve.ui.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.utn.sisve.data.local.AmbulancePreferences
import com.utn.sisve.domain.model.AmbulanceStatus
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class DashboardUiState(
    val ambulanceId: String = "",
    val operatorName: String = "",
    val licensePlate: String = "",
    val status: AmbulanceStatus = AmbulanceStatus.FUERA_DE_SERVICIO,
    val isLoading: Boolean = false
)

@HiltViewModel
class DashboardViewModel @Inject constructor(
    private val ambulancePreferences: AmbulancePreferences
) : ViewModel() {

    private val _uiState = MutableStateFlow(DashboardUiState())
    val uiState: StateFlow<DashboardUiState> = _uiState.asStateFlow()

    fun loadAmbulanceData() {
        // TODO Miembro 2: reemplazar operatorName con SessionManager.getOperatorName()
        _uiState.update {
            it.copy(
                ambulanceId = ambulancePreferences.getAmbulanceId(),
                licensePlate = ambulancePreferences.getLicensePlate(),
                operatorName = "Operador"
            )
        }
    }

    fun toggleStatus() {
        viewModelScope.launch {
            val newStatus = when (_uiState.value.status) {
                AmbulanceStatus.FUERA_DE_SERVICIO -> AmbulanceStatus.EN_SERVICIO
                AmbulanceStatus.EN_SERVICIO -> AmbulanceStatus.FUERA_DE_SERVICIO
            }
            _uiState.update { it.copy(status = newStatus, isLoading = true) }
            // TODO Miembro 6: notificar al servidor el cambio de estado
            _uiState.update { it.copy(isLoading = false) }
        }
    }
}
