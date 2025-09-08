package com.devonjerothe.justletmelisten.di

import androidx.navigation.NavHostController
import com.devonjerothe.justletmelisten.NavigationController
import com.devonjerothe.justletmelisten.network.ApiService
import com.devonjerothe.justletmelisten.network.PodcastDatabase
import com.devonjerothe.justletmelisten.repos.PodcastRepo
import com.devonjerothe.justletmelisten.view_models.MediaPlayerViewModel
import com.devonjerothe.justletmelisten.view_models.PodcastDetailsViewModel
import com.devonjerothe.justletmelisten.view_models.PodcastSearchViewModel
import com.devonjerothe.justletmelisten.view_models.PodcastViewModel
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
