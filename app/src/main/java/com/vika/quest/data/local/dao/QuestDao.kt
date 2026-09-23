package com.vika.quest.data.local.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Update
import androidx.room.Upsert
import com.vika.quest.data.local.entity.QuestEntity
import com.vika.quest.model.QuestStatus
import kotlinx.coroutines.flow.Flow

@Dao
interface QuestDao {
    @Query("SELECT * FROM quests ORDER BY createdAt DESC")
    fun observeAll(): Flow<List<QuestEntity>>

    @Query("SELECT * FROM quests ORDER BY createdAt DESC LIMIT :limit")
    fun observeRecent(limit: Int): Flow<List<QuestEntity>>

    @Query("SELECT * FROM quests WHERE id = :id LIMIT 1")
    suspend fun getById(id: String): QuestEntity?

    @Query("SELECT * FROM quests WHERE status = :status ORDER BY createdAt DESC LIMIT 1")
    suspend fun getLatestByStatus(status: QuestStatus): QuestEntity?

    @Upsert
    suspend fun upsert(quest: QuestEntity)

    @Update
    suspend fun update(quest: QuestEntity)
}
