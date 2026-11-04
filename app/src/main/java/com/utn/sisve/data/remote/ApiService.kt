package com.utn.sisve.data.remote

import com.utn.sisve.data.remote.dto.DispatchResponse
import com.utn.sisve.data.remote.dto.LocationRequest
import com.utn.sisve.data.remote.dto.LoginRequest
import com.utn.sisve.data.remote.dto.LoginResponse
import com.utn.sisve.data.remote.dto.StatusUpdateRequest
import okhttp3.MultipartBody
import okhttp3.RequestBody
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.Part
import retrofit2.http.Path
import retrofit2.http.Query

interface ApiService {

    @POST("auth/login")
    suspend fun login(@Body request: LoginRequest): LoginResponse

    @Multipart
    @POST("access/photo")
    suspend fun uploadLoginPhoto(
        @Part("accessId") accessId: RequestBody,
        @Part("capturedAt") capturedAt: RequestBody,
        @Part photo: MultipartBody.Part
    ): Response<Unit>

    @POST("location")
    suspend fun sendLocation(@Body request: LocationRequest) : Response<Unit>

    @POST("status")
    suspend fun updateStatus(@Body request: StatusUpdateRequest) : Response<Unit>

    @GET("dispatch")
    suspend fun getDispatch(): DispatchResponse

    @POST("dispatch/{id}/respond")
    suspend fun respondDispatch( @Path("id") dispatchId: String, @Query("accepted") accepted: Boolean) : Response<Unit>
}