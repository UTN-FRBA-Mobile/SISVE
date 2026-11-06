package com.utn.sisve.ui.map

import android.location.Location
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.utn.sisve.location.ActiveDispatchStore
import com.utn.sisve.location.AddressGeocoder
import com.utn.sisve.location.GeoCoord
import com.utn.sisve.location.LocationStore
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
    val recenterTick: Int = 0
)

@HiltViewModel
class MapViewModel @Inject constructor(
    locationStore: LocationStore,
    activeDispatchStore: ActiveDispatchStore,
    private val geocoder: AddressGeocoder
) : ViewModel() {

    private data class DestinationState(
        val coord: GeoCoord? = null,
        val notFound: Boolean = false
    )

    private val destination = MutableStateFlow(DestinationState())
    private val recenterTick = MutableStateFlow(0)

    private val dispatch = activeDispatchStore.dispatch

    val uiState: StateFlow<MapUiState> = combine(
        locationStore.lastFix,
        locationStore.gpsEnabled,
        dispatch,
        destination,
        recenterTick
    ) { fix, gpsEnabled, call, dest, tick ->
        val current = fix?.coord
        MapUiState(
            current = current,
            destination = dest.coord,
            destinationAddress = call?.address,
            destinationNotFound = dest.notFound,
            gpsEnabled = gpsEnabled,
            distanceMeters = if (current != null && dest.coord != null) distance(current, dest.coord) else null,
            recenterTick = tick
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), MapUiState())

    init {
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

    fun recenter() = recenterTick.update { it + 1 }

    private fun distance(a: GeoCoord, b: GeoCoord): Float {
        val result = FloatArray(1)
        Location.distanceBetween(a.latitude, a.longitude, b.latitude, b.longitude, result)
        return result[0]
    }
}
