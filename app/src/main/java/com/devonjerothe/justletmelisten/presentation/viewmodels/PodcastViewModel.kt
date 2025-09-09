package com.devonjerothe.justletmelisten.presentation.viewmodels

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.devonjerothe.justletmelisten.data.local.Podcast
import com.devonjerothe.justletmelisten.domain.PodcastRepo
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class PodcastUiState(
    val podcasts: List<Podcast> = emptyList(),
    val isLoading: Boolean = false,
    val errorMessage: String? = null
)

class PodcastViewModel(
    savedStateHandle: SavedStateHandle,
    private val podcastRepo: PodcastRepo
) : ViewModel() {

    private val _uiState = MutableStateFlow(PodcastUiState())
    val uiState: StateFlow<PodcastUiState> = _uiState
        .onStart {
            loadPodcasts()
        }
        .stateIn(
            scope= viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = PodcastUiState()
        )

    fun loadPodcasts() {
        _uiState.value = _uiState.value.copy(
            isLoading = true,
            errorMessage = null
        )

        viewModelScope.launch {
            val response = podcastRepo.observePodcasts()
            response.collect { podcasts ->
                _uiState.value = _uiState.value.copy(
                    podcasts = podcasts,
                    isLoading = false
                )
            }
        }
    }
}
