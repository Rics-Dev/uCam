package com.ricsdev.ucam.presentation.pairingScreen

import androidx.compose.runtime.Composable

@Composable
expect fun PairingScreen(onPaired: (String) -> Unit)