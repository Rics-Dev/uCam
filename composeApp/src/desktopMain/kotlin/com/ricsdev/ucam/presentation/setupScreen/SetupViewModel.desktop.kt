package com.ricsdev.ucam.presentation.setupScreen

import androidx.lifecycle.ViewModel
import com.ricsdev.ucam.util.AppLogger
import com.ricsdev.ucam.util.ConnectionManager
import com.ricsdev.ucam.util.ConnectionStateHolder

actual class SetupViewModel actual constructor(
    private val connectionManager: ConnectionManager,
    private val connectionStateHolder: ConnectionStateHolder,
    private val logger: AppLogger
) : ViewModel(){

    actual val connectionState = connectionStateHolder.connectionState


    fun getServerUrl(): String = connectionManager.getServerUrl()

}