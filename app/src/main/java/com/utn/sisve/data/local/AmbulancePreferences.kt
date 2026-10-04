package com.utn.sisve.data.local

import android.content.SharedPreferences
import javax.inject.Inject

class AmbulancePreferences @Inject constructor(private val prefs: SharedPreferences) {

    fun saveAmbulanceData(ambulanceId: String, licensePlate: String) {
        prefs.edit()
            .putString(KEY_AMBULANCE_ID, ambulanceId.trim())
            .putString(KEY_LICENSE_PLATE, licensePlate.trim())
            .apply()
    }

    fun getAmbulanceId(): String = prefs.getString(KEY_AMBULANCE_ID, "") ?: ""
    fun getLicensePlate(): String = prefs.getString(KEY_LICENSE_PLATE, "") ?: ""
    fun isConfigured(): Boolean = getAmbulanceId().isNotBlank()

    companion object {
        private const val KEY_AMBULANCE_ID = "ambulance_id"
        private const val KEY_LICENSE_PLATE = "license_plate"
    }
}
