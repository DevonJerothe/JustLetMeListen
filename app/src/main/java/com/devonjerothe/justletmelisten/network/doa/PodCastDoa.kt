package com.devonjerothe.justletmelisten.network.doa

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Upsert
import com.devonjerothe.justletmelisten.network.Episode
import com.devonjerothe.justletmelisten.network.Podcast
import com.devonjerothe.justletmelisten.network.PodcastWithEpisodes
import kotlinx.coroutines.flow.Flow

@Dao
interface PodCastDoa {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPodcast(podcast: Podcast): Long

    @Upsert
    suspend fun upsertPodcast(podcast: Podcast)

    @Delete
    suspend fun deletePodcast(podcast: Podcast)

    @Query("SELECT * FROM podcasts ORDER BY title ASC")
    fun observeAllPodcasts(): Flow<List<Podcast>>

    @Query("SELECT * FROM podcasts WHERE id = :id")
    fun observePodcast(id: Long): Flow<Podcast>

    @Query("SELECT * FROM podcasts WHERE id = :id")
    suspend fun getPodcastById(id: Long): Podcast?

    @Query("SELECT * FROM podcasts WHERE track_id = :trackId")
    suspend fun getPodcastByTrackId(trackId: Long): Podcast?

    @Query("SELECT * FROM podcasts WHERE title = :title")
    suspend fun getPodcastByTitle(title: String): Podcast?

    @Transaction
    @Query("SELECT * FROM podcasts WHERE id = :id")
    suspend fun getPodcastWithEpisodes(id: Long): PodcastWithEpisodes?

    @Transaction
    @Query("SELECT * FROM podcasts WHERE id = :id")
    fun observePodcastWithEpisodes(id: Long): Flow<PodcastWithEpisodes>
}

@Dao
interface EpisodeDao {
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertAll(episodes: List<Episode>)

    @Upsert
    suspend fun upsertAll(episodes: List<Episode>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertEpisode(episode: Episode)

    @Delete
    suspend fun deleteEpisode(episode: Episode)

    @Query("SELECT * FROM episodes WHERE last_played IS NOT NULL ORDER BY last_played DESC LIMIT 1")
    suspend fun getLastPlayedEpisode(): Episode?

    @Query("SELECT * FROM episodes WHERE podcast_id = :podcastId ORDER BY pub_date DESC")
    fun observeEpisodesByPodcastId(podcastId: Long): Flow<List<Episode>>

    @Query("SELECT * FROM episodes WHERE podcast_id = :podcastId ORDER BY pub_date DESC")
    suspend fun getEpisodesByPodcastId(podcastId: Long): List<Episode>

    @Query("SELECT * FROM episodes WHERE id = :id")
    suspend fun getEpisodeById(id: Long): Episode?

    @Query("SELECT * FROM episodes WHERE title = :title")
    suspend fun getEpisodeByTitle(title: String): Episode?

    @Query("SELECT * FROM episodes WHERE title = :title AND podcast_id = :podcastId")
    suspend fun getEpisodeByTitleAndPodcastId(title: String, podcastId: Long): Episode?
}
