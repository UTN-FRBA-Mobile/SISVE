package com.utn.sisve.ui.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
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
    val status: AmbulanceStatus = AmbulanceStatus.LIBRE,
    val isLoading: Boolean = false,
    val error: String? = null
)

@HiltViewModel
class DashboardViewModel @Inject constructor() : ViewModel() {

    private val _uiState = MutableStateFlow(DashboardUiState())
    val uiState: StateFlow<DashboardUiState> = _uiState.asStateFlow()

    fun loadAmbulanceData() {
        // TODO Miembro 4: reemplazar con AmbulanceSession.getAmbulanceId()
        _uiState.update { it.copy(ambulanceId = "AMB-001") }
    }

    fun updateStatus(newStatus: AmbulanceStatus) {
        viewModelScope.launch {
            _uiState.update { it.copy(status = newStatus, isLoading = true) }
            // TODO Miembro 6: llamar al repositorio para sincronizar con el servidor
            _uiState.update { it.copy(isLoading = false) }
        }
    }
}
