package com.utn.sisve.di

import com.utn.sisve.data.local.AmbulancePreferences
import com.utn.sisve.data.remote.ApiService
import com.utn.sisve.data.remote.AuthInterceptor
import com.utn.sisve.data.remote.DynamicBaseUrlInterceptor
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {

    @Provides
    @Singleton
    fun provideOkHttpClient(
        authInterceptor: AuthInterceptor,
        dynamicBaseUrlInterceptor: DynamicBaseUrlInterceptor
    ): OkHttpClient {
        return OkHttpClient.Builder()
            .addInterceptor(dynamicBaseUrlInterceptor) // <-- Reescribe la URL dinámicamente
            .addInterceptor(authInterceptor)
            .addInterceptor { chain ->
                val logging = HttpLoggingInterceptor().apply {
                    redactHeader("Authorization")
                    level = if (
                        chain.request().body is MultipartBody ||
                        chain.request().url.encodedPath.endsWith("/notifications/fcm-token") ||
                        chain.request().url.encodedPath.endsWith("/auth/login")
                    ) {
                        HttpLoggingInterceptor.Level.HEADERS
                    } else {
                        HttpLoggingInterceptor.Level.BODY
                    }
                }
                logging.intercept(chain)
            }
            .build()
    }

    @Provides
    @Singleton
    fun provideRetrofit(okHttpClient: OkHttpClient, preferences: AmbulancePreferences): Retrofit {
        return Retrofit.Builder()
            .baseUrl(preferences.getServerUrl())
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }

    @Provides
    @Singleton
    fun provideApiService(retrofit: Retrofit): ApiService {
        return retrofit.create(ApiService::class.java)
    }
}