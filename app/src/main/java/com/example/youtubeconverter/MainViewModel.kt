package com.example.youtubeconverter

import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow

class MainViewModel : ViewModel() {
    val isLoadingButtonOne = mutableStateOf(false)  // For MP4 to MP3 conversion
    val isLoadingButtonTwo = mutableStateOf(false)  // For YouTube to MP3 conversion
    val isLoadingButtonThree = mutableStateOf(false)  // For Playlist to MP3 conversion
    val isLoadingButtonFour = mutableStateOf(false)  // New state for YouTube to MP4 conversion

    // SharedFlow for service completion
    private val _serviceCompleted = MutableSharedFlow<Boolean>()
    val serviceCompleted = _serviceCompleted.asSharedFlow()

    fun setLoadingButtonOne(loading: Boolean) {
        isLoadingButtonOne.value = loading
    }

    fun setLoadingButtonTwo(loading: Boolean) {
        isLoadingButtonTwo.value = loading
    }

    fun setLoadingButtonThree(loading: Boolean) {
        isLoadingButtonThree.value = loading
    }

    fun setLoadingButtonFour(loading: Boolean) {  // New function for YouTube to MP4 conversion
        isLoadingButtonFour.value = loading
    }
}
