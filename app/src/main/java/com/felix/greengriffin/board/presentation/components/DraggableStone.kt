package com.felix.greengriffin.board.presentation.components

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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draganddrop.DragAndDropTransferData
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.felix.greengriffin.board.domain.model.StoneData
import com.felix.greengriffin.board.domain.model.StoneInBag
import com.felix.greengriffin.board.domain.model.StoneInHand
import com.felix.greengriffin.board.domain.model.StoneOnBoard
import com.felix.greengriffin.core.presentation.theme.GreenGriffinTheme

@Composable
fun DraggableStone(
    data: StoneData,
    modifier: Modifier = Modifier,
    width: Dp? = null,
) {
    Box {
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
                .background(
                    color = MaterialTheme.colorScheme.primary,
                    shape = RoundedCornerShape(4.dp)
                )
                .border(
                    width = 2.dp,
                    color = MaterialTheme.colorScheme.outline,
                    shape = RoundedCornerShape(4.dp)
                )
        ) {
            Text(
                modifier = Modifier.padding(4.dp),
                text = " " + data.letter.toString() + " ",
                color = MaterialTheme.colorScheme.onPrimary,
                maxLines = 1,
                style = TextStyle(
                    fontWeight = FontWeight.W900,
                    textAlign = TextAlign.Center
                ),
                autoSize = TextAutoSize.StepBased()
            )
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
            )
        )
    }
}