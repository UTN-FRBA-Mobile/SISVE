package com.utn.sisve.location

import com.utn.sisve.domain.model.DispatchCall
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

// Última ubicación y estado del GPS. Lo escribe el servicio y lo leen el mapa y el dashboard.
@Singleton
class LocationStore @Inject constructor() {
    private val _lastFix = MutableStateFlow<GeoFix?>(null)
    val lastFix: StateFlow<GeoFix?> = _lastFix.asStateFlow()

    private val _gpsEnabled = MutableStateFlow(true)
    val gpsEnabled: StateFlow<Boolean> = _gpsEnabled.asStateFlow()

    fun update(fix: GeoFix) { _lastFix.value = fix }
    fun setGpsEnabled(enabled: Boolean) { _gpsEnabled.value = enabled }
    fun reset() {
        _lastFix.value = null
        _gpsEnabled.value = true
    }
}

// Despacho aceptado, para que el mapa conozca el destino sin cambiar las rutas de navegación.
@Singleton
class ActiveDispatchStore @Inject constructor() {
    private val _dispatch = MutableStateFlow<DispatchCall?>(null)
    val dispatch: StateFlow<DispatchCall?> = _dispatch.asStateFlow()

    fun set(call: DispatchCall) { _dispatch.value = call }
    fun clear() { _dispatch.value = null }
}
