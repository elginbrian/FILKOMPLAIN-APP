package com.example.filkomplain.api

import okhttp3.MultipartBody
import okhttp3.RequestBody
import okhttp3.ResponseBody
import retrofit2.Call
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.Part

data class UploadResponse(
    val imageUrl: String
)

interface ApiService {
    @Multipart
    @POST("upload.php")
    fun uploadImage(
        @Part image: MultipartBody.Part,
        @Part("description") description: RequestBody
    ): Call<ResponseBody>
}