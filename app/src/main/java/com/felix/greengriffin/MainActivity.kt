package com.felix.greengriffin

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.core.tween
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.ui.NavDisplay
import com.felix.greengriffin.board.presentation.WordPlacementScreen
import com.felix.greengriffin.board.presentation.WordPlacementViewModel
import com.felix.greengriffin.core.presentation.theme.GreenGriffinTheme
import com.felix.greengriffin.trails.presentation.levels.TrailLevelsScreen
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.serialization.Serializable

@Serializable
private data object RouteToHomeScreen : NavKey

@Serializable
private data object RouteToWordPlacementScreen : NavKey

@Serializable
private data object RouteToWordyTrails : NavKey


@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        setContent {
            val backStack = rememberNavBackStack(RouteToHomeScreen)
            val viewModel: WordPlacementViewModel = hiltViewModel()
            val state by viewModel.state.collectAsStateWithLifecycle()
            GreenGriffinTheme(dynamicColor = false) {
                Surface(
                    modifier = Modifier
                        .fillMaxSize(),
                    color = MaterialTheme.colorScheme.background,
                ) {
                    NavDisplay(
                        backStack = backStack,
                        onBack = { backStack.removeLastOrNull() },
                        transitionSpec = {
                            slideInHorizontally(
                                initialOffsetX = { it },
                                animationSpec = tween(1000)
                            ) togetherWith slideOutHorizontally(
                                targetOffsetX = { -it },
                                animationSpec = tween(1000)
                            )
                        },
                        popTransitionSpec = {
                            // Slide in from left when navigating back
                            slideInHorizontally(
                                initialOffsetX = { -it },
                                animationSpec = tween(1000)
                            ) togetherWith slideOutHorizontally(
                                targetOffsetX = { it },
                                animationSpec = tween(1000)
                            )
                        },
                        predictivePopTransitionSpec = {
                            // Slide in from left when navigating back
                            slideInHorizontally(
                                initialOffsetX = { -it },
                                animationSpec = tween(1000)
                            ) togetherWith slideOutHorizontally(
                                targetOffsetX = { it },
                                animationSpec = tween(1000)
                            )
                        },
                        entryProvider = entryProvider {
                            entry<RouteToHomeScreen> {
                                HomeScreen(
                                    onFreePlayClick = {
                                        backStack.add(RouteToWordPlacementScreen)
                                    },
                                    onWordyTrailsClick = {
                                        backStack.add(RouteToWordyTrails)
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
                            entry<RouteToWordyTrails> {
                                TrailLevelsScreen(onBack = { backStack.removeLastOrNull() })
                            }
                        }
                    )
                }
            }
        }
    }
}