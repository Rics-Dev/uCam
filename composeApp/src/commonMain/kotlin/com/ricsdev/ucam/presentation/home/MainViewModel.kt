package com.ricsdev.ucam.presentation.home

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ricsdev.ucam.domain.repository.MainRepository
import kotlinx.coroutines.launch

class MainViewModel(
    private val mainRepository: MainRepository,
): ViewModel() {



    var state by mutableStateOf("")
        private set



    init {
        viewModelScope.launch {
            state = mainRepository.getData()
        }
    }
}