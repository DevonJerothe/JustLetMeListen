package com.devonjerothe.justletmelisten.view_models

import androidx.lifecycle.*
import com.devonjerothe.justletmelisten.network.*
import com.devonjerothe.justletmelisten.repos.PodcastRepo
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*

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
