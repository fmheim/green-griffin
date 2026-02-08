package com.felix.greengriffin.board.presentation.components

import android.content.ClipDescription
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideIn
import androidx.compose.animation.slideOut
import androidx.compose.foundation.draganddrop.dragAndDropTarget
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draganddrop.DragAndDropEvent
import androidx.compose.ui.draganddrop.DragAndDropTarget
import androidx.compose.ui.draganddrop.mimeTypes
import androidx.compose.ui.draganddrop.toAndroidDragEvent
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.felix.greengriffin.board.presentation.GameEvent
import com.felix.greengriffin.board.presentation.GameEvent.StoneDroppedOnHand
import com.felix.greengriffin.board.presentation.GameState
import com.felix.greengriffin.core.presentation.icons.DoubleArrowDown


@Composable
fun StonesRow(
    modifier: Modifier = Modifier,
    state: GameState,
    onEvent: (GameEvent) -> Unit,
    horizontalPaddingDp: Float = 16f,
    spacedByDp: Float = 4f,
) {
    val width =
        (LocalConfiguration.current.screenWidthDp - (2f * horizontalPaddingDp) - (6f * spacedByDp)) / 7f
    Box(modifier = modifier) {
        LazyRow(
            modifier = Modifier
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
                    modifier = Modifier.animateItem(),
                    width = width.dp,
                    data = data,
                    backgroundRes = state.stoneBackgroundImageRes,
                    textImageRes = state.stoneTextImageRes,
                )
                if (index < state.stonesInHand.lastIndex) Spacer(
                    modifier = Modifier.width(
                        spacedByDp.dp
                    )
                )
            }
        }
        ReturnAllUnlockedIconButton(state = state, onEvent = onEvent)
    }
}

@Composable
private fun BoxScope.ReturnAllUnlockedIconButton(
    state: GameState,
    onEvent: (GameEvent) -> Unit,
) {
    AnimatedVisibility(
        visible = state.hasUnlockedStones,
        enter = slideIn(initialOffset = { IntOffset(x = it.width, y = 0) }),
        exit = slideOut(targetOffset = { IntOffset(x = it.width, y = 0) })
    ) {
        Row(
            modifier = Modifier
                .matchParentSize()
                .padding(end = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Spacer(modifier = Modifier.weight(1f))
            IconButton(
                modifier = Modifier.align(Alignment.CenterVertically),
                onClick = { onEvent(GameEvent.ReturnAllUnlockedStonesClick) }) {
                Icon(
                    modifier = Modifier.size(32.dp),
                    imageVector = DoubleArrowDown,
                    contentDescription = null
                )
            }
        }
    }
}