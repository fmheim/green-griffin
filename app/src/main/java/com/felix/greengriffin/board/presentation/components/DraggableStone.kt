package com.felix.greengriffin.board.presentation.components

import android.annotation.SuppressLint
import android.content.ClipData
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.draganddrop.dragAndDropSource
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draganddrop.DragAndDropTransferData
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.felix.greengriffin.core.presentation.theme.ScrabbleStoneBackground
import com.felix.greengriffin.core.presentation.theme.ScrabbleStoneBorder
import com.felix.greengriffin.core.presentation.theme.ScrabbleStoneText
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

@SuppressLint("UnsafeOptInUsageError")
@Serializable
data class StoneData(
    val letter: Char,
    val value: Int,
    val id: String,
    val isLocked: Boolean = false
) {
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

@Composable
fun DraggableStone(
    width: Dp? = null,
    data: StoneData,
    modifier: Modifier = Modifier
) {
    var fontSize by remember {
        mutableStateOf(45.sp)
    }
    Box(
        modifier = modifier
            .then(other = if (width != null) Modifier.size(width) else Modifier)
            .dragAndDropSource { // TODO disallow drag for logged in stones
                detectTapGestures(onPress = { offset ->
                    startTransfer(
                        transferData = DragAndDropTransferData(
                            clipData = data.asClipData()
                        )
                    )
                })
            }
            .background(color = ScrabbleStoneBackground, shape = RoundedCornerShape(4.dp))
            .border(width = 2.dp, color = ScrabbleStoneBorder, shape = RoundedCornerShape(4.dp))
    ) {
        Text(
            modifier = Modifier.padding(4.dp),
            text = " " + data.letter.toString() + " ",
            color = ScrabbleStoneText,
            maxLines = 1,
            lineHeight = fontSize,
            onTextLayout = {
                if (it.didOverflowWidth || it.didOverflowHeight || it.hasVisualOverflow) fontSize *= 0.8f
            },
            fontSize = fontSize,
        )
        Box(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .clip(shape = RoundedCornerShape(4.dp))
                .background(color = ScrabbleStoneBorder)
        ) {
            Text(
                modifier = Modifier.padding(1.dp),
                color = ScrabbleStoneText,
                maxLines = 1,
                text = data.value.toString(), fontSize = 10.sp, lineHeight = 10.sp
            )
        }

    }
}