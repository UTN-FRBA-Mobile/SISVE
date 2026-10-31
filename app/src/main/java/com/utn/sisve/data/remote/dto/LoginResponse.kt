package com.utn.sisve.data.remote.dto


/**
 * Respuesta del servidor al autenticar un operador en una ambulancia
 * @param token Token de autenticación
 * @param ambulanceId ID de la ambulancia asociada a la sesión
 * @param operator Operador identificado por el PIN dentro de esa ambulancia
 */
data class LoginResponse(
    val token: String,
    val ambulanceId: String,
    val operator: OperatorResponse
)
