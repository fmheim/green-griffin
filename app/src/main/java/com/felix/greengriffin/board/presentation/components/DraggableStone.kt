package com.felix.greengriffin.board.presentation.components

import android.content.ClipData
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.draganddrop.dragAndDropSource
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
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

@Serializable
sealed class StoneData {
    abstract val letter: Char
    abstract val value: Int
    abstract val id: String

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

    fun toStoneInHand( userId: Int) = StoneInHand(
        letter = letter,
        value = value,
        id = id,
        userId = userId
    )

    fun toStoneOnBoard(rowIndex: Int, columnIndex: Int) = StoneOnBoard(
        letter = letter,
        value = value,
        id = id,
        rowIndex = rowIndex,
        columnIndex = columnIndex
    )
}

@Serializable
data class StoneInHand(
    override val letter: Char,
    override val value: Int,
    override val id: String,
    val userId: Int
) : StoneData() {

    fun toStoneInBag() = StoneInBag(
        letter = letter,
        value = value,
        id = id
    )

    fun toStoneOnBoard(rowIndex: Int, columnIndex: Int) = StoneOnBoard(
        letter = letter,
        value = value,
        id = id,
        rowIndex = rowIndex,
        columnIndex = columnIndex
    )
}

@Serializable
data class StoneOnBoard(
    override val letter: Char,
    override val value: Int,
    override val id: String,
    val rowIndex: Int,
    val columnIndex: Int,
    val horizontalContainingWord: List<StoneOnBoard> = emptyList(),
    val verticalContainingWord: List<StoneOnBoard> = emptyList(),
) : StoneData() {

    val isLocked get() = horizontalContainingWord.isNotEmpty() || verticalContainingWord.isNotEmpty()

    fun toStoneInBag() = StoneInBag(
        letter = letter,
        value = value,
        id = id
    )

    fun toStoneInHand(userId: Int) = StoneInHand(
        letter = letter,
        value = value,
        id = id,
        userId = userId,
    )

    fun isToLeftOf(other: StoneOnBoard) = columnIndex == other.columnIndex - 1 && rowIndex == other.rowIndex
    fun isAbove(other: StoneOnBoard) = columnIndex == other.columnIndex && rowIndex == other.rowIndex - 1
    fun isToRightOf(other: StoneOnBoard) = columnIndex == other.columnIndex + 1 && rowIndex == other.rowIndex
    fun isBelow(other: StoneOnBoard) = columnIndex == other.columnIndex && rowIndex == other.rowIndex + 1
}

@Composable
fun DraggableStone(
    data: StoneData,
    modifier: Modifier = Modifier,
    width: Dp? = null,
) {
    var fontSize by remember {
        mutableStateOf(45.sp)
    }
    Box(
        modifier = modifier
            .then(other = if (width != null) Modifier.size(width) else Modifier)
            .dragAndDropSource {
                if(data is StoneOnBoard && data.isLocked) return@dragAndDropSource
                detectTapGestures(
                    onPress = {
                    startTransfer(
                        transferData = DragAndDropTransferData(
                            clipData = data.asClipData()
                        )
                    )
                })
            }
            .background(color = MaterialTheme.colorScheme.secondary, shape = RoundedCornerShape(4.dp))
            .border(width = 2.dp, color = MaterialTheme.colorScheme.outline, shape = RoundedCornerShape(4.dp))
    ) {
        Text(
            modifier = Modifier.padding(4.dp),
            text = " " + data.letter.toString() + " ",
            color = MaterialTheme.colorScheme.onSecondary,
            maxLines = 1,
            lineHeight = fontSize,
            onTextLayout = {
                if (it.hasVisualOverflow) fontSize *= 0.8f
            },
            fontSize = fontSize,
        )
        Box(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .clip(shape = RoundedCornerShape(4.dp))
                .background(color = MaterialTheme.colorScheme.outline)
        ) {
            Text(
                modifier = Modifier.padding(1.dp),
                color = MaterialTheme.colorScheme.onSecondary,
                maxLines = 1,
                text = data.value.toString(), fontSize = 10.sp, lineHeight = 10.sp
            )
        }
    }
}