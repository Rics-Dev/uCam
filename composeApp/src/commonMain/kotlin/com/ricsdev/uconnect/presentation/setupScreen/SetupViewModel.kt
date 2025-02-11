package com.ricsdev.uconnect.presentation.setupScreen

import androidx.lifecycle.ViewModel
import com.ricsdev.uconnect.util.AppLogger
import com.ricsdev.uconnect.util.ConnectionManager
import com.ricsdev.uconnect.util.ConnectionState
import com.ricsdev.uconnect.util.ConnectionStateHolder
import kotlinx.coroutines.flow.StateFlow

expect class SetupViewModel(
    connectionManager: ConnectionManager,
    connectionStateHolder: ConnectionStateHolder,
    logger: AppLogger
) : ViewModel {
    val connectionState: StateFlow<ConnectionState>
}
