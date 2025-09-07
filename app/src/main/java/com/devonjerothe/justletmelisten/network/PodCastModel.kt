package com.devonjerothe.justletmelisten.network

import androidx.room.*

data class PodcastWithEpisodes(
    @Embedded
    val podcast: Podcast,

    @Relation(
        parentColumn = "id",
        entityColumn = "podcast_id"
    )
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

@Entity(
    tableName = "episodes",
    indices = [Index("podcast_id")],
    foreignKeys = [
        ForeignKey(
            entity = Podcast::class,
            parentColumns = ["id"],
            childColumns = ["podcast_id"],
            onDelete = ForeignKey.CASCADE
        )
    ]
)
data class Episode(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    @ColumnInfo(name = "guid")
    val guid: String?,

    @ColumnInfo(name = "podcast_id")
    var podcastId: Long,

    @ColumnInfo(name = "title")
    val title: String?,

    @ColumnInfo(name = "description")
    val description: String?,

    @ColumnInfo("image_url")
    val imageUrl: String?,

    @ColumnInfo(name = "audio_url")
    val audioUrl: String?,

    @ColumnInfo(name = "duration")
    val duration: Long?,

    @ColumnInfo(name = "pub_date")
    val pubDate: String?,
)
