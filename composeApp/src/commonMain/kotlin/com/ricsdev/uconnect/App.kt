package com.ricsdev.uconnect

import androidx.compose.runtime.*
import com.ricsdev.uconnect.navigation.AppNavigation
import com.ricsdev.uconnect.theme.UConnectTheme
import org.koin.compose.KoinContext
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun App(
) {
    UConnectTheme {
        KoinContext {
            val viewModel = koinViewModel<MainViewModel>()
            AppNavigation()
            }
        }
}