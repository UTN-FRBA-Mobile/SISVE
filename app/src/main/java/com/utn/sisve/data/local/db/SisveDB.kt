package com.utn.sisve.data.local.db

import androidx.room.Database
import androidx.room.RoomDatabase
import com.utn.sisve.data.local.Entity.LoginAccessEntity
import com.utn.sisve.data.local.Entity.PendingLocationEntity

@Database(entities = [PendingLocationEntity::class, LoginAccessEntity::class], version = 3)
abstract class SisveDB : RoomDatabase() {
    abstract fun locationDao(): LocationDao
    abstract fun loginAccessDao(): LoginAccessDao
}