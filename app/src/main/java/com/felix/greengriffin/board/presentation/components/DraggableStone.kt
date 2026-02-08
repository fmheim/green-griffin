package com.felix.greengriffin.board.presentation.components

import android.R.attr.fontWeight
import android.R.attr.maxLines
import android.content.ClipData
import androidx.annotation.DrawableRes
import androidx.compose.animation.core.copy
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.draganddrop.dragAndDropSource
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.TextAutoSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draganddrop.DragAndDropTransferData
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.BlurEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Color.Companion.Transparent
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.ImageShader
import androidx.compose.ui.graphics.Matrix
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.RenderEffect
import androidx.compose.ui.graphics.Shader
import androidx.compose.ui.graphics.ShaderBrush
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.TileMode
import androidx.compose.ui.graphics.asComposeRenderEffect
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.res.imageResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.felix.greengriffin.R
import com.felix.greengriffin.board.presentation.Field
import com.felix.greengriffin.core.presentation.theme.GreenGriffinTheme
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

data class Word(
    val letters: List<StoneData>,
) {
    val points get() = letters.sumOf { it.value }
    val asString get() = letters.map { it.letter }.joinToString("")
}

fun List<StoneData>.asWord() = Word(letters = this)


@Serializable
sealed class StoneData {
    abstract val letter: Char
    abstract val value: Int
    abstract val id: String
    val isJoker get() = letter == ' ' && value == 0

    fun asClipData(): ClipData =
        ClipData.newPlainText("${letter}_$value", Json.encodeToString(this))

    companion object {
        fun fromClipData(clipData: ClipData): StoneData? = try {
            Json.decodeFromString<StoneData>(clipData.getItemAt(0).text.toString())
        } catch (e: Exception) {
            println("Failed to parse stone data: $e")
            null
        }
    }
}

@Serializable
data class StoneInBag(
    override val letter: Char,
    override val value: Int,
    override val id: String,
) : StoneData() {

    fun toStoneInHand(userId: Int) = StoneInHand(
        letter = letter,
        value = value,
        id = id,
        userId = userId,
    )
}

@Serializable
data class StoneInHand(
    override val letter: Char,
    override val value: Int,
    override val id: String,
    val userId: Int,
) : StoneData() {

    fun toStoneOnBoard(rowIndex: Int, columnIndex: Int) = StoneOnBoard(
        letter = letter,
        value = value,
        id = id,
        rowIndex = rowIndex,
        columnIndex = columnIndex,
        isLocked = false
    )
}

@Serializable
data class StoneOnBoard(
    override val letter: Char,
    override val value: Int,
    override val id: String,
    val rowIndex: Int,
    val columnIndex: Int,
    val isLocked: Boolean,
) : StoneData() {

    fun toStoneInHand(userId: Int) = StoneInHand(
        letter = letter,
        value = value,
        id = id,
        userId = userId,
    )

    fun toField() = Field(
        row = rowIndex,
        column = columnIndex,
    )

    fun isToLeftOf(other: StoneOnBoard) =
        columnIndex == other.columnIndex - 1 && rowIndex == other.rowIndex

    fun isAbove(other: StoneOnBoard) =
        columnIndex == other.columnIndex && rowIndex == other.rowIndex - 1

    fun isToRightOf(other: StoneOnBoard) =
        columnIndex == other.columnIndex + 1 && rowIndex == other.rowIndex

    fun isBelow(other: StoneOnBoard) =
        columnIndex == other.columnIndex && rowIndex == other.rowIndex + 1
}

@Composable
fun DraggableStone(
    data: StoneData,
    modifier: Modifier = Modifier,
    width: Dp? = null,
    @DrawableRes backgroundRes: Int? = null,
    @DrawableRes textImageRes: Int? = null,
) {

    Box {
        val resources = LocalResources.current
        Box(
            modifier = modifier
                .then(other = if (width != null) Modifier.size(width) else Modifier)
                .dragAndDropSource(block = {
                    if (data is StoneOnBoard && data.isLocked || data is StoneInBag) return@dragAndDropSource
                    detectTapGestures(
                        onPress = {
                            startTransfer(
                                transferData = DragAndDropTransferData(
                                    clipData = data.asClipData()
                                )
                            )
                        })
                })
                .then(
                    if (backgroundRes == null) {
                        Modifier
                            .background(
                                color = MaterialTheme.colorScheme.primary,
                                shape = RoundedCornerShape(4.dp)
                            )
                            .border(
                                width = 2.dp,
                                color = MaterialTheme.colorScheme.outline,
                                shape = RoundedCornerShape(4.dp)
                            )
                    } else {
                        Modifier.drawBehind {
                            drawImage(
                                image = ImageBitmap.imageResource(
                                    res = resources,
                                    id = backgroundRes
                                ),
                                dstSize = IntSize(
                                    this.size.width.toInt(),
                                    this.size.height.toInt()
                                )
                            )
                        }
                    }
                )

        ) {

            val bitmap = textImageRes?.let {
                ImageBitmap.imageResource(textImageRes)
            }

            val brush = bitmap?.let {
                remember(textImageRes) {
                    ShaderBrush(
                        ImageShader(
                            image = bitmap,
                            tileModeX = TileMode.Mirror,
                            tileModeY = TileMode.Mirror,
                        )
                    )
                }
            }

            Box {
                val text = " " + data.letter.toString() + " "
                val baseTextStyle = TextStyle(
                    fontWeight = FontWeight.W900,
                    textAlign = TextAlign.Center
                )

                Text(
                    text = text,
                    modifier = Modifier
                        .padding(4.dp),
                    color = Color.Black.copy(alpha = 0.7f),
                    style = baseTextStyle.copy(
                        drawStyle = Stroke(
                            width = 4f,
                            join = StrokeJoin.Round,
                            pathEffect = PathEffect.cornerPathEffect(radius = 15f)
                        ),
                        shadow = Shadow(color = Color.Black, blurRadius = 10f),
                    ),
                    autoSize = TextAutoSize.StepBased()
                )
                Text(
                    modifier = Modifier
                        .padding(4.dp),
                    text = text,
                    style = baseTextStyle.copy(
                        brush = brush,
                    ),
                    color = MaterialTheme.colorScheme.onPrimary,
                    maxLines = 1,
                    autoSize = TextAutoSize.StepBased()
                )
                Text(
                    text = text,
                    modifier = Modifier
                        .padding(4.dp),
                    color = Color.Black.copy(alpha = 0.7f),
                    style = baseTextStyle.copy(
                        drawStyle = Stroke(
                            width = 4f,
                            join = StrokeJoin.Round,
                            pathEffect = PathEffect.cornerPathEffect(radius = 15f)
                        ),
                    ),
                    autoSize = TextAutoSize.StepBased()
                )


            }

        }
        data.value.takeIf { it > 0 }?.let {
            ValueBadge(it.toString())
        }
    }
}

@Composable
private fun BoxScope.ValueBadge(value: String) {
    Box(
        modifier = Modifier
            .offset(x = 2.dp, y = 2.dp)
            .align(Alignment.BottomEnd)
            .background(color = MaterialTheme.colorScheme.secondary, shape = CircleShape)

    ) {
        Text(
            modifier = Modifier
                .padding(1.dp)
                .widthIn(10.dp),
            color = MaterialTheme.colorScheme.onSecondary,
            maxLines = 1,
            text = value,
            textAlign = TextAlign.Center,
            fontSize = 8.sp,
            lineHeight = 10.sp
        )
    }
}

@Preview
@Composable
fun DraggableStonePreview() {
    GreenGriffinTheme {
        DraggableStone(
            data = StoneInHand(
                letter = 'A',
                value = 1,
                id = "1",
                userId = 0
            ),
            backgroundRes = R.drawable.stone,
            textImageRes = R.drawable.pure_stone
        )
    }
}