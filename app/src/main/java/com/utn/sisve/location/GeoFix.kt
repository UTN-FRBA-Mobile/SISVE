package com.utn.sisve.location

// Posición geográfica simple, independiente de cualquier librería de mapas.
data class GeoCoord(val latitude: Double, val longitude: Double)

// Lectura de GPS. El timestamp la hace distinta aunque el emulador repita coordenadas.
data class GeoFix(
    val coord: GeoCoord,
    val accuracyMeters: Float,
    val timestamp: Long,
    // Rumbo de avance en grados (0 = norte), o null si todavía no se movió lo suficiente.
    val bearing: Float? = null
)
