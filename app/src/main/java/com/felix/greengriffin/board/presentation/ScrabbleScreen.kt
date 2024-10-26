@file:OptIn(ExperimentalFoundationApi::class)

package com.felix.greengriffin.board.presentation

import android.annotation.SuppressLint
import android.content.ClipData
import android.content.ClipDescription
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.draganddrop.dragAndDropSource
import androidx.compose.foundation.draganddrop.dragAndDropTarget
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draganddrop.DragAndDropEvent
import androidx.compose.ui.draganddrop.DragAndDropTarget
import androidx.compose.ui.draganddrop.DragAndDropTransferData
import androidx.compose.ui.draganddrop.mimeTypes
import androidx.compose.ui.draganddrop.toAndroidDragEvent
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.felix.greengriffin.R
import com.felix.greengriffin.board.presentation.ScrabbleEvent.StoneDroppedOnBoard
import com.felix.greengriffin.board.presentation.ScrabbleEvent.StoneDroppedOnHand
import com.felix.greengriffin.board.presentation.components.StonesRow
import com.felix.greengriffin.core.presentation.theme.GreenGriffinTheme
import com.felix.greengriffin.core.presentation.theme.ScrabbleStoneBackground
import com.felix.greengriffin.core.presentation.theme.ScrabbleStoneBorder
import com.felix.greengriffin.core.presentation.theme.ScrabbleStoneText
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.util.UUID


@Composable
fun ScrabbleScreen(
    modifier: Modifier = Modifier,
    state: ScrabbleState,
    onEvent: (ScrabbleEvent) -> Unit
) {
    Column(modifier = modifier) {
        ScrabbleBoard(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 16.dp, horizontal = 8.dp)
                .border(width = 2.dp, color = MaterialTheme.colorScheme.onSurface),
            state = state,
            onEvent = onEvent
        )
        StonesRow(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 16.dp),
            state = state,
            onEvent = onEvent
        )

        Button(
            modifier = Modifier.padding(16.dp),
            colors = ButtonDefaults.buttonColors().copy(),
            shape = RoundedCornerShape(8.dp),
            border = BorderStroke(width = 2.dp, color = ScrabbleStoneBackground),
            onClick = { onEvent(ScrabbleEvent.DrawStonesClick) }) {


            Text(text = "Steine aufüllen", color = ScrabbleStoneText)
            Spacer(modifier = Modifier.width(8.dp))
            Icon(
                painter = painterResource(R.drawable.ic_draw_stones),
                contentDescription = null,
                tint = ScrabbleStoneText
            )
        }
        Spacer(modifier = Modifier.weight(1f))
        Button(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            colors = ButtonDefaults.buttonColors().copy(),
            shape = RoundedCornerShape(8.dp),
            border = BorderStroke(width = 2.dp, color = ScrabbleStoneBackground),
            onClick = { onEvent(ScrabbleEvent.DrawStonesClick) }) {


            Text(text = "Abschicken", color = ScrabbleStoneText)
            Spacer(modifier = Modifier.width(8.dp))
            Icon(
                painter = painterResource(R.drawable.ic_send),
                contentDescription = null,
                tint = ScrabbleStoneText
            )
        }
    }
}

@SuppressLint("UnsafeOptInUsageError")
@Serializable
data class StoneData(
    val letter: Char,
    val value: Int,
    val id: String,
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

@OptIn(ExperimentalFoundationApi::class)
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

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ScrabbleBoard(
    modifier: Modifier = Modifier,
    onEvent: (ScrabbleEvent) -> Unit,
    state: ScrabbleState
) {
    val numColumns = 15
    LazyVerticalGrid(
        columns = GridCells.Fixed(count = numColumns),
        userScrollEnabled = false,
        modifier = modifier
            .height((LocalConfiguration.current.screenWidthDp - (2f * 8f)).dp)
    ) {
        items(numColumns * numColumns) { index ->
            Box(
                modifier = Modifier
                    .aspectRatio(1f)
                    .background(color = if (state.enteredField == index) MaterialTheme.colorScheme.surfaceVariant else MaterialTheme.colorScheme.surface)
                    .border(width = 1.dp, color = MaterialTheme.colorScheme.onSurface)
                    .dragAndDropTarget(
                        shouldStartDragAndDrop = {
                            state.stonesOnBoard[index] == null && it
                                .mimeTypes()
                                .contains(ClipDescription.MIMETYPE_TEXT_PLAIN)
                        },
                        target = remember {
                            object : DragAndDropTarget {
                                override fun onDrop(event: DragAndDropEvent): Boolean {
                                    val data = StoneData
                                        .fromClipData(clipData = event.toAndroidDragEvent().clipData)
                                        ?: return false
                                    onEvent(
                                        StoneDroppedOnBoard(
                                            index = index,
                                            stoneData = data,
                                        )
                                    )
                                    return true
                                }

                                override fun onEntered(event: DragAndDropEvent) {
                                    super.onEntered(event)
                                    onEvent(ScrabbleEvent.FieldEntered(index = index))
                                }
                            }
                        }
                    )
            ) {
                state.stonesOnBoard[index]?.let {
                    DraggableStone(data = it)
                }
            }
        }
    }
}


@Preview(showBackground = true, backgroundColor = 0xFF164134)
@Composable
fun ScrabbleScreenPreview() {
    GreenGriffinTheme {
        ScrabbleScreen(state = ScrabbleState(
            stonesInHand = listOf(
                StoneData(letter = 'A', value = 1, id = UUID.randomUUID().toString()),
                StoneData(letter = 'B', value = 1, id = UUID.randomUUID().toString()),
                StoneData(letter = 'C', value = 1, id = UUID.randomUUID().toString()),
                StoneData(letter = 'D', value = 1, id = UUID.randomUUID().toString()),
                StoneData(letter = 'E', value = 1, id = UUID.randomUUID().toString()),
                StoneData(letter = 'F', value = 1, id = UUID.randomUUID().toString()),
                StoneData(letter = 'G', value = 1, id = UUID.randomUUID().toString())

            )
        ), onEvent = {})
    }
}
