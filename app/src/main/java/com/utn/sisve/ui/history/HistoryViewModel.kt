package com.utn.sisve.ui.history

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.utn.sisve.data.local.AmbulanceSession
import com.utn.sisve.data.remote.dto.ServiceHistoryItemDto
import com.utn.sisve.domain.model.DispatchCall
import com.utn.sisve.domain.model.SessionState
import com.utn.sisve.location.ActiveDispatchStore
import com.utn.sisve.repository.ApiRepository
import com.utn.sisve.session.SessionManager
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class HistoryUiState(
    val items: List<ServiceHistoryItemDto> = emptyList(),
    val isLoading: Boolean = true,
    val error: String? = null
)

@HiltViewModel
class HistoryViewModel @Inject constructor(
    private val apiRepository: ApiRepository,
    private val sessionManager: SessionManager,
    private val ambulanceSession: AmbulanceSession,
    private val activeDispatchStore: ActiveDispatchStore
) : ViewModel() {

    private val _uiState = MutableStateFlow(HistoryUiState())
    val uiState: StateFlow<HistoryUiState> = _uiState.asStateFlow()

    init {
        loadHistory()
    }

    fun loadHistory() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }

            val session = sessionManager.state.value as? SessionState.Active
            val operatorId = session?.session?.operatorId ?: "OP-001"
            val ambulanceId = ambulanceSession.getAmbulanceId().ifBlank { "AMB-001" }

            try {
                val history = apiRepository.getServiceHistory(operatorId, ambulanceId)
                val sortedHistory = history.reversed()
                _uiState.update { it.copy(isLoading = false, items = sortedHistory) }
            } catch (error: CancellationException) {
                throw error
            } catch (_: Exception) {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        error = "No se pudo cargar el historial de servicios."
                    )
                }
            }
        }
    }

    fun onItemClick(
        item: ServiceHistoryItemDto,
        onNavigateToDispatch: (String) -> Unit,
        onNavigateToMap: () -> Unit
    ) {
        val status = item.status.uppercase()
        when (status) {
            "ACCEPTED" -> {
                val call = DispatchCall(
                    id = item.id,
                    address = item.address,
                    emergencyType = item.emergencyType,
                    description = item.description,
                    priority = item.priority ?: "ALTA",
                    latitude = item.latitude,
                    longitude = item.longitude
                )
                activeDispatchStore.set(call)
                onNavigateToMap()
            }
            "PENDING" -> {
                onNavigateToDispatch(item.id)
            }
            else -> {
                // Finalizada o Rechazada no realizan ninguna acción
            }
        }
    }
}
