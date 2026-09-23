package com.vika.quest.data.repository

import com.vika.quest.data.local.dao.ProjectDao
import com.vika.quest.data.local.entity.ProjectEntity
import com.vika.quest.model.ProjectStatus
import java.util.UUID

class ProjectRepository(private val dao: ProjectDao) {
    fun observeProjects() = dao.observeAll()
    suspend fun getActive(limit: Int) = dao.getActive(limit)
    suspend fun getProject(id: String) = dao.getById(id)
    suspend fun save(id: String?, name: String, description: String, currentState: String, goalId: String? = null, now: Long = System.currentTimeMillis()) {
        require(name.isNotBlank()) { "项目名称不能为空" }
        val old = id?.let { dao.getById(it) }
        dao.upsert(ProjectEntity(id ?: UUID.randomUUID().toString(), goalId ?: old?.goalId, name.trim(), description.trim(), currentState.trim(), old?.status ?: ProjectStatus.ACTIVE, old?.createdAt ?: now, now))
    }
}
