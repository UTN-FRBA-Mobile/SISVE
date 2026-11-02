package com.utn.sisve.ui.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.utn.sisve.data.local.AmbulanceSession
import com.utn.sisve.data.local.AmbulanceStatusStore
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
    private val sessionManager: SessionManager,
    private val ambulanceStatusStore: AmbulanceStatusStore
) : ViewModel() {

    private val _uiState = MutableStateFlow(
        DashboardUiState(status = ambulanceStatusStore.status.value)
    )
    val uiState: StateFlow<DashboardUiState> = _uiState.asStateFlow()

    // Actualizar estado segun ambulanceStatusStore.
    init {
        viewModelScope.launch {
            ambulanceStatusStore.status.collect { status ->
                _uiState.update { it.copy(status = status) }
            }
        }
    }

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
        if (sessionManager.state.value !is SessionState.Active) {
            return
        }

        // TODO Miembro 6: enviar FUERA_DE_SERVICIO al servidor antes de eliminar el token.
        ambulanceStatusStore.setStatus(AmbulanceStatus.FUERA_DE_SERVICIO)
        _uiState.update { it.copy(operatorName = "") }

        sessionManager.logout()
    }

    fun toggleStatus() {
        viewModelScope.launch {
            if (sessionManager.state.value !is SessionState.Active) {
                return@launch
            }

            val newStatus = when (ambulanceStatusStore.status.value) {
                AmbulanceStatus.FUERA_DE_SERVICIO -> AmbulanceStatus.EN_SERVICIO
                AmbulanceStatus.EN_SERVICIO -> AmbulanceStatus.FUERA_DE_SERVICIO
            }
            _uiState.update { it.copy(isLoading = true) }
            ambulanceStatusStore.setStatus(newStatus)
            // TODO Miembro 6: notificar al servidor el cambio de estado
            _uiState.update { it.copy(isLoading = false) }
        }
    }
}