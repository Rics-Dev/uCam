package com.ricsdev.ucam.di

import com.ricsdev.ucam.MainViewModel
import com.ricsdev.ucam.presentation.pairingScreen.PairingViewModel
import com.ricsdev.ucam.service.ConnectionService
import com.ricsdev.ucam.util.CameraConfig
import com.ricsdev.ucam.util.ClipboardManager
import com.ricsdev.ucam.util.ConnectionManager
import org.koin.core.module.Module
import org.koin.core.module.dsl.singleOf
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module

actual val platformModule: Module = module {
    singleOf(::ClipboardManager)
    singleOf(::CameraConfig)
    singleOf(::ConnectionManager)
    viewModelOf(::PairingViewModel)
    viewModelOf(::MainViewModel)
}