package com.utn.sisve.data.remote.dto

import com.google.gson.annotations.SerializedName

data class ServiceHistoryResponse(
    @SerializedName("serviceRequests")
    val serviceRequests: List<ServiceHistoryItemDto> = emptyList()
)

data class ServiceHistoryItemDto(
    val id: String,
    val ambulanceId: String,
    val address: String,
    val emergencyType: String,
    val priority: String?,
    val description: String,
    val status: String,
    val createdAt: String?,
    val respondedAt: String?,
    val latitude: Double? = null,
    val longitude: Double? = null
)
