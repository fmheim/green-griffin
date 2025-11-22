package com.felix.greengriffin.core.presentation.icons

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

val DoubleArrowDown: ImageVector
    get() {
        if (_DoubleArrowDown != null) {
            return _DoubleArrowDown!!
        }
        _DoubleArrowDown = ImageVector.Builder(
            name = "DoubleArrowDown",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 960f,
            viewportHeight = 960f
        ).apply {
            path(fill = SolidColor(Color(0xFFE3E3E3))) {
                moveTo(480f, 760f)
                lineTo(240f, 520f)
                lineToRelative(56f, -56f)
                lineToRelative(184f, 183f)
                lineToRelative(184f, -183f)
                lineToRelative(56f, 56f)
                lineToRelative(-240f, 240f)
                close()
                moveTo(480f, 520f)
                lineTo(240f, 280f)
                lineToRelative(56f, -56f)
                lineToRelative(184f, 183f)
                lineToRelative(184f, -183f)
                lineToRelative(56f, 56f)
                lineToRelative(-240f, 240f)
                close()
            }
        }.build()

        return _DoubleArrowDown!!
    }

@Suppress("ObjectPropertyName")
private var _DoubleArrowDown: ImageVector? = null
