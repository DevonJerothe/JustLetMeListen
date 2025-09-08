package com.devonjerothe.justletmelisten

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.BottomSheetScaffold
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SheetValue
import androidx.compose.material3.Surface
import androidx.compose.material3.rememberBottomSheetScaffoldState
import androidx.compose.material3.rememberStandardBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.devonjerothe.justletmelisten.destinationArgs.FEED_URL
import com.devonjerothe.justletmelisten.destinationArgs.PODCAST_ID
import com.devonjerothe.justletmelisten.destinationArgs.TRACK_ID
import com.devonjerothe.justletmelisten.destinations.HOME
import com.devonjerothe.justletmelisten.destinations.PODCAST_DETAILS
import com.devonjerothe.justletmelisten.destinations.SEARCH_PODCASTS
import com.devonjerothe.justletmelisten.ui.theme.JustLetMeListenTheme
import com.devonjerothe.justletmelisten.view_models.MediaPlayerUIState
import com.devonjerothe.justletmelisten.view_models.MediaPlayerViewModel
import com.devonjerothe.justletmelisten.view_models.MediaService
import com.devonjerothe.justletmelisten.views.HomeScreen
import com.devonjerothe.justletmelisten.views.PodcastDetailsScreen
import com.devonjerothe.justletmelisten.views.SearchScreen
import com.devonjerothe.justletmelisten.views.player.MediaPlayer
import org.koin.compose.koinInject
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf


class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val serviceIntent = Intent(this, MediaService::class.java)
        startService(serviceIntent)

        setContent {
            JustLetMeListenTheme {
                PodcastApp()
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PodcastApp() {
    val navController = rememberNavController()
    val navigationController: NavigationController = koinInject(
        parameters = { parametersOf(navController) }
    )


    val mediaPlayer: MediaPlayerViewModel = koinInject()
    val sheetState = rememberStandardBottomSheetState(
        skipHiddenState = false,
        confirmValueChange = { state ->
            if (state == SheetValue.Hidden) {
                mediaPlayer.closePlayer()
            }
            true
        }
    )
    val scaffoldState = rememberBottomSheetScaffoldState(
        bottomSheetState = sheetState
    )

    val playerState by mediaPlayer.uiState.collectAsState()
    val playerHeight = if (playerState is MediaPlayerUIState.NoMedia) 0.dp else 150.dp

    BottomSheetScaffold(
        scaffoldState = scaffoldState,
        sheetPeekHeight = playerHeight,
        sheetContent = {
            MediaPlayer(
                viewModel = mediaPlayer,
                isExpanded = scaffoldState.bottomSheetState.currentValue == SheetValue.Expanded
            )
        }
    ) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.background
        ) {
            NavHost(
                navController = navController,
                startDestination = navigationController.currentDestination
            ) {
                composable(HOME) {
                    HomeScreen(
                        navController = navigationController,
                        viewModel = koinViewModel()
                    )
                }
                composable(SEARCH_PODCASTS) {
                    SearchScreen(
                        navController = navigationController,
                        viewModel = koinViewModel()
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
                        navController = navigationController,
                        viewModel = koinViewModel()
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
                        navController = navigationController,
                        viewModel = koinViewModel()
                    )
                }
            }
        }
    }
}
