package com.utn.sisve.data.local.Entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "pending_locations")
data class PendingLocationEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val ambulanceId: String,
    val latitude: Double,
    val longitude: Double,
    val timestamp: Long = System.currentTimeMillis()
)
