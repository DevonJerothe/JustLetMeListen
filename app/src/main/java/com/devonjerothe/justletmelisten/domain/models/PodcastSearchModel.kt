package com.devonjerothe.justletmelisten.domain.models

data class PodcastSearchModel(
    val title: String,
    val author: String,
    val imageUrl: String? = null,
    val trackId: Long,
    val feedUrl: String? = null
)
