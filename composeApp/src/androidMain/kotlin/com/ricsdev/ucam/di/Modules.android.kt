package com.ricsdev.ucam.di

import com.ricsdev.ucam.presentation.pairingScreen.PairingViewModel
import com.ricsdev.ucam.util.CameraManager
import com.ricsdev.ucam.util.KtorClient
import org.koin.core.module.Module
import org.koin.core.module.dsl.singleOf
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module

actual val platformModule: Module = module {
    singleOf(::CameraManager)
    singleOf(::KtorClient)
    viewModelOf(::PairingViewModel)
}