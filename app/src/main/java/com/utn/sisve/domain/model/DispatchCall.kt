package com.utn.sisve.domain.model

data class DispatchCall(
    val id: String,
    val address: String,
    val emergencyType: String,
    val description: String,
    val timestamp: Long = System.currentTimeMillis()
)
