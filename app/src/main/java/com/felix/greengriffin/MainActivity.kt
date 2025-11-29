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
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.ui.NavDisplay
import com.felix.greengriffin.board.presentation.WordPlacementScreen
import com.felix.greengriffin.board.presentation.WordPlacementViewModel
import com.felix.greengriffin.core.presentation.theme.GreenGriffinTheme
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.serialization.Serializable
import java.util.Map.entry

@Serializable
private data object RouteToHomeScreen : NavKey

@Serializable
private data object RouteToWordPlacementScreen : NavKey


@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        setContent {
            val backStack = rememberNavBackStack(RouteToHomeScreen)
            val viewModel: WordPlacementViewModel = hiltViewModel()
            val state by viewModel.state.collectAsStateWithLifecycle()
            GreenGriffinTheme {
                Surface(
                    modifier = Modifier
                        .fillMaxSize(),
                    color = MaterialTheme.colorScheme.background,
                ) {
                    NavDisplay(
                        backStack = backStack,
                        onBack = { backStack.removeLastOrNull() },
                        entryProvider = entryProvider {
                            entry<RouteToHomeScreen> {
                                HomeScreen(onStartGameClick = {
                                    backStack.add(RouteToWordPlacementScreen)
                                })
                            }
                            entry<RouteToWordPlacementScreen> {
                                WordPlacementScreen(
                                    modifier = Modifier
                                        .windowInsetsPadding(WindowInsets.systemBars)
                                        .verticalScroll(state = rememberScrollState()),
                                    state = state,
                                    onEvent = viewModel::onEvent
                                )
                            }

                        }
                    )
                }
            }
        }
    }
}