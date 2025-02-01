package com.ricsdev.ucam.di

import org.koin.core.module.Module
import org.koin.core.module.dsl.singleOf
import org.koin.dsl.module

expect val platformModule: Module


val sharedModule = module {
//    singleOf(::ClipboardManager)
}