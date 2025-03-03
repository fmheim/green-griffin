package com.felix.greengriffin.util.extensions.compose

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.TileMode
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp

@Stable
@Composable
fun Modifier.animatedGradientBrush(
): Modifier {

    val infiniteTransition = rememberInfiniteTransition(label = "infiniteTransition")

    val targetOffset = with(LocalDensity.current) {
        100.dp.toPx()
    }

    val offset by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = targetOffset,
        animationSpec = infiniteRepeatable(
            animation = tween(
                durationMillis = 350000,
                easing = LinearEasing,
            ), repeatMode = RepeatMode.Reverse
        ), label = "offset"
    )
    val colors = listOf(
        MaterialTheme.colorScheme.onSurface,
        MaterialTheme.colorScheme.surface.copy(alpha = 0.7f),
    )
    return this.then(
        Modifier
            .graphicsLayer(compositingStrategy = CompositingStrategy.Offscreen)
            .drawWithCache {
                val gradientBrush = Brush.linearGradient(
                    colors = colors,
                    start = Offset(offset * size.width, offset * size.width),
                    end = Offset(
                        offset * size.width + size.width,
                        offset * size.width + size.width
                    ),
                    tileMode = TileMode.Mirror
                )
                onDrawWithContent {
                    drawContent()
                    drawRect(gradientBrush, blendMode = BlendMode.SrcAtop)
                }
            }
    )
}
