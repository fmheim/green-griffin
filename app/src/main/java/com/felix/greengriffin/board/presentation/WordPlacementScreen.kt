@file:OptIn(ExperimentalFoundationApi::class)

package com.felix.greengriffin.board.presentation


import android.content.ClipDescription
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.ContentTransform
import androidx.compose.animation.Crossfade
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.core.FastOutLinearInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.VisibilityThreshold
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.draganddrop.dragAndDropTarget
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draganddrop.DragAndDropEvent
import androidx.compose.ui.draganddrop.DragAndDropTarget
import androidx.compose.ui.draganddrop.mimeTypes
import androidx.compose.ui.draganddrop.toAndroidDragEvent
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import com.felix.greengriffin.R
import com.felix.greengriffin.board.presentation.GameEvent.StoneDroppedOnBoard
import com.felix.greengriffin.board.presentation.components.DraggableStone
import com.felix.greengriffin.board.presentation.components.StoneData
import com.felix.greengriffin.board.presentation.components.StoneInBag
import com.felix.greengriffin.board.presentation.components.StoneInHand
import com.felix.greengriffin.board.presentation.components.StoneOnBoard
import com.felix.greengriffin.board.presentation.components.StoneSelector
import com.felix.greengriffin.board.presentation.components.StonesRow
import com.felix.greengriffin.core.presentation.theme.GreenGriffinTheme
import com.felix.greengriffin.util.extensions.compose.animatedGradientBrush


@OptIn(ExperimentalAnimationApi::class)
@Composable
fun WordPlacementScreen(
    modifier: Modifier = Modifier,
    state: GameState,
    onEvent: (GameEvent) -> Unit,
) {
    AnimatedVisibility(state.isJokerSelectorVisible) {
        StoneSelector(
            onLetterSelected = { onEvent(GameEvent.JokerSelected(it)) },
            onDismissRequest = { onEvent(GameEvent.JokerSelectorDismissRequested) }
        )
    }
    Column(modifier = modifier) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.End
        ) {
            Text(
                text = "Score: ",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 16.sp
            )
            AnimatedContent(
                targetState = state.totalPoints,
                transitionSpec = {
                    ContentTransform(
                        targetContentEnter =
                            slideInVertically(
                                initialOffsetY = { -2 * it },
                                animationSpec = spring(
                                    dampingRatio = Spring.DampingRatioMediumBouncy,
                                    stiffness = Spring.StiffnessLow
                                )
                            ) + fadeIn(),
                        initialContentExit = slideOutVertically(
                            animationSpec = tween(
                                5000,
                                easing = FastOutLinearInEasing,
                                delayMillis = 0
                            ),
                            targetOffsetY = { 200 * it }),
                        sizeTransform = SizeTransform(clip = false)
                    )
                },
                label = "score_animation"
            ) { points ->
                Text(
                    text = points.toString(),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 16.sp
                )
            }
        }
        WordBoard(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp)
                .padding(bottom = 16.dp)
                .border(
                    width = when {
                        state.isAbleToSubmit -> 5.dp
                        else -> 2.dp
                    },
                    color = when {
                        state.isAbleToSubmit -> MaterialTheme.colorScheme.outline // todo maybe animate border when able to send in
                        else -> MaterialTheme.colorScheme.outline
                    }
                ),
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
            border = BorderStroke(width = 2.dp, color = MaterialTheme.colorScheme.outline),
            onClick = { onEvent(GameEvent.DrawStonesClick) }) {
            Text(text = "Steine auffüllen", color = MaterialTheme.colorScheme.onSecondary)
            Icon(
                painter = painterResource(R.drawable.ic_draw_stones),
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSecondary
            )
        }
        Spacer(modifier = Modifier.weight(1f))

        AnimatedVisibility(
            visible = state.isAbleToSubmit,
            enter = slideInVertically(
                initialOffsetY = { it },
                animationSpec = spring(
                    dampingRatio = 0.3f,
                    stiffness = 100f,
                    visibilityThreshold = IntOffset.VisibilityThreshold
                )
            ),
            exit = slideOutVertically(targetOffsetY = { 2 * it })
        ) {
            Box(
                modifier = Modifier
                    .padding(16.dp)
                    .animatedGradientBrush()
                    .fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                Row(
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .clip(RoundedCornerShape(30.dp))
                        .border(
                            width = 2.dp,
                            color = MaterialTheme.colorScheme.onSurface,
                            shape = RoundedCornerShape(30.dp)
                        )
                        .clickable { onEvent(GameEvent.SubmitClick) }
                        .padding(16.dp)
                ) {

                    Text(
                        modifier = Modifier,
                        text = "Abschicken",
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        modifier = Modifier,
                        text = "(${state.pointsOfCurrentPlacement} Punkte)",
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Crossfade(
                        targetState = state.isPromptLoading,
                        label = "loading_transition"
                    ) { isLoading ->
                        if (isLoading) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(24.dp),
                                color = MaterialTheme.colorScheme.onSecondary,
                                strokeWidth = 2.dp
                            )
                        } else {
                            Icon(
                                modifier = Modifier,
                                painter = painterResource(R.drawable.ic_send),
                                contentDescription = null,
                            )
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun WordBoard(
    modifier: Modifier = Modifier,
    onEvent: (GameEvent) -> Unit,
    state: GameState,
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
                    .zIndex(numColumns * numColumns - index.toFloat())
                    .aspectRatio(1f)
                    .background(color = if (state.enteredField == index) MaterialTheme.colorScheme.onSecondaryContainer else MaterialTheme.colorScheme.secondaryContainer)
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
                                    onEvent(GameEvent.FieldEntered(index = index))
                                }
                            }
                        }
                    ),
                content = {
                    Box(
                        modifier = Modifier
                            .matchParentSize()
                            .border(width = 1.dp, color = MaterialTheme.colorScheme.outline)
                    )
                    state.stonesOnBoard.find { stone ->
                        stone.columnIndex == getColumnIndex(
                            gridIndex = index,
                            totalColumns = numColumns
                        ) && stone.rowIndex == getRowIndex(
                            gridIndex = index,
                            totalColumns = numColumns
                        )
                    }?.let {
                        DraggableStone(
                            modifier = Modifier.fillMaxSize(),
                            data = it
                        )
                    }
                }
            )
        }
    }
}


@Preview(showBackground = true, backgroundColor = 0xFF164134)
@Composable
fun WordPlacementScreenPreview() {
    GreenGriffinTheme {
        WordPlacementScreen(state = previewGameState, onEvent = {})
    }
}

fun getColumnIndex(gridIndex: Int, totalColumns: Int) = gridIndex % totalColumns
fun getRowIndex(gridIndex: Int, totalColumns: Int) = gridIndex / totalColumns

val previewGameState = GameState(
    isCurrentWordValid = true,
    totalPoints = 42,
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
        StoneOnBoard(
            letter = 'H',
            value = 4,
            id = "stone8",
            rowIndex = 7,
            columnIndex = 7,
            isLocked = true
        ),
        StoneOnBoard(
            letter = 'E',
            value = 1,
            id = "stone9",
            rowIndex = 7,
            columnIndex = 8,
            isLocked = true
        ),
        StoneOnBoard(
            letter = 'L',
            value = 1,
            id = "stone10",
            rowIndex = 7,
            columnIndex = 9,
            isLocked = true
        ),
        StoneOnBoard(
            letter = 'L',
            value = 1,
            id = "stone11",
            rowIndex = 7,
            columnIndex = 10,
            isLocked = true
        ),
        StoneOnBoard(
            letter = 'O',
            value = 1,
            id = "stone12",
            rowIndex = 7,
            columnIndex = 11,
            isLocked = false
        )
    ),
    stonesInBag = setOf(
        StoneInBag(letter = 'Q', value = 10, id = "stone13"),
        StoneInBag(letter = 'Z', value = 10, id = "stone14"),
        StoneInBag(letter = 'J', value = 8, id = "stone15"),
        StoneInBag(letter = 'X', value = 8, id = "stone16")
    ),
    currentUserId = 1
)
