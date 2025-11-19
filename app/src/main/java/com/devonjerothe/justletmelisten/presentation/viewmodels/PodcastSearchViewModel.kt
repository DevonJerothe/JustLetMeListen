package com.devonjerothe.justletmelisten.presentation.viewmodels

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.devonjerothe.justletmelisten.data.remote.ApiError
import com.devonjerothe.justletmelisten.data.remote.ApiResult
import com.devonjerothe.justletmelisten.data.remote.SearchResult
import com.devonjerothe.justletmelisten.data.remote.TrendingPodcast
import com.devonjerothe.justletmelisten.domain.PodcastRepo
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

sealed interface SearchPodcastUIState {
    data class Success(val searchResults: List<SearchResult>) : SearchPodcastUIState
    data class Error(val message: String) : SearchPodcastUIState
    data object Loading : SearchPodcastUIState
    data object Initial : SearchPodcastUIState
}

sealed interface TrendingPodcastUIState {
    data class Success(val trendingPodcasts: List<TrendingPodcast>) : TrendingPodcastUIState
    data class Error(val message: String) : TrendingPodcastUIState
    data object Loading : TrendingPodcastUIState
}

class PodcastSearchViewModel(
    savedStateHandle: SavedStateHandle,
    private val podcastRepo: PodcastRepo
) : ViewModel() {

    private val _uiState = MutableStateFlow<SearchPodcastUIState>(SearchPodcastUIState.Initial)
    val uiState: StateFlow<SearchPodcastUIState> = _uiState.asStateFlow()

    private val _trendingUiState = MutableStateFlow<TrendingPodcastUIState>(TrendingPodcastUIState.Loading)
    val trendingUiState: StateFlow<TrendingPodcastUIState> = _trendingUiState
        .onStart {
            getTrendingPodcasts()
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = TrendingPodcastUIState.Loading
        )

    fun searchPodcasts(query: String) {
        if (query.isBlank()) {
            return
        }

        viewModelScope.launch {
            _uiState.value = SearchPodcastUIState.Loading

            val response = podcastRepo.searchPodcasts(query)
            _uiState.value = when (response) {
                is ApiResult.Success -> {
                    SearchPodcastUIState.Success(response.data.results)
                }
                is ApiResult.Error -> {
                    val message = when (response.exception) {
                        is ApiError.NetworkError -> "Network Error"
                        is ApiError.ServerError -> "Server Error: ${response.exception.code}"
                        is ApiError.UnknownError -> response.exception.message
                        is ApiError.SerializationError -> "Error Parsing Data"
                        is ApiError.Timeout -> "Request Timed Out"
                    }
                    SearchPodcastUIState.Error(message)
                }
            }
        }
    }

    fun getTrendingPodcasts() {
        viewModelScope.launch {
            val response = podcastRepo.getTrendingPodcasts()
            _trendingUiState.value = when (response) {
                is ApiResult.Success -> {
                    TrendingPodcastUIState.Success(response.data.feeds)
                }
                is ApiResult.Error -> {
                    val message = when (response.exception) {
                        is ApiError.NetworkError -> "Network Error"
                        is ApiError.ServerError -> "Server Error: ${response.exception.code}"
                        is ApiError.UnknownError -> response.exception.message
                        is ApiError.SerializationError -> "Error Parsing Data"
                        is ApiError.Timeout -> "Request Timed Out"
                    }
                    TrendingPodcastUIState.Error(message)
                }
            }
        }
    }
}
