package com.vika.quest.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.vika.quest.model.ProjectStatus

@Entity(tableName = "projects", foreignKeys = [ForeignKey(entity = GoalEntity::class, parentColumns = ["id"], childColumns = ["goalId"], onDelete = ForeignKey.SET_NULL)], indices = [Index("goalId"), Index("status"), Index("updatedAt")])
data class ProjectEntity(
    @PrimaryKey val id: String,
    val goalId: String?,
    val name: String,
    val description: String,
    val currentState: String,
    val status: ProjectStatus,
    val createdAt: Long,
    val updatedAt: Long,
)
