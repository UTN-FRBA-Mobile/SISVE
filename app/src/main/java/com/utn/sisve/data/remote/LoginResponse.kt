package com.utn.sisve.data.remote


/**
 * Response del sv al autenticar una ambulancia
 * @param token Token de autenticación
 * @param ambulanceId ID de la ambulancia autenticada
 */
data class LoginResponse(
    val token: String,
    val ambulanceId: String
)
