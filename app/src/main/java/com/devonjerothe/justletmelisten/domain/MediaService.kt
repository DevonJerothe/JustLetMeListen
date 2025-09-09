package com.devonjerothe.justletmelisten.domain

import android.os.Bundle
import androidx.annotation.OptIn
import androidx.core.net.toUri
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.session.CommandButton
import androidx.media3.session.LibraryResult
import androidx.media3.session.MediaLibraryService
import androidx.media3.session.MediaSession
import androidx.media3.session.SessionCommand
import androidx.media3.session.SessionResult
import com.google.common.collect.ImmutableList
import com.google.common.util.concurrent.Futures
import com.google.common.util.concurrent.ListenableFuture
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.guava.future
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import org.koin.android.ext.android.inject

private const val ROOT_ID = "JUSTLETMELISTEN_ROOT"
private const val CUSTOM_SKIP_FORWARD = "skip_forward"
private const val CUSTOM_SKIP_BACKWARD = "skip_backward"

class MediaService : MediaLibraryService() {

    private val podcastRepo: PodcastRepo by inject()
    private val serviceScope = CoroutineScope(Dispatchers.Main)

    private lateinit var player: ExoPlayer
    private lateinit var mediaLibrarySession: MediaLibrarySession
    private var progressJob: Job? = null

    @OptIn(UnstableApi::class)
    override fun onCreate() {
        super.onCreate()

        val commandList = listOf(
            CommandButton.Builder(CommandButton.ICON_SKIP_BACK_30)
                .setDisplayName("Skip Back 30s")
                .setSessionCommand(SessionCommand(CUSTOM_SKIP_BACKWARD, Bundle.EMPTY))
                .build(),
            CommandButton.Builder(CommandButton.ICON_SKIP_FORWARD_30)
                .setDisplayName("Skip Forward 30s")
                .setSessionCommand(SessionCommand(CUSTOM_SKIP_FORWARD, Bundle.EMPTY))
                .build()
        )

        val audioAttributes = AudioAttributes.Builder()
            .setContentType(C.AUDIO_CONTENT_TYPE_SPEECH)
            .setUsage(C.USAGE_MEDIA)
            .build()

        player = ExoPlayer.Builder(this)
            .setAudioAttributes(audioAttributes, true)
            .build()
            .apply { 
                playWhenReady = false
                addListener(playerListener)
            }

        mediaLibrarySession =
            MediaLibrarySession.Builder(this, player, LibrarySessionCallback())
                .setCommandButtonsForMediaItems(commandList)
                .setMediaButtonPreferences(commandList)
                .build()
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaLibrarySession {
        return mediaLibrarySession
    }

    override fun onDestroy() {
        stopProgressJob()
        player.release()
        mediaLibrarySession.release()
        super.onDestroy()
    }

    private var hasAutoSeeked = false

    private val playerListener = object: Player.Listener {
        override fun onIsPlayingChanged(isPlaying: Boolean) {
            if (isPlaying) {
                startProgressJob()
            } else {
                stopProgressJob()
                // Save progress when pausing
                updateCurrentEpisodeProgress()
            }
        }

        override fun onEvents(player: Player, events: Player.Events) {
            // Handle auto-seek when media is ready and buffered
            if (events.contains(Player.EVENT_PLAYBACK_STATE_CHANGED) && 
                player.playbackState == Player.STATE_READY && !hasAutoSeeked) {
                
                val mediaItem = player.currentMediaItem
                val episodeProgress = mediaItem?.mediaMetadata?.extras?.getFloat("episode_progress", 0f) ?: 0f
                
                if (episodeProgress > 0) {
                    hasAutoSeeked = true
                    player.seekTo((episodeProgress * 1000f).toLong())
                }
            }
            
            // Mark episode as completed when ended
            if (events.contains(Player.EVENT_PLAYBACK_STATE_CHANGED) && 
                player.playbackState == Player.STATE_ENDED) {
                updateCurrentEpisodeProgress(markCompleted = true)
            }
        }

        override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
            // Reset auto-seek flag when media item changes
            hasAutoSeeked = false
        }

        override fun onPositionDiscontinuity(
            oldPosition: Player.PositionInfo,
            newPosition: Player.PositionInfo,
            reason: Int
        ) {
            // Update progress when user seeks
            if (reason == Player.DISCONTINUITY_REASON_SEEK) {
                updateCurrentEpisodeProgress()
            }
        }
    }

    private fun startProgressJob() {
        progressJob?.cancel()
        progressJob = serviceScope.launch {
            while (isActive && player.isPlaying) {
                updateCurrentEpisodeProgress()
                delay(15000) // Update every 15 seconds
            }
        }
    }

    private fun stopProgressJob() {
        progressJob?.cancel()
    }

    private fun updateCurrentEpisodeProgress(markCompleted: Boolean = false) {
        val mediaItem = player.currentMediaItem
        val episodeId = mediaItem?.mediaId?.toLongOrNull()
        
        if (episodeId != null) {
            val currentPosition = player.currentPosition
            val duration = player.duration

            if (currentPosition >= 0 && duration > 0) {
                val progress = if (markCompleted) {
                    duration.toFloat() / 1000f
                } else {
                    currentPosition.toFloat() / 1000f
                }

                serviceScope.launch(Dispatchers.IO) {
                    try {
                        // Get the current episode from database
                        val episode = podcastRepo.getEpisode(episodeId)
                        episode?.let {
                            val updatedEpisode = it.copy(
                                progress = progress,
                                lastPlayed = System.currentTimeMillis()
                            )
                            podcastRepo.updateEpisode(updatedEpisode)
                        }
                    } catch (e: Exception) {
                        // Handle error silently
                    }
                }
            }
        }
    }

    private inner class LibrarySessionCallback : MediaLibrarySession.Callback {
        
        override fun onGetLibraryRoot(
            session: MediaLibrarySession,
            browser: MediaSession.ControllerInfo,
            params: LibraryParams?
        ): ListenableFuture<LibraryResult<MediaItem>> {
            val rootItem = MediaItem.Builder()
                .setMediaId(ROOT_ID)
                .setMediaMetadata(
                    MediaMetadata.Builder()
                        .setTitle("Recently Played")
                        .setIsBrowsable(true)
                        .setIsPlayable(false)
                        .build()
                )
                .build()

            return Futures.immediateFuture(LibraryResult.ofItem(rootItem, params))
        }

        @OptIn(UnstableApi::class)
        override fun onGetChildren(
            session: MediaLibrarySession,
            browser: MediaSession.ControllerInfo,
            parentId: String,
            page: Int,
            pageSize: Int,
            params: LibraryParams?
        ): ListenableFuture<LibraryResult<ImmutableList<MediaItem>>> {

            if (parentId != ROOT_ID) {
                return Futures.immediateFuture(LibraryResult.ofItemList(emptyList(), params))
            }

            return serviceScope.future {
                val episodes = podcastRepo.getPlayedEpisodes()
                val mediaItems = episodes.map { episode ->
                    MediaItem.Builder()
                        .setMediaId(episode.id.toString())
                        .setUri(episode.audioUrl)
                        .setMediaMetadata(
                            MediaMetadata.Builder()
                                .setTitle(episode.title)
                                .setArtworkUri(episode.imageUrl?.toUri())
                                .setExtras(Bundle().apply {
                                    putFloat("episode_progress", episode.progress)
                                })
                                .setIsBrowsable(false)
                                .setIsPlayable(true)
                                .build()
                        )
                        .build()
                }

                LibraryResult.ofItemList(mediaItems, params)
            }
        }

        override fun onAddMediaItems(
            mediaSession: MediaSession,
            controller: MediaSession.ControllerInfo,
            mediaItems: MutableList<MediaItem>
        ): ListenableFuture<MutableList<MediaItem>> {
            return serviceScope.future {
                val updatedItems = mediaItems.map { mediaItem ->
                    if (mediaItem.localConfiguration?.uri != null) {
                        return@map mediaItem
                    }
                    
                    val episodeId = mediaItem.mediaId.toLongOrNull()
                    if (episodeId != null) {
                        MediaItem.Builder()
                            .setMediaId(episodeId.toString())
                            .setUri(mediaItem.requestMetadata.mediaUri ?: mediaItem.localConfiguration?.uri)
                            .setMediaMetadata(mediaItem.mediaMetadata)
                            .build()
                    } else {
                        mediaItem
                    }
                }.toMutableList()
                
                updatedItems
            }
        }

        @OptIn(UnstableApi::class)
        override fun onConnect(
            session: MediaSession,
            controller: MediaSession.ControllerInfo
        ): MediaSession.ConnectionResult {

            val sessionCommands = MediaSession.ConnectionResult.DEFAULT_SESSION_COMMANDS.buildUpon()
                .add(SessionCommand(CUSTOM_SKIP_BACKWARD, Bundle.EMPTY))
                .add(SessionCommand(CUSTOM_SKIP_FORWARD, Bundle.EMPTY))
                .build()

            return MediaSession.ConnectionResult.AcceptedResultBuilder(session)
                .setAvailableSessionCommands(sessionCommands)
                .build()
        }

        override fun onCustomCommand(
            session: MediaSession,
            controller: MediaSession.ControllerInfo,
            customCommand: SessionCommand,
            args: Bundle
        ): ListenableFuture<SessionResult> {

            when (customCommand.customAction) {
                CUSTOM_SKIP_FORWARD -> {
                    val currentPosition = player.currentPosition
                    val duration = player.duration
                    val newPosition = (currentPosition + 30000).coerceAtMost(duration)
                    player.seekTo(newPosition)
                    return Futures.immediateFuture(SessionResult(SessionResult.RESULT_SUCCESS))
                }
                CUSTOM_SKIP_BACKWARD -> {
                    val currentPosition = player.currentPosition
                    val newPosition = (currentPosition - 30000).coerceAtLeast(0L)
                    player.seekTo(newPosition)
                    return Futures.immediateFuture(SessionResult(SessionResult.RESULT_SUCCESS))
                }
            }

            return Futures.immediateFuture(SessionResult(SessionResult.RESULT_SUCCESS))
        }
    }
}
