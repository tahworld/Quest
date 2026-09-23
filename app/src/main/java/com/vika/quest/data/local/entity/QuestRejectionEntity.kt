package com.vika.quest.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.vika.quest.model.RejectionReason

@Entity(tableName = "quest_rejections", foreignKeys = [ForeignKey(entity = QuestEntity::class, parentColumns = ["id"], childColumns = ["questId"], onDelete = ForeignKey.CASCADE)], indices = [Index(value = ["questId"], unique = true), Index("createdAt")])
data class QuestRejectionEntity(
    @PrimaryKey val id: String,
    val questId: String,
    val reason: RejectionReason,
    val details: String?,
    val createdAt: Long,
)
