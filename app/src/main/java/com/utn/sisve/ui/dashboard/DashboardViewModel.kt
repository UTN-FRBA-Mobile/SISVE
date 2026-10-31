package com.utn.sisve.ui.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.utn.sisve.data.local.AmbulanceSession
import com.utn.sisve.domain.model.AmbulanceStatus
import com.utn.sisve.domain.model.SessionState
import com.utn.sisve.session.SessionManager
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
    private val ambulanceSession: AmbulanceSession,
    private val sessionManager: SessionManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(DashboardUiState())
    val uiState: StateFlow<DashboardUiState> = _uiState.asStateFlow()

    fun loadAmbulanceData() {
        val activeSession = sessionManager.state.value as? SessionState.Active

        _uiState.update {
            it.copy(
                ambulanceId = ambulanceSession.getAmbulanceId(),
                licensePlate = ambulanceSession.getLicensePlate(),
                operatorName = activeSession?.session?.operatorName.orEmpty()
            )
        }
    }

    fun logout() {
        // TODO Miembro 6: enviar FUERA_DE_SERVICIO al servidor antes de eliminar el token.
        _uiState.update {
            it.copy(status = AmbulanceStatus.FUERA_DE_SERVICIO, operatorName = "")
        }

        sessionManager.logout()
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