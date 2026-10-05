package com.utn.sisve.data.remote

import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query

interface ApiService {

    @POST("auth/login")
    suspend fun login(@Body request: LoginRequest): LoginResponse

    @POST("location")
    suspend fun sendLocation(@Body request: LocationRequest) : Response<Unit>

    @POST("status")
    suspend fun updateStatus(@Body request: StatusUpdateRequest) : Response<Unit>

    @GET("dispatch")
    suspend fun getDispatch(): DispatchResponse

    @POST("dispatch/{id}/respond")
    suspend fun respondDispatch( @Path("id") dispatchId: String, @Query("accepted") accepted: Boolean) : Response<Unit>
}