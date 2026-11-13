package com.utn.sisve.data.remote

import com.utn.sisve.session.SessionManager
import okhttp3.Interceptor
import okhttp3.Response
import javax.inject.Inject

class AuthInterceptor @Inject constructor(
    private val sessionManager: SessionManager
) : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()

        if (request.url.encodedPath.endsWith("/auth/login")) {
            return chain.proceed(request)
        }

        val token = sessionManager.getToken() ?: return chain.proceed(request)
        val authenticated = request.newBuilder()
            .header("Authorization", "Bearer $token")
            .build()

        return chain.proceed(authenticated)
    }
}