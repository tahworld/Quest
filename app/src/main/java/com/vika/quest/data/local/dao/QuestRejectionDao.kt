package com.vika.quest.data.local.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.vika.quest.data.local.entity.QuestRejectionEntity

@Dao
interface QuestRejectionDao {
    @Query("SELECT * FROM quest_rejections ORDER BY createdAt DESC LIMIT :limit") suspend fun getRecent(limit: Int): List<QuestRejectionEntity>
    @Upsert suspend fun upsert(rejection: QuestRejectionEntity)
}
