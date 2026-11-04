package com.utn.sisve.di

import android.content.Context
import androidx.room.Room
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.utn.sisve.data.local.db.LocationDao
import com.utn.sisve.data.local.db.LoginAccessDao
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

    private val migration1To2 = object : Migration(1, 2) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL(
                """
                CREATE TABLE IF NOT EXISTS login_accesses (
                    id TEXT NOT NULL,
                    ambulanceId TEXT NOT NULL,
                    operatorId TEXT NOT NULL,
                    operatorName TEXT NOT NULL,
                    capturedAt INTEGER NOT NULL,
                    photoFileName TEXT NOT NULL,
                    PRIMARY KEY(id)
                )
                """.trimIndent()
            )
        }
    }

    private val migration2To3 = object : Migration(2, 3) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL("ALTER TABLE login_accesses ADD COLUMN uploaded INTEGER NOT NULL DEFAULT 0")
        }
    }

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): SisveDB {
        return Room.databaseBuilder(
            context,
            SisveDB::class.java,
            "sisve_db"
        )
            .addMigrations(migration1To2, migration2To3)
            .build()
    }

    @Provides
    @Singleton
    fun provideLocationDao(database: SisveDB): LocationDao {
        return database.locationDao()
    }

    @Provides
    @Singleton
    fun provideLoginAccessDao(database: SisveDB): LoginAccessDao {
        return database.loginAccessDao()
    }
}