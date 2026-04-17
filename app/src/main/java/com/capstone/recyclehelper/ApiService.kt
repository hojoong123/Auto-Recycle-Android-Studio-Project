package com.capstone.recyclehelper

import retrofit2.Call
import retrofit2.http.*

// === 응답 DTO ===
data class LoginResponse(val token: String)
data class DeviceResponse(
    val id: Long,
    val deviceCode: String?,
    val deviceName: String?,
    val location: String?
)
data class BinResponse(
    val id: Long,
    val binCode: String?,
    val trashTypeCode: String?,
    val fillPercent: Int?,
    val isFull: Boolean?,
    val errorFlag: Boolean?
)

// === 요청 DTO ===
data class LoginRequest(val username: String, val password: String)

// === API 인터페이스 ===
interface ApiService {

    @POST("api/auth/login")
    fun login(@Body request: LoginRequest): Call<LoginResponse>

    @GET("api/devices")
    fun getDevices(@Header("Authorization") token: String): Call<List<DeviceResponse>>

    @GET("api/bins/{deviceId}")
    fun getBins(
        @Header("Authorization") token: String,
        @Path("deviceId") deviceId: Long
    ): Call<List<BinResponse>>

    @POST("api/bins/{id}/reset")
    fun resetBin(
        @Header("Authorization") token: String,
        @Path("id") binId: Long
    ): Call<Void>
}

