package com.devonjerothe.justletmelisten

import androidx.lifecycle.ViewModel
import androidx.navigation.NavController
import androidx.navigation.NavHostController
import com.devonjerothe.justletmelisten.destinationArgs.FEED_URL
import com.devonjerothe.justletmelisten.destinationArgs.PODCAST_ID
import com.devonjerothe.justletmelisten.destinationArgs.TRACK_ID
import com.devonjerothe.justletmelisten.destinations.HOME
import com.devonjerothe.justletmelisten.destinations.PODCAST_DETAILS
import com.devonjerothe.justletmelisten.destinations.SEARCH_PODCASTS
import java.net.URLEncoder
import java.nio.charset.StandardCharsets

object destinations {
    const val HOME = "home"
    const val SEARCH_PODCASTS = "searchPodcasts"
    const val PODCAST_DETAILS = "podcastDetails"
}

object destinationArgs {
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
    fun navigateToDetails(feedUrl: String, trackId: Long) {
        // encode URL
        val encodedUrl = URLEncoder.encode(feedUrl, StandardCharsets.UTF_8.toString())
        navController.navigate("$PODCAST_DETAILS?$FEED_URL=$encodedUrl?$TRACK_ID=$trackId")
    }
}
