package com.utn.sisve.data.local.Entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "login_accesses")
data class LoginAccessEntity(
    @PrimaryKey val id: String,
    val ambulanceId: String,
    val operatorId: String,
    val operatorName: String,
    val capturedAt: Long,
    val photoFileName: String,
    @ColumnInfo(defaultValue = "0") val uploaded: Boolean = false
)
