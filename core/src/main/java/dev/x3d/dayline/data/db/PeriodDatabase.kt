package dev.x3d.dayline.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [LessonEntity::class, TimegridEntity::class, MetaEntity::class],
    version = 1,
    exportSchema = false,
)
abstract class PeriodDatabase : RoomDatabase() {
    abstract fun lessonDao(): LessonDao
    abstract fun timegridDao(): TimegridDao
    abstract fun metaDao(): MetaDao

    companion object {
        fun create(context: Context): PeriodDatabase =
            Room.databaseBuilder(context, PeriodDatabase::class.java, "dayline.db")
                .fallbackToDestructiveMigration(dropAllTables = true)
                .build()
    }
}
