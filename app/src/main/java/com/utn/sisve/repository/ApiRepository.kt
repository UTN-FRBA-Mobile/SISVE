package com.utn.sisve.repository

import com.utn.sisve.data.local.AmbulancePreferences
import com.utn.sisve.data.local.Entity.PendingLocationEntity
import com.utn.sisve.data.local.db.LocationDao
import com.utn.sisve.data.remote.ApiService
import com.utn.sisve.data.remote.DispatchResponse
import com.utn.sisve.data.remote.LocationRequest
import com.utn.sisve.data.remote.LoginResponse
import com.utn.sisve.domain.model.AmbulanceStatus
import javax.inject.Inject

class ApiRepository @Inject constructor(
    private val apiService: ApiService,
    private val locationDao: LocationDao,
    private val preferences: AmbulancePreferences
) {


    // TODO: Esta mockeado y hardcodeado, modificar cuando este el back

    suspend fun login(ambulanceId: String, pin: String): LoginResponse {
        return LoginResponse(token = "123jlaksjxlz213", ambulanceId = ambulanceId)
        // return apiService.login(LoginRequest(ambulanceId, pin))
    }

    suspend fun sendLocation(latitude: Double, longitude: Double) {
        val request = LocationRequest(
            ambulanceId = preferences.getAmbulanceId(),
            latitude = latitude,
            longitude = longitude,
            timestamp = System.currentTimeMillis()
        )
        try {
            apiService.sendLocation(request)
        } catch (e: Exception) {
            locationDao.insert(
                PendingLocationEntity(
                    ambulanceId = preferences.getAmbulanceId(),
                    latitude = latitude,
                    longitude = longitude
                )
            )
        }
    }

    suspend fun updateStatus(status: AmbulanceStatus) {
        // TODO: modificar cuando este el back
        // apiService.updateStatus(StatusUpdateRequest(preferences.getAmbulanceId(), status))
    }

    suspend fun getDispatch(): DispatchResponse {
        // TODO: modificar cuando este el back
        return DispatchResponse(
            id = "asdjl123as",
            address = "Av. Medrano 421",
            emergencyType = "Accidente",
            description = "Colisión entre una bici y una moto",
        )
        // return apiService.getDispatch()
    }

    suspend fun respondDispatch(dispatchId: String, accepted: Boolean) {
        // TODO: modificar cuando este el back
        // apiService.respondDispatch(dispatchId, accepted)
    }

    suspend fun sendPendingLocations() {
        val pending = locationDao.getAll()
        pending.forEach { location ->
            try {
                apiService.sendLocation(
                    LocationRequest(
                        ambulanceId = location.ambulanceId,
                        latitude = location.latitude,
                        longitude = location.longitude,
                        timestamp = location.timestamp
                    )
                )
                locationDao.delete(location)
            } catch (e: Exception) {
                return
            }
        }
    }
}