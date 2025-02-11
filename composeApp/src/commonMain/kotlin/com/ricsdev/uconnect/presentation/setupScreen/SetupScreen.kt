package com.ricsdev.uconnect.presentation.setupScreen

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import com.ricsdev.uconnect.navigation.Screens
import com.ricsdev.uconnect.presentation.setupScreen.components.AllSet
import com.ricsdev.uconnect.presentation.setupScreen.components.GetStarted
import com.ricsdev.uconnect.presentation.setupScreen.components.PairDevices
import com.ricsdev.uconnect.presentation.setupScreen.components.ReviewPermissions
import com.ricsdev.uconnect.presentation.setupScreen.components.StepProgressIndicator

@Composable
fun SetupScreen(
    navController: NavHostController,
) {
    var currentStep by remember { mutableStateOf(0) }

    val steps = listOf("Get Started", "Pair Devices", "Permissions", "All Set")

    Scaffold { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            AnimatedContent(
                targetState = currentStep,
                transitionSpec = {
                    fadeIn(animationSpec = tween(300)) togetherWith
                            fadeOut(animationSpec = tween(300))
                },
                modifier = Modifier.weight(1f)
            ) { step ->
                when (step) {
                    0 -> GetStarted(
                        onNext = { currentStep++ }
                    )
                    1 -> PairDevices(
                        onNext = { currentStep++ },
                        onBack = { currentStep-- }
                    )
                    2 -> ReviewPermissions(
                        onNext = { currentStep++ },
                        onBack = { currentStep-- }
                    )
                    3 -> AllSet(
                        onFinish = {
                            navController.navigate(Screens.HomeScreen) {
                                popUpTo(Screens.SetupScreen) { inclusive = true }
                            }
                        }
                    )
                }
            }

            StepProgressIndicator(
                totalSteps = steps.size,
                currentStep = currentStep,
                steps = steps
            )
        }
    }
}




