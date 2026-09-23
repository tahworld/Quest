package com.vika.quest.data.local.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.vika.quest.data.local.entity.UserPreferenceEntity

@Dao
interface UserPreferenceDao {
    @Query("SELECT * FROM user_preferences ORDER BY updatedAt DESC LIMIT :limit") suspend fun getRecent(limit: Int): List<UserPreferenceEntity>
    @Upsert suspend fun upsert(preference: UserPreferenceEntity)
}
