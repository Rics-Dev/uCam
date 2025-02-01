package com.ricsdev.ucam.di

import com.ricsdev.ucam.presentation.pairingScreen.PairingViewModel
import com.ricsdev.ucam.util.AppLogger
import com.ricsdev.ucam.util.ClipboardManager
import com.ricsdev.ucam.util.KtorServer
import com.ricsdev.ucam.util.VirtualCamera
import org.koin.core.module.Module
import org.koin.core.module.dsl.singleOf
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module

actual val platformModule: Module = module {
    singleOf(::ClipboardManager)
    singleOf(::KtorServer)
    singleOf (::VirtualCamera)
    single { AppLogger }
    viewModelOf(::PairingViewModel)
}