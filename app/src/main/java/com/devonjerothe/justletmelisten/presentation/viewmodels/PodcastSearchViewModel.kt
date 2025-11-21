package com.devonjerothe.justletmelisten.presentation.viewmodels

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.devonjerothe.justletmelisten.data.remote.ApiError
import com.devonjerothe.justletmelisten.data.remote.ApiResult
import com.devonjerothe.justletmelisten.data.remote.itunes.toSearchListItem
import com.devonjerothe.justletmelisten.domain.PodcastRepo
import com.devonjerothe.justletmelisten.domain.models.PodcastSearchModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

sealed interface PodcastSearchUiState {
    data class Popular(
        val items: List<PodcastSearchModel> = emptyList(),
        val isLoading: Boolean = false,
        val error: String? = null
    ) : PodcastSearchUiState

    data class SearchResults(
        val query: String,
        val items: List<PodcastSearchModel> = emptyList(),
        val isLoading: Boolean = false,
        val error: String? = null
    ) : PodcastSearchUiState
}

class PodcastSearchViewModel(
        savedStateHandle: SavedStateHandle,
        private val podcastRepo: PodcastRepo
) : ViewModel() {

    private val _uiState =
            MutableStateFlow<PodcastSearchUiState>(PodcastSearchUiState.Popular(isLoading = true))
    val uiState: StateFlow<PodcastSearchUiState> =
            _uiState
                    .onStart { getPopularPodcasts() }
                    .stateIn(
                            scope = viewModelScope,
                            started = SharingStarted.WhileSubscribed(5000),
                            initialValue = PodcastSearchUiState.Popular(isLoading = true)
                    )

    fun searchPodcasts(query: String) {
        if (query.isBlank()) {
            // If query is blank, go back to trending
            val currentState = _uiState.value
            if (currentState is PodcastSearchUiState.SearchResults) {
                getPopularPodcasts()
            }
            return
        }

        viewModelScope.launch {
            _uiState.value = PodcastSearchUiState.SearchResults(query = query, isLoading = true)

            val response = podcastRepo.searchPodcasts(query)
            _uiState.value =
                    when (response) {
                        is ApiResult.Success -> {
                            PodcastSearchUiState.SearchResults(
                                    query = query,
                                    items = response.data.results.map { it.toSearchListItem() },
                                    isLoading = false
                            )
                        }
                        is ApiResult.Error -> {
                            val message = getErrorMessage(response.exception)
                            PodcastSearchUiState.SearchResults(
                                    query = query,
                                    isLoading = false,
                                    error = message
                            )
                        }
                    }
        }
    }

    fun getPopularPodcasts() {
        viewModelScope.launch {
            _uiState.value = PodcastSearchUiState.Popular(isLoading = true)

            val response = podcastRepo.getPopularPodcasts()
            _uiState.value =
                    when (response) {
                        is ApiResult.Success -> {
                            PodcastSearchUiState.Popular(
                                    items = response.data.map { it.toSearchListItem() },
                                    isLoading = false
                            )
                        }
                        is ApiResult.Error -> {
                            val message = getErrorMessage(response.exception)
                            PodcastSearchUiState.Popular(isLoading = false, error = message)
                        }
                    }
        }
    }

    fun clearSearch() {
        getPopularPodcasts()
    }

    private fun getErrorMessage(exception: ApiError): String {
        return when (exception) {
            is ApiError.NetworkError -> "Network Error"
            is ApiError.ServerError -> "Server Error: ${exception.code}"
            is ApiError.UnknownError -> exception.message
            is ApiError.SerializationError -> "Error Parsing Data"
            is ApiError.Timeout -> "Request Timed Out"
        }
    }
}
