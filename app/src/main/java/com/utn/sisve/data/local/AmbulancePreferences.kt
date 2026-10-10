package com.utn.sisve.data.local

import android.content.SharedPreferences
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AmbulancePreferences @Inject constructor(
    private val prefs: SharedPreferences
) {

    fun saveAmbulanceData(ambulanceId: String, licensePlate: String) {
        prefs.edit()
            .putString(KEY_AMBULANCE_ID, ambulanceId.trim())
            .putString(KEY_LICENSE_PLATE, licensePlate.trim())
            .apply()
    }

    fun saveSettings(serverUrl: String, gpsInterval: Long) {
        prefs.edit()
            .putString(KEY_SERVER_URL, serverUrl.trim())
            .putLong(KEY_GPS_INTERVAL, gpsInterval)
            .apply()
    }

    fun getAmbulanceId(): String = prefs.getString(KEY_AMBULANCE_ID, "") ?: ""
    fun getLicensePlate(): String = prefs.getString(KEY_LICENSE_PLATE, "") ?: ""
    fun getServerUrl(): String = prefs.getString(KEY_SERVER_URL, DEFAULT_URL) ?: DEFAULT_URL
    fun getGpsIntervalSeconds(): Long = prefs.getLong(KEY_GPS_INTERVAL, DEFAULT_GPS_INTERVAL)

    fun isConfigured(): Boolean = getAmbulanceId().isNotBlank()

    fun clearAmbulance() {
        prefs.edit()
            .remove(KEY_AMBULANCE_ID)
            .remove(KEY_LICENSE_PLATE)
            .apply()
    }

    companion object {
        private const val KEY_AMBULANCE_ID = "ambulance_id"
        private const val KEY_LICENSE_PLATE = "license_plate"
        private const val KEY_SERVER_URL = "server_url"
        private const val KEY_GPS_INTERVAL = "gps_interval_seconds"

        const val DEFAULT_URL = "http://10.0.2.2:8080/api/v1/"
        const val DEFAULT_GPS_INTERVAL = 10L
    }
}