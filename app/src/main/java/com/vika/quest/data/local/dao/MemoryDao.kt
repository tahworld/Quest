package com.vika.quest.data.local.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.vika.quest.data.local.entity.MemoryEntity

@Dao
interface MemoryDao {
    @Query("SELECT * FROM memories ORDER BY importance DESC, createdAt DESC LIMIT :limit") suspend fun getImportant(limit: Int): List<MemoryEntity>
    @Query("SELECT * FROM memories WHERE content = :content LIMIT 1") suspend fun findByContent(content: String): MemoryEntity?
    @Upsert suspend fun upsert(memory: MemoryEntity)
}
