package com.capstone.recyclehelper

import retrofit2.Call
import retrofit2.http.*

// === 응답 DTO ===
data class LoginResponse(
    val token: String,
    val adminId: Long,
    val username: String?,
    val name: String?,
    val role: String?,
    val floor: Int?
)
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

// === 알림 DTO ===
data class NotificationDto(
    val id: Long,
    val type: String,
    val status: String,
    val title: String,
    val message: String?,
    val floor: Int?,
    val deviceId: Long?,
    val binId: Long?,
    val senderId: Long,
    val senderName: String?,
    val sentAt: String,
    val readAt: String?,
    val confirmedAt: String?
)

// === 요청 DTO ===
data class LoginRequest(val username: String, val password: String)
data class InspectionDoneRequest(
    val floor: Int?,
    val deviceId: Long?,
    val binId: Long?,
    val message: String?
)

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

    // === 알림 ===
    @POST("api/notifications/inspection-done")
    fun sendInspectionDone(@Body body: InspectionDoneRequest): Call<Void>

    @GET("api/notifications")
    fun getNotifications(): Call<List<NotificationDto>>

    @GET("api/notifications/unread-count")
    fun getUnreadCount(): Call<Map<String, Long>>

    @PATCH("api/notifications/{id}/read")
    fun markNotificationRead(@Path("id") id: Long): Call<Void>
}

