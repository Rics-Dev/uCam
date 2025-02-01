package com.ricsdev.ucam

import android.content.Context
import android.content.Intent
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ricsdev.ucam.service.ConnectionService
import com.ricsdev.ucam.util.ConnectionManager
import com.ricsdev.ucam.util.ConnectionState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

actual class MainViewModel (
    private val clientConnection: ConnectionManager,
): ViewModel() {

//    private val _connectionState = MutableStateFlow<ConnectionState>(ConnectionState.Disconnected)
//    val connectionState: StateFlow<ConnectionState> = _connectionState.asStateFlow()


//    init {
//        viewModelScope.launch {
//            clientConnection.connectionState.collect { state ->
//                _connectionState.value = state
//            }
//        }
//    }



    override fun onCleared() {
        super.onCleared()
//        context.stopService(Intent(context, ConnectionService::class.java))
        viewModelScope.launch {
            clientConnection.disconnect()
        }
    }
}