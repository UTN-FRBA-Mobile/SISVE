package com.utn.sisve.data.remote

import com.utn.sisve.data.local.AmbulancePreferences
import okhttp3.Interceptor
import okhttp3.Response
import javax.inject.Inject

class AuthInterceptor @Inject constructor(private val preferences: AmbulancePreferences) : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
                                .newBuilder()
                                .addHeader("Authorization","Bearer ") // TODO: Agregar getToken de preferences cuando exista.
                                .build()

        return chain.proceed(request);
    }

}