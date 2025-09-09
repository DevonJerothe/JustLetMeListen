package com.devonjerothe.justletmelisten.data.local

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "episodes",
    indices = [
        Index("last_played"),
        Index("podcast_id")],
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

    @PrimaryKey
    @ColumnInfo(name = "guid")
    val guid: String,

    @ColumnInfo(name = "podcast_id")
    var podcastId: Long,

    @ColumnInfo(name = "last_played")
    var lastPlayed: Long? = null,

    @ColumnInfo(name = "progress")
    var progress: Float = 0f,

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
