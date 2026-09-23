package com.vika.quest.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.vika.quest.model.DifficultyRating

@Entity(tableName = "quest_results", foreignKeys = [ForeignKey(entity = QuestEntity::class, parentColumns = ["id"], childColumns = ["questId"], onDelete = ForeignKey.CASCADE)], indices = [Index(value = ["questId"], unique = true), Index("createdAt")])
data class QuestResultEntity(
    @PrimaryKey val id: String,
    val questId: String,
    val resultText: String,
    val actualMinutes: Int?,
    val difficultyRating: DifficultyRating?,
    val usefulnessRating: Int?,
    val createdAt: Long,
    val analysisSummary: String?,
    val evidence: List<String>,
    val insights: List<String>,
    val projectProgress: String?,
    val suggestedNextStep: String?,
)
