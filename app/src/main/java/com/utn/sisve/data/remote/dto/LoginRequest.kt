package com.utn.sisve.data.remote.dto



/**
 * Request para autenticar una ambulancia
 * @param ambulanceId ID de la ambulancia asignada al dispositivo
 * @param pin PIN de 4 o 6 dígitos del operador
 */
data class LoginRequest(
    val ambulanceId: String,
    val pin: String
)
