package com.utn.sisve.ui.dispatch

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.utn.sisve.R
import com.utn.sisve.domain.model.DispatchCall
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class DispatchUiState(
    val call: DispatchCall? = null,
    val isLoading: Boolean = true,
    val isAccepted: Boolean = false
)

@HiltViewModel
class DispatchViewModel @Inject constructor(
    @ApplicationContext private val context: Context
) : ViewModel() {

    private val _uiState = MutableStateFlow(DispatchUiState())
    val uiState: StateFlow<DispatchUiState> = _uiState.asStateFlow()

    fun loadCall(callId: String) {
        viewModelScope.launch {
            // TODO Miembro 6: cargar el despacho real desde el repositorio usando callId
            _uiState.update {
                it.copy(
                    isLoading = false,
                    call = DispatchCall(
                        id = callId,
                        // Solo para el despacho simulado; los datos reales vendrán del repositorio.
                        address = context.getString(R.string.dispatch_sample_address),
                        emergencyType = context.getString(R.string.dispatch_sample_type),
                        description = context.getString(R.string.dispatch_sample_description)
                    )
                )
            }
        }
    }

    fun acceptDispatch() {
        viewModelScope.launch {
            // TODO Miembro 6: notificar al servidor que se aceptó el despacho
            _uiState.update { it.copy(isAccepted = true) }
        }
    }

    fun rejectDispatch() {
        viewModelScope.launch {
            // TODO Miembro 6: notificar al servidor que se rechazó el despacho
        }
    }
}
