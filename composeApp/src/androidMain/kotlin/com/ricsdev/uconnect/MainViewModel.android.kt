package com.ricsdev.uconnect

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ricsdev.uconnect.util.ConnectionManager
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