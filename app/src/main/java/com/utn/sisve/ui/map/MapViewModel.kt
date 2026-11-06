package com.utn.sisve.ui.map

import android.location.Location
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.utn.sisve.location.ActiveDispatchStore
import com.utn.sisve.location.AddressGeocoder
import com.utn.sisve.location.GeoCoord
import com.utn.sisve.location.LocationStore
import com.utn.sisve.location.LocationTracker
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class MapUiState(
    val current: GeoCoord? = null,
    val destination: GeoCoord? = null,
    val destinationAddress: String? = null,
    val destinationNotFound: Boolean = false,
    val gpsEnabled: Boolean = true,
    val distanceMeters: Float? = null,
    val follow: Boolean = true
)

@HiltViewModel
class MapViewModel @Inject constructor(
    locationStore: LocationStore,
    activeDispatchStore: ActiveDispatchStore,
    private val geocoder: AddressGeocoder,
    private val tracker: LocationTracker
) : ViewModel() {

    private data class DestinationState(
        val coord: GeoCoord? = null,
        val notFound: Boolean = false
    )

    private val destination = MutableStateFlow(DestinationState())
    private val follow = MutableStateFlow(true)

    private val dispatch = activeDispatchStore.dispatch

    val uiState: StateFlow<MapUiState> = combine(
        locationStore.lastFix,
        locationStore.gpsEnabled,
        dispatch,
        destination,
        follow
    ) { fix, gpsEnabled, call, dest, follow ->
        val current = fix?.coord
        MapUiState(
            current = current,
            destination = dest.coord,
            destinationAddress = call?.address,
            destinationNotFound = dest.notFound,
            gpsEnabled = gpsEnabled,
            distanceMeters = if (current != null && dest.coord != null) distance(current, dest.coord) else null,
            follow = follow
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), MapUiState())

    init {
        // Con el mapa abierto la posición se actualiza cada segundo para verla en tiempo real.
        tracker.setHighFrequency(true)

        viewModelScope.launch {
            dispatch.collect { call ->
                destination.value = DestinationState()
                if (call != null) {
                    val coord = geocoder.locate(call.address)
                    destination.update { DestinationState(coord = coord, notFound = coord == null) }
                }
            }
        }
    }

    fun recenter() { follow.value = true }

    fun onUserPan() { follow.value = false }

    override fun onCleared() {
        // Al cerrar el mapa se vuelve al intervalo configurado para ahorrar batería.
        tracker.setHighFrequency(false)
        super.onCleared()
    }

    private fun distance(a: GeoCoord, b: GeoCoord): Float {
        val result = FloatArray(1)
        Location.distanceBetween(a.latitude, a.longitude, b.latitude, b.longitude, result)
        return result[0]
    }
}
