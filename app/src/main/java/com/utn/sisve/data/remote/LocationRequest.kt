package com.utn.sisve.data.remote


/**
 * Request para enviar la ubicacion GPS de la ambulancia.
 * @param ambulanceId ID de la ambulancia
 * @param latitude Latitud de la ubicación
 * @param longitude Longitud de la ubicación
 * @param timestamp Momento donde se tomó la ubicación (en ms)
 */
data class LocationRequest(
    val ambulanceId: String,
    val latitude: Double,
    val longitude: Double,
    val timestamp: Long
)
