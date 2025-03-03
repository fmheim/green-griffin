package com.felix.greengriffin.board.presentation.components

import androidx.compose.animation.rememberSplineBasedDecay
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PageSize
import androidx.compose.foundation.pager.PagerDefaults
import androidx.compose.foundation.pager.PagerSnapDistance
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Alignment.Companion.Center
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color.Companion.Transparent
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.lerp
import androidx.compose.ui.window.DialogProperties
import com.felix.greengriffin.board.presentation.alphabet
import com.felix.greengriffin.core.presentation.theme.GreenGriffinTheme
import java.util.UUID
import kotlin.math.absoluteValue

@Composable
fun StoneSelector( // TODO: add alphabet parameter and state + events from a viewmodel
    onLetterSelected: (Char) -> Unit,
    onDismissRequest: () -> Unit
) {
    val selectedLetter = remember { mutableStateOf<Char?>(null) }

    AlertDialog(
        containerColor = MaterialTheme.colorScheme.background,
        onDismissRequest = onDismissRequest,
        properties = DialogProperties(dismissOnBackPress = true, dismissOnClickOutside = true),
        title = {
            Text(
                text = "Select a Letter",
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp),
                textAlign = TextAlign.Center
            )
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                val pagerState = rememberPagerState(pageCount = alphabet::size)
                Box {
                    HorizontalPager(
                        modifier = Modifier.fillMaxWidth(0.7f),
                        pageSize = PageSize.Fill,
                        flingBehavior = PagerDefaults.flingBehavior(
                            state = pagerState,
                            pagerSnapDistance = PagerSnapDistance.atMost(pages = 100),
                            decayAnimationSpec = rememberSplineBasedDecay()
                        ),
                        contentPadding = PaddingValues(horizontal = 60.dp),
                        beyondViewportPageCount = 1,
                        state = pagerState,
                        pageContent = { page ->
                            Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Center) {
                                DraggableStone(
                                    modifier = Modifier
                                        .graphicsLayer {
                                            val pageOffset = (
                                                    (pagerState.currentPage - page)
                                                            + pagerState.currentPageOffsetFraction
                                                    ).absoluteValue

                                            alpha = lerp(
                                                start = 0.8f,
                                                stop = 1f,
                                                fraction = 1f - pageOffset.coerceIn(0f, 1f)
                                            )
                                            scaleX = lerp(
                                                start = 0.85f,
                                                stop = 1f,
                                                fraction = 1f - pageOffset.coerceIn(0f, 1f)
                                            )
                                            scaleY = lerp(
                                                start = 0.85f,
                                                stop = 1f,
                                                fraction = 1f - pageOffset.coerceIn(0f, 1f)
                                            )
                                        },
                                    data = StoneInBag(
                                        letter = alphabet[page],
                                        value = 0,
                                        id = UUID.randomUUID().toString()
                                    )
                                )
                            }
                        }
                    )
                    GradientOverlay()
                }

                Underline()
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    selectedLetter.value?.let { onLetterSelected(it) }
                    onDismissRequest()
                },
                enabled = selectedLetter.value != null
            ) {
                Text(text = "Confirm")
            }
        },
    )
}

@Composable
private fun BoxScope.GradientOverlay() {
    Box(
        modifier = Modifier
            .matchParentSize()
            .background(
                brush = Brush.horizontalGradient(
                    colors = listOf(
                        MaterialTheme.colorScheme.background,
                        Transparent,
                        Transparent,
                        MaterialTheme.colorScheme.background
                    )
                )
            )
    )
}

@Composable
private fun Underline() {
    Box(
        modifier = Modifier
            .padding(top = 12.dp)
            .background(
                color = MaterialTheme.colorScheme.primary,
                shape = CircleShape
            )
            .fillMaxWidth(0.3f)
            .height(8.dp)
    )
}

@Preview(showBackground = true)
@Composable
fun JokerSelectorPreview() = GreenGriffinTheme {
    val showDialog = remember { mutableStateOf(true) }
    if (showDialog.value) {
        StoneSelector(
            onLetterSelected = { showDialog.value = false },
            onDismissRequest = { showDialog.value = false }
        )
    }
}
