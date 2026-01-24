package com.felix.greengriffin.trails.presentation.levels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.felix.greengriffin.board.data.repository.CompletedLevelsRepository
import com.felix.greengriffin.board.presentation.GameMode
import com.felix.greengriffin.board.presentation.GameMode.Companion.TRAILS_ID
import com.felix.greengriffin.board.presentation.trailLevels
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class TrailLevelsState(
    val levelsCount: Int = trailLevels.count(),
    val completedLevels: Set<Int> = emptySet(),
)

@HiltViewModel
class TrailLevelsViewModel @Inject constructor(
    completedLevelsRepository: CompletedLevelsRepository,
) : ViewModel() {
    private val _state = MutableStateFlow(TrailLevelsState())
    val state = _state.asStateFlow()


    init {
        viewModelScope.launch {
            completedLevelsRepository
                .observeCompletedLevelsFor(TRAILS_ID)
                .collect { completedLevels ->
                    _state.update {
                        it.copy(completedLevels = completedLevels.toSet())
                    }
                }
        }
    }
}