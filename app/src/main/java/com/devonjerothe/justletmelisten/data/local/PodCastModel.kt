package com.devonjerothe.justletmelisten.data.local

import androidx.room.ColumnInfo
import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.Relation

data class PodcastWithEpisodes(
    val podcast: Podcast,
    val episodes: List<Episode>
)

// Pod cast model for RSS and Room
@Entity(tableName = "podcasts")
data class Podcast(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    @ColumnInfo(name = "track_id")
    val trackId: Long = 0,

    @ColumnInfo(name = "subscribed")
    val subscribed: Boolean = false,

    @ColumnInfo(name = "etag")
    val etag: String? = null,

    @ColumnInfo("last_modified")
    val lastModified: String? = null,

    @ColumnInfo("link")
    val link: String?,

    @ColumnInfo(name = "title")
    val title: String?,

    @ColumnInfo(name = "description")
    val description: String?,

    @ColumnInfo(name = "image_url")
    val imageUrl: String?,

    @ColumnInfo(name = "feed_url")
    val feedUrl: String?,

    @ColumnInfo(name = "category")
    val category: String?,
)
