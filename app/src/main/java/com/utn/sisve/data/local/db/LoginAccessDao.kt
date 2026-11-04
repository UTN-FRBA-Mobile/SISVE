package com.utn.sisve.data.local.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.utn.sisve.data.local.Entity.LoginAccessEntity

@Dao
interface LoginAccessDao {
    @Insert
    suspend fun insert(access: LoginAccessEntity)

    @Query("UPDATE login_accesses SET uploaded = 1 WHERE id = :id")
    suspend fun markUploaded(id: String)
}
