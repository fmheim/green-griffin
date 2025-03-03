package com.felix.greengriffin

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.felix.greengriffin.board.presentation.WordPlacementScreen
import com.felix.greengriffin.board.presentation.WordPlacementViewModel
import com.felix.greengriffin.core.presentation.theme.GreenGriffinTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        setContent {
            val viewModel: WordPlacementViewModel = hiltViewModel()
            val state by viewModel.state.collectAsStateWithLifecycle()

            GreenGriffinTheme {
                Surface(
                    modifier = Modifier
                        .fillMaxSize(),
                    color = MaterialTheme.colorScheme.background,
                ) {
                    WordPlacementScreen(
                        modifier = Modifier
                            .windowInsetsPadding(WindowInsets.systemBars)
                            .verticalScroll(state = rememberScrollState()),
                        state = state,
                        onEvent = viewModel::onEvent
                    )
                }
            }
        }
    }
}