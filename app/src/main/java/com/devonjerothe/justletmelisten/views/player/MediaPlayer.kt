package com.devonjerothe.justletmelisten.views.player

import androidx.compose.animation.Crossfade
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.devonjerothe.justletmelisten.network.Episode
import com.devonjerothe.justletmelisten.view_models.MediaPlayerUIState
import com.devonjerothe.justletmelisten.view_models.MediaPlayerViewModel
import com.devonjerothe.justletmelisten.R
import com.devonjerothe.justletmelisten.views.shared.formatTime
import ir.mahozad.multiplatform.wavyslider.WaveDirection
import ir.mahozad.multiplatform.wavyslider.material3.WavySlider

@Composable
fun MediaPlayer(
    viewModel: MediaPlayerViewModel,
    isExpanded: Boolean
) {
    val uiState by viewModel.uiState.collectAsState()
    var isDragging by remember { mutableStateOf(false) }

    if (uiState is MediaPlayerUIState.HasMedia) {
        val state = uiState as MediaPlayerUIState.HasMedia
        var sliderPosition by remember { mutableStateOf(state.progress) }

        LaunchedEffect(state.progress) {
            if (!isDragging) {
                sliderPosition = state.progress
            }
        }

        Crossfade(targetState = isExpanded, label = "MediaPlayer") { expanded ->
            if (expanded) {
                MediaPlayerView(
                    episode = state.currentEpisode,
                    progress = sliderPosition,
                    duration = state.duration,
                    paused = state.paused,
                    onSeek = { newPos ->
                        viewModel.seakTo(newPos)
                    },
                    onPlayPause = viewModel::onPlayPause,
                    onValueChange = { newPos ->
                        isDragging = true
                        sliderPosition = newPos
                    },
                    onValueChangeFinished = {
                        viewModel.seakTo(sliderPosition)
                        isDragging = false
                    }
                )
            } else {
                MediaPlayerViewCollapsed(
                    episode = state.currentEpisode,
                    progress = state.progress,
                    duration = state.duration, // Added duration here
                    paused = state.paused,
                    onSeek = { newPos ->
                        viewModel.seakTo(newPos)
                    },
                    onPlayPause = viewModel::onPlayPause
                )
            }
        }
    }
}

@Composable
fun MediaPlayerView(
    episode: Episode,
    progress: Float,
    duration: Float,
    paused: Boolean = false,
    onSeek: (Float) -> Unit = {},
    onPlayPause: () -> Unit = {},
    onValueChange: (Float) -> Unit = {},
    onValueChangeFinished: () -> Unit = {}
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp, vertical = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(16.dp))

        AsyncImage(
            model = episode.imageUrl,
            contentDescription = "Episode Image",
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1f)
                .clip(RoundedCornerShape(12.dp)),
            contentScale = ContentScale.Crop
        )

        Spacer(modifier = Modifier.height(32.dp))

        Text(
            text = episode.title ?: "",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = episode.description ?: "",
            style = MaterialTheme.typography.bodyMedium,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )

        Spacer(modifier = Modifier.weight(1f))

        // Media Controls
        WavySlider(
            value = progress,
            valueRange = 0f..duration,
            onValueChange = onValueChange,
            onValueChangeFinished = onValueChangeFinished,
            modifier = Modifier.fillMaxWidth(),
            waveLength = 48.dp,
            waveHeight = 12.dp,
            waveVelocity = 16.dp to WaveDirection.TAIL,
            waveThickness = 5.dp,
            trackThickness = 10.dp,
            incremental = true
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = formatTime(progress),
                style = MaterialTheme.typography.bodySmall
            )
            Text(
                text = formatTime(duration),
                style = MaterialTheme.typography.bodySmall
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // rewind
            IconButton(onClick = {
                val newPos = progress - 30f
                onSeek(newPos.coerceAtLeast(0f))
            }) {
                Icon(
                    painter = painterResource(id = R.drawable.replay_30_24px),
                    contentDescription = "Play/Pause",
                    modifier = Modifier.size(32.dp)
                )
            }

            Button(
                onClick = onPlayPause,
                modifier = Modifier.size(64.dp),
                shape = CircleShape,
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
            ) {
                Icon(
                    painter = painterResource(id = if (paused) R.drawable.play_arrow_24px else R.drawable.pause_24px),
                    contentDescription = "Play/Pause",
                    modifier = Modifier.size(64.dp)
                )
            }

            IconButton(onClick = {
                val newPos = progress + 30f
                onSeek(newPos.coerceAtMost(duration))
            }) {
                Icon(
                    painter = painterResource(id = R.drawable.forward_30_24px),
                    contentDescription = "Play/Pause",
                    modifier = Modifier.size(32.dp)
                )
            }
        }

        Spacer(modifier = Modifier.weight(1f))
    }
}

@Composable
fun MediaPlayerViewCollapsed(
    episode: Episode,
    progress: Float,
    duration: Float, // Added duration parameter
    paused: Boolean = false,
    onSeek: (Float) -> Unit = {},
    onPlayPause: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth()
            .padding(horizontal = 24.dp)
            .padding(bottom = 24.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        AsyncImage(
            model = episode.imageUrl,
            contentDescription = "Episode Image",
            modifier = Modifier
                .size(80.dp)
                .clip(RoundedCornerShape(8.dp)),
            contentScale = ContentScale.Crop
        )
        Spacer(modifier = Modifier.width(16.dp))
        Column(
            modifier = Modifier.weight(1f),
            horizontalAlignment = Alignment.Start
        ) {
            Text(
                text = episode.title ?: "",
                style = MaterialTheme.typography.bodyMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(8.dp))

            // Media Controls
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // rewind
                IconButton(onClick = {
                    val newPos = progress - 30f
                    onSeek(newPos.coerceAtLeast(0f))
                }) {
                    Icon(
                        painter = painterResource(id = R.drawable.replay_30_24px),
                        contentDescription = "Play/Pause",
                        modifier = Modifier.size(32.dp)
                    )
                }

                IconButton(
                    onClick = onPlayPause,
                ) {
                    Icon(
                        painter = painterResource(id = if (paused) R.drawable.play_arrow_24px else R.drawable.pause_24px),
                        contentDescription = "Play/Pause",
                        modifier = Modifier.size(124.dp)
                    )
                }

                // rewind
                IconButton(onClick = {
                    val newPos = progress + 30f
                    onSeek(newPos.coerceAtMost(duration)) // Changed progress to duration
                }) {
                    Icon(
                        painter = painterResource(id = R.drawable.forward_30_24px),
                        contentDescription = "Play/Pause",
                        modifier = Modifier.size(32.dp)
                    )
                }
            }
        }

    }
}
