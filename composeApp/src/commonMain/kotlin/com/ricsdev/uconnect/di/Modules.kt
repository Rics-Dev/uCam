package com.ricsdev.uconnect.di

import com.ricsdev.uconnect.presentation.setupScreen.SetupViewModel
import com.ricsdev.uconnect.util.AppLogger
import com.ricsdev.uconnect.util.ConnectionManager
import com.ricsdev.uconnect.util.ConnectionStateHolder
import org.koin.core.module.Module
import org.koin.core.module.dsl.singleOf
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module

expect val platformModule: Module


val sharedModule = module {
    singleOf(::ConnectionStateHolder)
    singleOf(::ConnectionManager)
    single { AppLogger }
    viewModelOf(::SetupViewModel)
}