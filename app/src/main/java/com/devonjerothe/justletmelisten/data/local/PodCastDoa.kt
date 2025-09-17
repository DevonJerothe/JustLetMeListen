package com.devonjerothe.justletmelisten.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Upsert
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
}
