package com.ricsdev.ucam.presentation.setupScreen.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Phonelink
import androidx.compose.material.icons.outlined.Shield
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp

@Composable
fun GetStarted(
    onNext: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // Illustration

        var iconAlpha by remember { mutableStateOf(0f) }
        val animatedAlpha by animateFloatAsState(
            targetValue = iconAlpha,
            animationSpec = tween(durationMillis = 1000)
        )

        LaunchedEffect(Unit) {
            iconAlpha = 1f
        }


        Icon(
            Icons.Outlined.Phonelink,
            contentDescription = "Welcome icon",
            modifier = Modifier.size(82.dp)
                .alpha(animatedAlpha)
        )

        // Title
        Text(
            text = "Welcome to uConnect",
            style = MaterialTheme.typography.headlineMedium,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(16.dp))
        // Description
        Text(
            text = "Let's link your phone and PC for seamless connectivity. This will only take a few minutes.",
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Get Started Button
        Button(
            onClick = onNext,
            modifier = Modifier.fillMaxWidth(0.5f)
        ) {
            Text("Get Started")
        }
    }
}
