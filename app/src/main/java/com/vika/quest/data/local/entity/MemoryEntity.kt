package com.vika.quest.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(tableName = "memories", foreignKeys = [
    ForeignKey(entity = GoalEntity::class, parentColumns = ["id"], childColumns = ["goalId"], onDelete = ForeignKey.SET_NULL),
    ForeignKey(entity = ProjectEntity::class, parentColumns = ["id"], childColumns = ["projectId"], onDelete = ForeignKey.SET_NULL),
], indices = [Index("goalId"), Index("projectId"), Index("importance"), Index("createdAt")])
data class MemoryEntity(
    @PrimaryKey val id: String,
    val goalId: String?,
    val projectId: String?,
    val type: String,
    val content: String,
    val importance: Int,
    val createdAt: Long,
)
