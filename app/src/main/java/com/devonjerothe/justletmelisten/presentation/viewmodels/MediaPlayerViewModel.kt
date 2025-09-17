package com.devonjerothe.justletmelisten.presentation.viewmodels

import android.app.Application
import android.content.ComponentName
import android.os.Bundle
import androidx.annotation.OptIn
import androidx.core.net.toUri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import com.devonjerothe.justletmelisten.data.local.Episode
import com.devonjerothe.justletmelisten.domain.MediaService
import com.devonjerothe.justletmelisten.domain.PodcastRepo
import com.devonjerothe.justletmelisten.presentation.viewmodels.MediaPlayerUIState.HasMedia
import com.devonjerothe.justletmelisten.presentation.viewmodels.MediaPlayerUIState.NoMedia
import com.google.common.util.concurrent.MoreExecutors
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

sealed interface MediaPlayerUIState {
    data object NoMedia : MediaPlayerUIState
    data class HasMedia(
        val paused: Boolean = false,
        val currentEpisode: Episode,
        val progress: Float = 0f,
        val duration: Float = 0f,
    ) : MediaPlayerUIState
}

@OptIn(UnstableApi::class)
class MediaPlayerViewModel
    (
    app: Application,
    private val podcastRepo: PodcastRepo
) : ViewModel() {
    private val _uiState = MutableStateFlow<MediaPlayerUIState>(NoMedia)
    val uiState: StateFlow<MediaPlayerUIState> = _uiState.asStateFlow()

    val playingEpisode: Flow<Episode?> = _uiState.map { state ->
        if (state is HasMedia) state.currentEpisode else null
    }
    
    val paused: Flow<Boolean> = _uiState.map { state ->
        if (state is HasMedia) state.paused else false
    }

    private var mediaController: MediaController? = null
    private var uiProgressJob: Job? = null

    init {
        val session = SessionToken(app, ComponentName(app, MediaService::class.java))
        val controllerFuture = MediaController.Builder(app, session).buildAsync()

        controllerFuture.addListener({
            mediaController = controllerFuture.get()
            addPlayerListener()
            loadLastPlayedEpisode()
        }, MoreExecutors.directExecutor())
    }

    private fun addPlayerListener() {
        mediaController?.addListener(object: Player.Listener {
            override fun onIsPlayingChanged(isPlaying: Boolean) {
                _uiState.update { state ->
                    if (state is HasMedia) {
                        state.copy(paused = !isPlaying)
                    } else {
                        state
                    }
                }

                if (isPlaying) {
                    startUIProgressJob()
                } else {
                    stopUIProgressJob()
                }
            }

            override fun onEvents(player: Player, events: Player.Events) {
                // Only handle UI-specific updates here
                if (events.containsAny(
                    Player.EVENT_PLAYBACK_STATE_CHANGED, 
                    Player.EVENT_MEDIA_METADATA_CHANGED,
                    Player.EVENT_TRACKS_CHANGED
                )) {
                    _uiState.update { state ->
                        if (state is HasMedia) {
                            state.copy(
                                duration = player.duration.coerceAtLeast(0L).toFloat() / 1000f
                            )
                        } else {
                            state
                        }
                    }
                }
            }
        })
    }

    private fun loadLastPlayedEpisode() {
        viewModelScope.launch {
            val controller = mediaController ?: return@launch
            val currentItem = controller.currentMediaItem
            if (currentItem != null) {
                val currentEpisode = podcastRepo.getEpisode(currentItem.mediaId) ?: return@launch
                _uiState.value = HasMedia(
                    currentEpisode = currentEpisode,
                    progress = controller.currentPosition.coerceAtLeast(0L).toFloat() / 1000f,
                    duration = controller.duration.coerceAtLeast(0L).toFloat() / 1000f,
                    paused = !controller.isPlaying
                )
                if (!controller.isPlaying) {
                    startUIProgressJob()
                }
            } else {
                val lastPlayed = podcastRepo.getLastPlayedEpisode()
                lastPlayed?.let {
                    playEpisode(lastPlayed, startPaused = true)
                }
            }
        }
    }

    private fun startUIProgressJob() {
        uiProgressJob?.cancel()
        uiProgressJob = viewModelScope.launch {
            while (isActive) {
                _uiState.update { state ->
                    if (state is HasMedia) {
                        val progress = mediaController?.currentPosition?.coerceAtLeast(0L) ?: 0L
                        state.copy(progress = progress.toFloat() / 1000f)
                    } else {
                        state
                    }
                }
                delay(1000) // Update UI every second for smooth progress bar
            }
        }
    }

    private fun stopUIProgressJob() {
        uiProgressJob?.cancel()
    }

    fun onPlayPause() {
        if (mediaController?.isPlaying == true) {
            mediaController?.pause()
        } else {
            mediaController?.play()
        }
    }

    fun playEpisode(episode: Episode, startPaused: Boolean = false) {
        if (mediaController == null) return

        if (mediaController?.currentMediaItem?.mediaId == episode.guid) {
            if (!startPaused) {
                mediaController?.play()
            }
            return
        }

        // Update UI state immediately
        _uiState.value = HasMedia(
            currentEpisode = episode,
            progress = episode.progress,
            paused = startPaused
        )

        val mediaItem = MediaItem.Builder()
            .setMediaId(episode.guid)
            .setUri(episode.audioUrl)
            .setMediaMetadata(
                MediaMetadata.Builder()
                    .setTitle(episode.title ?: "")
                    .setArtworkUri(episode.imageUrl?.toUri())
                    // Store episode progress in extras for the service to use
                    .setExtras(Bundle().apply { putFloat("episode_progress", episode.progress) })
                    .build()
            )
            .build()

        mediaController?.setMediaItem(mediaItem)
        mediaController?.prepare()

        if (!startPaused) {
            mediaController?.play()
        }
    }

    fun seekTo(position: Float) {
        mediaController?.seekTo((position * 1000f).toLong())
        
        // Update UI immediately for responsiveness
        _uiState.update { state ->
            if (state is HasMedia) {
                state.copy(progress = position)
            } else {
                state
            }
        }
    }

    fun closePlayer() {
        stopUIProgressJob()
        mediaController?.stop()
        mediaController?.clearMediaItems()
        _uiState.value = NoMedia
    }

    override fun onCleared() {
        super.onCleared()
        mediaController?.release()
        stopUIProgressJob()
    }
}
