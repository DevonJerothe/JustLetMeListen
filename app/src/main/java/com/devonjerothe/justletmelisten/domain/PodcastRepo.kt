package com.devonjerothe.justletmelisten.domain

import com.devonjerothe.justletmelisten.core.parseDuration
import com.devonjerothe.justletmelisten.core.stripHtml
import com.devonjerothe.justletmelisten.core.toDisplayDate
import com.devonjerothe.justletmelisten.data.local.Episode
import com.devonjerothe.justletmelisten.data.local.EpisodeDao
import com.devonjerothe.justletmelisten.data.local.PodCastDoa
import com.devonjerothe.justletmelisten.data.local.Podcast
import com.devonjerothe.justletmelisten.data.local.PodcastWithEpisodes
import com.devonjerothe.justletmelisten.data.remote.ApiResult
import com.devonjerothe.justletmelisten.data.remote.ApiService
import com.devonjerothe.justletmelisten.data.remote.RssResult
import com.devonjerothe.justletmelisten.data.remote.SearchResponse
import kotlinx.coroutines.flow.Flow

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

    suspend fun getEpisode(episodeId: Long): Episode? {
        return episodeDao.getEpisodeById(episodeId)
    }

    suspend fun updateEpisode(episode: Episode) {
        episodeDao.upsertAll(listOf(episode))
    }

    suspend fun getLastPlayedEpisode(): Episode? {
        return episodeDao.getLastPlayedEpisode()
    }

    suspend fun getPlayedEpisodes(): List<Episode> {
        return episodeDao.getPlayedEpisodes()
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

    fun observePodcasts(): Flow<List<Podcast>> {
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
}
