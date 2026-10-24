package com.utn.sisve.data.local.db

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import com.utn.sisve.data.local.Entity.PendingLocationEntity

@Dao
interface LocationDao {

    @Insert
    suspend fun insert(location: PendingLocationEntity)

    @Query("SELECT * FROM pending_locations ORDER BY timestamp ASC")
    suspend fun getAll(): List<PendingLocationEntity>

    @Delete
    suspend fun delete(location: PendingLocationEntity)

    @Query("DELETE FROM pending_locations")
    suspend fun deleteAll()
}