package com.felix.greengriffin.core.presentation.components

import androidx.annotation.DrawableRes
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.ImageShader
import androidx.compose.ui.graphics.Matrix
import androidx.compose.ui.graphics.ShaderBrush
import androidx.compose.ui.graphics.TileMode
import androidx.compose.ui.res.imageResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.felix.greengriffin.R
import com.felix.greengriffin.core.presentation.theme.GreenGriffinTheme

@Composable
fun TiledBackground(
    @DrawableRes tileImageRes: Int,
    modifier: Modifier = Modifier,
    scale: Float = 1f,
) {
    val matrix = remember(scale) {
        Matrix().apply {
            reset()
            scale(scale, scale)
        }
    }

    val bitmap = ImageBitmap.imageResource(id = tileImageRes)
    val brush = remember {
        ShaderBrush(
            ImageShader(
                image = bitmap,
                tileModeX = TileMode.Mirror,
                tileModeY = TileMode.Mirror,
            )
        )
    }
    brush.transform = matrix


    Box(modifier = modifier.background(brush))
}

@Preview
@Composable
private fun TiledBackgroundPreview() {
    GreenGriffinTheme {
        TiledBackground(
            modifier = Modifier
                .width(800.dp)
                .height(2000.dp),
            tileImageRes = R.drawable.water,
            scale = 0.2f
        )
    }
}
