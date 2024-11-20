@file:OptIn(ExperimentalFoundationApi::class)

package com.felix.greengriffin.board.presentation

import android.content.ClipDescription
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.draganddrop.dragAndDropTarget
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draganddrop.DragAndDropEvent
import androidx.compose.ui.draganddrop.DragAndDropTarget
import androidx.compose.ui.draganddrop.mimeTypes
import androidx.compose.ui.draganddrop.toAndroidDragEvent
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.felix.greengriffin.R
import com.felix.greengriffin.board.presentation.ScrabbleEvent.StoneDroppedOnBoard
import com.felix.greengriffin.board.presentation.components.DraggableStone
import com.felix.greengriffin.board.presentation.components.StoneData
import com.felix.greengriffin.board.presentation.components.StoneInBag
import com.felix.greengriffin.board.presentation.components.StoneInHand
import com.felix.greengriffin.board.presentation.components.StoneOnBoard
import com.felix.greengriffin.board.presentation.components.StonesRow
import com.felix.greengriffin.core.presentation.theme.GreenGriffinTheme
import com.felix.greengriffin.core.presentation.theme.ScrabbleStoneBackground
import com.felix.greengriffin.core.presentation.theme.ScrabbleStoneText


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


            Text(text = "Steine auffüllen", color = ScrabbleStoneText)
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
            onClick = { onEvent(ScrabbleEvent.SubmitClick) }) {


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
                            state.isPositionOnBoardAvailable(
                                columnIndex = getColumnIndex(
                                    gridIndex = index,
                                    totalColumns = numColumns
                                ),
                                rowIndex = getRowIndex(
                                    gridIndex = index,
                                    totalColumns = numColumns
                                )
                            ) && it // check if position is taken
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
                                            columnIndex = getColumnIndex(
                                                gridIndex = index,
                                                totalColumns = numColumns
                                            ),
                                            rowIndex = getRowIndex(
                                                gridIndex = index,
                                                totalColumns = numColumns
                                            ),
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
                    ),
                content = {
                    state.stonesOnBoard.find { stone ->
                        stone.columnIndex == getColumnIndex(
                            gridIndex = index,
                            totalColumns = numColumns
                        ) && stone.rowIndex == getRowIndex(
                            gridIndex = index,
                            totalColumns = numColumns
                        )
                    }?.let {
                        DraggableStone(data = it)
                    }
                }
            )
        }
    }
}


@Preview(showBackground = true, backgroundColor = 0xFF164134)
@Composable
fun ScrabbleScreenPreview() {
    GreenGriffinTheme {
        ScrabbleScreen(state = previewScrabbleState, onEvent = {})
    }
}

fun getColumnIndex(gridIndex: Int, totalColumns: Int) = gridIndex % totalColumns
fun getRowIndex(gridIndex: Int, totalColumns: Int) = gridIndex / totalColumns

val previewScrabbleState = ScrabbleState(
    stonesInHand = listOf(
        StoneInHand(letter = 'A', value = 1, id = "stone1", userId = 1),
        StoneInHand(letter = 'B', value = 3, id = "stone2", userId = 1),
        StoneInHand(letter = 'C', value = 3, id = "stone3", userId = 1),
        StoneInHand(letter = 'D', value = 2, id = "stone4", userId = 1),
        StoneInHand(letter = 'E', value = 1, id = "stone5", userId = 1),
        StoneInHand(letter = 'F', value = 4, id = "stone6", userId = 1),
        StoneInHand(letter = 'G', value = 2, id = "stone7", userId = 1)
    ),
    stonesOnBoard = setOf(
        StoneOnBoard(letter = 'H', value = 4, id = "stone8", rowIndex = 7, columnIndex = 7),
        StoneOnBoard(letter = 'E', value = 1, id = "stone9", rowIndex = 7, columnIndex = 8),
        StoneOnBoard(letter = 'L', value = 1, id = "stone10", rowIndex = 7, columnIndex = 9),
        StoneOnBoard(letter = 'L', value = 1, id = "stone11", rowIndex = 7, columnIndex = 10),
        StoneOnBoard(letter = 'O', value = 1, id = "stone12", rowIndex = 7, columnIndex = 11)
    ),
    stonesInBag = setOf(
        StoneInBag(letter = 'Q', value = 10, id = "stone13"),
        StoneInBag(letter = 'Z', value = 10, id = "stone14"),
        StoneInBag(letter = 'J', value = 8, id = "stone15"),
        StoneInBag(letter = 'X', value = 8, id = "stone16")
    ),
    currentUserId = 1
)
