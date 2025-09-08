package com.devonjerothe.justletmelisten.repos

import androidx.core.text.HtmlCompat
import com.devonjerothe.justletmelisten.network.ApiResult
import com.devonjerothe.justletmelisten.network.ApiService
import com.devonjerothe.justletmelisten.network.Episode
import com.devonjerothe.justletmelisten.network.Podcast
import com.devonjerothe.justletmelisten.network.PodcastWithEpisodes
import com.devonjerothe.justletmelisten.network.RssResult
import com.devonjerothe.justletmelisten.network.SearchResponse
import com.devonjerothe.justletmelisten.network.doa.EpisodeDao
import com.devonjerothe.justletmelisten.network.doa.PodCastDoa
import com.prof18.rssparser.model.RssChannel
import kotlinx.coroutines.flow.Flow
import java.text.SimpleDateFormat
import java.util.*

class PodcastRepo(
    private val podcastDao: PodCastDoa,
    private val episodeDao: EpisodeDao,
    private val apiService: ApiService
) {
    suspend fun searchPodcasts(query: String): ApiResult<SearchResponse> {
        // TODO: Handle DB Search
        val response = apiService.searchPodcasts(query)
        return response
    }

    suspend fun getPodcast(podcastId: Long): Podcast? {
        return podcastDao.getPodcastById(podcastId)
    }

    suspend fun updateEpisode(episode: Episode) {
        episodeDao.upsertAll(listOf(episode))
    }

    suspend fun getLastPlayedEpisode(): Episode? {
        return episodeDao.getLastPlayedEpisode()
    }

    suspend fun getPodcastByTrack(trackId: Long): Podcast? {
        return podcastDao.getPodcastByTrackId(trackId)
    }

    suspend fun followPodcast(podcast: PodcastWithEpisodes) {

        val subbedPodcast = podcast.podcast.copy(
            subscribed = true
        )

        // we need to save the podcast first so we know what ID to use for the episodes
        val podcastId = podcastDao.insertPodcast(subbedPodcast)

        // update the episodes with the podcast ID
        podcast.episodes.forEach { episode ->
            episode.podcastId = podcastId
        }

        episodeDao.insertAll(podcast.episodes)
    }

    suspend fun unfollowPodcast(podcast: Podcast) {
        podcastDao.deletePodcast(podcast)
    }

    suspend fun observePodcasts(): Flow<List<Podcast>> {
        return podcastDao.observeAllPodcasts()
    }

    suspend fun observePodcastFeed(
        podcastId: Long,
    ): Flow<PodcastWithEpisodes> {
        val podcast = podcastDao.getPodcastById(podcastId)

        val feedUrl = podcast?.feedUrl ?: return podcastDao.observePodcastWithEpisodes(podcastId)
        val response = apiService.getPodcastEpisodes(feedUrl, podcast.etag, podcast.lastModified)

        when (response) {
            is RssResult.Success -> {
                val episodes = response.channel.items

                // store or update the podcast item with new etag
                // this is needed for proper caching
                val updatePodcast = podcast.copy(
                    etag = response.etag,
                    lastModified = response.lastModified
                )

                // map each item to Episode
                val mappedEpisodes = episodes.map { item ->
                    Episode(
                        podcastId = updatePodcast.id,
                        guid = item.guid,
                        title = item.title,
                        description = item.description,
                        imageUrl = item.image,
                        audioUrl = item.rawEnclosure?.url,
                        duration = item.rawEnclosure?.length,
                        pubDate = toDisplayDate(item.pubDate ?: "")
                    )
                }

                // store items in DB
                podcastDao.upsertPodcast(updatePodcast)
                episodeDao.upsertAll(mappedEpisodes)

                return podcastDao.observePodcastWithEpisodes(podcastId)
            }
            is RssResult.NotModified -> {
                // pull episode data from db
                return podcastDao.observePodcastWithEpisodes(podcastId)
            }
            is RssResult.Error -> {
                // TODO: Handle error
                return podcastDao.observePodcastWithEpisodes(podcastId)
            }
        }
    }

    suspend fun getPodcastFeed(feedUrl: String, trackId: Long): PodcastWithEpisodes? {
        val response = apiService.getPodcastEpisodes(feedUrl)

        when (response) {
            is RssResult.Success -> {
                val episodes = response.channel.items

                val image = response.channel.image?.url ?: response.channel.itunesChannelData?.image
                val podcast = Podcast(
                    trackId = trackId,
                    etag = response.etag,
                    lastModified = response.lastModified,
                    link = response.channel.link,
                    title = response.channel.title,
                    description = response.channel.description,
                    imageUrl = image,
                    feedUrl = feedUrl,
                    category = response.channel.itunesChannelData?.categories?.firstOrNull()
                )

                val mappedEpisodes = episodes.map { item ->
                    val image = item.itunesItemData?.image ?: response.channel.image?.url

                    Episode(
                        guid = item.guid,
                        podcastId = podcast.id,
                        title = item.title,
                        description = stripHtml(item.description),
                        imageUrl = image,
                        audioUrl = item.rawEnclosure?.url,
                        duration = parseDuration(item.itunesItemData?.duration),
                        pubDate = toDisplayDate(item.pubDate ?: "")
                    )
                }

                val podcastWithEpisodes = PodcastWithEpisodes(
                    podcast = podcast,
                    episodes = mappedEpisodes
                )
                return podcastWithEpisodes
            }
            is RssResult.NotModified -> {
                return null
            }
            is RssResult.Error -> {
                // TODO: Handle error
                return null
            }
        }
    }

    // Helper Functions
    private fun parseDuration(duration: String?): Long {
        if (duration.isNullOrBlank()) return 0L

        val asSeconds = duration.toLongOrNull()
        if (asSeconds != null) return asSeconds

        try {
            val parts = duration.split(":").map { it.toLong() }.reversed()
            var totalSeconds = 0L
            if (parts.isNotEmpty()) totalSeconds += parts[0]
            if (parts.size > 1) totalSeconds += parts[1] * 60
            if (parts.size > 2) totalSeconds += parts[2] * 3600
            return totalSeconds
        } catch (e: Exception) {
            return 0L
        }
    }

    fun stripHtml(html: String?): String {
        if (html.isNullOrBlank()) return ""
        val spanned = HtmlCompat.fromHtml(html, HtmlCompat.FROM_HTML_MODE_LEGACY)
        return spanned.toString()
    }

    private fun parseDate(dateString: String): Long? {
        return try {
            // Example format: Mon, 01 Sep 2025 14:45:00 +0000
            val format = SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss Z", Locale.ENGLISH)
            format.parse(dateString)?.time ?: System.currentTimeMillis()
        } catch (e: Exception) {
            System.currentTimeMillis()
        }
    }

    private fun toDisplayDate(dateString: String): String? {
        val mili = parseDate(dateString)
        return mili?.let {
            try {
                val format = SimpleDateFormat("MMMM d, yyyy", Locale.ENGLISH)
                format.format(Date(it))
            } catch (e: Exception) {
                null
            }
        }
    }
}
