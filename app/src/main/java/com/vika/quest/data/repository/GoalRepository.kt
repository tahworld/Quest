package com.vika.quest.data.repository

import com.vika.quest.data.local.dao.GoalDao
import com.vika.quest.data.local.entity.GoalEntity
import java.util.UUID
import kotlinx.coroutines.flow.Flow

class GoalRepository(
    private val goalDao: GoalDao,
) {
    fun observeGoals(): Flow<List<GoalEntity>> = goalDao.observeAll()

    suspend fun getGoal(id: String): GoalEntity? = goalDao.getById(id)

    suspend fun createGoal(
        name: String,
        description: String,
        priority: Int = 0,
        createdAt: Long = System.currentTimeMillis(),
    ): GoalEntity {
        require(name.isNotBlank()) { "目标名称不能为空" }

        val goal = GoalEntity(
            id = UUID.randomUUID().toString(),
            name = name.trim(),
            description = description.trim(),
            priority = priority,
            createdAt = createdAt,
        )
        goalDao.upsert(goal)
        return goal
    }

    suspend fun saveGoal(goal: GoalEntity) {
        require(goal.name.isNotBlank()) { "目标名称不能为空" }
        goalDao.upsert(goal.copy(name = goal.name.trim(), description = goal.description.trim()))
    }

    suspend fun deleteGoal(goal: GoalEntity) = goalDao.delete(goal)
}
