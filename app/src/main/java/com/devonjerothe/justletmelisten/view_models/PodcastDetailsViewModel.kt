package com.devonjerothe.justletmelisten.view_models

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.devonjerothe.justletmelisten.destinationArgs.FEED_URL
import com.devonjerothe.justletmelisten.destinationArgs.PODCAST_ID
import com.devonjerothe.justletmelisten.destinationArgs.TRACK_ID
import com.devonjerothe.justletmelisten.network.Episode
import com.devonjerothe.justletmelisten.network.Podcast
import com.devonjerothe.justletmelisten.network.PodcastWithEpisodes
import com.devonjerothe.justletmelisten.repos.PodcastRepo
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.net.URLDecoder
import java.nio.charset.StandardCharsets

sealed interface PodcastDetailsUIState {
    data class Success(val podcast: Podcast, val episodes: List<Episode>) : PodcastDetailsUIState
    data class Error(val message: String) : PodcastDetailsUIState
    data object Loading : PodcastDetailsUIState
}

class PodcastDetailsViewModel(
    savedStateHandle: SavedStateHandle,
    private val podcastRepo: PodcastRepo,
    private val mediaPlayer: MediaPlayerViewModel
) : ViewModel() {

    private val podcastId = savedStateHandle.get<Long>(PODCAST_ID)
    private val feedUrl = savedStateHandle.get<String>(FEED_URL)?.let { encodedUrl ->
        URLDecoder.decode(encodedUrl, StandardCharsets.UTF_8.toString())
    }

    private val trackId = savedStateHandle.get<Long>(TRACK_ID)
    private var dbObserver: Job? = null

    private val _uiState = MutableStateFlow<PodcastDetailsUIState>(PodcastDetailsUIState.Loading)
    val uiState: StateFlow<PodcastDetailsUIState> = _uiState
        .onStart {
            loadDetails()
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = PodcastDetailsUIState.Loading
        )

    val activeEpisode: Flow<Episode?> = mediaPlayer.playingEpisode
    val paused: Flow<Boolean> = mediaPlayer.paused

    fun loadDetails(useCache: PodcastWithEpisodes? = null, skipLoading: Boolean = false) {
        dbObserver?.cancel()

        /*
        if we unsubscribe from a podcast and remove the DB entry then we would be reloading the
        API response when we refresh the details.. this is a waste of bandwidth. So we can simply pass
        a cached non DB entry for the UI.
         */
        if (useCache != null) {
            _uiState.value = PodcastDetailsUIState.Success(
                podcast = useCache.podcast,
                episodes = useCache.episodes
            )
            return
        }

        if (!skipLoading) {
            _uiState.value = PodcastDetailsUIState.Loading
        }

        viewModelScope.launch {
            var podcast: Podcast? = null
            podcastId?.let {
                podcast = podcastRepo.getPodcast(podcastId)
            }
            trackId?.let {
                podcast = podcastRepo.getPodcastByTrack(trackId)
            }

            // Check if podcast is already in DB cache
            if (podcast == null) {
                val podcastDetails = podcastRepo.getPodcastFeed(feedUrl ?: "", trackId ?: 0)
                if (podcastDetails != null) {
                    _uiState.value = PodcastDetailsUIState.Success(
                        podcast = podcastDetails.podcast,
                        episodes = podcastDetails.episodes
                    )
                } else {
                    _uiState.value = PodcastDetailsUIState.Error("Podcast not found")
                }
            } else {
                dbObserver = launch {
                    podcastRepo.observePodcastFeed(podcast.id).collect { podcast ->
                        _uiState.value = PodcastDetailsUIState.Success(
                            podcast = podcast.podcast,
                            episodes = podcast.episodes
                        )
                    }
                }
            }
        }
    }

    fun subscribeToPodcast() {
        val state = _uiState.value

        if (state is PodcastDetailsUIState.Success) {
            val podcast = state.podcast
            val episodes = state.episodes

            viewModelScope.launch {
                podcastRepo.followPodcast(PodcastWithEpisodes(
                    podcast = podcast,
                    episodes = episodes
                ))

                loadDetails(skipLoading = true)
            }
        }
    }

    fun unsubscribeToPodcast() {
        val state = _uiState.value
        if (state is PodcastDetailsUIState.Success) {
            val podcast = state.podcast
            val episodes = state.episodes

            viewModelScope.launch {
                dbObserver?.cancel()
                podcastRepo.unfollowPodcast(podcast)

                val podcast = podcast.copy(subscribed = false)
                loadDetails(
                    useCache = PodcastWithEpisodes(
                        podcast = podcast,
                        episodes = episodes
                    )
                )
            }
        }
    }

    fun playEpisode(episode: Episode, activeId: Long? = null) {
        if (activeId == episode.id) {
            mediaPlayer.onPlayPause()
        } else {
            mediaPlayer.playEpisode(episode)
        }
    }
}
