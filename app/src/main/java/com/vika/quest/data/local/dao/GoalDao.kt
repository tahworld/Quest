package com.vika.quest.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Query
import androidx.room.Upsert
import com.vika.quest.data.local.entity.GoalEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface GoalDao {
    @Query("SELECT * FROM goals ORDER BY priority DESC, createdAt ASC")
    fun observeAll(): Flow<List<GoalEntity>>

    @Query("SELECT * FROM goals ORDER BY priority DESC, createdAt ASC LIMIT :limit")
    suspend fun getTop(limit: Int): List<GoalEntity>

    @Query("SELECT * FROM goals WHERE id = :id LIMIT 1")
    suspend fun getById(id: String): GoalEntity?

    @Upsert
    suspend fun upsert(goal: GoalEntity)

    @Delete
    suspend fun delete(goal: GoalEntity)
}
