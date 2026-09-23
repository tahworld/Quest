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
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [
        Index("goalId"),
        Index("createdAt"),
        Index("status"),
    ],
)
data class QuestEntity(
    @PrimaryKey val id: String,
    val goalId: String,
    val skillId: String?,
    val chainId: String?,
    val title: String,
    val instruction: String,
    val estimatedMinutes: Int,
    val completionCriteria: List<String>,
    val difficulty: Int,
    val status: QuestStatus,
    val createdAt: Long,
    val completedAt: Long?,
)
