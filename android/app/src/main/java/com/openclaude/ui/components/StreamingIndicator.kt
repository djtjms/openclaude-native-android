package com.openclaude.ui.components

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp

@Composable
fun StreamingIndicator(modifier: Modifier = Modifier) {
    val infiniteTransition = rememberInfiniteTransition()
    val color = MaterialTheme.colorScheme.primary

    val alphas = List(3) { i ->
        infiniteTransition.animateFloat(
            initialValue = 0.3f,
            targetValue = 1.0f,
            animationSpec = infiniteRepeatable(
                tween(600, delayMillis = i * 200),
                RepeatMode.Reverse
            ),
            label = "dot$i"
        )
    }

    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        alphas.forEach { animatedAlpha ->
            val alpha by animatedAlpha
            Box(
                Modifier
                    .size(6.dp)
                    .alpha(alpha)
                    .clip(CircleShape)
                    .background(color)
            )
        }
    }
}