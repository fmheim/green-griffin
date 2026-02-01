package com.felix.greengriffin.core.presentation.drawing

import android.R
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Color.Companion.Transparent
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import kotlin.math.PI
import kotlin.math.sin

fun DrawScope.drawWater(wavePhase : Float) {
    clipRect {
        val white = Color(0xFFFFFFFF)
        val blue15 = Color(0xFF4AB8EA)
        val blue10 = Color(0xFF45B2E5)
        val blue5 = Color(0xFF61B4EE)

        val waveCount = 1
        val waveSegments = 2 // Number of curves per wave line
        val waveAmplitude = size.height / 2

        val gradientColors = listOf(blue10, blue15, blue15, blue10)

        val gradientBrush = Brush.verticalGradient(
            colors = gradientColors
        )
        drawRect(brush = gradientBrush)



        for (i in 0 until waveCount) {
            val path = Path()
            val segmentHeight = size.height / waveCount
            val startY = segmentHeight * (0.5f + i)
            path.moveTo(-size.width * 0.1f, startY)

            // Create a wavy line across the width
            var currentX = 0f
            for (j in 0 until waveSegments) {
                val segmentWidth = (size.width * 1.2f) / waveSegments
                val endX = currentX + segmentWidth

                // Use the animated wavePhase to create an oscillating motion for the control points
                val sineInput = wavePhase + (j.toFloat() / waveSegments) * PI.toFloat()
                val animatedAmplitude = sin(sineInput) * waveAmplitude

                val controlX1 = currentX + segmentWidth * 0.25f
                val controlY1 = startY + animatedAmplitude
                val controlX2 = currentX + segmentWidth * 0.75f
                val controlY2 = startY - animatedAmplitude // Invert for an 'S' curve
                val endY = startY

                path.cubicTo(controlX1, controlY1, controlX2, controlY2, endX, endY)
                currentX = endX
            }


            drawPath(
                path = path,
                color = white.copy(alpha = 0.05f),
                style = Stroke(
                    width = size.height / 6f
                )
            )
        }
    }
}

@Preview
@Composable
fun WaterPreview() {

    Column{
        repeat(10){
            Box(
                modifier = Modifier
                    .size(100.dp)
                    .drawBehind {
                        drawWater(wavePhase = 1f)
                    }
            )
        }

    }

}