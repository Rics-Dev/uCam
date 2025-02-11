package com.ricsdev.uconnect.presentation.setupScreen.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.filled.LaptopMac
import androidx.compose.material.icons.filled.PhonelinkRing
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.ricsdev.uconnect.getPlatform
import com.ricsdev.uconnect.presentation.setupScreen.SetupViewModel
import org.koin.compose.viewmodel.koinViewModel

sealed class PairingState {
    data object Initial : PairingState()
    data object QrCode : PairingState()
    data object PinCode : PairingState()
}

@Composable
fun PairDevices(
    onBack: () -> Unit,
    onNext: () -> Unit
) {
    val viewModel = koinViewModel<SetupViewModel>()
    val platform = getPlatform().name
    var pairingState by remember { mutableStateOf<PairingState>(PairingState.Initial) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // Header Section
        var iconAlpha by remember { mutableStateOf(0f) }
        val animatedAlpha by animateFloatAsState(
            targetValue = iconAlpha,
            animationSpec = tween(durationMillis = 1000)
        )

        LaunchedEffect(Unit) {
            iconAlpha = 1f
        }

        Icon(
            imageVector = if (platform.startsWith("Java")) Icons.Default.PhonelinkRing else Icons.Default.LaptopMac,
            contentDescription = "Pair devices icon",
            modifier = Modifier
                .size(58.dp)
                .alpha(animatedAlpha)
        )

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "Pair Your Devices",
            style = MaterialTheme.typography.headlineSmall,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(8.dp))

        // Use AnimatedContent for better control over transitions
        AnimatedContent(
            targetState = pairingState,
            transitionSpec = {
                // Define a fade-in and fade-out transition
                fadeIn(animationSpec = tween(300)) togetherWith fadeOut(animationSpec = tween(300))
            },
            modifier = Modifier.fillMaxWidth()
        ) { state ->
            when (state) {
                PairingState.Initial -> {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = "Choose your preferred pairing method",
                            style = MaterialTheme.typography.bodyMedium,
                            textAlign = TextAlign.Center,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        Spacer(modifier = Modifier.height(32.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            // QR Code Card
                            Card(
                                onClick = { pairingState = PairingState.QrCode },
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(16.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.secondaryContainer,
                                )
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(16.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Icon(
                                        Icons.Outlined.QrCode,
                                        contentDescription = "QR Code",
                                        modifier = Modifier.size(48.dp),
                                        tint = MaterialTheme.colorScheme.onSecondaryContainer
                                    )

                                    Spacer(modifier = Modifier.height(16.dp))

                                    Text(
                                        text = "Scan QR Code",
                                        style = MaterialTheme.typography.titleMedium,
                                        color = MaterialTheme.colorScheme.onSecondaryContainer
                                    )

                                    Spacer(modifier = Modifier.height(8.dp))

                                    Text(
                                        text = "Quick and easy pairing using your camera",
                                        style = MaterialTheme.typography.bodyMedium,
                                        textAlign = TextAlign.Center,
                                        color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.8f)
                                    )

                                    Spacer(modifier = Modifier.height(16.dp))
                                }
                            }

                            // PIN Code Card
                            Card(
                                onClick = { pairingState = PairingState.PinCode },
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(16.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.secondaryContainer,
                                )
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(16.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Icon(
                                        Icons.Outlined.Pin,
                                        contentDescription = "PIN Code",
                                        modifier = Modifier.size(48.dp),
                                        tint = MaterialTheme.colorScheme.onSecondaryContainer
                                    )

                                    Spacer(modifier = Modifier.height(16.dp))

                                    Text(
                                        text = "Enter PIN Code",
                                        style = MaterialTheme.typography.titleMedium,
                                        color = MaterialTheme.colorScheme.onSecondaryContainer
                                    )

                                    Spacer(modifier = Modifier.height(8.dp))

                                    Text(
                                        text = "Manually enter the code shown on your device",
                                        style = MaterialTheme.typography.bodyMedium,
                                        textAlign = TextAlign.Center,
                                        color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.8f)
                                    )

                                    Spacer(modifier = Modifier.height(16.dp))
                                }
                            }
                        }
                    }
                }
                PairingState.QrCode -> {
                    PairQrCode(
                        viewModel = viewModel,
                        onNext = onNext
                    )
                }
                PairingState.PinCode -> {
                    PairPinCode(
                        viewModel = viewModel,
                        onNext = onNext
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        TextButton(
            onClick = {
                when (pairingState) {
                    PairingState.Initial -> onBack()
                    else -> pairingState = PairingState.Initial
                }
            },
            modifier = Modifier.fillMaxWidth(0.5f),
            colors = ButtonDefaults.textButtonColors(
                contentColor = MaterialTheme.colorScheme.primary
            )
        ) {
            Icon(
                Icons.AutoMirrored.Outlined.ArrowBack,
                contentDescription = "Back",
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text("Back")
        }
    }
}

@Composable
expect fun PairQrCode(
    viewModel: SetupViewModel,
    onNext: () -> Unit
)

@Composable
expect fun PairPinCode(
    viewModel: SetupViewModel,
    onNext: () -> Unit
)