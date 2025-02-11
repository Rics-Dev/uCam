package com.ricsdev.uconnect.di

import com.ricsdev.uconnect.MainViewModel
import com.ricsdev.uconnect.presentation.setupScreen.PairingViewModel
import com.ricsdev.uconnect.util.AppLogger
import com.ricsdev.uconnect.util.ClipboardManager
import com.ricsdev.uconnect.util.ConnectionManager
import com.ricsdev.uconnect.util.NsdHelper
import com.ricsdev.uconnect.util.VirtualCamera
import org.koin.core.module.Module
import org.koin.core.module.dsl.singleOf
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module

actual val platformModule: Module = module {
    singleOf(::ClipboardManager)
    singleOf(::ConnectionManager)
    singleOf (::VirtualCamera)
    singleOf(::NsdHelper)
    single { AppLogger }
    viewModelOf(::PairingViewModel)
    viewModelOf(::MainViewModel)
}