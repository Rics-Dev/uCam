package com.ricsdev.ucam

import androidx.compose.material.MaterialTheme
import androidx.compose.runtime.*
import com.ricsdev.ucam.presentation.pairingScreen.PairingScreen
import org.koin.compose.KoinContext

@Composable
fun App(
) {
    MaterialTheme {
        KoinContext {
            PairingScreen(
                onPaired = {
                    println("Paired")
                },
            )
            }
        }
}