package com.devonjerothe.justletmelisten.core.navigation

import androidx.lifecycle.ViewModel
import androidx.navigation.NavHostController
import com.devonjerothe.justletmelisten.core.navigation.DestinationArgs.FEED_URL
import com.devonjerothe.justletmelisten.core.navigation.DestinationArgs.PODCAST_ID
import com.devonjerothe.justletmelisten.core.navigation.DestinationArgs.TRACK_ID
import com.devonjerothe.justletmelisten.core.navigation.Destinations.HOME
import com.devonjerothe.justletmelisten.core.navigation.Destinations.PODCAST_DETAILS
import com.devonjerothe.justletmelisten.core.navigation.Destinations.SEARCH_PODCASTS
import java.net.URLEncoder
import java.nio.charset.StandardCharsets

object Destinations {
    const val HOME = "home"
    const val SEARCH_PODCASTS = "searchPodcasts"
    const val PODCAST_DETAILS = "podcastDetails"
}

object DestinationArgs {
    const val PODCAST_ID = "podcastId"
    const val FEED_URL = "feedUrl"
    const val TRACK_ID = "trackId"
}

class NavigationController(val navController: NavHostController) : ViewModel() {

    val currentDestination = navController.currentBackStackEntry?.destination?.route ?: HOME

    fun navigateToHome() {
        navController.navigate(HOME)
    }

    fun navigateToSearch() {
        navController.navigate(SEARCH_PODCASTS)
    }

    fun navigateToDetails(podcastId: Long) {
        navController.navigate("$PODCAST_DETAILS?$PODCAST_ID=$podcastId")
    }
    fun navigateToDetails(feedUrl: String?, trackId: Long) {

        var route = "$PODCAST_DETAILS?$TRACK_ID=$trackId"

        feedUrl?.let {
            val encodedUrl = URLEncoder.encode(feedUrl, StandardCharsets.UTF_8.toString())
            route += "&$FEED_URL=$encodedUrl"
        }

        // encode URL
        navController.navigate(route)
    }
}
