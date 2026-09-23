package com.vika.quest.data.repository

import androidx.room.withTransaction
import com.vika.quest.ai.AiQuestResultAnalysis
import com.vika.quest.data.local.QuestDatabase
import com.vika.quest.data.local.entity.MemoryEntity
import com.vika.quest.data.local.entity.QuestEntity
import com.vika.quest.data.local.entity.QuestRejectionEntity
import com.vika.quest.data.local.entity.QuestResultEntity
import com.vika.quest.model.DifficultyRating
import com.vika.quest.model.NewQuest
import com.vika.quest.model.QuestStatus
import com.vika.quest.model.RejectionReason
import java.util.UUID
import kotlinx.coroutines.flow.Flow

data class NewQuestResult(val resultText: String, val actualMinutes: Int?, val difficultyRating: DifficultyRating?, val usefulnessRating: Int?)

class QuestRepository(private val database: QuestDatabase) {
    private val questDao = database.questDao()
    private val resultDao = database.questResultDao()
    private val rejectionDao = database.questRejectionDao()

    fun observeQuests(): Flow<List<QuestEntity>> = questDao.observeAll()
    fun observeRecentQuests(limit: Int = 20): Flow<List<QuestEntity>> = questDao.observeRecent(limit.also { require(it > 0) })
    fun observeResult(questId: String): Flow<QuestResultEntity?> = resultDao.observeForQuest(questId)
    suspend fun getQuest(id: String) = questDao.getById(id)
    suspend fun getResult(questId: String) = resultDao.getForQuest(questId)
    suspend fun getRecentQuests(limit: Int) = questDao.getRecent(limit)
    suspend fun getResults(questIds: List<String>) = if (questIds.isEmpty()) emptyList() else resultDao.getForQuests(questIds)
    suspend fun getRecentRejections(limit: Int) = rejectionDao.getRecent(limit)
    suspend fun getActiveQuest() = questDao.getLatestByStatus(QuestStatus.ACTIVE)

    suspend fun createQuest(newQuest: NewQuest, createdAt: Long = System.currentTimeMillis()): QuestEntity =
        newQuest.toEntity(createdAt).also { questDao.upsert(it) }

    suspend fun saveQuest(quest: QuestEntity) = questDao.upsert(quest)

    suspend fun startQuest(id: String) = database.withTransaction {
        val quest = checkNotNull(questDao.getById(id)) { "任务不存在" }
        check(quest.status == QuestStatus.PENDING) { "只有待开始任务可以启动" }
        questDao.update(quest.copy(status = QuestStatus.ACTIVE))
    }

    suspend fun abandonQuest(id: String) = database.withTransaction {
        val quest = checkNotNull(questDao.getById(id)) { "任务不存在" }
        check(quest.status == QuestStatus.ACTIVE) { "只有进行中的任务可以放弃" }
        questDao.update(quest.copy(status = QuestStatus.ABANDONED))
    }

    suspend fun reroll(rejectedQuestId: String, reason: RejectionReason, details: String?, replacement: NewQuest, now: Long = System.currentTimeMillis()): QuestEntity = database.withTransaction {
        val rejected = checkNotNull(questDao.getById(rejectedQuestId)) { "原任务不存在" }
        check(rejected.status == QuestStatus.PENDING) { "只能更换尚未开始的任务" }
        rejectionDao.upsert(QuestRejectionEntity(UUID.randomUUID().toString(), rejected.id, reason, details?.trim()?.takeIf(String::isNotEmpty), now))
        questDao.update(rejected.copy(status = QuestStatus.ABANDONED))
        replacement.toEntity(now).also { questDao.upsert(it) }
    }

    suspend fun completeQuest(id: String, input: NewQuestResult, now: Long = System.currentTimeMillis()): QuestResultEntity = database.withTransaction {
        val quest = checkNotNull(questDao.getById(id)) { "任务不存在" }
        check(quest.status == QuestStatus.ACTIVE) { "只有进行中的任务可以完成" }
        require(input.resultText.isNotBlank()) { "请填写你产出或发现了什么" }
        require(input.actualMinutes == null || input.actualMinutes > 0) { "实际用时必须大于 0" }
        require(input.usefulnessRating == null || input.usefulnessRating in 1..5) { "有用程度必须在 1 到 5 之间" }
        val result = QuestResultEntity(UUID.randomUUID().toString(), id, input.resultText.trim(), input.actualMinutes, input.difficultyRating, input.usefulnessRating, now, null, emptyList(), emptyList(), null, null)
        resultDao.upsert(result)
        questDao.update(quest.copy(status = QuestStatus.COMPLETED, completedAt = now))
        result
    }

    suspend fun saveAnalysis(questId: String, analysis: AiQuestResultAnalysis, memories: List<MemoryEntity>) = database.withTransaction {
        val result = checkNotNull(resultDao.getForQuest(questId)) { "任务结果不存在" }
        resultDao.upsert(result.copy(analysisSummary = analysis.summary.trim(), evidence = analysis.evidence, insights = analysis.insights, projectProgress = analysis.projectProgress.takeIf(String::isNotBlank), suggestedNextStep = analysis.suggestedNextStep.takeIf(String::isNotBlank)))
        memories.forEach { if (database.memoryDao().findByContent(it.content) == null) database.memoryDao().upsert(it) }
    }

    private fun NewQuest.toEntity(createdAt: Long) = QuestEntity(
        id = UUID.randomUUID().toString(), goalId = goalId, projectId = projectId, skillId = skillId, chainId = chainId,
        title = title.trim(), reason = reason.trim(), steps = steps.map(String::trim), instruction = instruction.trim(),
        estimatedMinutes = estimatedMinutes, completionCriteria = completionCriteria.map(String::trim), expectedOutput = expectedOutput.trim(),
        difficulty = difficulty, status = QuestStatus.PENDING, createdAt = createdAt, completedAt = null,
        sourceIntention = sourceIntention.trim(), sourceAvailableMinutes = sourceAvailableMinutes, sourceEnergy = sourceEnergy,
        sourceResources = sourceResources.distinct(),
    )
}
