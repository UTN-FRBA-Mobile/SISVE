package com.utn.sisve.data.remote

import com.utn.sisve.domain.model.AmbulanceStatus


/**
 * Request para actualizar el estado de la ambulancia.
 * @param ambulanceId ID de la ambulancia
 * @param status Estado actual de la ambulancia
 */
data class StatusUpdateRequest(
    val ambulanceId: String,
    val status: AmbulanceStatus
)
