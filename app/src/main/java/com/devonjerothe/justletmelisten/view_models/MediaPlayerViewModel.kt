package com.devonjerothe.justletmelisten.view_models

import android.app.Application
import android.content.ComponentName
import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.Player
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import com.devonjerothe.justletmelisten.network.Episode
import com.devonjerothe.justletmelisten.view_models.MediaPlayerUIState.*
import com.google.common.util.concurrent.ListenableFuture
import com.google.common.util.concurrent.MoreExecutors
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
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

class MediaPlayerViewModel(
    app: Application
) : ViewModel() {
    private val _uiState = MutableStateFlow<MediaPlayerUIState>(NoMedia)
    val uiState: StateFlow<MediaPlayerUIState> = _uiState.asStateFlow()

    private var mediaController: MediaController? = null
    private var progressJob: Job? = null

    init {
        val session = SessionToken(app, ComponentName(app, MediaService::class.java))
        val controllerFuture = MediaController.Builder(app, session).buildAsync()

        controllerFuture.addListener({
            mediaController = controllerFuture.get()
            addPlayerListener()
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
                    startProgressJob()
                } else {
                    stopProgressJob()
                }
            }

            override fun onEvents(player: Player, events: Player.Events) {
                if (events.containsAny(Player.EVENT_PLAYBACK_STATE_CHANGED, Player.EVENT_MEDIA_METADATA_CHANGED)) {
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

    private fun startProgressJob() {
        progressJob?.cancel()
        progressJob = viewModelScope.launch {
            while (isActive) {
                _uiState.update { state ->
                    if (state is HasMedia) {
                        val progress = mediaController?.currentPosition?.coerceAtLeast(0L) ?: 0L
                        state.copy(
                            progress = progress.toFloat() / 1000f
                        )
                    } else {
                        state
                    }
                }
                delay(1000)
            }
        }
    }

    private fun stopProgressJob() {
        progressJob?.cancel()
    }

    fun onPlayPause() {
        if (mediaController?.isPlaying == true) {
            mediaController?.pause()
        } else {
            mediaController?.play()
        }
    }

    fun playEpisode(episode: Episode) {
        if (mediaController == null) return

        _uiState.value = HasMedia(
            currentEpisode = episode
        )

        val mediaItem = MediaItem.Builder()
            .setMediaId(episode.id.toString())
            .setUri(episode.audioUrl)
            .setMediaMetadata(
                MediaMetadata.Builder()
                    .setTitle(episode.title ?: "")
                    .build()
            )
            .build()

        mediaController?.setMediaItem(mediaItem)
        mediaController?.prepare()
        mediaController?.play()
    }

    fun seakTo(position: Float) {
        mediaController?.seekTo((position * 1000f).toLong())
    }

    fun closePlayer() {
        mediaController?.stop()
        mediaController?.clearMediaItems()
        _uiState.value = NoMedia
    }

    override fun onCleared() {
        super.onCleared()
        mediaController?.release()
        stopProgressJob()
    }
}
