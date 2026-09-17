package com.clinic.callmonitor.network

import okhttp3.MultipartBody
import okhttp3.RequestBody
import retrofit2.Response
import retrofit2.http.*

data class ConsentPayload(
    val deviceUuid: String,
    val employeeName: String,
    val employeeId: String,
    val timestamp: Long
)

data class CallLogEntry(
    val deviceUuid: String,
    val employeeId: String,
    val phoneNumber: String,
    val callType: String, // INCOMING / OUTGOING / MISSED
    val startTimestamp: Long,
    val durationSeconds: Int,
    val recordingFileName: String? // اسم الملف لو موجود تسجيل مرتبط بيها
)

data class DeletionEvent(
    val deviceUuid: String,
    val employeeId: String,
    val recordingFileName: String,
    val detectedAt: Long,
    val note: String
)

data class SimpleResponse(val success: Boolean, val message: String?)

interface ApiService {

    @POST("consent.php")
    suspend fun recordConsent(@Body payload: ConsentPayload): Response<SimpleResponse>

    @POST("call_log.php")
    suspend fun uploadCallLog(@Body entry: CallLogEntry): Response<SimpleResponse>

    @Multipart
    @POST("upload_recording.php")
    suspend fun uploadRecording(
        @Part("deviceUuid") deviceUuid: RequestBody,
        @Part("employeeId") employeeId: RequestBody,
        @Part("fileName") fileName: RequestBody,
        @Part file: MultipartBody.Part
    ): Response<SimpleResponse>

    @POST("deletion_alert.php")
    suspend fun reportDeletion(@Body event: DeletionEvent): Response<SimpleResponse>

    @POST("heartbeat.php")
    suspend fun heartbeat(
        @Query("deviceUuid") deviceUuid: String,
        @Query("employeeId") employeeId: String,
        @Query("battery") battery: Int
    ): Response<SimpleResponse>
}
