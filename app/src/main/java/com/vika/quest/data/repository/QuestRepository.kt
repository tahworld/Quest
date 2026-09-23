package com.vika.quest.data.repository

import com.vika.quest.data.local.dao.QuestDao
import com.vika.quest.data.local.entity.QuestEntity
import com.vika.quest.model.NewQuest
import com.vika.quest.model.QuestStatus
import java.util.UUID
import kotlinx.coroutines.flow.Flow

class QuestRepository(
    private val questDao: QuestDao,
) {
    fun observeQuests(): Flow<List<QuestEntity>> = questDao.observeAll()

    fun observeRecentQuests(limit: Int = 20): Flow<List<QuestEntity>> {
        require(limit > 0) { "limit 必须大于 0" }
        return questDao.observeRecent(limit)
    }

    suspend fun getQuest(id: String): QuestEntity? = questDao.getById(id)

    suspend fun getActiveQuest(): QuestEntity? = questDao.getLatestByStatus(QuestStatus.ACTIVE)

    suspend fun createQuest(
        newQuest: NewQuest,
        createdAt: Long = System.currentTimeMillis(),
    ): QuestEntity {
        val quest = QuestEntity(
            id = UUID.randomUUID().toString(),
            goalId = newQuest.goalId,
            skillId = newQuest.skillId,
            chainId = newQuest.chainId,
            title = newQuest.title.trim(),
            instruction = newQuest.instruction.trim(),
            estimatedMinutes = newQuest.estimatedMinutes,
            completionCriteria = newQuest.completionCriteria.map(String::trim),
            difficulty = newQuest.difficulty,
            status = QuestStatus.PENDING,
            createdAt = createdAt,
            completedAt = null,
        )
        questDao.upsert(quest)
        return quest
    }

    suspend fun saveQuest(quest: QuestEntity) = questDao.upsert(quest)
}
