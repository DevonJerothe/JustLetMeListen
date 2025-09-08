package com.devonjerothe.justletmelisten.views

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.devonjerothe.justletmelisten.NavigationController
import com.devonjerothe.justletmelisten.R
import com.devonjerothe.justletmelisten.network.Episode
import com.devonjerothe.justletmelisten.network.Podcast
import com.devonjerothe.justletmelisten.view_models.PodcastDetailsUIState
import com.devonjerothe.justletmelisten.view_models.PodcastDetailsViewModel
import com.devonjerothe.justletmelisten.views.shared.ExpandingText
import com.devonjerothe.justletmelisten.views.shared.formatTime
import kotlinx.coroutines.flow.map
import org.koin.androidx.compose.koinViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PodcastDetailsScreen(
    navController: NavigationController,
    viewModel: PodcastDetailsViewModel = koinViewModel(),
    bottomPadding: PaddingValues
) {
    val uiState by viewModel.uiState.collectAsState()
    val activeEpisode by viewModel.activeEpisode
        .map { episode ->
            episode?.id
        }
        .collectAsStateWithLifecycle(null)
    val paused by viewModel.paused
        .collectAsStateWithLifecycle(false)

    Scaffold(
        topBar = {
            TopAppBar(
                title = { "" },
                navigationIcon = {
                    IconButton(onClick = { navController.navController.popBackStack() } ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                }
            )
        }
    ) { innerPadding ->
        Box(modifier = Modifier.padding(innerPadding)) {
            when (val state = uiState) {
                is PodcastDetailsUIState.Loading -> {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier.fillMaxSize()
                    ) {
                        CircularProgressIndicator()
                    }
                }
                is PodcastDetailsUIState.Success -> {
                    PodcastDetailsContent(
                        podcast = state.podcast,
                        episodes = state.episodes,
                        onClick = {
                            if (state.podcast.subscribed) {
                                viewModel.unsubscribeToPodcast()
                            } else {
                                viewModel.subscribeToPodcast()
                            }
                        },
                        onPlay = { episode ->
                            viewModel.playEpisode(episode, activeEpisode)
                        },
                        isActive = { episodeId ->
                            activeEpisode == episodeId
                        },
                        paused = paused,
                        bottomPadding = bottomPadding
                    )
                }
                is PodcastDetailsUIState.Error -> {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier.fillMaxSize()
                    ) {
                        Text(text = state.message)
                    }
                }
            }
        }
    }
}

@Composable
fun PodcastDetailsContent(
    podcast: Podcast,
    episodes: List<Episode>,
    onClick: () -> Unit = {},
    onPlay: (Episode) -> Unit = {},
    isActive: (Long) -> Boolean = { false },
    paused: Boolean = false,
    bottomPadding: PaddingValues
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = bottomPadding,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        item {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Spacer(modifier = Modifier.height(16.dp))
                AsyncImage(
                    model = podcast.imageUrl,
                    contentDescription = "Podcast cover art",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.size(200.dp).clip(RoundedCornerShape(12.dp))
                )

                Text(
                    text = podcast.title ?: "",
                    style = MaterialTheme.typography.headlineMedium
                )
                Text(
                    text = podcast.link ?: "",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Button(
                    onClick = onClick,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (podcast.subscribed) MaterialTheme.colorScheme.errorContainer else MaterialTheme.colorScheme.primary
                    )
                ) {
                    Text(
                        if (podcast.subscribed) "Unsubscribe" else "Subscribe",
                        color = if (podcast.subscribed) MaterialTheme.colorScheme.onErrorContainer else MaterialTheme.colorScheme.onPrimary
                    )
                }

                ExpandingText(
                    text = podcast.description ?: "",
                    minLines = 5
                )

                Spacer(modifier = Modifier.height(16.dp))
            }
        }

        item {
            Text(
                text = "Episodes",
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)
            )
        }

        items(episodes) { episode ->
            EpisodeRow(
                episode,
                onPlay = onPlay,
                isActive = isActive,
                paused = paused
            )
            HorizontalDivider()
        }
    }
}

@Composable
fun EpisodeRow(
    episode: Episode,
    onPlay: (Episode) -> Unit,
    isActive: (Long) -> Boolean = { false },
    paused: Boolean = false
) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = episode.title ?: "No Title",
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.SemiBold,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = "${episode.pubDate} - ${formatTime(episode.duration?.toFloat() ?: 0f, true)}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Spacer(modifier = Modifier.width(16.dp))
        IconButton(
            onClick = { onPlay(episode) }
        ) {
            val icon = if (!paused && isActive(episode.id)) R.drawable.pause_24px else R.drawable.play_arrow_24px

            Icon(
                painter = painterResource(icon),
                contentDescription = "Play",
                modifier = Modifier.size(32.dp)
            )
        }
    }
}
