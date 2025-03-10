package com.panevrn.emovoiceapplication.retrofit
import okhttp3.MultipartBody
import okhttp3.ResponseBody
import retrofit2.Call
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.Part


interface ApiService {

    @Multipart
    @POST("/analyze_audio") // API-эндпоинт на сервере
    fun uploadAudio(@Part file: MultipartBody.Part): Call<ResponseBody>

}