package com.vika.quest.data.repository

import com.vika.quest.data.local.dao.UserPreferenceDao
import com.vika.quest.data.local.entity.UserPreferenceEntity

class UserPreferenceRepository(private val dao: UserPreferenceDao) {
    suspend fun getRecent(limit: Int) = dao.getRecent(limit)
    suspend fun save(key: String, value: String) = dao.upsert(UserPreferenceEntity(key, value, System.currentTimeMillis()))
}
