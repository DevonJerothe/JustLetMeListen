package com.devonjerothe.justletmelisten.core.di

import androidx.navigation.NavHostController
import com.devonjerothe.justletmelisten.core.AppLifecycleObserver
import com.devonjerothe.justletmelisten.core.navigation.NavigationController
import com.devonjerothe.justletmelisten.data.local.PodcastDatabase
import com.devonjerothe.justletmelisten.data.remote.ApiService
import com.devonjerothe.justletmelisten.domain.PodcastRepo
import com.devonjerothe.justletmelisten.presentation.viewmodels.MediaPlayerViewModel
import com.devonjerothe.justletmelisten.presentation.viewmodels.PodcastDetailsViewModel
import com.devonjerothe.justletmelisten.presentation.viewmodels.PodcastSearchViewModel
import com.devonjerothe.justletmelisten.presentation.viewmodels.PodcastViewModel
import org.koin.android.ext.koin.androidApplication
import org.koin.android.ext.koin.androidContext
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

val serviceContainer = module {

    // Database
    single {
        PodcastDatabase.getInstance(androidContext())
    }

    // DOA
    single {
        val database = get<PodcastDatabase>()
        database.podcastDao()
    }
    single {
        val database = get<PodcastDatabase>()
        database.episodeDao()
    }

    // Network
    single {
        ApiService()
    }

    // Repositories
    single {
        PodcastRepo(
            podcastDao = get(),
            episodeDao = get(),
            apiService = get()
        )
    }

    // Media Player
    single {
        MediaPlayerViewModel(
            androidApplication(),
            podcastRepo = get()
        )
    }

    // Lifecycle Observer for MediaService
    single {
        AppLifecycleObserver()
    }

    // ViewModels
    viewModel {
        PodcastViewModel(
            savedStateHandle = get(),
            podcastRepo = get()
        )
    }
    viewModel {
        PodcastSearchViewModel(
            savedStateHandle = get(),
            podcastRepo = get()
        )
    }
    viewModel {
        PodcastDetailsViewModel(
            savedStateHandle = get(),
            podcastRepo = get(),
            mediaPlayer = get()
        )
    }
    viewModel { (navController: NavHostController) ->
        NavigationController(navController)
    }
}
