package com.ricsdev.ucam

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import com.ricsdev.ucam.presentation.pairingScreen.PairingScreen
import org.koin.compose.KoinContext
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun App(
) {
    MaterialTheme {
        KoinContext {
            val viewModel = koinViewModel<MainViewModel>()

            PairingScreen()
            }
        }
}

@Composable
fun Main(){

}