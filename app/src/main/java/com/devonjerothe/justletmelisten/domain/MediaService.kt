package com.devonjerothe.justletmelisten.domain

import android.os.Bundle
import androidx.annotation.OptIn
import androidx.core.net.toUri
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.ForwardingPlayer
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.Player
import androidx.media3.common.Player.COMMAND_SEEK_BACK
import androidx.media3.common.Player.COMMAND_SEEK_FORWARD
import androidx.media3.common.Player.COMMAND_SEEK_TO_NEXT
import androidx.media3.common.Player.COMMAND_SEEK_TO_PREVIOUS
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.SeekParameters
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
import okhttp3.MediaType
import org.koin.android.ext.android.inject

private const val ROOT_ID = "JUSTLETMELISTEN_ROOT"
private const val CUSTOM_SKIP_FORWARD = "skip_forward"
private const val CUSTOM_SKIP_BACKWARD = "skip_backward"

@UnstableApi
class MediaService : MediaLibraryService() {

    private val podcastRepo: PodcastRepo by inject()
    private val serviceScope = CoroutineScope(Dispatchers.Main)

    private lateinit var player: Player
    private lateinit var mediaLibrarySession: MediaLibrarySession
    private var progressJob: Job? = null


    val commandList = listOf(
        CommandButton.Builder(
            CommandButton.ICON_SKIP_BACK_30)
            .setDisplayName("Skip Back 30s")
            .setSessionCommand(SessionCommand(CUSTOM_SKIP_BACKWARD, Bundle.EMPTY))
            .setSlots(CommandButton.SLOT_BACK)
            .build(),
        CommandButton.Builder(
            CommandButton.ICON_SKIP_FORWARD_30)
            .setDisplayName("Skip Forward 30s")
            .setSessionCommand(SessionCommand(CUSTOM_SKIP_FORWARD, Bundle.EMPTY))
            .setSlots(CommandButton.SLOT_FORWARD)
            .setEnabled(true)
            .build()
    )

    val commandListAuto = listOf(
        CommandButton.Builder(
            CommandButton.ICON_SKIP_BACK_30)
            .setDisplayName("Skip Back 30s")
            .setPlayerCommand(COMMAND_SEEK_BACK)
            .setSlots(CommandButton.SLOT_BACK)
            .build(),
        CommandButton.Builder(
            CommandButton.ICON_SKIP_FORWARD_30)
            .setDisplayName("Skip Forward 30s")
            .setPlayerCommand(COMMAND_SEEK_FORWARD)
            .setSlots(CommandButton.SLOT_FORWARD)
            .build()
    )

    @OptIn(UnstableApi::class)
    override fun onCreate() {
        super.onCreate()

        val audioAttributes = AudioAttributes.Builder()
            .setContentType(C.AUDIO_CONTENT_TYPE_SPEECH)
            .setUsage(C.USAGE_MEDIA)
            .build()

        val exoPlayer = ExoPlayer.Builder(this)
            .setAudioAttributes(audioAttributes, true)
            .build()

        player = object: ForwardingPlayer(exoPlayer) {
            override fun getAvailableCommands(): Player.Commands {
                return super.getAvailableCommands().buildUpon()
                    .add(COMMAND_SEEK_FORWARD)
                    .add(COMMAND_SEEK_BACK)
                    .build()
            }

            override fun isCurrentMediaItemSeekable(): Boolean {
                return true
            }

            override fun isCurrentMediaItemLive(): Boolean {
                return false
            }

            override fun isCommandAvailable(command: Int): Boolean {
                if (command == COMMAND_SEEK_FORWARD || command == COMMAND_SEEK_BACK) {
                    return true
                }
                if (command == COMMAND_SEEK_TO_NEXT || command == COMMAND_SEEK_TO_PREVIOUS) {
                    return true
                }

                return super.isCommandAvailable(command)
            }

            override fun seekToNext() {
                if (duration != C.TIME_UNSET) {
                    val newPosition = (currentPosition + 30000L).coerceAtMost(duration)
                    seekTo(newPosition)
                }
            }

            override fun seekToPrevious() {
                val newPosition = (currentPosition - 30000L).coerceAtLeast(0L)
                seekTo(newPosition)
            }
        }

        player.addListener(playerListener)

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
        val episodeId = mediaItem?.mediaId
        
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
                        .setMediaId(episode.guid)
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

            val isAndroidAuto = controller.packageName.contains("android.auto") ||
                    controller.packageName.contains("gearhead")
            if (isAndroidAuto) {
                mediaSession.setMediaButtonPreferences(commandListAuto)
            }

            return serviceScope.future {
                val updatedItems = mediaItems.map { mediaItem ->
                    if (mediaItem.localConfiguration?.uri != null) {
                        return@map mediaItem
                    }
                    
                    val episodeId = mediaItem.mediaId
                    val episode = podcastRepo.getEpisode(episodeId)
                    if (episode == null) {
                        mediaItem
                    }

                    MediaItem.Builder()
                        .setMediaId(episodeId.toString())
                        .setUri(episode?.audioUrl)
                        .setMediaMetadata(
                            MediaMetadata.Builder()
                                .setTitle(episode?.title)
                                .setArtworkUri(episode?.imageUrl?.toUri())
                                .setExtras(Bundle().apply {
                                    putFloat("episode_progress", episode?.progress ?: 0f)
                                })
                                .setIsBrowsable(false)
                                .setIsPlayable(true)
                                .build()
                        )
                        .build()
                }.toMutableList()
                
                updatedItems
            }
        }

        @OptIn(UnstableApi::class)
        override fun onConnect(
            session: MediaSession,
            controller: MediaSession.ControllerInfo
        ): MediaSession.ConnectionResult {

            val isAndroidAuto = controller.packageName.contains("android.auto") ||
                    controller.packageName.contains("gearhead")

            if (!isAndroidAuto) {
                val sessionCommands = MediaSession.ConnectionResult.DEFAULT_SESSION_AND_LIBRARY_COMMANDS.buildUpon()
                    .add(SessionCommand(CUSTOM_SKIP_BACKWARD, Bundle.EMPTY))
                    .add(SessionCommand(CUSTOM_SKIP_FORWARD, Bundle.EMPTY))
                    .build()

                return MediaSession.ConnectionResult.AcceptedResultBuilder(session)
                    .setAvailableSessionCommands(sessionCommands)
                    .build()
            }

            val sessionCommands = MediaSession.ConnectionResult.DEFAULT_SESSION_AND_LIBRARY_COMMANDS.buildUpon()
                .add(SessionCommand(CUSTOM_SKIP_BACKWARD, Bundle.EMPTY))
                .add(SessionCommand(CUSTOM_SKIP_FORWARD, Bundle.EMPTY))
                .build()

            session.setMediaButtonPreferences(controller, commandListAuto)
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
