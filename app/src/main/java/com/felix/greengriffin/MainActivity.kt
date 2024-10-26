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
import androidx.lifecycle.viewmodel.compose.viewModel
import com.felix.greengriffin.board.presentation.ScrabbleScreen
import com.felix.greengriffin.board.presentation.ScrabbleViewModel
import com.felix.greengriffin.core.presentation.theme.GreenGriffinTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        setContent {
            val viewModel: ScrabbleViewModel = viewModel()
            val state by viewModel.state.collectAsState()

            GreenGriffinTheme {
                Surface(
                    modifier = Modifier
                        .fillMaxSize(),
                    color = MaterialTheme.colorScheme.background,
                ) {
                    ScrabbleScreen(
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