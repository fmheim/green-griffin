package com.felix.greengriffin.core.presentation.drawing

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp

fun DrawScope.drawGrass() {

    val darkGreen = Color(0xFF1E4D20)
    val mediumGreen = Color(0xFF376239)
    val lightGreen = Color(0xFF427945)
    val bladeColors = listOf(
        darkGreen,
        lightGreen,
        mediumGreen,
        mediumGreen,
        darkGreen,
        lightGreen,
        mediumGreen
    )


    val bladeCount = 6
    val startXList = listOf(0.2f, 0.7f, 0.4f, 0.5f, 0.9f, 0.55f, 0.3f)
    val endX = listOf(-5, 4, 3, 2, -4, 5, 1)
    val strokeWidthList = listOf(0.2f, 0.7f, 0.4f, 0.5f, 0.9f, 0.2f, 0.9f)

    for (i in 0..bladeCount) {
        val startX = startXList[i] * size.width
        val endX = startX + endX[i]
        val strokeWidth = (1.5f + strokeWidthList[i]) * 1.dp.toPx()

        // Draw a short, upward line
        drawLine(
            color = bladeColors[i],
            start = Offset(x = startX, y = size.height),
            end = Offset(x = endX, y = size.height * startXList.reversed()[i]),
            strokeWidth = strokeWidth
        )
    }
}

@Preview
@Composable
fun GrassPreview() {
    Box(modifier = Modifier
        .background(Color(0xFF18230E))
        .size(50.dp)
        .drawBehind {
            drawGrass()
        })
}

