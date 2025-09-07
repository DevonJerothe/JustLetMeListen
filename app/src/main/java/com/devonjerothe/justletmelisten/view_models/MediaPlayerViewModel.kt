package com.devonjerothe.justletmelisten.view_models

import androidx.lifecycle.ViewModel
import com.devonjerothe.justletmelisten.network.Episode
import com.devonjerothe.justletmelisten.view_models.MediaPlayerUIState.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

sealed interface MediaPlayerUIState {
    data object NoMedia : MediaPlayerUIState
    data class HasMedia(
        val paused: Boolean = false,
        val currentEpisode: Episode,
        val progress: Float
    ) : MediaPlayerUIState
}

class MediaPlayerViewModel : ViewModel() {
    private val _uiState = MutableStateFlow<MediaPlayerUIState>(NoMedia)
    val uiState: StateFlow<MediaPlayerUIState> = _uiState.asStateFlow()

    fun onPlayPause() {
        val state = _uiState.value
        _uiState.value = when (state) {
            is HasMedia -> HasMedia(
                paused = !state.paused,
                currentEpisode = state.currentEpisode,
                progress = state.progress
            )
            else -> NoMedia
        }
    }

    fun playEpisode(episode: Episode) {
        _uiState.value = HasMedia(
            paused = false,
            currentEpisode = episode,
            progress = 0f
        )
    }

    fun closePlayer() {
        _uiState.value = NoMedia
    }
}
