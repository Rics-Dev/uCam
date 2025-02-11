package com.ricsdev.uconnect.di

import android.os.Build
import androidx.annotation.RequiresApi
import com.ricsdev.uconnect.MainViewModel
import com.ricsdev.uconnect.presentation.setupScreen.PairingViewModel
import com.ricsdev.uconnect.util.CameraConfig
import com.ricsdev.uconnect.util.ClipboardManager
import com.ricsdev.uconnect.util.ConnectionManager
import com.ricsdev.uconnect.util.NsdHelper
import org.koin.core.module.Module
import org.koin.core.module.dsl.singleOf
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module

@RequiresApi(Build.VERSION_CODES.O)
actual val platformModule: Module = module {
    singleOf(::ClipboardManager)
    singleOf(::CameraConfig)
    singleOf(::NsdHelper)
    singleOf(::ConnectionManager)
    viewModelOf(::PairingViewModel)
    viewModelOf(::MainViewModel)
}