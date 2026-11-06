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

    private val listener = object : LocationListener {
        override fun onLocationChanged(location: Location) {
            store.update(
                GeoFix(
                    coord = GeoCoord(location.latitude, location.longitude),
                    accuracyMeters = location.accuracy,
                    timestamp = System.currentTimeMillis()
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

    @SuppressLint("MissingPermission")
    @Synchronized
    fun start(intervalMillis: Long) {
        if (started) return
        try {
            store.setGpsEnabled(manager.isProviderEnabled(LocationManager.GPS_PROVIDER))
            manager.getLastKnownLocation(LocationManager.GPS_PROVIDER)?.let(listener::onLocationChanged)
            manager.requestLocationUpdates(
                LocationManager.GPS_PROVIDER,
                intervalMillis,
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
        store.reset()
    }
}
