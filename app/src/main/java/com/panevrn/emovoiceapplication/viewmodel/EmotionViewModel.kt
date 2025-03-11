package com.panevrn.emovoiceapplication.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.panevrn.emovoiceapplication.dto.EmotionResponse
import com.panevrn.emovoiceapplication.repository.EmotionRepository
import kotlinx.coroutines.launch
import java.io.File


class EmotionViewModel: ViewModel() {

    private val repository = EmotionRepository()

    private val _emotionData = MutableLiveData<EmotionResponse?>()
    val emotionData: LiveData<EmotionResponse?> get() = _emotionData

    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> get() = _isLoading

    fun analyzeEmotion(audioFile: File) {
        _isLoading.value = true
        viewModelScope.launch {
            val result = repository.analyzeEmotion(audioFile)
            _emotionData.value = result
            _isLoading.value = false
        }
    }

}