package com.utn.sisve.domain.model

// Datos visibles de la sesión; el token se mantiene separado en SessionManager.
data class SessionData(
    val ambulanceId: String,
    val operatorId: String,
    val operatorName: String
)
