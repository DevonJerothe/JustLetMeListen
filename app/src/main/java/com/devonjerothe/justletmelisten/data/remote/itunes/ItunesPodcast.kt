package com.devonjerothe.justletmelisten.data.remote.itunes

import com.devonjerothe.justletmelisten.domain.models.PodcastSearchModel
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * ****************************************************
 * toppodcasts API
 * ****************************************************
 */
@Serializable
data class ItunesTopPodcastResponse(
    val feed: ItunesFeed
)

@Serializable
data class ItunesFeed(
    val entry: List<ItunesEntry>
)

@Serializable
data class ItunesEntry(
    val id: ItunesId,
    @SerialName("im:name")
    val name: ItunesLabel,
    @SerialName("im:artist")
    val artist: ItunesLabel,
    @SerialName("im:image")
    val image: List<ItunesImageEntry>,
    val summary: ItunesLabel,
    val title: ItunesLabel
)

@Serializable
data class ItunesLabel(
    val label: String
)

@Serializable
data class ItunesImageEntry(
    val label: String,
    val attributes: ImageAttributes
)

@Serializable
data class  ImageAttributes(
    val height: String
)

@Serializable
data class ItunesId(
    val label: String,
    val attributes: IdAttributes
)

@Serializable
data class IdAttributes(
    @SerialName("im:id")
    val id: String
)

/**
 * ****************************************************
 * search and lookup API
 * ****************************************************
 */
@Serializable
data class ItunesSearchResponse(
    val resultCount: Int,
    val results: List<ItunesPodcast>
)

@Serializable
data class ItunesPodcast(
    val wrapperType: String? = null,
    val kind: String? = null,
    val artistId: Long? = null,
    val collectionId: Long,
    val trackId: Long,
    val artistName: String? = null,
    val collectionName: String,
    val trackName: String,
    val collectionCensoredName: String? = null,
    val trackCensoredName: String? = null,
    val artistViewUrl: String? = null,
    val collectionViewUrl: String? = null,
    val feedUrl: String? = null,
    val trackViewUrl: String? = null,
    val artworkUrl30: String? = null,
    val artworkUrl60: String? = null,
    val artworkUrl100: String? = null,
    val collectionPrice: Double? = null,
    val trackPrice: Double? = null,
    val collectionHdPrice: Double? = null,
    val releaseDate: String? = null, // You might want to parse this to a Date object later
    val collectionExplicitness: String? = null,
    val trackExplicitness: String? = null,
    val trackCount: Int? = null,
    val trackTimeMillis: Long? = null,
    val country: String? = null,
    val currency: String? = null,
    val primaryGenreName: String? = null,
    val contentAdvisoryRating: String? = null,
    val artworkUrl600: String? = null,
    val genreIds: List<String>? = null,
    val genres: List<String>? = null
)

/**
 * ****************************************************
 * Data Classes
 * ****************************************************
 */
data class ItunesTopPodcast(
    val trackId: Long,
    val name: String,
    val artist: String,
    val summary: String,
    val imageUrl: String,

    val itunesLookup: String
)

/**
 * ****************************************************
 * Mappers
 * ****************************************************
 */
fun ItunesTopPodcastResponse.toPodcastList(): List<ItunesTopPodcast> {
    return this.feed.entry.map { entry ->
        ItunesTopPodcast(
            trackId = entry.id.attributes.id.toLongOrNull() ?: 0L,
            name = entry.name.label,
            artist = entry.artist.label,
            summary = entry.summary.label,
            imageUrl = entry.image.lastOrNull()?.label ?: "",
            itunesLookup = "https://itunes.apple.com/lookup?id=${entry.id.attributes.id}"
        )
    }
}

fun ItunesTopPodcast.toSearchListItem(): PodcastSearchModel {
    return PodcastSearchModel(
        title = name,
        author = artist,
        imageUrl = imageUrl,
        feedUrl = null,
        trackId = trackId
    )
}

fun ItunesPodcast.toSearchListItem(): PodcastSearchModel {
    return PodcastSearchModel(
        title = collectionName,
        author = artistName ?: "",
        imageUrl = artworkUrl600,
        feedUrl = feedUrl,
        trackId = trackId
    )
}

