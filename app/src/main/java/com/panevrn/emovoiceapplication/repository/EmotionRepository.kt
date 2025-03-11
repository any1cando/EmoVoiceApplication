package com.panevrn.emovoiceapplication.repository

import android.util.Log
import com.panevrn.emovoiceapplication.dto.EmotionResponse
import com.panevrn.emovoiceapplication.retrofit.RetrofitClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import java.io.File

class EmotionRepository {

    suspend fun analyzeEmotion(audioFile: File): EmotionResponse? {
        return withContext(Dispatchers.IO) {
            try {
                val requestFile = audioFile.asRequestBody("audio/wav".toMediaTypeOrNull())
                val filePart = MultipartBody.Part.createFormData("file", audioFile.name, requestFile)

                val response = RetrofitClient.apiService.uploadAudio(filePart).execute()

                if (response.isSuccessful) {
                    response.body()?.let {
                        val responseBody = it.string()
                        Log.d("SERVER_RESPONSE", "Ответ от сервера: $responseBody")

                        return@withContext RetrofitClient.gson.fromJson(responseBody, EmotionResponse::class.java)
                    }
                } else {
                    Log.e("SERVER_RESPONSE", "Ошибка сервера: ${response.code()}")
                    null
                }
            } catch (e: Exception) {
                Log.e("SERVER_RESPONSE", "Ошибка соединения: ${e.message}", e)
                null
            }
        }
    }
}