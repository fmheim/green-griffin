package com.felix.greengriffin.core.presentation.drawing

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import kotlin.random.Random

fun DrawScope.drawWater() {
    val darkBlue = Color(0xFF003366)
    val mediumBlue = Color(0xFF005792)
    val lightBlue = Color(0xFF53A8DB)
    val waveColors = listOf(darkBlue, mediumBlue, lightBlue, mediumBlue)

    val waveCount = 8
    val waveSegments = 6 // Number of curves per wave line

    // A seed for the random generator to ensure the waves are the same on each recomposition
    val random = Random(0)

    for (i in 0 until waveCount) {
        val path = Path()
        val startY = size.height * (i + 1) / (waveCount + 1)
        path.moveTo(-size.width * 0.1f, startY)

        // Create a wavy line across the width
        for (j in 0..waveSegments) {
            val x1 = size.width * j / waveSegments + random.nextFloat() * 20 - 10
            val y1 = startY + random.nextFloat() * 20 - 10
            val x2 = size.width * (j + 0.5f) / waveSegments
            val y2 = startY + random.nextFloat() * 10 - 5

            path.quadraticBezierTo(x1, y1, x2, y2)
        }

        drawPath(
            path = path,
            color = waveColors[i % waveColors.size],
            style = Stroke(
                width = (2f + random.nextFloat() * 2) * 1.dp.toPx()
            )
        )
    }
}

@Preview
@Composable
fun WaterPreview() {
    Box(
        modifier = Modifier
            .background(Color(0xFF3488AB)) // Sky blue background
            .size(100.dp)
            .drawBehind {
                drawWater()
            }
    )
}