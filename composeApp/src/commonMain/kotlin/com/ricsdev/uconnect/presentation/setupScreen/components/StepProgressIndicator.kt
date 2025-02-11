package com.ricsdev.uconnect.presentation.setupScreen.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.ricsdev.uconnect.getPlatform

@Composable
fun StepProgressIndicator(
    totalSteps: Int,
    currentStep: Int,
    steps: List<String> = listOf("Get Started", "Pair Devices", "Permissions", "All Set")
) {
    val primaryColor = MaterialTheme.colorScheme.primary
    val surfaceVariantColor = MaterialTheme.colorScheme.surfaceVariant
    val onPrimaryColor = MaterialTheme.colorScheme.onPrimary
    val platform = getPlatform().name
//    println(platform)

    Column(
        modifier = Modifier.fillMaxWidth()
            .padding(bottom = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Progress indicators row
        Row(
            modifier = Modifier
                .fillMaxWidth( if(platform.startsWith("Java")) 0.8f else 1f)
                .padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            for (step in 0 until totalSteps) {
                val stepProgress = animateFloatAsState(
                    targetValue = when {
                        step < currentStep -> 1f
                        step == currentStep -> 0f
                        else -> 0f
                    },
                    animationSpec = tween(300)
                )

                Box(
                    modifier = Modifier.weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    // Connecting line
//                    if (step < totalSteps) {
                        ConnectingLine(
                            step = step,
                            currentStep = currentStep,
                            totalSteps = totalSteps,
                            primaryColor = primaryColor,
                            surfaceVariantColor = surfaceVariantColor,
                            modifier = Modifier.align(Alignment.CenterStart)
                        )
//                    }

                    // Step circle
                    StepCircle(
                        step = step,
                        currentStep = currentStep,
                        stepProgress = stepProgress.value,
                        primaryColor = primaryColor,
                        surfaceVariantColor = surfaceVariantColor,
                        onPrimaryColor = onPrimaryColor
                    )
                }
            }
        }

        // Step labels
        Row(
            modifier = Modifier
                .fillMaxWidth( if(platform.startsWith("Java")) 0.8f else 1f)
                .padding(top = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            steps.forEachIndexed { index, stepName ->
                Text(
                    text = stepName,
                    style = MaterialTheme.typography.bodySmall,
                    color = if (index <= currentStep)
                        MaterialTheme.colorScheme.primary
                    else
                        MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.weight(1f)
                        .padding(start = if (index == 0) 24.dp else 0.dp, end = if (index == steps.size - 1) 24.dp else 0.dp)
                )
            }
        }
    }
}

@Composable
private fun StepCircle(
    step: Int,
    currentStep: Int,
    stepProgress: Float,
    primaryColor: Color,
    surfaceVariantColor: Color,
    onPrimaryColor: Color
) {
    val circleSize = 36.dp

    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .size(circleSize)
    ) {
        Canvas(
            modifier = Modifier.size(circleSize)
        ) {
            drawCircle(
                color = surfaceVariantColor,
                radius = size.minDimension / 2,
                style = Stroke(width = 4.dp.toPx())
            )

            drawArc(
                color = primaryColor,
                startAngle = -90f,
                sweepAngle = 360f * stepProgress,
                useCenter = false,
                style = Stroke(
                    width = 4.dp.toPx(),
                    cap = StrokeCap.Round
                )
            )
        }

        Box(
            modifier = Modifier
                .size(circleSize - 16.dp)
                .clip(CircleShape)
                .background(
                    if (step == currentStep || stepProgress > 0f) primaryColor
                    else surfaceVariantColor
                ),
            contentAlignment = Alignment.Center
        ) {
            if (step < currentStep) {
                Icon(
                    imageVector = Icons.Rounded.Check,
                    contentDescription = "Completed",
                    tint = onPrimaryColor,
                    modifier = Modifier
                        .size(16.dp)
                        .padding(2.dp)
                        .scale(stepProgress)
                )
            }
        }
    }
}

@Composable
private fun ConnectingLine(
    step: Int,
    currentStep: Int,
    totalSteps: Int,
    primaryColor: Color,
    surfaceVariantColor: Color,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(4.dp)
    ) {
        Canvas(
            modifier = Modifier.fillMaxSize()
        ) {
            val startX = if (step == 0) size.width / 2 else 0f
            val endX = if (step == totalSteps - 1) size.width / 2 else size.width

            drawLine(
                color = surfaceVariantColor,
                start = center.copy(x = startX),
                end = center.copy(x = endX),
                strokeWidth = 4.dp.toPx(),
                cap = StrokeCap.Round
            )

            if (step < currentStep) {
                drawLine(
                    color = primaryColor,
                    start = center.copy(x = startX),
                    end = center.copy(x = endX),
                    strokeWidth = 4.dp.toPx(),
                    cap = StrokeCap.Round
                )
            }
        }
    }
}