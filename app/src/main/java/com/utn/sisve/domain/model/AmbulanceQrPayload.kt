package com.utn.sisve.domain.model

import com.google.gson.Gson
import com.google.gson.annotations.SerializedName

data class AmbulanceQrPayload(
    @SerializedName("ambulanceId") val ambulanceId: String,
    @SerializedName("licensePlate") val licensePlate: String,
    @SerializedName("serverUrl") val serverUrl: String? = null,
    @SerializedName("gpsIntervalSeconds") val gpsIntervalSeconds: Long? = null
) {
    fun isValid(): Boolean {
        return ambulanceId.isNotBlank() && licensePlate.isNotBlank()
    }

    companion object {
        private val gson = Gson()

        fun fromJson(json: String): AmbulanceQrPayload? {
            return try {
                val parsed = gson.fromJson(json, AmbulanceQrPayload::class.java)
                if (parsed != null && parsed.isValid()) parsed else null
            } catch (_: Exception) {
                null
            }
        }
    }
}