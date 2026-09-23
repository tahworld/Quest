package com.vika.quest.data.local.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.vika.quest.data.local.entity.ProjectEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ProjectDao {
    @Query("SELECT * FROM projects ORDER BY updatedAt DESC") fun observeAll(): Flow<List<ProjectEntity>>
    @Query("SELECT * FROM projects WHERE status = 'ACTIVE' ORDER BY updatedAt DESC LIMIT :limit") suspend fun getActive(limit: Int): List<ProjectEntity>
    @Query("SELECT * FROM projects WHERE id = :id LIMIT 1") suspend fun getById(id: String): ProjectEntity?
    @Upsert suspend fun upsert(project: ProjectEntity)
}
