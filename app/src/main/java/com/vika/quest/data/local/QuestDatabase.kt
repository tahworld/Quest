package com.vika.quest.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.vika.quest.data.local.dao.GoalDao
import com.vika.quest.data.local.dao.QuestDao
import com.vika.quest.data.local.entity.GoalEntity
import com.vika.quest.data.local.entity.QuestEntity

@Database(
    entities = [GoalEntity::class, QuestEntity::class],
    version = 1,
    exportSchema = true,
)
@TypeConverters(RoomConverters::class)
abstract class QuestDatabase : RoomDatabase() {
    abstract fun goalDao(): GoalDao

    abstract fun questDao(): QuestDao

    companion object {
        const val DATABASE_NAME = "quest.db"

        fun create(context: Context): QuestDatabase =
            Room.databaseBuilder(
                context.applicationContext,
                QuestDatabase::class.java,
                DATABASE_NAME,
            ).build()
    }
}
