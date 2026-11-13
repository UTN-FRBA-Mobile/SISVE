package com.utn.sisve.ui.dispatch

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.utn.sisve.R
import com.utn.sisve.domain.model.DispatchCall
import com.utn.sisve.location.ActiveDispatchStore
import com.utn.sisve.repository.ApiRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class DispatchUiState(
    val call: DispatchCall? = null,
    val isLoading: Boolean = true,
    val isAccepted: Boolean = false,
    val isRejected: Boolean = false,
    val isResponding: Boolean = false,
    val error: String? = null
)

@HiltViewModel
class DispatchViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val activeDispatchStore: ActiveDispatchStore,
    private val apiRepository: ApiRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(DispatchUiState())
    val uiState: StateFlow<DispatchUiState> = _uiState.asStateFlow()

    fun loadCall(callId: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }

            try {
                val call = if (callId == "TEST-001") {
                    DispatchCall(
                        id = callId,
                        address = context.getString(R.string.dispatch_sample_address),
                        emergencyType = context.getString(R.string.dispatch_sample_type),
                        description = context.getString(R.string.dispatch_sample_description),
                        priority = "ALTA",
                        latitude = -34.5981,
                        longitude = -58.4202
                    )
                } else {
                    apiRepository.getDispatch(callId).let { dispatch ->
                        DispatchCall(
                            id = dispatch.id,
                            address = dispatch.address,
                            emergencyType = dispatch.emergencyType,
                            description = dispatch.description,
                            priority = dispatch.priority ?: "ALTA",
                            latitude = dispatch.latitude,
                            longitude = dispatch.longitude
                        )
                    }
                }

                activeDispatchStore.setPending(call)
                _uiState.update { it.copy(isLoading = false, call = call) }
            } catch (error: CancellationException) {
                throw error
            } catch (_: Exception) {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        error = context.getString(R.string.dispatch_load_error)
                    )
                }
            }
        }
    }

    fun acceptDispatch() {
        viewModelScope.launch {
            val call = _uiState.value.call ?: return@launch
            _uiState.update { it.copy(isResponding = true, error = null) }

            try {
                if (call.id != "TEST-001") {
                    apiRepository.respondDispatch(call.id, accepted = true)
                }
                activeDispatchStore.set(call)
                _uiState.update { it.copy(isResponding = false, isAccepted = true) }
            } catch (error: CancellationException) {
                throw error
            } catch (_: Exception) {
                _uiState.update {
                    it.copy(
                        isResponding = false,
                        error = context.getString(R.string.dispatch_response_error)
                    )
                }
            }
        }
    }

    fun rejectDispatch() {
        viewModelScope.launch {
            val call = _uiState.value.call ?: return@launch
            _uiState.update { it.copy(isResponding = true, error = null) }

            try {
                if (call.id != "TEST-001") {
                    apiRepository.respondDispatch(call.id, accepted = false)
                }
                activeDispatchStore.clearPending()
                _uiState.update { it.copy(isResponding = false, isRejected = true) }
            } catch (error: CancellationException) {
                throw error
            } catch (_: Exception) {
                _uiState.update {
                    it.copy(
                        isResponding = false,
                        error = context.getString(R.string.dispatch_response_error)
                    )
                }
            }
        }
    }
}
