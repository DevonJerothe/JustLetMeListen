package com.devonjerothe.justletmelisten.presentation.views

import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.devonjerothe.justletmelisten.core.navigation.NavigationController
import com.devonjerothe.justletmelisten.domain.models.PodcastSearchModel
import com.devonjerothe.justletmelisten.presentation.viewmodels.PodcastSearchUiState
import com.devonjerothe.justletmelisten.presentation.viewmodels.PodcastSearchViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchScreen(
    navController: NavigationController,
    viewModel: PodcastSearchViewModel,
    bottomPadding: PaddingValues
) {
    val uiState by viewModel.uiState.collectAsState()
    var searchQuery by remember { mutableStateOf("") }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Search Podcasts") },
                navigationIcon = {
                    IconButton(onClick = { navController.navController.popBackStack() }) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(modifier = Modifier.padding(innerPadding)) {
            SearchBar(
                query = searchQuery,
                onQueryChange = { searchQuery = it },
                onSearch = { viewModel.searchPodcasts(searchQuery) },
                onClear = {
                    searchQuery = ""
                    viewModel.clearSearch()
                }
            )

            when (val state = uiState) {
                is PodcastSearchUiState.Popular -> {
                    if (state.isLoading) {
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier.fillMaxSize()
                        ) { CircularProgressIndicator() }
                    } else if (state.error != null) {
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier.fillMaxSize()
                        ) { Text(text = state.error) }
                    } else {
                        ResultListView(
                            podcasts = state.items,
                            listTitle = "Popular",
                            contentPadding = bottomPadding,
                            onPodcastClick = { podcast ->
                                navController.navigateToDetails(
                                    feedUrl = podcast.feedUrl,
                                    trackId = podcast.trackId
                                )
                            }
                        )
                    }
                }

                is PodcastSearchUiState.SearchResults -> {
                    if (state.isLoading) {
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier.fillMaxSize()
                        ) { CircularProgressIndicator() }
                    } else if (state.error != null) {
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier.fillMaxSize()
                        ) { Text(text = state.error) }
                    } else {
                        ResultListView(
                            podcasts = state.items,
                            listTitle = "Search Results",
                            contentPadding = bottomPadding,
                            onPodcastClick = { podcast ->
                                navController.navigateToDetails(
                                    feedUrl = podcast.feedUrl,
                                    trackId = podcast.trackId
                                )
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun ResultListView(
    podcasts: List<PodcastSearchModel>,
    listTitle: String,
    contentPadding: PaddingValues,
    onPodcastClick: (PodcastSearchModel) -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = contentPadding,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Text(
                text = listTitle,
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(horizontal = 16.dp)
            )
        }
        items(podcasts) { podcast ->
            PodcastItem(
                podcast = podcast,
                onTap = { onPodcastClick(podcast) }
            )
        }
    }
}

@Composable
fun SearchBar(
    query: String,
    onQueryChange: (String) -> Unit,
    onSearch: () -> Unit,
    onClear: () -> Unit
) {
    OutlinedTextField(
        value = query,
        onValueChange = onQueryChange,
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        placeholder = { Text("Search...") },
        leadingIcon = {
            Icon(imageVector = Icons.Default.Search, contentDescription = "Search")
        },
        trailingIcon = {
            if (query.isNotEmpty()) {
                IconButton(onClick = onClear) {
                    Icon(imageVector = Icons.Default.Clear, contentDescription = "Clear")
                }
            }
        },
        singleLine = true,
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
        keyboardActions = KeyboardActions(onSearch = { onSearch() })
    )
}

@Composable
fun PodcastItem(podcast: PodcastSearchModel, onTap: () -> Unit) {
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .clickable(onClick = onTap)
                .padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        AsyncImage(
            model = podcast.imageUrl,
            contentDescription = "Podcast cover art",
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .size(80.dp)
                .clip(RoundedCornerShape(8.dp))
        )
        Spacer(modifier = Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = podcast.title,
                style = MaterialTheme.typography.titleMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = podcast.author,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}
