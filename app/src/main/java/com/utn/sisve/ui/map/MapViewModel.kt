package com.utn.sisve.ui.map

import android.location.Location
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.utn.sisve.location.ActiveDispatchStore
import com.utn.sisve.location.AddressGeocoder
import com.utn.sisve.location.GeoCoord
import com.utn.sisve.location.LocationStore
import com.utn.sisve.location.LocationTracker
import com.utn.sisve.location.RouteMath
import com.utn.sisve.location.RouteProvider
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

// Estado del recorrido hacia el destino, para el cartel del mapa.
enum class RouteStatus { FOLLOWING, CALCULATING, RECALCULATING, UNAVAILABLE }

data class MapUiState(
    val current: GeoCoord? = null,
    val bearing: Float? = null,
    val destination: GeoCoord? = null,
    val destinationAddress: String? = null,
    val destinationNotFound: Boolean = false,
    val gpsEnabled: Boolean = true,
    // Tramo que falta recorrer: por calles si hay recorrido, o línea recta como alternativa.
    val path: List<GeoCoord> = emptyList(),
    val pathFollowsStreets: Boolean = false,
    val routeStatus: RouteStatus? = null,
    val distanceMeters: Float? = null,
    val follow: Boolean = true
)

@HiltViewModel
class MapViewModel @Inject constructor(
    locationStore: LocationStore,
    activeDispatchStore: ActiveDispatchStore,
    private val geocoder: AddressGeocoder,
    private val routeProvider: RouteProvider,
    private val tracker: LocationTracker
) : ViewModel() {

    private data class DestinationState(
        val coord: GeoCoord? = null,
        val notFound: Boolean = false
    )

    private data class RouteState(
        val route: List<GeoCoord> = emptyList(),
        val follow: Boolean = true,
        val requesting: Boolean = false,
        val failed: Boolean = false
    )

    private val destination = MutableStateFlow(DestinationState())
    private val routeState = MutableStateFlow(RouteState())

    private var routeJob: Job? = null
    private var lastRouteAttemptMillis = 0L

    private val dispatch = activeDispatchStore.dispatch

    val uiState: StateFlow<MapUiState> = combine(
        locationStore.lastFix,
        locationStore.gpsEnabled,
        dispatch,
        destination,
        routeState
    ) { fix, gpsEnabled, call, dest, rs ->
        val current = fix?.coord
        val snap = current?.let { RouteMath.snap(it, rs.route) }
        val onRoute = snap != null && snap.distanceMeters <= OFF_ROUTE_METERS

        val path = when {
            current != null && onRoute -> listOf(current) + rs.route.drop(snap!!.segmentIndex + 1)
            current != null && dest.coord != null -> listOf(current, dest.coord)
            else -> emptyList()
        }
        val distance = when {
            path.size < 2 -> null
            onRoute -> RouteMath.length(path).toFloat()
            else -> straightDistance(path.first(), path.last())
        }
        val status = when {
            dest.coord == null || current == null -> null
            onRoute -> RouteStatus.FOLLOWING
            rs.failed && !rs.requesting -> RouteStatus.UNAVAILABLE
            rs.route.isEmpty() -> RouteStatus.CALCULATING
            else -> RouteStatus.RECALCULATING
        }

        MapUiState(
            current = current,
            bearing = fix?.bearing,
            destination = dest.coord,
            destinationAddress = call?.address,
            destinationNotFound = dest.notFound,
            gpsEnabled = gpsEnabled,
            path = path,
            pathFollowsStreets = onRoute,
            routeStatus = status,
            distanceMeters = distance,
            follow = rs.follow
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), MapUiState())

    init {
        // Con el mapa abierto la posición se actualiza cada segundo para verla en tiempo real.
        tracker.setHighFrequency(true)

        viewModelScope.launch {
            dispatch.collect { call ->
                routeJob?.cancel()
                routeState.update { it.copy(route = emptyList(), requesting = false, failed = false) }
                lastRouteAttemptMillis = 0L
                destination.value = DestinationState()
                if (call != null) {
                    val coord = geocoder.locate(call.address)
                    destination.update { DestinationState(coord = coord, notFound = coord == null) }
                }
            }
        }

        // Pide el recorrido por calles y lo recalcula si la ambulancia se sale de él.
        viewModelScope.launch {
            combine(locationStore.lastFix.filterNotNull(), destination) { fix, dest -> fix.coord to dest.coord }
                .collect { (current, target) ->
                    if (target == null) return@collect
                    val offRoute =
                        (RouteMath.snap(current, routeState.value.route)?.distanceMeters ?: Double.MAX_VALUE) >
                            OFF_ROUTE_METERS
                    val now = System.currentTimeMillis()
                    if (offRoute && routeJob?.isActive != true && now - lastRouteAttemptMillis > RETRY_MILLIS) {
                        lastRouteAttemptMillis = now
                        routeJob = launch { requestRoute(current, target) }
                    }
                }
        }
    }

    private suspend fun requestRoute(from: GeoCoord, to: GeoCoord) {
        routeState.update { it.copy(requesting = true) }
        try {
            val route = routeProvider.route(from, to)
            routeState.update {
                if (route != null) it.copy(route = route, failed = false) else it.copy(failed = true)
            }
        } finally {
            routeState.update { it.copy(requesting = false) }
        }
    }

    fun recenter() = routeState.update { it.copy(follow = true) }

    fun onUserPan() = routeState.update { it.copy(follow = false) }

    override fun onCleared() {
        // Al cerrar el mapa se vuelve al intervalo configurado para ahorrar batería.
        tracker.setHighFrequency(false)
        super.onCleared()
    }

    private fun straightDistance(a: GeoCoord, b: GeoCoord): Float {
        val result = FloatArray(1)
        Location.distanceBetween(a.latitude, a.longitude, b.latitude, b.longitude, result)
        return result[0]
    }

    private companion object {
        const val OFF_ROUTE_METERS = 80.0
        const val RETRY_MILLIS = 15_000L
    }
}
