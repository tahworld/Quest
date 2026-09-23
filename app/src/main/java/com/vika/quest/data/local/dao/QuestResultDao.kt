package com.vika.quest.data.local.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.vika.quest.data.local.entity.QuestResultEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface QuestResultDao {
    @Query("SELECT * FROM quest_results WHERE questId = :questId LIMIT 1") fun observeForQuest(questId: String): Flow<QuestResultEntity?>
    @Query("SELECT * FROM quest_results WHERE questId = :questId LIMIT 1") suspend fun getForQuest(questId: String): QuestResultEntity?
    @Query("SELECT * FROM quest_results WHERE questId IN (:questIds)") suspend fun getForQuests(questIds: List<String>): List<QuestResultEntity>
    @Upsert suspend fun upsert(result: QuestResultEntity)
}
