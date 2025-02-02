package com.ricsdev.ucam.di

import com.ricsdev.ucam.presentation.setupScreen.SetupViewModel
import com.ricsdev.ucam.util.ConnectionStateHolder
import org.koin.core.module.Module
import org.koin.core.module.dsl.singleOf
import org.koin.dsl.module

expect val platformModule: Module


val sharedModule = module {
    singleOf(::ConnectionStateHolder)
    singleOf(::SetupViewModel)
}