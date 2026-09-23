package com.vika.quest.data.repository

import com.vika.quest.data.local.dao.MemoryDao

class MemoryRepository(private val dao: MemoryDao) {
    suspend fun getImportant(limit: Int) = dao.getImportant(limit)
}
