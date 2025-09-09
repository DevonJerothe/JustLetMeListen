package com.devonjerothe.justletmelisten.core.navigation

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.devonjerothe.justletmelisten.core.navigation.DestinationArgs.FEED_URL
import com.devonjerothe.justletmelisten.core.navigation.DestinationArgs.PODCAST_ID
import com.devonjerothe.justletmelisten.core.navigation.DestinationArgs.TRACK_ID
import com.devonjerothe.justletmelisten.core.navigation.Destinations.HOME
import com.devonjerothe.justletmelisten.core.navigation.Destinations.PODCAST_DETAILS
import com.devonjerothe.justletmelisten.core.navigation.Destinations.SEARCH_PODCASTS
import com.devonjerothe.justletmelisten.presentation.views.HomeScreen
import com.devonjerothe.justletmelisten.presentation.views.PodcastDetailsScreen
import com.devonjerothe.justletmelisten.presentation.views.SearchScreen
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun PodcastNavGraph(
    navController: NavigationController,
    bottomPadding: Dp
) {
    NavHost(
        navController = navController.navController,
        startDestination = navController.currentDestination
    ) {
        composable(HOME) {
            HomeScreen(
                navController = navController,
                viewModel = koinViewModel(),
                bottomPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = bottomPadding)
            )
        }
        composable(SEARCH_PODCASTS) {
            SearchScreen(
                navController = navController,
                viewModel = koinViewModel(),
                bottomPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = bottomPadding)
            )
        }
        composable(
            "${PODCAST_DETAILS}?${PODCAST_ID}={${PODCAST_ID}}",
            arguments = listOf(
                navArgument(PODCAST_ID) {
                    type = NavType.LongType
                    nullable = false
                    defaultValue = 0L
                }
            )
        ) {
            PodcastDetailsScreen(
                navController = navController,
                viewModel = koinViewModel(),
                bottomPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = bottomPadding)
            )
        }
        composable(
            "${PODCAST_DETAILS}?${FEED_URL}={${FEED_URL}}?${TRACK_ID}={${TRACK_ID}}",
            arguments = listOf(
                navArgument(FEED_URL) {
                    type = NavType.StringType
                    nullable = false
                    defaultValue = ""
                },
                navArgument(TRACK_ID) {
                    type = NavType.LongType
                    nullable = false
                    defaultValue = 0L
                }
            )
        ) {
            PodcastDetailsScreen(
                navController = navController,
                viewModel = koinViewModel(),
                bottomPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = bottomPadding)
            )
        }
    }
}
