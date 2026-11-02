package com.utn.sisve.data.remote.dto



/**
 * Request para autenticar un operador en la ambulancia asignada
 * @param ambulanceId ID de la ambulancia asignada al dispositivo
 * @param pin PIN de entre 4 y 6 dígitos, único dentro de la ambulancia
 */
data class LoginRequest(
    val ambulanceId: String,
    val pin: String
)
