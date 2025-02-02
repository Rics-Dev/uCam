package com.ricsdev.ucam

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import com.ricsdev.ucam.navigation.AppNavigation
import com.ricsdev.ucam.theme.UConnectTheme
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