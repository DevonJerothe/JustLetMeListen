package com.devonjerothe.justletmelisten.view_models

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.devonjerothe.justletmelisten.network.ApiError
import com.devonjerothe.justletmelisten.network.ApiResult
import com.devonjerothe.justletmelisten.network.SearchResult
import com.devonjerothe.justletmelisten.repos.PodcastRepo
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed interface SearchPodcastUIState {
    data class Success(val searchResults: List<SearchResult>) : SearchPodcastUIState
    data class Error(val message: String) : SearchPodcastUIState
    data object Loading : SearchPodcastUIState
    data object Initial : SearchPodcastUIState
}

class PodcastSearchViewModel(
    savedStateHandle: SavedStateHandle,
    private val podcastRepo: PodcastRepo
) : ViewModel() {

    private val _uiState = MutableStateFlow<SearchPodcastUIState>(SearchPodcastUIState.Initial)
    val uiState: StateFlow<SearchPodcastUIState> = _uiState.asStateFlow()

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
                        ApiError.SerializationError -> "Error Parsing Data"
                        ApiError.Timeout -> "Request Timed Out"
                    }
                    SearchPodcastUIState.Error(message)
                }
            }
        }
    }
}
