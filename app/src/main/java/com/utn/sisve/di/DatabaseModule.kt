package com.utn.sisve.di

import android.content.Context
import androidx.room.Room
import com.utn.sisve.data.local.db.LocationDao
import com.utn.sisve.data.local.db.SisveDB
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton


@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): SisveDB {
        return Room.databaseBuilder(
            context,
            SisveDB::class.java,
            "sisve_db"
        ).build()
    }

    @Provides
    @Singleton
    fun provideLocationDao(database: SisveDB): LocationDao {
        return database.locationDao()
    }
}