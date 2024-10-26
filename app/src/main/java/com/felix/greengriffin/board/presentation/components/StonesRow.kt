package com.felix.greengriffin.board.presentation.components

import android.content.ClipDescription
import androidx.compose.foundation.draganddrop.dragAndDropTarget
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draganddrop.DragAndDropEvent
import androidx.compose.ui.draganddrop.DragAndDropTarget
import androidx.compose.ui.draganddrop.mimeTypes
import androidx.compose.ui.draganddrop.toAndroidDragEvent
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.unit.dp
import com.felix.greengriffin.board.presentation.DraggableStone
import com.felix.greengriffin.board.presentation.ScrabbleEvent
import com.felix.greengriffin.board.presentation.ScrabbleEvent.StoneDroppedOnHand
import com.felix.greengriffin.board.presentation.ScrabbleState
import com.felix.greengriffin.board.presentation.StoneData

@Composable
fun StonesRow(
    modifier: Modifier = Modifier,
    state: ScrabbleState,
    onEvent: (ScrabbleEvent) -> Unit,
    horizontalPaddingDp: Float = 16f,
    spacedByDp: Float = 4f
) {
    val width =
        (LocalConfiguration.current.screenWidthDp - (2f * horizontalPaddingDp) - (6f * spacedByDp)) / 7f

    LazyRow(modifier = modifier
        .padding(horizontal = horizontalPaddingDp.dp)
        .dragAndDropTarget(
            shouldStartDragAndDrop = { event ->
                event
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
                            StoneDroppedOnHand(
                                stoneData = data,
                            )
                        )
                        return true
                    }
                }
            }
        )) {
        itemsIndexed(state.stonesInHand, key = { _, data -> data.id }) { index, data ->
            DraggableStone(
                width = width.dp,
                data = data,
                modifier = Modifier.Companion.animateItem()
            )
            if (index < state.stonesInHand.lastIndex) Spacer(modifier = Modifier.width(spacedByDp.dp))
        }
    }
}