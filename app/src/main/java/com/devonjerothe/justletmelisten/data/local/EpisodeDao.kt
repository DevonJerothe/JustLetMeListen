package com.devonjerothe.justletmelisten.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

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

    @Query("SELECT * FROM episodes WHERE last_played IS NOT NULL ORDER BY last_played DESC")
    suspend fun getPlayedEpisodes() : List<Episode>

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
