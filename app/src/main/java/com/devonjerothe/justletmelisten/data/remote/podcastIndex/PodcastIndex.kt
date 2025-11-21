package com.devonjerothe.justletmelisten.data.remote.podcastIndex

import kotlinx.serialization.Serializable

@Serializable
data class PodcastIndexTrendingList(
    val status: String,
    val count: Int,
    val max: Int? = null,
    val since: Int? = null,
    val description: String,
    val feeds: List<PodcastIndexTrendingItem>
)

@Serializable
data class PodcastIndexTrendingItem(
    val id: Int,
    val url: String,
    val title: String,
    val description: String,
    val author: String,
    val image: String,
    val artwork: String,
    val newestItemPublishTime: Int,
    val itunesId: Int? = null,
    val trendScore: Int,
    val language: String
)
