package com.vika.quest.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "user_preferences")
data class UserPreferenceEntity(@PrimaryKey val key: String, val value: String, val updatedAt: Long)
