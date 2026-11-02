package com.utn.sisve.data.local

import com.utn.sisve.domain.model.AmbulanceStatus
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

// Estado operativo local compartido por el dashboard y el control de inactividad.
// Solo vive en memoria; no envía cambios al servidor.
@Singleton
class AmbulanceStatusStore @Inject constructor() {
    private val _status = MutableStateFlow(AmbulanceStatus.FUERA_DE_SERVICIO)
    val status: StateFlow<AmbulanceStatus> = _status.asStateFlow()

    fun setStatus(status: AmbulanceStatus) {
        _status.value = status
    }
}
