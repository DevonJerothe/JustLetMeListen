package com.devonjerothe.justletmelisten.network

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.devonjerothe.justletmelisten.network.doa.EpisodeDao
import com.devonjerothe.justletmelisten.network.doa.PodCastDoa

@Database(
    entities = [
        Podcast::class,
        Episode::class
    ],
    version = 1,
    exportSchema = false
)
abstract class PodcastDatabase : RoomDatabase() {
    abstract fun podcastDao(): PodCastDoa
    abstract fun episodeDao(): EpisodeDao

    companion object {

        @Volatile
        private var INSTANCE: PodcastDatabase? = null

        fun getInstance(context: Context): PodcastDatabase {
            var instance = INSTANCE
            if (instance == null) {
                instance = Room.databaseBuilder(
                    context.applicationContext,
                    PodcastDatabase::class.java,
                    "podcast_database"
                ).build()
                INSTANCE = instance
            }
            return instance
        }
    }
}
