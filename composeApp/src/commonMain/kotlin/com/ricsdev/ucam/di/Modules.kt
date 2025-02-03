package com.ricsdev.ucam.di

import com.ricsdev.ucam.presentation.setupScreen.SetupViewModel
import com.ricsdev.ucam.util.AppLogger
import com.ricsdev.ucam.util.ConnectionManager
import com.ricsdev.ucam.util.ConnectionStateHolder
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