package com.utn.sisve.di

import android.content.Context
import android.content.SharedPreferences
import android.os.SystemClock
import com.utn.sisve.session.SessionClock
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun provideSharedPreferences(@ApplicationContext context: Context): SharedPreferences =
        context.getSharedPreferences("sisve_prefs", Context.MODE_PRIVATE)

    @Provides
    @Singleton
    fun provideSessionClock(): SessionClock = SessionClock { SystemClock.elapsedRealtime() }
}
