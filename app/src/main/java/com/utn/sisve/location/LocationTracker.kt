package com.utn.sisve.location

import android.annotation.SuppressLint
import android.content.Context
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Bundle
import android.os.Looper
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

// Usa LocationManager (no Fused de Google Play Services) para que funcione en cualquier
// imagen de emulador y con la simulación de ubicación/GPX de Extended controls.
@Singleton
class LocationTracker @Inject constructor(
    @ApplicationContext private val context: Context,
    private val store: LocationStore
) {
    private val manager = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
    private var started = false
    private var baseIntervalMillis = 0L
    private var highFrequency = false
    private var previous: Location? = null
    private var bearing: Float? = null

    private val listener = object : LocationListener {
        override fun onLocationChanged(location: Location) {
            updateBearing(location)
            store.update(
                GeoFix(
                    coord = GeoCoord(location.latitude, location.longitude),
                    accuracyMeters = location.accuracy,
                    timestamp = System.currentTimeMillis(),
                    bearing = bearing
                )
            )
        }

        override fun onProviderEnabled(provider: String) {
            if (provider == LocationManager.GPS_PROVIDER) store.setGpsEnabled(true)
        }

        override fun onProviderDisabled(provider: String) {
            if (provider == LocationManager.GPS_PROVIDER) store.setGpsEnabled(false)
        }

        @Deprecated("Requerido en APIs anteriores a 30")
        override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) = Unit
    }

    // El rumbo se toma del GPS si hay velocidad, o de la dirección entre dos puntos separados
    // al menos 5 m (evita que el ruido haga girar el mapa estando detenido). Se suaviza para no dar saltos.
    private fun updateBearing(location: Location) {
        val last = previous
        val moved = last == null || last.distanceTo(location) >= MIN_MOVE_METERS
        val measured = when {
            location.hasBearing() && location.speed > 1f -> location.bearing
            last != null && moved -> (last.bearingTo(location) + 360f) % 360f
            else -> null
        }
        if (measured != null) {
            val old = bearing
            bearing = if (old == null) measured else {
                val delta = ((measured - old + 540f) % 360f) - 180f
                (old + delta * BEARING_SMOOTHING + 360f) % 360f
            }
        }
        if (moved) previous = Location(location)
    }

    @Synchronized
    fun start(intervalMillis: Long) {
        if (started) return
        baseIntervalMillis = intervalMillis
        register(seedLastKnown = true)
    }

    // Con el mapa visible se pide una lectura por segundo; si no, el intervalo configurado.
    @Synchronized
    fun setHighFrequency(enabled: Boolean) {
        if (highFrequency == enabled) return
        highFrequency = enabled
        if (started) {
            manager.removeUpdates(listener)
            started = false
            register(seedLastKnown = false)
        }
    }

    @SuppressLint("MissingPermission")
    private fun register(seedLastKnown: Boolean) {
        try {
            store.setGpsEnabled(manager.isProviderEnabled(LocationManager.GPS_PROVIDER))
            if (seedLastKnown) {
                manager.getLastKnownLocation(LocationManager.GPS_PROVIDER)?.let(listener::onLocationChanged)
            }
            manager.requestLocationUpdates(
                LocationManager.GPS_PROVIDER,
                if (highFrequency) HIGH_FREQUENCY_MILLIS else baseIntervalMillis,
                0f,
                listener,
                Looper.getMainLooper()
            )
            started = true
        } catch (e: SecurityException) {
            // Sin permiso: el controlador no debería haber iniciado el servicio.
            started = false
        } catch (e: IllegalArgumentException) {
            // El dispositivo no tiene proveedor GPS.
            store.setGpsEnabled(false)
        }
    }

    @Synchronized
    fun stop() {
        if (!started) return
        manager.removeUpdates(listener)
        started = false
        highFrequency = false
        previous = null
        bearing = null
        store.reset()
    }

    private companion object {
        const val HIGH_FREQUENCY_MILLIS = 1_000L
        const val MIN_MOVE_METERS = 5f
        const val BEARING_SMOOTHING = 0.5f
    }
}
