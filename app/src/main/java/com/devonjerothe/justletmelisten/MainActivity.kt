package com.devonjerothe.justletmelisten

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.core.animateDpAsState
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
import androidx.media3.common.util.UnstableApi
import androidx.navigation.compose.rememberNavController
import com.devonjerothe.justletmelisten.core.navigation.NavigationController
import com.devonjerothe.justletmelisten.core.navigation.PodcastNavGraph
import com.devonjerothe.justletmelisten.domain.MediaService
import com.devonjerothe.justletmelisten.presentation.theme.JustLetMeListenTheme
import com.devonjerothe.justletmelisten.presentation.viewmodels.MediaPlayerUIState
import com.devonjerothe.justletmelisten.presentation.viewmodels.MediaPlayerViewModel
import com.devonjerothe.justletmelisten.presentation.views.MediaPlayer
import org.koin.compose.koinInject
import org.koin.core.parameter.parametersOf

class MainActivity : ComponentActivity() {

    @SuppressLint("UnsafeOptInUsageError")
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
    val bottomPadding by animateDpAsState(
        targetValue = if (playerState is MediaPlayerUIState.NoMedia) 16.dp else 150.dp,
        label = "bottomPadding"
    )

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
            PodcastNavGraph(
                navController = navigationController,
                bottomPadding = bottomPadding
            )
        }
    }
}
