package com.vika.quest.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.vika.quest.model.QuestStatus

@Entity(
    tableName = "quests",
    foreignKeys = [
        ForeignKey(
            entity = GoalEntity::class,
            parentColumns = ["id"],
            childColumns = ["goalId"],
            onDelete = ForeignKey.SET_NULL,
        ),
        ForeignKey(
            entity = ProjectEntity::class,
            parentColumns = ["id"],
            childColumns = ["projectId"],
            onDelete = ForeignKey.SET_NULL,
        ),
    ],
    indices = [
        Index("goalId"),
        Index("projectId"),
        Index("createdAt"),
        Index("status"),
    ],
)
data class QuestEntity(
    @PrimaryKey val id: String,
    val goalId: String?,
    val projectId: String?,
    val skillId: String?,
    val chainId: String?,
    val title: String,
    val reason: String,
    val steps: List<String>,
    val instruction: String,
    val estimatedMinutes: Int,
    val completionCriteria: List<String>,
    val expectedOutput: String,
    val difficulty: Int,
    val status: QuestStatus,
    val createdAt: Long,
    val completedAt: Long?,
    val sourceIntention: String,
    val sourceAvailableMinutes: Int,
    val sourceEnergy: Int,
    val sourceResources: List<String>,
)
