package com.utn.sisve.data.local.db

import androidx.room.Database
import androidx.room.RoomDatabase
import com.utn.sisve.data.local.Entity.PendingLocationEntity

@Database(entities = [PendingLocationEntity::class], version = 1)
abstract class SisveDB : RoomDatabase() {
    abstract fun locationDao(): LocationDao
}