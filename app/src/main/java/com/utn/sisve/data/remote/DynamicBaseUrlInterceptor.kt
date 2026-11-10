package com.utn.sisve.data.remote

import com.utn.sisve.data.local.AmbulancePreferences
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import okhttp3.Interceptor
import okhttp3.Response
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DynamicBaseUrlInterceptor @Inject constructor(
    private val preferences: AmbulancePreferences
) : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        var request = chain.request()
        val configuredUrl = preferences.getServerUrl().toHttpUrlOrNull()

        if (configuredUrl != null) {
            val newUrl = request.url.newBuilder()
                .scheme(configuredUrl.scheme)
                .host(configuredUrl.host)
                .port(configuredUrl.port)
                .build()

            request = request.newBuilder()
                .url(newUrl)
                .build()
        }

        return chain.proceed(request)
    }
}