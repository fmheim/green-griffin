package com.felix.greengriffin.trails.presentation.levels

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.felix.greengriffin.core.presentation.icons.NavigateBefore
import kotlin.random.Random
import kotlin.random.nextInt

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun TrailLevelsScreen(
    state: TrailLevelsState,
    onBack: () -> Unit, onLevelClick: (level: Int) -> Unit) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(text = "Level Selection") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = NavigateBefore,
                            contentDescription = "Back"
                        )
                    }
                }
            )
        }
    ) { padding ->
        FlowRow(
            modifier = Modifier
                .verticalScroll(state = rememberScrollState())
                .padding(padding)
                .fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            (1..state.levelsCount).forEach { levelIndex ->
                LevelBox(
                    level = levelIndex,
                    completed = levelIndex in state.completedLevels,
                    onClick = { onLevelClick(levelIndex) }
                )
            }
        }
    }
}

@Composable
fun LevelBox(level: Int, completed: Boolean, onClick: () -> Unit) {
    val random = Random(level)
    val shape = RoundedCornerShape(
        topStartPercent = random.nextInt(20..100),
        topEndPercent = random.nextInt(20..100),
        bottomStartPercent = random.nextInt(20..100),
        bottomEndPercent = random.nextInt(20..100)
    )
    Box(
        modifier = Modifier
            .clickable(onClick = onClick)
            .size(120.dp)
            .background(
                if (completed) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.primaryContainer,
                shape = shape,
            )
            .border(
                width = 3.dp,
                shape = shape,
                color = if (completed) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.primary
            ),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = level.toString(),
            fontSize = 32.sp,
            modifier = Modifier.rotate(random.nextInt(-30..30).toFloat()),
            color = if (completed) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onPrimaryContainer
        )
    }
}

@Preview
@Composable
fun TrailLevelsScreenPreview() {
    TrailLevelsScreen(
        onBack = { },
        state = TrailLevelsState(levelsCount = 20, completedLevels = setOf(1, 2, 3)),
        onLevelClick = { }
    )
}
