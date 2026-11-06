package com.utn.sisve.data.local

import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AmbulanceSession @Inject constructor(
    private val preferences: AmbulancePreferences
) {
    fun getAmbulanceId(): String = preferences.getAmbulanceId()
    fun getLicensePlate(): String = preferences.getLicensePlate()
    fun getServerUrl(): String = preferences.getServerUrl()
    fun getGpsIntervalSeconds(): Long = preferences.getGpsIntervalSeconds()
    fun isConfigured(): Boolean = preferences.isConfigured()
}