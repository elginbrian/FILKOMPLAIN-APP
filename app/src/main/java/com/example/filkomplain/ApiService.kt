package com.example.filkomplain.api

import okhttp3.MultipartBody
import okhttp3.RequestBody
import okhttp3.ResponseBody
import retrofit2.Call
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Part

data class ApiReportItem(
    val id: Int,
    val user_name: String?,
    val title: String?,
    val content: String?,
    val place: String?,
    val phone_number: String?,
    val status: String?,
    val attachment: String?,
    val created_at: String?,
    val updated_at: String?
)

data class ReportData(
    val reports: List<ApiReportItem>?
)

data class FetchReportsResponse(
    val success: Boolean,
    val data: ReportData?,
    val message: String?
)

data class RegisterRequest(
    val email: String,
    val username: String,
    val password: String
)

data class GeneralApiResponse(
    val success: Boolean,
    val message: String
)

data class LoginRequest(
    val email: String,
    val password: String
)

data class LoginData(
    val token: String
)

data class LoginResponse(
    val success: Boolean,
    val data: LoginData?,
    val message: String?
)

data class CreateReportRequest(
    val title: String,
    val user_name: String,
    val content: String,
    val place: String,
    val phone_number: String,
    val status: String
)

data class CreateReportResponse(
    val success: Boolean,
    val message: String?,
    val data: ApiReportItem?
)

data class UserProfileData(
    val id: Int,
    val email: String?,
    val nim: String?,
    val phone_number: String?,
    val program_studi: String?,
    val type: String?,
    val username: String?,
    val profile_image_url: String?
)

data class FetchProfileResponse(
    val success: Boolean,
    val data: UserProfileData?,
    val message: String?
)

data class UpdateProfileRequest(
    val username: String?,
    val nim: String?,
    val program_studi: String?,
    val phone_number: String?,
    val profile_image_url: String?
)

interface ApiService {
    @POST("api/v1/register")
    fun registerUser(
        @Body registerRequest: RegisterRequest
    ): Call<GeneralApiResponse>

    @POST("api/v1/login")
    fun loginUser(
        @Body loginRequest: LoginRequest
    ): Call<LoginResponse>

    @GET("api/v1/reports")
    fun getReports(
        @Header("Authorization") token: String
    ): Call<FetchReportsResponse>

    @POST("api/v1/reports")
    fun createReport(
        @Header("Authorization") token: String,
        @Body createReportRequest: CreateReportRequest
    ): Call<CreateReportResponse>

    @Multipart
    @POST("api/v1/reports")
    fun createReportWithAttachment(
        @Header("Authorization") token: String,
        @Part("title") title: RequestBody,
        @Part("user_name") userName: RequestBody,
        @Part("content") content: RequestBody,
        @Part("place") place: RequestBody,
        @Part("phone_number") phoneNumber: RequestBody,
        @Part("status") status: RequestBody,
        @Part attachment: MultipartBody.Part? // Part untuk file gambar tetap ada di sini
    ): Call<CreateReportResponse>

    @GET("api/v1/profile")
    fun getUserProfile(
        @Header("Authorization") token: String
    ): Call<FetchProfileResponse>

    @PUT("api/v1/profile")
    fun updateUserProfile(
        @Header("Authorization") token: String,
        @Body updateProfileRequest: UpdateProfileRequest
    ): Call<GeneralApiResponse>

    @Multipart
    @PUT("api/v1/profile")
    fun updateUserProfileWithAttachment(
        @Header("Authorization") token: String,
        @Part("username") username: RequestBody?,
        @Part("nim") nim: RequestBody?,
        @Part("program_studi") programStudi: RequestBody?,
        @Part("phone_number") phoneNumber: RequestBody?,
        @Part profileImageFile: MultipartBody.Part?
    ): Call<GeneralApiResponse>
}