package com.utn.sisve.data.remote.dto


/**
 * Response del servidor cuando llega un despacho de emergencia
 * @param id ID del despacho
 * @param address Dirección de a donde tiene que ir la ambulancia
 * @param emergencyType Tipo de emergencia (accidente, paro cardíaco, etc..)
 * @param description Descripción del llamado
 */
data class DispatchResponse(
    val id: String,
    val address: String,
    val emergencyType: String,
    val description: String
)
